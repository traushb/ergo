package app.ergo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ergo.Blitz
import app.ergo.ErgoViewModel
import app.ergo.FallacyRun
import app.ergo.LoadingRun
import app.ergo.QuizRun
import app.ergo.Session
import app.ergo.SessionKind
import app.ergo.StructureRun
import app.ergo.Summary
import app.ergo.SummaryKind
import app.ergo.TopicFilter
import app.ergo.data.TopicStat
import app.ergo.data.count
import app.ergo.data.plural
import kotlinx.coroutines.delay

@Composable
fun PracticeScreen(vm: ErgoViewModel) {
    val p = vm.practice
    val summary = p.summary
    val blitz = p.blitz
    val session = p.session
    when {
        summary != null -> SummaryScreen(vm, summary)
        blitz != null -> BlitzScreen(vm, blitz)
        session != null -> SessionScreen(vm, session)
        else -> PracticeHome(vm)
    }
}

/** "верно 3 · ошибок 1 · повтор через 3 дня" */
fun masteryText(st: TopicStat, today: Long): String {
    if (st.box == 0) return "ещё не тренировали"
    val due = st.due - today
    val next = if (due <= 0) "повторить сегодня" else "повтор через ${count(due.toInt(), "день", "дня", "дней")}"
    return "верно ${st.right} · ошибок ${st.wrong} · $next"
}

// ── Home ───────────────────────────────────────────────────────────────────

@Composable
private fun PracticeHome(vm: ErgoViewModel) {
    val p = vm.practice
    val prog = vm.progress
    val day = vm.today()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Практика", style = serif(32f, 500, lh = 1.05f, ls = -0.02f))
                Text("Найдите ошибку и назовите её.", style = sans(15f, color = C.Body))
            }
            Text("сегодня: ${prog.solvedOn(day)}", style = mono(12f), maxLines = 1, modifier = Modifier.padding(bottom = 3.dp))
        }

        val topics = p.topicsOf(p.filter)
        Tappable(
            onClick = vm::openTopics,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
            bg = C.Card,
            border = BorderStroke(1.dp, C.Rule),
            padding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                MonoLabel("ТЕМА", ls = 0.1f)
                Text(p.label(), style = sans(16f, 600), maxLines = 2)
                val meta = if (topics.size == 1) masteryText(prog.stat(topics[0].id), day)
                else "${count(topics.size, "тема", "темы", "тем")} · освоено ${topics.count { prog.stat(it.id).box >= 3 }}"
                Text(meta, style = mono(12f))
            }
            Ico(Icons.Outlined.UnfoldMore, 22.dp, C.Mute)
        }

        HeroCard(
            kicker = "ТРЕНИРОВКА",
            title = "Найти, назвать, понять",
            sub = "Отметьте, где рассуждение ломается, назовите приём и разберите ошибку. Иногда ошибки нет вовсе.",
            meta = "6–8 заданий",
            button = "Начать",
            onClick = { p.startPractice() },
        )

        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModeCard(
                Modifier.weight(1f).fillMaxHeight(),
                onClick = { p.startBlitz() },
                title = "Блиц",
                sub = if (prog.blitzBest > 0) "60 секунд · рекорд ${prog.blitzBest}" else "60 секунд, кто быстрее",
            ) { Ico(Icons.Filled.Bolt, 22.dp, C.Red) }
            val due = prog.dueOn(day).size
            ModeCard(
                Modifier.weight(1f).fillMaxHeight(),
                onClick = vm::startReview,
                title = "Повторение",
                sub = if (due > 0) "${count(due, "тема", "темы", "тем")} на сегодня" else "Пока пусто",
            ) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(C.BlueTint), contentAlignment = Alignment.Center) {
                    Text("$due", style = sans(13f, 700, color = C.Blue))
                }
            }
        }
        if (vm.connected) {
            ModeCard(
                Modifier.fillMaxWidth(),
                onClick = { p.startFresh() },
                title = "Свежие задания",
                sub = "5 новых упражнений от модели по выбранной теме",
            ) { Ico(Icons.Outlined.AutoAwesome, 22.dp, C.Blue) }
        }

        val weak = prog.weakSpots()
        if (weak.isNotEmpty()) {
            Column {
                MonoLabel("СЛАБЫЕ МЕСТА", color = C.Red, modifier = Modifier.padding(bottom = 6.dp))
                weak.forEach { id ->
                    val t = vm.bank.topic(id) ?: return@forEach
                    val st = prog.stat(id)
                    Row(
                        Modifier.fillMaxWidth().rules(C.Rule, top = true).clickable { vm.train(id) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(t.label, style = sans(15f, 600))
                            Text("ошибок ${st.wrong} · верно ${st.right}", style = mono(12f))
                        }
                        Text("Тренировать", style = sans(14f, 600, color = C.Red))
                        Ico(Icons.Outlined.ChevronRight, 20.dp, C.Red)
                    }
                }
            }
        }

        Column {
            MonoLabel("ПРОГРЕСС", modifier = Modifier.padding(bottom = 6.dp))
            if (p.filter == TopicFilter.All) {
                vm.bank.units.forEach { u ->
                    val ts = vm.bank.topicsIn(u.index)
                    val mastered = ts.count { prog.stat(it.id).box >= 3 }
                    val touched = ts.count { prog.stat(it.id).box > 0 }
                    Row(
                        Modifier.fillMaxWidth().rules(C.Rule, top = true).clickable { p.select(TopicFilter.Section(u.index)) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(u.n, style = serif(20f, italic = true, color = if (touched > 0) C.Blue else C.Locked), modifier = Modifier.width(34.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(u.title, style = sans(15f, 600))
                            ProgressLine(if (ts.isEmpty()) 0f else (mastered + touched) / (2f * ts.size))
                        }
                        Text("$mastered/${ts.size}", style = mono(12f))
                    }
                }
            } else {
                val unit = when (val f = p.filter) {
                    is TopicFilter.Section -> f.unit
                    is TopicFilter.One -> vm.bank.unitIndexOf(f.topic)
                    TopicFilter.All -> 0
                }
                vm.bank.topicsIn(unit).forEach { t ->
                    Row(
                        Modifier.fillMaxWidth().rules(C.Rule, top = true).clickable { vm.openReference(t.id) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(t.name, style = sans(15f, if (p.filter == TopicFilter.One(t.id)) 700 else 500), modifier = Modifier.weight(1f))
                        Pips(prog.stat(t.id).box)
                        Ico(Icons.Outlined.Info, 18.dp, C.Mute)
                    }
                }
                Text(
                    "Все темы",
                    style = sans(14f, 600, color = C.Red),
                    modifier = Modifier.padding(top = 10.dp).clickable { p.select(TopicFilter.All) },
                )
            }
        }
    }
}

@Composable
private fun ProgressLine(fraction: Float) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), label = "progress")
    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(C.Sand)) {
        Box(Modifier.fillMaxWidth(f).fillMaxHeight().background(C.Blue))
    }
}

