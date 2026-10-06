package app.ergo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Integrity of the offline content: every reference resolves and every topic is playable. */
class BankTest {
    private val bank = TestBank.bank

    @Test fun syllabusCoversEveryTopicOnce() {
        val inUnits = bank.units.flatMap { it.topicIds }
        assertEquals(inUnits.size, inUnits.toSet().size)
        assertEquals(bank.topics.map { it.id }.toSet(), inUnits.toSet())
        assertEquals(26, bank.lessonOrder.size)
    }

    @Test fun topicsAreComplete() {
        for (t in bank.topics) {
            assertEquals(t.id, 3, t.steps.size)
            listOf(t.name, t.label, t.gist, t.tagline, t.formula, t.definition, t.tell, t.counter).forEach { assertTrue(t.id, it.isNotBlank()) }
        }
    }

    @Test fun everyDrillTopicHasMaterial() {
        for (t in bank.drillTopics) {
            assertTrue("${t.id} needs drills", bank.drillsFor(t.id).size >= 2)
        }
        assertTrue(bank.structure.size >= 4)
        assertTrue(bank.quizzesFor("validity").size >= 4)
    }

    @Test fun drillsAreWellFormed() {
        val ids = mutableSetOf<String>()
        for (d in bank.drills) {
            assertTrue("duplicate ${d.id}", ids.add(d.id))
            assertNotNull(d.id, bank.topic(d.topic))
            assertTrue(d.id, d.sentences.size >= 2)
            if (d.clean) {
                assertTrue(d.id, d.flawed.isEmpty())
            } else {
                assertTrue(d.id, d.flawed.isNotEmpty() && d.flawed.all { it in d.sentences.indices })
                assertTrue(d.id, d.brief.isNotBlank())
            }
            d.whyNot.keys.forEach { assertTrue("${d.id}: $it", bank.topic(it)?.drillable == true && it != d.topic) }
        }
    }

    @Test fun optionsHoldTheAnswerAndThreeDistinctDrillTopics() {
        val rnd = Random(1)
        for (d in bank.drills.filter { !it.clean }) {
            val opts = bank.optionsFor(d, rnd)
            assertEquals(d.id, 4, opts.toSet().size)
            assertTrue(d.id, d.topic in opts)
            assertTrue(d.id, opts.all { bank.topic(it)?.drillable == true })
            // The drill's own "why not" distractors are always offered.
            assertTrue(d.id, d.whyNot.keys.take(3).all { it in opts })
            opts.filter { it != d.topic }.forEach { assertTrue(bank.whyNot(d, it).isNotBlank()) }
        }
    }

    @Test fun structureAndQuizzesAreConsistent() {
        for (s in bank.structure) {
            assertTrue(s.id, s.conclusion in s.sentences.indices)
            assertTrue(s.id, s.premises.isNotEmpty() && s.premises.all { it in s.sentences.indices && it != s.conclusion })
            assertEquals(s.id, 2, s.wrong.size)
        }
        for (q in bank.quizzes) {
            assertNotNull(q.id, bank.topic(q.topic))
            assertTrue(q.id, q.answer in q.options.indices)
        }
    }

    @Test fun lessonExamplesMarkRealText() {
        for (t in bank.topics) {
            t.example?.lines?.forEach { l -> l.marks.forEach { assertTrue(t.id, it.first >= 0 && it.last < l.text.length) } }
            t.check?.let { c -> assertEquals(t.id, 1, c.options.count { it.ok }) }
        }
    }

    @Test fun samplesQuoteTheirOwnText() {
        for (s in bank.samples) {
            s.result.issues.forEach { assertTrue("${s.label}: ${it.quote}", it.quote in s.text) }
            s.result.issues.mapNotNull { it.topic }.forEach { assertNotNull(bank.topic(it)) }
        }
    }

    @Test fun motionsHaveRepliesAndCalloutsResolve() {
        assertTrue(bank.motions.size >= 6)
        bank.motions.forEach { assertTrue(it.id, it.replies.size >= 4) }
        bank.callouts.keys.forEach { assertNotNull(it, bank.topic(it)) }
    }

    @Test fun topicByNameMatchesLabelsAndModelNames() {
        assertEquals("post_hoc", bank.topicByName("Ложная причина")?.id)
        assertEquals("post_hoc", bank.topicByName("корреляция и причинность")?.id)
        assertEquals("straw_man", bank.topicByName("Соломенное чучело (подмена тезиса)")?.id)
        assertEquals(null, bank.topicByName(""))
    }

    @Test fun offlineAnalysisFindsCuesAndConclusion() {
        val text = "Все мои друзья перешли на этот банк. Либо вы с нами, либо остаётесь в прошлом веке. Значит, пора менять банк."
        val a = offlineAnalysis(bank, text)
        assertTrue(a.heuristic)
        assertEquals("Значит, пора менять банк.", a.conclusion)
        assertEquals(2, a.premises.size)
        val topics = a.issues.mapNotNull { it.topic }
        assertTrue(topics.toString(), "false_dilemma" in topics && "hasty" in topics)
        a.issues.forEach { assertTrue(it.quote, it.quote in text) }
    }

    @Test fun offlineAnalysisOfCleanTextHasNoIssues() {
        val a = offlineAnalysis(bank, "Опрос охватил двенадцать тысяч человек. Выборка подобрана по возрасту и доходу. Поэтому результатам можно доверять.")
        assertTrue(a.issues.isEmpty())
        assertEquals("Поэтому результатам можно доверять.", a.conclusion)
    }

    @Test fun offlineReplyCallsOutFlags() {
        val m = bank.motions.first()
        val flag = demoFlag("Либо запрещаем, либо дети страдают.", bank)
        val reply = offlineReply(bank, m, 0, flag)
        assertTrue(reply.startsWith(bank.callouts.getValue("false_dilemma")))
        assertFalse(offlineReply(bank, m, 1, null).startsWith(bank.callouts.getValue("false_dilemma")))
    }

    @Test fun sentenceRangesFollowJoinedText() {
        val s = listOf("Один.", "Два три.", "Четыре.")
        val joined = s.joinToString(" ")
        sentenceRanges(s).forEachIndexed { i, r -> assertEquals(s[i], joined.substring(r.first, r.last + 1)) }
    }
}
