package com.ondamic.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ondamic.app.audio.AudioConfig
import com.ondamic.app.audio.ChannelMode
import com.ondamic.app.audio.CustomUserPreset
import com.ondamic.app.audio.MicDirectionMode
import com.ondamic.app.audio.PresetProfile
import com.ondamic.app.audio.ProcessingMode
import com.ondamic.app.audio.ReverbRoomSize
import com.ondamic.app.audio.TriggerMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ondamic_settings")

class OndaMicPreferencesRepository(private val context: Context) {

    private object Keys {
        val PRESET_PROFILE = stringPreferencesKey("preset_profile")
        val CHANNEL_MODE = stringPreferencesKey("channel_mode")
        val PROCESSING_MODE = stringPreferencesKey("processing_mode")
        val TRIGGER_MODE = stringPreferencesKey("trigger_mode")
        val MIC_DIRECTION = stringPreferencesKey("mic_direction")
        val MIC_FIELD_DIM = floatPreferencesKey("mic_field_dim")
        val GAIN_DB = floatPreferencesKey("gain_db")
        val STEREO_BALANCE = floatPreferencesKey("stereo_balance")
        val SWAP_STEREO = booleanPreferencesKey("swap_stereo")
        val PHASE_INVERT = booleanPreferencesKey("phase_invert")
        val AUTO_NOTCH = booleanPreferencesKey("auto_notch")
        val HIGH_PASS_ENABLED = booleanPreferencesKey("high_pass_enabled")
        val HIGH_PASS_HZ = floatPreferencesKey("high_pass_hz")
        val NOISE_GATE_ENABLED = booleanPreferencesKey("noise_gate_enabled")
        val NOISE_GATE_DB = floatPreferencesKey("noise_gate_db")
        val VOX_DB = floatPreferencesKey("vox_db")
        val LIMITER_ENABLED = booleanPreferencesKey("limiter_enabled")
        val EQ_BAND_0 = floatPreferencesKey("eq_band_0")
        val EQ_BAND_1 = floatPreferencesKey("eq_band_1")
        val EQ_BAND_2 = floatPreferencesKey("eq_band_2")
        val EQ_BAND_3 = floatPreferencesKey("eq_band_3")
        val EQ_BAND_4 = floatPreferencesKey("eq_band_4")
        val BRIGHT_ENABLED = booleanPreferencesKey("bright_enabled")
        val BRIGHT_LEVEL = floatPreferencesKey("bright_level")
        val REVERB_ENABLED = booleanPreferencesKey("reverb_enabled")
        val REVERB_LEVEL = floatPreferencesKey("reverb_level")
        val REVERB_ROOM = stringPreferencesKey("reverb_room")
        val ECHO_ENABLED = booleanPreferencesKey("echo_enabled")
        val ECHO_LEVEL = floatPreferencesKey("echo_level")
        val ECHO_DELAY_MS = intPreferencesKey("echo_delay_ms")
        val VOLUME_KEY_PTT = booleanPreferencesKey("volume_key_ptt")
        val STAGE_DELAY_3S = booleanPreferencesKey("stage_delay_3s")
        val AUTO_DUCK_MUSIC = booleanPreferencesKey("auto_duck_music")
        val PARALLEL_WAV = booleanPreferencesKey("parallel_wav")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val STAGE_DIMMER = booleanPreferencesKey("stage_dimmer")
        val DISCONNECT_KILL_SWITCH = booleanPreferencesKey("disconnect_kill_switch")
        val CUSTOM_PRESETS_JSON = stringPreferencesKey("custom_presets_json")
    }

