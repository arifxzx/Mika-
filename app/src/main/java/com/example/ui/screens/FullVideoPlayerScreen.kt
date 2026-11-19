package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.VideoPlaybackUiState
import com.example.model.VideoResizeMode
import com.example.ui.components.MikaSleekSlider
import com.example.ui.icons.MikaIcons
import com.example.util.Formatters
import java.util.Locale
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun FullVideoPlayerScreen(
    videoState: VideoPlaybackUiState,
    videoAspectRatio: Float,
    onMinimize: () -> Unit,
    onAttachSurface: (SurfaceView) -> Unit,
    onDetachSurface: (SurfaceView) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onCycleResizeMode: () -> Unit,
    onToggleLock: () -> Unit,
    onToggleMute: () -> Unit = {},
    onToggleLoop: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val video = videoState.currentVideo ?: return
    val accent = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
    val activity = LocalContext.current as? Activity

    var controlsVisible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var dragDownOffsetPx by remember { mutableFloatStateOf(0f) }
    var userZoomScale by remember { mutableFloatStateOf(1f) }
    var isLandscapeForced by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    BackHandler {
        if (videoState.isControlsLocked) {
            onToggleLock()
            controlsVisible = true
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            onMinimize()
        }
    }

    LaunchedEffect(controlsVisible, videoState.isPlaying, videoState.isControlsLocked, interactionTick) {
        if (controlsVisible && videoState.isPlaying && !showSpeedMenu) {
            delay(3200L)
            controlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(0, dragDownOffsetPx.roundToInt()) }
            .background(Color.Black)
            .pointerInput(videoState.isControlsLocked) {
                detectTapGestures(
                    onTap = {
                        interactionTick++
                        controlsVisible = !controlsVisible
                    },
                    onDoubleTap = { pos ->
                        if (!videoState.isControlsLocked) {
                            interactionTick++
                            if (pos.x < size.width * 0.45f) {
                                onSeekRelative(-10_000L)
                            } else if (pos.x > size.width * 0.55f) {
                                onSeekRelative(10_000L)
                            } else {
                                onTogglePlayPause()
                            }
                            controlsVisible = true
                        }
                    }
                )
            }
            .pointerInput(videoState.isControlsLocked) {
                detectVerticalDragGestures(
                    onDragStart = { dragDownOffsetPx = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        if (!videoState.isControlsLocked) {
                            dragDownOffsetPx = (dragDownOffsetPx + dragAmount).coerceAtLeast(0f)
                        }
                    },
                    onDragEnd = {
                        val threshold = with(density) { 42.dp.toPx() }
                        if (!videoState.isControlsLocked && dragDownOffsetPx > threshold) {
                            dragDownOffsetPx = 0f
                            onMinimize()
                        } else {
                            dragDownOffsetPx = 0f
                        }
                    },
                    onDragCancel = { dragDownOffsetPx = 0f }
                )
            }
            .testTag("full_video_player_screen")
    ) {
        // Video Surface sized accurately according to resizeMode + pinch zoom
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(videoState.isControlsLocked) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (!videoState.isControlsLocked && kotlin.math.abs(zoom - 1f) > 0.01f) {
                            userZoomScale = (userZoomScale * zoom).coerceIn(1f, 3f)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val maxW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val maxH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
            val safeRatio = videoAspectRatio.coerceIn(0.25f, 4.0f)
            val containerRatio = maxW / maxH

            val (targetW, targetH) = when (videoState.resizeMode) {
                VideoResizeMode.FIT -> {
                    if (containerRatio > safeRatio) {
                        (maxH * safeRatio) to maxH
                    } else {
                        maxW to (maxW / safeRatio)
                    }
                }
                VideoResizeMode.FILL -> {
                    if (containerRatio > safeRatio) {
                        maxW to (maxW / safeRatio)
                    } else {
                        (maxH * safeRatio) to maxH
                    }
                }
                VideoResizeMode.ZOOM -> {
                    val baseW = if (containerRatio > safeRatio) maxW else (maxH * safeRatio)
                    val baseH = if (containerRatio > safeRatio) (maxW / safeRatio) else maxH
                    (baseW * 1.15f) to (baseH * 1.15f)
                }
                VideoResizeMode.STRETCH -> maxW to maxH
            }

            val wDp = with(density) { targetW.toDp() }
            val hDp = with(density) { targetH.toDp() }

            val surfaceHolder = remember { mutableStateOf<SurfaceView?>(null) }
            DisposableEffect(Unit) {
                onDispose {
                    surfaceHolder.value?.let(onDetachSurface)
                }
            }

            AndroidView(
                factory = { ctx ->
                    SurfaceView(ctx).also { sv ->
                        surfaceHolder.value = sv
                        onAttachSurface(sv)
                    }
                },
                update = { sv ->
                    if (surfaceHolder.value !== sv) {
                        surfaceHolder.value = sv
                        onAttachSurface(sv)
                    }
                },
                modifier = Modifier
                    .size(width = wDp, height = hDp)
                    .graphicsLayer(
                        scaleX = userZoomScale,
                        scaleY = userZoomScale
                    )
            )
        }

        // Locked Screen Floating Unlock Button
        AnimatedVisibility(
            visible = videoState.isControlsLocked && controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
        ) {
            Surface(
                onClick = {
                    interactionTick++
                    onToggleLock()
                    controlsVisible = true
                },
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.72f),
                modifier = Modifier.testTag("video_unlock_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(MikaIcons.Unlock, contentDescription = "Unlock screen", tint = Color.White, modifier = Modifier.size(20.dp))
                    Text("Unlock", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Unlocked Controls Overlay
        AnimatedVisibility(
            visible = !videoState.isControlsLocked && controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.72f),
                                Color.Black.copy(alpha = 0.18f),
                                Color.Black.copy(alpha = 0.78f)
                            )
                        )
                    )
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onMinimize,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("video_minimize_button")
                    ) {
                        Icon(
                            imageVector = MikaIcons.ArrowDown,
                            contentDescription = "Minimize video",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (video.width > 0 && video.height > 0) {
                            Text(
                                text = "${video.width}×${video.height}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.74f)
                            )
                        }
                    }

                    // Speed Selector
                    Box {
                        Surface(
                            onClick = {
                                interactionTick++
                                showSpeedMenu = true
                            },
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.2gx", videoState.playbackSpeed),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false }
                        ) {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${spd}x",
                                            fontWeight = FontWeight.Bold,
                                            color = if (kotlin.math.abs(videoState.playbackSpeed - spd) < 0.02f) {
                                                accent
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    },
                                    onClick = {
                                        showSpeedMenu = false
                                        interactionTick++
                                        onSetSpeed(spd)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            interactionTick++
                            isLandscapeForced = !isLandscapeForced
                            activity?.requestedOrientation = if (isLandscapeForced) {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            }
                        },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = MikaIcons.Refresh,
                            contentDescription = "Rotate orientation",
                            tint = if (isLandscapeForced) accent else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            interactionTick++
                            userZoomScale = 1f
                            onCycleResizeMode()
                        },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = MikaIcons.AspectRatio,
                            contentDescription = "Aspect mode ${videoState.resizeMode.label}",
                            tint = Color.White,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            interactionTick++
                            onToggleLock()
                        },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = MikaIcons.Lock,
                            contentDescription = "Lock controls",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Center Playback Controls
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            interactionTick++
                            onSkipPrevious()
                        },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(MikaIcons.PreviousBold, contentDescription = "Previous video", tint = Color.White, modifier = Modifier.size(30.dp))
                    }

                    IconButton(
                        onClick = {
                            interactionTick++
                            onSeekRelative(-10_000L)
                        },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(MikaIcons.Replay, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    Surface(
                        onClick = {
                            interactionTick++
                            onTogglePlayPause()
                        },
                        shape = CircleShape,
                        color = accent,
                        modifier = Modifier
                            .size(68.dp)
                            .testTag("video_full_play_pause")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (videoState.isPlaying) MikaIcons.PauseBold else MikaIcons.PlayBold,
                                contentDescription = if (videoState.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            interactionTick++
                            onSeekRelative(10_000L)
                        },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(MikaIcons.Forward, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    IconButton(
                        onClick = {
                            interactionTick++
                            onSkipNext()
                        },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(MikaIcons.NextBold, contentDescription = "Next video", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }

                // Bottom Seekbar & Duration
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val total = videoState.durationMs.coerceAtLeast(1L)
                    var isDragging by remember { mutableStateOf(false) }
                    var draftFraction by remember { mutableFloatStateOf(0f) }
                    val sliderVal = if (isDragging) {
                        draftFraction
                    } else {
                        (videoState.positionMs.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    }

                    MikaSleekSlider(
                        value = sliderVal,
                        onValueChange = {
                            interactionTick++
                            isDragging = true
                            draftFraction = it
                        },
                        onValueChangeFinished = {
                            interactionTick++
                            onSeekTo((draftFraction * total).toLong())
                            isDragging = false
                        },
                        accentColor = accent,
                        secondaryColor = accent.copy(alpha = 0.7f),
                        inactiveTrackColor = Color.White.copy(alpha = 0.28f),
                        thumbCenterColor = Color.White,
                        trackHeight = 6.dp,
                        thumbRadius = 9.dp,
                        touchHeight = 36.dp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val shownPos = if (isDragging) (draftFraction * total).toLong() else videoState.positionMs
                        Text(
                            text = "${Formatters.formatDuration(shownPos)} / ${Formatters.formatDuration(videoState.durationMs)}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    interactionTick++
                                    onToggleMute()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (videoState.isMuted) MikaIcons.VolumeMute else MikaIcons.VolumeHigh,
                                    contentDescription = if (videoState.isMuted) "Unmute" else "Mute",
                                    tint = if (videoState.isMuted) accent else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    interactionTick++
                                    onToggleLoop()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = MikaIcons.RepeatOne,
                                    contentDescription = "Toggle loop",
                                    tint = if (videoState.isLooping) accent else Color.White.copy(alpha = 0.72f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (userZoomScale > 1.05f) {
                                Surface(
                                    onClick = {
                                        interactionTick++
                                        userZoomScale = 1f
                                    },
                                    shape = RoundedCornerShape(50),
                                    color = accent.copy(alpha = 0.85f)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%.1fx", userZoomScale),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Surface(
                                onClick = {
                                    interactionTick++
                                    userZoomScale = 1f
                                    onCycleResizeMode()
                                },
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = videoState.resizeMode.label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
