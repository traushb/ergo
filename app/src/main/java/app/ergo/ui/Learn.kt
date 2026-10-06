package app.ergo.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.ergo.ErgoViewModel
import app.ergo.LessonRun
import app.ergo.data.Accent
import app.ergo.data.TopicKind
import app.ergo.data.count
import app.ergo.lessonMinutes

@Composable
fun LearnScreen(vm: ErgoViewModel) {
    val prog = vm.progress
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ErgoMark(24.dp, 21.dp, 7.dp)
            Text("Ergo", style = serif(26f, 600, ls = -0.02f))
            Spacer(Modifier.weight(1f))
            val streak = vm.streak
            Row(
                Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(C.Sand).padding(start = 8.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Ico(Icons.Filled.LocalFireDepartment, 18.dp, if (streak > 0) C.Red else C.Locked)
                Text(count(streak, "день", "дня", "дней"), style = sans(14f, 600), maxLines = 1)
            }
        }

        val next = vm.nextLesson
        if (next != null) {
            val unit = vm.bank.unitOf(next.id)
            HeroCard(
                kicker = "РАЗДЕЛ ${unit.n} · УРОК ${vm.bank.lessonNumber(next.id)} ИЗ ${unit.topicIds.size}",
                title = next.name,
                sub = next.tagline,
                meta = "${lessonMinutes(vm.bank, next)} мин · 5 шагов",
                button = if (vm.doneCount == 0) "Начать" else "Продолжить",
                onClick = vm::continueLearning,
                titleSize = 34f,
                italicSub = true,
            )
        } else {
            HeroCard(
                kicker = "ПРОГРАММА ПРОЙДЕНА",
                title = "Все уроки позади",
                sub = "Теперь — тренировки, блиц и повторение.",
                meta = count(vm.doneCount, "урок", "урока", "уроков"),
                button = "К практике",
                onClick = vm::continueLearning,
                titleSize = 34f,
                italicSub = true,
            )
        }

        val due = vm.dueCount
        Tappable(
            onClick = vm::startReview,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
            bg = C.Card,
            border = BorderStroke(1.dp, C.Rule),
            padding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(C.BlueTint), contentAlignment = Alignment.Center) {
                Text("$due", style = serif(20f, 600, color = C.Blue))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Повторение", style = sans(16f, 600))
                val sub = when {
                    due > 0 -> "${count(due, "тема", "темы", "тем")} на сегодня"
                    prog.stats.isEmpty() -> "Темы попадают сюда после уроков и тренировок"
                    else -> prog.nextDueAfter(vm.today())?.let { "На сегодня всё. Следующее — через ${count((it - vm.today()).toInt(), "день", "дня", "дней")}" } ?: "На сегодня всё"
                }
                Text(sub, style = sans(14f, color = C.Mute))
            }
            Ico(Icons.Outlined.ChevronRight, 22.dp, C.Mute)
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Программа", style = serif(24f, 500), modifier = Modifier.alignByBaseline())
                // After «из» the noun is genitive: «из 21 урока», «из 26 уроков».
                Text("${vm.doneCount} из ${count(vm.bank.lessonOrder.size, "урока", "уроков", "уроков")}", style = mono(12f), modifier = Modifier.alignByBaseline())
            }
            Syllabus(vm)
        }
    }
}