    val audioConfigFlow: Flow<AudioConfig> = context.dataStore.data.map { prefs ->
        val def = AudioConfig()
        AudioConfig(
            presetProfile = runCatching {
                PresetProfile.valueOf(prefs[Keys.PRESET_PROFILE] ?: def.presetProfile.name)
            }.getOrDefault(def.presetProfile),
            channelMode = runCatching {
                ChannelMode.valueOf(prefs[Keys.CHANNEL_MODE] ?: def.channelMode.name)
            }.getOrDefault(def.channelMode),
            processingMode = runCatching {
                ProcessingMode.valueOf(prefs[Keys.PROCESSING_MODE] ?: def.processingMode.name)
            }.getOrDefault(def.processingMode),
            triggerMode = runCatching {
                TriggerMode.valueOf(prefs[Keys.TRIGGER_MODE] ?: def.triggerMode.name)
            }.getOrDefault(def.triggerMode),
            micDirection = runCatching {
                MicDirectionMode.valueOf(prefs[Keys.MIC_DIRECTION] ?: def.micDirection.name)
            }.getOrDefault(def.micDirection),
            micFieldDimension = prefs[Keys.MIC_FIELD_DIM] ?: def.micFieldDimension,
            gainDb = prefs[Keys.GAIN_DB] ?: def.gainDb,
            stereoBalance = prefs[Keys.STEREO_BALANCE] ?: def.stereoBalance,
            swapStereoChannels = prefs[Keys.SWAP_STEREO] ?: def.swapStereoChannels,
            phaseInvert = prefs[Keys.PHASE_INVERT] ?: def.phaseInvert,
            autoNotchFeedback = prefs[Keys.AUTO_NOTCH] ?: def.autoNotchFeedback,
            highPassEnabled = prefs[Keys.HIGH_PASS_ENABLED] ?: def.highPassEnabled,
            highPassHz = prefs[Keys.HIGH_PASS_HZ] ?: def.highPassHz,
            noiseGateEnabled = prefs[Keys.NOISE_GATE_ENABLED] ?: def.noiseGateEnabled,
            noiseGateThresholdDb = prefs[Keys.NOISE_GATE_DB] ?: def.noiseGateThresholdDb,
            voxThresholdDb = prefs[Keys.VOX_DB] ?: def.voxThresholdDb,
            limiterEnabled = prefs[Keys.LIMITER_ENABLED] ?: def.limiterEnabled,
            eqBandsDb = listOf(
                prefs[Keys.EQ_BAND_0] ?: def.eqBandsDb[0],
                prefs[Keys.EQ_BAND_1] ?: def.eqBandsDb[1],
                prefs[Keys.EQ_BAND_2] ?: def.eqBandsDb[2],
                prefs[Keys.EQ_BAND_3] ?: def.eqBandsDb[3],
                prefs[Keys.EQ_BAND_4] ?: def.eqBandsDb[4]
            ),
            brightEnabled = prefs[Keys.BRIGHT_ENABLED] ?: def.brightEnabled,
            brightLevel = prefs[Keys.BRIGHT_LEVEL] ?: def.brightLevel,
            reverbEnabled = prefs[Keys.REVERB_ENABLED] ?: def.reverbEnabled,
            reverbLevel = prefs[Keys.REVERB_LEVEL] ?: def.reverbLevel,
            reverbRoomSize = runCatching {
                ReverbRoomSize.valueOf(prefs[Keys.REVERB_ROOM] ?: def.reverbRoomSize.name)
            }.getOrDefault(def.reverbRoomSize),
            echoEnabled = prefs[Keys.ECHO_ENABLED] ?: def.echoEnabled,
            echoLevel = prefs[Keys.ECHO_LEVEL] ?: def.echoLevel,
            echoDelayMs = prefs[Keys.ECHO_DELAY_MS] ?: def.echoDelayMs,
            volumeKeyPtt = prefs[Keys.VOLUME_KEY_PTT] ?: def.volumeKeyPtt,
            stageDelay3s = prefs[Keys.STAGE_DELAY_3S] ?: def.stageDelay3s,
            autoDuckMusic = prefs[Keys.AUTO_DUCK_MUSIC] ?: def.autoDuckMusic,
            parallelWavRecord = prefs[Keys.PARALLEL_WAV] ?: def.parallelWavRecord,
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: def.keepScreenOn,
            stageDimmerMode = prefs[Keys.STAGE_DIMMER] ?: def.stageDimmerMode,
            disconnectKillSwitch = prefs[Keys.DISCONNECT_KILL_SWITCH] ?: def.disconnectKillSwitch
        ).enforceCompatibility()
    }

