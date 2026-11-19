package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.R

val LexendDecaFontFamily = FontFamily(
    Font(R.font.lexend_deca_thin, FontWeight.Thin),
    Font(R.font.lexend_deca_extralight, FontWeight.ExtraLight),
    Font(R.font.lexend_deca_light, FontWeight.Light),
    Font(R.font.lexend_deca_regular, FontWeight.Normal),
    Font(R.font.lexend_deca_medium, FontWeight.Medium),
    Font(R.font.lexend_deca_semibold, FontWeight.SemiBold),
    Font(R.font.lexend_deca_bold, FontWeight.Bold),
    Font(R.font.lexend_deca_extrabold, FontWeight.ExtraBold),
    Font(R.font.lexend_deca_black, FontWeight.Black)
)

val BrandDisplayFontFamily = FontFamily(
    Font(R.font.journal_bold, FontWeight.Bold)
)

private fun TextUnit.scaled(factor: Float): TextUnit = (value * factor).sp

fun mikaTypography(scale: Float = 1.0f): Typography {
    val f = scale.coerceIn(0.85f, 1.30f)
    return Typography(
        displaySmall = TextStyle(
            fontFamily = LexendDecaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 31.sp.scaled(f),
            lineHeight = 37.sp.scaled(f),
            letterSpacing = (-0.5).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = LexendDecaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp.scaled(f),
            lineHeight = 30.sp.scaled(f),
            letterSpacing = (-0.25).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = LexendDecaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp.scaled(f),
            lineHeight = 26.sp.scaled(f)
        ),
        titleLarge = TextStyle(
            fontFamily = LexendDecaFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp.scaled(f),
            lineHeight = 24.sp.scaled(f)
        ),
        titleMedium = TextStyle(
            fontFamily = LexendDecaFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp.scaled(f),
            lineHeight = 22.sp.scaled(f),
            letterSpacing = 0.05.sp
        ),
        titleSmall = TextStyle(
            fontFamily = LexendDecaFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp.scaled(f),
            lineHeight = 20.sp.scaled(f)
        ),
        bodyLarge = TextStyle(
            fontFamily = LexendDecaFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp.scaled(f),
            lineHeight = 22.sp.scaled(f)
        ),
        bodyMedium = TextStyle(
            fontFamily = LexendDecaFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.5.sp.scaled(f),
            lineHeight = 19.sp.scaled(f)
        ),
        bodySmall = TextStyle(
            fontFamily = LexendDecaFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp.scaled(f),
            lineHeight = 16.sp.scaled(f)
        ),
        labelLarge = TextStyle(
            fontFamily = LexendDecaFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp.scaled(f),
            lineHeight = 18.sp.scaled(f)
        ),
        labelMedium = TextStyle(
            fontFamily = LexendDecaFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp.scaled(f),
            lineHeight = 16.sp.scaled(f)
        ),
        labelSmall = TextStyle(
            fontFamily = LexendDecaFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp.scaled(f),
            lineHeight = 12.sp.scaled(f),
            letterSpacing = 0.4.sp
        )
    )
}

val MikaTypography = mikaTypography(1.0f)
