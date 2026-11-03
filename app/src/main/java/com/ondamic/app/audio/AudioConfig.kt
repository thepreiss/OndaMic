package com.ondamic.app.audio

import kotlin.math.max
import kotlin.math.min

enum class ChannelMode {
    MONO,
    STEREO_DUAL_MIC
}

enum class ProcessingMode {
    CALL_VOICE_FOCUS,
    LOW_LATENCY_STUDIO
}

enum class TriggerMode {
    CONTINUOUS,
    HOLD_PTT,
    VOX
}

enum class MicDirectionMode(val androidDirectionConstant: Int) {
    UNSPECIFIED(0), // AudioRecord.MIC_DIRECTION_UNSPECIFIED
    FRONT(1),       // AudioRecord.MIC_DIRECTION_FRONT
    BACK(2),        // AudioRecord.MIC_DIRECTION_BACK
    EXTERNAL(3)     // AudioRecord.MIC_DIRECTION_EXTERNAL
}

enum class ReverbRoomSize(val feedbackFactor: Float, val damping: Float) {
    STUDIO(0.68f, 0.42f),
    AUDITORIUM(0.81f, 0.30f),
    ARENA(0.89f, 0.20f)
}

enum class PresetProfile {
    CALL_FOCUS,
    SPEECH,
    ANTI_FEEDBACK,
    KARAOKE,
    MEGAPHONE,
    CUSTOM
}

enum class CompatibilityRestriction {
    STEREO_DISABLES_CALL_MODE,
    STEREO_DISABLES_SINGLE_MIC_SELECTOR,
    SPEAKER_OUTPUT_FORCES_ANTI_FEEDBACK
}

data class CustomUserPreset(
    val id: String,
    val name: String,
    val config: AudioConfig
)

data class AudioTelemetry(
    val isLive: Boolean = false,
    val isMutedOrCough: Boolean = false,
    val isPttPressed: Boolean = false,
    val isVoxGateOpen: Boolean = true,
    val countdownSecondsLeft: Int = 0,
    val levelLeftDb: Float = -90f,
    val levelRightDb: Float = -90f,
    val peakLeftDb: Float = -90f,
    val peakRightDb: Float = -90f,
    val gainReductionDb: Float = 0f,
    val feedbackDetectedHz: Float? = null,
    val activeNotchCount: Int = 0,
    val estimatedLatencyMs: Int = 14,
    val sampleRateHz: Int = 48000,
    val activeChannelCount: Int = 1,
    val waveformLeft: FloatArray = FloatArray(48),
    val waveformRight: FloatArray = FloatArray(48),
    val isRecordingWav: Boolean = false,
    val currentRecordingDurationSec: Int = 0,
    val hardwareAecActive: Boolean = false,
    val hardwareNsActive: Boolean = false,
    val hardwareAgcActive: Boolean = false,
    val autoMutedByDisconnect: Boolean = false
)

