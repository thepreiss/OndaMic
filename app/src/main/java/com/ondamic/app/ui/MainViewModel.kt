package com.ondamic.app.ui

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ondamic.app.audio.AudioConfig
import com.ondamic.app.audio.AudioEndpointItem
import com.ondamic.app.audio.AudioEngine
import com.ondamic.app.audio.AudioTelemetry
import com.ondamic.app.audio.ChannelMode
import com.ondamic.app.audio.CompatibilityRestriction
import com.ondamic.app.audio.CustomUserPreset
import com.ondamic.app.audio.MicDirectionMode
import com.ondamic.app.audio.PresetProfile
import com.ondamic.app.audio.ProcessingMode
import com.ondamic.app.audio.ReverbRoomSize
import com.ondamic.app.audio.SavedRecordingItem
import com.ondamic.app.audio.TriggerMode
import com.ondamic.app.data.LanguageCatalog
import com.ondamic.app.data.OndaMicPreferencesRepository
import com.ondamic.app.service.OndaMicForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val prefsRepo = OndaMicPreferencesRepository(appContext)
    val audioEngine = AudioEngine(appContext)

    val config: StateFlow<AudioConfig> = audioEngine.config
    val telemetry: StateFlow<AudioTelemetry> = audioEngine.telemetry
    val inputDevices: StateFlow<List<AudioEndpointItem>> = audioEngine.deviceManager.inputDevices
    val outputDevices: StateFlow<List<AudioEndpointItem>> = audioEngine.deviceManager.outputDevices
    val customPresets: StateFlow<List<CustomUserPreset>> = prefsRepo.customPresetsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showSpeakerWarningDialog = MutableStateFlow(false)
    val showSpeakerWarningDialog: StateFlow<Boolean> = _showSpeakerWarningDialog.asStateFlow()

    private val _savedRecordings = MutableStateFlow<List<SavedRecordingItem>>(emptyList())
    val savedRecordings: StateFlow<List<SavedRecordingItem>> = _savedRecordings.asStateFlow()

    val activeRestrictions: StateFlow<Set<CompatibilityRestriction>> = combine(
        config,
        outputDevices
    ) { cfg, _ ->
        val isSpeaker = audioEngine.deviceManager.isOutputInternalSpeaker(cfg.preferredOutputDeviceId)
        cfg.getActiveRestrictions(isSpeaker)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        OndaMicForegroundService.onStopRequestedCallback = {
            stopAmplification()
        }
        viewModelScope.launch {
            val savedCfg = prefsRepo.audioConfigFlow.first()
            audioEngine.setDirectConfig(savedCfg)
            refreshRecordingsList()
        }
    }

    fun refreshHardwareDevices() {
        audioEngine.deviceManager.refreshDevices()
    }

    fun refreshRecordingsList() {
        _savedRecordings.value = audioEngine.wavRecorder.listRecordings()
    }

    fun onToggleAmplificationClicked() {
        if (telemetry.value.isLive || telemetry.value.countdownSecondsLeft > 0) {
            stopAmplification()
            return
        }
        val cfg = config.value
        val isInternalSpeaker = audioEngine.deviceManager.isOutputInternalSpeaker(cfg.preferredOutputDeviceId)
        if (isInternalSpeaker && !cfg.allowInternalSpeakerTest) {
            _showSpeakerWarningDialog.value = true
        } else {
            startAmplificationInternal()
        }
    }

    fun confirmInternalSpeakerTest() {
        _showSpeakerWarningDialog.value = false
        updateAndPersist {
            it.copy(
                allowInternalSpeakerTest = true,
                limiterEnabled = true,
                autoNotchFeedback = true
            )
        }
        startAmplificationInternal()
    }

    fun dismissSpeakerWarningDialog() {
        _showSpeakerWarningDialog.value = false
    }

    private fun startAmplificationInternal() {
        OndaMicForegroundService.startService(appContext)
        audioEngine.startLiveAmplification {
            refreshRecordingsList()
        }
    }

    fun stopAmplification() {
        audioEngine.stopLiveAmplification()
        OndaMicForegroundService.stopService(appContext)
        refreshRecordingsList()
    }

    fun onPushToTalkStateChanged(pressed: Boolean) {
        if (pressed && !telemetry.value.isLive) {
            val cfg = config.value
            val isSpeaker = audioEngine.deviceManager.isOutputInternalSpeaker(cfg.preferredOutputDeviceId)
            if (isSpeaker && !cfg.allowInternalSpeakerTest) {
                _showSpeakerWarningDialog.value = true
                return
            }
            OndaMicForegroundService.startService(appContext)
        }
        audioEngine.setPushToTalkPressed(pressed)
    }

    fun toggleCoughMute() {
        audioEngine.toggleCoughMute()
    }

    fun playAnnouncementChime() {
        if (!telemetry.value.isLive) {
            startAmplificationInternal()
        }
        audioEngine.playAnnouncementChime()
    }

    fun selectPreset(preset: PresetProfile) {
        updateAndPersist { current ->
            AudioConfig.fromPreset(preset, current)
        }
    }

    fun applyCustomPreset(custom: CustomUserPreset) {
        updateAndPersist { current ->
            custom.config.copy(
                preferredInputDeviceId = current.preferredInputDeviceId,
                preferredOutputDeviceId = current.preferredOutputDeviceId,
                keepScreenOn = current.keepScreenOn,
                stageDimmerMode = current.stageDimmerMode
            ).enforceCompatibility()
        }
    }

    fun saveCurrentAsCustomPreset(name: String) {
        viewModelScope.launch {
            prefsRepo.saveCustomPreset(name, config.value)
        }
    }

    fun deleteCustomPreset(id: String) {
        viewModelScope.launch {
            prefsRepo.deleteCustomPreset(id)
        }
    }

    fun setChannelMode(mode: ChannelMode) {
        updateAndPersist {
            it.copy(channelMode = mode, presetProfile = PresetProfile.CUSTOM)
        }
    }

    fun setProcessingMode(mode: ProcessingMode) {
        updateAndPersist {
            if (it.channelMode == ChannelMode.STEREO_DUAL_MIC) {
                it // Blocked in Stereo Dual-Mic mode
            } else {
                it.copy(processingMode = mode, presetProfile = PresetProfile.CUSTOM)
            }
        }
    }

    fun setTriggerMode(mode: TriggerMode) {
        updateAndPersist { it.copy(triggerMode = mode) }
    }

    fun setPreferredInputDevice(deviceId: Int?) {
        updateAndPersist {
            if (it.channelMode == ChannelMode.STEREO_DUAL_MIC) it
            else it.copy(preferredInputDeviceId = deviceId)
        }
    }

    fun setPreferredOutputDevice(deviceId: Int?) {
        updateAndPersist {
            it.copy(preferredOutputDeviceId = deviceId, allowInternalSpeakerTest = false)
        }
    }

    fun setMicDirection(direction: MicDirectionMode) {
        updateAndPersist { it.copy(micDirection = direction) }
    }

    fun setGainDb(gainDb: Float) {
        updateAndPersist { it.copy(gainDb = gainDb, presetProfile = PresetProfile.CUSTOM) }
    }

    fun setStereoBalance(balance: Float) {
        updateAndPersist { it.copy(stereoBalance = balance) }
    }

    fun setSwapStereoChannels(swap: Boolean) {
        updateAndPersist { it.copy(swapStereoChannels = swap) }
    }

    fun setBrightEnabled(enabled: Boolean) {
        updateAndPersist { it.copy(brightEnabled = enabled, presetProfile = PresetProfile.CUSTOM) }
    }

    fun setBrightLevel(level: Float) {
        updateAndPersist { it.copy(brightLevel = level, presetProfile = PresetProfile.CUSTOM) }
    }

    fun setReverbEnabled(enabled: Boolean) {
        updateAndPersist { it.copy(reverbEnabled = enabled, presetProfile = PresetProfile.CUSTOM) }
    }

    fun setReverbLevel(level: Float) {
        updateAndPersist { it.copy(reverbLevel = level, presetProfile = PresetProfile.CUSTOM) }
    }

    fun setReverbRoomSize(room: ReverbRoomSize) {
        updateAndPersist { it.copy(reverbRoomSize = room, presetProfile = PresetProfile.CUSTOM) }
    }

    fun setEchoEnabled(enabled: Boolean) {
        updateAndPersist { it.copy(echoEnabled = enabled, presetProfile = PresetProfile.CUSTOM) }
    }

    fun setEchoLevel(level: Float) {
        updateAndPersist { it.copy(echoLevel = level, presetProfile = PresetProfile.CUSTOM) }
    }

    fun setEchoDelayMs(delayMs: Int) {
        updateAndPersist { it.copy(echoDelayMs = delayMs, presetProfile = PresetProfile.CUSTOM) }
    }

    fun setEqBand(index: Int, gainDb: Float) {
        updateAndPersist { current ->
            val updatedBands = current.eqBandsDb.toMutableList()
            if (index in 0..4) {
                updatedBands[index] = gainDb
            }
            current.copy(eqBandsDb = updatedBands, presetProfile = PresetProfile.CUSTOM)
        }
    }

    fun resetEqFlat() {
        updateAndPersist {
            it.copy(eqBandsDb = listOf(0f, 0f, 0f, 0f, 0f), presetProfile = PresetProfile.CUSTOM)
        }
    }

    fun setAutoNotchFeedback(enabled: Boolean) {
        updateAndPersist { it.copy(autoNotchFeedback = enabled) }
    }

    fun clearActiveNotches() {
        audioEngine.clearFeedbackFilters()
    }

    fun setPhaseInvert(invert: Boolean) {
        updateAndPersist { it.copy(phaseInvert = invert, presetProfile = PresetProfile.CUSTOM) }
    }

    fun setHighPassHz(hz: Float) {
        updateAndPersist { it.copy(highPassEnabled = true, highPassHz = hz) }
    }

    fun setNoiseGateThresholdDb(db: Float) {
        updateAndPersist { it.copy(noiseGateEnabled = true, noiseGateThresholdDb = db) }
    }

    fun setVoxThresholdDb(db: Float) {
        updateAndPersist { it.copy(voxThresholdDb = db) }
    }

    fun setLimiterEnabled(enabled: Boolean) {
        updateAndPersist { it.copy(limiterEnabled = enabled) }
    }

    fun setVolumeKeyPtt(enabled: Boolean) {
        updateAndPersist { it.copy(volumeKeyPtt = enabled) }
    }

    fun setStageDelay3s(enabled: Boolean) {
        updateAndPersist { it.copy(stageDelay3s = enabled) }
    }

    fun setAutoDuckMusic(enabled: Boolean) {
        updateAndPersist { it.copy(autoDuckMusic = enabled) }
    }

    fun setDisconnectKillSwitch(enabled: Boolean) {
        updateAndPersist { it.copy(disconnectKillSwitch = enabled) }
    }

    fun setParallelWavRecord(enabled: Boolean) {
        updateAndPersist { it.copy(parallelWavRecord = enabled) }
    }

    fun toggleStageDimmer() {
        updateAndPersist { it.copy(stageDimmerMode = !it.stageDimmerMode) }
    }

    fun deleteRecording(file: File) {
        audioEngine.wavRecorder.deleteRecording(file)
        refreshRecordingsList()
    }

    fun createShareRecordingIntent(file: File): Intent? {
        return runCatching {
            val uri = FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                file
            )
            Intent(Intent.ACTION_SEND).apply {
                type = "audio/wav"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }.getOrNull()
    }

    fun changeAppLanguage(bcp47Tag: String) {
        LanguageCatalog.applyLanguage(bcp47Tag)
    }

    private fun updateAndPersist(transform: (AudioConfig) -> AudioConfig) {
        audioEngine.updateConfig(transform)
        viewModelScope.launch {
            prefsRepo.saveAudioConfig(audioEngine.config.value)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
