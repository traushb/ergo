package app.ergo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ergo.ErgoViewModel
import app.ergo.data.AUTO_MODEL
import app.ergo.data.per1M
import app.ergo.data.plural

/** Scrim for the model sheet; tapping it closes the sheet. */
@Composable
fun SheetScrim(onClose: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(C.Scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
    )
}

@Composable
fun ModelSheet(vm: ErgoViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(C.Card)
            .blockTouches()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(C.Handle))
        }
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Выбор модели", style = serif(24f, 500), modifier = Modifier.alignByBaseline())
                val n = vm.models.size
                if (n > 0) Text("$n ${plural(n, "модель", "модели", "моделей")}", style = mono(12f), modifier = Modifier.alignByBaseline())
            }
            Row(
                Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(C.Sand).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Ico(Icons.Outlined.Search, 20.dp, C.Mute)
                val style = sans(15f)
                Box(Modifier.weight(1f)) {
                    if (vm.modelQuery.isEmpty()) Text("Поиск по моделям и провайдерам", style = style.copy(color = C.Placeholder), maxLines = 1)
                    BasicTextField(
                        value = vm.modelQuery,
                        onValueChange = vm::onModelQuery,
                        singleLine = true,
                        textStyle = style,
                        cursorBrush = SolidColor(C.Ink),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Text("Цены за 1 млн токенов, ввод / вывод. Данные OpenRouter в реальном времени.", style = sans(13f, color = C.Mute))
        }
        val q = vm.modelQuery.trim().lowercase()
        val rows = vm.models.filter { q.isEmpty() || it.id.lowercase().contains(q) || it.name.lowercase().contains(q) }.take(80)
        LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 20.dp)) {
            if (vm.modelsLoading) {
                item {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Dots(C.Mute)
                        Text("Загружаем список моделей…", style = sans(14f, color = C.Mute))
                    }
                }
            }
            items(rows, key = { it.id }) { m ->
                val sel = m.id == vm.model
                val auto = m.id == AUTO_MODEL
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (sel) C.Sand else C.Card)
                        .clickable { vm.selectModel(m) }.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(if (auto) "Автовыбор (рекомендуем)" else m.name, style = sans(15f, 600), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(m.id, style = mono(11f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val detail = if (auto) "Подбирает модель под каждый запрос · цена меняется"
                        else per1M(m.promptPrice) + " / " + per1M(m.completionPrice) + (m.ctx?.let { " · контекст " + Math.round(it / 1000.0) + "K" } ?: "")
                        Text(detail, style = sans(13f, color = C.Body))
                    }
                    Ico(Icons.Outlined.Check, 20.dp, C.Ink, Modifier.alpha(if (sel) 1f else 0f))
                }
            }
        }
    }
}
