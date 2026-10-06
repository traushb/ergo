package app.ergo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ergo.ErgoViewModel
import app.ergo.data.MarkStyle
import app.ergo.data.ThemeMode
import app.ergo.data.fmtCost
import app.ergo.data.money
import app.ergo.data.plural

@Composable
fun ProfileScreen(vm: ErgoViewModel) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("Профиль", style = serif(32f, 500, lh = 1.05f, ls = -0.02f), modifier = Modifier.padding(top = 6.dp))

        val prog = vm.progress
        Column {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).rules(C.Rule, top = true, bottom = true)) {
                Stat("${vm.streak}", plural(vm.streak, "день подряд", "дня подряд", "дней подряд"), Modifier.weight(1f), first = true)
                Stat("${vm.doneCount}", plural(vm.doneCount, "урок пройден", "урока пройдено", "уроков пройдено"), Modifier.weight(1f))
                Stat("${vm.dueCount}", "к повторению", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).rules(C.Rule, bottom = true)) {
                Stat("${prog.solved}", plural(prog.solved, "задание решено", "задания решено", "заданий решено"), Modifier.weight(1f), first = true)
                Stat(prog.accuracy?.let { "$it%" } ?: "—", "точность", Modifier.weight(1f))
                Stat("${prog.blitzBest}", "рекорд блица", Modifier.weight(1f))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MonoLabel("ПРОВАЙДЕР МОДЕЛЕЙ")
            Panel {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("OpenRouter", style = sans(17f, 700), modifier = Modifier.weight(1f))
                    val on = vm.connected
                    Row(
                        Modifier.height(26.dp).clip(RoundedCornerShape(13.dp)).background(if (on) C.BlueTint else C.Sand).padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(if (on) C.Blue else C.Locked))
                        Text(if (on) "Подключён" else "Демо-режим", style = sans(12f, 700, color = if (on) C.Blue else C.Mute), maxLines = 1)
                    }
                }
                providerRows(vm).forEach { (k, v) ->
                    Row(
                        Modifier.fillMaxWidth().rules(C.RuleLight, top = true).padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(k, style = sans(14f, color = C.Mute), modifier = Modifier.weight(1f))
                        Text(v, style = mono(13f, color = C.Ink), textAlign = TextAlign.End, maxLines = 1)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().rules(C.RuleLight, top = true).clickable(onClick = vm::openModels).padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Модель", style = sans(14f, color = C.Mute), modifier = Modifier.weight(1f))
                    Text(vm.modelName, style = sans(14f, 600), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 190.dp))
                    Ico(Icons.Outlined.ChevronRight, 20.dp, C.Mute)
                }
            }
            if (vm.connected) {
                Box(Modifier.height(44.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = vm::disconnect).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                    Text("Отключить ключ", style = sans(15f, 600, color = C.Red))
                }
            } else {
                PillButton("Подключить ключ OpenRouter", vm::goConnect, height = 52.dp, fontSize = 15f)
            }
            Text(
                "Ключ хранится только на этом устройстве. Запросы идут из приложения прямо в OpenRouter, минуя серверы Ergo.",
                style = sans(13f, lh = 1.5f, color = C.Mute),
            )
        }

        // Settings that were design-time Tweaks in the prototype.
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MonoLabel("НАСТРОЙКИ")
            Panel {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Тема", style = sans(14f, color = C.Mute), modifier = Modifier.weight(1f))
                    Segmented(
                        options = listOf(ThemeMode.System to "Авто", ThemeMode.Light to "Светлая", ThemeMode.Dark to "Тёмная"),
                        selected = vm.themeMode,
                        onSelect = vm::updateTheme,
                    )
                }
                Row(
                    Modifier.fillMaxWidth().rules(C.RuleLight, top = true).padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Пометки", style = sans(14f, color = C.Mute), modifier = Modifier.weight(1f))
                    Segmented(
                        options = listOf(MarkStyle.Pencil to "Карандаш", MarkStyle.Highlighter to "Маркер"),
                        selected = vm.markStyle,
                        onSelect = vm::updateMarkStyle,
                    )
                }
                Row(
                    Modifier.fillMaxWidth().rules(C.RuleLight, top = true).clickable { vm.updateShowCost(!vm.showCost) }.padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Показывать стоимость запросов", style = sans(14f, color = C.Mute), modifier = Modifier.weight(1f))
                    Switch(
                        checked = vm.showCost,
                        onCheckedChange = vm::updateShowCost,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = C.Paper,
                            checkedTrackColor = C.Ink,
                            checkedBorderColor = C.Ink,
                            uncheckedThumbColor = C.Locked,
                            uncheckedTrackColor = C.Sand,
                            uncheckedBorderColor = C.Rule,
                        ),
                    )
                }
            }
        }

        Column {
            Box(Modifier.height(40.dp).clickable(onClick = vm::replayOnboarding), contentAlignment = Alignment.CenterStart) {
                Text("Пройти знакомство заново", style = sans(14f).copy(textDecoration = TextDecoration.Underline))
            }
            Box(Modifier.height(40.dp).clickable(onClick = vm::askResetProgress), contentAlignment = Alignment.CenterStart) {
                Text("Сбросить прогресс", style = sans(14f, 600, color = C.Red))
            }
        }
    }
}

private fun providerRows(vm: ErgoViewModel): List<Pair<String, String>> {
    if (!vm.connected) return listOf("Статус" to "Демо-контент")
    val ki = vm.keyInfo
    val key = vm.apiKey
    return buildList {
        add("Ключ" to key.take(9) + "…" + key.takeLast(4))
        ki?.label?.takeIf { it.isNotEmpty() }?.let { add("Метка" to if (it.length > 22) it.take(22) + "…" else it) }
        add("Потрачено по ключу" to money(ki?.usage))
        add("Остаток лимита" to (ki?.limitRemaining?.let(::money) ?: if (ki?.limit == null) "Без лимита" else "—"))
        if (vm.showCost) add("Эта сессия" to "${vm.calls} ${plural(vm.calls, "запрос", "запроса", "запросов")} · ${fmtCost(vm.cost)}")
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier, first: Boolean = false) {
    Column(
        modifier.fillMaxHeight().then(if (first) Modifier else Modifier.rules(C.Rule, left = true))
            .padding(start = if (first) 0.dp else 14.dp, top = 14.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, style = serif(28f, 500))
        Text(label, style = sans(13f, color = C.Mute))
    }
}

@Composable
private fun Panel(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.Card).border(1.dp, C.Rule, RoundedCornerShape(20.dp)),
    ) { content() }
}

@Composable
private fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(
        Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(C.Sand).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { (value, label) ->
            val on = value == selected
            Box(
                Modifier.fillMaxHeight().clip(RoundedCornerShape(13.dp)).background(if (on) C.Ink else C.Sand)
                    .clickable { onSelect(value) }.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label, style = sans(13f, 600, color = if (on) C.Paper else C.Ink), maxLines = 1) }
        }
    }
}
