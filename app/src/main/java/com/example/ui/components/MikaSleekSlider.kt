package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.SeekBarStyle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Custom polished Material 3 / Mika capsule slider with:
 * - Rounded gradient active capsule track
 * - Animated soft halo + thumb ring while dragging
 * - Instant tap-to-seek and jitter-free horizontal drag tracking
 * - 36-48dp comfortable touch area with sleek visual track height
 */
@Composable
fun MikaSleekSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = accentColor,
    inactiveTrackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
    thumbCenterColor: Color = Color.White,
    trackHeight: Dp = 4.dp,
    thumbRadius: Dp = 6.5.dp,
    touchHeight: Dp = 36.dp
) {
    MikaSeekBar(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        onValueChangeFinished = onValueChangeFinished,
        seekBarStyle = SeekBarStyle.SLEEK_CAPSULE,
        isPlaying = false,
        accentColor = accentColor,
        secondaryColor = secondaryColor,
        inactiveTrackColor = inactiveTrackColor,
        thumbCenterColor = thumbCenterColor,
        trackHeight = trackHeight,
        thumbRadius = thumbRadius,
        touchHeight = touchHeight
    )
}

/**
 * Multi-style interactive seek bar supporting 7 distinct visual designs:
 * 1. SLEEK_CAPSULE (default)
 * 2. WAVEFORM_BARS
 * 3. REAL_WAVEFORM (extracted track peaks)
 * 4. LIVE_FLOW_WAVEFORM (Poweramp-style bars flowing right to left)
 * 5. WAVY_PULSE
 * 6. MINIMAL_GLOW
 * 7. TICK_RULER
 */
