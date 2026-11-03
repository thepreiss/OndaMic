package com.ondamic.app.audio

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.os.Process
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Core Real-Time Audio Engine for OndaMic.
 * Manages:
 * - Dual-Mode Capture:
 *   1. CALL_VOICE_FOCUS (Mono VOICE_COMMUNICATION + Hardware AEC/NS/AGC + OEM Voice Focus)
 *   2. LOW_LATENCY_STUDIO (Mono or True Dual-Mic Stereo CAMCORDER/UNPROCESSED + Low-Latency Track)
 * - Hardware capsule routing (`preferredDevice`) and Polar Beamforming (`setMicrophoneDirection`)
 * - Hardware `DynamicsProcessing` output stage limiter + custom 32-bit Float `DspProcessor`
 * - Disconnection Kill-Switch (`ACTION_AUDIO_BECOMING_NOISY` & `AudioDeviceCallback`)
 * - AudioFocus management (with optional Auto-Ducking of background music and Call Interception)
 */
class AudioEngine(context: Context) {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val dspProcessor = DspProcessor(48000)
    val wavRecorder = WavRecorder(appContext)
    val deviceManager = AudioDeviceManager(
        context = appContext,
        onExternalOutputDisconnected = { handleUnexpectedOutputDisconnect() }
    )

    private val _config = MutableStateFlow(AudioConfig())
    val config: StateFlow<AudioConfig> = _config.asStateFlow()

    private val _telemetry = MutableStateFlow(AudioTelemetry())
    val telemetry: StateFlow<AudioTelemetry> = _telemetry.asStateFlow()

    @Volatile
    private var isEngineRunning = false

    @Volatile
    private var isCoughMuted = false

    @Volatile
    private var isPttHeld = false

