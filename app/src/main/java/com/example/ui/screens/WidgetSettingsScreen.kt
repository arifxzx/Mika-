package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Song
import com.example.model.WidgetArrangement
import com.example.model.WidgetArtworkEffect
import com.example.model.WidgetBackgroundStyle
import com.example.model.WidgetConfig
import com.example.model.WidgetPreviewSize
import com.example.model.WidgetTapAction
import com.example.model.WidgetThemeMode
import com.example.ui.components.TrackArtwork
import com.example.ui.icons.MikaIcons
import com.example.ui.theme.BrandDisplayFontFamily
import com.example.ui.theme.mikaScaledIcon
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSettingsScreen(
    initialConfig: WidgetConfig,
    currentSong: Song?,
    isPlaying: Boolean,
    artworkAccentPair: Pair<Color, Color>?,
    onSave: (WidgetConfig) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    var draft by remember(initialConfig) { mutableStateOf(initialConfig) }
    val accent = MaterialTheme.colorScheme.primary

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Widget Settings",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = BrandDisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(MikaIcons.ArrowLeft, contentDescription = "Back", modifier = Modifier.size(mikaScaledIcon(24.dp)))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            draft = WidgetConfig()
                            onReset()
                        },
                        modifier = Modifier.testTag("widget_reset_button")
                    ) {
                        Text("Reset", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onBack) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            onSave(draft)
                            onBack()
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("widget_save_button")
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("widget_settings_screen")
        ) {
            // Sticky Live Preview Panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LiveWidgetPreviewCard(
                        config = draft,
                        song = currentSong,
                        isPlaying = isPlaying,
                        artworkAccentPair = artworkAccentPair
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            WidgetPreviewSize.entries.forEach { sizeOption ->
                                val selected = draft.previewSize == sizeOption
                                Surface(
                                    onClick = { draft = draft.copy(previewSize = sizeOption) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) accent.copy(alpha = 0.22f) else Color.Transparent
                                ) {
                                    Text(
                                        text = sizeOption.label,
                                        color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Scrollable Customization List
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    WidgetSectionHeader("APPEARANCE", accent)
                }
                item {
                    WidgetEnumSelectorCard(
                        title = "Theme",
                        options = WidgetThemeMode.entries,
                        selected = draft.theme,
                        labelOf = { it.label },
                        onSelect = { draft = draft.copy(theme = it) }
                    )
                }
                item {
                    WidgetSwitchCard(
                        title = "Colors from artwork",
                        checked = draft.colorsFromArtwork,
                        onCheckedChange = { draft = draft.copy(colorsFromArtwork = it) }
                    )
                }
                item {
                    WidgetEnumSelectorCard(
                        title = "Background",
                        options = WidgetBackgroundStyle.entries,
                        selected = draft.backgroundStyle,
                        labelOf = { it.label },
                        onSelect = { draft = draft.copy(backgroundStyle = it) }
                    )
                }
                item {
                    WidgetEnumSelectorCard(
                        title = "Artwork effect",
                        options = WidgetArtworkEffect.entries,
                        selected = draft.artworkEffect,
                        labelOf = { it.label },
                        onSelect = { draft = draft.copy(artworkEffect = it) }
                    )
                }
                item {
                    WidgetSliderCard(
                        title = "Background opacity",
                        valueLabel = "${(draft.backgroundOpacity * 100).roundToInt()}%",
                        value = draft.backgroundOpacity,
                        valueRange = 0f..1f,
                        onValueChange = { draft = draft.copy(backgroundOpacity = it) }
                    )
                }
                item {
                    WidgetSliderCard(
                        title = "Corner radius",
                        valueLabel = "${draft.cornerRadiusDp}dp",
                        value = draft.cornerRadiusDp.toFloat(),
                        valueRange = 0f..32f,
                        onValueChange = { draft = draft.copy(cornerRadiusDp = it.roundToInt()) }
                    )
                }
                item {
                    WidgetSliderCard(
                        title = "Artwork rounding",
                        valueLabel = "${draft.artworkRoundingPct}%",
                        value = draft.artworkRoundingPct.toFloat(),
                        valueRange = 0f..50f,
                        onValueChange = { draft = draft.copy(artworkRoundingPct = it.roundToInt()) }
                    )
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    WidgetSectionHeader("LAYOUT", accent)
                }
                item {
                    WidgetEnumSelectorCard(
                        title = "Arrangement",
                        options = WidgetArrangement.entries,
                        selected = draft.arrangement,
                        labelOf = { it.label },
                        onSelect = { draft = draft.copy(arrangement = it) }
                    )
                }
                item {
                    WidgetSliderCard(
                        title = "Content size",
                        valueLabel = "${draft.contentSizePct}%",
                        value = draft.contentSizePct.toFloat(),
                        valueRange = 70f..130f,
                        onValueChange = { draft = draft.copy(contentSizePct = it.roundToInt()) }
                    )
                }
                item {
                    WidgetSliderCard(
                        title = "Artwork size",
                        valueLabel = "${draft.artworkSizePct}%",
                        value = draft.artworkSizePct.toFloat(),
                        valueRange = 60f..130f,
                        onValueChange = { draft = draft.copy(artworkSizePct = it.roundToInt()) }
                    )
                }
                item {
                    WidgetSwitchGroupCard(
                        items = listOf(
                            Triple("Artwork", draft.showArtwork) { v: Boolean -> draft = draft.copy(showArtwork = v) },
                            Triple("Title", draft.showTitle) { v: Boolean -> draft = draft.copy(showTitle = v) },
                            Triple("Subtitle", draft.showSubtitle) { v: Boolean -> draft = draft.copy(showSubtitle = v) }
                        )
                    )
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    WidgetSectionHeader("CONTROLS", accent)
                }
                item {
                    WidgetSwitchGroupCard(
                        items = listOf(
                            Triple("Favourite", draft.showFavourite) { v: Boolean -> draft = draft.copy(showFavourite = v) },
                            Triple("Previous", draft.showPrevious) { v: Boolean -> draft = draft.copy(showPrevious = v) },
                            Triple("Play / Pause", draft.showPlayPause) { v: Boolean -> draft = draft.copy(showPlayPause = v) },
                            Triple("Next", draft.showNext) { v: Boolean -> draft = draft.copy(showNext = v) },
                            Triple("Repeat mode", draft.showRepeat) { v: Boolean -> draft = draft.copy(showRepeat = v) },
                            Triple("Shuffle", draft.showShuffle) { v: Boolean -> draft = draft.copy(showShuffle = v) },
                            Triple("Stop", draft.showStop) { v: Boolean -> draft = draft.copy(showStop = v) }
                        )
                    )
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    WidgetSectionHeader("TAP ACTIONS", accent)
                }
                item {
                    WidgetEnumSelectorCard(
                        title = "Widget",
                        options = WidgetTapAction.entries,
                        selected = draft.widgetTapAction,
                        labelOf = { it.label },
                        onSelect = { draft = draft.copy(widgetTapAction = it) }
                    )
                }
                item {
                    WidgetEnumSelectorCard(
                        title = "Artwork",
                        options = WidgetTapAction.entries,
                        selected = draft.artworkTapAction,
                        labelOf = { it.label },
                        onSelect = { draft = draft.copy(artworkTapAction = it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveWidgetPreviewCard(
    config: WidgetConfig,
    song: Song?,
    isPlaying: Boolean,
    artworkAccentPair: Pair<Color, Color>?
) {
    val sysDark = isSystemInDarkTheme()
    val isDark = when (config.theme) {
        WidgetThemeMode.DARK -> true
        WidgetThemeMode.LIGHT -> false
        WidgetThemeMode.SYSTEM -> sysDark
    }
    val previewScheme = if (isDark) darkColorScheme() else lightColorScheme()
    val startCol = when {
        config.colorsFromArtwork && artworkAccentPair != null -> artworkAccentPair.first
        else -> previewScheme.surfaceContainerHigh
    }.copy(alpha = config.backgroundOpacity)
    val endCol = when {
        config.colorsFromArtwork && artworkAccentPair != null -> artworkAccentPair.second
        else -> previewScheme.surfaceContainerHighest
    }.copy(alpha = config.backgroundOpacity)

    val fgColor = if (config.colorsFromArtwork && artworkAccentPair != null) {
        if (artworkAccentPair.first.luminance() > 0.55f) lightColorScheme().onSurface else darkColorScheme().onSurface
    } else {
        previewScheme.onSurface
    }

    val widthMod = when (config.previewSize) {
        WidgetPreviewSize.WIDE -> Modifier.fillMaxWidth()
        WidgetPreviewSize.LARGE -> Modifier.fillMaxWidth(0.92f)
        WidgetPreviewSize.SMALL -> Modifier.width(210.dp)
    }
    val isCardLayout = config.arrangement == WidgetArrangement.CARD ||
        (config.arrangement == WidgetArrangement.AUTO && config.previewSize == WidgetPreviewSize.LARGE)
    val artSize = (58f * (config.artworkSizePct / 100f)).dp
    val artCorner = artSize * (config.artworkRoundingPct / 100f)

    Box(
        modifier = widthMod
            .clip(RoundedCornerShape(config.cornerRadiusDp.dp))
            .background(
                when (config.backgroundStyle) {
                    WidgetBackgroundStyle.SOLID -> Brush.linearGradient(listOf(startCol, startCol))
                    else -> Brush.linearGradient(listOf(startCol, endCol))
                }
            )
    ) {
        if (config.backgroundStyle == WidgetBackgroundStyle.BLURRED_ART && song != null) {
            TrackArtwork(
                song = song,
                cornerRadius = config.cornerRadiusDp.dp,
                bitmapSizePx = 180,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.38f * config.backgroundOpacity)
                    .blur(18.dp)
            )
        }

        val contentScale = config.contentSizePct / 100f
        if (isCardLayout) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (config.showArtwork) {
                    PreviewArtworkBox(song, artSize, artCorner, config.artworkEffect, endCol, fgColor)
                }
                if (config.showTitle) {
                    Text(
                        text = song?.title ?: "Mika Player",
                        color = fgColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = (15f * contentScale).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (config.showSubtitle) {
                    Text(
                        text = song?.artist ?: "Ready to play",
                        color = fgColor.copy(alpha = 0.78f),
                        fontSize = (12f * contentScale).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                PreviewControlsRow(config, isPlaying, song?.isFavorite == true, fgColor)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (config.showArtwork) {
                    PreviewArtworkBox(song, artSize, artCorner, config.artworkEffect, endCol, fgColor)
                    Spacer(Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (config.showTitle) {
                        Text(
                            text = song?.title ?: "Mika Player",
                            color = fgColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = (15f * contentScale).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (config.showSubtitle) {
                        Text(
                            text = song?.artist ?: "Ready to play",
                            color = fgColor.copy(alpha = 0.78f),
                            fontSize = (12f * contentScale).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    PreviewControlsRow(config, isPlaying, song?.isFavorite == true, fgColor)
                }
            }
        }
    }
}

@Composable
private fun PreviewArtworkBox(
    song: Song?,
    size: androidx.compose.ui.unit.Dp,
    cornerRadius: androidx.compose.ui.unit.Dp,
    effect: WidgetArtworkEffect,
    glowColor: Color,
    fgColor: Color
) {
    val effectMod = when (effect) {
        WidgetArtworkEffect.NONE -> Modifier
        WidgetArtworkEffect.GLOW -> Modifier.shadow(10.dp, RoundedCornerShape(cornerRadius), ambientColor = glowColor, spotColor = glowColor)
        WidgetArtworkEffect.SHADOW -> Modifier.shadow(10.dp, RoundedCornerShape(cornerRadius))
    }
    if (song != null) {
        TrackArtwork(
            song = song,
            cornerRadius = cornerRadius,
            bitmapSizePx = 160,
            modifier = Modifier
                .size(size)
                .then(effectMod)
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .then(effectMod)
                .clip(RoundedCornerShape(cornerRadius))
                .background(fgColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(MikaIcons.MusicNote, null, tint = fgColor, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun PreviewControlsRow(
    config: WidgetConfig,
    isPlaying: Boolean,
    isFavorite: Boolean,
    fgColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (config.showFavourite) {
            Icon(
                imageVector = if (isFavorite) MikaIcons.HeartBold else MikaIcons.HeartLinear,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(18.dp)
            )
        }
        if (config.showShuffle) {
            Icon(MikaIcons.Shuffle, null, tint = fgColor, modifier = Modifier.size(18.dp))
        }
        if (config.showPrevious) {
            Icon(MikaIcons.PreviousBold, null, tint = fgColor, modifier = Modifier.size(20.dp))
        }
        if (config.showPlayPause) {
            Icon(
                imageVector = if (isPlaying) MikaIcons.PauseBold else MikaIcons.PlayBold,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(24.dp)
            )
        }
        if (config.showNext) {
            Icon(MikaIcons.NextBold, null, tint = fgColor, modifier = Modifier.size(20.dp))
        }
        if (config.showRepeat) {
            Icon(MikaIcons.Repeat, null, tint = fgColor, modifier = Modifier.size(18.dp))
        }
        if (config.showStop) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(fgColor, RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun WidgetSectionHeader(title: String, accent: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = accent,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
private fun WidgetSwitchCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun WidgetSwitchGroupCard(
    items: List<Triple<String, Boolean, (Boolean) -> Unit>>
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            items.forEach { (title, checked, onChange) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChange(!checked) }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Switch(checked = checked, onCheckedChange = onChange)
                }
            }
        }
    }
}

@Composable
private fun WidgetSliderCard(
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(valueLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
        }
    }
}

@Composable
private fun <T> WidgetEnumSelectorCard(
    title: String,
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(labelOf(selected), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(options) { item ->
                    FilterChip(
                        selected = item == selected,
                        onClick = { onSelect(item) },
                        label = { Text(labelOf(item), fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        }
    }
}
