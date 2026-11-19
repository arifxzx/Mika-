package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun WaveformRulerDial(
    value: Int,
    range: IntRange,
    step: Int = 1,
    valueLabel: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    testTag: String? = null
) {
    val span = (range.last - range.first).coerceAtLeast(1)
    val clampedFraction = ((value - range.first).toFloat() / span.toFloat()).coerceIn(0f, 1f)
    val latestOnChange by rememberUpdatedState(onValueChange)
    var dragFraction by remember(value) { mutableFloatStateOf(clampedFraction) }

    val activeColor = MaterialTheme.colorScheme.onSurface
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)

    fun applyFraction(frac: Float) {
        val f = frac.coerceIn(0f, 1f)
        dragFraction = f
        val raw = range.first + (f * span)
        val stepped = ((raw - range.first) / step.toFloat()).roundToInt() * step + range.first
        val coerced = stepped.coerceIn(range.first, range.last)
        if (coerced != value) {
            latestOnChange(coerced)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Canvas(
            modifier = Modifier
                .width(76.dp)
                .height(26.dp)
                .pointerInput(range, step) {
                    detectTapGestures { offset ->
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        applyFraction(offset.x / w)
                    }
                }
                .pointerInput(range, step) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            applyFraction(offset.x / w)
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            applyFraction(change.position.x / w)
                        }
                    )
                }
        ) {
            val barCount = 15
            val centerX = dragFraction * size.width
            val barW = 2.dp.toPx()
            val spacing = (size.width - barCount * barW) / (barCount - 1).coerceAtLeast(1)

            for (i in 0 until barCount) {
                val x = i * (barW + spacing)
                val dist = abs((x + barW / 2f) - centerX) / size.width
                val isCenterHighlight = dist < 0.065f
                val patternFactor = when (i % 4) {
                    0 -> 0.52f
                    1 -> 0.72f
                    2 -> 0.44f
                    else -> 0.62f
                }
                val heightFraction = if (isCenterHighlight) 0.96f else patternFactor
                val barH = size.height * heightFraction
                val topY = (size.height - barH) / 2f
                val fill = when {
                    isCenterHighlight -> accentColor
                    x <= centerX -> activeColor.copy(alpha = 0.78f)
                    else -> mutedColor
                }
                drawRoundRect(
                    color = fill,
                    topLeft = Offset(x, topY),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                )
            }
        }

        Spacer(Modifier.height(3.dp))
        Text(
            text = valueLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