    private var audioThread: Thread? = null
    private var countdownJob: Job? = null
    private var activeFocusRequest: AudioFocusRequest? = null
    private var noisyReceiverRegistered = false

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                handleUnexpectedOutputDisconnect()
            }
        }
    }

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Incoming phone call or critical system interruption: mute immediately to prevent howling
                isCoughMuted = true
                _telemetry.update { it.copy(isMutedOrCough = true) }
            }
        }
    }

    fun updateConfig(transform: (AudioConfig) -> AudioConfig) {
        val previous = _config.value
        val updated = transform(previous).enforceCompatibility()
        _config.value = updated
        dspProcessor.updateConfig(updated, _telemetry.value.sampleRateHz)

        // If switching between Mono/Stereo or Call/Studio or changing preferred capsules while live, restart stream smoothly
        val requiresStreamReopen = isEngineRunning && (
            previous.channelMode != updated.channelMode ||
                previous.processingMode != updated.processingMode ||
                previous.preferredInputDeviceId != updated.preferredInputDeviceId ||
                previous.preferredOutputDeviceId != updated.preferredOutputDeviceId ||
                previous.micDirection != updated.micDirection ||
                previous.micFieldDimension != updated.micFieldDimension
            )
        if (requiresStreamReopen) {
            restartStreamInternal()
        }
    }

    fun setDirectConfig(newConfig: AudioConfig) {
        updateConfig { newConfig }
    }

    fun startLiveAmplification(bypassCountdown: Boolean = false, onStarted: () -> Unit = {}) {
        if (isEngineRunning) return
        countdownJob?.cancel()

        val cfg = _config.value
        if (cfg.stageDelay3s && !bypassCountdown && cfg.triggerMode != TriggerMode.HOLD_PTT) {
            countdownJob = engineScope.launch {
                for (sec in 3 downTo 1) {
                    _telemetry.update { it.copy(countdownSecondsLeft = sec, autoMutedByDisconnect = false) }
                    delay(1000L)
                }
                _telemetry.update { it.copy(countdownSecondsLeft = 0) }
                launchAudioPipeline()
                onStarted()
            }
        } else {
            _telemetry.update { it.copy(countdownSecondsLeft = 0, autoMutedByDisconnect = false) }
            launchAudioPipeline()
            onStarted()
        }
    }

    fun stopLiveAmplification() {
        countdownJob?.cancel()
        _telemetry.update { it.copy(countdownSecondsLeft = 0) }
        if (!isEngineRunning) return
        isEngineRunning = false
        isPttHeld = false
        isCoughMuted = false
        audioThread?.join(600L)
        audioThread = null
        wavRecorder.stopRecording()
        abandonAudioFocus()
        unregisterNoisyReceiver()
        _telemetry.update {
            it.copy(
                isLive = false,
                isMutedOrCough = false,
                isPttPressed = false,
                levelLeftDb = -90f,
                levelRightDb = -90f,
                peakLeftDb = -90f,
                peakRightDb = -90f,
                gainReductionDb = 0f,
                isRecordingWav = false,
                currentRecordingDurationSec = 0
            )
        }
    }

    fun setPushToTalkPressed(pressed: Boolean) {
        isPttHeld = pressed
        _telemetry.update { it.copy(isPttPressed = pressed) }
        if (pressed && !isEngineRunning && _config.value.triggerMode == TriggerMode.HOLD_PTT) {
            startLiveAmplification(bypassCountdown = true)
        }
    }

    fun toggleCoughMute() {
        isCoughMuted = !isCoughMuted
        _telemetry.update { it.copy(isMutedOrCough = isCoughMuted) }
    }

    fun playAnnouncementChime() {
        dspProcessor.triggerAnnouncementChime()
    }

    fun clearFeedbackFilters() {
        dspProcessor.clearFeedbackNotches()
        _telemetry.update { it.copy(feedbackDetectedHz = null, activeNotchCount = 0) }
    }

    private fun handleUnexpectedOutputDisconnect() {
        if (!isEngineRunning) return
        if (_config.value.disconnectKillSwitch) {
            stopLiveAmplification()
            _telemetry.update { it.copy(autoMutedByDisconnect = true) }
        }
    }

    private fun restartStreamInternal() {
        val wasRunning = isEngineRunning
        if (!wasRunning) return
        isEngineRunning = false
        audioThread?.join(500L)
        launchAudioPipeline()
    }

    @SuppressLint("MissingPermission")
    private fun launchAudioPipeline() {
        if (isEngineRunning) return
        isEngineRunning = true

        requestAudioFocusIfNeeded()
        registerNoisyReceiver()

        val cfg = _config.value.enforceCompatibility()
        if (cfg.parallelWavRecord && !wavRecorder.isRecording) {
            val recChannels = if (cfg.channelMode == ChannelMode.STEREO_DUAL_MIC) 2 else 1
            wavRecorder.startRecording(48000, recChannels)
        }

        audioThread = Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            runRealTimeLoop()
        }, "OndaMic-RealTimeDSP").apply {
            isDaemon = true
            start()
        }
    }

    @SuppressLint("MissingPermission")
    private fun runRealTimeLoop() {
        val cfg = _config.value.enforceCompatibility()
        val sampleRate = 48000
        val isStereoCapture = cfg.channelMode == ChannelMode.STEREO_DUAL_MIC
        val inChannelMask = if (isStereoCapture) {
            AudioFormat.CHANNEL_IN_STEREO
        } else {
            AudioFormat.CHANNEL_IN_MONO
        }

        val audioSource = when {
            isStereoCapture -> MediaRecorder.AudioSource.CAMCORDER
            cfg.processingMode == ProcessingMode.CALL_VOICE_FOCUS -> MediaRecorder.AudioSource.VOICE_COMMUNICATION
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> MediaRecorder.AudioSource.VOICE_PERFORMANCE
            else -> MediaRecorder.AudioSource.MIC
        }

        val minInBytes = AudioRecord.getMinBufferSize(
            sampleRate,
            inChannelMask,
            AudioFormat.ENCODING_PCM_FLOAT
        ).coerceAtLeast(1024)

        val minOutBytes = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT
        ).coerceAtLeast(1024)

        // Small block size (192 frames = 4ms at 48kHz) for ultra-low latency processing
        val blockFrames = 192
        val inChannels = if (isStereoCapture) 2 else 1
        val inputBuffer = FloatArray(blockFrames * inChannels)
        val outputStereoBuffer = FloatArray(blockFrames * 2)

        var audioRecord: AudioRecord? = null
        var audioTrack: AudioTrack? = null
        var hwAec: AcousticEchoCanceler? = null
        var hwNs: NoiseSuppressor? = null
        var hwAgc: AutomaticGainControl? = null
        var dynamicsProcessing: DynamicsProcessing? = null

        try {
            // Configure communication mode if Call Voice Focus is selected
            if (cfg.processingMode == ProcessingMode.CALL_VOICE_FOCUS && !isStereoCapture) {
                runCatching { audioManager.mode = AudioManager.MODE_IN_COMMUNICATION }
            } else {
                runCatching { audioManager.mode = AudioManager.MODE_NORMAL }
            }

            audioRecord = AudioRecord.Builder()
                .setAudioSource(audioSource)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setChannelMask(inChannelMask)
                        .build()
                )
                .setBufferSizeInBytes(minInBytes * 2)
                .build()

            // Apply preferred physical input microphone if in Mono mode
            if (!isStereoCapture) {
                deviceManager.findRawInputDevice(cfg.preferredInputDeviceId)?.let { dev ->
                    runCatching { audioRecord.preferredDevice = dev }
                }
            }

            // Apply polar beamforming direction (API 29+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching {
                    audioRecord.setPreferredMicrophoneDirection(cfg.micDirection.androidDirectionConstant)
                    audioRecord.setPreferredMicrophoneFieldDimension(cfg.micFieldDimension.coerceIn(-1f, 1f))
                }
            }

            // Attach Hardware Call DSP Effects when in CALL_VOICE_FOCUS mode
            val sessionId = audioRecord.audioSessionId
            var aecEnabled = false
            var nsEnabled = false
            var agcEnabled = false

            if (cfg.processingMode == ProcessingMode.CALL_VOICE_FOCUS && !isStereoCapture) {
                if (AcousticEchoCanceler.isAvailable()) {
                    hwAec = runCatching {
                        AcousticEchoCanceler.create(sessionId)?.apply { enabled = true }
                    }.getOrNull()
                    aecEnabled = hwAec?.enabled == true
                }
                if (NoiseSuppressor.isAvailable()) {
                    hwNs = runCatching {
                        NoiseSuppressor.create(sessionId)?.apply { enabled = true }
                    }.getOrNull()
                    nsEnabled = hwNs?.enabled == true
                }
                if (AutomaticGainControl.isAvailable()) {
                    hwAgc = runCatching {
                        AutomaticGainControl.create(sessionId)?.apply { enabled = true }
                    }.getOrNull()
                    agcEnabled = hwAgc?.enabled == true
                }
            }

            val outUsage = if (cfg.processingMode == ProcessingMode.CALL_VOICE_FOCUS && !isStereoCapture) {
                AudioAttributes.USAGE_VOICE_COMMUNICATION
            } else {
                AudioAttributes.USAGE_MEDIA
            }

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(outUsage)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(minOutBytes)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            deviceManager.findRawOutputDevice(cfg.preferredOutputDeviceId)?.let { outDev ->
                runCatching { audioTrack.preferredDevice = outDev }
            }

            // Attach Hardware DynamicsProcessing Limiter to Output Session (API 28+)
            runCatching {
                val dpConfig = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2, // Stereo channels
                    false, 0,
                    false, 0,
                    false, 0,
                    true // Limiter enabled
                ).build()
                dynamicsProcessing = DynamicsProcessing(0, audioTrack.audioSessionId, dpConfig).apply {
                    enabled = true
                }
            }

            dspProcessor.updateConfig(cfg, sampleRate)
            audioRecord.startRecording()
            audioTrack.play()

            val estimatedLatency = ((minInBytes + minOutBytes).toFloat() / (sampleRate * 4 * 2) * 1000f)
                .toInt()
                .coerceIn(8, 45)

            var uiFrameThrottle = 0
            val waveformL = FloatArray(48)
            val waveformR = FloatArray(48)

            while (isEngineRunning) {
                val activeCfg = _config.value
                val floatsToRead = blockFrames * inChannels
                val readCount = audioRecord.read(
                    inputBuffer,
                    0,
                    floatsToRead,
                    AudioRecord.READ_BLOCKING
                )
                if (readCount <= 0) continue

                val actualFrames = readCount / inChannels
                val shouldTransmit = when (activeCfg.triggerMode) {
                    TriggerMode.CONTINUOUS -> !isCoughMuted
                    TriggerMode.HOLD_PTT -> isPttHeld && !isCoughMuted
                    TriggerMode.VOX -> !isCoughMuted
                }
                val isSpeakerOut = deviceManager.isOutputInternalSpeaker(activeCfg.preferredOutputDeviceId)

                dspProcessor.processBlock(
                    inputBuffer = inputBuffer,
                    outputStereoBuffer = outputStereoBuffer,
                    numFrames = actualFrames,
                    inputChannels = inChannels,
                    isTransmitting = shouldTransmit,
                    isInternalSpeakerActive = isSpeakerOut
                )

                audioTrack.write(
                    outputStereoBuffer,
                    0,
                    actualFrames * 2,
                    AudioTrack.WRITE_BLOCKING
                )

                if (wavRecorder.isRecording) {
                    wavRecorder.writeStereoFloatFrames(outputStereoBuffer, actualFrames)
                }

                // Update UI telemetry at ~30 Hz (every 8th 4ms block = 32ms)
                uiFrameThrottle++
                if (uiFrameThrottle >= 8) {
                    uiFrameThrottle = 0
                    val step = max(1, actualFrames / 48)
                    for (i in 0 until 48) {
                        val fIdx = (i * step).coerceAtMost(actualFrames - 1) * 2
                        waveformL[i] = outputStereoBuffer[fIdx]
                        waveformR[i] = outputStereoBuffer[fIdx + 1]
                    }
                    _telemetry.update {
                        it.copy(
                            isLive = true,
                            isMutedOrCough = isCoughMuted,
                            isPttPressed = isPttHeld,
                            isVoxGateOpen = dspProcessor.isVoxOpen,
                            levelLeftDb = dspProcessor.lastRmsLeftDb,
                            levelRightDb = dspProcessor.lastRmsRightDb,
                            peakLeftDb = dspProcessor.lastPeakLeftDb,
                            peakRightDb = dspProcessor.lastPeakRightDb,
                            gainReductionDb = dspProcessor.lastLimiterReductionDb,
                            feedbackDetectedHz = dspProcessor.detectedFeedbackHz,
                            activeNotchCount = dspProcessor.activeNotchCount,
                            estimatedLatencyMs = estimatedLatency,
                            sampleRateHz = sampleRate,
                            activeChannelCount = inChannels,
                            waveformLeft = waveformL.copyOf(),
                            waveformRight = waveformR.copyOf(),
                            isRecordingWav = wavRecorder.isRecording,
                            currentRecordingDurationSec = wavRecorder.elapsedSeconds,
                            hardwareAecActive = aecEnabled,
                            hardwareNsActive = nsEnabled,
                            hardwareAgcActive = agcEnabled
                        )
                    }
                }
            }
        } catch (_: Throwable) {
            // Gracefully recover and clean up hardware resources
        } finally {
            runCatching { dynamicsProcessing?.release() }
            runCatching { hwAec?.release() }
            runCatching { hwNs?.release() }
            runCatching { hwAgc?.release() }
            runCatching {
                audioRecord?.stop()
                audioRecord?.release()
            }
            runCatching {
                audioTrack?.stop()
                audioTrack?.release()
            }
            runCatching { audioManager.mode = AudioManager.MODE_NORMAL }
        }
    }

    private fun requestAudioFocusIfNeeded() {
        val duckMusic = _config.value.autoDuckMusic
        val focusGain = if (duckMusic) {
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        } else {
            AudioManager.AUDIOFOCUS_GAIN
        }
        val req = AudioFocusRequest.Builder(focusGain)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener(audioFocusListener)
            .build()
        activeFocusRequest = req
        runCatching { audioManager.requestAudioFocus(req) }
    }

    private fun abandonAudioFocus() {
        activeFocusRequest?.let { req ->
            runCatching { audioManager.abandonAudioFocusRequest(req) }
        }
        activeFocusRequest = null
    }

    private fun registerNoisyReceiver() {
        if (noisyReceiverRegistered) return
        runCatching {
            ContextCompat.registerReceiver(
                appContext,
                becomingNoisyReceiver,
                IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            noisyReceiverRegistered = true
        }
    }

    private fun unregisterNoisyReceiver() {
        if (!noisyReceiverRegistered) return
        runCatching {
            appContext.unregisterReceiver(becomingNoisyReceiver)
        }
        noisyReceiverRegistered = false
    }

    fun release() {
        stopLiveAmplification()
        deviceManager.release()
    }
}
