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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.ergo.ErgoViewModel
import app.ergo.LessonState
import app.ergo.Tab
import app.ergo.data.CHECK
import app.ergo.data.UNITS
import app.ergo.data.plural

@Composable
fun LearnScreen(vm: ErgoViewModel) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ErgoMark(24.dp, 21.dp, 7.dp)
            Text("Ergo", style = serif(26f, 600, ls = -0.02f))
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(C.Sand).padding(start = 8.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Ico(Icons.Filled.LocalFireDepartment, 18.dp, C.Red)
                Text("6 дней", style = sans(14f, 600), maxLines = 1)
            }
        }

        // Continue card
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(C.Ink).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            val done = vm.strawDone
            MonoLabel(if (done) "РАЗДЕЛ II · УРОК 3 ИЗ 5" else "РАЗДЕЛ II · УРОК 2 ИЗ 5", color = C.OnDarkMute)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (done) "Ложный след" else "Соломенное чучело", style = serif(34f, 500, lh = 1.05f, ls = -0.02f, color = C.Paper))
                Text(
                    if (done) "Сменить тему, но убедительно." else "Опровергнуть то, чего никто не утверждал.",
                    style = serif(18f, italic = true, color = C.OnDarkBody),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("4 мин · 5 шагов", style = sans(14f, color = C.OnDarkMute), maxLines = 1)
                Spacer(Modifier.weight(1f))
                Tappable(
                    onClick = vm::continueLesson,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.height(44.dp),
                    bg = C.Red,
                    padding = PaddingValues(start = 20.dp, end = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Продолжить", style = sans(15f, 600, color = Color.White), maxLines = 1)
                    Ico(Icons.AutoMirrored.Outlined.ArrowForward, 20.dp, Color.White)
                }
            }
        }

        // Review deck
        Tappable(
            onClick = { vm.go(Tab.Drill) },
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
            bg = C.Card,
            border = BorderStroke(1.dp, C.Rule),
            padding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(C.BlueTint), contentAlignment = Alignment.Center) {
                Text("9", style = serif(20f, 600, color = C.Blue))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Повторение", style = sans(16f, 600))
                Text("Карточки на сегодня, интервалы 1 · 3 · 7 · 21 день", style = sans(14f, color = C.Mute))
            }
            Ico(Icons.Outlined.ChevronRight, 22.dp, C.Mute)
        }

        // Syllabus
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Программа", style = serif(24f, 500), modifier = Modifier.alignByBaseline())
                Text("${vm.doneCount} из 23 уроков", style = mono(12f), modifier = Modifier.alignByBaseline())
            }
            Syllabus(vm)
        }
    }
}

@Composable
private fun Syllabus(vm: ErgoViewModel) {
    val doneCount = vm.doneCount
    var flat = 0
    UNITS.forEachIndexed { ui, u ->
        val states = u.lessons.map {
            val idx = flat++
            when {
                idx < doneCount -> LessonState.Done
                idx == doneCount -> LessonState.Current
                else -> LessonState.Locked
            }
        }
        val d = states.count { it == LessonState.Done }
        val cur = LessonState.Current in states
        val open = vm.openUnit == ui
        val meta = when {
            d == u.lessons.size -> "Пройден"
            cur -> "$d из ${u.lessons.size} · в процессе"
            else -> "${u.lessons.size} ${plural(u.lessons.size, "урок", "урока", "уроков")}"
        }
        Column(Modifier.rules(C.Rule, top = true)) {
            Row(
                Modifier.fillMaxWidth().clickable { vm.toggleUnit(ui) }.padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    u.n,
                    style = serif(24f, italic = true, color = if (cur) C.Red else if (d == u.lessons.size) C.Blue else C.Locked),
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
                    u.lessons.forEachIndexed { li, name ->
                        val st = states[li]
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { vm.lessonClicked(name, st) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            when (st) {
                                LessonState.Done -> Ico(Icons.Filled.CheckCircle, 20.dp, C.Blue)
                                LessonState.Current -> Ico(Icons.Outlined.RadioButtonChecked, 20.dp, C.Red)
                                LessonState.Locked -> Ico(Icons.Outlined.Lock, 20.dp, C.Locked)
                            }
                            Text(
                                name,
                                style = sans(15f, if (st == LessonState.Current) 700 else 500, color = if (st == LessonState.Locked) C.DisabledText else C.Ink),
                                modifier = Modifier.weight(1f),
                            )
                            Text("${3 + (flatIndex(ui, li) % 3)} мин", style = mono(12f), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

private fun flatIndex(unit: Int, lesson: Int) = UNITS.take(unit).sumOf { it.lessons.size } + lesson

// ── Lesson: Straw man ──────────────────────────────────────────────────────

@Composable
fun LessonScreen(vm: ErgoViewModel) {
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
                repeat(5) { i ->
                    val c by animateColorAsState(if (i <= vm.lessonStep) C.Ink else C.Disabled, label = "bar")
                    Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(c))
                }
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val minH = maxHeight - 36.dp
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 28.dp, end = 28.dp, top = 12.dp, bottom = 24.dp)) {
                val centered = Modifier.fillMaxWidth().heightIn(min = minH)
                when (vm.lessonStep) {
                    0 -> LessonIntro(centered)
                    1 -> LessonPattern()
                    2 -> LessonExample(vm)
                    3 -> LessonCheck(vm)
                    else -> LessonDone(vm, centered)
                }
            }
        }
        val passed = vm.checkPassed
        val label = listOf(
            "Начать",
            "Покажите",
            if (vm.lessonMarked) "Проверить себя" else "Отметить",
            if (passed) "Завершить" else "Выберите честный ответ",
            "К программе",
        )[vm.lessonStep]
        val marking = vm.lessonStep == 2 && !vm.lessonMarked
        PillButton(
            label,
            vm::lessonNext,
            Modifier.padding(start = 28.dp, end = 28.dp, top = 12.dp, bottom = 24.dp),
            enabled = !(vm.lessonStep == 3 && !passed),
            bg = if (marking) C.Red else null,
            fg = if (marking) Color.White else null,
        )
    }
}

@Composable
private fun LessonIntro(modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically)) {
        MonoLabel("РАЗДЕЛ II · УРОК 2", size = 12f)
        Text("Соломенное чучело", style = serif(48f, 500, lh = 1f, ls = -0.03f))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("P", style = serif(60f, lh = 1f, color = C.Blue), modifier = Modifier.alignByBaseline())
            Text("→", style = serif(30f, lh = 1f, color = C.Mute), modifier = Modifier.alignByBaseline())
            Text(
                "P′",
                style = serif(60f, lh = 1f, color = C.Red),
                modifier = Modifier.alignByBaseline().drawWithContent {
                    drawContent()
                    val h = 3.dp.toPx()
                    drawRect(C.Red, Offset(0f, size.height * 0.56f - h / 2), Size(size.width, h))
                },
            )
        }
        Text(
            "Подменить настоящий тезис собеседника более слабой версией, опровергнуть её и уйти с видом победителя.",
            style = serif(20f, lh = 1.5f),
        )
    }
}

