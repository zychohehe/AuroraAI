package com.aurora.ai.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Palet "Aurora"
val Ink = Color(0xFF07050F)
val InkSoft = Color(0xFF0E0A1C)
val Violet = Color(0xFF7C5CFF)
val Pink = Color(0xFFFF5CAA)
val Cyan = Color(0xFF22D3EE)
val TextPrimary = Color(0xFFF4F1FF)
val TextMuted = Color(0xFFA79FC4)
val ErrorTint = Color(0xFFFF6B8A)

val GlassFill = Color.White.copy(alpha = 0.07f)
val GlassStroke = Color.White.copy(alpha = 0.12f)

val AuroraBrush = Brush.linearGradient(listOf(Violet, Pink))
val AuroraBrushWide = Brush.linearGradient(listOf(Cyan, Violet, Pink))

private val Scheme = darkColorScheme(
    primary = Violet,
    secondary = Pink,
    tertiary = Cyan,
    background = Ink,
    surface = InkSoft,
    onPrimary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

private val AuroraTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 38.sp,
        letterSpacing = (-1).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = 0.2.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    )
)

@Composable
fun AuroraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = AuroraTypography, content = content)
}
