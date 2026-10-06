package app.ergo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.ergo.data.Bank
import app.ergo.data.Drill
import app.ergo.data.Progress
import app.ergo.data.Quiz
import app.ergo.data.StructureItem
import app.ergo.data.Topic
import app.ergo.data.TopicKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/** What to train: everything, one unit of the syllabus, or a single topic. */
sealed interface TopicFilter {
    data object All : TopicFilter
    data class Section(val unit: Int) : TopicFilter
    data class One(val topic: String) : TopicFilter
}

sealed interface Exercise {
    val id: String
}

data class FallacyEx(val drill: Drill) : Exercise {
    override val id: String get() = drill.id
}

data class StructureEx(val item: StructureItem) : Exercise {
    override val id: String get() = item.id
}

data class QuizEx(val quiz: Quiz) : Exercise {
    override val id: String get() = quiz.id
}

/** A missed exercise, for the summary's "what to work on" list. */
data class Mistake(val topic: String, val text: String, val picked: String? = null, val falseAlarm: Boolean = false)

/** The result of one exercise: spaced-repetition records per topic, and points. */
data class Outcome(
    val records: List<Pair<String, Boolean>>,
    val points: Int,
    val max: Int,
    val correct: Boolean,
    val mistake: Mistake? = null,
    val skipped: Boolean = false,
)

// ── Exercise runs ──────────────────────────────────────────────────────────
// Each run holds the state of the exercise on screen. Answer methods return an
// Outcome once, when the exercise is resolved.

sealed interface Run {
    val resolved: Boolean
}

data object LoadingRun : Run {
    override val resolved = false
}

enum class FPhase { Spot, Name, Result }

/** Find the broken sentence (or say there's none), name the fallacy, read why. 3 points. */
class FallacyRun(val drill: Drill, val options: List<String>) : Run {
    var phase by mutableStateOf(FPhase.Spot); private set
    var spot by mutableStateOf<Int?>(null); private set
    var misses by mutableStateOf<List<Int>>(emptyList()); private set
    var falseAlarm by mutableStateOf(false); private set
    var pick by mutableStateOf<String?>(null); private set
    var explain by mutableStateOf("")
    var explainMeta by mutableStateOf("")
    var explainLoading by mutableStateOf(false)

    override val resolved: Boolean get() = phase == FPhase.Result
    val correct: Boolean get() = if (drill.clean) misses.isEmpty() else pick == drill.topic

    fun tap(i: Int): Outcome? {
        if (phase != FPhase.Spot || i !in drill.sentences.indices) return null
        if (!drill.clean && i in drill.flawed) {
            spot = i
            phase = FPhase.Name
        } else if (i !in misses) {
            misses = misses + i
        }
        return null
    }

    /** "Ошибки нет". Right on a clean snippet; on a flawed one it only costs the spotting point. */
    fun noFlaw(): Outcome? {
        if (phase != FPhase.Spot) return null
        if (!drill.clean) {
            falseAlarm = true
            return null
        }
        phase = FPhase.Result
        val ok = misses.isEmpty()
        return Outcome(
            records = listOf(drill.topic to ok),
            points = if (ok) 3 else 1,
            max = 3,
            correct = ok,
            mistake = if (ok) null else Mistake(drill.topic, drill.brief, falseAlarm = true),
        )
    }

    fun choose(topic: String): Outcome? {
        if (phase != FPhase.Name) return null
        pick = topic
        phase = FPhase.Result
        val ok = topic == drill.topic
        val spotPoint = if (misses.isEmpty() && !falseAlarm) 1 else 0
        return Outcome(
            records = listOf(drill.topic to ok),
            points = spotPoint + if (ok) 2 else 0,
            max = 3,
            correct = ok,
            mistake = if (ok) null else Mistake(drill.topic, drill.brief, picked = topic),
        )
    }
}

enum class SPhase { Conclusion, Premise, Assumption, Result }

/** Find the conclusion, then a premise, then pick the hidden assumption. One point each. */
class StructureRun(val item: StructureItem, val options: List<String>) : Run {
    var phase by mutableStateOf(SPhase.Conclusion); private set
    var conclusionMisses by mutableStateOf<List<Int>>(emptyList()); private set
    var premiseMisses by mutableStateOf<List<Int>>(emptyList()); private set
    var premise by mutableStateOf<Int?>(null); private set
    var pick by mutableStateOf<Int?>(null); private set
    /** True right after tapping the conclusion again while looking for a premise. */
    var tappedConclusion by mutableStateOf(false); private set

