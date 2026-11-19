package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppSettings
import com.example.model.FullPlayerConfig
import com.example.model.FullPlayerStyle
import com.example.model.MiniPlayerConfig
import com.example.model.MiniPlayerStyle
import com.example.model.PlaybackProgressState
import com.example.model.PlaybackUiState
import com.example.model.Song
import com.example.model.VisualizerStyle
import com.example.ui.components.MiniPlayer
import com.example.ui.components.TrackArtwork
import com.example.ui.icons.MikaIcons
import com.example.ui.theme.BrandDisplayFontFamily
import com.example.ui.theme.LocalMikaColors
import com.example.ui.theme.mikaScaledIcon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerStyleSettingsScreen(
    settings: AppSettings,
    currentSong: Song?,
    isPlaying: Boolean,
    artworkAccentPair: Pair<Color, Color>?,
    onBack: () -> Unit,
    onUpdateMiniPlayerConfig: (MiniPlayerConfig) -> Unit,
    onResetMiniPlayerConfig: () -> Unit,
    onUpdateFullPlayerConfig: (FullPlayerConfig) -> Unit,
    onResetFullPlayerConfig: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var draftMini by remember(settings.miniPlayerConfig) { mutableStateOf(settings.miniPlayerConfig) }
    var draftFull by remember(settings.fullPlayerConfig) { mutableStateOf(settings.fullPlayerConfig) }
    val accent = MaterialTheme.colorScheme.primary

    val previewSong = remember(currentSong) {
        currentSong ?: Song(
            id = -99L,
            title = "Midnight Serenade",
            artist = "Mika Audio Engine",
            album = "Hi-Fi Studio",
            albumId = -1L,
            artistId = -1L,
            durationMs = 214_000L,
            contentUri = "",
            filePath = "",
            folderPath = "",
            folderName = "Music",
            mimeType = "audio/flac",
            sizeBytes = 32_000_000L,
            dateAddedSec = 0L,
            dateModifiedSec = 0L,
            isFavorite = true
        )
    }
    val previewPlayback = remember(previewSong, isPlaying) {
        PlaybackUiState(
            currentSong = previewSong,
            queue = listOf(previewSong),
            currentIndex = 0,
            isPlaying = isPlaying
        )
    }
    val previewProgressFlow = remember {
        MutableStateFlow(PlaybackProgressState(positionMs = 82_000L, durationMs = 214_000L))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Player Style Studio",
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
                actions = {
                    TextButton(
                        onClick = {
                            if (selectedTabIndex == 0) {
                                draftMini = MiniPlayerConfig()
                                onResetMiniPlayerConfig()
                            } else {
                                draftFull = FullPlayerConfig()
                                onResetFullPlayerConfig()
                            }
                        }
                    ) {
                        Text("Reset", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("player_style_studio_screen")
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Mini Player", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Full Player", fontWeight = FontWeight.Bold) }
                )
            }

            AnimatedContent(targetState = selectedTabIndex, label = "player_style_tabs") { tab ->
                if (tab == 0) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "LIVE MINI PLAYER PREVIEW",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = accent,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    MiniPlayer(
                                        playbackState = previewPlayback,
                                        progressFlow = previewProgressFlow,
                                        artworkAccentPair = artworkAccentPair,
                                        onOpenFullPlayer = {},
                                        onDragExpansionDelta = {},
                                        onDragExpansionEnd = {},
                                        onTogglePlayPause = {},
                                        onSkipNext = {},
                                        onSkipPrevious = {},
                                        onToggleFavorite = {},
                                        miniPlayerConfig = draftMini,
                                        applyNavigationBarsPadding = false,
                                        forceTrackColors = settings.forceMiniPlayerTrackColors
                                    )
                                }
                            }
                        }

                        item {
                            StudioSectionHeader("MINI PLAYER LAYOUT STYLE", accent)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(MiniPlayerStyle.entries, key = { it.name }) { style ->
                                    val selected = draftMini.style == style
                                    Surface(
                                        onClick = {
                                            draftMini = draftMini.copy(style = style)
                                            onUpdateMiniPlayerConfig(draftMini)
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (selected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            width = if (selected) 1.5.dp else 1.dp,
                                            color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier.width(152.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = style.label,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selected) accent else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = style.description,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            StudioSliderCard(
                                title = "Corner Radius",
                                valueLabel = "${draftMini.cornerRadiusDp}dp",
                                value = draftMini.cornerRadiusDp.toFloat(),
                                valueRange = 0f..32f,
                                onValueChange = {
                                    draftMini = draftMini.copy(cornerRadiusDp = it.roundToInt())
                                    onUpdateMiniPlayerConfig(draftMini)
                                }
                            )
                        }
                        item {
                            StudioSliderCard(
                                title = "Artwork Size",
                                valueLabel = "${draftMini.artworkSizeDp}dp",
                                value = draftMini.artworkSizeDp.toFloat(),
                                valueRange = 42f..72f,
                                onValueChange = {
                                    draftMini = draftMini.copy(artworkSizeDp = it.roundToInt())
                                    onUpdateMiniPlayerConfig(draftMini)
                                }
                            )
                        }
                        item {
                            StudioSliderCard(
                                title = "Artwork Corner Radius",
                                valueLabel = "${draftMini.artworkCornerDp}dp",
                                value = draftMini.artworkCornerDp.toFloat(),
                                valueRange = 4f..36f,
                                onValueChange = {
                                    draftMini = draftMini.copy(artworkCornerDp = it.roundToInt())
                                    onUpdateMiniPlayerConfig(draftMini)
                                }
                            )
                        }

                        item {
                            StudioSectionHeader("MINI PLAYER ELEMENTS", accent)
                            StudioSwitchGroup(
                                rows = listOf(
                                    Triple("Show Previous / Next", draftMini.showPrevNext) { v ->
                                        draftMini = draftMini.copy(showPrevNext = v)
                                        onUpdateMiniPlayerConfig(draftMini)
                                    },
                                    Triple("Show Favorite Heart", draftMini.showFavorite) { v ->
                                        draftMini = draftMini.copy(showFavorite = v)
                                        onUpdateMiniPlayerConfig(draftMini)
                                    },
                                    Triple("Show Progress Indicator", draftMini.showProgressIndicator) { v ->
                                        draftMini = draftMini.copy(showProgressIndicator = v)
                                        onUpdateMiniPlayerConfig(draftMini)
                                    },
                                    Triple("Show Artist Name", draftMini.showArtist) { v ->
                                        draftMini = draftMini.copy(showArtist = v)
                                        onUpdateMiniPlayerConfig(draftMini)
                                    },
                                    Triple("Compact Height", draftMini.compactHeight) { v ->
                                        draftMini = draftMini.copy(compactHeight = v)
                                        onUpdateMiniPlayerConfig(draftMini)
                                    },
                                    Triple("Swipe Horizontally to Skip", draftMini.swipeToChangeTrack) { v ->
                                        draftMini = draftMini.copy(swipeToChangeTrack = v)
                                        onUpdateMiniPlayerConfig(draftMini)
                                    }
                                )
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            FullPlayerStylePreviewCard(
                                config = draftFull,
                                song = previewSong,
                                artworkAccentPair = artworkAccentPair
                            )
                        }

                        item {
                            StudioSectionHeader("FULL SCREEN PLAYER STYLE", accent)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(FullPlayerStyle.entries, key = { it.name }) { style ->
                                    val selected = draftFull.style == style
                                    Surface(
                                        onClick = {
                                            draftFull = draftFull.copy(style = style)
                                            onUpdateFullPlayerConfig(draftFull)
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (selected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            width = if (selected) 1.5.dp else 1.dp,
                                            color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier.width(156.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = style.label,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selected) accent else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = style.description,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            StudioSectionHeader("AUDIO VISUALIZER STYLE", accent)
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(VisualizerStyle.entries, key = { it.name }) { vis ->
                                            FilterChip(
                                                selected = draftFull.visualizerStyle == vis,
                                                onClick = {
                                                    draftFull = draftFull.copy(visualizerStyle = vis)
                                                    onUpdateFullPlayerConfig(draftFull)
                                                },
                                                label = { Text(vis.label, fontWeight = FontWeight.Bold) }
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val next = !draftFull.showSimpleLyricsInVisualizer
                                                draftFull = draftFull.copy(showSimpleLyricsInVisualizer = next)
                                                onUpdateFullPlayerConfig(draftFull)
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Show Live Lyrics Line inside Visualizer",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Switch(
                                            checked = draftFull.showSimpleLyricsInVisualizer,
                                            onCheckedChange = { v ->
                                                draftFull = draftFull.copy(showSimpleLyricsInVisualizer = v)
                                                onUpdateFullPlayerConfig(draftFull)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            StudioSliderCard(
                                title = "Artwork Corner Radius",
                                valueLabel = "${draftFull.artworkCornerDp}dp",
                                value = draftFull.artworkCornerDp.toFloat(),
                                valueRange = 0f..48f,
                                onValueChange = {
                                    draftFull = draftFull.copy(artworkCornerDp = it.roundToInt())
                                    onUpdateFullPlayerConfig(draftFull)
                                }
                            )
                        }
                        item {
                            StudioSliderCard(
                                title = "Artwork Border Width",
                                valueLabel = "${draftFull.artworkBorderDp}dp",
                                value = draftFull.artworkBorderDp.toFloat(),
                                valueRange = 0f..14f,
                                onValueChange = {
                                    draftFull = draftFull.copy(artworkBorderDp = it.roundToInt())
                                    onUpdateFullPlayerConfig(draftFull)
                                }
                            )
                        }
                        item {
                            StudioSliderCard(
                                title = "Transport Controls Scale",
                                valueLabel = "${(draftFull.controlsScale * 100).roundToInt()}%",
                                value = draftFull.controlsScale,
                                valueRange = 0.80f..1.25f,
                                onValueChange = {
                                    draftFull = draftFull.copy(controlsScale = (it * 20f).roundToInt() / 20f)
                                    onUpdateFullPlayerConfig(draftFull)
                                }
                            )
                        }

                        item {
                            StudioSectionHeader("FULL PLAYER ELEMENTS", accent)
                            StudioSwitchGroup(
                                rows = listOf(
                                    Triple("Show Queue Position in Top Bar", draftFull.showQueuePosition) { v ->
                                        draftFull = draftFull.copy(showQueuePosition = v)
                                        onUpdateFullPlayerConfig(draftFull)
                                    },
                                    Triple("Show Artist in Top Bar", draftFull.showArtistInTopBar) { v ->
                                        draftFull = draftFull.copy(showArtistInTopBar = v)
                                        onUpdateFullPlayerConfig(draftFull)
                                    },
                                    Triple("Show Quick Actions Row", draftFull.showQuickActionsRow) { v ->
                                        draftFull = draftFull.copy(showQuickActionsRow = v)
                                        onUpdateFullPlayerConfig(draftFull)
                                    },
                                    Triple("Show Skip Backward / Forward Chips", draftFull.showSkipChips) { v ->
                                        draftFull = draftFull.copy(showSkipChips = v)
                                        onUpdateFullPlayerConfig(draftFull)
                                    },
                                    Triple("Show Bottom Utility Bar", draftFull.showBottomUtilityBar) { v ->
                                        draftFull = draftFull.copy(showBottomUtilityBar = v)
                                        onUpdateFullPlayerConfig(draftFull)
                                    },
                                    Triple("Show Visualizer Pill", draftFull.showVisualizerPill) { v ->
                                        draftFull = draftFull.copy(showVisualizerPill = v)
                                        onUpdateFullPlayerConfig(draftFull)
                                    },
                                    Triple("Center Song Title & Artist", draftFull.centerMetadata) { v ->
                                        draftFull = draftFull.copy(centerMetadata = v)
                                        onUpdateFullPlayerConfig(draftFull)
                                    },
                                    Triple("Show Shuffle & Repeat Buttons", draftFull.showShuffleRepeat) { v ->
                                        draftFull = draftFull.copy(showShuffleRepeat = v)
                                        onUpdateFullPlayerConfig(draftFull)
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullPlayerStylePreviewCard(
    config: FullPlayerConfig,
    song: Song,
    artworkAccentPair: Pair<Color, Color>?
) {
    val accent = artworkAccentPair?.first ?: MaterialTheme.colorScheme.primary
    val secondary = artworkAccentPair?.second ?: LocalMikaColors.current.accentSecondary
    val cornerDp = if (config.style == FullPlayerStyle.VINYL) 100.dp else config.artworkCornerDp.dp

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            accent.copy(alpha = 0.24f),
                            secondary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(16.dp),
            horizontalAlignment = if (config.centerMetadata) Alignment.CenterHorizontally else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (config.showQueuePosition) "1 / 24 · ${config.style.label}" else config.style.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                if (config.showArtistInTopBar) {
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(116.dp)
                    .align(Alignment.CenterHorizontally)
                    .then(
                        if (config.artworkBorderDp > 0) {
                            Modifier.border(
                                config.artworkBorderDp.coerceAtMost(10).dp,
                                accent.copy(alpha = 0.55f),
                                RoundedCornerShape(cornerDp)
                            )
                        } else Modifier
                    )
                    .clip(RoundedCornerShape(cornerDp))
            ) {
                TrackArtwork(
                    song = song,
                    cornerRadius = cornerDp,
                    bitmapSizePx = 220,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (config.showVisualizerPill) {
                VisualizerStylePreviewStrip(
                    style = config.visualizerStyle,
                    accent = accent,
                    secondary = secondary
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = if (config.centerMetadata) Alignment.CenterHorizontally else Alignment.Start
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = if (config.centerMetadata) TextAlign.Center else TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (config.showShuffleRepeat) {
                    Icon(MikaIcons.Shuffle, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                Icon(MikaIcons.PreviousBold, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size((24f * config.controlsScale).dp))
                Box(
                    modifier = Modifier
                        .size((42f * config.controlsScale).dp)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(MikaIcons.PauseBold, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                }
                Icon(MikaIcons.NextBold, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size((24f * config.controlsScale).dp))
                if (config.showShuffleRepeat) {
                    Icon(MikaIcons.Repeat, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun VisualizerStylePreviewStrip(
    style: VisualizerStyle,
    accent: Color,
    secondary: Color
) {
    val heights = remember { listOf(0.35f, 0.72f, 0.52f, 0.92f, 0.68f, 0.44f, 0.85f, 0.58f, 0.76f, 0.40f, 0.64f, 0.30f) }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(horizontal = 16.dp)
    ) {
        when (style) {
            VisualizerStyle.BARS, VisualizerStyle.MIRROR_PEAKS -> {
                val barW = size.width / (heights.size * 1.7f)
                val step = size.width / heights.size
                heights.forEachIndexed { idx, h ->
                    val bh = size.height * h
                    val top = if (style == VisualizerStyle.MIRROR_PEAKS) (size.height - bh) / 2f else size.height - bh
                    drawRoundRect(
                        color = if (idx % 2 == 0) accent else secondary,
                        topLeft = Offset(idx * step + (step - barW) / 2f, top),
                        size = Size(barW, bh),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                    )
                }
            }
            VisualizerStyle.DOT_MATRIX -> {
                val step = size.width / heights.size
                heights.forEachIndexed { idx, h ->
                    val r = (3.dp.toPx() * (0.6f + h * 0.7f))
                    drawCircle(
                        color = if (idx % 2 == 0) accent else secondary,
                        radius = r,
                        center = Offset(idx * step + step / 2f, size.height * (1f - h * 0.75f))
                    )
                }
            }
            VisualizerStyle.WAVEFORM -> {
                val step = size.width / (heights.size - 1)
                for (i in 0 until heights.lastIndex) {
                    val y1 = size.height * (0.85f - heights[i] * 0.65f)
                    val y2 = size.height * (0.85f - heights[i + 1] * 0.65f)
                    drawLine(
                        color = accent,
                        start = Offset(i * step, y1),
                        end = Offset((i + 1) * step, y2),
                        strokeWidth = 2.6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            VisualizerStyle.CIRCULAR -> {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val baseR = size.height * 0.34f
                for (i in 0 until 12) {
                    val ang = (i / 12f) * 2f * PI.toFloat()
                    val ext = baseR * (0.5f + heights[i] * 0.85f)
                    drawLine(
                        color = if (i % 2 == 0) accent else secondary,
                        start = Offset(cx + cos(ang) * baseR * 0.55f, cy + sin(ang) * baseR * 0.55f),
                        end = Offset(cx + cos(ang) * ext, cy + sin(ang) * ext),
                        strokeWidth = 2.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioSectionHeader(title: String, accent: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = accent,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
private fun StudioSliderCard(
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
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
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(valueLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
        }
    }
}

@Composable
private fun StudioSwitchGroup(
    rows: List<Triple<String, Boolean, (Boolean) -> Unit>>
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            rows.forEachIndexed { idx, (label, checked, onChanged) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChanged(!checked) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = checked, onCheckedChange = onChanged)
                }
                if (idx < rows.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}
