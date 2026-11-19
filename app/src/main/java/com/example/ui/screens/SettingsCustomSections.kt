package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import com.example.model.AppSettings
import com.example.model.ArtworkGestureAction
import com.example.model.BackgroundEffect
import com.example.model.CrossfadeStyle
import com.example.ui.components.WaveformRulerDial
import com.example.ui.icons.MikaIcons
import com.example.ui.theme.LocalMikaColors
import kotlin.math.roundToInt

@Composable
fun ColorAndThemeExtrasCard(
    settings: AppSettings,
    onToggleAutoColoring: (Boolean) -> Unit,
    onTogglePickColorsFromWallpaper: (Boolean) -> Unit,
    onToggleForceMiniPlayerTrackColors: (Boolean) -> Unit,
    onToggleUsePitchBlack: (Boolean) -> Unit,
    onOpenLightColorPicker: () -> Unit,
    onOpenDarkColorPicker: () -> Unit,
    onToggleApplyColorOnApp: (Boolean) -> Unit = {},
    onToggleApplyColorOnPlayer: (Boolean) -> Unit = {}
) {
    val lightColor = if (settings.customAccentArgb != 0L) Color(settings.customAccentArgb.toInt()) else MaterialTheme.colorScheme.primary
    val darkColor = if (settings.customDarkAccentArgb != 0L) Color(settings.customDarkAccentArgb.toInt()) else LocalMikaColors.current.accentSecondary

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            ExtrasSwitchRow(
                icon = MikaIcons.Palette,
                title = "Auto Coloring",
                subtitle = "Derive harmonic player shades from track artwork",
                checked = settings.autoColoring,
                onCheckedChange = onToggleAutoColoring,
                testTag = "auto_coloring_switch"
            )
            ExtrasSwitchRow(
                icon = MikaIcons.Brush,
                title = "Apply Theme Color to App",
                subtitle = "Tint navigation bars, buttons, and cards across screens",
                checked = settings.applyColorOnApp,
                onCheckedChange = onToggleApplyColorOnApp,
                testTag = "apply_color_app_switch"
            )
            ExtrasSwitchRow(
                icon = MikaIcons.Disc,
                title = "Apply Theme Color to Player",
                subtitle = "Tint Full Player, Mini-Player, and artwork glow",
                checked = settings.applyColorOnPlayer,
                onCheckedChange = onToggleApplyColorOnPlayer,
                testTag = "apply_color_player_switch"
            )
            ExtrasSwitchRow(
                icon = MikaIcons.SystemTheme,
                title = "Pick Colors from Device Wallpaper",
                subtitle = null,
                checked = settings.pickColorsFromWallpaper,
                onCheckedChange = onTogglePickColorsFromWallpaper,
                testTag = "wallpaper_colors_switch"
            )
            ExtrasSwitchRow(
                icon = MikaIcons.Disc,
                title = "Force miniplayer to follow track colors",
                subtitle = "Overrides Auto Coloring, Wallpaper & Default Color",
                checked = settings.forceMiniPlayerTrackColors,
                onCheckedChange = onToggleForceMiniPlayerTrackColors,
                testTag = "force_miniplayer_colors_switch"
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            ExtrasSwitchRow(
                icon = MikaIcons.Moon,
                title = "Use Pitch Black",
                subtitle = "Pure AMOLED black background in Dark Mode",
                checked = settings.usePitchBlack,
                onCheckedChange = onToggleUsePitchBlack,
                testTag = "pitch_black_switch"
            )
            ColorSwatchSettingRow(
                icon = MikaIcons.Sun,
                title = "Default Color",
                subtitle = "Base color used in Light Mode",
                swatchColor = lightColor,
                onClick = onOpenLightColorPicker,
                testTag = "default_light_color_row"
            )
            ColorSwatchSettingRow(
                icon = MikaIcons.Moon,
                title = "Default Color (Dark)",
                subtitle = "Base color used in Dark Mode",
                swatchColor = darkColor,
                onClick = onOpenDarkColorPicker,
                testTag = "default_dark_color_row"
            )
        }
    }
}