    val customPresetsFlow: Flow<List<CustomUserPreset>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.CUSTOM_PRESETS_JSON].orEmpty()
        parseCustomPresetsJson(raw)
    }

    suspend fun saveAudioConfig(config: AudioConfig) {
        val compat = config.enforceCompatibility()
        context.dataStore.edit { prefs ->
            prefs[Keys.PRESET_PROFILE] = compat.presetProfile.name
            prefs[Keys.CHANNEL_MODE] = compat.channelMode.name
            prefs[Keys.PROCESSING_MODE] = compat.processingMode.name
            prefs[Keys.TRIGGER_MODE] = compat.triggerMode.name
            prefs[Keys.MIC_DIRECTION] = compat.micDirection.name
            prefs[Keys.MIC_FIELD_DIM] = compat.micFieldDimension
            prefs[Keys.GAIN_DB] = compat.gainDb
            prefs[Keys.STEREO_BALANCE] = compat.stereoBalance
            prefs[Keys.SWAP_STEREO] = compat.swapStereoChannels
            prefs[Keys.PHASE_INVERT] = compat.phaseInvert
            prefs[Keys.AUTO_NOTCH] = compat.autoNotchFeedback
            prefs[Keys.HIGH_PASS_ENABLED] = compat.highPassEnabled
            prefs[Keys.HIGH_PASS_HZ] = compat.highPassHz
            prefs[Keys.NOISE_GATE_ENABLED] = compat.noiseGateEnabled
            prefs[Keys.NOISE_GATE_DB] = compat.noiseGateThresholdDb
            prefs[Keys.VOX_DB] = compat.voxThresholdDb
            prefs[Keys.LIMITER_ENABLED] = compat.limiterEnabled
            prefs[Keys.EQ_BAND_0] = compat.eqBandsDb.getOrElse(0) { 0f }
            prefs[Keys.EQ_BAND_1] = compat.eqBandsDb.getOrElse(1) { 0f }
            prefs[Keys.EQ_BAND_2] = compat.eqBandsDb.getOrElse(2) { 0f }
            prefs[Keys.EQ_BAND_3] = compat.eqBandsDb.getOrElse(3) { 0f }
            prefs[Keys.EQ_BAND_4] = compat.eqBandsDb.getOrElse(4) { 0f }
            prefs[Keys.BRIGHT_ENABLED] = compat.brightEnabled
            prefs[Keys.BRIGHT_LEVEL] = compat.brightLevel
            prefs[Keys.REVERB_ENABLED] = compat.reverbEnabled
            prefs[Keys.REVERB_LEVEL] = compat.reverbLevel
            prefs[Keys.REVERB_ROOM] = compat.reverbRoomSize.name
            prefs[Keys.ECHO_ENABLED] = compat.echoEnabled
            prefs[Keys.ECHO_LEVEL] = compat.echoLevel
            prefs[Keys.ECHO_DELAY_MS] = compat.echoDelayMs
            prefs[Keys.VOLUME_KEY_PTT] = compat.volumeKeyPtt
            prefs[Keys.STAGE_DELAY_3S] = compat.stageDelay3s
            prefs[Keys.AUTO_DUCK_MUSIC] = compat.autoDuckMusic
            prefs[Keys.PARALLEL_WAV] = compat.parallelWavRecord
            prefs[Keys.KEEP_SCREEN_ON] = compat.keepScreenOn
            prefs[Keys.STAGE_DIMMER] = compat.stageDimmerMode
            prefs[Keys.DISCONNECT_KILL_SWITCH] = compat.disconnectKillSwitch
        }
    }

    suspend fun saveCustomPreset(name: String, config: AudioConfig) {
        context.dataStore.edit { prefs ->
            val existing = parseCustomPresetsJson(prefs[Keys.CUSTOM_PRESETS_JSON].orEmpty()).toMutableList()
            val preset = CustomUserPreset(
                id = "preset_${System.currentTimeMillis()}",
                name = name.trim().take(32).ifEmpty { "Preset" },
                config = config.copy(presetProfile = PresetProfile.CUSTOM).enforceCompatibility()
            )
            existing.add(0, preset)
            prefs[Keys.CUSTOM_PRESETS_JSON] = serializeCustomPresetsJson(existing.take(15))
        }
    }

    suspend fun deleteCustomPreset(id: String) {
        context.dataStore.edit { prefs ->
            val filtered = parseCustomPresetsJson(prefs[Keys.CUSTOM_PRESETS_JSON].orEmpty())
                .filterNot { it.id == id }
            prefs[Keys.CUSTOM_PRESETS_JSON] = serializeCustomPresetsJson(filtered)
        }
    }

    private fun parseCustomPresetsJson(raw: String): List<CustomUserPreset> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val eqArr = obj.optJSONArray("eq")
                    val eqList = if (eqArr != null && eqArr.length() == 5) {
                        List(5) { idx -> eqArr.optDouble(idx, 0.0).toFloat() }
                    } else {
                        listOf(0f, 0f, 1.5f, 2f, 0.5f)
                    }
                    val cfg = AudioConfig(
                        presetProfile = PresetProfile.CUSTOM,
                        channelMode = runCatching { ChannelMode.valueOf(obj.optString("channelMode", "MONO")) }.getOrDefault(ChannelMode.MONO),
                        processingMode = runCatching { ProcessingMode.valueOf(obj.optString("processingMode", "LOW_LATENCY_STUDIO")) }.getOrDefault(ProcessingMode.LOW_LATENCY_STUDIO),
                        gainDb = obj.optDouble("gainDb", 4.0).toFloat(),
                        stereoBalance = obj.optDouble("stereoBalance", 0.0).toFloat(),
                        swapStereoChannels = obj.optBoolean("swapStereo", false),
                        phaseInvert = obj.optBoolean("phaseInvert", false),
                        autoNotchFeedback = obj.optBoolean("autoNotch", true),
                        highPassEnabled = obj.optBoolean("highPassEnabled", true),
                        highPassHz = obj.optDouble("highPassHz", 100.0).toFloat(),
                        noiseGateEnabled = obj.optBoolean("noiseGateEnabled", true),
                        noiseGateThresholdDb = obj.optDouble("noiseGateDb", -48.0).toFloat(),
                        eqBandsDb = eqList,
                        brightEnabled = obj.optBoolean("brightEnabled", true),
                        brightLevel = obj.optDouble("brightLevel", 0.35).toFloat(),
                        reverbEnabled = obj.optBoolean("reverbEnabled", false),
                        reverbLevel = obj.optDouble("reverbLevel", 0.28).toFloat(),
                        echoEnabled = obj.optBoolean("echoEnabled", false),
                        echoLevel = obj.optDouble("echoLevel", 0.25).toFloat(),
                        echoDelayMs = obj.optInt("echoDelayMs", 180)
                    ).enforceCompatibility()
                    add(
                        CustomUserPreset(
                            id = obj.optString("id", "p_$i"),
                            name = obj.optString("name", "Preset ${i + 1}"),
                            config = cfg
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun serializeCustomPresetsJson(presets: List<CustomUserPreset>): String {
        val arr = JSONArray()
        for (p in presets) {
            val c = p.config
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("channelMode", c.channelMode.name)
                put("processingMode", c.processingMode.name)
                put("gainDb", c.gainDb.toDouble())
                put("stereoBalance", c.stereoBalance.toDouble())
                put("swapStereo", c.swapStereoChannels)
                put("phaseInvert", c.phaseInvert)
                put("autoNotch", c.autoNotchFeedback)
                put("highPassEnabled", c.highPassEnabled)
                put("highPassHz", c.highPassHz.toDouble())
                put("noiseGateEnabled", c.noiseGateEnabled)
                put("noiseGateDb", c.noiseGateThresholdDb.toDouble())
                put("brightEnabled", c.brightEnabled)
                put("brightLevel", c.brightLevel.toDouble())
                put("reverbEnabled", c.reverbEnabled)
                put("reverbLevel", c.reverbLevel.toDouble())
                put("echoEnabled", c.echoEnabled)
                put("echoLevel", c.echoLevel.toDouble())
                put("echoDelayMs", c.echoDelayMs)
                put("eq", JSONArray(c.eqBandsDb))
            }
            arr.put(obj)
        }
        return arr.toString()
    }
}
