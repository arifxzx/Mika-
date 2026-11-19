package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.media.ArtworkDetails
import com.example.model.AppSettings
import com.example.model.ArtworkGestureAction
import com.example.model.FullPlayerConfig
import com.example.model.FullPlayerStyle
import com.example.model.LyricsData
import com.example.model.PlaybackProgressState
import com.example.model.PlaybackUiState
import com.example.model.RepeatMode
import com.example.model.SeekBarStyle
import com.example.model.SleepTimerState
import com.example.model.Song
import com.example.model.VisualizerStyle
import com.example.playback.AudioSpectrumAnalyzer
import com.example.ui.components.MikaSeekBar
import com.example.ui.components.MikaSleekSlider
import com.example.ui.components.PlayerBackgroundLayer
import com.example.ui.components.TrackArtwork
import com.example.ui.components.rememberParallaxOffset
import com.example.ui.icons.MikaIcons
import com.example.ui.theme.LocalMikaColors
import com.example.ui.theme.mikaScaledIcon
import com.example.util.Formatters
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullPlayerScreen(
    playbackState: PlaybackUiState,
    isFavorite: Boolean,
    isEqualizerEnabled: Boolean = false,
    progressFlow: StateFlow<PlaybackProgressState>,
    lyrics: LyricsData?,
    artworkAccentPair: Pair<Color, Color>?,
    sleepTimerState: SleepTimerState,
    skipIntervalSec: Int,
    trackWaveform: FloatArray = FloatArray(0),
    liveWaveformHistory: FloatArray = FloatArray(0),
    onMinimize: () -> Unit,
    onDragExpansionDelta: (Float) -> Unit,
    onDragExpansionEnd: (Float) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onRateAndMood: (Song) -> Unit = {},
    onSaveArtwork: (Song) -> Unit = {},
    onInspectArtworkDetails: suspend (Song) -> ArtworkDetails? = { null },
    onOpenSpeedPitch: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenFullLyrics: () -> Unit,
    onImportLyrics: () -> Unit,
    onEditTags: (Song) -> Unit,
    onShowSongInfo: (Song) -> Unit,
    onMoreClick: (Song) -> Unit,
    settings: AppSettings = AppSettings(),
    onOpenPlayerBackground: () -> Unit = {},
    onShareSong: (Song) -> Unit = {},
    onOpenSoundControl: () -> Unit = onOpenSpeedPitch,
    onUpdateFullPlayerConfig: (FullPlayerConfig) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val song = playbackState.currentSong ?: return

    val fpConfig = settings.fullPlayerConfig
    val mikaColors = LocalMikaColors.current
    val accent = mikaColors.playerAccent
    val secondaryAccent = mikaColors.playerAccentSecondary
    val transportIcons = remember(settings.playbackIconStyle) {
        MikaIcons.transportIcons(settings.playbackIconStyle)
    }
    val haptic = LocalHapticFeedback.current
    var showLyricsMode by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showArtworkPreviewDialog by remember { mutableStateOf(false) }
    var showVisualizerMenu by remember { mutableStateOf(false) }

    val latestDragDelta by rememberUpdatedState(onDragExpansionDelta)
    val latestDragEnd by rememberUpdatedState(onDragExpansionEnd)
    val latestOpenQueue by rememberUpdatedState(onOpenQueue)
    val latestSkipNext by rememberUpdatedState(onSkipNext)
    val latestSkipPrevious by rememberUpdatedState(onSkipPrevious)

    fun executeArtworkGesture(action: ArtworkGestureAction, targetSong: Song) {
        when (action) {
            ArtworkGestureAction.NONE -> Unit
            ArtworkGestureAction.PLAY_PAUSE -> onTogglePlayPause()
            ArtworkGestureAction.OPEN_LYRICS -> showLyricsMode = !showLyricsMode
            ArtworkGestureAction.ARTWORK_OPEN -> showArtworkPreviewDialog = true
            ArtworkGestureAction.NEXT_TRACK -> onSkipNext()
            ArtworkGestureAction.INFO -> onShowSongInfo(targetSong)
            ArtworkGestureAction.EQUALIZER -> onOpenEqualizer()
            ArtworkGestureAction.FAVORITE -> onToggleFavorite(targetSong.id)
            ArtworkGestureAction.SHARE -> onShareSong(targetSong)
        }
    }

    val isCustomSpeedOrPitch = abs(playbackState.playbackSpeed - 1f) > 0.01f ||
        abs(playbackState.playbackPitch - 1f) > 0.01f ||
        abs(settings.soundControl.pitchSemitones) > 0.01f ||
        settings.soundControl.loudnessEnhancerEnabled

    val playButtonScale by animateFloatAsState(
        targetValue = if (playbackState.isPlaying) 1.0f else 0.96f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "play_btn_scale"
    )

    val baseDark = if (mikaColors.isDark) Color(0xFF0A0C10) else MaterialTheme.colorScheme.background
    val dynamicStart = artworkAccentPair?.first ?: mikaColors.gradientStart
    val dynamicEnd = artworkAccentPair?.second ?: mikaColors.gradientEnd
    val bgBrush = if (mikaColors.applyGradientToFullPlayer) {
        Brush.verticalGradient(
            colors = listOf(
                lerp(baseDark, dynamicStart, if (mikaColors.isDark) 0.72f else 0.28f),
                lerp(baseDark, dynamicEnd, if (mikaColors.isDark) 0.42f else 0.16f),
                baseDark
            )
        )
    } else {
        Brush.verticalGradient(listOf(baseDark, baseDark))
    }

    val fgPrimary = if (mikaColors.isDark) Color.White else MaterialTheme.colorScheme.onBackground
    val fgSecondary = if (mikaColors.isDark) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant

    if (showArtworkPreviewDialog) {
        var showArtworkDetails by remember { mutableStateOf(false) }
        var artworkDetails by remember(song.id) { mutableStateOf<ArtworkDetails?>(null) }
        LaunchedEffect(song.id, showArtworkDetails) {
            if (showArtworkDetails && artworkDetails == null) {
                artworkDetails = runCatching { onInspectArtworkDetails(song) }.getOrNull()
            }
        }
        Dialog(
            onDismissRequest = { showArtworkPreviewDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .clickable { showArtworkPreviewDialog = false }
                    .padding(24.dp)
                    .testTag("full_player_artwork_viewer"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val viewerCorner = settings.cornerRadiusDp.coerceIn(8, 28).dp
                    val viewerModifier = if (settings.showActualArtworkRatio) {
                        Modifier
                            .fillMaxWidth()
                            .widthIn(max = 460.dp)
                            .clip(RoundedCornerShape(viewerCorner))
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .widthIn(max = 460.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(viewerCorner))
                    }
                    Box(modifier = viewerModifier) {
                        TrackArtwork(
                            song = song,
                            cornerRadius = viewerCorner,
                            bitmapSizePx = 1024,
                            preserveAspectRatio = settings.showActualArtworkRatio,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = Formatters.formatArtistAndAlbum(song.artist, song.album),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.74f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { onSaveArtwork(song) },
                            shape = RoundedCornerShape(50),
                            color = accent.copy(alpha = 0.24f),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.65f)),
                            modifier = Modifier.testTag("artwork_viewer_save")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = MikaIcons.ArtworkDownload,
                                    contentDescription = "Save artwork",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Save to /mika/artwork",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }

                        Surface(
                            onClick = { showArtworkDetails = !showArtworkDetails },
                            shape = RoundedCornerShape(50),
                            color = if (showArtworkDetails) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.26f)),
                            modifier = Modifier.testTag("artwork_viewer_details")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = MikaIcons.InfoCircle,
                                    contentDescription = "Artwork details",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Details",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    if (showArtworkDetails) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 420.dp)
                                .clickable(enabled = false) {}
                        ) {
                            val details = artworkDetails
                            if (details == null) {
                                Text(
                                    text = "Inspecting artwork metadata...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.padding(14.dp)
                                )
                            } else {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${details.width} x ${details.height} px · ${details.format}",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Size: ${Formatters.formatFileSize(details.sizeBytes)} · Source: ${details.sourceLabel}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.78f)
                                    )
                                    Text(
                                        text = "Palette: ${details.dominantHex} / ${details.secondaryHex}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.78f)
                                    )
                                }
                            }
                        }
                    }
                }
                IconButton(
                    onClick = { showArtworkPreviewDialog = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .size(48.dp)
                ) {
                    Icon(MikaIcons.Close, contentDescription = "Close artwork view", tint = Color.White)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var totalX = 0f
                    var totalY = 0f
                    var lockedVertical = false
                    var lockedHorizontal = false
                    var upwardTriggered = false
                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (lockedVertical) {
                                val vy = velocityTracker.calculateVelocity().y
                                if (totalY < -85f && vy < -260f && !upwardTriggered) {
                                    latestOpenQueue()
                                }
                                latestDragEnd(vy)
                            }
                            break
                        }
                        if (change.isConsumed && !lockedVertical) {
                            break
                        }
                        val delta = change.positionChange()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        totalX += delta.x
                        totalY += delta.y
                        if (!lockedVertical && !lockedHorizontal) {
                            if (abs(totalY) > 4f && abs(totalY) >= abs(totalX) * 0.85f) {
                                lockedVertical = true
                            } else if (abs(totalX) > 22f && abs(totalX) > abs(totalY)) {
                                lockedHorizontal = true
                            }
                        }
                        if (lockedVertical) {
                            change.consume()
                            if (totalY > 0f) {
                                latestDragDelta(delta.y)
                            } else if (totalY < -140f && !upwardTriggered) {
                                upwardTriggered = true
                                latestOpenQueue()
                            }
                        }
                    }
                }
            }
            .testTag("full_player_screen")
    ) {
        PlayerBackgroundLayer(
            song = song,
            settings = settings,
            artworkAccentPair = artworkAccentPair,
            baseBackgroundBrush = bgBrush,
            isExpanded = true,
            modifier = Modifier.fillMaxSize()
        )

        if (fpConfig.style == FullPlayerStyle.IMMERSIVE) {
            TrackArtwork(
                song = song,
                cornerRadius = 0.dp,
                bitmapSizePx = 720,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.28f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.55f),
                                Color.Black.copy(alpha = 0.82f)
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .widthIn(max = 580.dp)
                .align(Alignment.Center)
                .padding(horizontal = 22.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar (SS1: Down chevron | Index/Total + Artist | Circular ••• menu pill)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onMinimize,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("full_player_minimize")
                ) {
                    Icon(
                        imageVector = MikaIcons.ArrowDown,
                        contentDescription = "Minimize player",
                        tint = fgPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                val queueCount = playbackState.queue.size.coerceAtLeast(1)
                val queueIndex = (playbackState.currentIndex + 1).coerceIn(1, queueCount)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (fpConfig.showQueuePosition) {
                        Text(
                            text = "$queueIndex/$queueCount",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = fgSecondary,
                            modifier = Modifier.testTag("full_player_queue_position")
                        )
                    }
                    if (fpConfig.showArtistInTopBar) {
                        Text(
                            text = song.artist.ifBlank { "Unknown Artist" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = fgPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (sleepTimerState.isActive) {
                        val label = if (sleepTimerState.stopAtEndOfTrack) {
                            "Sleep: End of track"
                        } else {
                            "Sleep: ${Formatters.formatDuration(sleepTimerState.remainingMs)}"
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }
                }

                // Right circular translucent pill with three horizontal dots (SS1)
                Box {
                    Surface(
                        onClick = { showOverflowMenu = true },
                        shape = CircleShape,
                        color = fgPrimary.copy(alpha = 0.14f),
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("full_player_more_menu")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = MikaIcons.MoreVert,
                                contentDescription = "Player menu",
                                tint = if (sleepTimerState.isActive) accent else fgPrimary,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer { rotationZ = 90f }
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false }
                    ) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    MikaIcons.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            text = { Text("Player Background", fontWeight = FontWeight.Bold) },
                            onClick = {
                                showOverflowMenu = false
                                onOpenPlayerBackground()
                            },
                            modifier = Modifier.testTag("menu_player_background")
                        )

                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    MikaIcons.SleepTimer,
                                    contentDescription = null,
                                    tint = if (sleepTimerState.isActive) accent else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            text = {
                                Column {
                                    Text(
                                        text = "Sleep Timer",
                                        fontWeight = FontWeight.Bold,
                                        color = if (sleepTimerState.isActive) accent else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (sleepTimerState.isActive) {
                                        Text(
                                            text = if (sleepTimerState.stopAtEndOfTrack) {
                                                "End of track"
                                            } else {
                                                Formatters.formatDuration(sleepTimerState.remainingMs)
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = accent
                                        )
                                    }
                                }
                            },
                            onClick = {
                                showOverflowMenu = false
                                onOpenSleepTimer()
                            },
                            modifier = Modifier.testTag("menu_sleep_timer")
                        )

                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    MikaIcons.Speedometer,
                                    contentDescription = null,
                                    tint = if (isCustomSpeedOrPitch) accent else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            text = {
                                Column {
                                    Text("Speed & Pitch", fontWeight = FontWeight.Bold)
                                    Text(
                                        text = String.format(Locale.US, "%.2fx · Pitch %.2fx", playbackState.playbackSpeed, playbackState.playbackPitch),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                showOverflowMenu = false
                                onOpenSpeedPitch()
                            },
                            modifier = Modifier.testTag("menu_speed_pitch")
                        )

                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    if (isFavorite) MikaIcons.HeartBold else MikaIcons.HeartLinear,
                                    contentDescription = null,
                                    tint = if (isFavorite) accent else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                onToggleFavorite(song.id)
                            },
                            modifier = Modifier.testTag("menu_toggle_favorite")
                        )

                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(MikaIcons.Lyrics, contentDescription = null, modifier = Modifier.size(22.dp))
                            },
                            text = { Text("Full Lyrics Page", fontWeight = FontWeight.Bold) },
                            onClick = {
                                showOverflowMenu = false
                                onOpenFullLyrics()
                            },
                            modifier = Modifier.testTag("menu_full_lyrics")
                        )

                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(MikaIcons.Equalizer, contentDescription = null, modifier = Modifier.size(22.dp))
                            },
                            text = { Text("Equalizer & FX", fontWeight = FontWeight.Bold) },
                            onClick = {
                                showOverflowMenu = false
                                onOpenEqualizer()
                            }
                        )

                        HorizontalDivider()

                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(MikaIcons.TagEdit, contentDescription = null, modifier = Modifier.size(22.dp))
                            },
                            text = { Text("Edit Tags & Cover", fontWeight = FontWeight.Bold) },
                            onClick = {
                                showOverflowMenu = false
                                onEditTags(song)
                            },
                            modifier = Modifier.testTag("menu_edit_tags")
                        )

                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(MikaIcons.Mood, contentDescription = null, modifier = Modifier.size(22.dp))
                            },
                            text = { Text("Mood & Rating", fontWeight = FontWeight.Bold) },
                            onClick = {
                                showOverflowMenu = false
                                onRateAndMood(song)
                            },
                            modifier = Modifier.testTag("menu_rate_mood")
                        )

                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(MikaIcons.InfoCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                            },
                            text = { Text("Song Actions & Info", fontWeight = FontWeight.Bold) },
                            onClick = {
                                showOverflowMenu = false
                                onMoreClick(song)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Centerpiece: Large Individual Artwork (5 Styles) OR Synchronized Lyrics View
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val maxSquareSize = minOf(maxWidth, maxHeight) * 0.92f
                val artworkBorderWidth = fpConfig.artworkBorderDp.dp
                val artCorner = when (fpConfig.style) {
                    FullPlayerStyle.EDITORIAL -> 14.dp
                    FullPlayerStyle.IMMERSIVE -> 28.dp
                    else -> settings.cornerRadiusDp.coerceIn(6, 28).dp
                }
                val parallaxOffset by rememberParallaxOffset(settings.enableParallaxEffect)

                val glowPrimary = artworkAccentPair?.first ?: accent
                val glowSecondary = artworkAccentPair?.second ?: secondaryAccent
                val borderColor = lerp(accent, fgPrimary, 0.35f)

                val vinylRotation = if (fpConfig.style == FullPlayerStyle.VINYL && playbackState.isPlaying) {
                    val inf = rememberInfiniteTransition(label = "vinyl_turntable_spin")
                    inf.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 10000, easing = LinearEasing),
                            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
                        ),
                        label = "vinyl_angle"
                    ).value
                } else 0f

                if (showLyricsMode) {
                    LyricsPanelContainer(
                        lyrics = lyrics,
                        progressFlow = progressFlow,
                        accentColor = accent,
                        fgPrimary = fgPrimary,
                        fgSecondary = fgSecondary,
                        onSeekTo = onSeekTo,
                        onImportLyrics = onImportLyrics,
                        onOpenFullLyrics = onOpenFullLyrics,
                        onCloseLyrics = { showLyricsMode = false },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    var horizontalDrag by remember { mutableFloatStateOf(0f) }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (settings.enableGlowEffect) {
                            ArtworkReactiveGlow(
                                isPlaying = playbackState.isPlaying,
                                maxSquareSize = maxSquareSize,
                                glowPrimary = glowPrimary,
                                glowSecondary = glowSecondary
                            )
                        }

                        AnimatedContent(
                            targetState = song,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "full_player_artwork"
                        ) { activeSong ->
                            if (fpConfig.style == FullPlayerStyle.VINYL) {
                                // Vinyl Turntable Style
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(maxSquareSize)
                                        .aspectRatio(1f)
                                        .graphicsLayer {
                                            translationX = parallaxOffset.x
                                            translationY = parallaxOffset.y
                                        }
                                        .pointerInput(activeSong.id) {
                                            detectHorizontalDragGestures(
                                                onDragEnd = {
                                                    if (horizontalDrag < -90f) latestSkipNext()
                                                    else if (horizontalDrag > 90f) latestSkipPrevious()
                                                    horizontalDrag = 0f
                                                },
                                                onDragCancel = { horizontalDrag = 0f },
                                                onHorizontalDrag = { _, dragAmount -> horizontalDrag += dragAmount }
                                            )
                                        }
                                        .pointerInput(activeSong.id, settings.doubleTapToToggleLyrics) {
                                            detectTapGestures(
                                                onDoubleTap = if (settings.doubleTapToToggleLyrics) {
                                                    { showLyricsMode = !showLyricsMode }
                                                } else null,
                                                onTap = { executeArtworkGesture(settings.artworkTapAction, activeSong) },
                                                onLongPress = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    executeArtworkGesture(settings.artworkLongPressAction, activeSong)
                                                }
                                            )
                                        }
                                        .testTag("full_player_artwork")
                                ) {
                                    // Outer Vinyl Record Disc with grooves
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer { rotationZ = vinylRotation }
                                    ) {
                                        val r = size.minDimension / 2f
                                        drawCircle(color = Color(0xFF14161D), radius = r)
                                        for (ring in 1..6) {
                                            val ringRadius = r * (0.58f + ring * 0.06f)
                                            drawCircle(
                                                color = Color.White.copy(alpha = 0.07f),
                                                radius = ringRadius,
                                                style = Stroke(width = 1.dp.toPx())
                                            )
                                        }
                                        drawCircle(
                                            color = borderColor.copy(alpha = 0.7f),
                                            radius = r - 2.dp.toPx(),
                                            style = Stroke(width = 3.dp.toPx())
                                        )
                                    }
                                    // Inner Label Art
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(maxSquareSize * 0.56f)
                                            .clip(CircleShape)
                                            .border(3.dp, accent.copy(alpha = 0.8f), CircleShape)
                                            .graphicsLayer { rotationZ = vinylRotation }
                                    ) {
                                        TrackArtwork(
                                            song = activeSong,
                                            cornerRadius = 200.dp,
                                            crossfade = true,
                                            bitmapSizePx = 512,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF0A0C10))
                                                .border(2.dp, fgPrimary.copy(alpha = 0.75f), CircleShape)
                                        )
                                    }
                                }
                            } else {
                                val useActualRatio = settings.showActualArtworkRatio
                                val frameModifier = if (useActualRatio) {
                                    Modifier.fillMaxSize(0.92f)
                                } else {
                                    Modifier
                                        .size(maxSquareSize)
                                        .aspectRatio(1f)
                                }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = frameModifier
                                        .graphicsLayer {
                                            translationX = parallaxOffset.x
                                            translationY = parallaxOffset.y
                                        }
                                        .then(
                                            if (!useActualRatio) {
                                                Modifier
                                                    .shadow(22.dp, RoundedCornerShape(artCorner))
                                                    .clip(RoundedCornerShape(artCorner))
                                                    .border(artworkBorderWidth, borderColor.copy(alpha = 0.9f), RoundedCornerShape(artCorner))
                                            } else {
                                                Modifier.clip(RoundedCornerShape(artCorner))
                                            }
                                        )
                                        .pointerInput(activeSong.id) {
                                            detectHorizontalDragGestures(
                                                onDragEnd = {
                                                    if (horizontalDrag < -90f) latestSkipNext()
                                                    else if (horizontalDrag > 90f) latestSkipPrevious()
                                                    horizontalDrag = 0f
                                                },
                                                onDragCancel = { horizontalDrag = 0f },
                                                onHorizontalDrag = { _, dragAmount -> horizontalDrag += dragAmount }
                                            )
                                        }
                                        .pointerInput(
                                            activeSong.id,
                                            settings.doubleTapToToggleLyrics,
                                            settings.artworkTapAction,
                                            settings.artworkLongPressAction
                                        ) {
                                            detectTapGestures(
                                                onDoubleTap = if (settings.doubleTapToToggleLyrics) {
                                                    { showLyricsMode = !showLyricsMode }
                                                } else null,
                                                onTap = {
                                                    executeArtworkGesture(settings.artworkTapAction, activeSong)
                                                },
                                                onLongPress = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    executeArtworkGesture(settings.artworkLongPressAction, activeSong)
                                                }
                                            )
                                        }
                                        .testTag("full_player_artwork")
                                ) {
                                    TrackArtwork(
                                        song = activeSong,
                                        cornerRadius = artCorner,
                                        crossfade = true,
                                        bitmapSizePx = 640,
                                        preserveAspectRatio = useActualRatio,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Title & Artist Metadata Row
            val isEditorial = fpConfig.style == FullPlayerStyle.EDITORIAL
            val alignCenter = if (isEditorial) false else fpConfig.centerMetadata
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = if (alignCenter) Alignment.CenterHorizontally else Alignment.Start
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = fgPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = if (alignCenter) TextAlign.Center else TextAlign.Start
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = Formatters.formatArtistAndAlbum(song.artist, song.album),
                    style = MaterialTheme.typography.bodyMedium,
                    color = fgSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = if (alignCenter) TextAlign.Center else TextAlign.Start
                )
                Spacer(Modifier.height(6.dp))
                val hasMarks = song.rating > 0 || song.mood.isNotBlank()
                Surface(
                    onClick = { onRateAndMood(song) },
                    shape = RoundedCornerShape(50),
                    color = if (hasMarks) accent.copy(alpha = 0.18f) else fgPrimary.copy(alpha = 0.08f),
                    border = BorderStroke(
                        1.dp,
                        if (hasMarks) accent.copy(alpha = 0.48f) else fgPrimary.copy(alpha = 0.16f)
                    ),
                    modifier = Modifier.testTag("full_player_mood_rating_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (song.rating > 0) MikaIcons.StarFilled else MikaIcons.Mood,
                            contentDescription = "Mark mood and rating",
                            tint = if (hasMarks) accent else fgSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        val labelText = buildString {
                            if (song.rating > 0) append("${song.rating}/5")
                            if (song.rating > 0 && song.mood.isNotBlank()) append(" · ")
                            if (song.mood.isNotBlank()) append(song.mood)
                            if (isEmpty()) append("Mood & Rate")
                        }
                        Text(
                            text = labelText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (hasMarks) fgPrimary else fgSecondary,
                            maxLines = 1
                        )
                    }
                }
            }

            if (fpConfig.showQuickActionsRow && fpConfig.style != FullPlayerStyle.ZEN) {
                Spacer(Modifier.height(6.dp))

                // 5-Icon Quick Action Row: Lyrics > Equalizer > Sleep timer > Speed controller > Like button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .testTag("full_player_quick_actions"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showLyricsMode = !showLyricsMode },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("full_player_quick_lyrics")
                    ) {
                        Icon(
                            imageVector = MikaIcons.Lyrics,
                            contentDescription = "Toggle lyrics view",
                            tint = when {
                                showLyricsMode -> accent
                                lyrics != null -> fgPrimary
                                else -> fgSecondary
                            },
                            modifier = Modifier.size(mikaScaledIcon(24.dp))
                        )
                    }

                    IconButton(
                        onClick = onOpenEqualizer,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("full_player_quick_eq")
                    ) {
                        Icon(
                            imageVector = MikaIcons.Equalizer,
                            contentDescription = "Equalizer",
                            tint = if (isEqualizerEnabled) accent else fgSecondary,
                            modifier = Modifier.size(mikaScaledIcon(24.dp))
                        )
                    }

                    IconButton(
                        onClick = onOpenSleepTimer,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("full_player_quick_sleep")
                    ) {
                        Icon(
                            imageVector = MikaIcons.SleepTimer,
                            contentDescription = "Sleep timer",
                            tint = if (sleepTimerState.isActive) accent else fgSecondary,
                            modifier = Modifier.size(mikaScaledIcon(24.dp))
                        )
                    }

                    IconButton(
                        onClick = onOpenSpeedPitch,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("full_player_quick_speed")
                    ) {
                        Icon(
                            imageVector = MikaIcons.Speedometer,
                            contentDescription = "Speed & pitch",
                            tint = if (isCustomSpeedOrPitch) accent else fgSecondary,
                            modifier = Modifier.size(mikaScaledIcon(24.dp))
                        )
                    }

                    IconButton(
                        onClick = { onToggleFavorite(song.id) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("full_player_favorite")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) MikaIcons.HeartBold else MikaIcons.HeartLinear,
                            contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                            tint = if (isFavorite) accent else fgSecondary,
                            modifier = Modifier.size(mikaScaledIcon(26.dp))
                        )
                    }
                }
            }

            Spacer(Modifier.height(2.dp))

            val effectiveSeekBarStyle = when {
                settings.showLiveWaveformSeekbar -> SeekBarStyle.LIVE_FLOW_WAVEFORM
                settings.showActualWaveformSeekbar -> SeekBarStyle.REAL_WAVEFORM
                else -> settings.seekBarStyle
            }
            // Thicker Rounded Seekbar + Clean Timestamps (no skip buttons beside duration)
            FullPlayerProgressSection(
                progressFlow = progressFlow,
                seekBarStyle = effectiveSeekBarStyle,
                isPlaying = playbackState.isPlaying,
                trackWaveform = trackWaveform,
                liveWaveformHistory = liveWaveformHistory,
                accentColor = accent,
                secondaryColor = secondaryAccent,
                fgPrimary = fgPrimary,
                fgSecondary = fgSecondary,
                onSeekTo = onSeekTo
            )

            Spacer(Modifier.height(2.dp))

            // Primary Playback Controls Row (elevated higher for balanced ergonomics)
            val ctrlScale = fpConfig.controlsScale.coerceIn(0.82f, 1.18f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = ctrlScale
                        scaleY = ctrlScale
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (fpConfig.showShuffleRepeat && fpConfig.style != FullPlayerStyle.ZEN) {
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier
                            .size(50.dp)
                            .testTag("full_player_shuffle")
                    ) {
                        Icon(
                            imageVector = MikaIcons.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playbackState.shuffleEnabled) accent else fgSecondary,
                            modifier = Modifier.size(mikaScaledIcon(26.dp))
                        )
                    }
                }

                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier
                        .size(58.dp)
                        .testTag("full_player_prev")
                ) {
                    Icon(
                        imageVector = transportIcons.previous,
                        contentDescription = "Previous track",
                        tint = fgPrimary,
                        modifier = Modifier.size(mikaScaledIcon(38.dp))
                    )
                }

                Surface(
                    onClick = onTogglePlayPause,
                    shape = CircleShape,
                    color = accent,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .size(74.dp)
                        .scale(playButtonScale)
                        .testTag("full_player_play_pause")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) transportIcons.pause else transportIcons.play,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(mikaScaledIcon(38.dp))
                        )
                    }
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier
                        .size(58.dp)
                        .testTag("full_player_next")
                ) {
                    Icon(
                        imageVector = transportIcons.next,
                        contentDescription = "Next track",
                        tint = fgPrimary,
                        modifier = Modifier.size(mikaScaledIcon(38.dp))
                    )
                }

                if (fpConfig.showShuffleRepeat && fpConfig.style != FullPlayerStyle.ZEN) {
                    IconButton(
                        onClick = onCycleRepeat,
                        modifier = Modifier
                            .size(50.dp)
                            .testTag("full_player_repeat")
                    ) {
                        val (repeatIcon, repeatTint) = when (playbackState.repeatMode) {
                            RepeatMode.OFF -> MikaIcons.Repeat to fgSecondary
                            RepeatMode.ALL -> MikaIcons.Repeat to accent
                            RepeatMode.ONE -> MikaIcons.RepeatOne to accent
                        }
                        Icon(
                            imageVector = repeatIcon,
                            contentDescription = "Repeat mode",
                            tint = repeatTint,
                            modifier = Modifier.size(mikaScaledIcon(26.dp))
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            // Bottom Utility Bar: Playing Queue | Visualizer / Simple Line Lyrics Pill | Sound Control
            if (fpConfig.showBottomUtilityBar && fpConfig.style != FullPlayerStyle.ZEN) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onOpenQueue,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("full_player_queue")
                    ) {
                        Icon(
                            imageVector = MikaIcons.Queue,
                            contentDescription = "Playing queue",
                            tint = fgSecondary,
                            modifier = Modifier.size(mikaScaledIcon(25.dp))
                        )
                    }

                    if (fpConfig.showVisualizerPill) {
                        Box(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            FullPlayerVisualizerPill(
                                songId = song.id,
                                isPlaying = playbackState.isPlaying,
                                playbackSpeed = playbackState.playbackSpeed,
                                visualizerStyle = fpConfig.visualizerStyle,
                                showSimpleLyrics = fpConfig.showSimpleLyricsInVisualizer,
                                lyrics = lyrics,
                                progressFlow = progressFlow,
                                showLyricsMode = showLyricsMode,
                                accentColor = accent,
                                secondaryColor = secondaryAccent,
                                fgPrimary = fgPrimary,
                                fgSecondary = fgSecondary,
                                onClick = {
                                    if (fpConfig.showSimpleLyricsInVisualizer) {
                                        onOpenFullLyrics()
                                    } else {
                                        showLyricsMode = !showLyricsMode
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showVisualizerMenu = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            DropdownMenu(
                                expanded = showVisualizerMenu,
                                onDismissRequest = { showVisualizerMenu = false }
                            ) {
                                VisualizerStyle.entries.forEach { vStyle ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = vStyle.label,
                                                fontWeight = if (fpConfig.visualizerStyle == vStyle && !fpConfig.showSimpleLyricsInVisualizer) FontWeight.Bold else FontWeight.Medium,
                                                color = if (fpConfig.visualizerStyle == vStyle && !fpConfig.showSimpleLyricsInVisualizer) accent else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            showVisualizerMenu = false
                                            onUpdateFullPlayerConfig(
                                                fpConfig.copy(
                                                    visualizerStyle = vStyle,
                                                    showSimpleLyricsInVisualizer = false,
                                                    showVisualizerPill = true
                                                )
                                            )
                                        }
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (fpConfig.showSimpleLyricsInVisualizer) "Switch to Audio Visualizer" else "Simple Line Lyrics View",
                                            fontWeight = FontWeight.Bold,
                                            color = accent
                                        )
                                    },
                                    onClick = {
                                        showVisualizerMenu = false
                                        onUpdateFullPlayerConfig(
                                            fpConfig.copy(showSimpleLyricsInVisualizer = !fpConfig.showSimpleLyricsInVisualizer)
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Hide Visualizer", fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        showVisualizerMenu = false
                                        onUpdateFullPlayerConfig(fpConfig.copy(showVisualizerPill = false))
                                    }
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }

                    IconButton(
                        onClick = onOpenSoundControl,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("full_player_sound_control")
                    ) {
                        Icon(
                            imageVector = MikaIcons.SoundControl,
                            contentDescription = "Sound control",
                            tint = if (isCustomSpeedOrPitch || isEqualizerEnabled) accent else fgPrimary,
                            modifier = Modifier.size(mikaScaledIcon(25.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FullPlayerProgressSection(
    progressFlow: StateFlow<PlaybackProgressState>,
    seekBarStyle: SeekBarStyle,
    isPlaying: Boolean,
    trackWaveform: FloatArray,
    liveWaveformHistory: FloatArray,
    accentColor: Color,
    secondaryColor: Color,
    fgPrimary: Color,
    fgSecondary: Color,
    onSeekTo: (Long) -> Unit
) {
    val progressState by progressFlow.collectAsStateWithLifecycle()
    val totalMs = progressState.durationMs.coerceAtLeast(1L)
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderDraft by remember { mutableFloatStateOf(0f) }
    val currentFraction = if (isDraggingSlider) {
        sliderDraft
    } else {
        (progressState.positionMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    }

    MikaSeekBar(
        value = currentFraction,
        onValueChange = {
            isDraggingSlider = true
            sliderDraft = it
        },
        onValueChangeFinished = {
            onSeekTo((sliderDraft * totalMs).toLong())
            isDraggingSlider = false
        },
        seekBarStyle = seekBarStyle,
        isPlaying = isPlaying,
        trackWaveform = trackWaveform,
        liveWaveformHistory = liveWaveformHistory,
        accentColor = accentColor,
        secondaryColor = secondaryColor,
        inactiveTrackColor = fgPrimary.copy(alpha = 0.20f),
        thumbCenterColor = fgPrimary,
        trackHeight = 8.dp,
        thumbRadius = 10.dp,
        touchHeight = 40.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
            .testTag("full_player_slider")
    )

    Spacer(Modifier.height(2.dp))

    val shownPos = if (isDraggingSlider) (sliderDraft * totalMs).toLong() else progressState.positionMs
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = Formatters.formatDuration(shownPos),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = fgSecondary,
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .testTag("full_player_elapsed")
        )
        Text(
            text = Formatters.formatDuration(progressState.durationMs),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = fgSecondary,
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .testTag("full_player_total")
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FullPlayerVisualizerPill(
    songId: Long,
    isPlaying: Boolean,
    playbackSpeed: Float,
    visualizerStyle: VisualizerStyle,
    showSimpleLyrics: Boolean,
    lyrics: LyricsData?,
    progressFlow: StateFlow<PlaybackProgressState>,
    showLyricsMode: Boolean,
    accentColor: Color,
    secondaryColor: Color,
    fgPrimary: Color,
    fgSecondary: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spectrumBands by AudioSpectrumAnalyzer.bandsFlow.collectAsStateWithLifecycle()
    val progressState by progressFlow.collectAsStateWithLifecycle()
    val latestSpectrum by rememberUpdatedState(spectrumBands)
    val latestSpeed by rememberUpdatedState(playbackSpeed)

    val activeLyricText = remember(lyrics, progressState.positionMs) {
        if (lyrics == null || lyrics.lines.isEmpty()) {
            "No synced lyrics · Hold for styles"
        } else if (!lyrics.isSynced) {
            lyrics.lines.first().text
        } else {
            val idx = lyrics.lines.indexOfLast { it.timestampMs <= progressState.positionMs }.coerceAtLeast(0)
            lyrics.lines[idx].text
        }
    }

    val barSeeds = remember(songId) {
        val count = AudioSpectrumAnalyzer.BAND_COUNT
        FloatArray(count) { i ->
            val hash = ((songId * 31L) + (i * 17L)) xor (i.toLong() shl 3)
            0.28f + (abs(hash % 55) / 100f)
        }
    }

    val renderedBands = remember { FloatArray(AudioSpectrumAnalyzer.BAND_COUNT) { 0.18f } }
    var frameTick by remember { mutableIntStateOf(0) }
    var phase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying, songId, showSimpleLyrics) {
        if (showSimpleLyrics) return@LaunchedEffect
        var lastNanos = 0L
        while (isActive) {
            var maxIdleDelta = 0f
            withFrameNanos { nanos ->
                val dt = if (lastNanos == 0L) 0.016f else ((nanos - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastNanos = nanos

                val raw = latestSpectrum
                val hasReal = isPlaying && raw.any { it > 0.015f }
                if (isPlaying) {
                    phase += dt * 12.5f * latestSpeed.coerceIn(0.4f, 2.2f)
                }

                val count = AudioSpectrumAnalyzer.BAND_COUNT
                for (i in 0 until count) {
                    val target = when {
                        !isPlaying -> 0.15f + barSeeds[i] * 0.14f
                        hasReal -> {
                            val mirroredIdx = if (i < count / 2) {
                                (count / 2 - 1 - i) * 2
                            } else {
                                (i - count / 2) * 2 + 1
                            }.coerceIn(0, count - 1)
                            val realVal = raw[i] * 0.68f + raw[mirroredIdx] * 0.32f
                            (0.14f + realVal * 0.86f).coerceIn(0.14f, 1.0f)
                        }
                        else -> {
                            val s1 = sin(phase + i * 0.48f)
                            val s2 = sin(phase * 1.55f - i * 0.30f + barSeeds[i] * PI.toFloat())
                            (0.22f + 0.58f * ((s1 * 0.5f + s2 * 0.5f + 1f) * 0.5f))
                        }
                    }

                    val cur = renderedBands[i]
                    val rate = if (target > cur) 60f else 32f
                    val alpha = 1f - exp(-rate * dt)
                    val nextVal = cur + (target - cur) * alpha
                    renderedBands[i] = nextVal
                    if (!isPlaying) {
                        val d = abs(target - nextVal)
                        if (d > maxIdleDelta) maxIdleDelta = d
                    }
                }
                frameTick++
            }
            if (!isPlaying && maxIdleDelta < 0.002f) {
                break
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = if (showLyricsMode || showSimpleLyrics) accentColor.copy(alpha = 0.20f) else fgPrimary.copy(alpha = 0.09f),
        border = BorderStroke(
            width = 1.dp,
            color = if (showLyricsMode || showSimpleLyrics) accentColor.copy(alpha = 0.60f) else fgPrimary.copy(alpha = 0.16f)
        ),
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("full_player_visualizer_pill")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (showSimpleLyrics) {
                AnimatedContent(
                    targetState = activeLyricText,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                    label = "simple_line_lyric"
                ) { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = fgPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                ) {
                    @Suppress("UNUSED_EXPRESSION")
                    frameTick

                    val barCount = AudioSpectrumAnalyzer.BAND_COUNT
                    when (visualizerStyle) {
                        VisualizerStyle.BARS -> {
                            val slotWidth = size.width / barCount
                            val barWidth = (slotWidth * 0.56f).coerceIn(2.dp.toPx(), 4.dp.toPx())
                            val centerY = size.height / 2f
                            for (i in 0 until barCount) {
                                val centerBias = 1f - abs((i - (barCount - 1) / 2f) / (barCount / 2f)) * 0.22f
                                val amp = renderedBands[i]
                                val barHeight = (size.height * amp * centerBias).coerceIn(3.5.dp.toPx(), size.height)
                                val x = i * slotWidth + (slotWidth - barWidth) / 2f
                                val y = centerY - barHeight / 2f
                                val t = i.toFloat() / (barCount - 1).coerceAtLeast(1)
                                val col = if (isPlaying) lerp(accentColor, secondaryColor, t) else fgSecondary.copy(alpha = 0.45f)
                                drawRoundRect(
                                    color = col,
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                                )
                            }
                        }
                        VisualizerStyle.WAVEFORM -> {
                            val path = Path()
                            val step = size.width / (barCount - 1).coerceAtLeast(1)
                            val midY = size.height / 2f
                            for (i in 0 until barCount) {
                                val x = i * step
                                val sign = if (i % 2 == 0) 1f else -1f
                                val y = midY + sign * renderedBands[i] * (size.height * 0.44f)
                                if (i == 0) path.moveTo(x, y)
                                else {
                                    val prevX = (i - 1) * step
                                    val prevSign = if ((i - 1) % 2 == 0) 1f else -1f
                                    val prevY = midY + prevSign * renderedBands[i - 1] * (size.height * 0.44f)
                                    val cx = (prevX + x) / 2f
                                    path.cubicTo(cx, prevY, cx, y, x, y)
                                }
                            }
                            drawPath(
                                path = path,
                                brush = Brush.horizontalGradient(
                                    if (isPlaying) listOf(accentColor, secondaryColor)
                                    else listOf(fgSecondary.copy(alpha = 0.5f), fgSecondary.copy(alpha = 0.5f))
                                ),
                                style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        VisualizerStyle.CIRCULAR -> {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val baseR = size.height * 0.25f
                            for (i in 0 until 20) {
                                val angle = (i.toFloat() / 20f) * 2f * PI.toFloat()
                                val amp = renderedBands[i % barCount]
                                val len = baseR + amp * (size.height * 0.26f)
                                val p1 = Offset(cx + cos(angle) * baseR, cy + sin(angle) * baseR)
                                val p2 = Offset(cx + cos(angle) * len, cy + sin(angle) * len)
                                drawLine(
                                    color = if (isPlaying) lerp(accentColor, secondaryColor, i / 20f) else fgSecondary.copy(alpha = 0.45f),
                                    start = p1,
                                    end = p2,
                                    strokeWidth = 2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                        VisualizerStyle.MIRROR_PEAKS -> {
                            val half = barCount / 2
                            val slotW = size.width / barCount
                            val bw = (slotW * 0.5f).coerceIn(2.dp.toPx(), 4.dp.toPx())
                            for (i in 0 until barCount) {
                                val mirIdx = abs(i - half)
                                val amp = renderedBands[mirIdx.coerceIn(0, barCount - 1)]
                                val h = (size.height * amp).coerceIn(3.dp.toPx(), size.height)
                                val x = i * slotW + (slotW - bw) / 2f
                                drawRoundRect(
                                    color = if (isPlaying) lerp(accentColor, secondaryColor, mirIdx.toFloat() / half.coerceAtLeast(1)) else fgSecondary.copy(alpha = 0.45f),
                                    topLeft = Offset(x, size.height - h),
                                    size = Size(bw, h),
                                    cornerRadius = CornerRadius(bw / 2f, bw / 2f)
                                )
                            }
                        }
                        VisualizerStyle.DOT_MATRIX -> {
                            val cols = 16
                            val slotW = size.width / cols
                            for (c in 0 until cols) {
                                val amp = renderedBands[(c * 2) % barCount]
                                val dots = (1 + (amp * 3f).toInt()).coerceIn(1, 4)
                                val x = c * slotW + slotW / 2f
                                for (d in 0 until dots) {
                                    val y = size.height - (d + 0.6f) * (size.height / 4.2f)
                                    drawCircle(
                                        color = if (isPlaying) lerp(accentColor, secondaryColor, d / 3f) else fgSecondary.copy(alpha = 0.45f),
                                        radius = 2.1.dp.toPx(),
                                        center = Offset(x, y)
                                    )
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
private fun LyricsPanelContainer(
    lyrics: LyricsData?,
    progressFlow: StateFlow<PlaybackProgressState>,
    accentColor: Color,
    fgPrimary: Color,
    fgSecondary: Color,
    onSeekTo: (Long) -> Unit,
    onImportLyrics: () -> Unit,
    onOpenFullLyrics: () -> Unit,
    onCloseLyrics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressState by progressFlow.collectAsStateWithLifecycle()
    LyricsPanel(
        lyrics = lyrics,
        currentPositionMs = progressState.positionMs,
        accentColor = accentColor,
        fgPrimary = fgPrimary,
        fgSecondary = fgSecondary,
        onSeekTo = onSeekTo,
        onImportLyrics = onImportLyrics,
        onOpenFullLyrics = onOpenFullLyrics,
        onCloseLyrics = onCloseLyrics,
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LyricsPanel(
    lyrics: LyricsData?,
    currentPositionMs: Long,
    accentColor: Color,
    fgPrimary: Color,
    fgSecondary: Color,
    onSeekTo: (Long) -> Unit,
    onImportLyrics: () -> Unit,
    onOpenFullLyrics: () -> Unit,
    onCloseLyrics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var autoScrollEnabled by remember { mutableStateOf(true) }
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    val activeIndex = remember(lyrics, currentPositionMs) {
        if (lyrics == null || !lyrics.isSynced) -1
        else lyrics.lines.indexOfLast { it.timestampMs <= currentPositionMs }
    }

    LaunchedEffect(activeIndex, autoScrollEnabled) {
        if (autoScrollEnabled && activeIndex >= 0) {
            listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
        }
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenFullLyrics()
                }
            ),
        shape = RoundedCornerShape(22.dp),
        color = Color.Black.copy(alpha = 0.28f)
    ) {
        if (lyrics == null || lyrics.lines.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = MikaIcons.Lyrics,
                    contentDescription = null,
                    tint = fgSecondary,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "No embedded or local .lrc lyrics found",
                    style = MaterialTheme.typography.titleMedium,
                    color = fgPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Import an .lrc file or open the full Lyrics Studio to add/edit lyrics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = fgSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onImportLyrics) {
                        Icon(MikaIcons.BackupUpload, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import .lrc")
                    }
                    TextButton(onClick = onOpenFullLyrics) {
                        Text("Lyrics Studio", color = accentColor, fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = onCloseLyrics) {
                    Text("Show Artwork", color = fgPrimary)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 46.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(lyrics.lines, key = { idx, line -> "${idx}_${line.timestampMs}" }) { idx, line ->
                        val isActive = lyrics.isSynced && idx == activeIndex
                        Text(
                            text = line.text,
                            style = if (isActive) {
                                MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            } else {
                                MaterialTheme.typography.titleMedium
                            },
                            color = when {
                                isActive -> accentColor
                                !lyrics.isSynced -> fgPrimary.copy(alpha = 0.88f)
                                else -> fgSecondary.copy(alpha = 0.65f)
                            },
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(
                                    onClick = {
                                        if (lyrics.isSynced) {
                                            autoScrollEnabled = true
                                            onSeekTo(line.timestampMs)
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onOpenFullLyrics()
                                    }
                                )
                                .padding(vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (lyrics.isSynced) {
                        Surface(
                            onClick = { autoScrollEnabled = !autoScrollEnabled },
                            shape = RoundedCornerShape(50),
                            color = Color.Black.copy(alpha = 0.45f)
                        ) {
                            Text(
                                text = if (autoScrollEnabled) "Auto-sync ON" else "Manual",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (autoScrollEnabled) accentColor else Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                    Surface(
                        onClick = {
                            val joined = lyrics.lines.joinToString("\n") { it.text }
                            clipboardManager.setText(AnnotatedString(joined))
                        },
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.45f)
                    ) {
                        Icon(
                            MikaIcons.Copy,
                            contentDescription = "Copy all lyrics",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                                .size(16.dp)
                        )
                    }
                    Surface(
                        onClick = onOpenFullLyrics,
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.45f)
                    ) {
                        Text(
                            text = "Full View",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Surface(
                        onClick = onCloseLyrics,
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.45f)
                    ) {
                        Text(
                            text = "Artwork",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtworkReactiveGlow(
    isPlaying: Boolean,
    maxSquareSize: Dp,
    glowPrimary: Color,
    glowSecondary: Color
) {
    val beatEnergy by AudioSpectrumAnalyzer.beatEnergyFlow.collectAsStateWithLifecycle()
    val targetGlow = if (isPlaying) {
        (0.50f + beatEnergy * 0.50f).coerceIn(0f, 1f)
    } else {
        0f
    }
    val glowAlpha by animateFloatAsState(
        targetValue = targetGlow,
        animationSpec = tween(durationMillis = if (isPlaying) 140 else 420),
        label = "artwork_reactive_glow"
    )
    if (glowAlpha > 0.005f) {
        Canvas(
            modifier = Modifier
                .size(maxSquareSize * (1.20f + beatEnergy * 0.08f))
                .testTag("full_player_artwork_glow")
        ) {
            val radius = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowPrimary.copy(alpha = 0.72f * glowAlpha),
                        glowSecondary.copy(alpha = 0.42f * glowAlpha),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
    }
}
