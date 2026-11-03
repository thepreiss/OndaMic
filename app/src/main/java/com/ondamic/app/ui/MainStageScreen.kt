package com.ondamic.app.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ondamic.app.R
import com.ondamic.app.audio.AudioConfig
import com.ondamic.app.audio.AudioEndpointItem
import com.ondamic.app.audio.AudioTelemetry
import com.ondamic.app.audio.ChannelMode
import com.ondamic.app.audio.CompatibilityRestriction
import com.ondamic.app.audio.CustomUserPreset
import com.ondamic.app.audio.DeviceConnectionKind
import com.ondamic.app.audio.MicDirectionMode
import com.ondamic.app.audio.PresetProfile
import com.ondamic.app.audio.ProcessingMode
import com.ondamic.app.audio.ReverbRoomSize
import com.ondamic.app.audio.TriggerMode
import com.ondamic.app.ui.theme.AmberGuard
import com.ondamic.app.ui.theme.BorderSubtle
import com.ondamic.app.ui.theme.OnAirCoral
import com.ondamic.app.ui.theme.OndaCyan
import com.ondamic.app.ui.theme.OndaEmerald
import com.ondamic.app.ui.theme.SurfaceCardElevated
import java.util.Locale
import kotlin.math.abs

@Composable
fun MainStageScreen(
    config: AudioConfig,
    telemetry: AudioTelemetry,
    inputDevices: List<AudioEndpointItem>,
    outputDevices: List<AudioEndpointItem>,
    restrictions: Set<CompatibilityRestriction>,
    customPresets: List<CustomUserPreset>,
    hasMicPermission: Boolean,
    onRequestMicPermission: () -> Unit,
    onToggleAmplify: () -> Unit,
    onPttHoldChanged: (Boolean) -> Unit,
    onToggleCoughMute: () -> Unit,
    onPlayChime: () -> Unit,
    onOpenLanguagePicker: () -> Unit,
    onOpenRecordings: () -> Unit,
    onOpenSavePreset: () -> Unit,
    onToggleStageDimmer: () -> Unit,
    onSelectPreset: (PresetProfile) -> Unit,
    onSelectCustomPreset: (CustomUserPreset) -> Unit,
    onDeleteCustomPreset: (String) -> Unit,
    onChannelModeChanged: (ChannelMode) -> Unit,
    onProcessingModeChanged: (ProcessingMode) -> Unit,
    onTriggerModeChanged: (TriggerMode) -> Unit,
    onInputDeviceSelected: (Int?) -> Unit,
    onOutputDeviceSelected: (Int?) -> Unit,
    onMicDirectionChanged: (MicDirectionMode) -> Unit,
    onGainChanged: (Float) -> Unit,
    onStereoBalanceChanged: (Float) -> Unit,
    onSwapStereoChanged: (Boolean) -> Unit,
    onBrightEnabledChanged: (Boolean) -> Unit,
    onBrightLevelChanged: (Float) -> Unit,
    onReverbEnabledChanged: (Boolean) -> Unit,
    onReverbLevelChanged: (Float) -> Unit,
    onReverbRoomChanged: (ReverbRoomSize) -> Unit,
    onEchoEnabledChanged: (Boolean) -> Unit,
    onEchoLevelChanged: (Float) -> Unit,
    onEchoDelayChanged: (Int) -> Unit,
    onEqBandChanged: (Int, Float) -> Unit,
    onResetEqFlat: () -> Unit,
    onAutoNotchChanged: (Boolean) -> Unit,
    onClearNotches: () -> Unit,
    onPhaseInvertChanged: (Boolean) -> Unit,
    onHighPassHzChanged: (Float) -> Unit,
    onNoiseGateDbChanged: (Float) -> Unit,
    onVoxDbChanged: (Float) -> Unit,
    onLimiterChanged: (Boolean) -> Unit,
    onVolumeKeyPttChanged: (Boolean) -> Unit,
    onStageDelay3sChanged: (Boolean) -> Unit,
    onAutoDuckMusicChanged: (Boolean) -> Unit,
    onKillSwitchChanged: (Boolean) -> Unit,
    onParallelWavChanged: (Boolean) -> Unit
) {
    val view = LocalView.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // 1. Header Bar with Brand & Quick Utility Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = OndaCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceCardElevated)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.latency_badge, telemetry.estimatedLatencyMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = OndaEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPlayChime) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = stringResource(R.string.btn_chime),
                            tint = AmberGuard
                        )
                    }
                    IconButton(onClick = onOpenRecordings) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = stringResource(R.string.btn_recordings),
                            tint = if (telemetry.isRecordingWav) OnAirCoral else OndaEmerald
                        )
                    }
                    IconButton(onClick = onToggleStageDimmer) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = stringResource(R.string.btn_stage_dimmer),
                            tint = if (config.stageDimmerMode) AmberGuard else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onOpenLanguagePicker) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = stringResource(R.string.btn_language),
                            tint = OndaCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 100% Offline & Hardware DSP Status Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BadgePill(text = "🔒 ${stringResource(R.string.status_offline_badge)}", color = OndaEmerald)
                BadgePill(
                    text = if (config.channelMode == ChannelMode.STEREO_DUAL_MIC) "🎧 STEREO DUAL-MIC (L+R)" else "🎙️ MONO (L=R)",
                    color = OndaCyan
                )
                if (telemetry.hardwareAecActive || telemetry.hardwareNsActive) {
                    BadgePill(text = "✨ HW VOICE FOCUS / AEC", color = AmberGuard)
                }
                if (telemetry.isRecordingWav) {
                    BadgePill(text = "🔴 REC WAV (${telemetry.currentRecordingDurationSec}s)", color = OnAirCoral)
                }
            }

            // Auto-Mute Disconnect Alert Banner
            AnimatedVisibility(visible = telemetry.autoMutedByDisconnect) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF331800))
                ) {
                    Text(
                        text = stringResource(R.string.disconnect_alert_banner),
                        color = AmberGuard,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Permission Banner if not yet granted
            if (!hasMicPermission) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCardElevated)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.permission_mic_rationale),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onRequestMicPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = OndaCyan)
                        ) {
                            Text(
                                text = stringResource(R.string.permission_grant_btn),
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Central ON-AIR Stage Button + Real-Time Acoustic Wave & Dual L/R VU Meters
            StageHeroControlCard(
                config = config,
                telemetry = telemetry,
                onToggleAmplify = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onToggleAmplify()
                },
                onPttHoldChanged = { held ->
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onPttHoldChanged(held)
                },
                onToggleCoughMute = {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onToggleCoughMute()
                },
                onTriggerModeChanged = onTriggerModeChanged
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Smart Voice Profiles Carousel + Custom Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.presets_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                AssistChip(
                    onClick = onOpenSavePreset,
                    label = { Text(stringResource(R.string.btn_save_preset), fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.BookmarkAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            val isCallFocusBlockedByStereo = restrictions.contains(CompatibilityRestriction.STEREO_DISABLES_CALL_MODE)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val builtInProfiles = listOf(
                    PresetProfile.SPEECH to stringResource(R.string.preset_speech),
                    PresetProfile.CALL_FOCUS to stringResource(R.string.preset_call_focus),
                    PresetProfile.ANTI_FEEDBACK to stringResource(R.string.preset_anti_feedback),
                    PresetProfile.KARAOKE to stringResource(R.string.preset_karaoke),
                    PresetProfile.MEGAPHONE to stringResource(R.string.preset_megaphone)
                )

                builtInProfiles.forEach { (preset, label) ->
                    val isBlocked = preset == PresetProfile.CALL_FOCUS && isCallFocusBlockedByStereo
                    val selected = config.presetProfile == preset
                    FilterChip(
                        selected = selected,
                        enabled = !isBlocked,
                        onClick = { onSelectPreset(preset) },
                        label = { Text(label) },
                        leadingIcon = if (isBlocked) {
                            { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(15.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OndaCyan.copy(alpha = 0.22f),
                            selectedLabelColor = OndaCyan
                        )
                    )
                }

                customPresets.forEach { custom ->
                    FilterChip(
                        selected = false,
                        onClick = { onSelectCustomPreset(custom) },
                        label = { Text("⭐ ${custom.name}") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Configuration Tabs (1. Channels & Mics | 2. Gain, EQ & FX | 3. Anti-Feedback & Stage)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = OndaCyan,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.tab_routing), fontSize = 12.sp, maxLines = 1) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.tab_effects), fontSize = 12.sp, maxLines = 1) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(stringResource(R.string.tab_antifeedback), fontSize = 12.sp, maxLines = 1) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> TabChannelsAndMicrophones(
                    config = config,
                    inputDevices = inputDevices,
                    outputDevices = outputDevices,
                    restrictions = restrictions,
                    onChannelModeChanged = onChannelModeChanged,
                    onProcessingModeChanged = onProcessingModeChanged,
                    onInputDeviceSelected = onInputDeviceSelected,
                    onOutputDeviceSelected = onOutputDeviceSelected,
                    onMicDirectionChanged = onMicDirectionChanged,
                    onStereoBalanceChanged = onStereoBalanceChanged,
                    onSwapStereoChanged = onSwapStereoChanged
                )
                1 -> TabGainEqAndAdjustableEffects(
                    config = config,
                    onGainChanged = onGainChanged,
                    onBrightEnabledChanged = onBrightEnabledChanged,
                    onBrightLevelChanged = onBrightLevelChanged,
                    onReverbEnabledChanged = onReverbEnabledChanged,
                    onReverbLevelChanged = onReverbLevelChanged,
                    onReverbRoomChanged = onReverbRoomChanged,
                    onEchoEnabledChanged = onEchoEnabledChanged,
                    onEchoLevelChanged = onEchoLevelChanged,
                    onEchoDelayChanged = onEchoDelayChanged,
                    onEqBandChanged = onEqBandChanged,
                    onResetEqFlat = onResetEqFlat
                )
                2 -> TabAntiFeedbackAndStageTools(
                    config = config,
                    telemetry = telemetry,
                    onAutoNotchChanged = onAutoNotchChanged,
                    onClearNotches = onClearNotches,
                    onPhaseInvertChanged = onPhaseInvertChanged,
                    onHighPassHzChanged = onHighPassHzChanged,
                    onNoiseGateDbChanged = onNoiseGateDbChanged,
                    onVoxDbChanged = onVoxDbChanged,
                    onLimiterChanged = onLimiterChanged,
                    onVolumeKeyPttChanged = onVolumeKeyPttChanged,
                    onStageDelay3sChanged = onStageDelay3sChanged,
                    onAutoDuckMusicChanged = onAutoDuckMusicChanged,
                    onKillSwitchChanged = onKillSwitchChanged,
                    onParallelWavChanged = onParallelWavChanged
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun StageHeroControlCard(
    config: AudioConfig,
    telemetry: AudioTelemetry,
    onToggleAmplify: () -> Unit,
    onPttHoldChanged: (Boolean) -> Unit,
    onToggleCoughMute: () -> Unit,
    onTriggerModeChanged: (TriggerMode) -> Unit
) {
    val isStereo = config.channelMode == ChannelMode.STEREO_DUAL_MIC
    val isActivelyTransmitting = telemetry.isLive && !telemetry.isMutedOrCough &&
        (config.triggerMode != TriggerMode.HOLD_PTT || telemetry.isPttPressed) &&
        (config.triggerMode != TriggerMode.VOX || telemetry.isVoxGateOpen)

    val statusText = when {
        telemetry.countdownSecondsLeft > 0 -> stringResource(R.string.status_countdown, telemetry.countdownSecondsLeft)
        !telemetry.isLive -> stringResource(R.string.status_standby)
        telemetry.isMutedOrCough -> stringResource(R.string.status_muted_cough)
        config.triggerMode == TriggerMode.VOX && !telemetry.isVoxGateOpen -> stringResource(R.string.status_vox_waiting)
        else -> stringResource(R.string.status_on_air)
    }

    val statusColor = when {
        telemetry.countdownSecondsLeft > 0 -> AmberGuard
        isActivelyTransmitting -> OnAirCoral
        telemetry.isLive -> AmberGuard
        else -> OndaCyan
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, statusColor.copy(alpha = 0.55f), RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(statusColor.copy(alpha = 0.16f))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    text = statusText,
                    color = statusColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Central Interactive Stage Orb + Live Waveform Ring
            val isPttMode = config.triggerMode == TriggerMode.HOLD_PTT
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(164.dp)
                    .clip(CircleShape)
                    .pointerInput(isPttMode) {
                        if (isPttMode) {
                            detectTapGestures(
                                onPress = {
                                    onPttHoldChanged(true)
                                    tryAwaitRelease()
                                    onPttHoldChanged(false)
                                }
                            )
                        } else {
                            detectTapGestures(onTap = { onToggleAmplify() })
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val baseRadius = size.minDimension * 0.37f
                    val levelNormalizedL = ((telemetry.levelLeftDb + 75f) / 75f).coerceIn(0f, 1f)
                    val levelNormalizedR = ((telemetry.levelRightDb + 75f) / 75f).coerceIn(0f, 1f)
                    val avgLevel = (levelNormalizedL + levelNormalizedR) * 0.5f

                    // Outer reactive glow ring
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                statusColor.copy(alpha = 0.38f * avgLevel + 0.12f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.5f
                        ),
                        radius = size.minDimension * 0.5f
                    )

                    // Left / Right Stereo Arc Meters around the button
                    drawArc(
                        color = OndaCyan,
                        startAngle = 135f,
                        sweepAngle = 180f * levelNormalizedL,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx())
                    )
                    drawArc(
                        color = if (isStereo) OndaEmerald else OndaCyan,
                        startAngle = 45f,
                        sweepAngle = -180f * levelNormalizedR,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx())
                    )

                    // Core circle button
                    drawCircle(
                        color = if (isActivelyTransmitting) OnAirCoral.copy(alpha = 0.25f) else SurfaceCardElevated,
                        radius = baseRadius
                    )
                    drawCircle(
                        color = statusColor,
                        radius = baseRadius,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (telemetry.isMutedOrCough) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when {
                            isPttMode -> stringResource(R.string.btn_hold_ptt)
                            telemetry.isLive -> stringResource(R.string.btn_tap_to_stop)
                            else -> stringResource(R.string.btn_tap_to_start)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // VU Level Meters: Unified Mono or Dual Independent L / R Stereo Bars
            if (isStereo) {
                VuMeterBar(label = "L (Mic 1)", levelDb = telemetry.levelLeftDb, peakDb = telemetry.peakLeftDb, accent = OndaCyan)
                Spacer(modifier = Modifier.height(6.dp))
                VuMeterBar(label = "R (Mic 2)", levelDb = telemetry.levelRightDb, peakDb = telemetry.peakRightDb, accent = OndaEmerald)
            } else {
                VuMeterBar(label = "MONO VU", levelDb = telemetry.levelLeftDb, peakDb = telemetry.peakLeftDb, accent = OndaCyan)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Trigger Mode Selector + Cough/Mute Quick Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = config.triggerMode == TriggerMode.CONTINUOUS,
                        onClick = { onTriggerModeChanged(TriggerMode.CONTINUOUS) },
                        label = { Text(stringResource(R.string.trigger_continuous), fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = config.triggerMode == TriggerMode.HOLD_PTT,
                        onClick = { onTriggerModeChanged(TriggerMode.HOLD_PTT) },
                        label = { Text(stringResource(R.string.trigger_ptt), fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = config.triggerMode == TriggerMode.VOX,
                        onClick = { onTriggerModeChanged(TriggerMode.VOX) },
                        label = { Text(stringResource(R.string.trigger_vox), fontSize = 11.sp) }
                    )
                }

                if (telemetry.isLive && config.triggerMode != TriggerMode.HOLD_PTT) {
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onToggleCoughMute,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (telemetry.isMutedOrCough) AmberGuard else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = if (telemetry.isMutedOrCough) stringResource(R.string.btn_unmute)
                            else stringResource(R.string.btn_cough_mute),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VuMeterBar(
    label: String,
    levelDb: Float,
    peakDb: Float,
    accent: Color
) {
    val norm = ((levelDb + 72f) / 72f).coerceIn(0f, 1f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accent,
            modifier = Modifier.width(68.dp)
        )
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(SurfaceCardElevated)
        ) {
            val fillWidth = size.width * norm
            val barColor = when {
                levelDb > -3f -> OnAirCoral
                levelDb > -10f -> AmberGuard
                else -> accent
            }
            drawRoundRect(
                color = barColor,
                size = Size(fillWidth, size.height),
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = String.format(Locale.US, "%4.0f dB", peakDb.coerceIn(-90f, 0f)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun TabChannelsAndMicrophones(
    config: AudioConfig,
    inputDevices: List<AudioEndpointItem>,
    outputDevices: List<AudioEndpointItem>,
    restrictions: Set<CompatibilityRestriction>,
    onChannelModeChanged: (ChannelMode) -> Unit,
    onProcessingModeChanged: (ProcessingMode) -> Unit,
    onInputDeviceSelected: (Int?) -> Unit,
    onOutputDeviceSelected: (Int?) -> Unit,
    onMicDirectionChanged: (MicDirectionMode) -> Unit,
    onStereoBalanceChanged: (Float) -> Unit,
    onSwapStereoChanged: (Boolean) -> Unit
) {
    val isStereo = config.channelMode == ChannelMode.STEREO_DUAL_MIC
    val callBlocked = restrictions.contains(CompatibilityRestriction.STEREO_DISABLES_CALL_MODE)
    val singleMicBlocked = restrictions.contains(CompatibilityRestriction.STEREO_DISABLES_SINGLE_MIC_SELECTOR)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Mono vs Stereo Dual-Mic Card
        SectionCard(title = stringResource(R.string.channel_mode_title), icon = Icons.Default.Tune) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectableOptionRow(
                    title = stringResource(R.string.channel_mono),
                    selected = config.channelMode == ChannelMode.MONO,
                    enabled = true,
                    onClick = { onChannelModeChanged(ChannelMode.MONO) }
                )
                SelectableOptionRow(
                    title = stringResource(R.string.channel_stereo_dual),
                    selected = config.channelMode == ChannelMode.STEREO_DUAL_MIC,
                    enabled = true,
                    onClick = { onChannelModeChanged(ChannelMode.STEREO_DUAL_MIC) }
                )

                // Clear UI Warning & Explanation when Stereo Dual-Mic is active
                AnimatedVisibility(visible = isStereo) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF142738))
                            .border(1.dp, OndaCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.stereo_restriction_banner),
                            style = MaterialTheme.typography.bodySmall,
                            color = OndaCyan
                        )

                        val balanceText = when {
                            abs(config.stereoBalance) < 0.05f -> "Centro (L = R)"
                            config.stereoBalance < 0f -> "L +${(-config.stereoBalance * 100).toInt()}%"
                            else -> "R +${(config.stereoBalance * 100).toInt()}%"
                        }
                        Text(
                            text = stringResource(R.string.stereo_balance_label, balanceText),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = config.stereoBalance,
                            onValueChange = onStereoBalanceChanged,
                            valueRange = -1.0f..1.0f
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.stereo_swap_channels),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = config.swapStereoChannels,
                                onCheckedChange = onSwapStereoChanged
                            )
                        }
                    }
                }
            }
        }

        // 2. Audio Processing Pipeline (Call Voice Focus vs Low-Latency Studio)
        SectionCard(title = stringResource(R.string.processing_engine_title), icon = Icons.Default.Security) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectableOptionRow(
                    title = stringResource(R.string.processing_call_focus),
                    subtitle = if (callBlocked) stringResource(R.string.processing_disabled_by_stereo) else null,
                    selected = config.processingMode == ProcessingMode.CALL_VOICE_FOCUS && !callBlocked,
                    enabled = !callBlocked,
                    onClick = { onProcessingModeChanged(ProcessingMode.CALL_VOICE_FOCUS) }
                )
                SelectableOptionRow(
                    title = stringResource(R.string.processing_low_latency),
                    selected = config.processingMode == ProcessingMode.LOW_LATENCY_STUDIO || callBlocked,
                    enabled = true,
                    onClick = { onProcessingModeChanged(ProcessingMode.LOW_LATENCY_STUDIO) }
                )
            }
        }

        // 3. Physical Microphone Capsule Selector + Polar Beamforming
        SectionCard(title = stringResource(R.string.mic_source_title), icon = Icons.Default.Mic) {
            Column(
                modifier = Modifier.alpha(if (singleMicBlocked) 0.5f else 1.0f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (singleMicBlocked) {
                    Text(
                        text = "🔒 ${stringResource(R.string.mic_disabled_by_stereo)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberGuard,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                SelectableOptionRow(
                    title = stringResource(R.string.mic_auto_default),
                    selected = config.preferredInputDeviceId == null,
                    enabled = !singleMicBlocked,
                    onClick = { onInputDeviceSelected(null) }
                )

                inputDevices.forEach { dev ->
                    val friendlyLabel = when (dev.kind) {
                        DeviceConnectionKind.BUILTIN_MIC_BOTTOM -> stringResource(R.string.mic_bottom_main)
                        DeviceConnectionKind.BUILTIN_MIC_TOP_OR_BACK -> stringResource(R.string.mic_top_camcorder)
                        DeviceConnectionKind.WIRED_P2_AUX -> "${stringResource(R.string.mic_wired_headset)} (${dev.productName})"
                        DeviceConnectionKind.USB_AUDIO -> "${stringResource(R.string.mic_usb_external)} (${dev.productName})"
                        DeviceConnectionKind.BLUETOOTH_SCO_OR_BLE -> "${stringResource(R.string.mic_bt_wireless)} (${dev.productName})"
                        else -> dev.productName.ifBlank { "Microphone #${dev.id}" }
                    }
                    SelectableOptionRow(
                        title = friendlyLabel,
                        selected = config.preferredInputDeviceId == dev.id && !singleMicBlocked,
                        enabled = !singleMicBlocked,
                        onClick = { onInputDeviceSelected(dev.id) }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.mic_direction_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = config.micDirection == MicDirectionMode.FRONT,
                        onClick = { onMicDirectionChanged(MicDirectionMode.FRONT) },
                        label = { Text(stringResource(R.string.dir_front), fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = config.micDirection == MicDirectionMode.BACK,
                        onClick = { onMicDirectionChanged(MicDirectionMode.BACK) },
                        label = { Text(stringResource(R.string.dir_back), fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = config.micDirection == MicDirectionMode.UNSPECIFIED,
                        onClick = { onMicDirectionChanged(MicDirectionMode.UNSPECIFIED) },
                        label = { Text(stringResource(R.string.dir_omni), fontSize = 11.sp) }
                    )
                }
            }
        }

        // 4. Output Speaker Destination Selector
        SectionCard(title = stringResource(R.string.output_device_title), icon = Icons.Default.Speaker) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectableOptionRow(
                    title = stringResource(R.string.mic_auto_default),
                    selected = config.preferredOutputDeviceId == null,
                    enabled = true,
                    onClick = { onOutputDeviceSelected(null) }
                )
                outputDevices.forEach { out ->
                    val label = when (out.kind) {
                        DeviceConnectionKind.WIRED_P2_AUX -> "🔌 ${stringResource(R.string.out_wired_p2)}"
                        DeviceConnectionKind.BLUETOOTH_A2DP,
                        DeviceConnectionKind.BLUETOOTH_SCO_OR_BLE -> "🔵 ${stringResource(R.string.out_bluetooth_a2dp)} (${out.productName})"
                        DeviceConnectionKind.USB_AUDIO -> "🎛️ ${stringResource(R.string.out_usb_dac)} (${out.productName})"
                        DeviceConnectionKind.BUILTIN_SPEAKER -> "⚠️ ${stringResource(R.string.out_internal_speaker)}"
                        else -> out.productName.ifBlank { "Output #${out.id}" }
                    }
                    SelectableOptionRow(
                        title = label,
                        selected = config.preferredOutputDeviceId == out.id,
                        enabled = true,
                        onClick = { onOutputDeviceSelected(out.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabGainEqAndAdjustableEffects(
    config: AudioConfig,
    onGainChanged: (Float) -> Unit,
    onBrightEnabledChanged: (Boolean) -> Unit,
    onBrightLevelChanged: (Float) -> Unit,
    onReverbEnabledChanged: (Boolean) -> Unit,
    onReverbLevelChanged: (Float) -> Unit,
    onReverbRoomChanged: (ReverbRoomSize) -> Unit,
    onEchoEnabledChanged: (Boolean) -> Unit,
    onEchoLevelChanged: (Float) -> Unit,
    onEchoDelayChanged: (Int) -> Unit,
    onEqBandChanged: (Int, Float) -> Unit,
    onResetEqFlat: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Pre-Amp Voice Gain
        SectionCard(title = stringResource(R.string.gain_preamp_label, String.format(Locale.US, "%+.1f", config.gainDb)), icon = Icons.Default.GraphicEq) {
            Slider(
                value = config.gainDb,
                onValueChange = onGainChanged,
                valueRange = -12.0f..24.0f,
                colors = SliderDefaults.colors(thumbColor = OndaCyan, activeTrackColor = OndaCyan)
            )
        }

        // 2. Individual ON/OFF & Level (0..100%) Vocal Effects: Brightness, Reverb, Echo
        SectionCard(title = stringResource(R.string.fx_section_title), icon = Icons.Default.Tune) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Effect A: Vocal Brightness (High-Shelf 4.8kHz)
                EffectControlBlock(
                    title = stringResource(R.string.fx_bright_title),
                    enabled = config.brightEnabled,
                    onEnabledChanged = onBrightEnabledChanged,
                    levelLabel = stringResource(R.string.fx_bright_level, (config.brightLevel * 100).toInt()),
                    level = config.brightLevel,
                    onLevelChanged = onBrightLevelChanged
                )

                // Effect B: Acoustic Stage Reverb
                EffectControlBlock(
                    title = stringResource(R.string.fx_reverb_title),
                    enabled = config.reverbEnabled,
                    onEnabledChanged = onReverbEnabledChanged,
                    levelLabel = stringResource(R.string.fx_reverb_level, (config.reverbLevel * 100).toInt()),
                    level = config.reverbLevel,
                    onLevelChanged = onReverbLevelChanged
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = config.reverbRoomSize == ReverbRoomSize.STUDIO,
                            enabled = config.reverbEnabled,
                            onClick = { onReverbRoomChanged(ReverbRoomSize.STUDIO) },
                            label = { Text(stringResource(R.string.fx_reverb_room_studio), fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = config.reverbRoomSize == ReverbRoomSize.AUDITORIUM,
                            enabled = config.reverbEnabled,
                            onClick = { onReverbRoomChanged(ReverbRoomSize.AUDITORIUM) },
                            label = { Text(stringResource(R.string.fx_reverb_room_auditorium), fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = config.reverbRoomSize == ReverbRoomSize.ARENA,
                            enabled = config.reverbEnabled,
                            onClick = { onReverbRoomChanged(ReverbRoomSize.ARENA) },
                            label = { Text(stringResource(R.string.fx_reverb_room_arena), fontSize = 11.sp) }
                        )
                    }
                }

                // Effect C: Vocal Echo / Delay
                EffectControlBlock(
                    title = stringResource(R.string.fx_echo_title),
                    enabled = config.echoEnabled,
                    onEnabledChanged = onEchoEnabledChanged,
                    levelLabel = stringResource(R.string.fx_echo_level, (config.echoLevel * 100).toInt()),
                    level = config.echoLevel,
                    onLevelChanged = onEchoLevelChanged
                ) {
                    Text(
                        text = stringResource(R.string.fx_echo_delay_ms, config.echoDelayMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = config.echoDelayMs.toFloat(),
                        onValueChange = { onEchoDelayChanged(it.toInt()) },
                        enabled = config.echoEnabled,
                        valueRange = 80f..450f
                    )
                }
            }
        }

        // 3. 5-Band Parametric Voice Equalizer
        SectionCard(title = stringResource(R.string.eq_section_title), icon = Icons.Default.GraphicEq) {
            val bandLabels = listOf("100 Hz (Graves)", "350 Hz (Corpo)", "1 kHz (Voz)", "3.5 kHz (Clareza)", "8 kHz (Ar)")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 0..4) {
                    val db = config.eqBandsDb.getOrElse(i) { 0f }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = bandLabels[i], style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = String.format(Locale.US, "%+.1f dB", db),
                            style = MaterialTheme.typography.labelMedium,
                            color = OndaCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = db,
                        onValueChange = { onEqBandChanged(i, it) },
                        valueRange = -12.0f..12.0f
                    )
                }
                OutlinedButton(
                    onClick = onResetEqFlat,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.eq_reset_flat))
                }
            }
        }
    }
}

@Composable
private fun TabAntiFeedbackAndStageTools(
    config: AudioConfig,
    telemetry: AudioTelemetry,
    onAutoNotchChanged: (Boolean) -> Unit,
    onClearNotches: () -> Unit,
    onPhaseInvertChanged: (Boolean) -> Unit,
    onHighPassHzChanged: (Float) -> Unit,
    onNoiseGateDbChanged: (Float) -> Unit,
    onVoxDbChanged: (Float) -> Unit,
    onLimiterChanged: (Boolean) -> Unit,
    onVolumeKeyPttChanged: (Boolean) -> Unit,
    onStageDelay3sChanged: (Boolean) -> Unit,
    onAutoDuckMusicChanged: (Boolean) -> Unit,
    onKillSwitchChanged: (Boolean) -> Unit,
    onParallelWavChanged: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard(title = stringResource(R.string.af_section_title), icon = Icons.Default.Security) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SwitchSettingRow(
                    title = stringResource(R.string.af_auto_notch_title),
                    subtitle = stringResource(R.string.af_auto_notch_desc),
                    checked = config.autoNotchFeedback,
                    onCheckedChange = onAutoNotchChanged
                )
                if (telemetry.activeNotchCount > 0 || telemetry.feedbackDetectedHz != null) {
                    OutlinedButton(onClick = onClearNotches, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.af_reset_notches, telemetry.activeNotchCount))
                    }
                }

                SwitchSettingRow(
                    title = stringResource(R.string.af_phase_invert_title),
                    subtitle = stringResource(R.string.af_phase_invert_desc),
                    checked = config.phaseInvert,
                    onCheckedChange = onPhaseInvertChanged
                )

                Text(
                    text = stringResource(R.string.af_highpass_title, config.highPassHz.toInt()),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = config.highPassHz,
                    onValueChange = onHighPassHzChanged,
                    valueRange = 40f..220f
                )

                Text(
                    text = stringResource(R.string.af_noisegate_title, config.noiseGateThresholdDb.toInt()),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = config.noiseGateThresholdDb,
                    onValueChange = onNoiseGateDbChanged,
                    valueRange = -70f..-20f
                )

                Text(
                    text = stringResource(R.string.af_vox_threshold_title, config.voxThresholdDb.toInt()),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = config.voxThresholdDb,
                    onValueChange = onVoxDbChanged,
                    valueRange = -55f..-15f
                )

                SwitchSettingRow(
                    title = stringResource(R.string.af_limiter_title),
                    subtitle = null,
                    checked = config.limiterEnabled,
                    onCheckedChange = onLimiterChanged
                )
            }
        }

        SectionCard(title = stringResource(R.string.stage_tools_title), icon = Icons.Default.Tune) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SwitchSettingRow(
                    title = stringResource(R.string.stage_killswitch_title),
                    subtitle = stringResource(R.string.stage_killswitch_desc),
                    checked = config.disconnectKillSwitch,
                    onCheckedChange = onKillSwitchChanged
                )
                SwitchSettingRow(
                    title = stringResource(R.string.stage_volume_ptt_title),
                    subtitle = stringResource(R.string.stage_volume_ptt_desc),
                    checked = config.volumeKeyPtt,
                    onCheckedChange = onVolumeKeyPttChanged
                )
                SwitchSettingRow(
                    title = stringResource(R.string.stage_delay_3s_title),
                    subtitle = stringResource(R.string.stage_delay_3s_desc),
                    checked = config.stageDelay3s,
                    onCheckedChange = onStageDelay3sChanged
                )
                SwitchSettingRow(
                    title = stringResource(R.string.stage_autoduck_title),
                    subtitle = stringResource(R.string.stage_autoduck_desc),
                    checked = config.autoDuckMusic,
                    onCheckedChange = onAutoDuckMusicChanged
                )
                SwitchSettingRow(
                    title = stringResource(R.string.stage_wav_record_title),
                    subtitle = stringResource(R.string.stage_wav_record_desc),
                    checked = config.parallelWavRecord,
                    onCheckedChange = onParallelWavChanged
                )
            }
        }
    }
}

@Composable
private fun EffectControlBlock(
    title: String,
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    levelLabel: String,
    level: Float,
    onLevelChanged: (Float) -> Unit,
    extraContent: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCardElevated)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) OndaCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = enabled, onCheckedChange = onEnabledChanged)
        }
        Text(
            text = levelLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = level,
            onValueChange = onLevelChanged,
            enabled = enabled,
            valueRange = 0f..1f
        )
        extraContent?.invoke()
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = OndaCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SelectableOptionRow(
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) OndaCyan.copy(alpha = 0.16f) else SurfaceCardElevated)
            .border(
                width = 1.dp,
                color = if (selected) OndaCyan else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .alpha(if (enabled) 1f else 0.45f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) OndaCyan else MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AmberGuard
                )
            }
        }
    }
}

@Composable
private fun SwitchSettingRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun BadgePill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