    override val resolved: Boolean get() = phase == SPhase.Result
    val answer: Int get() = options.indexOf(item.assumption)

    fun tap(i: Int): Outcome? {
        if (i !in item.sentences.indices) return null
        when (phase) {
            SPhase.Conclusion ->
                if (i == item.conclusion) phase = SPhase.Premise
                else if (i !in conclusionMisses) conclusionMisses = conclusionMisses + i
            SPhase.Premise -> when {
                i in item.premises -> {
                    premise = i
                    tappedConclusion = false
                    phase = SPhase.Assumption
                }
                i == item.conclusion -> tappedConclusion = true
                i !in premiseMisses -> premiseMisses = premiseMisses + i
            }
            else -> {}
        }
        return null
    }

    fun choose(k: Int): Outcome? {
        if (phase != SPhase.Assumption || k !in options.indices) return null
        pick = k
        phase = SPhase.Result
        val c = conclusionMisses.isEmpty()
        val p = premiseMisses.isEmpty()
        val a = k == answer
        val failed = when {
            !a -> "assumptions"
            !c -> "conclusion"
            !p -> "premises"
            else -> null
        }
        return Outcome(
            records = listOf("conclusion" to c, "premises" to p, "assumptions" to a),
            points = listOf(c, p, a).count { it },
            max = 3,
            correct = c && p && a,
            mistake = failed?.let { Mistake(it, item.sentences.joinToString(" "), picked = if (!a) options[k] else null) },
        )
    }
}

/** A single multiple-choice question about a passage. 3 points. */
class QuizRun(val quiz: Quiz) : Run {
    var pick by mutableStateOf<Int?>(null); private set
    override val resolved: Boolean get() = pick != null

    fun choose(k: Int): Outcome? {
        if (pick != null || k !in quiz.options.indices) return null
        pick = k
        val ok = k == quiz.answer
        return Outcome(
            records = listOf(quiz.topic to ok),
            points = if (ok) 3 else 0,
            max = 3,
            correct = ok,
            mistake = if (ok) null else Mistake(quiz.topic, quiz.question, picked = quiz.options[k]),
        )
    }
}

// ── Sessions, blitz, summary ───────────────────────────────────────────────

enum class SessionKind { Practice, Review, Fresh }

class Session(
    val kind: SessionKind,
    val title: String,
    val filter: TopicFilter,
    private val planned: List<Exercise>,
    val total: Int,
) {
    var index by mutableIntStateOf(0)
    var run by mutableStateOf<Run>(LoadingRun)
    var points by mutableIntStateOf(0)
    var maxPoints by mutableIntStateOf(0)
    val outcomes = mutableStateListOf<Outcome>()
    /** Bumped whenever a new exercise appears, so the screen can scroll back to the top. */
    var serial by mutableIntStateOf(0)
    var generating by mutableStateOf(false)

    fun plannedAt(i: Int): Exercise? = planned.getOrNull(i)
}

data class BlitzQ(val drill: Drill, val options: List<String>)

class Blitz(val filter: TopicFilter, val pool: List<Drill>) {
    var timeLeft by mutableLongStateOf(DURATION_MS)
    var score by mutableIntStateOf(0)
    var combo by mutableIntStateOf(1)
    var right by mutableIntStateOf(0)
    var total by mutableIntStateOf(0)
    var q by mutableStateOf<BlitzQ?>(null)
    /** The picked option while its colour feedback is showing; the timer pauses meanwhile. */
    var feedback by mutableStateOf<String?>(null)
    var penalties by mutableIntStateOf(0)
    var paused by mutableStateOf(false)
    var finished by mutableStateOf(false)
    val mistakes = mutableStateListOf<Mistake>()
    internal val recent = ArrayDeque<String>()

    companion object {
        const val DURATION_MS = 60_000L
        const val PENALTY_MS = 3_000L
        const val MAX_COMBO = 4
    }
}

enum class SummaryKind { Practice, Review, Fresh, Blitz }

