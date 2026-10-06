package app.ergo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Info
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ergo.ErgoViewModel
import app.ergo.data.Analysis
import app.ergo.data.VERDICT_RU

private data class MarkSpan(val s: Int, val e: Int, val n: Int)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalyzeScreen(vm: ErgoViewModel) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Анализ", style = serif(32f, 500, lh = 1.05f, ls = -0.02f))
            Text(
                "Вставьте любой аргумент: колонку, пост, рекламу. Ergo покажет его скелет и отметит слабые места.",
                style = sans(15f, lh = 1.45f, color = C.Body),
            )
        }

        val interaction = remember { MutableInteractionSource() }
        val focused by interaction.collectIsFocusedAsState()
        val style = serif(17f, lh = 1.5f)
        BasicTextField(
            value = vm.analyze.text,
            onValueChange = vm.analyze::onText,
            textStyle = style,
            cursorBrush = SolidColor(C.Ink),
            interactionSource = interaction,
            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
            decorationBox = { inner ->
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 150.dp).clip(RoundedCornerShape(18.dp)).background(C.Card)
                        .border(1.dp, if (focused) C.Ink else C.Rule, RoundedCornerShape(18.dp))
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    if (vm.analyze.text.isEmpty()) Text("Вставьте или напишите аргумент…", style = style.copy(color = C.Placeholder))
                    inner()
                }
            },
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            vm.bank.samples.forEachIndexed { i, sm ->
                Tappable(
                    onClick = { vm.analyze.useSample(i) },
                    shape = RoundedCornerShape(17.dp),
                    modifier = Modifier.height(34.dp),
                    border = BorderStroke(1.dp, C.Rule),
                    padding = PaddingValues(start = 10.dp, end = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Ico(Icons.Outlined.ContentPaste, 16.dp, C.Ink)
                    Text(sm.label, style = sans(13f, 500), maxLines = 1)
                }
            }
        }

        Tappable(
            onClick = vm.analyze::analyze,
            shape = RoundedCornerShape(27.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            bg = C.Ink,
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        ) {
            if (vm.analyze.loading) Dots(C.Paper)
            Ico(Icons.Outlined.AccountTree, 20.dp, C.Paper)
            Text(if (vm.analyze.loading) "Разбираем…" else "Разобрать аргумент", style = sans(16f, 600, color = C.Paper))
        }

        vm.analyze.result?.let { AnalysisResult(vm, it) }
    }
}

