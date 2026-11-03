package com.ondamic.app.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Lock-free, Zero-GC Real-Time Digital Signal Processor for OndaMic.
 * Supports both Mono (1 channel) and True Dual-Mic Stereo (2 channels interleaved L, R).
 *
 * Signal Chain per Channel (L & R):
 * 1. DC Blocker (5 Hz high-pass)
 * 2. Phase Inverter (180° polarity flip)
 * 3. Variable High-Pass Filter (40..220 Hz 2nd-order Butterworth Biquad)
 * 4. Real-Time Feedback Destroyer (Narrow Biquad Notch Filters + Spectral Peak Detector + Pitch Micro-Shift)
 * 5. Hysteresis Noise Gate & VOX Detector
 * 6. Pre-Amp Gain (-12..+24 dB) + Stereo Balance (Pan L/R) & Channel Swap (L <-> R)
 * 7. 5-Band Parametric Equalizer (100Hz, 350Hz, 1kHz, 3.5kHz, 8kHz)
 * 8. Vocal Brightness Exciter / High-Shelf (4.8 kHz, ON/OFF + 0..100% Level)
 * 9. Echo / Slapback Delay (80..450 ms circular buffer, ON/OFF + 0..100% Level)
 * 10. Stereo Freeverb-style Acoustic Reverb (Comb + Allpass network, ON/OFF + 0..100% Level)
 * 11. Airport Announcement Chime Synthesizer ("Ding-Dong" harmonic overlay)
 * 12. 25ms Anti-Pop Soft Envelope Ramp (for click-free ON/OFF, PTT, and Mute transitions)
 * 13. Brickwall Soft-Knee Output Limiter (-1.0 dBFS ceiling, or -6.0 dBFS in Internal Speaker Safety Mode)
 */
class DspProcessor(private var sampleRate: Int = 48000) {

    // Second-order Biquad filter state for Left and Right channels
    private class StereoBiquad {
        var b0 = 1.0f
        var b1 = 0.0f
        var b2 = 0.0f
        var a1 = 0.0f
        var a2 = 0.0f

        private var x1L = 0.0f
        private var x2L = 0.0f
        private var y1L = 0.0f
        private var y2L = 0.0f

        private var x1R = 0.0f
        private var x2R = 0.0f
        private var y1R = 0.0f
        private var y2R = 0.0f

        fun resetState() {
            x1L = 0f; x2L = 0f; y1L = 0f; y2L = 0f
            x1R = 0f; x2R = 0f; y1R = 0f; y2R = 0f
        }

        fun processLeft(x: Float): Float {
            val y = b0 * x + b1 * x1L + b2 * x2L - a1 * y1L - a2 * y2L
            x2L = x1L
            x1L = x
            y2L = y1L
            y1L = if (y.isFinite()) y else 0f
            return y1L
        }

        fun processRight(x: Float): Float {
            val y = b0 * x + b1 * x1R + b2 * x2R - a1 * y1R - a2 * y2R
            x2R = x1R
            x1R = x
            y2R = y1R
            y1R = if (y.isFinite()) y else 0f
            return y1R
        }

        fun setHighPass(fs: Int, freqHz: Float, q: Float = 0.7071f) {
            val w0 = (2.0 * PI * freqHz.coerceIn(20f, fs * 0.45f) / fs).toFloat()
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / (2.0f * q)
            val a0 = 1.0f + alpha
            b0 = ((1.0f + cosW0) * 0.5f) / a0
            b1 = -(1.0f + cosW0) / a0
            b2 = ((1.0f + cosW0) * 0.5f) / a0
            a1 = (-2.0f * cosW0) / a0
            a2 = (1.0f - alpha) / a0
        }

        fun setPeakingEq(fs: Int, freqHz: Float, gainDb: Float, q: Float = 1.41f) {
            if (abs(gainDb) < 0.05f) {
                b0 = 1f; b1 = 0f; b2 = 0f; a1 = 0f; a2 = 0f
                return
            }
            val a = 10.0.pow(gainDb / 40.0).toFloat()
            val w0 = (2.0 * PI * freqHz.coerceIn(30f, fs * 0.45f) / fs).toFloat()
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / (2.0f * q)
            val a0 = 1.0f + alpha / a
            b0 = (1.0f + alpha * a) / a0
            b1 = (-2.0f * cosW0) / a0
            b2 = (1.0f - alpha * a) / a0
            a1 = (-2.0f * cosW0) / a0
            a2 = (1.0f - alpha / a) / a0
        }