@Composable
private fun Syllabus(vm: ErgoViewModel) {
    val prog = vm.progress
    val next = vm.nextLesson?.id
    vm.bank.units.forEach { u ->
        val topics = vm.bank.topicsIn(u.index)
        val done = topics.count { it.id in prog.lessons }
        val current = topics.any { it.id == next }
        val open = vm.openUnit == u.index
        val meta = when {
            done == topics.size -> "Пройден"
            done > 0 || current -> "$done из ${topics.size} · в процессе"
            else -> count(topics.size, "урок", "урока", "уроков")
        }
        Column(Modifier.rules(C.Rule, top = true)) {
            Row(
                Modifier.fillMaxWidth().clickable { vm.toggleUnit(u.index) }.padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    u.n,
                    style = serif(24f, italic = true, color = if (current) C.Red else if (done == topics.size) C.Blue else C.Locked),
                    modifier = Modifier.width(36.dp),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(u.title, style = sans(16f, 600))
                    Text(meta, style = mono(12f))
                }
                val rot by animateFloatAsState(if (open) 180f else 0f, label = "chev")
                Ico(Icons.Outlined.ExpandMore, 22.dp, C.Mute, Modifier.rotate(rot))
            }
            if (open) {
                Column(Modifier.padding(start = 50.dp, bottom = 12.dp)) {
                    topics.forEach { t ->
                        val isDone = t.id in prog.lessons
                        val isNext = t.id == next
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { vm.openLesson(t.id) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            when {
                                isDone -> Ico(Icons.Filled.CheckCircle, 20.dp, C.Blue)
                                isNext -> Ico(Icons.Outlined.RadioButtonChecked, 20.dp, C.Red)
                                else -> Ico(Icons.Outlined.Circle, 20.dp, C.Locked)
                            }
                            Text(t.name, style = sans(15f, if (isNext) 700 else 500), modifier = Modifier.weight(1f))
                            Text("${lessonMinutes(vm.bank, t)} мин", style = mono(12f), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

// ── Lesson ─────────────────────────────────────────────────────────────────

@Composable
fun LessonScreen(vm: ErgoViewModel, l: LessonRun) {
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Paper)
            .blockTouches()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 10.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RoundIconButton(Icons.Outlined.Close, vm::closeLesson, 44.dp, 24.dp)
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(LessonRun.STEPS) { i ->
                    val c by animateColorAsState(if (i <= l.step) C.Ink else C.Disabled, label = "bar")
                    Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(c))
                }
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val minH = maxHeight - 36.dp
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 28.dp, end = 28.dp, top = 12.dp, bottom = 24.dp)) {
                val centered = Modifier.fillMaxWidth().heightIn(min = minH)
                when (l.step) {
                    0 -> LessonIntro(vm, l, centered)
                    1 -> LessonPattern(l)
                    2 -> LessonExample(vm, l)
                    3 -> LessonCheck(vm, l)
                    else -> LessonDone(vm, l, centered)
                }
            }
        }
        val label = listOf(
            "Начать",
            "Покажите пример",
            if (l.marked) "Проверить себя" else "Отметить",
            if (l.passed) "Завершить" else "Выберите ответ",
            "К программе",
        )[l.step]
        val marking = l.step == 2 && !l.marked
        PillButton(
            label,
            vm::lessonNext,
            Modifier.padding(start = 28.dp, end = 28.dp, top = 12.dp, bottom = 24.dp),
            enabled = !(l.step == 3 && !l.passed),
            bg = if (marking) C.RedFill else null,
            fg = if (marking) Color.White else null,
        )
    }
}

private fun structural(l: LessonRun) = l.topic.kind == TopicKind.Structure || l.topic.kind == TopicKind.Logic

@Composable
private fun LessonIntro(vm: ErgoViewModel, l: LessonRun, modifier: Modifier) {
    val t = l.topic
    Column(modifier, verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically)) {
        MonoLabel("РАЗДЕЛ ${vm.bank.unitOf(t.id).n} · УРОК ${vm.bank.lessonNumber(t.id)}", size = 12f)
        Text(t.name, style = serif(44f, 500, lh = 1.02f, ls = -0.03f))
        Text(t.formula, style = serif(34f, lh = 1.1f, color = if (structural(l)) C.Blue else C.Red))
        Text(t.definition, style = serif(20f, lh = 1.5f))
    }
}

