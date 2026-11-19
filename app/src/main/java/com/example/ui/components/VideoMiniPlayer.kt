package com.example.ui.components

import android.view.SurfaceView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.VideoPlaybackUiState
import com.example.ui.icons.MikaIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Floating, draggable Picture-in-Picture mini video player window.
 */
@Composable
fun FloatingVideoPipOverlay(
    videoState: VideoPlaybackUiState,
    videoAspectRatio: Float,
    bottomNavVisible: Boolean,
    onAttachSurface: (SurfaceView) -> Unit,
    onDetachSurface: (SurfaceView) -> Unit,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val video = videoState.currentVideo ?: return
    val accent = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var showControls by remember(video.id) { mutableStateOf(true) }
    LaunchedEffect(showControls, videoState.isPlaying) {
        if (showControls && videoState.isPlaying) {
            delay(2800L)
            showControls = false
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val safeRatio = videoAspectRatio.coerceIn(0.85f, 2.1f)
        val pipWidthDp = 192.dp
        val pipHeightDp = (pipWidthDp / safeRatio).coerceIn(104.dp, 168.dp)

        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }
        val pipWidthPx = with(density) { pipWidthDp.toPx() }
        val pipHeightPx = with(density) { pipHeightDp.toPx() }
        val edgeMarginPx = with(density) { 14.dp.toPx() }
        val bottomReservePx = with(density) { (if (bottomNavVisible) 142.dp else 78.dp).toPx() }

        val minX = edgeMarginPx
        val maxX = (containerWidthPx - pipWidthPx - edgeMarginPx).coerceAtLeast(minX)
        val minY = edgeMarginPx
        val maxY = (containerHeightPx - pipHeightPx - edgeMarginPx).coerceAtLeast(minY)
        val defaultY = (containerHeightPx - pipHeightPx - bottomReservePx).coerceIn(minY, maxY)

        val offsetX = remember { Animatable(maxX) }
        val offsetY = remember { Animatable(defaultY) }

        LaunchedEffect(containerWidthPx, containerHeightPx) {
            offsetX.snapTo(offsetX.value.coerceIn(minX, maxX))
            offsetY.snapTo(offsetY.value.coerceIn(minY, maxY))
        }

        val shape = RoundedCornerShape(16.dp)
        Surface(
            shape = shape,
            shadowElevation = 18.dp,
            tonalElevation = 10.dp,
            color = Color.Black,
            border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.24f)),
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .width(pipWidthDp)
                .height(pipHeightDp)
                .clip(shape)
                .pointerInput(containerWidthPx, containerHeightPx) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                offsetX.snapTo((offsetX.value + dragAmount.x).coerceIn(minX, maxX))
                                offsetY.snapTo((offsetY.value + dragAmount.y).coerceIn(minY, maxY))
                            }
                        },
                        onDragEnd = {
                            val snapLeft = (offsetX.value - minX) < (maxX - offsetX.value)
                            val targetX = if (snapLeft) minX else maxX
                            scope.launch {
                                offsetX.animateTo(
                                    targetX,
                                    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                )
                            }
                        }
                    )
                }
                .clickable { showControls = !showControls }
                .testTag("video_pip_window")
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                var heldSurface by remember { mutableStateOf<SurfaceView?>(null) }
                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx).apply {
                            setZOrderMediaOverlay(true)
                            holder.setKeepScreenOn(true)
                            heldSurface = this
                            onAttachSurface(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                DisposableEffect(heldSurface, video.id) {
                    heldSurface?.let(onAttachSurface)
                    onDispose {
                        heldSurface?.let(onDetachSurface)
                    }
                }

                // Controls overlay on tap or pause
                AnimatedVisibility(
                    visible = showControls || !videoState.isPlaying,
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
                                        Color.Black.copy(alpha = 0.56f),
                                        Color.Black.copy(alpha = 0.22f),
                                        Color.Black.copy(alpha = 0.62f)
                                    )
                                )
                            )
                            .padding(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onExpand,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .testTag("video_pip_expand")
                            ) {
                                Icon(
                                    imageVector = MikaIcons.Fullscreen,
                                    contentDescription = "Expand video",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .testTag("video_pip_close")
                            ) {
                                Icon(
                                    imageVector = MikaIcons.Close,
                                    contentDescription = "Close video",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.90f))
                                .clickable(onClick = onTogglePlayPause)
                                .testTag("video_pip_play_pause")
                        ) {
                            Icon(
                                imageVector = if (videoState.isPlaying) MikaIcons.PauseBold else MikaIcons.PlayBold,
                                contentDescription = if (videoState.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White.copy(alpha = 0.92f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(bottom = 4.dp, end = 4.dp)
                        )
                    }
                }

                val progress = if (videoState.durationMs > 0L) {
                    (videoState.positionMs.toFloat() / videoState.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = accent,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
            }
        }
    }
}
