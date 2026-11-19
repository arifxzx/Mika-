package com.example.ui.theme

import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AccentPalette
import com.example.model.AppSettings
import com.example.model.GradientStyle
import com.example.model.ThemeMode

@Immutable
data class MikaExtendedColors(
    val isDark: Boolean,
    val usePitchBlack: Boolean,
    val accent: Color,
    val accentSecondary: Color,
    val surfaceDeep: Color,
    val surfaceCard: Color,
    val surfaceElevated: Color,
    val glassSurface: Color,
    val glassBorder: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val gradientStyle: GradientStyle,
    val applyGradientToMiniPlayer: Boolean,
    val applyGradientToFullPlayer: Boolean,
    val applyGradientToSurfaces: Boolean,
    val applyColorOnApp: Boolean = true,
    val applyColorOnPlayer: Boolean = true,
    val playerAccent: Color = accent,
    val playerAccentSecondary: Color = accentSecondary,
    val cornerRadiusDp: Int = 16
) {
    fun surfaceBrush(
        baseColor: Color = surfaceCard,
        overrideStart: Color? = null,
        overrideEnd: Color? = null,
        alphaScale: Float = 1f
    ): Brush {
        val s = (overrideStart ?: gradientStart)
        val e = (overrideEnd ?: gradientEnd)
        return when (gradientStyle) {
            GradientStyle.NONE -> Brush.linearGradient(listOf(baseColor, baseColor))
            GradientStyle.SUBTLE_LINEAR -> Brush.linearGradient(
                colors = listOf(
                    lerp(baseColor, s, 0.56f * alphaScale),
                    lerp(baseColor, e, 0.46f * alphaScale)
                ),
                start = Offset.Zero,
                end = Offset(1000f, 900f)
            )
            GradientStyle.RADIAL_GLOW -> Brush.radialGradient(
                colors = listOf(
                    lerp(baseColor, e, 0.62f * alphaScale),
                    lerp(baseColor, s, 0.34f * alphaScale),
                    baseColor
                )
            )
            GradientStyle.AURORA_DUAL -> Brush.linearGradient(
                colors = listOf(
                    lerp(baseColor, s, 0.62f * alphaScale),
                    lerp(baseColor, accent, 0.24f * alphaScale),
                    lerp(baseColor, e, 0.58f * alphaScale)
                ),
                start = Offset(0f, 0f),
                end = Offset(1200f, 1200f)
            )
        }
    }
}

val LocalMikaColors = compositionLocalOf {
    MikaExtendedColors(
        isDark = true,
        usePitchBlack = false,
        accent = Color(0xFF1E88FF),
        accentSecondary = Color(0xFF38BDF8),
        surfaceDeep = MikaDeepBlack,
        surfaceCard = MikaCharcoalDark,
        surfaceElevated = MikaCharcoalElevated,
        glassSurface = Color(0xFF171C28).copy(alpha = 0.72f),
        glassBorder = Color.White.copy(alpha = 0.14f),
        gradientStart = Color(0xFF0B162C),
        gradientEnd = Color(0xFF1A3A6E),
        gradientStyle = GradientStyle.SUBTLE_LINEAR,
        applyGradientToMiniPlayer = true,
        applyGradientToFullPlayer = true,
        applyGradientToSurfaces = true,
        applyColorOnApp = true,
        applyColorOnPlayer = true,
        playerAccent = Color(0xFF1E88FF),
        playerAccentSecondary = Color(0xFF38BDF8),
        cornerRadiusDp = 16
    )
}

val LocalMikaIconScale = compositionLocalOf { 1.0f }

@Composable
fun mikaScaledIcon(base: Dp): Dp =
    (base.value * LocalMikaIconScale.current.coerceIn(0.80f, 1.30f)).dp

val MikaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private fun extractWallpaperSeedColor(context: Context, isDark: Boolean): Color? {
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            return scheme.primary
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val colors = WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            colors?.primaryColor?.toArgb()?.let { Color(it) }
        } else {
            null
        }
    }.getOrNull()
}

