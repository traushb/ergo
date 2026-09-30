package app.ergo.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.ergo.data.MarkStyle

// ── Marks ──────────────────────────────────────────────────────────────────

private data class LineSeg(val left: Float, val right: Float, val top: Float, val bottom: Float, val baseline: Float)

private fun segmentsFor(l: TextLayoutResult, start: Int, end: Int): List<LineSeg> {
    val len = l.layoutInput.text.length
    val s = start.coerceIn(0, len)
    val e = end.coerceIn(0, len)
    if (e <= s) return emptyList()
    val first = l.getLineForOffset(s)
    val last = l.getLineForOffset(e - 1)
    return (first..last).mapNotNull { line ->
        val lineStart = l.getLineStart(line)
        val visibleEnd = l.getLineEnd(line, visibleEnd = true)
        val a = maxOf(s, lineStart)
        val b = minOf(e, visibleEnd)
        if (b <= a) return@mapNotNull null
        val x1 = l.getHorizontalPosition(a, usePrimaryDirection = true)
        val x2 = if (b >= visibleEnd) l.getLineRight(line) else l.getHorizontalPosition(b, usePrimaryDirection = true)
        LineSeg(minOf(x1, x2), maxOf(x1, x2), l.getLineTop(line), l.getLineBottom(line), l.getLineBaseline(line))
    }
}

private fun DrawScope.wavyLine(x1: Float, x2: Float, y: Float, color: Color) {
    val amp = 1.6.dp.toPx()
    val half = 3.5.dp.toPx()
    val path = Path().apply {
        moveTo(x1, y)
        var x = x1
        var up = true
        while (x < x2) {
            quadraticTo(x + half / 2, if (up) y - amp * 2 else y + amp * 2, x + half, y)
            x += half
            up = !up
        }
    }
    drawPath(path, color, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
}

/**
 * Text with "red pencil" marks: a wavy red underline, or a highlighter band.
 * [marks] are character ranges (end exclusive) in [text]. [onTapOffset] receives
 * the character offset under a tap.
 */
@Composable
fun MarkedText(
    text: AnnotatedString,
    style: TextStyle,
    marks: List<IntRange>,
    markStyle: MarkStyle,
    modifier: Modifier = Modifier,
    underlineOffset: Dp = 6.dp,
    onTapOffset: ((Int) -> Unit)? = null,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val onTap by rememberUpdatedState(onTapOffset)
    val tap = if (onTapOffset != null) {
        Modifier.pointerInput(Unit) {
            detectTapGestures { pos -> layout?.let { l -> onTap?.invoke(l.getOffsetForPosition(pos)) } }
        }
    } else Modifier
    Text(
        text = text,
        style = style,
        onTextLayout = { layout = it },
        modifier = modifier
            .then(tap)
            .drawWithContent {
                val l = layout
                val segs = if (l == null) emptyList() else marks.flatMap { segmentsFor(l, it.first, it.last + 1) }
                if (markStyle == MarkStyle.Highlighter) {
                    val spread = 3.dp.toPx()
                    val band = style.fontSize.toPx() * 1.3f
                    segs.forEach { sg ->
                        val pad = ((sg.bottom - sg.top - band) / 2).coerceAtLeast(0f)
                        drawRoundRect(
                            color = C.RedTint,
                            topLeft = Offset(sg.left - spread, sg.top + pad - spread),
                            size = Size(sg.right - sg.left + spread * 2, sg.bottom - sg.top - pad * 2 + spread * 2),
                            cornerRadius = CornerRadius(3.dp.toPx() + spread),
                        )
                    }
                }
                drawContent()
                if (markStyle == MarkStyle.Pencil) {
                    segs.forEach { sg -> wavyLine(sg.left, sg.right, sg.baseline + underlineOffset.toPx(), C.Red) }
                }
            },
    )
}

/** A wavy underline that ignores the user's mark style (used for display type). */
@Composable
fun PencilText(text: AnnotatedString, style: TextStyle, marks: List<IntRange>, underlineOffset: Dp, modifier: Modifier = Modifier) =
    MarkedText(text, style, marks, MarkStyle.Pencil, modifier, underlineOffset)

// ── Small pieces ───────────────────────────────────────────────────────────

/** Three pulsing dots, the loading indicator. Uses the current [color]. */
@Composable
fun Dots(color: Color) {
    val t = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val a by t.animateFloat(
                initialValue = 0.2f,
                targetValue = 0.2f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1000
                        0.2f at 0
                        1f at 500
                        0.2f at 1000
                    },
                    initialStartOffset = StartOffset(i * 150),
                ),
                label = "dot$i",
            )
            Box(Modifier.size(6.dp).alpha(a).clip(CircleShape).background(color))
        }
    }
}

