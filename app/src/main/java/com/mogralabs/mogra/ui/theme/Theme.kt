package com.mogralabs.mogra.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mogralabs.mogra.R

/**
 * Palette lifted from the logo: a crimson-and-pink knot on black. Everything in the app is
 * either the ink, the crimson, or the cream at some opacity -- there is no fourth hue.
 */
object Mogra {
    val Ink = Color(0xFF0A0709)
    val Crimson = Color(0xFFDD2F4F)
    val CrimsonSoft = Color(0xFFF7758F)
    val Cream = Color(0xFFFBEDE9)

    /** Cream at the four opacities the design uses, named by role rather than by number. */
    val TextPrimary = Cream
    val TextSecondary = Cream.copy(alpha = 0.52f)
    val TextMuted = Cream.copy(alpha = 0.42f)
    val TextFaint = Cream.copy(alpha = 0.30f)

    val Hairline = Cream.copy(alpha = 0.10f)
    val SurfaceTint = Cream.copy(alpha = 0.028f)
    val CrimsonTint = Crimson.copy(alpha = 0.085f)
    val CrimsonEdge = Crimson.copy(alpha = 0.32f)
}

/** Display face -- the wordmark, raag names, big numerals. Has real Devanagari for mr/hi. */
val Tiro = FontFamily(Font(R.font.tiro_devanagari_marathi_regular, FontWeight.Normal))

/** UI face. Also has real Devanagari, so a translated build needs no second type decision. */
val Mukta = FontFamily(
    Font(R.font.mukta_light, FontWeight.Light),
    Font(R.font.mukta_regular, FontWeight.Normal),
    Font(R.font.mukta_medium, FontWeight.Medium),
    Font(R.font.mukta_semibold, FontWeight.SemiBold),
    Font(R.font.mukta_bold, FontWeight.Bold),
)

private val MograTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Tiro, fontSize = 47.sp, lineHeight = 50.sp, letterSpacing = (-0.8).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Tiro, fontSize = 33.sp, lineHeight = 38.sp, letterSpacing = (-0.3).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Mukta, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Mukta, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Mukta, fontWeight = FontWeight.Normal, fontSize = 13.5f.sp, lineHeight = 21.sp,
    ),
    // The small-caps micro label: section headings, step counters, state words.
    labelSmall = TextStyle(
        fontFamily = Mukta, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 14.sp,
        letterSpacing = 2.sp,
    ),
)

private val MograColors = darkColorScheme(
    primary = Mogra.Crimson,
    onPrimary = Color(0xFFFFF1F3),
    background = Mogra.Ink,
    onBackground = Mogra.Cream,
    surface = Mogra.Ink,
    onSurface = Mogra.Cream,
)

@Composable
fun MograTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MograColors, typography = MograTypography, content = content)
}
