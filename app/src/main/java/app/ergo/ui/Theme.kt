package app.ergo.ui

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.ergo.R

// Paper and ink, with "red pencil" for flaws and blue for argument structure.
object C {
    val Paper = Color(0xFFF4F0E8)
    val Ink = Color(0xFF1E1C19)
    val InkHover = Color(0xFF3A3630)
    val Red = Color(0xFFBF3B2A)
    val RedDeep = Color(0xFF8E2A1D)
    val RedTint = Color(0xFFF5DED7)
    val Blue = Color(0xFF2E58B8)
    val BlueTint = Color(0xFFDFE6F4)
    val Mute = Color(0xFF6A6459)
    val Body = Color(0xFF5B564D)
    val Soft = Color(0xFF3F3B35)
    val Rule = Color(0xFFD9D2C4)
    val RuleLight = Color(0xFFE6E0D4)
    val Card = Color(0xFFFBF9F4)
    val Sand = Color(0xFFECE6DA)
    val Pressed = Color(0xFFE9E3D7)
    val Locked = Color(0xFFA9A194)
    val Disabled = Color(0xFFD6CFC2)
    val DisabledText = Color(0xFF8A8376)
    val Dashed = Color(0xFF8A8376)
    val BarOff = Color(0xFFD0C8B9)
    val Handle = Color(0xFFC9C1B2)
    val Placeholder = Color(0xFF9A9386)
    val OnDarkMute = Color(0xFFB8AF9F)
    val OnDarkBody = Color(0xFFD8D0C1)
    val Scrim = Color(0x731E1C19)
    val VerdictWeak = Color(0xFFE0705E)
    val VerdictStrong = Color(0xFF8FB0F0)
}

private fun literata(weight: Int, italic: Boolean, opsz: Float) = Font(
    resId = if (italic) R.font.literata_italic else R.font.literata,
    weight = FontWeight(weight),
    style = if (italic) FontStyle.Italic else FontStyle.Normal,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.Setting("opsz", opsz),
    ),
)

private fun serifFamily(opsz: Float) = FontFamily(
    listOf(400, 500, 600, 700).flatMap { w -> listOf(literata(w, false, opsz), literata(w, true, opsz)) }
)

// Browsers pick Literata's optical size from the font size; Android does not, so there are two cuts.
val SerifText = serifFamily(12f)
val SerifDisplay = serifFamily(36f)

val Sans = FontFamily(
    listOf(400, 500, 600, 700).map { w ->
        Font(R.font.onest, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    }
)

val Mono = FontFamily(
    listOf(400, 500).map { w ->
        Font(R.font.jetbrains_mono, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    }
)

private val Base = TextStyle(
    color = C.Ink,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)

/** Literata. [lh] is a line-height multiplier, [ls] letter-spacing in em, as in the design. */
fun serif(size: Float, weight: Int = 400, lh: Float? = null, ls: Float = 0f, italic: Boolean = false, color: Color = C.Ink) =
    Base.copy(
        fontFamily = if (size >= 24f) SerifDisplay else SerifText,
        fontSize = size.sp,
        fontWeight = FontWeight(weight),
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        lineHeight = lh?.let { (size * it).sp } ?: Base.lineHeight,
        letterSpacing = ls.em,
        color = color,
    )

/** Onest, the interface face. */
fun sans(size: Float, weight: Int = 400, lh: Float? = null, italic: Boolean = false, color: Color = C.Ink) =
    Base.copy(
        fontFamily = Sans,
        fontSize = size.sp,
        fontWeight = FontWeight(weight),
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        lineHeight = lh?.let { (size * it).sp } ?: Base.lineHeight,
        color = color,
    )

/** JetBrains Mono, used for kickers, ids and metadata. */
fun mono(size: Float, weight: Int = 400, ls: Float = 0f, color: Color = C.Mute) =
    Base.copy(fontFamily = Mono, fontSize = size.sp, fontWeight = FontWeight(weight), letterSpacing = ls.em, color = color)

@Composable
fun ErgoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = C.Ink,
            onPrimary = C.Paper,
            background = C.Paper,
            onBackground = C.Ink,
            surface = C.Paper,
            onSurface = C.Ink,
            error = C.Red,
        ),
    ) {
        CompositionLocalProvider(
            LocalTextSelectionColors provides TextSelectionColors(handleColor = C.Red, backgroundColor = C.RedTint),
            content = content,
        )
    }
}
