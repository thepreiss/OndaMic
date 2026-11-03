package com.ondamic.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.ondamic.app.audio.PresetProfile
import com.ondamic.app.ui.MainStageScreen
import com.ondamic.app.ui.MainViewModel
import com.ondamic.app.ui.components.InternalSpeakerWarningDialog
import com.ondamic.app.ui.components.LanguagePickerSheet
import com.ondamic.app.ui.components.SaveCustomPresetDialog
import com.ondamic.app.ui.components.SavedRecordingsSheet
import com.ondamic.app.ui.theme.OndaMicTheme

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleLaunchShortcut(intent)

        setContent {
            val config by viewModel.config.collectAsState()
            val telemetry by viewModel.telemetry.collectAsState()
            val inputDevices by viewModel.inputDevices.collectAsState()
            val outputDevices by viewModel.outputDevices.collectAsState()
            val restrictions by viewModel.activeRestrictions.collectAsState()
            val customPresets by viewModel.customPresets.collectAsState()
            val showSpeakerWarning by viewModel.showSpeakerWarningDialog.collectAsState()
            val recordings by viewModel.savedRecordings.collectAsState()

            var hasMicPermission by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { result ->
                hasMicPermission = result[Manifest.permission.RECORD_AUDIO] == true ||
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                viewModel.refreshHardwareDevices()
            }

            LaunchedEffect(config.keepScreenOn) {
                if (config.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            var showLanguageSheet by remember { mutableStateOf(false) }
            var showRecordingsSheet by remember { mutableStateOf(false) }
            var showSavePresetDialog by remember { mutableStateOf(false) }

            OndaMicTheme(stageDimmer = config.stageDimmerMode) {
                MainStageScreen(
                    config = config,
                    telemetry = telemetry,
                    inputDevices = inputDevices,
                    outputDevices = outputDevices,
                    restrictions = restrictions,
                    customPresets = customPresets,
                    hasMicPermission = hasMicPermission,
                    onRequestMicPermission = {
                        val perms = buildList {
                            add(Manifest.permission.RECORD_AUDIO)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                add(Manifest.permission.BLUETOOTH_CONNECT)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                        permissionLauncher.launch(perms.toTypedArray())
                    },
                    onToggleAmplify = {
                        if (!hasMicPermission) {
                            permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                        } else {
                            viewModel.onToggleAmplificationClicked()
                        }
                    },
                    onPttHoldChanged = { held ->
                        if (!hasMicPermission && held) {
                            permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                        } else {
                            viewModel.onPushToTalkStateChanged(held)
                        }
                    },
                    onToggleCoughMute = viewModel::toggleCoughMute,
                    onPlayChime = {
                        if (hasMicPermission) viewModel.playAnnouncementChime()
                    },
                    onOpenLanguagePicker = { showLanguageSheet = true },
                    onOpenRecordings = {
                        viewModel.refreshRecordingsList()
                        showRecordingsSheet = true
                    },
                    onOpenSavePreset = { showSavePresetDialog = true },
                    onToggleStageDimmer = viewModel::toggleStageDimmer,
                    onSelectPreset = viewModel::selectPreset,
                    onSelectCustomPreset = viewModel::applyCustomPreset,
                    onDeleteCustomPreset = viewModel::deleteCustomPreset,
                    onChannelModeChanged = viewModel::setChannelMode,
                    onProcessingModeChanged = viewModel::setProcessingMode,
                    onTriggerModeChanged = viewModel::setTriggerMode,
                    onInputDeviceSelected = viewModel::setPreferredInputDevice,
                    onOutputDeviceSelected = viewModel::setPreferredOutputDevice,
                    onMicDirectionChanged = viewModel::setMicDirection,
                    onGainChanged = viewModel::setGainDb,
                    onStereoBalanceChanged = viewModel::setStereoBalance,
                    onSwapStereoChanged = viewModel::setSwapStereoChannels,
                    onBrightEnabledChanged = viewModel::setBrightEnabled,
                    onBrightLevelChanged = viewModel::setBrightLevel,
                    onReverbEnabledChanged = viewModel::setReverbEnabled,
                    onReverbLevelChanged = viewModel::setReverbLevel,
                    onReverbRoomChanged = viewModel::setReverbRoomSize,
                    onEchoEnabledChanged = viewModel::setEchoEnabled,
                    onEchoLevelChanged = viewModel::setEchoLevel,
                    onEchoDelayChanged = viewModel::setEchoDelayMs,
                    onEqBandChanged = viewModel::setEqBand,
                    onResetEqFlat = viewModel::resetEqFlat,
                    onAutoNotchChanged = viewModel::setAutoNotchFeedback,
                    onClearNotches = viewModel::clearActiveNotches,
                    onPhaseInvertChanged = viewModel::setPhaseInvert,
                    onHighPassHzChanged = viewModel::setHighPassHz,
                    onNoiseGateDbChanged = viewModel::setNoiseGateThresholdDb,
                    onVoxDbChanged = viewModel::setVoxThresholdDb,
                    onLimiterChanged = viewModel::setLimiterEnabled,
                    onVolumeKeyPttChanged = viewModel::setVolumeKeyPtt,
                    onStageDelay3sChanged = viewModel::setStageDelay3s,
                    onAutoDuckMusicChanged = viewModel::setAutoDuckMusic,
                    onKillSwitchChanged = viewModel::setDisconnectKillSwitch,
                    onParallelWavChanged = viewModel::setParallelWavRecord
                )

                if (showSpeakerWarning) {
                    InternalSpeakerWarningDialog(
                        onConfirm = viewModel::confirmInternalSpeakerTest,
                        onDismiss = viewModel::dismissSpeakerWarningDialog
                    )
                }

                if (showLanguageSheet) {
                    LanguagePickerSheet(
                        onDismiss = { showLanguageSheet = false },
                        onSelectLanguage = viewModel::changeAppLanguage
                    )
                }

                if (showSavePresetDialog) {
                    SaveCustomPresetDialog(
                        onSave = viewModel::saveCurrentAsCustomPreset,
                        onDismiss = { showSavePresetDialog = false }
                    )
                }

                if (showRecordingsSheet) {
                    SavedRecordingsSheet(
                        recordings = recordings,
                        onShare = { file ->
                            viewModel.createShareRecordingIntent(file)?.let { shareIntent ->
                                startActivity(Intent.createChooser(shareIntent, file.name))
                            }
                        },
                        onDelete = viewModel::deleteRecording,
                        onDismiss = { showRecordingsSheet = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLaunchShortcut(intent)
    }

    private fun handleLaunchShortcut(intent: Intent?) {
        when (intent?.getStringExtra("shortcut_preset")) {
            "CALL_FOCUS" -> viewModel.selectPreset(PresetProfile.CALL_FOCUS)
            "SPEECH" -> viewModel.selectPreset(PresetProfile.SPEECH)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val cfg = viewModel.config.value
        if (cfg.volumeKeyPtt && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            if (event?.repeatCount == 0) {
                viewModel.onPushToTalkStateChanged(true)
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val cfg = viewModel.config.value
        if (cfg.volumeKeyPtt && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            viewModel.onPushToTalkStateChanged(false)
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
