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

// Palet "Midnight & Champagne": gelap pekat, aksen emas lembut, teks putih hangat.
val Ink = Color(0xFF09090E)
val InkDeep = Color(0xFF0D0D15)
val Surface1 = Color(0xFF111118)
val Surface2 = Color(0xFF1A1A23)
val CodeBg = Color(0xFF0C0C12)

val Gold = Color(0xFFD9B97B)
val GoldDeep = Color(0xFFB8935A)
val GoldSoft = Color(0xFFF1E1BC)
val Indigo = Color(0xFF3A3F8F)

val TextPrimary = Color(0xFFECE8DF)
val TextMuted = Color(0xFF8D8998)
val OnGold = Color(0xFF1B1409)
val ErrorTint = Color(0xFFFF8A9B)
val Hairline = Color.White.copy(alpha = 0.09f)

val GoldBrush = Brush.linearGradient(listOf(GoldSoft, Gold, GoldDeep))
val SerifFont = FontFamily.Serif

private val Scheme = darkColorScheme(
    primary = Gold,
    secondary = GoldDeep,
    tertiary = GoldSoft,
    background = Ink,
    surface = Surface1,
    onPrimary = OnGold,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

private val AuroraTypography = Typography(
    titleMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        letterSpacing = 0.3.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.sp
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
        fontSize = 12.sp,
        letterSpacing = 0.3.sp
    )
)

@Composable
fun AuroraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = AuroraTypography, content = content)
}
