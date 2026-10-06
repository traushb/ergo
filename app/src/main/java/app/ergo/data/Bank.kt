package app.ergo.data

import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

// The offline content bank, loaded from assets/content/*.json.

enum class TopicKind { Structure, Logic, Fallacy, Bias, Rhetoric }

/** Red pencil marks flaws; blue marks argument structure. */
enum class Accent { Red, Blue }

data class ExampleLine(val who: String?, val whoAccent: Accent?, val text: String, val marks: List<IntRange>)

data class LessonExample(
    val label: String,
    val accent: Accent,
    val prompt: String,
    val lines: List<ExampleLine>,
    val noteTitle: String,
    val note: String,
)

data class CheckOption(val text: String, val ok: Boolean, val fb: String)

data class LessonCheck(val question: String, val options: List<CheckOption>)

data class Topic(
    val id: String,
    val name: String,
    /** The fallacy's name as an answer option; usually the same as the lesson title. */
    val label: String,
    val kind: TopicKind,
    /** A short noun phrase for generated sentences: "«X» — это <gist>". */
    val gist: String,
    val tagline: String,
    val formula: String,
    val definition: String,
    val steps: List<String>,
    val tell: String,
    val counter: String,
    val example: LessonExample?,
    val check: LessonCheck?,
) {
    /** Fallacies, biases and rhetoric are trained with find-and-name drills. */
    val drillable: Boolean get() = kind == TopicKind.Fallacy || kind == TopicKind.Bias || kind == TopicKind.Rhetoric
}

data class UnitInfo(val index: Int, val n: String, val title: String, val topicIds: List<String>)

data class Drill(
    val id: String,
    val topic: String,
    val source: String,
    val sentences: List<String>,
    val flawed: List<Int>,
    /** A clean lookalike: the right answer is "there is no error". */
    val clean: Boolean,
    val brief: String,
    val explanation: String,
    /** Topic id of a tempting wrong answer → why it doesn't fit. */
    val whyNot: Map<String, String>,
    val ai: Boolean = false,
    val meta: String? = null,
)

data class StructureItem(
    val id: String,
    val source: String,
    val sentences: List<String>,
    val conclusion: Int,
    val premises: List<Int>,
    val assumption: String,
    val wrong: List<String>,
    val explanation: String,
    val ai: Boolean = false,
    val meta: String? = null,
)

data class Quiz(
    val id: String,
    val topic: String,
    val source: String,
    val passage: String,
    val question: String,
    val options: List<String>,
    val answer: Int,
    val explanation: String,
    val ai: Boolean = false,
    val meta: String? = null,
)

data class Motion(val id: String, val motion: String, val opener: String, val replies: List<String>)

data class Sample(val label: String, val text: String, val result: Analysis)

