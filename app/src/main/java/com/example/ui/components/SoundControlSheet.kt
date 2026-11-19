package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.EqPreset
import com.example.model.EqualizerState
import com.example.model.SoundControlSettings
import com.example.model.SoundEffectType
import com.example.ui.icons.MikaIcons
import com.example.ui.theme.LocalMikaColors
import com.example.util.Formatters
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundControlSheet(
    currentSpeed: Float,
    skipSilence: Boolean,
    soundControl: SoundControlSettings,
    equalizerState: EqualizerState,
    onUpdateSoundControl: (SoundControlSettings, Float?) -> Unit,
    onToggleSkipSilence: (Boolean) -> Unit,
    onEqToggle: (Boolean) -> Unit,
    onEqSelectPreset: (EqPreset) -> Unit,
    onEqBandChange: (Int, Int) -> Unit,
    onEqBassBoostChange: (Int) -> Unit,
    onEqVirtualizerChange: (Int) -> Unit,
    onEqReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val mikaColors = LocalMikaColors.current
    val accent = MaterialTheme.colorScheme.primary
    val secondary = mikaColors.accentSecondary
    var eqCurveMode by remember { mutableStateOf(false) }
    var eqExpanded by remember { mutableStateOf(false) }
    var effectsExpanded by remember { mutableStateOf(true) }

    val speedPresets = remember { listOf(1.0f, 1.2f, 1.3f, 1.4f, 1.5f, 0.8f) }

    fun round2(v: Float): Float = round(v * 100f) / 100f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = mikaColors.surfaceElevated.copy(alpha = 0.96f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 26.dp)
                .testTag("sound_control_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accent.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                MikaIcons.SoundControl,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text("Sound Control", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format(Locale.US, "%.2fx · %.2f st · Vol %.0f%%", currentSpeed, soundControl.pitchSemitones, soundControl.playerVolumePct),
                            style = MaterialTheme.typography.labelMedium,
                            color = accent
                        )
                    }
                }
                TextButton(
                    onClick = {
                        onUpdateSoundControl(SoundControlSettings(), 1.0f)
                        onToggleSkipSilence(false)
                        onEqReset()
                    },
                    modifier = Modifier.testTag("sound_control_reset_all")
                ) {
                    Icon(MikaIcons.Refresh, contentDescription = "Reset all sound controls", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reset", fontWeight = FontWeight.Bold)
                }
            }

            // Speed presets + Skip Silence strip
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(speedPresets) { presetSpeed ->
                    val selected = abs(currentSpeed - presetSpeed) < 0.015f
                    FilterChip(
                        selected = selected,
                        onClick = { onUpdateSoundControl(soundControl, presetSpeed) },
                        label = {
                            Text(
                                text = if (presetSpeed == 1.0f) "Default" else "${presetSpeed}x",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent.copy(alpha = 0.22f),
                            selectedLabelColor = accent
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = skipSilence,
                        onClick = { onToggleSkipSilence(!skipSilence) },
                        leadingIcon = {
                            Icon(
                                MikaIcons.ForwardSkip,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Skip Silence", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("sound_control_skip_silence")
                    )
                }
            }

            // Pitch (Semitones) Card with 432Hz toggle
            val is432Hz = abs(soundControl.pitchSemitones - (-0.3177f)) < 0.02f
            SoundGlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pitch (Semitones)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = String.format(Locale.US, "%+.2f st", soundControl.pitchSemitones),
                                style = MaterialTheme.typography.labelLarge,
                                color = accent
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = is432Hz,
                                onClick = {
                                    val nextPitch = if (is432Hz) 0f else -0.3177f
                                    onUpdateSoundControl(soundControl.copy(pitchSemitones = nextPitch, linkPitchToSpeed = false), null)
                                },
                                label = { Text("432Hz", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("sound_control_432hz")
                            )
                            IconButton(
                                onClick = {
                                    onUpdateSoundControl(soundControl.copy(pitchSemitones = 0f), null)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(MikaIcons.Refresh, contentDescription = "Reset pitch", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NudgeCircleButton(icon = MikaIcons.ArrowLeft, label = "Lower pitch") {
                            val next = (soundControl.pitchSemitones - 0.5f).coerceIn(-12f, 12f)
                            onUpdateSoundControl(soundControl.copy(pitchSemitones = next, linkPitchToSpeed = false), null)
                        }
                        MikaSleekSlider(
                            value = soundControl.pitchSemitones,
                            onValueChange = {
                                onUpdateSoundControl(soundControl.copy(pitchSemitones = round2(it), linkPitchToSpeed = false), null)
                            },
                            valueRange = -12f..12f,
                            accentColor = accent,
                            secondaryColor = secondary,
                            trackHeight = 6.dp,
                            thumbRadius = 8.dp,
                            modifier = Modifier.weight(1f)
                        )
                        NudgeCircleButton(icon = MikaIcons.ArrowRight, label = "Raise pitch") {
                            val next = (soundControl.pitchSemitones + 0.5f).coerceIn(-12f, 12f)
                            onUpdateSoundControl(soundControl.copy(pitchSemitones = next, linkPitchToSpeed = false), null)
                        }
                    }
                }
            }

            // Speed Card with Pitch Link
            SoundGlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Speed", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = String.format(Locale.US, "%.2fx", currentSpeed),
                                style = MaterialTheme.typography.labelLarge,
                                color = accent
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = soundControl.linkPitchToSpeed,
                                onClick = {
                                    onUpdateSoundControl(
                                        soundControl.copy(linkPitchToSpeed = !soundControl.linkPitchToSpeed),
                                        currentSpeed
                                    )
                                },
                                leadingIcon = {
                                    Icon(MikaIcons.Link, contentDescription = null, modifier = Modifier.size(15.dp))
                                },
                                label = { Text("Pitch", fontWeight = FontWeight.Bold) }
                            )
                            IconButton(
                                onClick = {
                                    onUpdateSoundControl(soundControl.copy(linkPitchToSpeed = false), 1.0f)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(MikaIcons.Refresh, contentDescription = "Reset speed", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NudgeCircleButton(icon = MikaIcons.ArrowLeft, label = "Slower") {
                            val next = round2((currentSpeed - 0.05f).coerceIn(0.25f, 3.0f))
                            onUpdateSoundControl(soundControl, next)
                        }
                        MikaSleekSlider(
                            value = currentSpeed,
                            onValueChange = { onUpdateSoundControl(soundControl, round2(it.coerceIn(0.25f, 3.0f))) },
                            valueRange = 0.25f..3.0f,
                            accentColor = accent,
                            secondaryColor = secondary,
                            trackHeight = 6.dp,
                            thumbRadius = 8.dp,
                            modifier = Modifier.weight(1f)
                        )
                        NudgeCircleButton(icon = MikaIcons.ArrowRight, label = "Faster") {
                            val next = round2((currentSpeed + 0.05f).coerceIn(0.25f, 3.0f))
                            onUpdateSoundControl(soundControl, next)
                        }
                    }
                }
            }

            // Player Volume Card
            SoundGlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(MikaIcons.VolumeHigh, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                            Text("Volume", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${soundControl.playerVolumePct.toInt()}%", style = MaterialTheme.typography.labelLarge, color = accent)
                            IconButton(
                                onClick = { onUpdateSoundControl(soundControl.copy(playerVolumePct = 100f), null) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(MikaIcons.Refresh, contentDescription = "Reset volume", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    MikaSleekSlider(
                        value = soundControl.playerVolumePct,
                        onValueChange = { onUpdateSoundControl(soundControl.copy(playerVolumePct = it.coerceIn(0f, 100f)), null) },
                        valueRange = 0f..100f,
                        accentColor = accent,
                        secondaryColor = secondary,
                        trackHeight = 6.dp,
                        thumbRadius = 8.dp
                    )
                }
            }

            // Loudness Enhancer (PreAmp) Card
            SoundGlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Loudness Enhancer (PreAmp)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = String.format(Locale.US, "+%.1f dB", soundControl.preAmpDb),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (soundControl.loudnessEnhancerEnabled) accent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    onUpdateSoundControl(
                                        soundControl.copy(loudnessEnhancerEnabled = false, preAmpDb = 0f),
                                        null
                                    )
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(MikaIcons.Refresh, contentDescription = "Reset preamp", modifier = Modifier.size(17.dp))
                            }
                            Switch(
                                checked = soundControl.loudnessEnhancerEnabled,
                                onCheckedChange = {
                                    val defaultDb = if (it && soundControl.preAmpDb < 0.2f) 3.5f else soundControl.preAmpDb
                                    onUpdateSoundControl(
                                        soundControl.copy(loudnessEnhancerEnabled = it, preAmpDb = defaultDb),
                                        null
                                    )
                                }
                            )
                        }
                    }
                    MikaSleekSlider(
                        value = soundControl.preAmpDb,
                        onValueChange = {
                            onUpdateSoundControl(
                                soundControl.copy(loudnessEnhancerEnabled = true, preAmpDb = it.coerceIn(0f, 15f)),
                                null
                            )
                        },
                        valueRange = 0f..15f,
                        accentColor = accent,
                        secondaryColor = secondary,
                        trackHeight = 6.dp,
                        thumbRadius = 8.dp
                    )
                }
            }

            // Equalizer Interactive Card
            SoundGlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(MikaIcons.Equalizer, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                            Column {
                                Text("Equalizer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (equalizerState.enabled) equalizerState.preset.label else "Off",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (equalizerState.enabled) accent else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = eqCurveMode,
                                onClick = { eqCurveMode = !eqCurveMode },
                                label = { Text(if (eqCurveMode) "Curve" else "Bars", fontWeight = FontWeight.Bold) }
                            )
                            IconButton(onClick = { eqExpanded = !eqExpanded }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = if (eqExpanded) MikaIcons.ArrowUp else MikaIcons.ArrowDown,
                                    contentDescription = "Expand equalizer",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = onEqReset, modifier = Modifier.size(36.dp)) {
                                Icon(MikaIcons.Refresh, contentDescription = "Reset EQ", modifier = Modifier.size(18.dp))
                            }
                            Switch(checked = equalizerState.enabled, onCheckedChange = onEqToggle)
                        }
                    }

                    // Mini preview graph
                    EqMiniGraph(
                        bandLevelsMb = equalizerState.bandLevelsMb,
                        minMb = equalizerState.minBandLevelMb,
                        maxMb = equalizerState.maxBandLevelMb,
                        curveMode = eqCurveMode,
                        enabled = equalizerState.enabled,
                        accent = accent,
                        secondary = secondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { eqExpanded = !eqExpanded }
                    )

                    AnimatedVisibility(visible = eqExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(EqPreset.entries) { preset ->
                                    FilterChip(
                                        selected = equalizerState.preset == preset,
                                        onClick = { onEqSelectPreset(preset) },
                                        label = { Text(preset.label) }
                                    )
                                }
                            }
                            equalizerState.bandLevelsMb.forEachIndexed { idx, lvl ->
                                val hz = equalizerState.bandFrequenciesHz.getOrElse(idx) { 1000 }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = Formatters.formatFrequencyHz(hz),
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.width(52.dp)
                                    )
                                    MikaSleekSlider(
                                        value = lvl.toFloat(),
                                        onValueChange = { onEqBandChange(idx, it.toInt()) },
                                        valueRange = equalizerState.minBandLevelMb.toFloat()..equalizerState.maxBandLevelMb.toFloat(),
                                        accentColor = accent,
                                        secondaryColor = secondary,
                                        trackHeight = 5.dp,
                                        thumbRadius = 7.dp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%+.0fdB", lvl / 100f),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accent,
                                        modifier = Modifier.width(44.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Mono Audio Card
            SoundGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mono audio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Combine left & right stereo channels",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (soundControl.monoAudio) {
                            IconButton(
                                onClick = { onUpdateSoundControl(soundControl.copy(monoAudio = false), null) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(MikaIcons.Refresh, contentDescription = "Reset mono audio", modifier = Modifier.size(17.dp))
                            }
                        }
                        Switch(
                            checked = soundControl.monoAudio,
                            onCheckedChange = { onUpdateSoundControl(soundControl.copy(monoAudio = it), null) }
                        )
                    }
                }
            }

            // Collapsible Sound Effects Grid
            SoundGlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { effectsExpanded = !effectsExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(MikaIcons.AudioSquare, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                            Text("Sound effects", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Icon(
                            imageVector = if (effectsExpanded) MikaIcons.ArrowUp else MikaIcons.ArrowDown,
                            contentDescription = "Toggle sound effects",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(visible = effectsExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            HorizontalDivider(color = mikaColors.glassBorder)
                            SoundEffectType.entries.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    pair.forEach { effect ->
                                        val isActive = when (effect) {
                                            SoundEffectType.BASS_ENHANCER -> equalizerState.enabled && equalizerState.bassBoostStrength > 0
                                            SoundEffectType.VIRTUAL_SURROUND -> equalizerState.enabled && equalizerState.virtualizerStrength > 0
                                            SoundEffectType.REVERB -> soundControl.reverbEnabled
                                            SoundEffectType.EIGHT_D -> soundControl.eightDEnabled
                                            SoundEffectType.INSTRUMENTAL -> soundControl.instrumentalEnabled
                                            SoundEffectType.COMPRESSOR -> soundControl.compressorEnabled
                                            SoundEffectType.CROSSFEED -> soundControl.crossfeedEnabled
                                            SoundEffectType.ECHO -> soundControl.echoEnabled
                                            SoundEffectType.TUBE_WARMTH -> soundControl.tubeWarmthEnabled
                                            SoundEffectType.CHORUS -> soundControl.chorusEnabled
                                        }
                                        Surface(
                                            onClick = {
                                                when (effect) {
                                                    SoundEffectType.BASS_ENHANCER -> onEqBassBoostChange(if (isActive) 0 else 650)
                                                    SoundEffectType.VIRTUAL_SURROUND -> onEqVirtualizerChange(if (isActive) 0 else 650)
                                                    SoundEffectType.REVERB -> onUpdateSoundControl(soundControl.copy(reverbEnabled = !isActive), null)
                                                    SoundEffectType.EIGHT_D -> onUpdateSoundControl(soundControl.copy(eightDEnabled = !isActive), null)
                                                    SoundEffectType.INSTRUMENTAL -> onUpdateSoundControl(soundControl.copy(instrumentalEnabled = !isActive), null)
                                                    SoundEffectType.COMPRESSOR -> onUpdateSoundControl(soundControl.copy(compressorEnabled = !isActive), null)
                                                    SoundEffectType.CROSSFEED -> onUpdateSoundControl(soundControl.copy(crossfeedEnabled = !isActive), null)
                                                    SoundEffectType.ECHO -> onUpdateSoundControl(soundControl.copy(echoEnabled = !isActive), null)
                                                    SoundEffectType.TUBE_WARMTH -> onUpdateSoundControl(soundControl.copy(tubeWarmthEnabled = !isActive), null)
                                                    SoundEffectType.CHORUS -> onUpdateSoundControl(soundControl.copy(chorusEnabled = !isActive), null)
                                                }
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isActive) accent.copy(alpha = 0.22f) else mikaColors.glassSurface,
                                            border = BorderStroke(1.dp, if (isActive) accent.copy(alpha = 0.65f) else mikaColors.glassBorder),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("sound_effect_${effect.name.lowercase()}")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = effect.label,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isActive) accent else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(9.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isActive) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                                                )
                                            }
                                        }
                                    }
                                    if (pair.size == 1) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundGlassCard(content: @Composable () -> Unit) {
    val mikaColors = LocalMikaColors.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = mikaColors.glassSurface,
        border = BorderStroke(1.dp, mikaColors.glassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(14.dp)) {
            content()
        }
    }
}

@Composable
private fun NudgeCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val mikaColors = LocalMikaColors.current
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = mikaColors.glassSurface,
        border = BorderStroke(1.dp, mikaColors.glassBorder),
        modifier = Modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun EqMiniGraph(
    bandLevelsMb: List<Int>,
    minMb: Int,
    maxMb: Int,
    curveMode: Boolean,
    enabled: Boolean,
    accent: Color,
    secondary: Color,
    modifier: Modifier = Modifier
) {
    val mikaColors = LocalMikaColors.current
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 12.dp, vertical = 8.dp)) {
            val count = bandLevelsMb.size.coerceAtLeast(1)
            val range = (maxMb - minMb).coerceAtLeast(1).toFloat()
            val col = if (enabled) accent else Color.Gray.copy(alpha = 0.5f)
            val col2 = if (enabled) secondary else Color.Gray.copy(alpha = 0.35f)
            val midY = size.height / 2f
            drawLine(
                color = mikaColors.glassBorder,
                start = Offset(0f, midY),
                end = Offset(size.width, midY),
                strokeWidth = 1.dp.toPx()
            )
            if (curveMode) {
                val path = Path()
                val pts = bandLevelsMb.mapIndexed { idx, lvl ->
                    val x = if (count == 1) size.width / 2f else (idx.toFloat() / (count - 1)) * size.width
                    val norm = ((lvl - minMb) / range).coerceIn(0f, 1f)
                    val y = size.height * (1f - norm)
                    Offset(x, y)
                }
                pts.forEachIndexed { idx, pt ->
                    if (idx == 0) path.moveTo(pt.x, pt.y)
                    else {
                        val prev = pts[idx - 1]
                        val cx = (prev.x + pt.x) / 2f
                        path.cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                    }
                }
                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(listOf(col, col2)),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
                pts.forEach { pt ->
                    drawCircle(color = col, radius = 4.dp.toPx(), center = pt)
                }
            } else {
                val slot = size.width / count
                val barW = (slot * 0.42f).coerceIn(6.dp.toPx(), 18.dp.toPx())
                bandLevelsMb.forEachIndexed { idx, lvl ->
                    val norm = ((lvl - minMb) / range).coerceIn(0f, 1f)
                    val y = size.height * (1f - norm)
                    val top = minOf(y, midY)
                    val h = abs(y - midY).coerceAtLeast(3.dp.toPx())
                    val x = idx * slot + (slot - barW) / 2f
                    drawRoundRect(
                        color = col,
                        topLeft = Offset(x, top),
                        size = androidx.compose.ui.geometry.Size(barW, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2f, barW / 2f)
                    )
                }
            }
        }
    }
}