data class Summary(
    val kind: SummaryKind,
    val title: String,
    val filter: TopicFilter,
    val points: Int,
    val max: Int,
    val right: Int,
    val total: Int,
    val mistakes: List<Mistake>,
    /** Topics answered correctly at least once. */
    val strong: List<String>,
    val streak: Int,
    val blitzBest: Int = 0,
    val newRecord: Boolean = false,
)

class PracticeController(
    private val bank: Bank,
    private val ai: Ai,
    private val scope: CoroutineScope,
    private val progress: () -> Progress,
    private val update: ((Progress) -> Progress) -> Unit,
    private val toast: (String) -> Unit,
    private val today: () -> Long,
    private val rnd: Random = Random.Default,
) {
    var filter by mutableStateOf<TopicFilter>(TopicFilter.All)
    var session by mutableStateOf<Session?>(null); private set
    var blitz by mutableStateOf<Blitz?>(null); private set
    var summary by mutableStateOf<Summary?>(null); private set

    private val recent = ArrayDeque<String>()
    private var blitzJob: Job? = null

    fun label(f: TopicFilter = filter): String = when (f) {
        TopicFilter.All -> "Все темы"
        is TopicFilter.Section -> bank.units[f.unit].let { "Раздел ${it.n}. ${it.title}" }
        is TopicFilter.One -> bank.topic(f.topic)?.name ?: "Тема"
    }

    fun topicsOf(f: TopicFilter): List<Topic> = when (f) {
        TopicFilter.All -> bank.lessonOrder
        is TopicFilter.Section -> bank.topicsIn(f.unit)
        is TopicFilter.One -> listOfNotNull(bank.topic(f.topic))
    }

    fun select(f: TopicFilter) {
        filter = f
    }

    // ── Building sessions ──────────────────────────────────────────────────

    /** Shuffled, with exercises seen recently in this run of the app pushed to the back. */
    private fun <T> fresh(items: List<T>, id: (T) -> String): List<T> =
        items.shuffled(rnd).sortedBy { if (id(it) in recent) 1 else 0 }

    /** Unseen first, and one per topic before any topic repeats. */
    private fun varied(pool: List<Drill>, n: Int): List<Drill> {
        if (n <= 0) return emptyList()
        val ordered = fresh(pool) { it.id }
        val first = ordered.distinctBy { it.topic }
        return (first + ordered.filter { it !in first }).take(n)
    }

    fun buildExercises(f: TopicFilter): List<Exercise> {
        val out = mutableListOf<Exercise>()
        val flawed = bank.drills.filter { !it.clean }
        when (f) {
            TopicFilter.All -> {
                out += varied(flawed, 6).map(::FallacyEx)
                fresh(bank.drills.filter { it.clean }) { it.id }.firstOrNull()?.let { out += FallacyEx(it) }
                val extra: List<Exercise> = bank.structure.map(::StructureEx) + bank.quizzes.map(::QuizEx)
                fresh(extra) { it.id }.firstOrNull()?.let { out += it }
            }
            is TopicFilter.Section -> {
                val topics = bank.topicsIn(f.unit)
                val ids = topics.map { it.id }.toSet()
                if (topics.none { it.drillable }) {
                    out += fresh(bank.structure) { it.id }.take(4).map(::StructureEx)
                    out += fresh(bank.quizzes.filter { it.topic in ids }) { it.id }.take(2).map(::QuizEx)
                } else {
                    out += varied(flawed.filter { it.topic in ids }, 6).map(::FallacyEx)
                    fresh(bank.drills.filter { it.clean && it.topic in ids }) { it.id }.firstOrNull()?.let { out += FallacyEx(it) }
                    fresh(bank.quizzes.filter { it.topic in ids }) { it.id }.firstOrNull()?.let { out += QuizEx(it) }
                }
            }
            is TopicFilter.One -> {
                val t = bank.topic(f.topic) ?: return buildExercises(TopicFilter.All)
                when (t.kind) {
                    TopicKind.Structure -> out += fresh(bank.structure) { it.id }.take(6).map(::StructureEx)
                    TopicKind.Logic -> out += fresh(bank.quizzesFor(t.id)) { it.id }.take(6).map(::QuizEx)
                    else -> {
                        out += fresh(bank.drillsFor(t.id)) { it.id }.map(::FallacyEx)
                        out += fresh(bank.quizzesFor(t.id)) { it.id }.take(1).map(::QuizEx)
                        out += fresh(bank.cleanFor(t.id)) { it.id }.take(1).map(::FallacyEx)
                        // Neighbours from the same unit, so naming the topic is a real choice.
                        val mates = bank.topicsIn(bank.unitIndexOf(t.id)).filter { it.drillable && it.id != t.id }.map { it.id }.toSet()
                        out += varied(flawed.filter { it.topic in mates }, 6 - out.size).map(::FallacyEx)
                    }
                }
            }
        }
        return out.distinctBy { it.id }.shuffled(rnd)
    }

    /** One exercise per due topic, up to eight. */
    fun buildReview(due: List<String>): List<Exercise> {
        val out = LinkedHashMap<String, Exercise>()
        for (id in due) {
            val t = bank.topic(id) ?: continue
            val ex: Exercise? = when (t.kind) {
                TopicKind.Structure -> fresh(bank.structure) { it.id }.firstOrNull { it.id !in out }?.let(::StructureEx)
                TopicKind.Logic -> fresh(bank.quizzesFor(t.id)) { it.id }.firstOrNull()?.let(::QuizEx)
                else -> fresh(bank.drillsFor(t.id)) { it.id }.firstOrNull()?.let(::FallacyEx)
                    ?: fresh(bank.quizzesFor(t.id)) { it.id }.firstOrNull()?.let(::QuizEx)
            }
            if (ex != null) out[ex.id] = ex
            if (out.size >= 8) break
        }
        return out.values.toList()
    }

    // ── Starting ───────────────────────────────────────────────────────────

    fun startPractice(f: TopicFilter = filter) {
        filter = f
        val ex = buildExercises(f)
        if (ex.isEmpty()) return toast("Для этой темы пока нет заданий.")
        begin(Session(SessionKind.Practice, label(f), f, ex, ex.size))
    }

    fun startReview() {
        val due = progress().dueOn(today())
        if (due.isEmpty()) return toast("На сегодня повторять нечего. Темы вернутся по расписанию: через 1, 3, 7 и 21 день.")
        val ex = buildReview(due)
        if (ex.isEmpty()) return toast("Для этих тем пока нет заданий.")
        begin(Session(SessionKind.Review, "Повторение", TopicFilter.All, ex, ex.size))
    }

    /** A session where every exercise is written by the model. */
    fun startFresh(f: TopicFilter = filter) {
        if (!ai.connected) return toast("Для свежих заданий нужен ключ OpenRouter. Добавьте его в профиле.")
        filter = f
        begin(Session(SessionKind.Fresh, label(f), f, emptyList(), FRESH_SIZE))
    }

    private fun begin(s: Session) {
        stopBlitz()
        summary = null
        session = s
        show(s, 0)
    }

    private fun show(s: Session, i: Int) {
        s.index = i
        s.serial += 1
        val planned = s.plannedAt(i)
        if (planned != null) {
            s.run = runFor(planned)
            remember(planned.id)
        } else {
            generate(s, pickTopic(s.filter), replace = false)
        }
    }

    private fun runFor(e: Exercise): Run = when (e) {
        is FallacyEx -> FallacyRun(e.drill, bank.optionsFor(e.drill, rnd))
        is StructureEx -> StructureRun(e.item, (e.item.wrong + e.item.assumption).shuffled(rnd))
        is QuizEx -> QuizRun(e.quiz)
    }

    private fun remember(id: String) {
        recent.remove(id)
        recent.addLast(id)
        while (recent.size > 40) recent.removeFirst()
    }

    private fun pickTopic(f: TopicFilter): Topic = topicsOf(f).randomOrNull(rnd) ?: bank.drillTopics.random(rnd)

    private fun generate(s: Session, topic: Topic, replace: Boolean) {
        if (s.generating) return
        s.generating = true
        if (!replace) s.run = LoadingRun
        val index = s.index
        scope.launch {
            val ex: Exercise? = try {
                when (topic.kind) {
                    TopicKind.Structure -> StructureEx(ai.structure())
                    TopicKind.Logic -> QuizEx(ai.quiz(topic))
                    else -> FallacyEx(ai.drill(bank, topic, rnd))
                }
            } catch (e: Exception) {
                toast("Не удалось сгенерировать: " + (e.message ?: e.javaClass.simpleName))
                null
            }
            s.generating = false
            if (session !== s || s.index != index) return@launch
            val next = ex ?: if (replace) null else (buildExercises(TopicFilter.One(topic.id)).firstOrNull() ?: buildExercises(TopicFilter.All).first())
            if (next != null) {
                s.run = runFor(next)
                s.serial += 1
            }
        }
    }

    /** Swaps the exercise on screen for a new one from the model, before it's answered. */
    fun regenerate() {
        val s = session ?: return
        if (!ai.connected) return toast("Для новых заданий нужен ключ OpenRouter. Добавьте его в профиле.")
        if (s.run.resolved || s.generating) return
        generate(s, pickTopic(s.filter), replace = true)
    }

    // ── Answers ────────────────────────────────────────────────────────────

    fun tap(i: Int) {
        val s = session ?: return
        when (val r = s.run) {
            is FallacyRun -> r.tap(i)
            is StructureRun -> r.tap(i)
            else -> null
        }?.let { done(s, it) }
    }

    fun noFlaw() {
        val s = session ?: return
        (s.run as? FallacyRun)?.noFlaw()?.let { done(s, it) }
    }

    fun choose(topic: String) {
        val s = session ?: return
        (s.run as? FallacyRun)?.choose(topic)?.let { done(s, it) }
    }

    fun chooseOption(k: Int) {
        val s = session ?: return
        when (val r = s.run) {
            is StructureRun -> r.choose(k)
            is QuizRun -> r.choose(k)
            else -> null
        }?.let { done(s, it) }
    }

    private fun done(s: Session, o: Outcome) {
        s.outcomes += o
        s.points += o.points
        s.maxPoints += o.max
        val day = today()
        update { p -> o.records.fold(p) { acc, (t, ok) -> acc.record(t, ok, day) }.finish(o.correct, day) }
    }

    /** "Дальше" after an answer, or "Пропустить" before one. */
    fun next() {
        val s = session ?: return
        if (s.generating && s.run is LoadingRun) return
        if (!s.run.resolved && s.run !is LoadingRun) {
            s.outcomes += Outcome(emptyList(), 0, 3, false, skipped = true)
            s.maxPoints += 3
        }
        if (s.index + 1 >= s.total) finish(s) else show(s, s.index + 1)
    }

    /** The close button: shows the summary if anything was answered. */
    fun quit() {
        val s = session ?: return
        if (s.outcomes.any { !it.skipped }) finish(s) else session = null
    }

    private fun finish(s: Session) {
        val done = s.outcomes.filter { !it.skipped }
        summary = Summary(
            kind = when (s.kind) {
                SessionKind.Practice -> SummaryKind.Practice
                SessionKind.Review -> SummaryKind.Review
                SessionKind.Fresh -> SummaryKind.Fresh
            },
            title = s.title,
            filter = s.filter,
            points = s.points,
            max = s.maxPoints,
            right = done.count { it.correct },
            total = done.size,
            mistakes = done.mapNotNull { it.mistake },
            strong = done.flatMap { o -> o.records.filter { it.second }.map { it.first } }.distinct(),
            streak = progress().streakOn(today()),
        )
        session = null
    }

    fun closeSummary() {
        summary = null
    }

    fun again() {
        val s = summary ?: return
        when (s.kind) {
            SummaryKind.Practice -> startPractice(s.filter)
            SummaryKind.Review -> startReview()
            SummaryKind.Fresh -> startFresh(s.filter)
            SummaryKind.Blitz -> startBlitz(s.filter)
        }
    }

    /** "Почему не «X»?": the model's explanation when connected, the bank's note otherwise. */
    fun explainWrong() {
        val r = session?.run as? FallacyRun ?: return
        val picked = r.pick ?: return
        if (r.explainLoading || r.explain.isNotEmpty()) return
        val d = r.drill
        if (!ai.connected) {
            r.explain = bank.whyNot(d, picked)
            r.explainMeta = "Подсказка из встроенной базы. С ключом OpenRouter объяснение напишет модель."
            return
        }
        r.explainLoading = true
        scope.launch {
            try {
                val (text, meta) = ai.explain(d, labelOf(d.topic), labelOf(picked))
                r.explain = text
                r.explainMeta = meta
            } catch (e: Exception) {
                r.explain = bank.whyNot(d, picked)
                r.explainMeta = "Модель не ответила (${e.message ?: "ошибка"}). Показана подсказка из встроенной базы."
            }
            r.explainLoading = false
        }
    }

    private fun labelOf(id: String) = bank.topic(id)?.label ?: id

    // ── Blitz ──────────────────────────────────────────────────────────────

    fun startBlitz(f: TopicFilter = filter) {
        filter = f
        val pool = blitzPool(f)
        session = null
        summary = null
        stopBlitz()
        val b = Blitz(f, pool)
        b.q = nextQuestion(b)
        blitz = b
        blitzJob = scope.launch {
            while (!b.finished) {
                delay(TICK_MS)
                if (b.feedback == null && !b.paused) b.timeLeft = (b.timeLeft - TICK_MS).coerceAtLeast(0)
                if (b.timeLeft <= 0L) finishBlitz(b)
            }
        }
    }

    /** The filter's unit when it has enough material, otherwise everything. */
    private fun blitzPool(f: TopicFilter): List<Drill> {
        val all = bank.drills.filter { !it.clean && it.brief.isNotBlank() }
        val ids = when (f) {
            TopicFilter.All -> return all
            is TopicFilter.Section -> bank.units[f.unit].topicIds.toSet()
            is TopicFilter.One -> bank.unitOf(f.topic).topicIds.toSet()
        }
        val pool = all.filter { it.topic in ids }
        return if (pool.size >= 8) pool else all
    }

    private fun nextQuestion(b: Blitz): BlitzQ {
        val d = b.pool.shuffled(rnd).firstOrNull { it.id !in b.recent } ?: b.pool.random(rnd)
        b.recent.addLast(d.id)
        while (b.recent.size > minOf(12, b.pool.size - 1)) b.recent.removeFirst()
        return BlitzQ(d, bank.optionsFor(d, rnd))
    }

    fun blitzPick(topic: String) {
        val b = blitz ?: return
        val q = b.q ?: return
        if (b.finished || b.feedback != null) return
        val ok = topic == q.drill.topic
        b.feedback = topic
        b.total += 1
        if (ok) {
            b.score += 10 * b.combo
            b.combo = minOf(Blitz.MAX_COMBO, b.combo + 1)
            b.right += 1
        } else {
            b.combo = 1
            b.timeLeft = (b.timeLeft - Blitz.PENALTY_MS).coerceAtLeast(0)
            b.penalties += 1
            b.mistakes += Mistake(q.drill.topic, q.drill.brief, picked = topic)
        }
        val day = today()
        update { it.record(q.drill.topic, ok, day).finish(ok, day) }
        scope.launch {
            delay(if (ok) 450 else 1100)
            if (blitz === b && !b.finished) {
                b.feedback = null
                b.q = nextQuestion(b)
            }
        }
    }

    /** Pauses the clock while the blitz screen isn't visible. */
    fun setBlitzVisible(visible: Boolean) {
        blitz?.paused = !visible
    }

    fun quitBlitz() {
        val b = blitz ?: return
        if (b.total > 0) finishBlitz(b) else stopBlitz()
    }

    private fun stopBlitz() {
        blitzJob?.cancel()
        blitzJob = null
        blitz = null
    }

    private fun finishBlitz(b: Blitz) {
        if (b.finished) return
        b.finished = true
        val before = progress().blitzBest
        val day = today()
        update { it.blitz(b.score, day) }
        summary = Summary(
            kind = SummaryKind.Blitz,
            title = "Блиц",
            filter = b.filter,
            points = b.score,
            max = 0,
            right = b.right,
            total = b.total,
            mistakes = b.mistakes.toList(),
            strong = emptyList(),
            streak = progress().streakOn(day),
            blitzBest = maxOf(before, b.score),
            newRecord = b.score > before && b.score > 0,
        )
        stopBlitz()
    }

    companion object {
        const val FRESH_SIZE = 5
        private const val TICK_MS = 100L
    }
}