@Composable
private fun LessonPattern(l: LessonRun) {
    val t = l.topic
    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        MonoLabel("КАК ЭТО УСТРОЕНО", size = 12f)
        Column(Modifier.rules(C.Rule, top = true)) {
            t.steps.forEachIndexed { i, s ->
                Row(Modifier.fillMaxWidth().rules(C.Rule, bottom = true).padding(vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("${i + 1}", style = serif(22f, italic = true, color = C.Mute), modifier = Modifier.width(20.dp))
                    Text(s, style = sans(17f, lh = 1.45f))
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.Sand).padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MonoLabel(if (structural(l)) "ПОДСКАЗКА" else "ВЫДАЁТ СЕБЯ ФРАЗОЙ", ls = 0.1f)
            Text(t.tell, style = serif(18f, italic = true, lh = 1.4f))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MonoLabel("КАК ОТВЕТИТЬ", ls = 0.1f)
            Text(t.counter, style = sans(16f, lh = 1.5f))
        }
    }
}

@Composable
private fun LessonExample(vm: ErgoViewModel, l: LessonRun) {
    val ex = l.example
    val accentColor = if (ex.accent == Accent.Blue) C.Blue else C.Red
    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        MonoLabel(ex.label, size = 12f)
        ex.lines.forEach { line ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                line.who?.let { who ->
                    Text(who, style = sans(13f, 700, color = if (line.whoAccent == Accent.Blue) C.Blue else C.Ink))
                }
                MarkedText(
                    text = buildAnnotatedString { append(line.text) },
                    style = serif(20f, lh = 1.5f),
                    marks = if (l.marked) line.marks.map { TextMark(it, ex.accent) } else emptyList(),
                    markStyle = vm.markStyle,
                )
            }
        }
        if (l.marked) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.Card)
                    .border(1.5.dp, accentColor, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Ico(Icons.Outlined.Edit, 16.dp, accentColor)
                    Text(ex.noteTitle, style = sans(14f, 700, color = accentColor))
                }
                Text(ex.note, style = sans(15f, lh = 1.5f))
            }
        } else if (ex.prompt.isNotEmpty()) {
            Text(ex.prompt, style = sans(15f, color = C.Mute))
        }
    }
}

@Composable
private fun LessonCheck(vm: ErgoViewModel, l: LessonRun) {
    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        MonoLabel("ПРОВЕРКА", size = 12f)
        Text(l.check.question, style = serif(24f, 500, lh = 1.25f))
        ChoiceList(
            options = l.check.options.map { it.text },
            state = { i ->
                when {
                    l.answer != i -> OptState.Idle
                    l.check.options[i].ok -> OptState.Right
                    else -> OptState.Wrong
                }
            },
            feedback = { i -> if (l.answer == i) l.check.options[i].fb else null },
            enabled = !l.passed,
            onPick = vm::lessonAnswer,
        )
    }
}

@Composable
private fun LessonDone(vm: ErgoViewModel, l: LessonRun, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically)) {
        ErgoMark(64.dp, 56.dp, 18.dp)
        Text("Урок пройден.", style = serif(42f, 500, lh = 1.05f, ls = -0.02f))
        Text(
            "«${l.topic.name}» теперь в колоде повторения. Тема вернётся завтра, а потом всё реже, пока вы отвечаете правильно.",
            style = sans(16f, lh = 1.5f, color = C.Body),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("1 д", "3 д", "7 д", "21 д").forEachIndexed { i, s ->
                Text(
                    s,
                    style = mono(12f, color = if (i == 0) C.Paper else C.Ink),
                    modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(if (i == 0) C.Ink else C.Sand).padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        val n = vm.bank.drillsFor(l.topic.id).size + vm.bank.quizzesFor(l.topic.id).size
        Box(Modifier.height(44.dp).clickable(onClick = vm::lessonToPractice), contentAlignment = Alignment.CenterStart) {
            Text(
                if (n > 0) "Потренироваться прямо сейчас · ${count(n, "задание", "задания", "заданий")}" else "Потренироваться прямо сейчас",
                style = sans(15f, 600, color = C.Red).copy(textDecoration = TextDecoration.Underline),
            )
        }
    }
}
