package app.ergo

import app.ergo.data.Bank
import app.ergo.data.ChatMessage
import app.ergo.data.ChatResult
import app.ergo.data.Drill
import app.ergo.data.OpenRouter
import app.ergo.data.Quiz
import app.ergo.data.SYS_DRILL
import app.ergo.data.SYS_EXPLAIN
import app.ergo.data.SYS_QUIZ
import app.ergo.data.SYS_STRUCT
import app.ergo.data.StructureItem
import app.ergo.data.Topic
import app.ergo.data.fmtCost
import app.ergo.data.parseModelJson
import app.ergo.data.str
import app.ergo.data.stringList
import org.json.JSONException
import org.json.JSONObject
import kotlin.random.Random

/** The model reply was unusable: empty, cut off, or not the JSON we asked for. */
class EmptyReply(message: String) : Exception(message)

/** OpenRouter calls with session cost accounting. Key, model and settings are read live. */
class Ai(
    private val key: () -> String,
    private val model: () -> String,
    private val showCost: () -> Boolean,
    private val onUsage: (Double?) -> Unit,
) {
    val connected: Boolean get() = key().isNotEmpty()

    /** One call. Returns the result and a "model · cost" line for the UI. */
    suspend fun complete(messages: List<ChatMessage>, max: Int, json: Boolean): Pair<ChatResult, String> {
        val r = OpenRouter.chat(key(), model(), messages, max, json)
        onUsage(r.cost)
        if (r.text.isBlank()) {
            throw EmptyReply(
                if (r.finishReason == "length") "модель потратила весь лимит токенов на рассуждения и ничего не ответила. Попробуйте другую модель."
                else "модель вернула пустой ответ."
            )
        }
        val meta = (r.model ?: model()) + if (showCost() && r.cost != null) " · " + fmtCost(r.cost) else ""
        return r to meta
    }

    suspend fun chat(messages: List<ChatMessage>, max: Int): Pair<String, String> =
        complete(messages, max, json = false).let { (r, meta) -> r.text to meta }

    /**
     * For structured tasks: parses and checks the reply with [read], retrying once on an
     * empty, broken, truncated or malformed reply.
     */
    suspend fun <T> chatJson(messages: List<ChatMessage>, max: Int, read: (JSONObject) -> T): Pair<T, String> {
        var last: Exception? = null
        repeat(2) {
            try {
                val (r, meta) = complete(messages, max, json = true)
                try {
                    return read(parseModelJson(r.text)) to meta
                } catch (e: JSONException) {
                    last = EmptyReply(if (r.finishReason == "length") "ответ модели обрезан по лимиту токенов." else "модель вернула некорректный ответ.")
                }
            } catch (e: EmptyReply) {
                last = e
            }
        }
        throw last!!
    }

    suspend fun chatJson(messages: List<ChatMessage>, max: Int): Pair<JSONObject, String> = chatJson(messages, max) { it }

    // ── Exercise generators ────────────────────────────────────────────────

    suspend fun drill(bank: Bank, topic: Topic, rnd: Random): Drill {
        val distractors = bank.distractorsFor(topic.id, rnd).mapNotNull { bank.topic(it) }
        val user = "Ошибка: «${topic.label}» — ${topic.gist}.\n" +
            "Отвлекающие варианты (используй эти названия как ключи whyNot): " +
            distractors.joinToString(", ") { "«${it.label}»" } + "."
        val (d, meta) = chatJson(listOf(ChatMessage("system", SYS_DRILL), ChatMessage("user", user)), 4000) { o ->
            val sentences = o.stringList("sentences").filter { it.isNotBlank() }
            val flawed = o.optJSONArray("flawed")?.let { a -> (0 until a.length()).map { a.optInt(it, -1) } } ?: listOf(o.optInt("flawed", -1))
            if (sentences.size < 2 || flawed.isEmpty() || flawed.any { it !in sentences.indices }) throw JSONException("bad drill")
            val whyNot = o.optJSONObject("whyNot")?.let { w ->
                w.keys().asSequence().mapNotNull { k -> bank.topicByName(k.trim('«', '»', ' '))?.id?.let { it to w.str(k) } }
                    .filter { it.first != topic.id && it.second.isNotBlank() }.toMap()
            }.orEmpty()
            Drill(
                id = "ai-" + System.nanoTime(),
                topic = topic.id,
                source = o.str("source").ifEmpty { "Сгенерированный фрагмент" },
                sentences = sentences,
                flawed = flawed.distinct().sorted(),
                clean = false,
                brief = flawed.distinct().sorted().joinToString(" ") { sentences[it] },
                explanation = o.str("explanation").ifEmpty { "«${topic.label}» — это ${topic.gist}." },
                whyNot = whyNot,
                ai = true,
            )
        }
        return d.copy(meta = meta)
    }

    suspend fun structure(): StructureItem {
        val (s, meta) = chatJson(listOf(ChatMessage("system", SYS_STRUCT), ChatMessage("user", "Новое упражнение на разбор аргумента.")), 3000) { o ->
            val sentences = o.stringList("sentences").filter { it.isNotBlank() }
            val concl = o.optInt("conclusion", -1)
            val premises = o.optJSONArray("premises")?.let { a -> (0 until a.length()).map { a.optInt(it, -1) } }.orEmpty()
                .filter { it in sentences.indices && it != concl }.distinct()
            val wrong = o.stringList("wrong").filter { it.isNotBlank() }.take(2)
            val assumption = o.str("assumption")
            if (sentences.size < 2 || concl !in sentences.indices || premises.isEmpty() || wrong.size < 2 || assumption.isBlank()) throw JSONException("bad structure")
            StructureItem(
                id = "ai-" + System.nanoTime(),
                source = o.str("source").ifEmpty { "Сгенерированный аргумент" },
                sentences = sentences,
                conclusion = concl,
                premises = premises,
                assumption = assumption,
                wrong = wrong,
                explanation = o.str("explanation"),
                ai = true,
            )
        }
        return s.copy(meta = meta)
    }

    suspend fun quiz(topic: Topic): Quiz {
        val (q, meta) = chatJson(listOf(ChatMessage("system", SYS_QUIZ), ChatMessage("user", "Новая задача.")), 3000) { o ->
            val options = o.stringList("options").filter { it.isNotBlank() }
            val answer = o.optInt("answer", -1)
            if (options.size < 2 || answer !in options.indices || o.str("passage").isBlank()) throw JSONException("bad quiz")
            Quiz(
                id = "ai-" + System.nanoTime(),
                topic = topic.id,
                source = o.str("source").ifEmpty { "Логическая задача" },
                passage = o.str("passage"),
                question = o.str("question").ifEmpty { "Следует ли вывод из посылок?" },
                options = options,
                answer = answer,
                explanation = o.str("explanation"),
                ai = true,
            )
        }
        return q.copy(meta = meta)
    }

    /** Why the learner's pick is wrong, in the tutor's voice. */
    suspend fun explain(d: Drill, answer: String, picked: String): Pair<String, String> = chat(
        listOf(
            ChatMessage("system", SYS_EXPLAIN),
            ChatMessage(
                "user",
                "Фрагмент: «${d.sentences.joinToString(" ")}»\n" +
                    "Ошибочная часть: «${d.flawed.mapNotNull { d.sentences.getOrNull(it) }.joinToString(" ")}»\n" +
                    "Правильный ответ: $answer\n" +
                    "Ученик выбрал: $picked\n" +
                    "Объясни, почему «$picked» не подходит и что выдаёт «$answer». Закончи одним коротким вопросом, который проверяет разницу.",
            ),
        ),
        1500,
    ).let { (t, meta) -> t.trim() to meta }
}