@Composable
fun MikaSeekBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    seekBarStyle: SeekBarStyle = SeekBarStyle.SLEEK_CAPSULE,
    isPlaying: Boolean = false,
    trackWaveform: FloatArray = FloatArray(0),
    liveWaveformHistory: FloatArray = FloatArray(0),
    accentColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = accentColor,
    inactiveTrackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
    thumbCenterColor: Color = Color.White,
    trackHeight: Dp = 4.dp,
    thumbRadius: Dp = 6.5.dp,
    touchHeight: Dp = 40.dp
) {
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val clampedValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
    val fraction = ((clampedValue - valueRange.start) / span).coerceIn(0f, 1f)

    var isPressed by remember { mutableStateOf(false) }
    val animatedThumbRadius by animateDpAsState(
        targetValue = if (isPressed) thumbRadius + 2.5.dp else thumbRadius,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "seek_thumb_radius"
    )
    val haloAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.30f else 0.14f,
        label = "seek_thumb_halo"
    )

    val phaseTransition = rememberInfiniteTransition(label = "wavy_seek_phase")
    val wavePhase by phaseTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1650, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "seek_wave_phase"
    )
    val animatedAmplitude by animateFloatAsState(
        targetValue = if (isPlaying || isPressed) 1f else 0.48f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "wavy_amp"
    )
    val effectivePhase = if (isPlaying && (seekBarStyle == SeekBarStyle.WAVY_PULSE || seekBarStyle == SeekBarStyle.LIVE_FLOW_WAVEFORM)) {
        wavePhase
    } else 0f

    val fallbackWaveform = remember {
        floatArrayOf(
            0.32f, 0.55f, 0.80f, 0.48f, 0.94f, 0.65f, 0.42f, 0.88f,
            0.70f, 0.38f, 0.62f, 0.95f, 0.76f, 0.45f, 0.84f, 0.56f,
            0.35f, 0.72f, 0.90f, 0.60f, 0.44f, 0.78f, 0.52f, 0.86f,
            0.66f, 0.40f, 0.74f, 0.92f, 0.54f, 0.36f, 0.68f, 0.48f,
            0.58f, 0.82f, 0.46f, 0.76f, 0.62f, 0.89f, 0.50f, 0.38f,
            0.71f, 0.85f, 0.43f, 0.64f, 0.78f, 0.51f, 0.67f, 0.40f
        )
    }

    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val latestOnFinished by rememberUpdatedState(onValueChangeFinished)
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(touchHeight)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = clampedValue,
                    range = valueRange
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val totalWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val horizontalPadPx = with(density) { (thumbRadius + 4.dp).toPx() }
        val usableWidthPx = (totalWidthPx - horizontalPadPx * 2f).coerceAtLeast(1f)

        fun fractionFromX(x: Float): Float {
            return ((x - horizontalPadPx) / usableWidthPx).coerceIn(0f, 1f)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(valueRange) {
                    detectTapGestures { offset ->
                        val f = fractionFromX(offset.x)
                        latestOnValueChange(valueRange.start + f * span)
                        latestOnFinished?.invoke()
                    }
                }
                .pointerInput(valueRange) {
                    detectHorizontalDragGestures(
                        onDragStart = { startOffset ->
                            isPressed = true
                            val f = fractionFromX(startOffset.x)
                            latestOnValueChange(valueRange.start + f * span)
                        },
                        onDragEnd = {
                            isPressed = false
                            latestOnFinished?.invoke()
                        },
                        onDragCancel = {
                            isPressed = false
                            latestOnFinished?.invoke()
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val f = fractionFromX(change.position.x)
                            latestOnValueChange(valueRange.start + f * span)
                        }
                    )
                }
        ) {
            val centerY = size.height / 2f
            val startX = horizontalPadPx
            val endX = (size.width - horizontalPadPx).coerceAtLeast(startX + 1f)
            val usableW = endX - startX
            val thumbX = startX + usableW * fraction
            val rPx = animatedThumbRadius.toPx()

            when (seekBarStyle) {
                SeekBarStyle.SLEEK_CAPSULE -> {
                    val trackH = trackHeight.toPx()
                    drawRoundRect(
                        color = inactiveTrackColor,
                        topLeft = Offset(startX, centerY - trackH / 2f),
                        size = Size(usableW, trackH),
                        cornerRadius = CornerRadius(trackH / 2f, trackH / 2f)
                    )
                    val activeW = (thumbX - startX).coerceAtLeast(0f)
                    if (activeW > 0.5f) {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(accentColor, secondaryColor),
                                startX = startX,
                                endX = endX
                            ),
                            topLeft = Offset(startX, centerY - trackH / 2f),
                            size = Size(activeW, trackH),
                            cornerRadius = CornerRadius(trackH / 2f, trackH / 2f)
                        )
                    }
                    drawCircle(color = accentColor.copy(alpha = haloAlpha), radius = rPx * 1.95f, center = Offset(thumbX, centerY))
                    drawCircle(color = accentColor, radius = rPx, center = Offset(thumbX, centerY))
                    drawCircle(color = thumbCenterColor, radius = rPx * 0.54f, center = Offset(thumbX, centerY))
                }

                SeekBarStyle.WAVEFORM_BARS,
                SeekBarStyle.REAL_WAVEFORM -> {
                    val bars = if (seekBarStyle == SeekBarStyle.REAL_WAVEFORM && trackWaveform.isNotEmpty()) {
                        trackWaveform
                    } else {
                        fallbackWaveform
                    }
                    val barCount = bars.size
                    val step = usableW / barCount.toFloat()
                    val barW = (step * 0.58f).coerceIn(1.8.dp.toPx(), 4.2.dp.toPx())
                    val maxBarH = (size.height * (if (seekBarStyle == SeekBarStyle.REAL_WAVEFORM) 0.74f else 0.62f))
                        .coerceAtLeast(14.dp.toPx())
                    for (i in 0 until barCount) {
                        val barCenterX = startX + (i + 0.5f) * step
                        val barFraction = (i + 0.5f) / barCount.toFloat()
                        val amplitude = bars[i].coerceIn(0.12f, 1.0f)
                        val barH = (maxBarH * amplitude).coerceAtLeast(3.5.dp.toPx())
                        val isPast = barFraction <= fraction
                        val nearThumb = abs(barFraction - fraction) < (1.5f / barCount)
                        val brush = if (isPast) {
                            Brush.verticalGradient(listOf(secondaryColor, accentColor))
                        } else {
                            Brush.verticalGradient(listOf(inactiveTrackColor, inactiveTrackColor))
                        }
                        val boost = if (nearThumb && isPressed) 1.16f else 1f
                        val finalH = (barH * boost).coerceAtMost(size.height * 0.88f)
                        drawRoundRect(
                            brush = brush,
                            topLeft = Offset(barCenterX - barW / 2f, centerY - finalH / 2f),
                            size = Size(barW, finalH),
                            cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                        )
                    }
                    val needleH = (size.height * 0.72f).coerceAtLeast(18.dp.toPx())
                    drawRoundRect(
                        color = accentColor.copy(alpha = haloAlpha),
                        topLeft = Offset(thumbX - 4.5.dp.toPx(), centerY - needleH / 2f - 2.dp.toPx()),
                        size = Size(9.dp.toPx(), needleH + 4.dp.toPx()),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                    drawRoundRect(
                        color = thumbCenterColor,
                        topLeft = Offset(thumbX - 1.8.dp.toPx(), centerY - needleH / 2f),
                        size = Size(3.6.dp.toPx(), needleH),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }

                SeekBarStyle.LIVE_FLOW_WAVEFORM -> {
                    val liveCount = 44
                    val step = usableW / liveCount.toFloat()
                    val barW = (step * 0.62f).coerceIn(2.dp.toPx(), 4.5.dp.toPx())
                    val maxBarH = (size.height * 0.78f).coerceAtLeast(16.dp.toPx())
                    val hasLiveSamples = liveWaveformHistory.size >= liveCount && liveWaveformHistory.any { it > 0.08f }

                    // Subtle progress baseline underneath live flowing bars
                    val railH = 2.dp.toPx()
                    drawRoundRect(
                        color = inactiveTrackColor.copy(alpha = 0.45f),
                        topLeft = Offset(startX, centerY - railH / 2f),
                        size = Size(usableW, railH),
                        cornerRadius = CornerRadius(railH / 2f, railH / 2f)
                    )

                    for (i in 0 until liveCount) {
                        val barCenterX = startX + (i + 0.5f) * step
                        val barFraction = (i + 0.5f) / liveCount.toFloat()
                        val rawAmp = if (hasLiveSamples) {
                            liveWaveformHistory[i]
                        } else if (isPlaying) {
                            val scrollWave = sin(i * 0.42f + effectivePhase * 2f) * 0.28f +
                                sin(i * 0.85f + effectivePhase * 3.1f) * 0.18f
                            (0.42f + scrollWave).coerceIn(0.14f, 0.92f)
                        } else {
                            fallbackWaveform[i % fallbackWaveform.size] * 0.45f
                        }
                        // Edge taper so bars glide cleanly into & out of left/right bounds
                        val edgeTaper = sin(barFraction * PI.toFloat()).coerceIn(0.45f, 1f)
                        val barH = (maxBarH * rawAmp * edgeTaper).coerceAtLeast(3.5.dp.toPx())
                        val isPast = barFraction <= fraction
                        val barBrush = if (isPast) {
                            Brush.verticalGradient(listOf(secondaryColor, accentColor))
                        } else {
                            Brush.verticalGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.26f),
                                    inactiveTrackColor.copy(alpha = 0.72f)
                                )
                            )
                        }
                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(barCenterX - barW / 2f, centerY - barH / 2f),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                        )
                    }

                    // Crisp floating playhead indicator
                    val needleH = (size.height * 0.74f).coerceAtLeast(20.dp.toPx())
                    drawRoundRect(
                        color = accentColor.copy(alpha = haloAlpha + 0.08f),
                        topLeft = Offset(thumbX - 4.5.dp.toPx(), centerY - needleH / 2f - 2.dp.toPx()),
                        size = Size(9.dp.toPx(), needleH + 4.dp.toPx()),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                    drawRoundRect(
                        color = thumbCenterColor,
                        topLeft = Offset(thumbX - 1.8.dp.toPx(), centerY - needleH / 2f),
                        size = Size(3.6.dp.toPx(), needleH),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }

                SeekBarStyle.WAVY_PULSE -> {
                    val strokeW = (trackHeight * 0.92f).toPx().coerceAtLeast(2.8.dp.toPx())
                    if (thumbX < endX - 1f) {
                        drawLine(
                            color = inactiveTrackColor,
                            start = Offset(thumbX, centerY),
                            end = Offset(endX, centerY),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                    }
                    val activeW = (thumbX - startX).coerceAtLeast(0f)
                    if (activeW > 1f) {
                        val amp = 4.6.dp.toPx() * animatedAmplitude
                        val wavelength = 24.dp.toPx()
                        val path = Path()
                        val steps = (activeW / 2.5f).toInt().coerceIn(10, 140)
                        for (s in 0..steps) {
                            val t = s.toFloat() / steps.toFloat()
                            val px = startX + activeW * t
                            val edgeTaper = sin(t * PI).toFloat().coerceIn(0.18f, 1f)
                            val primaryWave = sin((px / wavelength) * 2f * PI.toFloat() - effectivePhase)
                            val harmonicWave = 0.22f * sin((px / (wavelength * 0.55f)) * 2f * PI.toFloat() - effectivePhase * 1.5f)
                            val py = centerY + (primaryWave + harmonicWave) * amp * edgeTaper
                            if (s == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        drawPath(
                            path = path,
                            brush = Brush.horizontalGradient(listOf(accentColor, secondaryColor)),
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                    }
                    val pillH = if (isPressed) 19.dp.toPx() else 15.dp.toPx()
                    drawCircle(color = accentColor.copy(alpha = haloAlpha), radius = rPx * 1.85f, center = Offset(thumbX, centerY))
                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(thumbX - 3.dp.toPx(), centerY - pillH / 2f),
                        size = Size(6.dp.toPx(), pillH),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                    drawRoundRect(
                        color = thumbCenterColor,
                        topLeft = Offset(thumbX - 1.4.dp.toPx(), centerY - pillH * 0.34f),
                        size = Size(2.8.dp.toPx(), pillH * 0.68f),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }

                SeekBarStyle.MINIMAL_GLOW -> {
                    val beamH = 2.4.dp.toPx()
                    drawRoundRect(
                        color = inactiveTrackColor,
                        topLeft = Offset(startX, centerY - beamH / 2f),
                        size = Size(usableW, beamH),
                        cornerRadius = CornerRadius(beamH / 2f, beamH / 2f)
                    )
                    val activeW = (thumbX - startX).coerceAtLeast(0f)
                    if (activeW > 0.5f) {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(accentColor.copy(alpha = 0.25f), secondaryColor.copy(alpha = 0.36f)),
                                startX = startX,
                                endX = endX
                            ),
                            topLeft = Offset(startX, centerY - beamH * 1.9f),
                            size = Size(activeW, beamH * 3.8f),
                            cornerRadius = CornerRadius(beamH * 2f, beamH * 2f)
                        )
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(accentColor, secondaryColor),
                                startX = startX,
                                endX = endX
                            ),
                            topLeft = Offset(startX, centerY - beamH / 2f),
                            size = Size(activeW, beamH),
                            cornerRadius = CornerRadius(beamH / 2f, beamH / 2f)
                        )
                    }
                    drawCircle(color = accentColor.copy(alpha = haloAlpha + 0.08f), radius = rPx * 2.1f, center = Offset(thumbX, centerY))
                    drawCircle(color = thumbCenterColor, radius = rPx * 0.78f, center = Offset(thumbX, centerY))
                    drawCircle(
                        color = accentColor,
                        radius = rPx * 0.92f,
                        center = Offset(thumbX, centerY),
                        style = Stroke(width = 2.2.dp.toPx())
                    )
                }

                SeekBarStyle.TICK_RULER -> {
                    val tickCount = 33
                    val step = usableW / (tickCount - 1).toFloat()
                    for (i in 0 until tickCount) {
                        val tx = startX + i * step
                        val tickFrac = i.toFloat() / (tickCount - 1).toFloat()
                        val isMajor = i % 4 == 0
                        val tickH = if (isMajor) 12.dp.toPx() else 6.5.dp.toPx()
                        val isActive = tickFrac <= fraction
                        drawLine(
                            color = if (isActive) {
                                if (isMajor) secondaryColor else accentColor
                            } else {
                                inactiveTrackColor
                            },
                            start = Offset(tx, centerY - tickH / 2f),
                            end = Offset(tx, centerY + tickH / 2f),
                            strokeWidth = if (isMajor) 2.2.dp.toPx() else 1.4.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    val indicatorH = if (isPressed) 20.dp.toPx() else 16.dp.toPx()
                    drawRoundRect(
                        color = accentColor.copy(alpha = haloAlpha),
                        topLeft = Offset(thumbX - 5.dp.toPx(), centerY - indicatorH / 2f - 2.dp.toPx()),
                        size = Size(10.dp.toPx(), indicatorH + 4.dp.toPx()),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(secondaryColor, accentColor)),
                        topLeft = Offset(thumbX - 2.5.dp.toPx(), centerY - indicatorH / 2f),
                        size = Size(5.dp.toPx(), indicatorH),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                }
            }
        }
    }
}