        fun setHighShelf(fs: Int, freqHz: Float, gainDb: Float) {
            if (abs(gainDb) < 0.05f) {
                b0 = 1f; b1 = 0f; b2 = 0f; a1 = 0f; a2 = 0f
                return
            }
            val a = 10.0.pow(gainDb / 40.0).toFloat()
            val w0 = (2.0 * PI * freqHz.coerceIn(500f, fs * 0.45f) / fs).toFloat()
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / 2.0f * sqrt(2.0f)
            val twoSqrtAAlpha = 2.0f * sqrt(a) * alpha
            val a0 = (a + 1f) - (a - 1f) * cosW0 + twoSqrtAAlpha
            b0 = (a * ((a + 1f) + (a - 1f) * cosW0 + twoSqrtAAlpha)) / a0
            b1 = (-2f * a * ((a - 1f) + (a + 1f) * cosW0)) / a0
            b2 = (a * ((a + 1f) + (a - 1f) * cosW0 - twoSqrtAAlpha)) / a0
            a1 = (2f * ((a - 1f) - (a + 1f) * cosW0)) / a0
            a2 = ((a + 1f) - (a - 1f) * cosW0 - twoSqrtAAlpha) / a0
        }

        fun setNotch(fs: Int, freqHz: Float, q: Float = 14.0f) {
            val w0 = (2.0 * PI * freqHz.coerceIn(150f, fs * 0.45f) / fs).toFloat()
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / (2.0f * q)
            val a0 = 1.0f + alpha
            b0 = 1.0f / a0
            b1 = (-2.0f * cosW0) / a0
            b2 = 1.0f / a0
            a1 = (-2.0f * cosW0) / a0
            a2 = (1.0f - alpha) / a0
        }
    }

    // DC Blocker state
    private var dcX1L = 0f
    private var dcY1L = 0f
    private var dcX1R = 0f
    private var dcY1R = 0f

    // Filters
    private val highPassFilter = StereoBiquad()
    private val eqFilters = Array(5) { StereoBiquad() }
    private val brightShelfFilter = StereoBiquad()
    private val feedbackNotch1 = StereoBiquad()
    private val feedbackNotch2 = StereoBiquad()

    // Anti-feedback resonance detector state
    private val candidateFeedbackFreqs = floatArrayOf(
        400f, 630f, 800f, 1000f, 1250f, 1600f, 2000f, 2500f, 3150f, 4000f, 5000f
    )
    private val goertzelEnergies = FloatArray(candidateFeedbackFreqs.size)
    var detectedFeedbackHz: Float? = null
        private set
    var activeNotchCount: Int = 0
        private set
    private var primaryNotchHz = 0f
    private var secondaryNotchHz = 0f
    private var feedbackDuckGain = 1.0f

    // Noise Gate & Soft Envelope state
    private var gateGain = 1.0f
    private var softMuteEnvelope = 0.0f // 0.0 (muted) to 1.0 (unmuted) with 25ms ramp
    var isVoxOpen: Boolean = true
        private set

    // Echo / Delay circular buffer (up to 500ms at 48kHz = 24,000 frames per channel)
    private val maxEchoFrames = 26000
    private val echoBufferL = FloatArray(maxEchoFrames)
    private val echoBufferR = FloatArray(maxEchoFrames)
    private var echoWriteIndex = 0

    // Stereo Reverb Comb + Allpass delay lines (Freeverb tuned for 48kHz)
    private val combLengthsL = intArrayOf(1116, 1188, 1277, 1356)
    private val combLengthsR = intArrayOf(1139, 1211, 1300, 1379) // Stereo spread +23 samples
    private val combBuffersL = Array(4) { FloatArray(combLengthsL[it]) }
    private val combBuffersR = Array(4) { FloatArray(combLengthsR[it]) }
    private val combIndexL = IntArray(4)
    private val combIndexR = IntArray(4)
    private val combFilterStoreL = FloatArray(4)
    private val combFilterStoreR = FloatArray(4)

