package app.ergo

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.ergo.data.AUTO_MODEL
import app.ergo.data.Analysis
import app.ergo.data.CHECK
import app.ergo.data.CUR_LESSON
import app.ergo.data.ChatMessage
import app.ergo.data.ChatResult
import app.ergo.data.DRILLS
import app.ergo.data.Drill
import app.ergo.data.FALLACIES
import app.ergo.data.FALLBACK_MODELS
import app.ergo.data.Flag
import app.ergo.data.Issue
import app.ergo.data.KeyInfo
import app.ergo.data.MarkStyle
import app.ergo.data.ModelInfo
import app.ergo.data.OpenRouter
import app.ergo.data.SAMPLES
import app.ergo.data.SPAR_DEMO
import app.ergo.data.SYS_AN
import app.ergo.data.SYS_DRILL
import app.ergo.data.SYS_EXPLAIN
import app.ergo.data.Store
import app.ergo.data.Topic
import app.ergo.data.demoFlag
import app.ergo.data.fmtCost
import app.ergo.data.parseModelJson
import app.ergo.data.str
import app.ergo.data.stringList
import app.ergo.data.sysSpar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject

enum class Screen { Onboarding, App }
enum class Tab { Learn, Drill, Analyze, Spar, You }
enum class KeyStatus { Idle, Checking, Ok, Error }
enum class DrillPhase { Spot, Name, Result }

data class SparMsg(val fromMe: Boolean, val text: String, val flag: Flag? = null)

class ErgoViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)

    // Navigation
    var screen by mutableStateOf(if (store.onboarded) Screen.App else Screen.Onboarding); private set
    var obStep by mutableIntStateOf(0); private set
    var goal by mutableStateOf(store.goal); private set
    var tab by mutableStateOf(Tab.Learn); private set

    // Learn / lesson
    var openUnit by mutableIntStateOf(1); private set
    var strawDone by mutableStateOf(store.strawDone); private set
    var lessonOpen by mutableStateOf(false); private set
    var lessonStep by mutableIntStateOf(0); private set
    var lessonMarked by mutableStateOf(false); private set
    var lessonAns by mutableStateOf<Int?>(null); private set

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
    var sheetOpen by mutableStateOf(false); private set
    var calls by mutableIntStateOf(0); private set
    var cost by mutableStateOf(0.0); private set

    // Settings (were Tweaks in the prototype)
    var markStyle by mutableStateOf(store.markStyle); private set
    var showCost by mutableStateOf(store.showCost); private set

    // Drill
    var drillIdx by mutableIntStateOf(0); private set
    var drill by mutableStateOf(DRILLS[0]); private set
    var drillPhase by mutableStateOf(DrillPhase.Spot); private set
    var drillSpot by mutableStateOf<Int?>(null); private set
    var drillMiss by mutableStateOf<List<Int>>(emptyList()); private set
    var drillPick by mutableStateOf<String?>(null); private set
    var drillExplain by mutableStateOf(""); private set
    var drillExplainMeta by mutableStateOf(""); private set
    var drillExplainLoading by mutableStateOf(false); private set
    var genLoading by mutableStateOf(false); private set
    var scoreRight by mutableIntStateOf(0); private set
    var scoreTotal by mutableIntStateOf(0); private set
    /** Bumped whenever a new exercise is shown, so the screen can scroll back to the top. */
    var drillSerial by mutableIntStateOf(0); private set

    // Analyze
    var anText by mutableStateOf(""); private set
    var anSource by mutableStateOf(""); private set
    var anResult by mutableStateOf<Analysis?>(null); private set
    var anLoading by mutableStateOf(false); private set
    var anMeta by mutableStateOf(""); private set

    // Spar
    var sparTopic by mutableStateOf<Topic?>(null); private set
    var sparMsgs by mutableStateOf<List<SparMsg>>(emptyList()); private set
    var sparInput by mutableStateOf(""); private set
    var sparLoading by mutableStateOf(false); private set
    private var sparI = 0

    /** Last toast text; kept after [toastVisible] turns off so the fade-out still shows it. */
    var toast by mutableStateOf(""); private set
    var toastVisible by mutableStateOf(false); private set
    private var toastJob: Job? = null

    val connected get() = apiKey.isNotEmpty()
    val doneCount get() = 5 + if (strawDone) 1 else 0

    init {
        if (apiKey.isNotEmpty()) viewModelScope.launch { runCatching { keyInfo = OpenRouter.keyInfo(apiKey) } }
    }

    fun showToast(msg: String) {
        toastJob?.cancel()
        toast = msg
        toastVisible = true
        toastJob = viewModelScope.launch { delay(3200); toastVisible = false }
    }

    fun go(t: Tab) {
        tab = t
        screen = Screen.App
    }

    // ── Onboarding ─────────────────────────────────────────────────────────

    fun obBack() { obStep = maxOf(0, obStep - 1) }
    fun obNext() {
        if (obStep == 1 && goal == null) return
        obStep += 1
    }
    fun pickGoal(id: String) { goal = id; store.goal = id }
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
        showToast("Демо-режим: только встроенный контент. Ключ можно добавить в профиле.")
    }

    fun goConnect() { screen = Screen.Onboarding; obStep = 2 }
    fun replayOnboarding() { screen = Screen.Onboarding; obStep = 0 }

    fun disconnect() {
        store.apiKey = ""
        apiKey = ""
        keyInput = ""
        keyStatus = KeyStatus.Idle
        keyInfo = null
        showToast("Ключ удалён с этого устройства.")
    }

    // ── Models ─────────────────────────────────────────────────────────────

    fun openSheet() {
        sheetOpen = true
        loadModels()
    }
    fun closeSheet() { sheetOpen = false }
    fun onModelQuery(v: String) { modelQuery = v }

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
        sheetOpen = false
        showToast("Выбрана модель: " + (if (m.id == AUTO_MODEL) "Автовыбор" else m.name) + ".")
    }

    val modelName: String
        get() = if (model == AUTO_MODEL) "Автовыбор"
        else (models.find { it.id == model } ?: FALLBACK_MODELS.find { it.id == model })?.name ?: model

    /** One OpenRouter call, with session cost accounting. Throws [EmptyReply] when the model says nothing. */
    private suspend fun complete(messages: List<ChatMessage>, max: Int, json: Boolean): Pair<ChatResult, String> {
        val r = OpenRouter.chat(apiKey, model, messages, max, json)
        calls += 1
        cost += r.cost ?: 0.0
        if (r.text.isBlank()) {
            throw EmptyReply(
                if (r.finishReason == "length") "модель потратила весь лимит токенов на рассуждения и ничего не ответила. Попробуйте другую модель."
                else "модель вернула пустой ответ."
            )
        }
        val meta = (r.model ?: model) + if (showCost && r.cost != null) " · " + fmtCost(r.cost) else ""
        return r to meta
    }

    private suspend fun chat(messages: List<ChatMessage>, max: Int): Pair<String, String> =
        complete(messages, max, json = false).let { (r, meta) -> r.text to meta }

    /** For structured tasks: parses the reply, retrying once on an empty, broken or truncated JSON reply. */
    private suspend fun chatJson(messages: List<ChatMessage>, max: Int): Pair<JSONObject, String> {
        var last: Exception? = null
        repeat(2) {
            try {
                val (r, meta) = complete(messages, max, json = true)
                try {
                    return parseModelJson(r.text) to meta
                } catch (e: JSONException) {
                    last = if (r.finishReason == "length") EmptyReply("ответ модели обрезан по лимиту токенов.") else EmptyReply("модель вернула некорректный JSON.")
                }
            } catch (e: EmptyReply) {
                last = e
            }
        }
        throw last!!
    }

    private class EmptyReply(message: String) : Exception(message)

    // ── Settings ───────────────────────────────────────────────────────────

    fun updateMarkStyle(v: MarkStyle) { markStyle = v; store.markStyle = v }
    fun updateShowCost(v: Boolean) { showCost = v; store.showCost = v }

    // ── Learn / lesson ─────────────────────────────────────────────────────

    fun toggleUnit(i: Int) { openUnit = if (openUnit == i) -1 else i }

    fun lessonClicked(name: String, state: LessonState) {
        when {
            name == CUR_LESSON && state != LessonState.Locked -> openLesson()
            state == LessonState.Current -> showToast("Урока «$name» пока нет в прототипе. Пример урока — «Соломенное чучело».")
            state == LessonState.Done -> showToast("Пройденные уроки возвращаются через колоду повторения.")
            else -> showToast("Закрыто. Сначала пройдите текущий урок.")
        }
    }

    fun continueLesson() {
        if (strawDone) showToast("Урока «Ложный след» пока нет в прототипе. Загляните в «Практику».") else openLesson()
    }

    fun openLesson() {
        lessonOpen = true
        lessonStep = 0
        lessonMarked = false
        lessonAns = null
    }
    fun closeLesson() { lessonOpen = false }
    fun answerCheck(i: Int) { lessonAns = i }
    val checkPassed get() = lessonAns?.let { CHECK[it].ok } == true

    fun lessonNext() {
        if (lessonStep == 2 && !lessonMarked) { lessonMarked = true; return }
        if (lessonStep == 3 && !checkPassed) return
        if (lessonStep == 4) {
            lessonOpen = false
            markStrawDone()
            lessonStep = 0
            return
        }
        lessonStep += 1
    }

    fun lessonToDrill() {
        lessonOpen = false
        markStrawDone()
        lessonStep = 0
        resetDrill(DRILLS[0], 0)
        go(Tab.Drill)
    }

    private fun markStrawDone() { strawDone = true; store.strawDone = true }

    // ── Drill ──────────────────────────────────────────────────────────────

    private fun resetDrill(d: Drill, i: Int? = null) {
        drill = d
        if (i != null) drillIdx = i
        drillPhase = DrillPhase.Spot
        drillSpot = null
        drillMiss = emptyList()
        drillPick = null
        drillExplain = ""
        drillExplainMeta = ""
        drillExplainLoading = false
        drillSerial += 1
    }

    fun spot(i: Int) {
        if (drillPhase != DrillPhase.Spot) return
        if (i in drill.flawed) {
            drillSpot = i
            drillPhase = DrillPhase.Name
        } else if (i !in drillMiss) {
            drillMiss = drillMiss + i
        }
    }

    fun pick(name: String) {
        if (drillPhase != DrillPhase.Name) return
        drillPick = name
        drillPhase = DrillPhase.Result
        if (name == drill.answer) scoreRight += 1
        scoreTotal += 1
    }

    fun nextDrill() {
        val i = (drillIdx + 1) % DRILLS.size
        resetDrill(DRILLS[i], i)
    }

    fun genDrill() {
        if (apiKey.isEmpty()) return showToast("Для новых упражнений нужен ключ OpenRouter. Добавьте его в профиле.")
        if (genLoading) return
        genLoading = true
        viewModelScope.launch {
            try {
                val (o, meta) = chatJson(
                    listOf(
                        ChatMessage("system", SYS_DRILL),
                        ChatMessage("user", "Новое упражнение. Выбери одну ошибку случайно из списка: $FALLACIES. Не используй: ${drill.answer}."),
                    ),
                    4000,
                )
                val answer = o.str("answer")
                val flawed = o.optJSONArray("flawed")?.let { a -> (0 until a.length()).map { a.optInt(it) } }
                    ?: listOf(o.optInt("flawed"))
                var opts = o.stringList("options").take(4)
                if (answer !in opts) opts = (listOf(answer) + opts).take(4)
                val sentences = o.stringList("sentences")
                if (sentences.isEmpty()) throw IllegalStateException("модель вернула неожиданный формат")
                val whyNot = o.optJSONObject("whyNot")?.let { w -> w.keys().asSequence().associateWith { w.str(it) } }.orEmpty()
                resetDrill(
                    Drill(
                        source = o.str("source").ifEmpty { "Сгенерированный фрагмент" },
                        sentences = sentences,
                        flawed = flawed,
                        answer = answer,
                        options = opts.shuffled(),
                        explanation = o.str("explanation"),
                        whyNot = whyNot,
                        ai = true,
                        meta = meta,
                    )
                )
            } catch (e: Exception) {
                showToast("Не удалось сгенерировать: " + (e.message ?: e.javaClass.simpleName))
            }
            genLoading = false
        }
    }

    fun explainWrong() {
        if (drillExplainLoading) return
        val d = drill
        val picked = drillPick.orEmpty()
        drillExplainLoading = true
        drillExplain = ""
        viewModelScope.launch {
            if (apiKey.isEmpty()) {
                delay(800)
                drillExplainLoading = false
                drillExplain = d.whyNot[picked] ?: d.explanation
                drillExplainMeta = "Демо-объяснение. Подключите OpenRouter, чтобы получить персональное."
                return@launch
            }
            try {
                val (text, meta) = chat(
                    listOf(
                        ChatMessage("system", SYS_EXPLAIN),
                        ChatMessage(
                            "user",
                            "Фрагмент: «${d.sentences.joinToString(" ")}»\n" +
                                "Ошибочная часть: «${d.flawed.mapNotNull { d.sentences.getOrNull(it) }.joinToString(" ")}»\n" +
                                "Правильный ответ: ${d.answer}\n" +
                                "Ученик выбрал: $picked\n" +
                                "Объясни, почему «$picked» не подходит и что выдаёт «${d.answer}». Закончи одним коротким вопросом, который проверяет разницу.",
                        ),
                    ),
                    1500,
                )
                drillExplain = text.trim()
                drillExplainMeta = meta
            } catch (e: Exception) {
                showToast("Не удалось получить объяснение: " + (e.message ?: e.javaClass.simpleName))
            }
            drillExplainLoading = false
        }
    }

    // ── Analyze ────────────────────────────────────────────────────────────

    fun onAnText(v: String) { anText = v }
    fun useSample(i: Int) { anText = SAMPLES[i].text }

    fun analyze() {
        val text = anText.trim()
        if (anLoading) return
        if (text.length < 40) return showToast("Дайте Ergo хотя бы пару предложений.")
        anLoading = true
        anResult = null
        viewModelScope.launch {
            if (apiKey.isEmpty()) {
                delay(1100)
                anLoading = false
                val sm = SAMPLES.find { it.text == text }
                if (sm != null) {
                    anResult = sm.result
                    anSource = text
                    anMeta = "Демо-карта. Подключите OpenRouter, чтобы разбирать свои тексты."
                } else {
                    showToast("В демо-режиме разбираются только примеры. Подключите OpenRouter для своих текстов.")
                }
                return@launch
            }
            try {
                val (o, meta) = chatJson(listOf(ChatMessage("system", SYS_AN), ChatMessage("user", text)), 4000)
                val issues = o.optJSONArray("issues")?.let { a ->
                    (0 until a.length()).mapNotNull { a.optJSONObject(it) }
                        .map { Issue(it.str("name"), it.str("quote"), it.str("note")) }
                }.orEmpty()
                anResult = Analysis(
                    conclusion = o.str("conclusion").ifEmpty { "—" },
                    premises = o.stringList("premises"),
                    assumptions = o.stringList("assumptions"),
                    issues = issues,
                    verdict = o.str("verdict").ifEmpty { "Moderate" },
                    summary = o.str("summary"),
                )
                anSource = text
                anMeta = "Разобрано моделью $meta"
            } catch (e: Exception) {
                showToast("Анализ не удался: " + (e.message ?: e.javaClass.simpleName))
            }
            anLoading = false
        }
    }

    // ── Spar ───────────────────────────────────────────────────────────────

    fun startSpar(t: Topic) {
        sparTopic = t
        sparMsgs = listOf(SparMsg(fromMe = false, text = t.opener))
        sparI = 0
    }
    fun endSpar() { sparTopic = null; sparMsgs = emptyList() }
    fun onSparInput(v: String) { sparInput = v }

    fun sendSpar() {
        val txt = sparInput.trim()
        val topic = sparTopic ?: return
        if (txt.isEmpty() || sparLoading) return
        val msgs = sparMsgs + SparMsg(fromMe = true, text = txt)
        sparMsgs = msgs
        sparInput = ""
        sparLoading = true
        viewModelScope.launch {
            val (reply, flag) = try {
                if (apiKey.isEmpty()) {
                    delay(1100)
                    SPAR_DEMO[sparI % SPAR_DEMO.size] to demoFlag(txt)
                } else {
                    val hist = msgs.map { ChatMessage(if (it.fromMe) "user" else "assistant", it.text) }
                    val (parsed, _) = chatJson(listOf(ChatMessage("system", sysSpar(topic.motion))) + hist, 2000)
                    val f = parsed.optJSONObject("flag")
                    val name = f?.str("name").orEmpty()
                    val reply = parsed.str("reply").ifBlank { throw EmptyReply("модель вернула пустой ответ.") }
                    reply to if (name.isNotEmpty()) Flag(name, f!!.str("quote"), f.str("note")) else null
                }
            } catch (e: Exception) {
                sparLoading = false
                return@launch showToast("Ergo не смог ответить: " + (e.message ?: e.javaClass.simpleName))
            }
            if (sparTopic !== topic) { sparLoading = false; return@launch }
            val m2 = sparMsgs.toMutableList()
            m2[m2.lastIndex] = m2.last().copy(flag = flag)
            sparMsgs = m2 + SparMsg(fromMe = false, text = reply)
            sparLoading = false
            sparI += 1
        }
    }

    // ── Back navigation ────────────────────────────────────────────────────

    /** Returns true when the back press was consumed. */
    fun back(): Boolean = when {
        sheetOpen -> { sheetOpen = false; true }
        lessonOpen -> { lessonOpen = false; true }
        screen == Screen.Onboarding && obStep > 0 -> { obBack(); true }
        screen == Screen.Onboarding && store.onboarded -> { screen = Screen.App; true }
        screen == Screen.App && tab == Tab.Spar && sparTopic != null -> { endSpar(); true }
        screen == Screen.App && tab != Tab.Learn -> { go(Tab.Learn); true }
        else -> false
    }
}

enum class LessonState { Done, Current, Locked }
