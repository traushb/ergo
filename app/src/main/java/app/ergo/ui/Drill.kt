package app.ergo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.ergo.DrillPhase
import app.ergo.ErgoViewModel

@Composable
fun DrillScreen(vm: ErgoViewModel, scroll: ScrollState = rememberScrollState()) {
    val d = vm.drill
    LaunchedEffect(vm.drillSerial) { scroll.scrollTo(0) }
    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Практика", style = serif(32f, 500, lh = 1.05f, ls = -0.02f))
                Text("Найдите ошибку и назовите её.", style = sans(15f, color = C.Body))
            }
            Text(
                if (vm.scoreTotal > 0) "${vm.scoreRight} из ${vm.scoreTotal} сегодня" else "Разделы I–III",
                style = mono(12f),
                maxLines = 1,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }

        // Find → Name → Understand
        val pi = vm.drillPhase.ordinal
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Найти", "Назвать", "Понять").forEachIndexed { i, label ->
                val shape = RoundedCornerShape(15.dp)
                val bg = if (i == pi) C.Ink else if (i < pi) C.Sand else C.Paper
                val fg = if (i == pi) C.Paper else if (i < pi) C.Ink else C.DisabledText
                Row(
                    Modifier.height(30.dp).clip(shape).background(bg)
                        .border(1.dp, if (i > pi) C.Rule else bg, shape)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("${i + 1}", style = mono(11f, color = fg))
                    Text(label, style = sans(13f, 600, color = fg), maxLines = 1)
                }
            }
        }

        // The snippet
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.Card)
                .border(1.dp, C.Rule, RoundedCornerShape(20.dp))
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MonoLabel(d.source.uppercase(), ls = 0.08f, modifier = Modifier.weight(1f))
                if (d.ai) {
                    Row(
                        Modifier.height(24.dp).clip(RoundedCornerShape(12.dp)).background(C.BlueTint).padding(horizontal = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Ico(Icons.Outlined.AutoAwesome, 14.dp, C.Blue)
                        Text("Новое", style = sans(12f, 600, color = C.Blue), maxLines = 1)
                    }
                }
            }
            Snippet(vm)
        }

        val hint = when (vm.drillPhase) {
            DrillPhase.Spot -> if (vm.drillMiss.isNotEmpty()) "С этим предложением всё в порядке. Попробуйте другое." else "Нажмите на предложение, где рассуждение ломается."
            DrillPhase.Name -> "Зоркий глаз. Так что с ним не так?"
            DrillPhase.Result -> ""
        }
        if (hint.isNotEmpty()) {
            Text(hint, style = sans(15f, color = if (vm.drillMiss.isNotEmpty() && vm.drillPhase == DrillPhase.Spot) C.Red else C.Body))
        }

        if (vm.drillPhase != DrillPhase.Spot) Options(vm)

        if (vm.drillPhase == DrillPhase.Result) Result(vm)

        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(26.dp))
                    .border(1.5.dp, C.Ink, RoundedCornerShape(26.dp))
                    .clickable(onClick = vm::nextDrill),
                contentAlignment = Alignment.Center,
            ) { Text("Дальше", style = sans(15f, 600)) }
            Tappable(
                onClick = vm::genDrill,
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.weight(1.4f).height(52.dp),
                bg = C.Ink,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                Ico(Icons.Outlined.AutoAwesome, 18.dp, C.Paper)
                Text(if (vm.genLoading) "Пишем…" else "Сгенерировать", style = sans(15f, 600, color = C.Paper), maxLines = 1)
            }
        }
        if (d.ai && d.meta != null) {
            Text("Сгенерировано: ${d.meta}", style = mono(11f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Snippet(vm: ErgoViewModel) {
    val d = vm.drill
    val ranges = mutableListOf<IntRange>()
    val marks = mutableListOf<IntRange>()
    val text = buildAnnotatedString {
        d.sentences.forEachIndexed { i, t ->
            val start = length
            val found = vm.drillSpot != null && i in d.flawed && (vm.drillSpot == i || vm.drillPhase == DrillPhase.Result)
            val miss = i in vm.drillMiss
            if (miss) withStyle(SpanStyle(color = C.Locked, textDecoration = TextDecoration.LineThrough)) { append(t) } else append(t)
            ranges += start until length
            if (found) marks += start until length
            append(" ")
        }
    }
    MarkedText(
        text = text,
        style = serif(20f, lh = 1.55f),
        marks = marks,
        markStyle = vm.markStyle,
        onTapOffset = { off ->
            val i = ranges.indexOfFirst { off in it.first..it.last + 1 }
            if (i >= 0) vm.spot(i)
        },
    )
}

@Composable
private fun Options(vm: ErgoViewModel) {
    val d = vm.drill
    val res = vm.drillPhase == DrillPhase.Result
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        d.options.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { name ->
                    val isAns = name == d.answer
                    val isPick = name == vm.drillPick
                    val (border, bg, fg) = when {
                        res && isAns -> Triple(C.Blue, C.BlueTint, C.Blue)
                        res && isPick -> Triple(C.Red, C.RedTint, C.Red)
                        res -> Triple(C.Rule, C.Card, C.Locked)
                        else -> Triple(C.Rule, C.Card, C.Ink)
                    }
                    val shape = RoundedCornerShape(14.dp)
                    Box(
                        Modifier.weight(1f).fillMaxHeight().heightIn(min = 52.dp).clip(shape).background(bg)
                            .border(1.5.dp, border, shape)
                            .clickable(enabled = !res) { vm.pick(name) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(name, style = sans(14f, 600, lh = 1.3f, color = fg), textAlign = TextAlign.Center)
                    }
                }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Result(vm: ErgoViewModel) {
    val d = vm.drill
    val correct = vm.drillPick == d.answer
    val pick = vm.drillPick.orEmpty()
    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val color = if (correct) C.Blue else C.Red
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Ico(if (correct) Icons.Outlined.Check else Icons.Outlined.Close, 22.dp, color, Modifier.padding(top = 2.dp))
            Text(
                if (correct) "Верно: ${d.answer.lowercase()}." else "Не совсем. Это ${d.answer.lowercase()}.",
                style = serif(22f, 500, lh = 1.25f, color = color),
            )
        }
        Text(d.explanation, style = serif(17f, lh = 1.55f))
        if (!correct && vm.drillExplain.isEmpty() && !vm.drillExplainLoading) {
            Tappable(
                onClick = vm::explainWrong,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.heightIn(min = 44.dp),
                border = BorderStroke(1.5.dp, C.Red),
                padding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Ico(Icons.AutoMirrored.Outlined.Help, 18.dp, C.Red)
                Text("Почему не «${pick.lowercase()}»?", style = sans(15f, 600, color = C.Red))
            }
        }
        if (vm.drillExplainLoading) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Dots(C.Red)
                Text("Разбираемся…", style = sans(14f, color = C.Red))
            }
        }
        if (vm.drillExplain.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                    .clip(RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp)).background(C.RedTint)
            ) {
                Box(Modifier.width(3.dp).fillMaxHeight().background(C.Red))
                Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MonoLabel("ПОЧЕМУ НЕ «${pick.uppercase()}»", color = C.RedDeep, ls = 0.1f)
                    Text(vm.drillExplain, style = sans(15f, lh = 1.55f))
                    if (vm.drillExplainMeta.isNotEmpty()) Text(vm.drillExplainMeta, style = mono(11f, color = C.RedDeep))
                }
            }
        }
    }
}
