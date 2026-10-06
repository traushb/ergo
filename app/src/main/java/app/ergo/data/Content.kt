package app.ergo.data

// Shared models, model prompts and formatting. The learning content itself lives in
// assets/content/*.json (see Bank.kt).

enum class MarkStyle { Pencil, Highlighter }

enum class ThemeMode { System, Light, Dark }

data class Goal(val id: String, val title: String, val sub: String)

val GOALS = listOf(
    Goal("course", "Курс критического мышления", "Идти в ногу с программой"),
    Goal("online", "Спорить в интернете лучше", "Замечать нечестные приёмы в тредах"),
    Goal("news", "Читать новости скептически", "Утверждения, факты и манипуляции"),
    Goal("curious", "Просто интересно", "В своём темпе, без спешки"),
)

/** [topic] links an issue to the reference card, when it matches a known topic. */
data class Issue(val name: String, val quote: String, val note: String, val topic: String? = null)

data class Analysis(
    val conclusion: String,
    val premises: List<String>,
    val assumptions: List<String>,
    val issues: List<Issue>,
    val verdict: String,
    val summary: String,
    /** True for the offline keyword-based pass, which only points at possible problems. */
    val heuristic: Boolean = false,
)

val VERDICT_RU = mapOf("Strong" to "Сильный", "Moderate" to "Средний", "Weak" to "Слабый")

data class Flag(val name: String, val quote: String, val note: String, val topic: String? = null)

// ── Prompts ────────────────────────────────────────────────────────────────

private const val RU = " Пиши ВСЕ текстовые значения на русском языке; ключи JSON оставь на английском."

const val SYS_DRILL = """You write practice exercises for an informal-logic learning app for Russian-speaking learners. The user names ONE fallacy or bias and three distractors. Return ONLY a JSON object, no prose, no code fences:
{"source": short label such as "Комментарий · городской форум" or "Реклама · доставка еды" (fictional names only),
 "sentences": [3-4 short sentences forming ONE realistic snippet: a social post, ad, op-ed excerpt, group chat, dialogue or review, set in a Russian-speaking context],
 "flawed": [0-based indices of the sentence(s) that contain the named flaw],
 "explanation": 2 sentences, wry and precise, on why this is the named flaw,
 "whyNot": {"<distractor name exactly as given>": one sentence on why it doesn't fit — one entry for each of the three distractors}}
The snippet must contain exactly one flaw: the named one. Make it subtle and realistic, not cartoonish.$RU"""

const val SYS_STRUCT = """You write argument-analysis exercises for a logic-learning app for Russian-speaking learners. Return ONLY a JSON object, no prose, no code fences:
{"source": short label of where the argument comes from (fictional),
 "sentences": [exactly 3 short sentences forming one everyday argument: one conclusion and two premises, in any order],
 "conclusion": 0-based index of the conclusion sentence,
 "premises": [0-based indices of the premise sentences],
 "assumption": one sentence: the unstated premise the argument needs for the conclusion to follow,
 "wrong": [two plausible sentences on the same subject that the argument does NOT need],
 "explanation": 2 sentences: what the conclusion is and why the assumption is needed}$RU"""

const val SYS_QUIZ = """You write deductive-logic puzzles for a logic-learning app for Russian-speaking learners. Return ONLY a JSON object, no prose, no code fences:
{"source": "Логическая задача",
 "passage": two premises and a conclusion in plain everyday language,
 "question": a short question about whether the conclusion follows and whether the argument is sound,
 "options": [exactly 3 short answers],
 "answer": 0-based index of the correct option,
 "explanation": 2 sentences}
Vary the form: valid and sound, valid with a false premise, affirming the consequent, undistributed middle, modus tollens.$RU"""

const val SYS_AN = """You are an argument analyst in a logic-learning app. Given a passage, extract its structure. Return ONLY a JSON object, no prose, no code fences:
{"conclusion": string, "premises": [max 4 short paraphrased strings], "assumptions": [max 3 unstated premises the argument needs], "issues": [max 4 {"name": fallacy or weakness name, "quote": EXACT substring copied verbatim from the passage, "note": one or two plain sentences}], "verdict": "Strong" | "Moderate" | "Weak" (keep this value in English), "summary": one sentence}$RU"""

fun sysSpar(motion: String) =
    """You are Ergo, a sharp but fair debate opponent in a logic-learning app. Motion: "$motion". The student argues FOR; you argue AGAINST. Keep replies under 70 words, conversational, ending with one pointed question. Then check ONLY the student's latest message for a clear informal fallacy or cognitive bias. Return ONLY JSON: {"reply": string, "flag": null or {"name": Russian fallacy name, "quote": short exact phrase from the student's message, "note": one sentence}}. Flag only clear cases.$RU"""

const val SYS_EXPLAIN = "Ты Ergo — остроумный наставник по неформальной логике в сократовском духе. Только простой текст на русском, не больше 80 слов, без вступлений."

// ── Formatting ─────────────────────────────────────────────────────────────

fun fmtCost(c: Double?): String = when {
    c == null -> ""
    c == 0.0 -> "$0"
    c < 0.01 -> "$" + "%.5f".format(java.util.Locale.US, c)
    else -> "$" + "%.3f".format(java.util.Locale.US, c)
}

fun per1M(p: Double): String {
    val x = p * 1e6
    return when {
        x == 0.0 -> "бесплатно"
        x < 0.1 -> "$" + "%.3f".format(java.util.Locale.US, x)
        else -> "$" + "%.2f".format(java.util.Locale.US, x)
    }
}

fun money(v: Double?): String = if (v == null) "—" else "$" + "%.2f".format(java.util.Locale.US, v)

/** Russian plural forms: 1 урок, 2 урока, 5 уроков. */
fun plural(n: Int, one: String, few: String, many: String): String {
    val a = n % 10
    val b = n % 100
    return if (a == 1 && b != 11) one else if (a in 2..4 && (b < 12 || b > 14)) few else many
}

/** "5 уроков" — the number with its plural form. */
fun count(n: Int, one: String, few: String, many: String) = "$n ${plural(n, one, few, many)}"