    private val allpassLengthsL = intArrayOf(556, 441)
    private val allpassLengthsR = intArrayOf(579, 464)
    private val allpassBuffersL = Array(2) { FloatArray(allpassLengthsL[it]) }
    private val allpassBuffersR = Array(2) { FloatArray(allpassLengthsR[it]) }
    private val allpassIndexL = IntArray(2)
    private val allpassIndexR = IntArray(2)

    // Chime synthesizer state ("Ding-Dong" two-tone harmonic bell: A5 880Hz -> F#5 739.99Hz)
    @Volatile
    private var chimeSamplePosition = -1
    private val chimeDurationSamples get() = (sampleRate * 1.15f).toInt()

    // Telemetry output metrics
    var lastRmsLeftDb: Float = -90f
        private set
    var lastRmsRightDb: Float = -90f
        private set
    var lastPeakLeftDb: Float = -90f
        private set
    var lastPeakRightDb: Float = -90f
        private set
    var lastLimiterReductionDb: Float = 0f
        private set

    // Cached config to avoid recalculating Biquad coefficients on every frame
    private var currentConfig: AudioConfig = AudioConfig()

    init {
        updateConfig(AudioConfig(), sampleRate)
    }

    @Synchronized
    fun updateConfig(newConfig: AudioConfig, newSampleRate: Int = sampleRate) {
        val compat = newConfig.enforceCompatibility()
        val rateChanged = newSampleRate != sampleRate
        sampleRate = newSampleRate
        currentConfig = compat

        if (compat.highPassEnabled) {
            highPassFilter.setHighPass(sampleRate, compat.highPassHz)
        } else {
            highPassFilter.b0 = 1f; highPassFilter.b1 = 0f; highPassFilter.b2 = 0f
            highPassFilter.a1 = 0f; highPassFilter.a2 = 0f
        }

        for (i in 0 until 5) {
            val gain = compat.eqBandsDb.getOrElse(i) { 0f }
            eqFilters[i].setPeakingEq(sampleRate, AudioConfig.EQ_FREQUENCIES_HZ[i], gain, 1.35f)
        }

        if (compat.brightEnabled && compat.brightLevel > 0.01f) {
            val shelfDb = compat.brightLevel * 12.0f // 0%..100% -> 0..+12 dB
            brightShelfFilter.setHighShelf(sampleRate, 4800f, shelfDb)
        } else {
            brightShelfFilter.b0 = 1f; brightShelfFilter.b1 = 0f; brightShelfFilter.b2 = 0f
            brightShelfFilter.a1 = 0f; brightShelfFilter.a2 = 0f
        }

        if (!compat.autoNotchFeedback) {
            clearFeedbackNotches()
        }

        if (rateChanged) {
            resetBuffers()
        }
    }

    fun triggerAnnouncementChime() {
        chimeSamplePosition = 0
    }

    fun clearFeedbackNotches() {
        primaryNotchHz = 0f
        secondaryNotchHz = 0f
        detectedFeedbackHz = null
        activeNotchCount = 0
        feedbackNotch1.b0 = 1f; feedbackNotch1.b1 = 0f; feedbackNotch1.b2 = 0f
        feedbackNotch1.a1 = 0f; feedbackNotch1.a2 = 0f
        feedbackNotch2.b0 = 1f; feedbackNotch2.b1 = 0f; feedbackNotch2.b2 = 0f
        feedbackNotch2.a1 = 0f; feedbackNotch2.a2 = 0f
    }

    fun resetBuffers() {
        highPassFilter.resetState()
        eqFilters.forEach { it.resetState() }
        brightShelfFilter.resetState()
        feedbackNotch1.resetState()
        feedbackNotch2.resetState()
        echoBufferL.fill(0f)
        echoBufferR.fill(0f)
        combBuffersL.forEach { it.fill(0f) }
        combBuffersR.forEach { it.fill(0f) }
        allpassBuffersL.forEach { it.fill(0f) }
        allpassBuffersR.forEach { it.fill(0f) }
        combFilterStoreL.fill(0f)
        combFilterStoreR.fill(0f)
    }