@Composable
private fun AnalysisResult(vm: ErgoViewModel, an: Analysis) {
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        // Source text with numbered marks
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MonoLabel("С ПОМЕТКАМИ")
            val src = vm.analyze.source
            val found = an.issues.mapIndexedNotNull { i, issue ->
                val q = issue.quote.trim()
                if (q.isEmpty()) return@mapIndexedNotNull null
                var at = src.indexOf(q)
                if (at < 0) at = src.lowercase().indexOf(q.lowercase())
                if (at >= 0) MarkSpan(at, at + q.length, i + 1) else null
            }.sortedBy { it.s }
            val marks = mutableListOf<IntRange>()
            val sup = SpanStyle(fontFamily = Mono, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = C.Red, baselineShift = BaselineShift.Superscript)
            val text = buildAnnotatedString {
                var pos = 0
                found.forEach { m ->
                    if (m.s < pos) return@forEach
                    append(src.substring(pos, m.s))
                    val start = length
                    append(src.substring(m.s, m.e))
                    marks += start until length
                    withStyle(sup) { append(" ${m.n}") }
                    pos = m.e
                }
                if (pos < src.length) append(src.substring(pos))
            }
            MarkedText(text, serif(17f, lh = 1.7f), marks.map { TextMark(it) }, vm.markStyle)
        }

        // Skeleton: conclusion, premises, hidden assumptions
        Column {
            MonoLabel("СКЕЛЕТ", modifier = Modifier.padding(bottom = 10.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.Card)
                    .border(2.dp, C.Ink, RoundedCornerShape(16.dp)).padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MonoLabel("∴ ВЫВОД", color = C.Ink, ls = 0.1f)
                Text(an.conclusion, style = serif(19f, 500, lh = 1.35f))
            }
            Row(Modifier.padding(start = 24.dp).height(IntrinsicSize.Min)) {
                Box(Modifier.width(2.dp).fillMaxHeight().background(C.Blue))
                Column(
                    Modifier.weight(1f).padding(start = 18.dp, top = 14.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    an.premises.forEachIndexed { i, t ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.BlueTint).padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("P${i + 1}", style = mono(12f, 500, color = C.Blue), modifier = Modifier.padding(top = 2.dp))
                            Text(t, style = sans(15f, lh = 1.45f))
                        }
                    }
                    if (an.heuristic) {
                        Row(
                            Modifier.fillMaxWidth().dashedBorder(C.Dashed, 1.5.dp, 14.dp).padding(horizontal = 13.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("A?", style = mono(12f, 500), modifier = Modifier.padding(top = 2.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                MonoLabel("ВОПРОС К ВАМ", size = 10f, ls = 0.1f)
                                Text("Что должно быть правдой, чтобы из посылок следовал вывод? Скрытые допущения офлайн-разбор не ищет.", style = sans(15f, lh = 1.45f, italic = true, color = C.Soft))
                            }
                        }
                    }
                    an.assumptions.forEachIndexed { i, t ->
                        Row(
                            Modifier.fillMaxWidth().dashedBorder(C.Dashed, 1.5.dp, 14.dp).padding(horizontal = 13.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("A${i + 1}", style = mono(12f, 500), modifier = Modifier.padding(top = 2.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                MonoLabel("НЕЯВНОЕ ДОПУЩЕНИЕ", size = 10f, ls = 0.1f)
                                Text(t, style = sans(15f, lh = 1.45f, italic = true, color = C.Soft))
                            }
                        }
                    }
                }
            }
        }

        // Red pencil notes
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MonoLabel("КРАСНЫЙ КАРАНДАШ", color = C.Red)
            an.issues.forEachIndexed { i, issue ->
                val topic = issue.topic
                Row(
                    (if (topic != null) Modifier.clickable { vm.openReference(topic) } else Modifier),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(24.dp).border(1.5.dp, C.Red, CircleShape), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", style = mono(12f, 500, color = C.Red))
                    }
                    Column(Modifier.padding(top = 1.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(issue.name, style = sans(16f, 700, color = C.Red), modifier = Modifier.weight(1f, fill = false))
                            if (topic != null) Ico(Icons.Outlined.Info, 16.dp, C.Mute)
                        }
                        Text(issue.note, style = sans(15f, lh = 1.5f, color = C.Soft))
                    }
                }
            }
        }

        // Verdict
        val score = mapOf("Strong" to 3, "Moderate" to 2, "Weak" to 1)[an.verdict] ?: 2
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.Hero).padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MonoLabel(if (an.heuristic) "ПРЕДВАРИТЕЛЬНО" else "ИТОГ", color = C.HeroMute, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    (1..3).forEach { i ->
                        val on = when (score) { 1 -> C.VerdictWeak; 3 -> C.VerdictStrong; else -> C.OnHero }
                        Box(Modifier.size(10.dp).clip(CircleShape).background(if (i <= score) on else C.OnHero.copy(alpha = .2f)))
                    }
                }
            }
            Text(if (an.heuristic) (if (an.issues.isEmpty()) "Явных ошибок не видно" else "Есть что проверить") else (VERDICT_RU[an.verdict] ?: an.verdict) + " аргумент", style = serif(26f, 500, lh = 1.1f, color = C.OnHero))
            Text(an.summary, style = sans(15f, lh = 1.5f, color = C.HeroBody))
        }
        Text(vm.analyze.meta, style = mono(11f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
