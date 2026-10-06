package app.ergo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.outlined.DoNotDisturb
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.ergo.ErgoViewModel
import app.ergo.FPhase
import app.ergo.FallacyRun
import app.ergo.QuizRun
import app.ergo.SPhase
import app.ergo.StructureRun
import app.ergo.data.Accent
import app.ergo.data.sentenceRanges

// The three kinds of exercise inside a practice session.

/** Find the broken sentence, name the fallacy, read why. */
@Composable
fun FallacyView(vm: ErgoViewModel, r: FallacyRun) {
    val p = vm.practice
    val d = r.drill
    val topic = vm.bank.topic(d.topic)
    StepChips(listOf("Найти", "Назвать", "Понять"), r.phase.ordinal)

    SnippetCard(d.source, d.ai) {
        val ranges = sentenceRanges(d.sentences)
        val struck = SpanStyle(color = C.Locked, textDecoration = TextDecoration.LineThrough)
        val text = buildAnnotatedString {
            d.sentences.forEachIndexed { i, t ->
                if (i in r.misses) withStyle(struck) { append(t) } else append(t)
                if (i < d.sentences.lastIndex) append(" ")
            }
        }
        val marked = when {
            d.clean -> emptyList()
            r.phase == FPhase.Result -> d.flawed
            r.phase == FPhase.Name -> listOfNotNull(r.spot)
            else -> emptyList()
        }
        MarkedText(
            text = text,
            style = serif(20f, lh = 1.55f),
            marks = marked.map { TextMark(ranges[it]) },
            markStyle = vm.markStyle,
            onTapOffset = if (r.phase == FPhase.Spot) { off ->
                ranges.indexOfFirst { off in it.first..it.last + 1 }.takeIf { it >= 0 }?.let(p::tap)
            } else null,
        )
    }

    if (r.phase == FPhase.Spot) {
        val (hint, warn) = when {
            r.falseAlarm -> "Ошибка здесь есть — поищите внимательнее." to true
            d.clean && r.misses.size >= 2 -> "С этими предложениями всё в порядке. Может, ошибки нет вовсе?" to true
            r.misses.isNotEmpty() -> "С этим предложением всё в порядке. Попробуйте другое." to true
            else -> "Нажмите на предложение, где рассуждение ломается. Или решите, что ошибки нет." to false
        }
        Text(hint, style = sans(15f, lh = 1.4f, color = if (warn) C.Red else C.Body))
        OutlineButton("Ошибки нет", p::noFlaw, height = 44.dp, icon = Icons.Outlined.DoNotDisturb)
    } else if (r.phase == FPhase.Name) {
        Text("Зоркий глаз. Так что с ним не так?", style = sans(15f, color = C.Body))
    }

    if (!d.clean && r.phase != FPhase.Spot) {
        OptionGrid(
            options = r.options,
            label = { vm.bank.topic(it)?.label ?: it },
            state = { o ->
                when {
                    r.phase != FPhase.Result -> OptState.Idle
                    o == d.topic -> OptState.Right
                    o == r.pick -> OptState.Wrong
                    else -> OptState.Dim
                }
            },
            enabled = r.phase == FPhase.Name,
            onPick = p::choose,
        )
    }

    if (r.resolved) {
        Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val label = topic?.label?.lowercase() ?: ""
            when {
                d.clean -> Verdict(r.correct, if (r.correct) "Верно: здесь нет ошибки." else "Ошибки здесь действительно нет — а вы её искали.")
                else -> Verdict(r.correct, if (r.correct) "Верно: $label." else "Не совсем. Это $label.")
            }
            Text(d.explanation, style = serif(17f, lh = 1.55f))
            val pick = r.pick
            if (!d.clean && !r.correct && pick != null) {
                val pickLabel = vm.bank.topic(pick)?.label ?: pick
                if (r.explain.isEmpty() && !r.explainLoading) {
                    Tappable(
                        onClick = p::explainWrong,
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.heightIn(min = 44.dp),
                        border = BorderStroke(1.5.dp, C.Red),
                        padding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Ico(Icons.AutoMirrored.Outlined.Help, 18.dp, C.Red)
                        Text("Почему не «${pickLabel.lowercase()}»?", style = sans(15f, 600, color = C.Red))
                    }
                }
                if (r.explainLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Dots(C.Red)
                        Text("Разбираемся…", style = sans(14f, color = C.Red))
                    }
                }
                if (r.explain.isNotEmpty()) NoteBox("ПОЧЕМУ НЕ «${pickLabel.uppercase()}»", r.explain, r.explainMeta)
            }
            ReferenceLink(vm, d.topic)
            if (d.ai && d.meta != null) Text("Сгенерировано: ${d.meta}", style = mono(11f))
        }
    }
}