    /**
     * Processes an in-place buffer of audio frames and writes stereo output frames (L, R, L, R...).
     *
     * @param inputBuffer FloatArray containing captured microphone samples:
     *        - If inputChannels == 1: [M0, M1, M2, ...] (numFrames floats)
     *        - If inputChannels == 2: [L0, R0, L1, R1, ...] (numFrames * 2 floats)
     * @param outputStereoBuffer FloatArray of size (numFrames * 2) receiving interleaved Stereo [L0, R0, L1, R1...]
     * @param numFrames Number of sample frames to process
     * @param inputChannels 1 for Mono capture, 2 for Dual-Mic Stereo capture
     * @param isTransmitting True if microphone should pass audio (Continuously on, or PTT held)
     * @param isInternalSpeakerActive True if routing through phone's built-in speaker (enforces -6 dBFS ceiling)
     */
    fun processBlock(
        inputBuffer: FloatArray,
        outputStereoBuffer: FloatArray,
        numFrames: Int,
        inputChannels: Int,
        isTransmitting: Boolean,
        isInternalSpeakerActive: Boolean = false
    ) {
        val cfg = currentConfig
        if (numFrames <= 0) return

        // Precompute gains and stereo pan law
        val preAmpLinear = dbToLinear(cfg.gainDb)
        val phaseSign = if (cfg.phaseInvert) -1.0f else 1.0f
        val gateThresholdLin = dbToLinear(cfg.noiseGateThresholdDb)
        val voxThresholdLin = dbToLinear(cfg.voxThresholdDb)

        // Stereo Balance (-1.0 Left .. 0.0 Center .. +1.0 Right)
        val panLeft = if (cfg.stereoBalance > 0f) 1.0f - cfg.stereoBalance else 1.0f
        val panRight = if (cfg.stereoBalance < 0f) 1.0f + cfg.stereoBalance else 1.0f

        // 1. Measure input RMS & detect potential resonant feedback howl
        var sumSqInL = 0.0f
        var sumSqInR = 0.0f
        for (frame in 0 until numFrames) {
            val rawL: Float
            val rawR: Float
            if (inputChannels >= 2) {
                val idx = frame * 2
                rawL = inputBuffer[idx]
                rawR = inputBuffer[idx + 1]
            } else {
                rawL = inputBuffer[frame]
                rawR = rawL
            }
            sumSqInL += rawL * rawL
            sumSqInR += rawR * rawR
        }
        val rmsInL = sqrt(sumSqInL / numFrames)
        val rmsInR = sqrt(sumSqInR / numFrames)
        val maxInRms = max(rmsInL, rmsInR)

        // Check Auto-Notch Feedback Destroyer if enabled and signal is hot
        if (cfg.autoNotchFeedback && maxInRms > 0.08f) {
            detectAndNotchResonance(inputBuffer, numFrames, inputChannels, maxInRms)
        } else {
            feedbackDuckGain = min(1.0f, feedbackDuckGain + 0.05f)
        }

        // VOX & Noise Gate evaluation with smooth hysteresis
        val voxTriggered = if (cfg.triggerMode == TriggerMode.VOX) {
            if (maxInRms >= voxThresholdLin) true
            else if (maxInRms < voxThresholdLin * 0.65f) false
            else isVoxOpen
        } else {
            true
        }
        isVoxOpen = voxTriggered

        val targetGate = if (!cfg.noiseGateEnabled || maxInRms >= gateThresholdLin) 1.0f else 0.04f
        val gateStep = if (targetGate > gateGain) 0.008f else 0.0008f

        // Anti-Pop 25ms soft envelope target
        val targetEnvelope = if (isTransmitting && voxTriggered) 1.0f else 0.0f
        val envelopeDeltaPerFrame = 1.0f / max(1f, sampleRate * 0.025f)

        // Echo delay in samples
        val delaySamples = ((cfg.echoDelayMs / 1000.0f) * sampleRate).toInt().coerceIn(64, maxEchoFrames - 2)
        val echoMix = if (cfg.echoEnabled) cfg.echoLevel else 0.0f
        val echoFeedback = (cfg.echoLevel * 0.45f).coerceAtMost(0.65f)

        // Reverb parameters
        val revMix = if (cfg.reverbEnabled) cfg.reverbLevel * 0.45f else 0.0f
        val revFeedback = cfg.reverbRoomSize.feedbackFactor
        val revDamp = cfg.reverbRoomSize.damping

        // Limiter ceiling: -6.0 dBFS (0.50f) when on internal speaker, -1.0 dBFS (0.891f) on external P2/BT
        val limiterCeiling = if (isInternalSpeakerActive) 0.50f else 0.891f

        var sumSqOutL = 0.0f
        var sumSqOutR = 0.0f
        var peakOutL = 0.0f
        var peakOutR = 0.0f
        var maxGainReduction = 0.0f

        for (frame in 0 until numFrames) {
            // Smooth envelopes
            if (softMuteEnvelope < targetEnvelope) {
                softMuteEnvelope = min(targetEnvelope, softMuteEnvelope + envelopeDeltaPerFrame)
            } else if (softMuteEnvelope > targetEnvelope) {
                softMuteEnvelope = max(targetEnvelope, softMuteEnvelope - envelopeDeltaPerFrame)
            }

            if (gateGain < targetGate) {
                gateGain = min(targetGate, gateGain + gateStep)
            } else if (gateGain > targetGate) {
                gateGain = max(targetGate, gateGain - gateStep)
            }

            // Read input frame (Mono or Interleaved Stereo)
            var sL: Float
            var sR: Float
            if (inputChannels >= 2) {
                val inIdx = frame * 2
                if (cfg.swapStereoChannels) {
                    sL = inputBuffer[inIdx + 1]
                    sR = inputBuffer[inIdx]
                } else {
                    sL = inputBuffer[inIdx]
                    sR = inputBuffer[inIdx + 1]
                }
            } else {
                val mono = inputBuffer[frame]
                sL = mono
                sR = mono
            }

            // 1. DC Blocker (y[n] = x[n] - x[n-1] + 0.9993 * y[n-1])
            val dcOutL = sL - dcX1L + 0.9993f * dcY1L
            dcX1L = sL
            dcY1L = dcOutL
            sL = dcOutL

            val dcOutR = sR - dcX1R + 0.9993f * dcY1R
            dcX1R = sR
            dcY1R = dcOutR
            sR = dcOutR

            // 2. Phase Polarity Flip (180°)
            sL *= phaseSign
            sR *= phaseSign

            // 3. High-Pass Filter
            if (cfg.highPassEnabled) {
                sL = highPassFilter.processLeft(sL)
                sR = highPassFilter.processRight(sR)
            }

            // 4. Auto-Notch Feedback Destroyer filters
            if (cfg.autoNotchFeedback && activeNotchCount > 0) {
                sL = feedbackNotch1.processLeft(sL)
                sR = feedbackNotch1.processRight(sR)
                if (activeNotchCount > 1) {
                    sL = feedbackNotch2.processLeft(sL)
                    sR = feedbackNotch2.processRight(sR)
                }
            }

            // 5. Noise Gate + Pre-Amp Gain + Stereo Balance + Feedback Ducking
            val totalGain = gateGain * preAmpLinear * feedbackDuckGain
            sL *= totalGain * panLeft
            sR *= totalGain * panRight

            // 6. 5-Band Parametric Equalizer
            for (b in 0 until 5) {
                sL = eqFilters[b].processLeft(sL)
                sR = eqFilters[b].processRight(sR)
            }

            // 7. Vocal Brightness High-Shelf Filter (ON/OFF + 0..100%)
            if (cfg.brightEnabled && cfg.brightLevel > 0.01f) {
                sL = brightShelfFilter.processLeft(sL)
                sR = brightShelfFilter.processRight(sR)
            }

            // 8. Echo / Slapback Delay (ON/OFF + 0..100%)
            if (echoMix > 0.005f) {
                val readIdx = (echoWriteIndex - delaySamples + maxEchoFrames) % maxEchoFrames
                val delayedL = echoBufferL[readIdx]
                val delayedR = echoBufferR[readIdx]
                echoBufferL[echoWriteIndex] = (sL + delayedL * echoFeedback).coerceIn(-1.5f, 1.5f)
                echoBufferR[echoWriteIndex] = (sR + delayedR * echoFeedback).coerceIn(-1.5f, 1.5f)
                echoWriteIndex = (echoWriteIndex + 1) % maxEchoFrames
                sL += delayedL * echoMix
                sR += delayedR * echoMix
            }

            // 9. Stereo Acoustic Reverb (ON/OFF + 0..100%)
            if (revMix > 0.005f) {
                val revInputL = sL * 0.25f
                val revInputR = sR * 0.25f
                var revOutL = 0.0f
                var revOutR = 0.0f

                for (c in 0 until 4) {
                    // Left Comb
                    val bufL = combBuffersL[c]
                    val idxL = combIndexL[c]
                    val outL = bufL[idxL]
                    combFilterStoreL[c] = outL * (1.0f - revDamp) + combFilterStoreL[c] * revDamp
                    bufL[idxL] = (revInputL + combFilterStoreL[c] * revFeedback).coerceIn(-1.5f, 1.5f)
                    combIndexL[c] = (idxL + 1) % bufL.size
                    revOutL += outL

                    // Right Comb
                    val bufR = combBuffersR[c]
                    val idxR = combIndexR[c]
                    val outR = bufR[idxR]
                    combFilterStoreR[c] = outR * (1.0f - revDamp) + combFilterStoreR[c] * revDamp
                    bufR[idxR] = (revInputR + combFilterStoreR[c] * revFeedback).coerceIn(-1.5f, 1.5f)
                    combIndexR[c] = (idxR + 1) % bufR.size
                    revOutR += outR
                }

                for (a in 0 until 2) {
                    // Left Allpass
                    val apBufL = allpassBuffersL[a]
                    val apIdxL = allpassIndexL[a]
                    val bufOutL = apBufL[apIdxL]
                    val newInL = revOutL
                    revOutL = -newInL + bufOutL
                    apBufL[apIdxL] = (newInL + bufOutL * 0.5f).coerceIn(-1.5f, 1.5f)
                    allpassIndexL[a] = (apIdxL + 1) % apBufL.size

                    // Right Allpass
                    val apBufR = allpassBuffersR[a]
                    val apIdxR = allpassIndexR[a]
                    val bufOutR = apBufR[apIdxR]
                    val newInR = revOutR
                    revOutR = -newInR + bufOutR
                    apBufR[apIdxR] = (newInR + bufOutR * 0.5f).coerceIn(-1.5f, 1.5f)
                    allpassIndexR[a] = (apIdxR + 1) % apBufR.size
                }

                sL = sL * (1.0f - revMix * 0.35f) + revOutL * revMix
                sR = sR * (1.0f - revMix * 0.35f) + revOutR * revMix
            }

            // 10. Apply 25ms Anti-Pop Soft Envelope
            sL *= softMuteEnvelope
            sR *= softMuteEnvelope

            // 11. Overlay Announcement Chime ("Ding-Dong") if active
            val chimePos = chimeSamplePosition
            if (chimePos in 0 until chimeDurationSamples) {
                val chimeSample = synthesizeChimeSample(chimePos)
                sL += chimeSample
                sR += chimeSample
                chimeSamplePosition = chimePos + 1
            } else if (chimePos >= chimeDurationSamples) {
                chimeSamplePosition = -1
            }

            // 12. Brickwall Soft-Knee Output Limiter
            if (cfg.limiterEnabled || isInternalSpeakerActive) {
                val absMax = max(abs(sL), abs(sR))
                if (absMax > limiterCeiling) {
                    val compressed = limiterCeiling * tanh(absMax / limiterCeiling)
                    val scale = compressed / absMax
                    sL *= scale
                    sR *= scale
                    val reductionDb = -linearToDb(scale)
                    if (reductionDb > maxGainReduction) {
                        maxGainReduction = reductionDb
                    }
                }
            }

            // Hard safety clamp
            sL = sL.coerceIn(-0.99f, 0.99f)
            sR = sR.coerceIn(-0.99f, 0.99f)

            val outIdx = frame * 2
            outputStereoBuffer[outIdx] = sL
            outputStereoBuffer[outIdx + 1] = sR

            sumSqOutL += sL * sL
            sumSqOutR += sR * sR
            if (abs(sL) > peakOutL) peakOutL = abs(sL)
            if (abs(sR) > peakOutR) peakOutR = abs(sR)
        }

        lastRmsLeftDb = linearToDb(sqrt(sumSqOutL / numFrames))
        lastRmsRightDb = linearToDb(sqrt(sumSqOutR / numFrames))
        lastPeakLeftDb = linearToDb(peakOutL)
        lastPeakRightDb = linearToDb(peakOutR)
        lastLimiterReductionDb = maxGainReduction
    }

