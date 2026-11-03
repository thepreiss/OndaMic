package com.ondamic.app.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MicrophoneInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DeviceConnectionKind {
    BUILTIN_MIC_BOTTOM,
    BUILTIN_MIC_TOP_OR_BACK,
    BUILTIN_SPEAKER,
    WIRED_P2_AUX,
    BLUETOOTH_A2DP,
    BLUETOOTH_SCO_OR_BLE,
    USB_AUDIO,
    OTHER
}

data class AudioEndpointItem(
    val id: Int,
    val productName: String,
    val deviceType: Int,
    val isInput: Boolean,
    val kind: DeviceConnectionKind,
    val channelCounts: List<Int>,
    val isExternalSafeOutput: Boolean
)

class AudioDeviceManager(
    context: Context,
    private val onExternalOutputDisconnected: () -> Unit
) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _inputDevices = MutableStateFlow<List<AudioEndpointItem>>(emptyList())
    val inputDevices: StateFlow<List<AudioEndpointItem>> = _inputDevices.asStateFlow()

    private val _outputDevices = MutableStateFlow<List<AudioEndpointItem>>(emptyList())
    val outputDevices: StateFlow<List<AudioEndpointItem>> = _outputDevices.asStateFlow()

    private val _hasExternalOutputConnected = MutableStateFlow(false)
    val hasExternalOutputConnected: StateFlow<Boolean> = _hasExternalOutputConnected.asStateFlow()

    private var previousExternalConnected = false

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            refreshDevices()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            val hadExternal = previousExternalConnected
            refreshDevices()
            if (hadExternal && !_hasExternalOutputConnected.value) {
                onExternalOutputDisconnected()
            }
        }
    }

    init {
        refreshDevices()
        audioManager.registerAudioDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))
    }

    fun release() {
        runCatching {
            audioManager.unregisterAudioDeviceCallback(deviceCallback)
        }
    }

    fun refreshDevices() {
        val rawInputs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        val micDescriptions = runCatching { audioManager.microphones }.getOrDefault(emptyList())

        var builtInMicIndex = 0
        val mappedInputs = rawInputs
            .filter { it.isSource && isSupportedInputType(it.type) }
            .map { info ->
                val kind = classifyInputKind(info, builtInMicIndex, micDescriptions)
                if (info.type == AudioDeviceInfo.TYPE_BUILTIN_MIC) {
                    builtInMicIndex++
                }
                AudioEndpointItem(
                    id = info.id,
                    productName = info.productName?.toString().orEmpty(),
                    deviceType = info.type,
                    isInput = true,
                    kind = kind,
                    channelCounts = info.channelCounts.toList(),
                    isExternalSafeOutput = false
                )
            }

        val rawOutputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val mappedOutputs = rawOutputs
            .filter { it.isSink && isSupportedOutputType(it.type) }
            .map { info ->
                val kind = classifyOutputKind(info)
                AudioEndpointItem(
                    id = info.id,
                    productName = info.productName?.toString().orEmpty(),
                    deviceType = info.type,
                    isInput = false,
                    kind = kind,
                    channelCounts = info.channelCounts.toList(),
                    isExternalSafeOutput = kind != DeviceConnectionKind.BUILTIN_SPEAKER
                )
            }
            .sortedByDescending { it.isExternalSafeOutput }

        _inputDevices.value = mappedInputs
        _outputDevices.value = mappedOutputs

        val externalAvailable = mappedOutputs.any { it.isExternalSafeOutput }
        previousExternalConnected = externalAvailable
        _hasExternalOutputConnected.value = externalAvailable
    }

    fun findRawInputDevice(deviceId: Int?): AudioDeviceInfo? {
        if (deviceId == null) return null
        return audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).firstOrNull { it.id == deviceId }
    }

    fun findRawOutputDevice(deviceId: Int?): AudioDeviceInfo? {
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        if (deviceId != null) {
            outputs.firstOrNull { it.id == deviceId }?.let { return it }
        }
        // Default priority: Wired P2 Aux -> USB Audio -> Bluetooth A2DP -> Bluetooth BLE -> Builtin Speaker
        return outputs.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_LINE_ANALOG
        } ?: outputs.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_USB_ACCESSORY
        } ?: outputs.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER) ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && it.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
        } ?: outputs.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        }
    }

    fun isOutputInternalSpeaker(preferredOutputId: Int?): Boolean {
        val target = findRawOutputDevice(preferredOutputId) ?: return true
        return target.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ||
            target.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
    }

    private fun isSupportedInputType(type: Int): Boolean {
        return type == AudioDeviceInfo.TYPE_BUILTIN_MIC ||
            type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            type == AudioDeviceInfo.TYPE_USB_DEVICE ||
            type == AudioDeviceInfo.TYPE_USB_HEADSET ||
            type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && type == AudioDeviceInfo.TYPE_BLE_HEADSET)
    }

    private fun isSupportedOutputType(type: Int): Boolean {
        return type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            type == AudioDeviceInfo.TYPE_LINE_ANALOG ||
            type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            type == AudioDeviceInfo.TYPE_USB_DEVICE ||
            type == AudioDeviceInfo.TYPE_USB_HEADSET ||
            type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                (type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER))
    }

    private fun classifyInputKind(
        info: AudioDeviceInfo,
        builtInIndex: Int,
        micList: List<MicrophoneInfo>
    ): DeviceConnectionKind {
        return when (info.type) {
            AudioDeviceInfo.TYPE_BUILTIN_MIC -> {
                val matchedMic = micList.firstOrNull { it.id == info.id }
                if (matchedMic != null && matchedMic.position.y > 0.02f) {
                    DeviceConnectionKind.BUILTIN_MIC_TOP_OR_BACK
                } else if (builtInIndex == 0) {
                    DeviceConnectionKind.BUILTIN_MIC_BOTTOM
                } else {
                    DeviceConnectionKind.BUILTIN_MIC_TOP_OR_BACK
                }
            }
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> DeviceConnectionKind.WIRED_P2_AUX
            AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> DeviceConnectionKind.USB_AUDIO
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> DeviceConnectionKind.BLUETOOTH_SCO_OR_BLE
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.type == AudioDeviceInfo.TYPE_BLE_HEADSET) {
                DeviceConnectionKind.BLUETOOTH_SCO_OR_BLE
            } else {
                DeviceConnectionKind.OTHER
            }
        }
    }

    private fun classifyOutputKind(info: AudioDeviceInfo): DeviceConnectionKind {
        return when (info.type) {
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_LINE_ANALOG -> DeviceConnectionKind.WIRED_P2_AUX
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> DeviceConnectionKind.BLUETOOTH_A2DP
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> DeviceConnectionKind.BLUETOOTH_SCO_OR_BLE
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_ACCESSORY -> DeviceConnectionKind.USB_AUDIO
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> DeviceConnectionKind.BUILTIN_SPEAKER
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                (info.type == AudioDeviceInfo.TYPE_BLE_SPEAKER || info.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
            ) {
                DeviceConnectionKind.BLUETOOTH_SCO_OR_BLE
            } else {
                DeviceConnectionKind.OTHER
            }
        }
    }
}