/** Find the conclusion, a premise, then the hidden assumption. */
@Composable
fun StructureView(vm: ErgoViewModel, r: StructureRun) {
    val p = vm.practice
    val item = r.item
    StepChips(listOf("Вывод", "Довод", "Допущение"), r.phase.ordinal)

    SnippetCard(item.source, item.ai) {
        val ranges = sentenceRanges(item.sentences)
        val struck = SpanStyle(color = C.Locked, textDecoration = TextDecoration.LineThrough)
        val found = r.phase != SPhase.Conclusion
        val misses = when (r.phase) {
            SPhase.Conclusion -> r.conclusionMisses
            SPhase.Premise -> r.premiseMisses
            else -> emptyList()
        }
        val text = buildAnnotatedString {
            item.sentences.forEachIndexed { i, t ->
                when {
                    found && i == item.conclusion -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(t) }
                    i in misses -> withStyle(struck) { append(t) }
                    else -> append(t)
                }
                if (i < item.sentences.lastIndex) append(" ")
            }
        }
        val marks = buildList {
            if (found) add(item.conclusion)
            if (r.phase == SPhase.Result) addAll(item.premises) else r.premise?.let { add(it) }
        }.distinct().map { TextMark(ranges[it], Accent.Blue) }
        val tappable = r.phase == SPhase.Conclusion || r.phase == SPhase.Premise
        MarkedText(
            text = text,
            style = serif(20f, lh = 1.55f),
            marks = marks,
            markStyle = vm.markStyle,
            onTapOffset = if (tappable) { off ->
                ranges.indexOfFirst { off in it.first..it.last + 1 }.takeIf { it >= 0 }?.let(p::tap)
            } else null,
        )
    }

    val (hint, warn) = when (r.phase) {
        SPhase.Conclusion ->
            if (r.conclusionMisses.isEmpty()) "Нажмите на вывод — то, ради чего всё сказано." to false
            else "Это не вывод: эта фраза что-то обосновывает. Что из всего этого следует?" to true
        SPhase.Premise -> when {
            r.tappedConclusion -> "Это вывод. Теперь найдите довод в его поддержку." to true
            r.premiseMisses.isNotEmpty() -> "Это не довод в пользу вывода. Попробуйте другое." to true
            else -> "Вывод найден. Теперь нажмите на довод — посылку." to false
        }
        SPhase.Assumption -> "Что автор молча допускает, чтобы вывод следовал?" to false
        SPhase.Result -> "" to false
    }
    if (hint.isNotEmpty()) Text(hint, style = sans(15f, lh = 1.4f, color = if (warn) C.Red else C.Body))

    if (r.phase == SPhase.Assumption || r.phase == SPhase.Result) {
        MonoLabel("СКРЫТОЕ ДОПУЩЕНИЕ", color = C.Blue)
        ChoiceList(
            options = r.options,
            state = { i ->
                when {
                    r.phase != SPhase.Result -> OptState.Idle
                    i == r.answer -> OptState.Right
                    i == r.pick -> OptState.Wrong
                    else -> OptState.Dim
                }
            },
            feedback = { i ->
                when {
                    r.phase != SPhase.Result -> null
                    i == r.answer -> "Да: без этого вывод не следует."
                    i == r.pick -> "Это рассуждению не нужно: вывод держится и без этого."
                    else -> null
                }
            },
            enabled = r.phase == SPhase.Assumption,
            onPick = p::chooseOption,
            serifText = false,
        )
    }

    if (r.resolved) {
        val points = listOf(r.conclusionMisses.isEmpty(), r.premiseMisses.isEmpty(), r.pick == r.answer).count { it }
        Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Verdict(points == 3, if (points == 3) "Разобрано без единой ошибки." else "Разобрано: $points из 3.")
            Text(item.explanation, style = serif(17f, lh = 1.55f))
            ReferenceLink(vm, "assumptions")
            if (item.ai && item.meta != null) Text("Сгенерировано: ${item.meta}", style = mono(11f))
        }
    }
}

/** A passage and one multiple-choice question. */
@Composable
fun QuizView(vm: ErgoViewModel, r: QuizRun) {
    val p = vm.practice
    val q = r.quiz
    SnippetCard(q.source, q.ai) {
        Text(q.passage, style = serif(19f, lh = 1.5f))
    }
    Text(q.question, style = serif(21f, 500, lh = 1.3f))
    val pick = r.pick
    ChoiceList(
        options = q.options,
        state = { i ->
            when {
                pick == null -> OptState.Idle
                i == q.answer -> OptState.Right
                i == pick -> OptState.Wrong
                else -> OptState.Dim
            }
        },
        feedback = { null },
        enabled = pick == null,
        onPick = p::chooseOption,
        serifText = false,
    )
    if (pick != null) {
        Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Verdict(pick == q.answer, if (pick == q.answer) "Верно." else "Не совсем.")
            Text(q.explanation, style = serif(17f, lh = 1.55f))
            ReferenceLink(vm, q.topic)
            if (q.ai && q.meta != null) Text("Сгенерировано: ${q.meta}", style = mono(11f))
        }
    }
}

/** "Справка: Ложная дилемма" — opens the topic's reference card. */
@Composable
fun ReferenceLink(vm: ErgoViewModel, topicId: String) {
    val t = vm.bank.topic(topicId) ?: return
    Row(
        Modifier.clickable { vm.openReference(topicId) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Ico(Icons.Outlined.Info, 18.dp, C.Mute)
        Text("Справка: ${t.name}", style = sans(14f, 600, color = C.Mute))
    }
}