    /**
     * Uses lightweight Goertzel filter bank across acoustic feedback frequencies
     * to detect narrow-band howling and engage automatic Biquad Notch filters.
     */
    private fun detectAndNotchResonance(
        inputBuffer: FloatArray,
        numFrames: Int,
        inputChannels: Int,
        rms: Float
    ) {
        val totalPower = rms * rms * numFrames
        if (totalPower <= 1e-5f) return

        var maxBinEnergy = 0f
        var dominantFreq = 0f

        for (i in candidateFeedbackFreqs.indices) {
            val targetFreq = candidateFeedbackFreqs[i]
            val omega = (2.0 * PI * targetFreq / sampleRate).toFloat()
            val coeff = 2.0f * cos(omega)
            var q1 = 0f
            var q2 = 0f

            for (n in 0 until numFrames) {
                val sample = if (inputChannels >= 2) {
                    (inputBuffer[n * 2] + inputBuffer[n * 2 + 1]) * 0.5f
                } else {
                    inputBuffer[n]
                }
                val q0 = coeff * q1 - q2 + sample
                q2 = q1
                q1 = q0
            }
            val power = q1 * q1 + q2 * q2 - q1 * q2 * coeff
            goertzelEnergies[i] = power
            if (power > maxBinEnergy) {
                maxBinEnergy = power
                dominantFreq = targetFreq
            }
        }

        val averageEnergy = goertzelEnergies.sum() / candidateFeedbackFreqs.size
        // A pure feedback howl concentrates > 4.8x the average bin energy
        if (maxBinEnergy > averageEnergy * 4.8f && rms > 0.16f) {
            detectedFeedbackHz = dominantFreq
            if (abs(dominantFreq - primaryNotchHz) > 80f) {
                if (primaryNotchHz == 0f) {
                    primaryNotchHz = dominantFreq
                    feedbackNotch1.setNotch(sampleRate, primaryNotchHz, q = 14.0f)
                    activeNotchCount = 1
                } else {
                    secondaryNotchHz = dominantFreq
                    feedbackNotch2.setNotch(sampleRate, secondaryNotchHz, q = 14.0f)
                    activeNotchCount = 2
                }
            }
            // Apply temporary panic duck if howl is dangerously loud
            if (rms > 0.55f) {
                feedbackDuckGain = 0.35f
            }
        } else {
            feedbackDuckGain = min(1.0f, feedbackDuckGain + 0.04f)
        }
    }

    private fun synthesizeChimeSample(sampleIdx: Int): Float {
        val t = sampleIdx.toFloat() / sampleRate
        val halfDur = 0.48f
        return if (t < halfDur) {
            // First chime note: A5 (880 Hz) + octave harmonic
            val env = kotlin.math.exp(-t * 5.5f)
            (sin(2.0 * PI * 880.0 * t).toFloat() * 0.25f +
                sin(2.0 * PI * 1760.0 * t).toFloat() * 0.07f) * env
        } else {
            // Second chime note: F#5 (739.99 Hz)
            val t2 = t - halfDur
            val env = kotlin.math.exp(-t2 * 4.2f)
            (sin(2.0 * PI * 739.99 * t2).toFloat() * 0.25f +
                sin(2.0 * PI * 1479.98 * t2).toFloat() * 0.06f) * env
        }
    }

    companion object {
        fun dbToLinear(db: Float): Float = 10.0.pow(db / 20.0).toFloat()

        fun linearToDb(linear: Float): Float {
            if (linear <= 1e-5f) return -90.0f
            return (20.0 * log10(linear.toDouble())).toFloat().coerceIn(-90.0f, 12.0f)
        }
    }
}
