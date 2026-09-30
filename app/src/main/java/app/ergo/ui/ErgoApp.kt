package app.ergo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.ergo.ErgoViewModel
import app.ergo.Screen
import app.ergo.Tab

private data class NavItem(val tab: Tab, val label: String, val outlined: ImageVector, val filled: ImageVector)

private val NAV = listOf(
    NavItem(Tab.Learn, "Учёба", Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Filled.MenuBook),
    NavItem(Tab.Drill, "Практика", Icons.Outlined.Bolt, Icons.Filled.Bolt),
    NavItem(Tab.Analyze, "Анализ", Icons.Outlined.AccountTree, Icons.Filled.AccountTree),
    NavItem(Tab.Spar, "Спор", Icons.Outlined.Forum, Icons.Filled.Forum),
    NavItem(Tab.You, "Профиль", Icons.Outlined.Person, Icons.Filled.Person),
)

@Composable
fun ErgoApp(vm: ErgoViewModel, onExit: () -> Unit) {
    BackHandler { if (!vm.back()) onExit() }

    Box(Modifier.fillMaxSize().background(C.Paper)) {
        when (vm.screen) {
            Screen.Onboarding -> OnboardingScreen(vm)
            Screen.App -> AppShell(vm)
        }

        AnimatedVisibility(vm.lessonOpen, enter = fadeIn(), exit = fadeOut()) {
            LessonScreen(vm)
        }

        AnimatedVisibility(vm.sheetOpen, enter = fadeIn(), exit = fadeOut()) {
            SheetScrim(vm::closeSheet)
        }
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            val sheetHeight = maxHeight * 0.8f
            AnimatedVisibility(
                vm.sheetOpen,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                ModelSheet(vm, Modifier.height(sheetHeight))
            }
        }

        AnimatedVisibility(
            vm.toastVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding().padding(start = 16.dp, end = 16.dp, bottom = 92.dp),
        ) {
            Toast(vm.toast)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppShell(vm: ErgoViewModel) {
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (vm.tab) {
                Tab.Learn -> LearnScreen(vm)
                Tab.Drill -> DrillScreen(vm)
                Tab.Analyze -> AnalyzeScreen(vm)
                Tab.Spar -> SparScreen(vm)
                Tab.You -> ProfileScreen(vm)
            }
        }
        if (vm.tab == Tab.Spar && vm.sparTopic != null) SparInputBar(vm)
        // The tab bar would ride on top of the keyboard; hide it while typing.
        if (!WindowInsets.isImeVisible) BottomNav(vm)
    }
}

@Composable
private fun BottomNav(vm: ErgoViewModel) {
    Box(Modifier.fillMaxWidth().background(C.Sand).navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 4.dp)) {
            NAV.forEach { item ->
                val on = vm.tab == item.tab
                val pill by animateColorAsState(if (on) C.Ink else C.Sand, label = "navPill")
                Column(
                    Modifier.weight(1f).fillMaxHeight()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { vm.go(item.tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                ) {
                    Box(Modifier.size(60.dp, 32.dp).clip(RoundedCornerShape(16.dp)).background(pill), contentAlignment = Alignment.Center) {
                        Ico(if (on) item.filled else item.outlined, 22.dp, if (on) C.Paper else C.Ink)
                    }
                    Text(item.label, style = sans(12f, if (on) 700 else 500, color = if (on) C.Ink else C.Body), maxLines = 1)
                }
            }
        }
    }
}
