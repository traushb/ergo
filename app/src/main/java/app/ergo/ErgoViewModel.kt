package app.ergo

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.ergo.data.AUTO_MODEL
import app.ergo.data.Bank
import app.ergo.data.FALLBACK_MODELS
import app.ergo.data.KeyInfo
import app.ergo.data.MarkStyle
import app.ergo.data.ModelInfo
import app.ergo.data.OpenRouter
import app.ergo.data.Progress
import app.ergo.data.Store
import app.ergo.data.ThemeMode
import app.ergo.data.Topic
import app.ergo.data.loadBank
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class Screen { Onboarding, App }
enum class Tab { Learn, Drill, Analyze, Spar, You }
enum class KeyStatus { Idle, Checking, Ok, Error }

sealed interface Sheet {
    data object Model : Sheet
    data object Topics : Sheet
    data class Reference(val topic: String) : Sheet
}

data class Confirm(val title: String, val text: String, val action: String, val run: () -> Unit)

class ErgoViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    val bank: Bank = loadBank(app)

    var progress by mutableStateOf(store.progress); private set

    // Navigation
    var screen by mutableStateOf(if (store.onboarded) Screen.App else Screen.Onboarding); private set
    var obStep by mutableIntStateOf(0); private set
    var goal by mutableStateOf(store.goal); private set
    var tab by mutableStateOf(Tab.Learn); private set
    var openUnit by mutableIntStateOf(0); private set
    var sheet by mutableStateOf<Sheet?>(null); private set
    var confirm by mutableStateOf<Confirm?>(null); private set
    var lesson by mutableStateOf<LessonRun?>(null); private set

    // OpenRouter
    var apiKey by mutableStateOf(store.apiKey); private set
    var keyInput by mutableStateOf(apiKey); private set
    var keyStatus by mutableStateOf(KeyStatus.Idle); private set
    var keyInfo by mutableStateOf<KeyInfo?>(null); private set
    var keyError by mutableStateOf(""); private set
    var model by mutableStateOf(store.model); private set
    var models by mutableStateOf<List<ModelInfo>>(emptyList()); private set
    var modelsLoading by mutableStateOf(false); private set
    var modelQuery by mutableStateOf(""); private set
    var calls by mutableIntStateOf(0); private set
    var cost by mutableStateOf(0.0); private set

    // Settings
    var markStyle by mutableStateOf(store.markStyle); private set
    var showCost by mutableStateOf(store.showCost); private set
    var themeMode by mutableStateOf(store.themeMode); private set

    /** Last toast text; kept after [toastVisible] turns off so the fade-out still shows it. */
    var toast by mutableStateOf(""); private set
    var toastVisible by mutableStateOf(false); private set
    private var toastJob: Job? = null

    private val ai = Ai(
        key = { apiKey },
        model = { model },
        showCost = { showCost },
        onUsage = { c ->
            calls += 1
            cost += c ?: 0.0
        },
    )
    val practice = PracticeController(bank, ai, viewModelScope, { progress }, ::updateProgress, ::showToast, ::today)
    val spar = SparController(bank, ai, viewModelScope, ::showToast)
    val analyze = AnalyzeController(bank, ai, viewModelScope, ::showToast)

    val connected get() = apiKey.isNotEmpty()

    init {
        openUnit = nextLesson?.let { bank.unitIndexOf(it.id) } ?: 0
        if (apiKey.isNotEmpty()) viewModelScope.launch { runCatching { keyInfo = OpenRouter.keyInfo(apiKey) } }
    }

    fun today(): Long = LocalDate.now().toEpochDay()

    private fun updateProgress(f: (Progress) -> Progress) {
        progress = f(progress)
        store.progress = progress
    }

    fun showToast(msg: String) {
        toastJob?.cancel()
        toast = msg
        toastVisible = true
        toastJob = viewModelScope.launch {
            delay(3200)
            toastVisible = false
        }
    }

    fun go(t: Tab) {
        tab = t
        screen = Screen.App
    }

    // ── Learning state ─────────────────────────────────────────────────────

    val doneCount: Int get() = bank.lessonOrder.count { it.id in progress.lessons }
    val nextLesson: Topic? get() = bank.lessonOrder.firstOrNull { it.id !in progress.lessons }
    val dueCount: Int get() = progress.dueOn(today()).size
    val streak: Int get() = progress.streakOn(today())

    // ── Onboarding ─────────────────────────────────────────────────────────

    fun obBack() {
        obStep = maxOf(0, obStep - 1)
    }

    fun obNext() {
        if (obStep == 1 && goal == null) return
        obStep += 1
    }

    fun pickGoal(id: String) {
        goal = id
        store.goal = id
    }

    fun onKeyInput(v: String) {
        keyInput = v
        if (keyStatus == KeyStatus.Ok) keyStatus = KeyStatus.Idle
    }

    fun verifyKey() {
        val k = keyInput.trim()
        if (!k.startsWith("sk-or-")) {
            keyStatus = KeyStatus.Error
            keyError = "Ключи OpenRouter начинаются с «sk-or-». Проверьте, что скопировали ключ целиком."
            return
        }
        keyStatus = KeyStatus.Checking
        keyError = ""
        viewModelScope.launch {
            try {
                keyInfo = OpenRouter.keyInfo(k)
                store.apiKey = k
                apiKey = k
                keyStatus = KeyStatus.Ok
            } catch (e: Exception) {
                keyStatus = KeyStatus.Error
                keyError = "OpenRouter отклонил ключ: " + (e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun finishOnboarding() {
        if (keyStatus != KeyStatus.Ok) return showToast("Сначала проверьте ключ или перейдите в демо-режим.")
        store.onboarded = true
        go(Tab.Learn)
    }

    fun skipDemo() {
        store.onboarded = true
        go(Tab.Learn)
        showToast("Офлайн-режим: встроенные упражнения и уроки. Ключ можно добавить в профиле.")
    }

    fun goConnect() {
        screen = Screen.Onboarding
        obStep = 2
    }

    fun replayOnboarding() {
        screen = Screen.Onboarding
        obStep = 0
    }

    fun disconnect() {
        store.apiKey = ""
        apiKey = ""
        keyInput = ""
        keyStatus = KeyStatus.Idle
        keyInfo = null
        showToast("Ключ удалён с этого устройства.")
    }

    // ── Sheets and dialogs ─────────────────────────────────────────────────

    fun openModels() {
        sheet = Sheet.Model
        loadModels()
    }

    fun openTopics() {
        sheet = Sheet.Topics
    }

    fun openReference(topicId: String) {
        sheet = Sheet.Reference(topicId)
    }

    fun closeSheet() {
        sheet = null
    }

    fun onModelQuery(v: String) {
        modelQuery = v
    }

    private fun loadModels() {
        if (models.isNotEmpty() || modelsLoading) return
        modelsLoading = true
        viewModelScope.launch {
            try {
                models = listOf(FALLBACK_MODELS[0]) + OpenRouter.models().filter { it.id != AUTO_MODEL }
            } catch (e: Exception) {
                models = FALLBACK_MODELS
                showToast("Не удалось связаться с OpenRouter, показан офлайн-список.")
            }
            modelsLoading = false
        }
    }

    fun selectModel(m: ModelInfo) {
        store.model = m.id
        model = m.id
        sheet = null
        showToast("Выбрана модель: " + (if (m.id == AUTO_MODEL) "Автовыбор" else m.name) + ".")
    }

    val modelName: String
        get() = if (model == AUTO_MODEL) "Автовыбор"
        else (models.find { it.id == model } ?: FALLBACK_MODELS.find { it.id == model })?.name ?: model

    fun askResetProgress() {
        confirm = Confirm(
            title = "Сбросить прогресс?",
            text = "Пройденные уроки, серия, статистика и колода повторения обнулятся. Ключ и настройки останутся.",
            action = "Сбросить",
        ) {
            updateProgress { Progress() }
            showToast("Прогресс сброшен.")
        }
    }

    fun confirmYes() {
        val c = confirm ?: return
        confirm = null
        c.run()
    }

    fun confirmNo() {
        confirm = null
    }

    // ── Settings ───────────────────────────────────────────────────────────

    fun updateMarkStyle(v: MarkStyle) {
        markStyle = v
        store.markStyle = v
    }

    fun updateShowCost(v: Boolean) {
        showCost = v
        store.showCost = v
    }

    fun updateTheme(v: ThemeMode) {
        themeMode = v
        store.themeMode = v
    }

    // ── Learn and lessons ──────────────────────────────────────────────────

    fun toggleUnit(i: Int) {
        openUnit = if (openUnit == i) -1 else i
    }

    fun openLesson(topicId: String) {
        val t = bank.topic(topicId) ?: return
        sheet = null
        lesson = buildLesson(bank, t)
    }

    fun continueLearning() {
        val next = nextLesson
        if (next != null) openLesson(next.id) else go(Tab.Drill)
    }

    fun closeLesson() {
        lesson = null
    }

    fun lessonAnswer(i: Int) {
        lesson?.answer = i
    }

    fun lessonNext() {
        val l = lesson ?: return
        when (l.step) {
            2 -> if (!l.marked) {
                l.marked = true
                return
            }
            3 -> {
                if (!l.passed) return
                updateProgress { it.lessonDone(l.topic.id, today()) }
            }
            4 -> {
                lesson = null
                openUnit = nextLesson?.let { bank.unitIndexOf(it.id) } ?: openUnit
                return
            }
        }
        l.step += 1
    }

    /** From the lesson's last step straight into practice on that topic. */
    fun lessonToPractice() {
        val l = lesson ?: return
        lesson = null
        train(l.topic.id)
    }

    fun train(topicId: String) {
        sheet = null
        practice.startPractice(TopicFilter.One(topicId))
        go(Tab.Drill)
    }

    fun startReview() {
        practice.startReview()
        if (practice.session != null) go(Tab.Drill)
    }

    fun pickFilter(f: TopicFilter) {
        practice.select(f)
        sheet = null
    }

    // ── Back navigation ────────────────────────────────────────────────────

    /** Returns true when the back press was consumed. */
    fun back(): Boolean = when {
        confirm != null -> {
            confirm = null
            true
        }
        sheet != null -> {
            sheet = null
            true
        }
        lesson != null -> {
            lesson = null
            true
        }
        screen == Screen.Onboarding && obStep > 0 -> {
            obBack()
            true
        }
        screen == Screen.Onboarding && store.onboarded -> {
            screen = Screen.App
            true
        }
        screen == Screen.App && tab == Tab.Drill && practice.summary != null -> {
            practice.closeSummary()
            true
        }
        screen == Screen.App && tab == Tab.Drill && practice.blitz != null -> {
            practice.quitBlitz()
            true
        }
        screen == Screen.App && tab == Tab.Drill && practice.session != null -> {
            practice.quit()
            true
        }
        screen == Screen.App && tab == Tab.Spar && spar.ended -> {
            spar.leave()
            true
        }
        screen == Screen.App && tab == Tab.Spar && spar.motion != null -> {
            spar.end()
            true
        }
        screen == Screen.App && tab != Tab.Learn -> {
            go(Tab.Learn)
            true
        }
        else -> false
    }
}
