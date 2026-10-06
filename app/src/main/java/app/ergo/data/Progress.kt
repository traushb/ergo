package app.ergo.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Leitner box for one topic. Box 0 means never practised; boxes 1–4 come back
 * after 1, 3, 7 and 21 days. A mistake drops the topic back to box 1.
 */
data class TopicStat(val box: Int = 0, val due: Long = 0, val right: Int = 0, val wrong: Int = 0)

/** Everything the app remembers about the learner. Days are epoch days (LocalDate.toEpochDay). */
data class Progress(
    val lessons: Set<String> = emptySet(),
    val stats: Map<String, TopicStat> = emptyMap(),
    val streak: Int = 0,
    val lastDay: Long = NEVER,
    val solved: Int = 0,
    val correct: Int = 0,
    val todayDay: Long = NEVER,
    val todayCount: Int = 0,
    val blitzBest: Int = 0,
) {
    fun stat(id: String): TopicStat = stats[id] ?: TopicStat()

    /** The streak as of [day]: it survives until the end of the next day. */
    fun streakOn(day: Long): Int = if (lastDay == day || lastDay == day - 1) streak else 0

    fun solvedOn(day: Long): Int = if (todayDay == day) todayCount else 0

    /** Topics whose review is due, oldest first. */
    fun dueOn(day: Long): List<String> =
        stats.entries.filter { it.value.box > 0 && it.value.due <= day }.sortedBy { it.value.due }.map { it.key }

    /** The soonest future review, for "next review in N days". */
    fun nextDueAfter(day: Long): Long? = stats.values.filter { it.box > 0 && it.due > day }.minOfOrNull { it.due }

    val accuracy: Int? get() = if (solved == 0) null else (correct * 100 + solved / 2) / solved

    /** Topics with the most mistakes relative to successes. */
    fun weakSpots(n: Int = 3): List<String> = stats.entries
        .filter { it.value.wrong > 0 && it.value.wrong * 2 >= it.value.right }
        .sortedWith(compareByDescending<Map.Entry<String, TopicStat>> { it.value.wrong * 2 - it.value.right }.thenByDescending { it.value.wrong })
        .take(n)
        .map { it.key }

    /** Moves a topic between Leitner boxes after an answer. */
    fun record(topicId: String, ok: Boolean, day: Long): Progress {
        val s = stat(topicId)
        val box = if (ok) minOf(MAX_BOX, s.box + 1) else 1
        val next = s.copy(
            box = box,
            due = day + INTERVALS[box],
            right = s.right + if (ok) 1 else 0,
            wrong = s.wrong + if (ok) 0 else 1,
        )
        return copy(stats = stats + (topicId to next))
    }

    /** Counts one finished exercise towards totals, today's count and the streak. */
    fun finish(ok: Boolean, day: Long): Progress = touch(day).let {
        it.copy(
            solved = it.solved + 1,
            correct = it.correct + if (ok) 1 else 0,
            todayCount = (if (it.todayDay == day) it.todayCount else 0) + 1,
            todayDay = day,
        )
    }

    /** A finished lesson puts its topic into the review deck: it comes back tomorrow. */
    fun lessonDone(topicId: String, day: Long): Progress {
        val s = stat(topicId)
        val st = if (s.box == 0) s.copy(box = 1, due = day + 1) else s
        return touch(day).copy(lessons = lessons + topicId, stats = stats + (topicId to st))
    }

    fun blitz(score: Int, day: Long): Progress = touch(day).copy(blitzBest = maxOf(blitzBest, score))

    private fun touch(day: Long): Progress = when (lastDay) {
        day -> this
        day - 1 -> copy(streak = streak + 1, lastDay = day)
        else -> copy(streak = 1, lastDay = day)
    }

    fun toJson(): String = JSONObject()
        .put("v", 1)
        .put("lessons", JSONArray(lessons.toList()))
        .put("stats", JSONObject().also { o -> stats.forEach { (k, s) -> o.put(k, JSONArray(listOf(s.box, s.due, s.right, s.wrong))) } })
        .put("streak", streak)
        .put("lastDay", lastDay)
        .put("solved", solved)
        .put("correct", correct)
        .put("todayDay", todayDay)
        .put("todayCount", todayCount)
        .put("blitzBest", blitzBest)
        .toString()

    companion object {
        const val NEVER = -1_000_000L
        const val MAX_BOX = 4
        val INTERVALS = intArrayOf(0, 1, 3, 7, 21)

        /** Never throws: unreadable progress starts fresh rather than crashing the app. */
        fun fromJson(s: String?): Progress {
            if (s.isNullOrBlank()) return Progress()
            return runCatching {
                val o = JSONObject(s)
                val lessons = o.optJSONArray("lessons")?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }.orEmpty()
                val stats = o.optJSONObject("stats")?.let { so ->
                    so.keys().asSequence().associateWith { k ->
                        val a = so.getJSONArray(k)
                        TopicStat(a.getInt(0).coerceIn(0, MAX_BOX), a.getLong(1), a.getInt(2), a.getInt(3))
                    }
                }.orEmpty()
                Progress(
                    lessons = lessons,
                    stats = stats,
                    streak = o.optInt("streak"),
                    lastDay = o.optLong("lastDay", NEVER),
                    solved = o.optInt("solved"),
                    correct = o.optInt("correct"),
                    todayDay = o.optLong("todayDay", NEVER),
                    todayCount = o.optInt("todayCount"),
                    blitzBest = o.optInt("blitzBest"),
                )
            }.getOrDefault(Progress())
        }
    }
}
