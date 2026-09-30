package app.ergo.ui

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.ergo.ErgoViewModel
import app.ergo.KeyStatus
import app.ergo.data.GOALS
import app.ergo.data.money

private val goalIcons = mapOf(
    "course" to Icons.Outlined.School,
    "online" to Icons.Outlined.Forum,
    "news" to Icons.Outlined.Newspaper,
    "curious" to Icons.Outlined.Explore,
)

@Composable
fun OnboardingScreen(vm: ErgoViewModel) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Steps fill the viewport so their buttons sit at the bottom, but can still scroll on small screens.
        val bodyMin = maxHeight - 12.dp - 28.dp - 48.dp
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 28.dp, end = 28.dp, top = 12.dp, bottom = 28.dp)
        ) {
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (vm.obStep > 0) {
                    RoundIconButton(Icons.AutoMirrored.Outlined.ArrowBack, vm::obBack, 40.dp, 24.dp, Modifier.offset(x = (-10).dp))
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { i ->
                        val w by animateDpAsState(if (i == vm.obStep) 22.dp else 8.dp, label = "obBar")
                        Box(Modifier.size(w, 8.dp).clip(RoundedCornerShape(4.dp)).background(if (i <= vm.obStep) C.Ink else C.BarOff))
                    }
                }
            }
            val body = Modifier.fillMaxWidth().heightIn(min = bodyMin)
            when (vm.obStep) {
                0 -> Welcome(vm, body)
                1 -> Goals(vm, body)
                else -> ConnectKey(vm, body)
            }
        }
    }
}

@Composable
private fun Welcome(vm: ErgoViewModel, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.Bottom)) {
        ErgoMark(64.dp, 56.dp, 18.dp)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MonoLabel("ERGO · НЕФОРМАЛЬНАЯ ЛОГИКА", size = 12f)
            val lead = "Научитесь отличать хороший аргумент от "
            val marked = "убедительно звучащего"
            val h1 = buildAnnotatedString {
                append(lead)
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(marked) }
                append(".")
            }
            PencilText(h1, serif(36f, 500, lh = 1.12f, ls = -0.02f), listOf(lead.length until lead.length + marked.length), underlineOffset = 7.dp)
            Text(
                "Шесть коротких разделов о том, как устроены рассуждения и где они ломаются. Упражнения, карты аргументов и оппонент для спора на модели, которую выберете вы.",
                style = sans(16f, lh = 1.5f, color = C.Body),
            )
        }
        PillButton("Начать", vm::obNext)
    }
}

@Composable
private fun Goals(vm: ErgoViewModel, modifier: Modifier) {
    Column(modifier.padding(top = 24.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Что вас сюда привело?", style = serif(32f, 500, lh = 1.12f, ls = -0.02f))
                Text("От этого зависят примеры, а не программа.", style = sans(15f, lh = 1.5f, color = C.Body))
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GOALS.forEach { g ->
                    val on = vm.goal == g.id
                    Tappable(
                        onClick = { vm.pickGoal(g.id) },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                        bg = if (on) C.Card else C.Paper,
                        border = BorderStroke(1.5.dp, if (on) C.Ink else C.Rule),
                        padding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(g.title, style = sans(16f, 600))
                            Text(g.sub, style = sans(14f, color = C.Mute))
                        }
                        Ico(goalIcons.getValue(g.id), 24.dp, if (on) C.Red else C.Mute)
                    }
                }
            }
        }
        PillButton("Продолжить", vm::obNext, Modifier.padding(top = 24.dp), enabled = vm.goal != null)
    }
}

@Composable
private fun ConnectKey(vm: ErgoViewModel, modifier: Modifier) {
    Column(modifier.padding(top = 24.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Подключите свою модель.", style = serif(32f, 500, lh = 1.12f, ls = -0.02f))
                Text(
                    "Новые упражнения, объяснения и карты аргументов работают через OpenRouter на выбранной вами модели. Ключ хранится только на этом устройстве, а OpenRouter выставляет счёт напрямую. Одно упражнение обычно стоит доли цента.",
                    style = sans(15f, lh = 1.5f, color = C.Body),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MonoLabel("API-КЛЮЧ OPENROUTER", ls = 0.1f)
                val borderColor = when (vm.keyStatus) {
                    KeyStatus.Error -> C.Red
                    KeyStatus.Ok -> C.Blue
                    else -> C.Rule
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(C.Card)
                        .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
                        .padding(start = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Ico(Icons.Outlined.Key, 20.dp, C.Mute)
                    Box(Modifier.weight(1f)) {
                        if (vm.keyInput.isEmpty()) Text("sk-or-v1-…", style = mono(14f, color = C.Placeholder))
                        BasicTextField(
                            value = vm.keyInput,
                            onValueChange = vm::onKeyInput,
                            singleLine = true,
                            textStyle = mono(14f, color = C.Ink),
                            cursorBrush = SolidColor(C.Ink),
                            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { vm.verifyKey() }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    val label = when (vm.keyStatus) {
                        KeyStatus.Checking -> "Проверяем…"
                        KeyStatus.Ok -> "Проверен"
                        else -> "Проверить"
                    }
                    Box(
                        Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(C.Ink).clickable { vm.verifyKey() }.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(label, style = sans(13f, 600, color = C.Paper), maxLines = 1) }
                }
                if (vm.keyStatus == KeyStatus.Ok) {
                    val ki = vm.keyInfo
                    val text = "Ключ работает" +
                        (ki?.label?.let { " · «$it»" } ?: "") +
                        (ki?.limitRemaining?.let { " · осталось ${money(it)}" } ?: "")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Ico(Icons.Outlined.CheckCircle, 18.dp, C.Blue)
                        Text(text, style = sans(14f, color = C.Blue))
                    }
                }
                if (vm.keyStatus == KeyStatus.Error) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Ico(Icons.Outlined.ErrorOutline, 18.dp, C.Red)
                        Text(vm.keyError, style = sans(14f, lh = 1.4f, color = C.Red))
                    }
                }
                val link = buildAnnotatedString {
                    append("Ещё нет ключа? ")
                    withLink(
                        LinkAnnotation.Url(
                            "https://openrouter.ai/keys",
                            TextLinkStyles(SpanStyle(color = C.Red, textDecoration = TextDecoration.Underline)),
                        )
                    ) { append("Создайте на openrouter.ai/keys") }
                }
                Text(link, style = sans(14f, color = C.Mute))
            }
            Tappable(
                onClick = vm::openSheet,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                bg = C.Card,
                border = BorderStroke(1.dp, C.Rule),
                padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    MonoLabel("МОДЕЛЬ", ls = 0.1f)
                    Text(vm.modelName, style = sans(16f, 600))
                    Text(vm.model, style = mono(12f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Ico(Icons.Outlined.UnfoldMore, 22.dp, C.Mute)
            }
        }
        Column(Modifier.padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PillButton("Начать обучение", vm::finishOnboarding, enabled = vm.keyStatus == KeyStatus.Ok)
            Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).clickable(onClick = vm::skipDemo), contentAlignment = Alignment.Center) {
                Text("Пропустить и открыть демо-режим", style = sans(15f, 500).copy(textDecoration = TextDecoration.Underline))
            }
        }
    }
}