@Composable
fun MikaTheme(
    settings: AppSettings,
    artworkAccentPair: Pair<Color, Color>? = null,
    autoAccentPair: Pair<Color, Color>? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDark = when (settings.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val wallpaperColor = remember(settings.pickColorsFromWallpaper, isDark) {
        if (settings.pickColorsFromWallpaper) extractWallpaperSeedColor(context, isDark) else null
    }

    val effectiveCustomArgb = if (isDark && settings.customDarkAccentArgb != 0L) {
        settings.customDarkAccentArgb
    } else {
        settings.customAccentArgb
    }
    val manualAccent = AccentPalette.resolve(settings.accentColorId, effectiveCustomArgb)
    val selectedSeed = when {
        settings.autoColoring && autoAccentPair != null -> autoAccentPair.first
        settings.pickColorsFromWallpaper && wallpaperColor != null -> wallpaperColor
        else -> manualAccent.color
    }
    val neutralSeed = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val appSeedColor = if (settings.applyColorOnApp) selectedSeed else neutralSeed
    val playerSeedColor = if (settings.applyColorOnPlayer) selectedSeed else neutralSeed

    val harmonicShades = AccentPalette.deriveHarmonicThemeColors(appSeedColor, isDark = isDark)
    val playerShades = AccentPalette.deriveHarmonicThemeColors(playerSeedColor, isDark = isDark)
    val onPrimaryContrast = if (harmonicShades.primary.luminance() > 0.56f) Color(0xFF0A0F1A) else Color.White

    // Smooth single-hue multi-shade gradients tailored for Light vs Dark mode
    val effectiveGradStart: Color
    val effectiveGradEnd: Color
    if ((settings.autoColoring && autoAccentPair != null) || (settings.pickColorsFromWallpaper && wallpaperColor != null)) {
        effectiveGradStart = harmonicShades.gradientStart
        effectiveGradEnd = harmonicShades.gradientEnd
    } else if (settings.gradientStyle != GradientStyle.NONE && artworkAccentPair != null && settings.applyGradientToSurfaces) {
        val artShades = AccentPalette.deriveHarmonicThemeColors(artworkAccentPair.second, isDark = isDark)
        effectiveGradStart = lerp(harmonicShades.gradientStart, artShades.gradientStart, 0.35f)
        effectiveGradEnd = lerp(harmonicShades.gradientEnd, artShades.gradientEnd, 0.35f)
    } else {
        effectiveGradStart = harmonicShades.gradientStart
        effectiveGradEnd = harmonicShades.gradientEnd
    }

    val darkBg = if (settings.usePitchBlack) Color.Black else MikaDeepBlack
    val darkSurface = if (settings.usePitchBlack) Color(0xFF090A0D) else MikaCharcoalDark
    val darkElevated = if (settings.usePitchBlack) Color(0xFF12141A) else MikaCharcoalElevated
    val darkHigh = if (settings.usePitchBlack) Color(0xFF1A1D25) else MikaCharcoalHigh

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = harmonicShades.primary,
            onPrimary = onPrimaryContrast,
            primaryContainer = lerp(darkElevated, harmonicShades.container, 0.65f),
            onPrimaryContainer = MikaTextPrimaryDark,
            secondary = harmonicShades.secondary,
            onSecondary = if (harmonicShades.secondary.luminance() > 0.56f) Color(0xFF0A0F1A) else Color.White,
            secondaryContainer = darkElevated,
            onSecondaryContainer = MikaTextPrimaryDark,
            background = darkBg,
            onBackground = MikaTextPrimaryDark,
            surface = darkSurface,
            onSurface = MikaTextPrimaryDark,
            surfaceVariant = darkElevated,
            onSurfaceVariant = MikaTextSecondaryDark,
            surfaceContainerLowest = darkBg,
            surfaceContainerLow = darkSurface,
            surfaceContainer = darkElevated,
            surfaceContainerHigh = darkHigh,
            outline = MikaOutlineDark,
            outlineVariant = if (settings.usePitchBlack) Color(0xFF262B38) else Color(0xFF2D3344),
            error = Color(0xFFF87171),
            onError = Color(0xFF0A0F1A)
        )
    } else {
        lightColorScheme(
            primary = harmonicShades.primary,
            onPrimary = onPrimaryContrast,
            primaryContainer = lerp(Color.White, harmonicShades.container, 0.72f),
            onPrimaryContainer = MikaTextPrimaryLight,
            secondary = harmonicShades.secondary,
            onSecondary = if (harmonicShades.secondary.luminance() > 0.56f) Color(0xFF0A0F1A) else Color.White,
            secondaryContainer = MikaSurfaceElevatedLight,
            onSecondaryContainer = MikaTextPrimaryLight,
            background = MikaAlabasterLight,
            onBackground = MikaTextPrimaryLight,
            surface = MikaSurfaceLight,
            onSurface = MikaTextPrimaryLight,
            surfaceVariant = MikaSurfaceElevatedLight,
            onSurfaceVariant = MikaTextSecondaryLight,
            surfaceContainerLowest = MikaSurfaceLight,
            surfaceContainerLow = MikaAlabasterLight,
            surfaceContainer = MikaSurfaceElevatedLight,
            surfaceContainerHigh = MikaSurfaceHighLight,
            outline = MikaOutlineLight,
            outlineVariant = Color(0xFFE6ECF5),
            error = Color(0xFFDC2626),
            onError = Color.White
        )
    }

    val radius = settings.cornerRadiusDp.coerceIn(4, 28)
    val dynamicShapes = remember(radius) {
        Shapes(
            extraSmall = RoundedCornerShape((radius * 0.5f).coerceAtLeast(4f).dp),
            small = RoundedCornerShape((radius * 0.75f).coerceAtLeast(6f).dp),
            medium = RoundedCornerShape(radius.dp),
            large = RoundedCornerShape((radius * 1.35f).coerceAtMost(32f).dp),
            extraLarge = RoundedCornerShape((radius * 1.65f).coerceAtMost(36f).dp)
        )
    }

    val extended = MikaExtendedColors(
        isDark = isDark,
        usePitchBlack = settings.usePitchBlack && isDark,
        accent = harmonicShades.primary,
        accentSecondary = harmonicShades.secondary,
        surfaceDeep = if (isDark) darkBg else MikaAlabasterLight,
        surfaceCard = if (isDark) darkSurface else MikaSurfaceLight,
        surfaceElevated = if (isDark) darkElevated else MikaSurfaceElevatedLight,
        glassSurface = if (isDark) {
            lerp(darkSurface, harmonicShades.gradientEnd, 0.25f).copy(alpha = 0.78f)
        } else {
            lerp(Color.White, harmonicShades.container, 0.30f).copy(alpha = 0.88f)
        },
        glassBorder = if (isDark) {
            Color.White.copy(alpha = 0.14f)
        } else {
            harmonicShades.primary.copy(alpha = 0.18f)
        },
        gradientStart = effectiveGradStart,
        gradientEnd = effectiveGradEnd,
        gradientStyle = settings.gradientStyle,
        applyGradientToMiniPlayer = settings.applyGradientToMiniPlayer,
        applyGradientToFullPlayer = settings.applyGradientToFullPlayer,
        applyGradientToSurfaces = settings.applyGradientToSurfaces,
        applyColorOnApp = settings.applyColorOnApp,
        applyColorOnPlayer = settings.applyColorOnPlayer,
        playerAccent = playerShades.primary,
        playerAccentSecondary = playerShades.secondary,
        cornerRadiusDp = radius
    )

    val typography = remember(settings.fontScale) {
        mikaTypography(settings.fontScale)
    }

    CompositionLocalProvider(
        LocalMikaColors provides extended,
        LocalMikaIconScale provides settings.appIconScale.coerceIn(0.80f, 1.30f)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = dynamicShapes,
            content = content
        )
    }
}
