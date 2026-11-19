package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.MiniPlayerConfig
import com.example.model.MiniPlayerStyle
import com.example.model.PlaybackIconStyle
import com.example.model.PlaybackProgressState
import com.example.model.PlaybackUiState
import com.example.ui.icons.MikaIcons
import com.example.ui.theme.LocalMikaColors
import com.example.ui.theme.mikaScaledIcon
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

@Composable
fun MiniPlayer(
    playbackState: PlaybackUiState,
    progressFlow: StateFlow<PlaybackProgressState>,
    artworkAccentPair: Pair<Color, Color>?,
    onOpenFullPlayer: () -> Unit,
    onDragExpansionDelta: (Float) -> Unit,
    onDragExpansionEnd: (Float) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleFavorite: (Long) -> Unit = {},
    miniPlayerConfig: MiniPlayerConfig = MiniPlayerConfig(),
    playbackIconStyle: PlaybackIconStyle = PlaybackIconStyle.NORMAL,
    applyNavigationBarsPadding: Boolean = true,
    forceTrackColors: Boolean = false,
    modifier: Modifier = Modifier
) {
    val song = playbackState.currentSong ?: return
    val mikaColors = LocalMikaColors.current
    val progressState by progressFlow.collectAsStateWithLifecycle()
    val progressFraction = if (progressState.durationMs > 0L) {
        (progressState.positionMs.toFloat() / progressState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val accent = if (forceTrackColors && artworkAccentPair != null) {
        artworkAccentPair.first
    } else {
        mikaColors.playerAccent
    }
    val secondaryAccent = if (forceTrackColors && artworkAccentPair != null) {
        artworkAccentPair.second
    } else {
        mikaColors.playerAccentSecondary
    }
    val transportIcons = MikaIcons.transportIcons(playbackIconStyle)

    val latestDragDelta by rememberUpdatedState(onDragExpansionDelta)
    val latestDragEnd by rememberUpdatedState(onDragExpansionEnd)
    val latestNext by rememberUpdatedState(onSkipNext)
    val latestPrev by rememberUpdatedState(onSkipPrevious)

    val backgroundBrush = when {
        forceTrackColors && artworkAccentPair != null -> {
            Brush.linearGradient(
                colors = listOf(
                    lerp(mikaColors.surfaceElevated, artworkAccentPair.first, 0.34f),
                    lerp(mikaColors.surfaceElevated, artworkAccentPair.second, 0.26f)
                )
            )
        }
        mikaColors.applyGradientToMiniPlayer -> {
            mikaColors.surfaceBrush(
                baseColor = mikaColors.surfaceElevated,
                overrideStart = artworkAccentPair?.first,
                overrideEnd = artworkAccentPair?.second,
                alphaScale = 0.88f
            )
        }
        else -> {
            Brush.linearGradient(listOf(mikaColors.surfaceElevated, mikaColors.surfaceElevated))
        }
    }

    val outerShape = when (miniPlayerConfig.style) {
        MiniPlayerStyle.FLOATING_PILL -> RoundedCornerShape(32.dp)
        MiniPlayerStyle.MINIMAL -> RoundedCornerShape(16.dp)
        else -> RoundedCornerShape(
            topStart = miniPlayerConfig.cornerRadiusDp.dp,
            topEnd = miniPlayerConfig.cornerRadiusDp.dp,
            bottomStart = if (miniPlayerConfig.style == MiniPlayerStyle.CLASSIC) 0.dp else 18.dp,
            bottomEnd = if (miniPlayerConfig.style == MiniPlayerStyle.CLASSIC) 0.dp else 18.dp
        )
    }

    val outerHorizontalPadding = when (miniPlayerConfig.style) {
        MiniPlayerStyle.FLOATING_PILL,
        MiniPlayerStyle.ARTWORK_BANNER,
        MiniPlayerStyle.VINYL_SPIN -> 10.dp
        MiniPlayerStyle.MINIMAL -> 12.dp
        MiniPlayerStyle.CLASSIC -> 0.dp
    }

    val isVinylStyle = miniPlayerConfig.style == MiniPlayerStyle.VINYL_SPIN
    val vinylSpinAngle = if (isVinylStyle && playbackState.isPlaying) {
        val infinite = rememberInfiniteTransition(label = "mini_vinyl_spin")
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 8500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "mini_vinyl_angle"
        ).value
    } else 0f

    val artworkSize = (miniPlayerConfig.artworkSizeDp + if (miniPlayerConfig.compactHeight) -6 else 4)
        .coerceIn(46, 68).dp
    val artworkRadius = if (isVinylStyle) artworkSize / 2 else miniPlayerConfig.artworkCornerDp.dp
    val verticalPad = if (miniPlayerConfig.compactHeight) 7.dp else 12.dp

    val borderStroke = BorderStroke(
        width = 1.dp,
        color = if (forceTrackColors && artworkAccentPair != null) {
            artworkAccentPair.first.copy(alpha = 0.42f)
        } else {
            mikaColors.glassBorder
        }
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (outerHorizontalPadding > 0.dp) {
                    Modifier.padding(horizontal = outerHorizontalPadding, vertical = 4.dp)
                } else Modifier
            )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(outerShape)
                .pointerInput(song.id, miniPlayerConfig.swipeToChangeTrack) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var totalX = 0f
                        var totalY = 0f
                        var axisLocked = 0 // 1 = horizontal, 2 = vertical
                        val velocityTracker = VelocityTracker()
                        velocityTracker.addPosition(down.uptimeMillis, down.position)
                        drag(down.id) { change ->
                            val delta = change.positionChange()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            totalX += delta.x
                            totalY += delta.y
                            if (axisLocked == 0 && (abs(totalX) > 4f || abs(totalY) > 4f)) {
                                axisLocked = if (abs(totalX) > abs(totalY) * 1.3f) 1 else 2
                            }
                            if (axisLocked == 2) {
                                change.consume()
                                latestDragDelta(delta.y)
                            } else if (axisLocked == 1 && miniPlayerConfig.swipeToChangeTrack) {
                                change.consume()
                            }
                        }
                        if (axisLocked == 1 && miniPlayerConfig.swipeToChangeTrack) {
                            if (totalX < -72f) latestNext()
                            else if (totalX > 72f) latestPrev()
                        } else if (axisLocked == 2) {
                            val vy = velocityTracker.calculateVelocity().y
                            latestDragEnd(vy)
                        }
                    }
                }
                .clickable(onClick = onOpenFullPlayer)
                .testTag("mini_player"),
            shape = outerShape,
            border = borderStroke,
            tonalElevation = 8.dp,
            shadowElevation = 14.dp,
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundBrush)
            ) {
                if (miniPlayerConfig.style == MiniPlayerStyle.ARTWORK_BANNER) {
                    TrackArtwork(
                        song = song,
                        cornerRadius = 0.dp,
                        bitmapSizePx = 256,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(0.24f)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        mikaColors.surfaceElevated.copy(alpha = 0.72f),
                                        mikaColors.surfaceElevated.copy(alpha = 0.56f),
                                        mikaColors.surfaceElevated.copy(alpha = 0.80f)
                                    )
                                )
                            )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (applyNavigationBarsPadding) Modifier.navigationBarsPadding() else Modifier)
                ) {
                    if (miniPlayerConfig.style == MiniPlayerStyle.MINIMAL && miniPlayerConfig.showProgressIndicator) {
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = accent,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 10.dp, top = verticalPad, bottom = verticalPad),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedContent(
                            targetState = song,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "mini_art"
                        ) { activeSong ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(artworkSize)
                                    .then(
                                        if (isVinylStyle) {
                                            Modifier
                                                .clip(CircleShape)
                                                .border(2.dp, accent.copy(alpha = 0.55f), CircleShape)
                                                .graphicsLayer { rotationZ = vinylSpinAngle }
                                        } else {
                                            Modifier
                                                .clip(RoundedCornerShape(artworkRadius))
                                                .border(1.dp, mikaColors.glassBorder, RoundedCornerShape(artworkRadius))
                                        }
                                    )
                            ) {
                                TrackArtwork(
                                    song = activeSong,
                                    cornerRadius = artworkRadius,
                                    bitmapSizePx = 192,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isVinylStyle) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(mikaColors.surfaceElevated)
                                            .border(1.5.dp, accent, CircleShape)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (miniPlayerConfig.showArtist) {
                                Text(
                                    text = song.artist.ifBlank { "Unknown Artist" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        if (miniPlayerConfig.showFavorite || miniPlayerConfig.style == MiniPlayerStyle.MINIMAL) {
                            IconButton(
                                onClick = { onToggleFavorite(song.id) },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("mini_player_favorite")
                            ) {
                                Icon(
                                    imageVector = if (song.isFavorite) MikaIcons.HeartBold else MikaIcons.HeartLinear,
                                    contentDescription = if (song.isFavorite) "Remove favorite" else "Add favorite",
                                    tint = if (song.isFavorite) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(mikaScaledIcon(22.dp))
                                )
                            }
                        }

                        if (miniPlayerConfig.showPrevNext && miniPlayerConfig.style != MiniPlayerStyle.MINIMAL) {
                            IconButton(
                                onClick = onSkipPrevious,
                                modifier = Modifier
                                    .size(46.dp)
                                    .testTag("mini_player_prev")
                            ) {
                                Icon(
                                    imageVector = transportIcons.previous,
                                    contentDescription = "Previous track",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(mikaScaledIcon(24.dp))
                                )
                            }
                        }

                        MiniPlayerPlayButton(
                            isPlaying = playbackState.isPlaying,
                            progress = progressFraction,
                            showRing = miniPlayerConfig.showProgressIndicator && miniPlayerConfig.style != MiniPlayerStyle.MINIMAL && miniPlayerConfig.style != MiniPlayerStyle.FLOATING_PILL,
                            accentColor = accent,
                            playIcon = transportIcons.play,
                            pauseIcon = transportIcons.pause,
                            onTogglePlayPause = onTogglePlayPause
                        )

                        if (miniPlayerConfig.showPrevNext && miniPlayerConfig.style != MiniPlayerStyle.MINIMAL) {
                            IconButton(
                                onClick = onSkipNext,
                                modifier = Modifier
                                    .size(46.dp)
                                    .testTag("mini_player_next")
                            ) {
                                Icon(
                                    imageVector = transportIcons.next,
                                    contentDescription = "Next track",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(mikaScaledIcon(24.dp))
                                )
                            }
                        }
                    }

                    if (miniPlayerConfig.style == MiniPlayerStyle.FLOATING_PILL && miniPlayerConfig.showProgressIndicator) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 4.dp)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction)
                                    .height(3.dp)
                                    .clip(CircleShape)
                                    .background(Brush.horizontalGradient(listOf(accent, secondaryAccent)))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerPlayButton(
    isPlaying: Boolean,
    progress: Float,
    showRing: Boolean,
    accentColor: Color,
    playIcon: androidx.compose.ui.graphics.vector.ImageVector,
    pauseIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onTogglePlayPause: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .clickable(onClick = onTogglePlayPause)
            .testTag("mini_player_play_pause")
    ) {
        if (showRing) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(50.dp),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                strokeWidth = 2.8.dp
            )
        }
        Icon(
            imageVector = if (isPlaying) pauseIcon else playIcon,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(mikaScaledIcon(30.dp))
        )
    }
}
