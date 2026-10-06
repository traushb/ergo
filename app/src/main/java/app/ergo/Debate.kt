package app.ergo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.ergo.data.Analysis
import app.ergo.data.Bank
import app.ergo.data.ChatMessage
import app.ergo.data.Flag
import app.ergo.data.Issue
import app.ergo.data.Motion
import app.ergo.data.SYS_AN
import app.ergo.data.demoFlag
import app.ergo.data.offlineAnalysis
import app.ergo.data.offlineReply
import app.ergo.data.str
import app.ergo.data.stringList
import app.ergo.data.sysSpar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SparMsg(val fromMe: Boolean, val text: String, val flag: Flag? = null)

/** The debate: you defend a motion, Ergo argues against and flags your fallacies. */
class SparController(
    private val bank: Bank,
    private val ai: Ai,
    private val scope: CoroutineScope,
    private val toast: (String) -> Unit,
) {
    var motion by mutableStateOf<Motion?>(null); private set
    var msgs by mutableStateOf<List<SparMsg>>(emptyList()); private set
    var input by mutableStateOf(""); private set
    var loading by mutableStateOf(false); private set
    /** The round-up card is showing. */
    var ended by mutableStateOf(false); private set
    private var turn = 0

    val myTurns: Int get() = msgs.count { it.fromMe }
    val flags: List<Flag> get() = msgs.mapNotNull { it.flag }

    fun start(m: Motion) {
        motion = m
        msgs = listOf(SparMsg(fromMe = false, text = m.opener))
        input = ""
        turn = 0
        ended = false
    }

    /** Back arrow: a round-up if you said anything, otherwise straight back to the motions. */
    fun end() {
        if (myTurns > 0) ended = true else leave()
    }

    fun resume() {
        ended = false
    }

    fun leave() {
        motion = null
        msgs = emptyList()
        input = ""
        ended = false
        loading = false
    }

    fun onInput(v: String) {
        input = v
    }

    fun send() {
        val txt = input.trim()
        val m = motion ?: return
        if (txt.isEmpty() || loading) return
        val history = msgs + SparMsg(fromMe = true, text = txt)
        msgs = history
        input = ""
        loading = true
        scope.launch {
            val (reply, flag) = try {
                if (!ai.connected) {
                    delay(700)
                    val f = demoFlag(txt, bank)
                    offlineReply(bank, m, turn, f) to f
                } else {
                    val hist = history.map { ChatMessage(if (it.fromMe) "user" else "assistant", it.text) }
                    ai.chatJson(listOf(ChatMessage("system", sysSpar(m.motion))) + hist, 2000) { o ->
                        val reply = o.str("reply").ifBlank { throw org.json.JSONException("empty reply") }
                        val f = o.optJSONObject("flag")
                        val name = f?.str("name").orEmpty()
                        reply to if (name.isNotEmpty()) Flag(name, f!!.str("quote"), f.str("note"), bank.topicByName(name)?.id) else null
                    }.first
                }
            } catch (e: Exception) {
                loading = false
                return@launch toast("Ergo не смог ответить: " + (e.message ?: e.javaClass.simpleName))
            }
            if (motion !== m) {
                loading = false
                return@launch
            }
            val updated = msgs.toMutableList()
            updated[updated.lastIndex] = updated.last().copy(flag = flag)
            msgs = updated + SparMsg(fromMe = false, text = reply)
            loading = false
            turn += 1
        }
    }
}

/** Paste an argument, get its skeleton and the weak spots. */
class AnalyzeController(
    private val bank: Bank,
    private val ai: Ai,
    private val scope: CoroutineScope,
    private val toast: (String) -> Unit,
) {
    var text by mutableStateOf(""); private set
    var source by mutableStateOf(""); private set
    var result by mutableStateOf<Analysis?>(null); private set
    var loading by mutableStateOf(false); private set
    var meta by mutableStateOf(""); private set

    fun onText(v: String) {
        text = v
    }

    fun useSample(i: Int) {
        text = bank.samples[i].text
    }

    fun analyze() {
        val t = text.trim()
        if (loading) return
        if (t.length < 40) return toast("Дайте Ergo хотя бы пару предложений.")
        val sample = bank.samples.find { it.text == t }
        if (sample != null || !ai.connected) {
            source = t
            if (sample != null) {
                result = sample.result
                meta = "Разбор из встроенных примеров."
            } else {
                result = offlineAnalysis(bank, t)
                meta = "Быстрый офлайн-разбор по ключевым словам. Подключите OpenRouter, чтобы получить полный анализ."
            }
            return
        }
        loading = true
        result = null
        scope.launch {
            try {
                val (an, m) = ai.chatJson(listOf(ChatMessage("system", SYS_AN), ChatMessage("user", t)), 4000) { o ->
                    val issues = o.optJSONArray("issues")?.let { a ->
                        (0 until a.length()).mapNotNull { a.optJSONObject(it) }.map {
                            val name = it.str("name")
                            Issue(name, it.str("quote"), it.str("note"), bank.topicByName(name)?.id)
                        }
                    }.orEmpty()
                    Analysis(
                        conclusion = o.str("conclusion").ifEmpty { "—" },
                        premises = o.stringList("premises"),
                        assumptions = o.stringList("assumptions"),
                        issues = issues,
                        verdict = o.str("verdict").ifEmpty { "Moderate" },
                        summary = o.str("summary"),
                    )
                }
                result = an
                source = t
                meta = "Разобрано моделью $m"
            } catch (e: Exception) {
                toast("Анализ не удался: " + (e.message ?: e.javaClass.simpleName))
            }
            loading = false
        }
    }
}
