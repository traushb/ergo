package app.ergo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.ergo.ErgoViewModel
import app.ergo.data.count

@Composable
fun SparScreen(vm: ErgoViewModel) {
    val sp = vm.spar
    val scroll = rememberScrollState()
    // Keep the newest message in view.
    LaunchedEffect(sp.msgs.size, sp.loading, sp.ended) {
        withFrameNanos {}
        scroll.animateScrollTo(scroll.maxValue)
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        val motion = sp.motion
        if (motion == null) {
            Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Спор", style = serif(32f, 500, lh = 1.05f, ls = -0.02f))
                Text(
                    "Вы защищаете тезис. Ergo спорит с вами и обводит каждую логическую ошибку, которую вы допустите.",
                    style = sans(15f, lh = 1.45f, color = C.Body),
                )
                if (!vm.connected) {
                    Text(
                        "Без ключа OpenRouter соперник отвечает заготовками и ловит ошибки по ключевым фразам.",
                        style = sans(13f, lh = 1.45f, color = C.Mute),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                vm.bank.motions.forEach { m ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.Card)
                            .border(1.dp, C.Rule, RoundedCornerShape(20.dp))
                            .clickable { sp.start(m) }
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MonoLabel("ТЕЗИС")
                        Text(m.motion, style = serif(20f, 500, lh = 1.3f))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Защищать", style = sans(14f, 600, color = C.Red))
                            Ico(Icons.AutoMirrored.Outlined.ArrowForward, 18.dp, C.Red)
                        }
                    }
                }
            }
            return@Column
        }

        Row(
            Modifier.fillMaxWidth().rules(C.Rule, bottom = true).padding(top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RoundIconButton(Icons.AutoMirrored.Outlined.ArrowBack, sp::end, 40.dp, 22.dp, Modifier.offset(x = (-8).dp))
            Column(Modifier.weight(1f).padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                MonoLabel("ВЫ: ЗА · ERGO: ПРОТИВ", ls = 0.1f)
                Text(motion.motion, style = serif(16f, 500, lh = 1.35f))
            }
            val n = sp.flags.size
            Chip(count(n, "ошибка", "ошибки", "ошибок"), C.Red, C.RedTint, Modifier.padding(top = 4.dp), icon = Icons.Outlined.Edit, height = 28.dp, fontSize = 13f)
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val w = maxWidth
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                sp.msgs.forEach { m ->
                    if (!m.fromMe) {
                        Column(Modifier.widthIn(max = w * 0.88f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            MonoLabel("ERGO", ls = 0.1f)
                            val shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
                            Text(
                                m.text,
                                style = serif(16f, lh = 1.55f),
                                modifier = Modifier.clip(shape).background(C.Card).border(1.dp, C.Rule, shape).padding(horizontal = 16.dp, vertical = 14.dp),
                            )
                        }
                    } else {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                m.text,
                                style = sans(15f, lh = 1.5f, color = C.Paper),
                                modifier = Modifier.widthIn(max = w * 0.84f)
                                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomEnd = 18.dp, bottomStart = 18.dp))
                                    .background(C.Ink).padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                            m.flag?.let { f ->
                                val topic = f.topic
                                Column(
                                    Modifier.widthIn(max = w * 0.84f).clip(RoundedCornerShape(14.dp)).background(C.Card)
                                        .border(1.5.dp, C.Red, RoundedCornerShape(14.dp))
                                        .then(if (topic != null) Modifier.clickable { vm.openReference(topic) } else Modifier)
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Ico(Icons.Outlined.Edit, 16.dp, C.Red)
                                        Text(f.name, style = sans(14f, 700, color = C.Red), modifier = Modifier.weight(1f, fill = false))
                                        if (topic != null) Ico(Icons.Outlined.Info, 16.dp, C.Mute)
                                    }
                                    Text("«${f.quote}»", style = serif(15f, italic = true, color = C.Soft))
                                    Text(f.note, style = sans(14f, lh = 1.45f, color = C.Soft))
                                }
                            }
                        }
                    }
                }
                if (sp.loading) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Dots(C.Mute)
                        Text("Ergo готовит возражение…", style = sans(14f, color = C.Mute))
                    }
                }
                if (sp.ended) SparRoundUp(vm)
            }
        }
    }
}

/** The end-of-debate card: how many turns, which fallacies. */
@Composable
private fun SparRoundUp(vm: ErgoViewModel) {
    val sp = vm.spar
    val turns = sp.myTurns
    val flags = sp.flags
    val clean = turns - flags.size
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.Hero).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Ico(Icons.Outlined.Flag, 18.dp, C.HeroMute)
            MonoLabel("ИТОГ СПОРА", color = C.HeroMute)
        }
        Text(
            if (flags.isEmpty()) "Ни одной ошибки за ${count(turns, "реплику", "реплики", "реплик")}."
            else "Чистых реплик: $clean из $turns.",
            style = serif(24f, 500, lh = 1.2f, color = C.OnHero),
        )
        if (flags.isNotEmpty()) {
            Text(
                "Замечено: " + flags.map { it.name }.distinct().joinToString(", ") + ".",
                style = sans(15f, lh = 1.5f, color = C.HeroBody),
            )
        }
        Text(
            if (flags.isEmpty()) "Хорошая работа. Попробуйте тезис посложнее." else "Нажмите на пометку под репликой, чтобы открыть справку.",
            style = sans(14f, lh = 1.45f, color = C.HeroMute),
        )
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlineButton("Продолжить", sp::resume, Modifier.weight(1f), height = 46.dp, color = C.OnHero)
            PillButton("Новый тезис", sp::leave, Modifier.weight(1f), height = 46.dp, fontSize = 15f, bg = C.OnHero, fg = C.Hero)
        }
    }
}

/** Composer bar shown above the tab bar while a debate is running. */
@Composable
fun SparInputBar(vm: ErgoViewModel) {
    val sp = vm.spar
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Row(
        Modifier.fillMaxWidth().background(C.Paper).rules(C.Rule, top = true).padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val style = sans(15f)
        BasicTextField(
            value = sp.input,
            onValueChange = sp::onInput,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(C.Ink),
            interactionSource = interaction,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { sp.send() }),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(
                    Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(23.dp)).background(C.Card)
                        .border(1.dp, if (focused) C.Ink else C.Rule, RoundedCornerShape(23.dp))
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (sp.input.isEmpty()) Text("Ваш аргумент…", style = style.copy(color = C.Placeholder))
                    inner()
                }
            },
        )
        Box(
            Modifier.size(46.dp).clip(CircleShape).background(C.Ink).clickable(onClick = sp::send),
            contentAlignment = Alignment.Center,
        ) { Ico(Icons.Outlined.ArrowUpward, 20.dp, C.Paper) }
    }
}
