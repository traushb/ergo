package app.ergo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
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
import app.ergo.data.Accent
import app.ergo.data.MarkStyle

// ── Marks ──────────────────────────────────────────────────────────────────

/** A marked range of text (end inclusive): red for a flaw, blue for structure. */
data class TextMark(val range: IntRange, val accent: Accent = Accent.Red)

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
 * Text with pencil marks: a wavy underline, or a highlighter band behind the text.
 * [onTapOffset] receives the character offset under a tap.
 */
@Composable
fun MarkedText(
    text: AnnotatedString,
    style: TextStyle,
    marks: List<TextMark>,
    markStyle: MarkStyle,
    modifier: Modifier = Modifier,
    underlineOffset: Dp = 6.dp,
    onTapOffset: ((Int) -> Unit)? = null,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val onTap by rememberUpdatedState(onTapOffset)
    val red = C.Red
    val blue = C.Blue
    val redTint = C.RedTint
    val blueTint = C.BlueTint
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
                val segs = if (l == null) emptyList() else marks.flatMap { m -> segmentsFor(l, m.range.first, m.range.last + 1).map { it to m.accent } }
                if (markStyle == MarkStyle.Highlighter) {
                    val spread = 3.dp.toPx()
                    val band = style.fontSize.toPx() * 1.3f
                    segs.forEach { (sg, accent) ->
                        val pad = ((sg.bottom - sg.top - band) / 2).coerceAtLeast(0f)
                        drawRoundRect(
                            color = if (accent == Accent.Blue) blueTint else redTint,
                            topLeft = Offset(sg.left - spread, sg.top + pad - spread),
                            size = Size(sg.right - sg.left + spread * 2, sg.bottom - sg.top - pad * 2 + spread * 2),
                            cornerRadius = CornerRadius(3.dp.toPx() + spread),
                        )
                    }
                }
                drawContent()
                if (markStyle == MarkStyle.Pencil) {
                    segs.forEach { (sg, accent) -> wavyLine(sg.left, sg.right, sg.baseline + underlineOffset.toPx(), if (accent == Accent.Blue) blue else red) }
                }
            },
    )
}

/** A wavy red underline that ignores the user's mark style (used for display type). */
@Composable
fun PencilText(text: AnnotatedString, style: TextStyle, marks: List<IntRange>, underlineOffset: Dp, modifier: Modifier = Modifier) =
    MarkedText(text, style, marks.map { TextMark(it) }, MarkStyle.Pencil, modifier, underlineOffset)

// ── Small pieces ───────────────────────────────────────────────────────────

/** Three pulsing dots, the loading indicator. */
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
    val red = C.Red
    val ink = C.Ink
    Canvas(Modifier.size(width, height)) {
        val r = dot.toPx() / 2
        drawCircle(red, r, Offset(size.width / 2, r))
        drawCircle(ink, r, Offset(r, size.height - r))
        drawCircle(ink, r, Offset(size.width - r, size.height - r))
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
    icon: ImageVector? = null,
) {
    val background by animateColorAsState(bg ?: if (enabled) C.Ink else C.Disabled, label = "pillBg")
    val color = fg ?: if (enabled) C.Paper else C.DisabledText
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Ico(icon, 18.dp, color)
        Text(text, style = sans(fontSize, 600, color = color), textAlign = TextAlign.Center, maxLines = 1)
    }
}

/** Outlined pill, the secondary action. */
@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
    color: Color = C.Ink,
    icon: ImageVector? = null,
    fontSize: Float = 15f,
) {
    Row(
        modifier
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .border(1.5.dp, color, RoundedCornerShape(height / 2))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Ico(icon, 18.dp, color)
        Text(text, style = sans(fontSize, 600, color = color), maxLines = 1)
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
fun RoundIconButton(icon: ImageVector, onClick: () -> Unit, size: Dp, iconSize: Dp, modifier: Modifier = Modifier, tint: Color = C.Ink) {
    Box(
        modifier.size(size).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Ico(icon, iconSize, tint) }
}

/** A small rounded label: "×3", "Новое", topic chips. */
@Composable
fun Chip(text: String, fg: Color, bg: Color, modifier: Modifier = Modifier, icon: ImageVector? = null, height: Dp = 26.dp, fontSize: Float = 12f) {
    Row(
        modifier.height(height).clip(RoundedCornerShape(height / 2)).background(bg).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Ico(icon, 14.dp, fg)
        Text(text, style = sans(fontSize, 600, color = fg), maxLines = 1)
    }
}

/** Leitner box as four pips: how well a topic is learnt. */
@Composable
fun Pips(level: Int, modifier: Modifier = Modifier, max: Int = 4, color: Color = C.Blue) {
    val off = C.BarOff
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(max) { i ->
            Box(Modifier.size(width = 10.dp, height = 6.dp).clip(RoundedCornerShape(3.dp)).background(if (i < level) color else off))
        }
    }
}

/** Session progress: one segment per exercise. */
@Composable
fun SegmentBar(total: Int, done: Int, modifier: Modifier = Modifier) {
    val ink = C.Ink
    val off = C.Disabled
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total.coerceAtLeast(1)) { i ->
            val c by animateColorAsState(if (i < done) ink else off, label = "seg")
            Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(c))
        }
    }
}

/** "1 Найти · 2 Назвать · 3 Понять": where you are in an exercise. */
@Composable
fun StepChips(labels: List<String>, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { i, label ->
            val shape = RoundedCornerShape(15.dp)
            val bg = if (i == current) C.Ink else if (i < current) C.Sand else C.Paper
            val fg = if (i == current) C.Paper else if (i < current) C.Ink else C.DisabledText
            Row(
                Modifier.height(30.dp).clip(shape).background(bg)
                    .border(1.dp, if (i > current) C.Rule else bg, shape)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("${i + 1}", style = mono(11f, color = fg))
                Text(label, style = sans(13f, 600, color = fg), maxLines = 1)
            }
        }
    }
}