data class AudioConfig(
    val presetProfile: PresetProfile = PresetProfile.SPEECH,
    val channelMode: ChannelMode = ChannelMode.MONO,
    val processingMode: ProcessingMode = ProcessingMode.LOW_LATENCY_STUDIO,
    val triggerMode: TriggerMode = TriggerMode.CONTINUOUS,
    val preferredInputDeviceId: Int? = null,
    val preferredOutputDeviceId: Int? = null,
    val micDirection: MicDirectionMode = MicDirectionMode.FRONT,
    val micFieldDimension: Float = 0.5f, // 0.0 (narrow beam) to 1.0 (wide)

    // Pre-Amp & Stereo controls
    val gainDb: Float = 4.0f,            // -12.0f to +24.0f dB
    val stereoBalance: Float = 0.0f,     // -1.0f (Full Left) to +1.0f (Full Right)
    val swapStereoChannels: Boolean = false,

    // Anti-Feedback (Microfonia) & Dynamics
    val phaseInvert: Boolean = false,
    val autoNotchFeedback: Boolean = true,
    val highPassEnabled: Boolean = true,
    val highPassHz: Float = 100.0f,      // 40 Hz to 220 Hz
    val noiseGateEnabled: Boolean = true,
    val noiseGateThresholdDb: Float = -48.0f, // -70 dB to -20 dB
    val voxThresholdDb: Float = -38.0f,       // -55 dB to -15 dB
    val limiterEnabled: Boolean = true,

    // 5-Band Equalizer (100Hz, 350Hz, 1kHz, 3.5kHz, 8kHz) in dB (-12 to +12)
    val eqBandsDb: List<Float> = listOf(0.0f, 0.0f, 1.5f, 2.0f, 0.5f),

    // Individual Vocal Effects with independent ON/OFF and Level (0.0f..1.0f)
    val brightEnabled: Boolean = true,
    val brightLevel: Float = 0.35f,      // 0% to 100% (maps to 0..+12 dB high-shelf at 4.8kHz)

    val reverbEnabled: Boolean = false,
    val reverbLevel: Float = 0.28f,      // 0% to 100% wet mix
    val reverbRoomSize: ReverbRoomSize = ReverbRoomSize.AUDITORIUM,

    val echoEnabled: Boolean = false,
    val echoLevel: Float = 0.25f,        // 0% to 100% echo level
    val echoDelayMs: Int = 180,          // 80 ms to 450 ms

    // Stage & Safety Utilities
    val volumeKeyPtt: Boolean = false,
    val stageDelay3s: Boolean = false,
    val autoDuckMusic: Boolean = true,
    val parallelWavRecord: Boolean = false,
    val keepScreenOn: Boolean = true,
    val stageDimmerMode: Boolean = false,
    val disconnectKillSwitch: Boolean = true,
    val allowInternalSpeakerTest: Boolean = false
) {
    /**
     * Enforces hardware and Android OS compatibility rules:
     * - Stereo Dual-Mic mode requires raw/camcorder dual-capsule input and cannot use
     *   mono telephony VOICE_COMMUNICATION or single-capsule lock.
     */
    fun enforceCompatibility(): AudioConfig {
        val clampedBands = if (eqBandsDb.size == 5) {
            eqBandsDb.map { min(12f, max(-12f, it)) }
        } else {
            listOf(0f, 0f, 0f, 0f, 0f)
        }
        return if (channelMode == ChannelMode.STEREO_DUAL_MIC) {
            copy(
                processingMode = ProcessingMode.LOW_LATENCY_STUDIO,
                preferredInputDeviceId = null,
                presetProfile = if (presetProfile == PresetProfile.CALL_FOCUS) PresetProfile.CUSTOM else presetProfile,
                gainDb = min(24f, max(-12f, gainDb)),
                stereoBalance = min(1f, max(-1f, stereoBalance)),
                brightLevel = min(1f, max(0f, brightLevel)),
                reverbLevel = min(1f, max(0f, reverbLevel)),
                echoLevel = min(1f, max(0f, echoLevel)),
                echoDelayMs = min(450, max(80, echoDelayMs)),
                highPassHz = min(220f, max(40f, highPassHz)),
                eqBandsDb = clampedBands
            )
        } else {
            copy(
                gainDb = min(24f, max(-12f, gainDb)),
                stereoBalance = min(1f, max(-1f, stereoBalance)),
                brightLevel = min(1f, max(0f, brightLevel)),
                reverbLevel = min(1f, max(0f, reverbLevel)),
                echoLevel = min(1f, max(0f, echoLevel)),
                echoDelayMs = min(450, max(80, echoDelayMs)),
                highPassHz = min(220f, max(40f, highPassHz)),
                eqBandsDb = clampedBands
            )
        }
    }

    fun getActiveRestrictions(isInternalSpeakerActive: Boolean): Set<CompatibilityRestriction> {
        val restrictions = mutableSetOf<CompatibilityRestriction>()
        if (channelMode == ChannelMode.STEREO_DUAL_MIC) {
            restrictions.add(CompatibilityRestriction.STEREO_DISABLES_CALL_MODE)
            restrictions.add(CompatibilityRestriction.STEREO_DISABLES_SINGLE_MIC_SELECTOR)
        }
        if (isInternalSpeakerActive) {
            restrictions.add(CompatibilityRestriction.SPEAKER_OUTPUT_FORCES_ANTI_FEEDBACK)
        }
        return restrictions
    }

    companion object {
        val EQ_FREQUENCIES_HZ = floatArrayOf(100f, 350f, 1000f, 3500f, 8000f)

        fun fromPreset(preset: PresetProfile, current: AudioConfig = AudioConfig()): AudioConfig {
            val base = when (preset) {
                PresetProfile.CALL_FOCUS -> current.copy(
                    presetProfile = PresetProfile.CALL_FOCUS,
                    channelMode = ChannelMode.MONO,
                    processingMode = ProcessingMode.CALL_VOICE_FOCUS,
                    gainDb = 3.0f,
                    highPassEnabled = true,
                    highPassHz = 110f,
                    noiseGateEnabled = true,
                    noiseGateThresholdDb = -45f,
                    autoNotchFeedback = true,
                    phaseInvert = false,
                    eqBandsDb = listOf(-3.0f, -1.0f, 2.5f, 3.5f, 1.0f),
                    brightEnabled = true,
                    brightLevel = 0.40f,
                    reverbEnabled = false,
                    echoEnabled = false,
                    limiterEnabled = true
                )
                PresetProfile.SPEECH -> current.copy(
                    presetProfile = PresetProfile.SPEECH,
                    processingMode = ProcessingMode.LOW_LATENCY_STUDIO,
                    gainDb = 5.0f,
                    highPassEnabled = true,
                    highPassHz = 95f,
                    noiseGateEnabled = true,
                    noiseGateThresholdDb = -48f,
                    autoNotchFeedback = true,
                    phaseInvert = false,
                    eqBandsDb = listOf(-1.5f, 0.5f, 2.0f, 3.0f, 1.5f),
                    brightEnabled = true,
                    brightLevel = 0.35f,
                    reverbEnabled = false,
                    echoEnabled = false,
                    limiterEnabled = true
                )
                PresetProfile.ANTI_FEEDBACK -> current.copy(
                    presetProfile = PresetProfile.ANTI_FEEDBACK,
                    processingMode = ProcessingMode.CALL_VOICE_FOCUS,
                    channelMode = if (current.channelMode == ChannelMode.STEREO_DUAL_MIC) ChannelMode.STEREO_DUAL_MIC else ChannelMode.MONO,
                    gainDb = 1.5f,
                    highPassEnabled = true,
                    highPassHz = 140f,
                    noiseGateEnabled = true,
                    noiseGateThresholdDb = -38f,
                    autoNotchFeedback = true,
                    phaseInvert = true,
                    eqBandsDb = listOf(-6.0f, -2.0f, 1.5f, -1.5f, -4.5f),
                    brightEnabled = false,
                    brightLevel = 0.15f,
                    reverbEnabled = false,
                    echoEnabled = false,
                    limiterEnabled = true
                )
                PresetProfile.KARAOKE -> current.copy(
                    presetProfile = PresetProfile.KARAOKE,
                    processingMode = ProcessingMode.LOW_LATENCY_STUDIO,
                    gainDb = 6.0f,
                    highPassEnabled = true,
                    highPassHz = 85f,
                    noiseGateEnabled = true,
                    noiseGateThresholdDb = -52f,
                    autoNotchFeedback = true,
                    phaseInvert = false,
                    eqBandsDb = listOf(1.5f, 0.0f, 1.5f, 3.5f, 3.0f),
                    brightEnabled = true,
                    brightLevel = 0.55f,
                    reverbEnabled = true,
                    reverbLevel = 0.42f,
                    reverbRoomSize = ReverbRoomSize.AUDITORIUM,
                    echoEnabled = true,
                    echoLevel = 0.25f,
                    echoDelayMs = 195,
                    limiterEnabled = true
                )
                PresetProfile.MEGAPHONE -> current.copy(
                    presetProfile = PresetProfile.MEGAPHONE,
                    processingMode = ProcessingMode.LOW_LATENCY_STUDIO,
                    gainDb = 12.0f,
                    highPassEnabled = true,
                    highPassHz = 180f,
                    noiseGateEnabled = true,
                    noiseGateThresholdDb = -42f,
                    autoNotchFeedback = true,
                    phaseInvert = false,
                    eqBandsDb = listOf(-10.0f, -3.0f, 6.5f, 7.5f, -4.0f),
                    brightEnabled = true,
                    brightLevel = 0.65f,
                    reverbEnabled = false,
                    echoEnabled = false,
                    limiterEnabled = true
                )
                PresetProfile.CUSTOM -> current.copy(presetProfile = PresetProfile.CUSTOM)
            }
            return base.enforceCompatibility()
        }
    }
}
