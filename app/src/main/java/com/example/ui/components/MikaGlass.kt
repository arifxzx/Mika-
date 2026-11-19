package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalMikaColors

/**
 * Applies genuine RenderEffect blur on Android 12+ (API 31+) while remaining a crisp no-op
 * on older OS levels where translucent glass surfaces provide the frosted look cleanly.
 */
fun Modifier.mikaFrostedBlur(
    radius: Dp = 18.dp,
    enabled: Boolean = true
): Modifier = if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    this.blur(radius = radius)
} else {
    this
}

@Composable
fun mikaGlassFillBrush(
    tintColor: Color = MaterialTheme.colorScheme.primary,
    tintStrength: Float = 0.09f,
    frostedAlpha: Float = 1f
): Brush {
    val mikaColors = LocalMikaColors.current
    val baseGlass = mikaColors.glassSurface
    return if (mikaColors.isDark) {
        val topSpecular = lerp(baseGlass, Color.White, 0.12f).copy(
            alpha = ((if (mikaColors.usePitchBlack) 0.78f else 0.74f) * frostedAlpha).coerceIn(0f, 1f)
        )
        val midGlass = lerp(baseGlass, tintColor, tintStrength).copy(
            alpha = ((if (mikaColors.usePitchBlack) 0.70f else 0.66f) * frostedAlpha).coerceIn(0f, 1f)
        )
        val bottomEdge = lerp(mikaColors.surfaceCard, tintColor, tintStrength * 0.65f).copy(
            alpha = ((if (mikaColors.usePitchBlack) 0.84f else 0.78f) * frostedAlpha).coerceIn(0f, 1f)
        )
        Brush.linearGradient(
            colors = listOf(topSpecular, midGlass, bottomEdge),
            start = Offset.Zero,
            end = Offset(920f, 920f)
        )
    } else {
        val topHighlight = lerp(baseGlass, Color.White, 0.66f).copy(alpha = (0.92f * frostedAlpha).coerceIn(0f, 1f))
        val midGlass = lerp(baseGlass, tintColor, tintStrength).copy(alpha = (0.84f * frostedAlpha).coerceIn(0f, 1f))
        val bottomEdge = lerp(MaterialTheme.colorScheme.surfaceVariant, tintColor, tintStrength * 0.75f)
            .copy(alpha = (0.80f * frostedAlpha).coerceIn(0f, 1f))
        Brush.linearGradient(
            colors = listOf(topHighlight, midGlass, bottomEdge),
            start = Offset.Zero,
            end = Offset(920f, 920f)
        )
    }
}

@Composable
fun mikaGlassBorderBrush(
    accentTint: Color = MaterialTheme.colorScheme.primary
): Brush {
    val mikaColors = LocalMikaColors.current
    return if (mikaColors.isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.28f),
                accentTint.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.08f)
            ),
            start = Offset.Zero,
            end = Offset(780f, 780f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.96f),
                accentTint.copy(alpha = 0.22f),
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
            ),
            start = Offset.Zero,
            end = Offset(780f, 780f)
        )
    }
}

@Composable
fun Modifier.mikaGlass(
    shape: Shape = RoundedCornerShape(22.dp),
    tintColor: Color = MaterialTheme.colorScheme.primary,
    tintStrength: Float = 0.09f,
    borderWidth: Dp = 1.dp,
    frostedAlpha: Float = 1f
): Modifier {
    val fill = mikaGlassFillBrush(
        tintColor = tintColor,
        tintStrength = tintStrength,
        frostedAlpha = frostedAlpha
    )
    val border = mikaGlassBorderBrush(accentTint = tintColor)
    return this
        .clip(shape)
        .background(fill, shape)
        .border(BorderStroke(borderWidth, border), shape)
}

@Composable
fun MikaGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    tintColor: Color = MaterialTheme.colorScheme.primary,
    tintStrength: Float = 0.09f,
    borderWidth: Dp = 1.dp,
    tonalElevation: Dp = 0.dp,
    frostedAlpha: Float = 1f,
    content: @Composable () -> Unit
) {
    val fill = mikaGlassFillBrush(
        tintColor = tintColor,
        tintStrength = tintStrength,
        frostedAlpha = frostedAlpha
    )
    val border = mikaGlassBorderBrush(accentTint = tintColor)
    Surface(
        modifier = modifier.border(BorderStroke(borderWidth, border), shape),
        shape = shape,
        color = Color.Transparent,
        tonalElevation = tonalElevation
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .background(fill)
        ) {
            content()
        }
    }
}