/** The bordered card that holds a snippet, with its source line. */
@Composable
fun SnippetCard(source: String, ai: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.Card)
            .border(1.dp, C.Rule, RoundedCornerShape(20.dp))
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MonoLabel(source.uppercase(), ls = 0.08f, modifier = Modifier.weight(1f))
            if (ai) Chip("Новое", C.Blue, C.BlueTint, icon = Icons.Outlined.AutoAwesome, height = 24.dp)
        }
        content()
    }
}

enum class OptState { Idle, Right, Wrong, Dim }

private data class OptColors(val border: Color, val bg: Color, val fg: Color)

@Composable
private fun optColors(s: OptState) = when (s) {
    OptState.Idle -> OptColors(C.Rule, C.Card, C.Ink)
    OptState.Right -> OptColors(C.Blue, C.BlueTint, C.Blue)
    OptState.Wrong -> OptColors(C.Red, C.RedTint, C.Red)
    OptState.Dim -> OptColors(C.Rule, C.Card, C.Locked)
}

/** Two-column grid of short answer options (fallacy names). */
@Composable
fun <T> OptionGrid(options: List<T>, label: (T) -> String, state: (T) -> OptState, enabled: Boolean, onPick: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { o ->
                    val c = optColors(state(o))
                    val shape = RoundedCornerShape(14.dp)
                    Box(
                        Modifier.weight(1f).fillMaxHeight().heightIn(min = 52.dp).clip(shape).background(c.bg)
                            .border(1.5.dp, c.border, shape)
                            .clickable(enabled = enabled) { onPick(o) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label(o), style = sans(14f, 600, lh = 1.3f, color = c.fg), textAlign = TextAlign.Center)
                    }
                }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

/** Full-width answer cards for longer options, with feedback under the chosen one. */
@Composable
fun ChoiceList(
    options: List<String>,
    state: (Int) -> OptState,
    feedback: (Int) -> String?,
    enabled: Boolean,
    onPick: (Int) -> Unit,
    serifText: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEachIndexed { i, text ->
            val s = state(i)
            val c = optColors(s)
            val shape = RoundedCornerShape(16.dp)
            Column(
                Modifier.fillMaxWidth().clip(shape).background(if (s == OptState.Dim) C.Card else c.bg)
                    .border(1.5.dp, c.border, shape)
                    .clickable(enabled = enabled) { onPick(i) }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val textColor = if (s == OptState.Dim) C.Mute else C.Ink
                Text(text, style = if (serifText) serif(17f, lh = 1.45f, color = textColor) else sans(15f, lh = 1.45f, color = textColor))
                feedback(i)?.let { Text(it, style = sans(14f, 600, lh = 1.45f, color = if (s == OptState.Right) C.Blue else C.Red)) }
            }
        }
    }
}

/** "Верно: …" / "Не совсем…" with a check or cross. */
@Composable
fun Verdict(correct: Boolean, text: String) {
    val color = if (correct) C.Blue else C.Red
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Ico(if (correct) Icons.Outlined.Check else Icons.Outlined.Close, 22.dp, color, Modifier.padding(top = 2.dp))
        Text(text, style = serif(22f, 500, lh = 1.25f, color = color))
    }
}

/** A note with a coloured bar on the left: explanations, model replies. */
@Composable
fun NoteBox(title: String, text: String, meta: String? = null, accent: Accent = Accent.Red) {
    val bar = if (accent == Accent.Blue) C.Blue else C.Red
    val bg = if (accent == Accent.Blue) C.BlueTint else C.RedTint
    val deep = if (accent == Accent.Blue) C.Blue else C.RedDeep
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp)).background(bg)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(bar))
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MonoLabel(title, color = deep, ls = 0.1f)
            Text(text, style = sans(15f, lh = 1.55f))
            if (!meta.isNullOrEmpty()) Text(meta, style = mono(11f, color = deep))
        }
    }
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

/** Hairline rules drawn on the top, bottom or left edge. */
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

private class Holder<T>(var value: T?)

/** Keeps showing the last non-null value while an exit animation runs. */
@Composable
fun <T : Any> rememberLast(value: T?): T? {
    val h = remember { Holder<T>(null) }
    if (value != null) h.value = value
    return h.value
}

/**
 * A bottom sheet over a scrim. [fraction] fixes the height as a share of the screen;
 * null wraps the content (up to 90%).
 */
@Composable
fun BottomSheet(visible: Boolean, onDismiss: () -> Unit, fraction: Float? = 0.8f, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier.fillMaxSize().background(C.Scrim)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            )
        }
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            val maxH = maxHeight
            AnimatedVisibility(visible, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .then(if (fraction != null) Modifier.height(maxH * fraction) else Modifier.heightIn(max = maxH * 0.9f))
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(C.Card)
                        .blockTouches()
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(C.Handle))
                    }
                    content()
                }
            }
        }
    }
}

/** A small centred confirmation card. */
@Composable
fun ConfirmDialog(visible: Boolean, title: String, text: String, action: String, onYes: () -> Unit, onNo: () -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier.fillMaxSize().background(C.Scrim)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onNo),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.padding(24.dp).widthIn(max = 420.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(C.Card)
                    .blockTouches().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(title, style = serif(24f, 500, lh = 1.2f))
                Text(text, style = sans(15f, lh = 1.5f, color = C.Body))
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlineButton("Отмена", onNo, Modifier.weight(1f), height = 48.dp)
                    PillButton(action, onYes, Modifier.weight(1f), height = 48.dp, fontSize = 15f, bg = C.RedFill, fg = Color.White)
                }
            }
        }
    }
}
