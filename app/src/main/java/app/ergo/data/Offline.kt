package app.ergo.data

// What the app can do without a model: spot typical cue phrases, sketch an argument's
// structure, and keep a debate going with prepared replies.

private class Cue(val topic: String, pattern: String, val note: String) {
    val regex = Regex(pattern, RegexOption.IGNORE_CASE)
}

/** In priority order: specific phrasings first, the broad quantifier check last. */
private val CUES = listOf(
    Cue("straw_man", "то есть (?:вы|ты) (?:хоти|счита|говори|предлага)[^.?!]*", "Это пересказ чужой позиции в более резкой форме, чем она была высказана."),
    Cue("tu_quoque", "(?:^|[\\s«(—-])(?:а сам(?:и|а)?(?:-то)?|на себя посмотри|кто бы говорил)(?=[\\s,.!?»]|$)[^.?!]*", "Непоследовательность собеседника не опровергает его довод."),
    Cue("ad_hominem", "(?:^|[\\s«(—-])(?:типичн\\S*|(?:вы|ты) просто|что (?:ещё|еще) (?:ждать|ожидать) от)[^.?!]*", "Удар по человеку, а не по аргументу."),
    Cue("bandwagon", "(?:все так делают|большинство (?:людей )?(?:считает|думает|знает|выбирает)|миллион\\S* (?:людей|человек|семей|покупателей)|все знают|все понимают)[^.?!]*", "Популярность — не довод. Почему они так решили?"),
    Cue("authority", "(?:учёные|ученые|эксперты|врачи|специалисты|профессор\\S*) (?:доказали|говорят|считают|утверждают|рекомендуют)[^.?!]*", "Кто именно и на что он опирается? Ссылка на статус — ещё не доказательство."),
    Cue("nature", "(?:неестественн\\S*|естественн\\S*|натуральн\\S*|никакой химии|задумано природой)[^.?!]*", "«Естественное» не значит «полезное». Что известно о пользе и вреде?"),
    Cue("false_dilemma", "(?:(?:либо|или)[^.?!]*(?:либо|или)|третьего не дано)[^.?!]*", "Точно ли вариантов только два?"),
    Cue("slippery", "(?:если (?:мы|это|их|им|сейчас)[^.?!]*(?:скоро|рано или поздно|в итоге|а там|а потом|завтра)|сегодня[^.?!]*,? а завтра)[^.?!]*", "Каждое звено этой цепочки нуждается в собственном обосновании."),
    Cue("emotion", "(?:подумайте о детях|неужели (?:вам|тебе|у вас|у тебя)[^.?!]*(?:не жалко|нет сердца|готовы рисковать)|только представьте)[^.?!]*", "Сильное чувство вместо довода. Какие факты говорят за это решение?"),
    Cue("weasel", "(?:по некоторым данным|есть мнение|многие (?:эксперты|специалисты|считают)|до \\d+[,.]?\\d*\\s?%)[^.?!]*", "Расплывчато: кто именно, сколько именно? Проверить это нельзя."),
    Cue("post_hoc", "(?:с тех пор как|после того как)[^.?!]*", "Совпадение по времени — ещё не причина. Что ещё изменилось за это время?"),
    Cue("hasty", "(?:^|[\\s«(—-])(?:все|всегда|никогда|каждый|никто)(?=[\\s,.!?»]|$)[^.?!]*", "Слишком широкий квантор. Можно ли защитить «все», или хватит «многие»?"),
)

data class CueHit(val topic: String, val start: Int, val end: Int, val quote: String, val note: String)

/** Non-overlapping cue matches in reading order, at most [max]. */
fun findCues(text: String, max: Int = 4): List<CueHit> {
    val hits = mutableListOf<CueHit>()
    for (cue in CUES) {
        for (m in cue.regex.findAll(text)) {
            val raw = m.value
            val lead = raw.length - raw.trimStart().length
            val quote = raw.trim().trimStart('«', '(', '—', '-').trim()
            if (quote.length < 3) continue
            val start = m.range.first + lead + raw.trimStart().indexOf(quote)
            val end = start + quote.length
            if (hits.any { start < it.end && end > it.start }) continue
            if (hits.any { it.topic == cue.topic }) continue
            hits += CueHit(cue.topic, start, end, quote, cue.note)
        }
    }
    return hits.sortedBy { it.start }.take(max)
}

/** Offline stand-in for the model's fallacy check in Spar. */
fun demoFlag(text: String, bank: Bank? = null): Flag? {
    for (cue in CUES) {
        val m = cue.regex.find(text) ?: continue
        val quote = m.value.trim().trimStart('«', '(', '—', '-').trim().take(90)
        if (quote.length < 3) continue
        val name = bank?.topic(cue.topic)?.label ?: cue.topic
        return Flag(name, quote, cue.note, cue.topic)
    }
    return null
}

/** The opponent's next line in an offline debate; calls out a spotted fallacy first. */
fun offlineReply(bank: Bank, motion: Motion, turn: Int, flag: Flag?): String {
    val base = motion.replies[turn % motion.replies.size]
    val call = flag?.topic?.let { bank.callouts[it] }
    return if (call != null) "$call $base" else base
}

private val STRONG_MARKERS = Regex("(?:^|[\\s«(—-])(?:значит|поэтому|следовательно|так что|итак|стало быть|выходит)(?=[\\s,.!?:»]|$)", RegexOption.IGNORE_CASE)
private val WEAK_MARKERS = Regex("(?:^|[\\s«(—-])(?:нужно|надо|должн\\S*|необходимо|пора|стоит|следует)(?=[\\s,.!?:»]|$)", RegexOption.IGNORE_CASE)

fun splitSentences(text: String): List<String> =
    text.trim().split(Regex("(?<=[.!?…])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }

/**
 * A rough offline reading: the conclusion is the sentence with an inference marker
 * ("значит", "поэтому"…), the rest are premises, and issues are cue-phrase hits.
 */
fun offlineAnalysis(bank: Bank, text: String): Analysis {
    val sentences = splitSentences(text)
    val concl = sentences.indexOfFirst { STRONG_MARKERS.containsMatchIn(it) }
        .takeIf { it >= 0 } ?: sentences.indexOfFirst { WEAK_MARKERS.containsMatchIn(it) }.takeIf { it >= 0 } ?: 0
    val hits = findCues(text)
    val issues = hits.map { h ->
        Issue("Возможно: " + (bank.topic(h.topic)?.label ?: h.topic), h.quote, h.note, h.topic)
    }
    val summary = when (issues.size) {
        0 -> "Явных примет типичных ошибок не нашлось. Это ещё не значит, что аргумент сильный: проверьте посылки и спросите себя, что автор не договаривает."
        else -> "Нашлись приметы возможных ошибок. Это подсказки по ключевым словам, а не приговор: проверьте каждую сами."
    }
    return Analysis(
        conclusion = sentences.getOrElse(concl) { text.trim() },
        premises = sentences.filterIndexed { i, _ -> i != concl }.take(4),
        assumptions = emptyList(),
        issues = issues,
        verdict = if (issues.size >= 2) "Weak" else "Moderate",
        summary = summary,
        heuristic = true,
    )
}