@Composable
private fun LessonPattern() {
    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        MonoLabel("КАК ЭТО УСТРОЕНО", size = 12f)
        val p = SpanStyle(color = C.Blue, fontWeight = FontWeight.Bold)
        val p2 = SpanStyle(color = C.Red, fontWeight = FontWeight.Bold)
        val steps = listOf(
            buildAnnotatedString { append("А утверждает "); withStyle(p) { append("P") }; append(".") },
            buildAnnotatedString { append("Б пересказывает это как "); withStyle(p2) { append("P′") }; append(": грубее, радикальнее, смешнее.") },
            buildAnnotatedString { append("Б опровергает "); withStyle(p2) { append("P′") }; append(" и делает вид, что опроверг "); withStyle(p) { append("P") }; append(".") },
        )
        Column(Modifier.rules(C.Rule, top = true)) {
            steps.forEachIndexed { i, s ->
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
            MonoLabel("ВЫДАЁТ СЕБЯ ФРАЗОЙ", ls = 0.1f)
            Text("«То есть на самом деле вы хотите сказать…»", style = serif(18f, italic = true))
        }
    }
}

@Composable
private fun LessonExample(vm: ErgoViewModel) {
    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        MonoLabel("ПРИМЕР · ГОРОДСКИЕ СЛУШАНИЯ", size = 12f)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("МАША", style = sans(13f, 700, color = C.Blue))
            Text("Давайте перенесём небольшую часть бюджета на дороги в строительство защищённых велодорожек.", style = serif(20f, lh = 1.5f))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("ЛЁША", style = sans(13f, 700))
            val flaw = "То есть вы хотите запретить машины в центре?"
            MarkedText(
                text = buildAnnotatedString { append(flaw); append(" Удачи довезти продукты домой на велосипеде.") },
                style = serif(20f, lh = 1.5f),
                marks = if (vm.lessonMarked) listOf(flaw.indices) else emptyList(),
                markStyle = vm.markStyle,
            )
        }
        if (vm.lessonMarked) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.Card)
                    .border(1.5.dp, C.Red, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Ico(Icons.Outlined.Edit, 16.dp, C.Red)
                    Text("P′ ≠ P", style = sans(14f, 700, color = C.Red))
                }
                Text(
                    "Маша сказала «небольшую часть». Лёша услышал «запретить машины». Всё дальнейшее бьёт по позиции, которой в зале никто не придерживается.",
                    style = sans(15f, lh = 1.5f),
                )
            }
        } else {
            Text("Где Лёша подменяет P на P′? Нажмите «Отметить».", style = sans(15f, color = C.Mute))
        }
    }
}

@Composable
private fun LessonCheck(vm: ErgoViewModel) {
    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        MonoLabel("ПРОВЕРКА", size = 12f)
        Text("Какой ответ спорит с тем, что Маша сказала на самом деле?", style = serif(26f, 500, lh = 1.25f))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CHECK.forEachIndexed { i, c ->
                val sel = vm.lessonAns == i
                val shape = RoundedCornerShape(16.dp)
                Column(
                    Modifier.fillMaxWidth().clip(shape)
                        .background(if (sel) (if (c.ok) C.BlueTint else C.RedTint) else C.Card)
                        .border(1.5.dp, if (sel) (if (c.ok) C.Blue else C.Red) else C.Rule, shape)
                        .clickable { vm.answerCheck(i) }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(c.text, style = serif(17f, lh = 1.45f))
                    if (sel) Text(c.fb, style = sans(14f, 600, lh = 1.45f, color = if (c.ok) C.Blue else C.Red))
                }
            }
        }
    }
}

@Composable
private fun LessonDone(vm: ErgoViewModel, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically)) {
        ErgoMark(64.dp, 56.dp, 18.dp)
        Text("Урок пройден.", style = serif(42f, 500, lh = 1.05f, ls = -0.02f))
        Text(
            "«Соломенное чучело» теперь в колоде повторения. Карточка вернётся завтра, а потом всё реже, пока вы отвечаете правильно.",
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
        Box(Modifier.height(44.dp).clickable(onClick = vm::lessonToDrill), contentAlignment = Alignment.CenterStart) {
            Text("Потренироваться прямо сейчас", style = sans(15f, 600, color = C.Red).copy(textDecoration = TextDecoration.Underline))
        }
    }
}

