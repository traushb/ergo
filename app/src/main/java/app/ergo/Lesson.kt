package app.ergo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.ergo.data.Accent
import app.ergo.data.Bank
import app.ergo.data.CheckOption
import app.ergo.data.Drill
import app.ergo.data.ExampleLine
import app.ergo.data.LessonCheck
import app.ergo.data.LessonExample
import app.ergo.data.Topic
import app.ergo.data.sentenceRanges
import kotlin.random.Random

/**
 * A five-step lesson: definition, how it works, an example to mark up, a check
 * question, done. Hand-written examples and checks come from the bank; the rest
 * are assembled from the topic's drills.
 */
class LessonRun(val topic: Topic, val example: LessonExample, val check: LessonCheck, val minutes: Int) {
    var step by mutableIntStateOf(0)
    var marked by mutableStateOf(false)
    var answer by mutableStateOf<Int?>(null)

    val passed: Boolean get() = answer?.let { check.options.getOrNull(it)?.ok } == true

    companion object {
        const val STEPS = 5
    }
}

fun lessonMinutes(bank: Bank, topic: Topic): Int = 3 + bank.lessonOrder.indexOf(topic).coerceAtLeast(0) % 3

fun buildLesson(bank: Bank, topic: Topic, rnd: Random = Random.Default): LessonRun {
    val drills = bank.drillsFor(topic.id)
    val example = topic.example ?: drills.firstOrNull()?.let { exampleFrom(topic, it) } ?: LessonExample(
        label = "ПРИМЕР",
        accent = Accent.Red,
        prompt = "",
        lines = listOf(ExampleLine(null, null, topic.tell, emptyList())),
        noteTitle = topic.label,
        note = topic.definition,
    )
    val check = topic.check ?: derivedCheck(bank, topic, drills.drop(1).ifEmpty { drills }, rnd)
    return LessonRun(topic, example, check, lessonMinutes(bank, topic))
}

private fun exampleFrom(topic: Topic, d: Drill): LessonExample {
    val ranges = sentenceRanges(d.sentences)
    return LessonExample(
        label = "ПРИМЕР · " + d.source.uppercase(),
        accent = Accent.Red,
        prompt = "Где здесь «${topic.label}»? Нажмите «Отметить».",
        lines = listOf(ExampleLine(null, null, d.sentences.joinToString(" "), d.flawed.map { ranges[it] })),
        noteTitle = topic.label,
        note = d.explanation,
    )
}

/** "Which snippet is X?": one of the topic's drills against two from its neighbours. */
private fun derivedCheck(bank: Bank, topic: Topic, own: List<Drill>, rnd: Random): LessonCheck {
    val right = own.shuffled(rnd).firstOrNull()
    val mates = bank.topicsIn(bank.unitIndexOf(topic.id)).filter { it.drillable && it.id != topic.id }.shuffled(rnd)
    val rest = bank.drillTopics.filter { it.id != topic.id && it !in mates }.shuffled(rnd)
    val others = (mates + rest).mapNotNull { t -> bank.drillsFor(t.id).randomOrNull(rnd)?.let { t to it } }.take(2)
    val options = buildList {
        if (right != null) add(CheckOption(right.brief, true, "Да. " + right.explanation))
        others.forEach { (t, d) -> add(CheckOption(d.brief, false, "Нет, здесь скорее «${t.label}» — ${t.gist}.")) }
    }.shuffled(rnd)
    return LessonCheck("В каком фрагменте — «${topic.label}»?", options)
}