class Bank(
    val units: List<UnitInfo>,
    val topics: List<Topic>,
    val drills: List<Drill>,
    val structure: List<StructureItem>,
    val quizzes: List<Quiz>,
    val motions: List<Motion>,
    val callouts: Map<String, String>,
    val samples: List<Sample>,
) {
    private val byId = topics.associateBy { it.id }
    private val unitOfTopic = units.flatMap { u -> u.topicIds.map { it to u.index } }.toMap()

    /** Lessons in syllabus order. */
    val lessonOrder: List<Topic> = units.flatMap { it.topicIds }.mapNotNull { byId[it] }
    val drillTopics: List<Topic> = lessonOrder.filter { it.drillable }

    fun topic(id: String?): Topic? = id?.let { byId[it] }
    fun unitIndexOf(topicId: String): Int = unitOfTopic[topicId] ?: 0
    fun unitOf(topicId: String): UnitInfo = units[unitIndexOf(topicId)]

    /** 1-based position of a lesson in its unit, for "урок 2 из 7". */
    fun lessonNumber(topicId: String): Int = unitOf(topicId).topicIds.indexOf(topicId) + 1

    fun topicsIn(unit: Int): List<Topic> = units[unit].topicIds.mapNotNull { byId[it] }
    fun drillsFor(topicId: String): List<Drill> = drills.filter { it.topic == topicId && !it.clean }
    fun cleanFor(topicId: String): List<Drill> = drills.filter { it.topic == topicId && it.clean }
    fun quizzesFor(topicId: String): List<Quiz> = quizzes.filter { it.topic == topicId }

    /** Matches a model's or a sample's free-text fallacy name to a topic. */
    fun topicByName(name: String): Topic? {
        val n = name.trim().lowercase()
        if (n.isEmpty()) return null
        return topics.firstOrNull { it.name.lowercase() == n || it.label.lowercase() == n }
            ?: topics.firstOrNull { n.contains(it.label.lowercase()) || n.contains(it.name.lowercase()) }
    }

    /** The answer plus three distractors: the drill's own "why not" topics, then unit-mates, then the rest. */
    fun optionsFor(d: Drill, rnd: Random = Random.Default): List<String> {
        val picked = LinkedHashSet<String>()
        d.whyNot.keys.filter { it != d.topic && byId[it]?.drillable == true }.forEach { picked += it }
        units[unitIndexOf(d.topic)].topicIds
            .filter { it != d.topic && byId[it]?.drillable == true }
            .shuffled(rnd)
            .forEach { picked += it }
        drillTopics.map { it.id }.filter { it != d.topic }.shuffled(rnd).forEach { picked += it }
        return (listOf(d.topic) + picked.take(3)).shuffled(rnd)
    }

    /** Distractor topics for a generated drill: unit-mates first, then the rest. */
    fun distractorsFor(topicId: String, rnd: Random = Random.Default): List<String> {
        val picked = LinkedHashSet<String>()
        units[unitIndexOf(topicId)].topicIds.filter { it != topicId && byId[it]?.drillable == true }.shuffled(rnd).forEach { picked += it }
        drillTopics.map { it.id }.filter { it != topicId }.shuffled(rnd).forEach { picked += it }
        return picked.take(3)
    }

    /** Why a picked option doesn't fit: the drill's own note, or one built from both topics' gists. */
    fun whyNot(d: Drill, picked: String): String {
        d.whyNot[picked]?.let { return it }
        val p = byId[picked] ?: return d.explanation
        val a = byId[d.topic] ?: return d.explanation
        return "«${p.label}» — это ${p.gist}. Здесь другое: ${a.gist}."
    }

    companion object {
        fun parse(topics: String, drills: String, structure: String, quizzes: String, motions: String, samples: String): Bank {
            val t = JSONObject(topics)
            val units = t.getJSONArray("units").objects().mapIndexed { i, u ->
                UnitInfo(i, u.getString("n"), u.getString("title"), u.getJSONArray("topics").strings())
            }
            val m = JSONObject(motions)
            return Bank(
                units = units,
                topics = t.getJSONArray("topics").objects().map(::parseTopic),
                drills = JSONObject(drills).getJSONArray("drills").objects().map(::parseDrill),
                structure = JSONObject(structure).getJSONArray("items").objects().map(::parseStructure),
                quizzes = JSONObject(quizzes).getJSONArray("quizzes").objects().map(::parseQuiz),
                motions = m.getJSONArray("motions").objects().map {
                    Motion(it.getString("id"), it.getString("motion"), it.getString("opener"), it.getJSONArray("replies").strings())
                },
                callouts = m.optJSONObject("callouts")?.let { c -> c.keys().asSequence().associateWith { c.getString(it) } }.orEmpty(),
                samples = JSONObject(samples).getJSONArray("samples").objects().map {
                    Sample(it.getString("label"), it.getString("text"), parseAnalysis(it.getJSONObject("result")))
                },
            )
        }

        private fun parseTopic(o: JSONObject): Topic {
            val name = o.getString("name")
            return Topic(
                id = o.getString("id"),
                name = name,
                label = o.optString("label").ifEmpty { name },
                kind = when (o.getString("kind")) {
                    "structure" -> TopicKind.Structure
                    "logic" -> TopicKind.Logic
                    "bias" -> TopicKind.Bias
                    "rhetoric" -> TopicKind.Rhetoric
                    else -> TopicKind.Fallacy
                },
                gist = o.getString("gist"),
                tagline = o.getString("tagline"),
                formula = o.getString("formula"),
                definition = o.getString("definition"),
                steps = o.getJSONArray("steps").strings(),
                tell = o.getString("tell"),
                counter = o.getString("counter"),
                example = o.optJSONObject("example")?.let(::parseExample),
                check = o.optJSONObject("check")?.let(::parseCheck),
            )
        }

        private fun accent(s: String?) = if (s == "blue") Accent.Blue else Accent.Red

        private fun parseExample(o: JSONObject) = LessonExample(
            label = o.getString("label"),
            accent = accent(o.optString("accent")),
            prompt = o.getString("prompt"),
            lines = o.getJSONArray("lines").objects().map { l ->
                val text = l.getString("text")
                ExampleLine(
                    who = l.optString("who").ifEmpty { null },
                    whoAccent = if (l.has("whoAccent")) accent(l.getString("whoAccent")) else null,
                    text = text,
                    marks = l.optJSONArray("marks")?.strings().orEmpty().mapNotNull { m ->
                        text.indexOf(m).takeIf { it >= 0 }?.let { it until it + m.length }
                    },
                )
            },
            noteTitle = o.getString("noteTitle"),
            note = o.getString("note"),
        )

        private fun parseCheck(o: JSONObject) = LessonCheck(
            question = o.getString("question"),
            options = o.getJSONArray("options").objects().map { CheckOption(it.getString("text"), it.getBoolean("ok"), it.getString("fb")) },
        )

        private fun parseDrill(o: JSONObject) = Drill(
            id = o.getString("id"),
            topic = o.getString("topic"),
            source = o.getString("source"),
            sentences = o.getJSONArray("sentences").strings(),
            flawed = o.optJSONArray("flawed")?.ints().orEmpty(),
            clean = o.optBoolean("clean", false),
            brief = o.optString("brief"),
            explanation = o.getString("explanation"),
            whyNot = o.optJSONObject("whyNot")?.let { w -> w.keys().asSequence().associateWith { w.getString(it) } }.orEmpty(),
        )

        private fun parseStructure(o: JSONObject) = StructureItem(
            id = o.getString("id"),
            source = o.getString("source"),
            sentences = o.getJSONArray("sentences").strings(),
            conclusion = o.getInt("conclusion"),
            premises = o.getJSONArray("premises").ints(),
            assumption = o.getString("assumption"),
            wrong = o.getJSONArray("wrong").strings(),
            explanation = o.getString("explanation"),
        )

        private fun parseQuiz(o: JSONObject) = Quiz(
            id = o.getString("id"),
            topic = o.getString("topic"),
            source = o.getString("source"),
            passage = o.getString("passage"),
            question = o.getString("question"),
            options = o.getJSONArray("options").strings(),
            answer = o.getInt("answer"),
            explanation = o.getString("explanation"),
        )

        private fun parseAnalysis(o: JSONObject) = Analysis(
            conclusion = o.getString("conclusion"),
            premises = o.getJSONArray("premises").strings(),
            assumptions = o.getJSONArray("assumptions").strings(),
            issues = o.getJSONArray("issues").objects().map {
                Issue(it.getString("name"), it.getString("quote"), it.getString("note"), it.optString("topic").ifEmpty { null })
            },
            verdict = o.getString("verdict"),
            summary = o.getString("summary"),
        )
    }
}

private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
private fun JSONArray.ints(): List<Int> = (0 until length()).map { getInt(it) }

/** Character ranges of the given sentences once joined with single spaces. */
fun sentenceRanges(sentences: List<String>): List<IntRange> {
    var at = 0
    return sentences.map { s -> (at until at + s.length).also { at += s.length + 1 } }
}
