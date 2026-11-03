package com.ondamic.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class DspProcessorTest {

    @Test
    fun stereoDualMicMode_enforcesCompatibilityAndDisablesCallVoiceFocus() {
        val initial = AudioConfig(
            presetProfile = PresetProfile.CALL_FOCUS,
            channelMode = ChannelMode.STEREO_DUAL_MIC,
            processingMode = ProcessingMode.CALL_VOICE_FOCUS,
            preferredInputDeviceId = 42
        )

        val enforced = initial.enforceCompatibility()

        assertEquals(ChannelMode.STEREO_DUAL_MIC, enforced.channelMode)
        assertEquals(ProcessingMode.LOW_LATENCY_STUDIO, enforced.processingMode)
        assertNull(enforced.preferredInputDeviceId)

        val restrictions = enforced.getActiveRestrictions(isInternalSpeakerActive = false)
        assertTrue(restrictions.contains(CompatibilityRestriction.STEREO_DISABLES_CALL_MODE))
        assertTrue(restrictions.contains(CompatibilityRestriction.STEREO_DISABLES_SINGLE_MIC_SELECTOR))
    }

    @Test
    fun dspProcessor_processesIndependentStereoChannelsAndSwap() {
        val dsp = DspProcessor(48000)
        val cfg = AudioConfig(
            channelMode = ChannelMode.STEREO_DUAL_MIC,
            gainDb = 0f,
            noiseGateEnabled = false,
            highPassEnabled = false,
            autoNotchFeedback = false,
            brightEnabled = false,
            reverbEnabled = false,
            echoEnabled = false,
            eqBandsDb = listOf(0f, 0f, 0f, 0f, 0f),
            swapStereoChannels = false
        )
        dsp.updateConfig(cfg, 48000)

        val frames = 1200
        val inputStereo = FloatArray(frames * 2)
        val outputStereo = FloatArray(frames * 2)

        // Put strong 440Hz sine on Left channel and near-silence on Right channel
        for (n in 0 until frames) {
            inputStereo[n * 2] = (sin(2.0 * PI * 440.0 * n / 48000.0) * 0.4).toFloat()
            inputStereo[n * 2 + 1] = 0.001f
        }

        dsp.processBlock(
            inputBuffer = inputStereo,
            outputStereoBuffer = outputStereo,
            numFrames = frames,
            inputChannels = 2,
            isTransmitting = true
        )

        assertTrue("Left channel should be significantly louder than Right channel", dsp.lastRmsLeftDb > dsp.lastRmsRightDb + 20f)

        // Now enable Swap Stereo Channels (L <-> R)
        dsp.updateConfig(cfg.copy(swapStereoChannels = true), 48000)
        dsp.processBlock(
            inputBuffer = inputStereo,
            outputStereoBuffer = outputStereo,
            numFrames = frames,
            inputChannels = 2,
            isTransmitting = true
        )

        assertTrue("After channel swap, Right channel should be louder than Left channel", dsp.lastRmsRightDb > dsp.lastRmsLeftDb + 20f)
    }

    @Test
    fun dspProcessor_individualBrightnessReverbAndEchoControlsWork() {
        val dsp = DspProcessor(48000)
        val cfg = AudioConfig(
            channelMode = ChannelMode.MONO,
            gainDb = 0f,
            noiseGateEnabled = false,
            highPassEnabled = false,
            autoNotchFeedback = false,
            brightEnabled = true,
            brightLevel = 0.85f,
            reverbEnabled = true,
            reverbLevel = 0.60f,
            reverbRoomSize = ReverbRoomSize.AUDITORIUM,
            echoEnabled = true,
            echoLevel = 0.50f,
            echoDelayMs = 120
        )
        dsp.updateConfig(cfg, 48000)

        val frames = 2048
        val inputMono = FloatArray(frames) { n ->
            if (n < 512) (sin(2.0 * PI * 1000.0 * n / 48000.0) * 0.35).toFloat() else 0f
        }
        val outputStereo = FloatArray(frames * 2)

        dsp.processBlock(
            inputBuffer = inputMono,
            outputStereoBuffer = outputStereo,
            numFrames = frames,
            inputChannels = 1,
            isTransmitting = true
        )

        // Because reverb tail spreads into the second half of the block where dry input is 0, tail energy must be non-zero
        var tailEnergy = 0f
        for (n in 1024 until frames) {
            tailEnergy += abs(outputStereo[n * 2]) + abs(outputStereo[n * 2 + 1])
        }
        assertTrue("Reverb and Echo tail should produce audible energy after dry signal stops", tailEnergy > 0.05f)
    }

    @Test
    fun dspProcessor_autoNotchFeedbackDestroyerDetectsResonantHowl() {
        val dsp = DspProcessor(48000)
        val cfg = AudioConfig(
            autoNotchFeedback = true,
            noiseGateEnabled = false
        )
        dsp.updateConfig(cfg, 48000)

        val frames = 1024
        // Simulate a narrow-band 2000 Hz acoustic feedback howl
        val howlInput = FloatArray(frames) { n ->
            (sin(2.0 * PI * 2000.0 * n / 48000.0) * 0.45).toFloat()
        }
        val outputStereo = FloatArray(frames * 2)

        dsp.processBlock(
            inputBuffer = howlInput,
            outputStereoBuffer = outputStereo,
            numFrames = frames,
            inputChannels = 1,
            isTransmitting = true
        )

        assertNotNull("Auto-Notch should detect 2000Hz feedback resonance", dsp.detectedFeedbackHz)
        assertEquals(2000f, dsp.detectedFeedbackHz ?: 0f, 10f)
        assertTrue("At least 1 dynamic notch filter should be engaged", dsp.activeNotchCount >= 1)

        dsp.clearFeedbackNotches()
        assertEquals(0, dsp.activeNotchCount)
        assertNull(dsp.detectedFeedbackHz)
    }

    @Test
    fun dspProcessor_internalSpeakerSafetyLimiterCapsOutputAtMinus6dbfs() {
        val dsp = DspProcessor(48000)
        dsp.updateConfig(AudioConfig(gainDb = 18.0f, limiterEnabled = true, noiseGateEnabled = false), 48000)

        val frames = 1500
        val loudInput = FloatArray(frames) { n ->
            (sin(2.0 * PI * 500.0 * n / 48000.0) * 0.8f).toFloat()
        }
        val outputStereo = FloatArray(frames * 2)

        dsp.processBlock(
            inputBuffer = loudInput,
            outputStereoBuffer = outputStereo,
            numFrames = frames,
            inputChannels = 1,
            isTransmitting = true,
            isInternalSpeakerActive = true
        )

        val maxSample = outputStereo.maxOf { abs(it) }
        assertTrue("Internal speaker safety ceiling must keep peak <= 0.51 (-6 dBFS)", maxSample <= 0.51f)
    }
}