/** The dark feature card with a red call to action. */
@Composable
fun HeroCard(kicker: String, title: String, sub: String, meta: String, button: String, onClick: () -> Unit, titleSize: Float = 30f, italicSub: Boolean = false) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(C.Hero).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MonoLabel(kicker, color = C.HeroMute)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = serif(titleSize, 500, lh = 1.05f, ls = -0.02f, color = C.OnHero))
            Text(sub, style = if (italicSub) serif(18f, italic = true, color = C.HeroBody) else sans(15f, lh = 1.45f, color = C.HeroBody))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(meta, style = sans(14f, color = C.HeroMute), maxLines = 1, modifier = Modifier.weight(1f))
            Tappable(
                onClick = onClick,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.height(44.dp),
                bg = C.RedFill,
                padding = PaddingValues(start = 20.dp, end = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(button, style = sans(15f, 600, color = Color.White), maxLines = 1)
                Ico(Icons.AutoMirrored.Outlined.ArrowForward, 20.dp, Color.White)
            }
        }
    }
}

@Composable
private fun ModeCard(modifier: Modifier, onClick: () -> Unit, title: String, sub: String, icon: @Composable () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(C.Card).border(1.dp, C.Rule, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        icon()
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = sans(16f, 600))
            Text(sub, style = sans(13f, lh = 1.35f, color = C.Mute))
        }
    }
}

// ── Session ────────────────────────────────────────────────────────────────

