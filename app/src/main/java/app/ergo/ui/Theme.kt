// Android marks Font(resId, …, variationSettings) as experimental.
@file:OptIn(ExperimentalTextApi::class)

package app.ergo.ui

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
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

/**
 * Paper and ink, with "red pencil" for flaws and blue for argument structure.
 * The dark theme is ink paper: warm near-black, off-white text, lifted red and blue.
 */
@Immutable
class Palette(
    val isDark: Boolean,
    val Paper: Color,
    val Ink: Color,
    val Red: Color,
    /** Red behind white text (buttons). */
    val RedFill: Color,
    val RedDeep: Color,
    val RedTint: Color,
    val Blue: Color,
    val BlueTint: Color,
    val Mute: Color,
    val Body: Color,
    val Soft: Color,
    val Rule: Color,
    val RuleLight: Color,
    val Card: Color,
    val Sand: Color,
    val Locked: Color,
    val Disabled: Color,
    val DisabledText: Color,
    val Dashed: Color,
    val BarOff: Color,
    val Handle: Color,
    val Placeholder: Color,
    /** The dark feature card ("continue", verdict) and its text. */
    val Hero: Color,
    val OnHero: Color,
    val HeroMute: Color,
    val HeroBody: Color,
    val Scrim: Color,
    val VerdictWeak: Color,
    val VerdictStrong: Color,
)

val LightPalette = Palette(
    isDark = false,
    Paper = Color(0xFFF4F0E8),
    Ink = Color(0xFF1E1C19),
    Red = Color(0xFFBF3B2A),
    RedFill = Color(0xFFBF3B2A),
    RedDeep = Color(0xFF8E2A1D),
    RedTint = Color(0xFFF5DED7),
    Blue = Color(0xFF2E58B8),
    BlueTint = Color(0xFFDFE6F4),
    Mute = Color(0xFF6A6459),
    Body = Color(0xFF5B564D),
    Soft = Color(0xFF3F3B35),
    Rule = Color(0xFFD9D2C4),
    RuleLight = Color(0xFFE6E0D4),
    Card = Color(0xFFFBF9F4),
    Sand = Color(0xFFECE6DA),
    Locked = Color(0xFFA9A194),
    Disabled = Color(0xFFD6CFC2),
    DisabledText = Color(0xFF8A8376),
    Dashed = Color(0xFF8A8376),
    BarOff = Color(0xFFD0C8B9),
    Handle = Color(0xFFC9C1B2),
    Placeholder = Color(0xFF9A9386),
    Hero = Color(0xFF1E1C19),
    OnHero = Color(0xFFF4F0E8),
    HeroMute = Color(0xFFB8AF9F),
    HeroBody = Color(0xFFD8D0C1),
    Scrim = Color(0x731E1C19),
    VerdictWeak = Color(0xFFE0705E),
    VerdictStrong = Color(0xFF8FB0F0),
)

val DarkPalette = Palette(
    isDark = true,
    Paper = Color(0xFF171513),
    Ink = Color(0xFFEDE7DB),
    Red = Color(0xFFEE7E68),
    RedFill = Color(0xFFC64A33),
    RedDeep = Color(0xFFF4AE9E),
    RedTint = Color(0xFF402823),
    Blue = Color(0xFF93AEF2),
    BlueTint = Color(0xFF1F2A40),
    Mute = Color(0xFFA39B8C),
    Body = Color(0xFFBEB6A7),
    Soft = Color(0xFFD2CABB),
    Rule = Color(0xFF3A352F),
    RuleLight = Color(0xFF2D2925),
    Card = Color(0xFF211E1A),
    Sand = Color(0xFF2B2723),
    Locked = Color(0xFF6E675C),
    Disabled = Color(0xFF35312B),
    DisabledText = Color(0xFF857E71),
    Dashed = Color(0xFF857E71),
    BarOff = Color(0xFF3A352F),
    Handle = Color(0xFF4B453E),
    Placeholder = Color(0xFF746D61),
    Hero = Color(0xFF2A2521),
    OnHero = Color(0xFFF4F0E8),
    HeroMute = Color(0xFFADA494),
    HeroBody = Color(0xFFD6CDBE),
    Scrim = Color(0xA6000000),
    VerdictWeak = Color(0xFFE0705E),
    VerdictStrong = Color(0xFF8FB0F0),
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

/** The current palette: `C.Ink`, `C.Red`… */
val C: Palette
    @Composable @ReadOnlyComposable
    get() = LocalPalette.current

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
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)

/** Literata. [lh] is a line-height multiplier, [ls] letter-spacing in em, as in the design. */
@Composable
@ReadOnlyComposable
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
@Composable
@ReadOnlyComposable
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
@Composable
@ReadOnlyComposable
fun mono(size: Float, weight: Int = 400, ls: Float = 0f, color: Color = C.Mute) =
    Base.copy(fontFamily = Mono, fontSize = size.sp, fontWeight = FontWeight(weight), letterSpacing = ls.em, color = color)

@Composable
fun ErgoTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(primary = p.Ink, onPrimary = p.Paper, background = p.Paper, onBackground = p.Ink, surface = p.Paper, onSurface = p.Ink, error = p.Red)
    } else {
        lightColorScheme(primary = p.Ink, onPrimary = p.Paper, background = p.Paper, onBackground = p.Ink, surface = p.Paper, onSurface = p.Ink, error = p.Red)
    }
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalPalette provides p,
            // Ripples and default icon tint follow the ink colour in both themes.
            LocalContentColor provides p.Ink,
            LocalTextSelectionColors provides TextSelectionColors(handleColor = p.Red, backgroundColor = p.RedTint),
            content = content,
        )
    }
}