/** The Ergo mark: a red premise-dot over two ink dots, "therefore" upside down. */
@Composable
fun ErgoMark(width: Dp, height: Dp, dot: Dp) {
    Canvas(Modifier.size(width, height)) {
        val r = dot.toPx() / 2
        drawCircle(C.Red, r, Offset(size.width / 2, r))
        drawCircle(C.Ink, r, Offset(r, size.height - r))
        drawCircle(C.Ink, r, Offset(size.width - r, size.height - r))
    }
}

@Composable
fun MonoLabel(text: String, color: Color = C.Mute, size: Float = 11f, ls: Float = 0.12f, modifier: Modifier = Modifier) =
    Text(text, style = mono(size, ls = ls, color = color), modifier = modifier)

@Composable
fun Ico(icon: ImageVector, size: Dp, tint: Color, modifier: Modifier = Modifier) =
    Icon(icon, contentDescription = null, tint = tint, modifier = modifier.size(size))

/** Full-width primary pill. Disabled pills look muted but still receive taps, as in the design. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    fontSize: Float = 16f,
    bg: Color? = null,
    fg: Color? = null,
) {
    val background by animateColorAsState(bg ?: if (enabled) C.Ink else C.Disabled, label = "pillBg")
    val color = fg ?: if (enabled) C.Paper else C.DisabledText
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = sans(fontSize, 600, color = color), textAlign = TextAlign.Center)
    }
}

/** Row-shaped tappable surface with optional border, the design's generic card button. */
@Composable
fun Tappable(
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    bg: Color = Color.Transparent,
    border: BorderStroke? = null,
    padding: PaddingValues = PaddingValues(0.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier
            .clip(shape)
            .background(bg)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(padding),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        content = content,
    )
}

/** Round icon button (back / close). */
@Composable
fun RoundIconButton(icon: ImageVector, onClick: () -> Unit, size: Dp, iconSize: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Ico(icon, iconSize, C.Ink) }
}

fun Modifier.dashedBorder(color: Color, width: Dp, radius: Dp, dash: Dp = 6.dp, gap: Dp = 4.dp) = drawBehind {
    val w = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(w / 2, w / 2),
        size = Size(size.width - w, size.height - w),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx()))),
    )
}

/** Hairline rules drawn on the top and/or bottom edge. */
fun Modifier.rules(color: Color, top: Boolean = false, bottom: Boolean = false, left: Boolean = false, width: Dp = 1.dp) = drawBehind {
    val w = width.toPx()
    if (top) drawRect(color, Offset.Zero, Size(size.width, w))
    if (bottom) drawRect(color, Offset(0f, size.height - w), Size(size.width, w))
    if (left) drawRect(color, Offset.Zero, Size(w, size.height))
}

@Composable
fun Toast(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(12.dp), ambientColor = Color.Black.copy(alpha = .2f), spotColor = Color.Black.copy(alpha = .2f))
            .background(C.Ink)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(text, style = sans(14f, lh = 1.45f, color = C.Paper))
    }
}

/** Swallows touches so an overlay doesn't leak taps to the screen underneath. */
fun Modifier.blockTouches() = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) awaitPointerEvent().changes.forEach { it.consume() }
    }
}