@Composable
private fun SessionScreen(vm: ErgoViewModel, s: Session) {
    val p = vm.practice
    val scroll = rememberScrollState()
    LaunchedEffect(s.serial) { scroll.scrollTo(0) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RoundIconButton(Icons.Outlined.Close, p::quit, 44.dp, 24.dp)
            SegmentBar(s.total, s.index + if (s.run.resolved) 1 else 0, Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${s.points}", style = sans(16f, 700))
                Text(plural(s.points, "очко", "очка", "очков"), style = mono(12f))
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(scroll).padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            val kind = when (s.kind) {
                SessionKind.Practice -> "ТРЕНИРОВКА"
                SessionKind.Review -> "ПОВТОРЕНИЕ"
                SessionKind.Fresh -> "СВЕЖИЕ ЗАДАНИЯ"
            }
            MonoLabel("$kind · ${s.index + 1} ИЗ ${s.total}")
            when (val r = s.run) {
                is FallacyRun -> FallacyView(vm, r)
                is StructureRun -> StructureView(vm, r)
                is QuizRun -> QuizView(vm, r)
                LoadingRun -> SnippetCard("Модель пишет задание") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Dots(C.Mute)
                        Text("Обычно это несколько секунд…", style = sans(15f, color = C.Mute))
                    }
                }
            }
            SessionButtons(vm, s)
        }
    }
}

@Composable
private fun SessionButtons(vm: ErgoViewModel, s: Session) {
    val p = vm.practice
    val last = s.index + 1 >= s.total
    when {
        s.run.resolved -> PillButton(if (last) "Итоги" else "Дальше", p::next, Modifier.padding(top = 6.dp), height = 52.dp, fontSize = 15f)
        s.run is LoadingRun -> {}
        else -> Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlineButton("Пропустить", p::next, Modifier.weight(1f))
            if (vm.connected) {
                Tappable(
                    onClick = p::regenerate,
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier.weight(1.4f).height(52.dp),
                    bg = C.Ink,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    if (s.generating) Dots(C.Paper) else Ico(Icons.Outlined.AutoAwesome, 18.dp, C.Paper)
                    Text(if (s.generating) "Пишем…" else "Сгенерировать", style = sans(15f, 600, color = C.Paper), maxLines = 1)
                }
            }
        }
    }
}