@Composable
fun VisualAndBackgroundEffectsCard(
    settings: AppSettings,
    onToggleBlur: (Boolean) -> Unit,
    onToggleGlow: (Boolean) -> Unit,
    onToggleParallax: (Boolean) -> Unit,
    onSelectBackgroundEffect: (BackgroundEffect) -> Unit,
    onOpenPlayerBackground: () -> Unit,
    onOpenWidgetSettings: () -> Unit
) {
    var showEffectMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            ExtrasSwitchRow(
                icon = MikaIcons.Cleaning,
                title = "Enable Blur Effect",
                subtitle = "Might affect performance",
                checked = settings.enableBlurEffect,
                onCheckedChange = onToggleBlur,
                testTag = "enable_blur_switch"
            )
            ExtrasSwitchRow(
                icon = MikaIcons.Sun,
                title = "Enable Glow Effect",
                subtitle = "Might affect performance",
                checked = settings.enableGlowEffect,
                onCheckedChange = onToggleGlow,
                testTag = "enable_glow_switch"
            )
            ExtrasSwitchRow(
                icon = MikaIcons.Fullscreen,
                title = "Enable Parallax Effect",
                subtitle = "Might affect performance",
                checked = settings.enableParallaxEffect,
                onCheckedChange = onToggleParallax,
                testTag = "enable_parallax_switch"
            )
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEffectMenu = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("background_effect_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(MikaIcons.Star, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Background effect", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Auto follows the season\nMight affect performance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = settings.backgroundEffect.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                DropdownMenu(
                    expanded = showEffectMenu,
                    onDismissRequest = { showEffectMenu = false }
                ) {
                    BackgroundEffect.entries.forEach { effect ->
                        DropdownMenuItem(
                            text = { Text(effect.label, fontWeight = if (effect == settings.backgroundEffect) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                showEffectMenu = false
                                onSelectBackgroundEffect(effect)
                            }
                        )
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenPlayerBackground)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("settings_player_bg_row"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(MikaIcons.Palette, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Player background", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${settings.playerBackgroundMode.label} · ${if (settings.playerBackgroundAnimated) "Animated" else "Static"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(MikaIcons.ArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenWidgetSettings)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("settings_widget_row"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(MikaIcons.HomeLinear, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Home Screen Widget", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Customize theme, layout, buttons & live preview",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(MikaIcons.ArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun ArtworkGesturesCollapsibleCard(
    settings: AppSettings,
    onToggleDoubleTapLyrics: (Boolean) -> Unit,
    onSelectTapAction: (ArtworkGestureAction) -> Unit,
    onSelectLongPressAction: (ArtworkGestureAction) -> Unit,
    onReset: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    var showTapMenu by remember { mutableStateOf(false) }
    var showLongPressMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(MikaIcons.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Artwork Gestures",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onReset, modifier = Modifier.size(36.dp)) {
                    Icon(MikaIcons.Restore, contentDescription = "Reset artwork gestures", modifier = Modifier.size(19.dp))
                }
                Icon(
                    imageVector = if (expanded) MikaIcons.ArrowUp else MikaIcons.ArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleDoubleTapLyrics(!settings.doubleTapToToggleLyrics) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(MikaIcons.Lyrics, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Double tap to toggle lyrics", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Switch(checked = settings.doubleTapToToggleLyrics, onCheckedChange = onToggleDoubleTapLyrics)
                    }

                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTapMenu = true }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(MikaIcons.Disc, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Tap Action", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Text(
                                text = settings.artworkTapAction.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        DropdownMenu(expanded = showTapMenu, onDismissRequest = { showTapMenu = false }) {
                            ArtworkGestureAction.entries.forEach { action ->
                                DropdownMenuItem(
                                    text = { Text(action.label) },
                                    onClick = {
                                        showTapMenu = false
                                        onSelectTapAction(action)
                                    }
                                )
                            }
                        }
                    }

                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLongPressMenu = true }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(MikaIcons.Shield, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Long-Press Action", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Text(
                                text = settings.artworkLongPressAction.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        DropdownMenu(expanded = showLongPressMenu, onDismissRequest = { showLongPressMenu = false }) {
                            ArtworkGestureAction.entries.forEach { action ->
                                DropdownMenuItem(
                                    text = { Text(action.label) },
                                    onClick = {
                                        showLongPressMenu = false
                                        onSelectLongPressAction(action)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaybackAudioAndQueueExtras(
    settings: AppSettings,
    onToggleSkipSilence: (Boolean) -> Unit,
    onToggleGapless: (Boolean) -> Unit,
    onToggleCrossfadeEnabled: (Boolean) -> Unit,
    onCrossfadeDurationMs: (Int) -> Unit,
    onCrossfadeTriggerSec: (Int) -> Unit,
    onCrossfadeStyle: (CrossfadeStyle) -> Unit,
    onTogglePlayPauseFade: (Boolean) -> Unit,
    onPlayFadeDurationMs: (Int) -> Unit,
    onPauseFadeDurationMs: (Int) -> Unit,
    onToggleJumpToFirst: (Boolean) -> Unit,
    onTogglePreviousReplays: (Boolean) -> Unit,
    onSkipIntervalSec: (Int) -> Unit,
    onRestoreMinDurationMin: (Int) -> Unit
) {
    var showStyleMenu by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Skip Silence + Gapless + Crossfade Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                ExtrasSwitchRow(
                    icon = MikaIcons.NextBold,
                    title = "Skip Silence",
                    subtitle = null,
                    checked = settings.skipSilence,
                    onCheckedChange = onToggleSkipSilence,
                    testTag = "skip_silence_switch"
                )
                ExtrasSwitchRow(
                    icon = MikaIcons.Disc,
                    title = "Gapless Playback",
                    subtitle = null,
                    checked = settings.gaplessPlayback,
                    onCheckedChange = onToggleGapless,
                    testTag = "gapless_playback_switch"
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                ExtrasSwitchRow(
                    icon = MikaIcons.PlaylistAdd,
                    title = "Enable Crossfade Effect",
                    subtitle = null,
                    checked = settings.crossfadeEnabled,
                    onCheckedChange = onToggleCrossfadeEnabled,
                    testTag = "crossfade_enabled_switch"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(start = 26.dp, end = 16.dp, bottom = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(1.5.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(MikaIcons.Disc, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("Crossfade duration", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                WaveformRulerDial(
                                    value = settings.crossfadeDurationMs,
                                    range = 200..12000,
                                    step = 100,
                                    valueLabel = "${settings.crossfadeDurationMs}ms",
                                    onValueChange = onCrossfadeDurationMs,
                                    testTag = "crossfade_duration_dial"
                                )
                            }
                            Slider(
                                value = settings.crossfadeDurationMs.toFloat(),
                                onValueChange = { raw ->
                                    val snapped = ((raw / 100f).roundToInt() * 100).coerceIn(200, 12000)
                                    onCrossfadeDurationMs(snapped)
                                },
                                valueRange = 200f..12000f
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(500 to "0.5s", 1000 to "1s", 2000 to "2s", 4000 to "4s", 8000 to "8s").forEach { (ms, label) ->
                                    FilterChip(
                                        selected = settings.crossfadeDurationMs == ms,
                                        onClick = { onCrossfadeDurationMs(ms) },
                                        label = { Text(label, fontWeight = FontWeight.SemiBold) }
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(MikaIcons.Clock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Trigger in last ${settings.crossfadeTriggerLastSec} seconds",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                WaveformRulerDial(
                                    value = settings.crossfadeTriggerLastSec,
                                    range = 1..15,
                                    step = 1,
                                    valueLabel = "${settings.crossfadeTriggerLastSec}s",
                                    onValueChange = onCrossfadeTriggerSec,
                                    testTag = "crossfade_trigger_dial"
                                )
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(2, 5, 8, 12).forEach { sec ->
                                    FilterChip(
                                        selected = settings.crossfadeTriggerLastSec == sec,
                                        onClick = { onCrossfadeTriggerSec(sec) },
                                        label = { Text("${sec}s", fontWeight = FontWeight.SemiBold) }
                                    )
                                }
                            }
                        }

                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showStyleMenu = true }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(MikaIcons.Palette, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("Style", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                Text(
                                    text = settings.crossfadeStyle.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            DropdownMenu(expanded = showStyleMenu, onDismissRequest = { showStyleMenu = false }) {
                                CrossfadeStyle.entries.forEach { st ->
                                    DropdownMenuItem(
                                        text = { Text(st.label) },
                                        onClick = {
                                            showStyleMenu = false
                                            onCrossfadeStyle(st)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Play/Pause Fade Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                ExtrasSwitchRow(
                    icon = MikaIcons.PauseBold,
                    title = "Enable Fade Effect on Play/Pause",
                    subtitle = null,
                    checked = settings.playPauseFadeEnabled,
                    onCheckedChange = onTogglePlayPauseFade,
                    testTag = "play_pause_fade_switch"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(start = 26.dp, end = 16.dp, bottom = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(1.5.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(MikaIcons.PlayBold, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("Play Fade Duration", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                WaveformRulerDial(
                                    value = settings.playFadeDurationMs,
                                    range = 0..3000,
                                    step = 100,
                                    valueLabel = "${settings.playFadeDurationMs}ms",
                                    onValueChange = onPlayFadeDurationMs,
                                    testTag = "play_fade_dial"
                                )
                            }
                            Slider(
                                value = settings.playFadeDurationMs.toFloat(),
                                onValueChange = { raw ->
                                    val snapped = ((raw / 50f).roundToInt() * 50).coerceIn(0, 3000)
                                    onPlayFadeDurationMs(snapped)
                                },
                                valueRange = 0f..3000f
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(0 to "Off", 250 to "250ms", 500 to "500ms", 1000 to "1s", 2000 to "2s").forEach { (ms, label) ->
                                    FilterChip(
                                        selected = settings.playFadeDurationMs == ms,
                                        onClick = { onPlayFadeDurationMs(ms) },
                                        label = { Text(label, fontWeight = FontWeight.SemiBold) }
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(MikaIcons.PauseBold, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("Pause Fade Duration", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                WaveformRulerDial(
                                    value = settings.pauseFadeDurationMs,
                                    range = 0..3000,
                                    step = 100,
                                    valueLabel = "${settings.pauseFadeDurationMs}ms",
                                    onValueChange = onPauseFadeDurationMs,
                                    testTag = "pause_fade_dial"
                                )
                            }
                            Slider(
                                value = settings.pauseFadeDurationMs.toFloat(),
                                onValueChange = { raw ->
                                    val snapped = ((raw / 50f).roundToInt() * 50).coerceIn(0, 3000)
                                    onPauseFadeDurationMs(snapped)
                                },
                                valueRange = 0f..3000f
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(0 to "Off", 250 to "250ms", 500 to "500ms", 1000 to "1s", 2000 to "2s").forEach { (ms, label) ->
                                    FilterChip(
                                        selected = settings.pauseFadeDurationMs == ms,
                                        onClick = { onPauseFadeDurationMs(ms) },
                                        label = { Text(label, fontWeight = FontWeight.SemiBold) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Queue & Seek Behavior Card (SS7)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                ExtrasSwitchRow(
                    icon = MikaIcons.Repeat,
                    title = "Jump to first track after finishing queue",
                    subtitle = null,
                    checked = settings.jumpToFirstAfterQueue,
                    onCheckedChange = onToggleJumpToFirst,
                    testTag = "jump_first_after_queue_switch"
                )
                ExtrasSwitchRow(
                    icon = MikaIcons.Replay,
                    title = "Previous button replays",
                    subtitle = "If pressed while current position is greater than seek duration",
                    checked = settings.previousButtonReplays,
                    onCheckedChange = onTogglePreviousReplays,
                    testTag = "previous_replays_switch"
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(MikaIcons.Forward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Seek Duration (Seconds)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "You can tap on current duration to seek backwards & total duration to seek forwards",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    WaveformRulerDial(
                        value = settings.skipIntervalSec,
                        range = 3..60,
                        step = 1,
                        valueLabel = "${settings.skipIntervalSec}s",
                        onValueChange = onSkipIntervalSec,
                        testTag = "seek_duration_dial"
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(MikaIcons.Restore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Minimum track duration to restore last played position",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    WaveformRulerDial(
                        value = settings.restorePositionMinDurationMin,
                        range = 0..60,
                        step = 1,
                        valueLabel = "${settings.restorePositionMinDurationMin}m",
                        onValueChange = onRestoreMinDurationMin,
                        testTag = "restore_min_duration_dial"
                    )
                }
            }
        }
    }
}

@Composable
private fun ExtrasSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun ColorSwatchSettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    swatchColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(swatchColor)
                .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
        )
    }
}

@Composable
fun SimpleHueColorPickerDialog(
    title: String,
    initialArgb: Long,
    onSelectArgb: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val initialColor = if (initialArgb != 0L) Color(initialArgb.toInt()) else Color(0xFF4F5FD5)
    val initialHsv = remember(initialArgb) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor.toArgb(), it) }
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0].coerceIn(0f, 360f)) }
    var sat by remember { mutableFloatStateOf(initialHsv[1].coerceIn(0.05f, 1f)) }
    var value by remember { mutableFloatStateOf(initialHsv[2].coerceIn(0.25f, 1f)) }
    val previewColor = Color.hsv(hue, sat, value)
    val hexString = remember(previewColor) {
        String.format(Locale.US, "#%06X", previewColor.toArgb() and 0xFFFFFF)
    }
    var hexInput by remember(hexString) { mutableStateOf(hexString) }

    val quickPresets = remember {
        listOf(
            Color(0xFF1E88FF),
            Color(0xFF00BCD4),
            Color(0xFF6366F1),
            Color(0xFF8B5CF6),
            Color(0xFFF43F5E),
            Color(0xFFF59E0B),
            Color(0xFF10B981)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(previewColor)
                            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    )
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { raw ->
                            hexInput = raw
                            val clean = raw.trim().removePrefix("#")
                            if (clean.length == 6) {
                                runCatching { android.graphics.Color.parseColor("#$clean") }.getOrNull()?.let { parsed ->
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(parsed, hsv)
                                    hue = hsv[0]
                                    sat = hsv[1].coerceIn(0.05f, 1f)
                                    value = hsv[2].coerceIn(0.25f, 1f)
                                }
                            }
                        },
                        label = { Text("Hex Color") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Interactive 2D Saturation / Value gradient pad
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                sat = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                                value = 1f - (offset.y / size.height.coerceAtLeast(1)).coerceIn(0f, 0.75f)
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                sat = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                                value = 1f - (change.position.y / size.height.coerceAtLeast(1)).coerceIn(0f, 0.75f)
                            }
                        }
                ) {
                    val pureHue = Color.hsv(hue, 1f, 1f)
                    drawRoundRect(
                        brush = Brush.horizontalGradient(listOf(Color.White, pureHue)),
                        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                    )
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)),
                        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                    )
                    val cx = sat * size.width
                    val cy = (1f - value) * size.height
                    drawCircle(color = Color.White, radius = 9.dp.toPx(), center = Offset(cx, cy), style = Stroke(width = 2.5.dp.toPx()))
                    drawCircle(color = previewColor, radius = 6.dp.toPx(), center = Offset(cx, cy))
                }

                // Hue Spectrum Slider Bar
                val spectrumColors = remember {
                    listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                }
                val latestHueUpdate by rememberUpdatedState<(Float) -> Unit>({ hue = it })
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .clip(RoundedCornerShape(50))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                latestHueUpdate((offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f) * 360f)
                            }
                        }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, _ ->
                                change.consume()
                                latestHueUpdate((change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f) * 360f)
                            }
                        }
                ) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(spectrumColors),
                        cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                    )
                    val tx = (hue / 360f).coerceIn(0f, 1f) * size.width
                    drawCircle(color = Color.White, radius = 10.dp.toPx(), center = Offset(tx, size.height / 2f))
                    drawCircle(color = Color.hsv(hue, 0.9f, 1f), radius = 7.dp.toPx(), center = Offset(tx, size.height / 2f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    quickPresets.forEach { swatch ->
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .clickable {
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(swatch.toArgb(), hsv)
                                    hue = hsv[0]
                                    sat = hsv[1]
                                    value = hsv[2]
                                }
                        )
                    }
                }

                val contrastText = if (previewColor.luminance() > 0.35f) "Dark text contrast OK" else "Light text contrast OK"
                Text(
                    text = contrastText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSelectArgb(previewColor.toArgb().toLong() and 0xFFFFFFFFL)
                onDismiss()
            }) {
                Text("Apply", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
