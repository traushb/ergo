package app.ergo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.ergo.ErgoViewModel
import app.ergo.TopicFilter
import app.ergo.data.TopicKind
import app.ergo.data.count

/** What to train: everything, a unit, or one topic, with how well each is learnt. */
@Composable
fun ColumnScope.TopicSheet(vm: ErgoViewModel) {
    val current = vm.practice.filter
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Что тренировать", style = serif(24f, 500))
        Text("Тема задаёт тренировку, блиц и свежие задания от модели.", style = sans(13f, color = C.Mute))
    }
    LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 20.dp)) {
        item(key = "all") {
            FilterRow(current == TopicFilter.All, onClick = { vm.pickFilter(TopicFilter.All) }) {
                Ico(Icons.Outlined.Shuffle, 22.dp, C.Ink, Modifier.width(34.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Все темы", style = sans(16f, 600))
                    Text("Смешанная тренировка по всей программе", style = sans(13f, color = C.Mute))
                }
            }
        }
        vm.bank.units.forEach { u ->
            val topics = vm.bank.topicsIn(u.index)
            item(key = "unit-${u.index}") {
                FilterRow(current == TopicFilter.Section(u.index), onClick = { vm.pickFilter(TopicFilter.Section(u.index)) }, top = 10.dp) {
                    Text(u.n, style = serif(22f, italic = true, color = C.Red), modifier = Modifier.width(34.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(u.title, style = sans(16f, 600))
                        Text("весь раздел · ${count(topics.size, "тема", "темы", "тем")}", style = mono(12f))
                    }
                }
            }
            items(topics, key = { it.id }) { t ->
                FilterRow(current == TopicFilter.One(t.id), onClick = { vm.pickFilter(TopicFilter.One(t.id)) }, indent = 34.dp) {
                    Text(t.name, style = sans(15f), modifier = Modifier.weight(1f))
                    Pips(vm.progress.stat(t.id).box)
                    RoundIconButton(Icons.Outlined.Info, { vm.openReference(t.id) }, 36.dp, 18.dp, tint = C.Mute)
                }
            }
        }
    }
}

@Composable
private fun FilterRow(selected: Boolean, onClick: () -> Unit, indent: Dp = 0.dp, top: Dp = 0.dp, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = top).clip(RoundedCornerShape(14.dp)).background(if (selected) C.Sand else C.Card)
            .clickable(onClick = onClick).padding(start = 12.dp + indent, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        content()
        Ico(Icons.Outlined.Check, 20.dp, C.Ink, Modifier.alpha(if (selected) 1f else 0f))
    }
}

/** A topic's reference card: definition, tell, how to answer, and your record. */
@Composable
fun ReferenceSheet(vm: ErgoViewModel, topicId: String) {
    val t = vm.bank.topic(topicId) ?: return
    val unit = vm.bank.unitOf(t.id)
    val st = vm.progress.stat(t.id)
    val structural = t.kind == TopicKind.Structure || t.kind == TopicKind.Logic
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MonoLabel("СПРАВКА · РАЗДЕЛ ${unit.n} · УРОК ${vm.bank.lessonNumber(t.id)}")
            Text(t.name, style = serif(30f, 500, lh = 1.1f, ls = -0.02f))
            Text(t.formula, style = serif(24f, color = if (structural) C.Blue else C.Red))
        }
        Text(t.definition, style = serif(18f, lh = 1.5f))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.Sand).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MonoLabel(if (structural) "ПОДСКАЗКА" else "ВЫДАЁТ СЕБЯ", ls = 0.1f)
            Text(t.tell, style = serif(17f, italic = true, lh = 1.4f))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MonoLabel("КАК ОТВЕТИТЬ", ls = 0.1f)
            Text(t.counter, style = sans(15f, lh = 1.5f))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pips(st.box)
            Text(masteryText(st, vm.today()), style = mono(12f))
        }
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton("Тренировать", { vm.train(t.id) }, Modifier.weight(1f), height = 50.dp, fontSize = 15f)
            OutlineButton(if (t.id in vm.progress.lessons) "Урок заново" else "Урок", { vm.openLesson(t.id) }, Modifier.weight(1f), height = 50.dp)
        }
    }
}