// ── Summary ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryScreen(vm: ErgoViewModel, sum: Summary) {
    val p = vm.practice
    val blitz = sum.kind == SummaryKind.Blitz
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RoundIconButton(Icons.Outlined.Close, p::closeSummary, 44.dp, 24.dp, Modifier.padding(end = 2.dp))
            MonoLabel(
                when (sum.kind) {
                    SummaryKind.Blitz -> "БЛИЦ · ВРЕМЯ ВЫШЛО"
                    SummaryKind.Practice -> "ТРЕНИРОВКА ЗАВЕРШЕНА"
                    SummaryKind.Review -> "ПОВТОРЕНИЕ ЗАВЕРШЕНО"
                    SummaryKind.Fresh -> "СВЕЖИЕ ЗАДАНИЯ"
                },
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ErgoMark(48.dp, 42.dp, 14.dp)
            if (blitz) {
                Text("${sum.points} ${plural(sum.points, "очко", "очка", "очков")}", style = serif(44f, 500, lh = 1.05f, ls = -0.02f))
                Text(
                    if (sum.newRecord) "Новый рекорд!" else "Рекорд — ${sum.blitzBest}",
                    style = sans(16f, 600, color = if (sum.newRecord) C.Red else C.Body),
                )
            } else {
                Text("${sum.right} из ${sum.total}", style = serif(44f, 500, lh = 1.05f, ls = -0.02f))
                val share = if (sum.total == 0) 0f else sum.right.toFloat() / sum.total
                Text(
                    when {
                        sum.total == 0 -> "Ни одного ответа — попробуйте ещё раз."
                        share >= 0.9f -> "Чисто сработано."
                        share >= 0.6f -> "Хорошо. Ошибки ниже стоит разобрать."
                        else -> "Темы с ошибками вернутся в повторение — так и задумано."
                    },
                    style = sans(16f, lh = 1.45f, color = C.Body),
                )
            }
        }

        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).rules(C.Rule, top = true, bottom = true)) {
            if (blitz) {
                SummaryStat("${sum.right}", "верно", Modifier.weight(1f), first = true)
                SummaryStat("${sum.total - sum.right}", plural(sum.total - sum.right, "ошибка", "ошибки", "ошибок"), Modifier.weight(1f))
            } else {
                SummaryStat("${sum.points}/${sum.max}", "очков", Modifier.weight(1f), first = true)
                SummaryStat(if (sum.total == 0) "—" else "${(sum.right * 100 + sum.total / 2) / sum.total}%", "точность", Modifier.weight(1f))
            }
            SummaryStat("${sum.streak}", plural(sum.streak, "день подряд", "дня подряд", "дней подряд"), Modifier.weight(1f))
        }

        if (sum.mistakes.isNotEmpty()) {
            Column {
                MonoLabel("НАД ЧЕМ ПОРАБОТАТЬ", color = C.Red, modifier = Modifier.padding(bottom = 6.dp))
                sum.mistakes.forEach { m ->
                    val t = vm.bank.topic(m.topic)
                    Column(
                        Modifier.fillMaxWidth().rules(C.Rule, top = true).clickable { vm.openReference(m.topic) }.padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(t?.label ?: m.topic, style = sans(15f, 700, color = C.Red), modifier = Modifier.weight(1f))
                            Ico(Icons.Outlined.Info, 18.dp, C.Mute)
                        }
                        val picked = m.picked
                        when {
                            m.falseAlarm -> Text("Ошибки здесь не было, а вы её искали.", style = sans(14f, color = C.Body))
                            picked != null -> Text("Ваш ответ: «${vm.bank.topic(picked)?.label ?: picked}»", style = sans(14f, color = C.Body))
                        }
                        if (m.text.isNotBlank()) {
                            Text("«${m.text}»", style = serif(15f, italic = true, lh = 1.45f, color = C.Soft), maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        if (sum.strong.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MonoLabel("ПОЛУЧИЛОСЬ", color = C.Blue)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sum.strong.forEach { id -> Chip(vm.bank.topic(id)?.label ?: id, C.Blue, C.BlueTint, height = 30.dp, fontSize = 13f) }
                }
            }
        }

        Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton("Ещё раз", p::again, height = 52.dp, fontSize = 15f, icon = Icons.Outlined.Replay)
            OutlineButton("Готово", p::closeSummary, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String, modifier: Modifier, first: Boolean = false) {
    Column(
        modifier.fillMaxHeight().then(if (first) Modifier else Modifier.rules(C.Rule, left = true))
            .padding(start = if (first) 0.dp else 14.dp, top = 14.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, style = serif(26f, 500))
        Text(label, style = sans(13f, color = C.Mute))
    }
}

// ── Blitz ──────────────────────────────────────────────────────────────────

@Composable
private fun BlitzScreen(vm: ErgoViewModel, b: Blitz) {
    val p = vm.practice
    DisposableEffect(b) {
        p.setBlitzVisible(true)
        onDispose { p.setBlitzVisible(false) }
    }
    var penaltyShown by remember { mutableStateOf(false) }
    LaunchedEffect(b.penalties) {
        if (b.penalties > 0) {
            penaltyShown = true
            delay(900)
            penaltyShown = false
        }
    }
    val urgent = b.timeLeft <= 10_000
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RoundIconButton(Icons.Outlined.Close, p::quitBlitz, 44.dp, 24.dp)
            MonoLabel("БЛИЦ · " + p.label(b.filter).uppercase(), modifier = Modifier.weight(1f))
            val secs = ((b.timeLeft + 999) / 1000).toInt()
            Text("%d:%02d".format(secs / 60, secs % 60), style = mono(18f, 500, color = if (urgent) C.Red else C.Ink))
        }
        val frac by animateFloatAsState(b.timeLeft.toFloat() / Blitz.DURATION_MS, label = "time")
        val barColor by animateColorAsState(if (urgent) C.Red else C.Ink, label = "timeColor")
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(C.Sand)) {
            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(barColor))
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${b.score}", style = serif(44f, 500, lh = 1f))
            Text(plural(b.score, "очко", "очка", "очков"), style = mono(12f), modifier = Modifier.padding(bottom = 6.dp))
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(penaltyShown, enter = fadeIn(), exit = fadeOut()) {
                Text("−3 с", style = sans(16f, 700, color = C.Red), modifier = Modifier.padding(bottom = 6.dp))
            }
            if (b.combo > 1) Chip("×${b.combo}", C.Red, C.RedTint, height = 30.dp, fontSize = 15f, modifier = Modifier.padding(bottom = 4.dp))
        }
        val q = b.q
        if (q != null) {
            SnippetCard(q.drill.source) {
                Text(q.drill.brief, style = serif(20f, lh = 1.5f))
            }
            Text("Что за приём?", style = sans(15f, color = C.Body))
            val fb = b.feedback
            OptionGrid(
                options = q.options,
                label = { vm.bank.topic(it)?.label ?: it },
                state = { o ->
                    when {
                        fb == null -> OptState.Idle
                        o == q.drill.topic -> OptState.Right
                        o == fb -> OptState.Wrong
                        else -> OptState.Dim
                    }
                },
                enabled = fb == null,
                onPick = p::blitzPick,
            )
        }
        Text(
            "Верно ${b.right} из ${b.total} · ошибка стоит 3 секунды, серия верных ответов умножает очки",
            style = mono(11f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}
