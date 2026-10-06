package app.ergo

import app.ergo.data.Progress
import app.ergo.data.TestBank
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeTest {
    private val bank = TestBank.bank
    private val day = 20_000L
    private var progress = Progress()
    private val toasts = mutableListOf<String>()
    private val offline = Ai(key = { "" }, model = { "test" }, showCost = { false }, onUsage = {})

    private fun TestScope.controller() = PracticeController(
        bank, offline, backgroundScope, { progress }, { f -> progress = f(progress) }, { toasts += it }, { day }, Random(7),
    )

    @Test fun topicSessionCentresOnItsTopic() = runTest {
        val c = controller()
        val ex = c.buildExercises(TopicFilter.One("straw_man"))
        assertEquals(6, ex.size)
        val own = ex.filterIsInstance<FallacyEx>().count { it.drill.topic == "straw_man" }
        assertEquals(bank.drillsFor("straw_man").size, own)
        val unit = bank.unitOf("straw_man").topicIds
        assertTrue(ex.filterIsInstance<FallacyEx>().all { it.drill.topic in unit })
    }

    @Test fun everyFilterBuildsASession() = runTest {
        val c = controller()
        assertTrue(c.buildExercises(TopicFilter.All).size >= 7)
        bank.units.forEach { u -> assertTrue(u.title, c.buildExercises(TopicFilter.Section(u.index)).size >= 4) }
        bank.topics.forEach { t -> assertTrue(t.id, c.buildExercises(TopicFilter.One(t.id)).isNotEmpty()) }
    }

    @Test fun fallacyFlowScoresAndRecords() = runTest {
        val c = controller()
        c.startPractice(TopicFilter.One("false_dilemma"))
        val s = c.session!!
        // Walk to the first find-and-name exercise with a flaw.
        while (!(s.run is FallacyRun && !(s.run as FallacyRun).drill.clean)) c.next()
        val r = s.run as FallacyRun
        val miss = r.drill.sentences.indices.first { it !in r.drill.flawed }
        c.tap(miss)
        assertEquals(listOf(miss), r.misses)
        assertEquals(FPhase.Spot, r.phase)
        c.tap(r.drill.flawed.first())
        assertEquals(FPhase.Name, r.phase)
        c.choose(r.drill.topic)
        assertTrue(r.resolved && r.correct)
        // The miss cost the spotting point; naming earned two.
        assertEquals(2, s.outcomes.last().points)
        assertEquals(1, progress.stat(r.drill.topic).box)
        assertEquals(1, progress.solved)
    }

    @Test fun wrongNameIsAMistakeWithAnExplanation() = runTest {
        val c = controller()
        c.startPractice(TopicFilter.One("slippery"))
        val s = c.session!!
        while (!(s.run is FallacyRun && !(s.run as FallacyRun).drill.clean)) c.next()
        val r = s.run as FallacyRun
        c.tap(r.drill.flawed.first())
        val wrong = r.options.first { it != r.drill.topic }
        c.choose(wrong)
        assertFalse(r.correct)
        assertEquals(1, s.outcomes.last().points)
        assertEquals(wrong, s.outcomes.last().mistake?.picked)
        c.explainWrong()
        assertTrue(r.explain.isNotBlank())
        assertEquals(1, progress.stat(r.drill.topic).wrong)
    }

    @Test fun cleanSnippetRewardsSayingThereIsNoError() = runTest {
        val c = controller()
        val clean = bank.drills.first { it.clean }
        val r = FallacyRun(clean, bank.optionsFor(clean))
        val o = r.noFlaw()!!
        assertEquals(3, o.points)
        assertTrue(o.correct)
        val r2 = FallacyRun(clean, bank.optionsFor(clean))
        r2.tap(0)
        val o2 = r2.noFlaw()!!
        assertEquals(1, o2.points)
        assertTrue(o2.mistake!!.falseAlarm)
        // Saying "no error" on a flawed snippet costs the spotting point but keeps the exercise going.
        val flawed = bank.drills.first { !it.clean }
        val r3 = FallacyRun(flawed, bank.optionsFor(flawed))
        assertNull(r3.noFlaw())
        r3.tap(flawed.flawed.first())
        assertEquals(2, r3.choose(flawed.topic)!!.points)
    }

    @Test fun structureFlowRecordsAllThreeSkills() = runTest {
        val item = bank.structure.first()
        val r = StructureRun(item, (item.wrong + item.assumption).shuffled(Random(3)))
        r.tap(item.premises.first())
        assertEquals(SPhase.Conclusion, r.phase)
        r.tap(item.conclusion)
        assertEquals(SPhase.Premise, r.phase)
        r.tap(item.conclusion)
        assertTrue(r.tappedConclusion)
        r.tap(item.premises.first())
        assertEquals(SPhase.Assumption, r.phase)
        val o = r.choose(r.answer)!!
        assertEquals(2, o.points)
        assertEquals(listOf("conclusion" to false, "premises" to true, "assumptions" to true), o.records)
        assertEquals("conclusion", o.mistake?.topic)
    }

    @Test fun sessionEndsInASummary() = runTest {
        val c = controller()
        c.startPractice(TopicFilter.Section(1))
        val s = c.session!!
        repeat(s.total) {
            when (val r = s.run) {
                is FallacyRun -> if (r.drill.clean) c.noFlaw() else {
                    c.tap(r.drill.flawed.first())
                    c.choose(r.drill.topic)
                }
                is QuizRun -> c.chooseOption(r.quiz.answer)
                is StructureRun -> {
                    c.tap(r.item.conclusion)
                    c.tap(r.item.premises.first())
                    c.chooseOption(r.answer)
                }
                LoadingRun -> error("unexpected loading")
            }
            c.next()
        }
        assertNull(c.session)
        val sum = c.summary!!
        assertEquals(s.total, sum.total)
        assertEquals(sum.total, sum.right)
        assertEquals(sum.max, sum.points)
        assertTrue(sum.mistakes.isEmpty())
        assertEquals(1, sum.streak)
        assertEquals(s.total, progress.solvedOn(day))
    }

    @Test fun quittingEarlyKeepsWhatWasDone() = runTest {
        val c = controller()
        c.startPractice(TopicFilter.All)
        c.quit()
        assertNull(c.summary)
        c.startPractice(TopicFilter.All)
        val s = c.session!!
        when (val r = s.run) {
            is FallacyRun -> if (r.drill.clean) c.noFlaw() else { c.tap(r.drill.flawed.first()); c.choose(r.drill.topic) }
            is QuizRun -> c.chooseOption(r.quiz.answer)
            is StructureRun -> { c.tap(r.item.conclusion); c.tap(r.item.premises.first()); c.chooseOption(r.answer) }
            LoadingRun -> {}
        }
        c.quit()
        assertEquals(1, c.summary!!.total)
    }

    @Test fun reviewUsesDueTopics() = runTest {
        progress = Progress().record("hasty", false, day - 1).record("conclusion", false, day - 1)
        val c = controller()
        c.startReview()
        val s = c.session!!
        assertEquals(2, s.total)
        progress = Progress()
        val c2 = controller()
        c2.startReview()
        assertNull(c2.session)
        assertTrue(toasts.last().contains("повторять нечего"))
    }

    @Test fun freshSessionNeedsAKey() = runTest {
        val c = controller()
        c.startFresh(TopicFilter.All)
        assertNull(c.session)
        assertTrue(toasts.last().contains("ключ"))
    }

    @Test fun blitzScoresCombosAndEndsOnTime() = runTest {
        val c = controller()
        c.startBlitz(TopicFilter.All)
        val b = c.blitz!!
        val q1 = b.q!!
        c.blitzPick(q1.drill.topic)
        assertEquals(10, b.score)
        assertEquals(2, b.combo)
        advanceTimeBy(500)
        runCurrent()
        val q2 = b.q!!
        assertNull(b.feedback)
        c.blitzPick(q2.drill.topic)
        assertEquals(30, b.score)
        advanceTimeBy(500)
        runCurrent()
        val q3 = b.q!!
        c.blitzPick(q3.options.first { it != q3.drill.topic })
        assertEquals(1, b.combo)
        assertEquals(1, b.mistakes.size)
        advanceTimeBy(Blitz.DURATION_MS + 5_000)
        runCurrent()
        assertNull(c.blitz)
        val sum = c.summary!!
        assertEquals(SummaryKind.Blitz, sum.kind)
        assertEquals(30, sum.points)
        assertEquals(3, sum.total)
        assertTrue(sum.newRecord)
        assertEquals(30, progress.blitzBest)
    }

    @Test fun blitzPausesWhileHidden() = runTest {
        val c = controller()
        c.startBlitz(TopicFilter.All)
        val b = c.blitz!!
        c.setBlitzVisible(false)
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(Blitz.DURATION_MS, b.timeLeft)
        c.setBlitzVisible(true)
        advanceTimeBy(1_050)
        runCurrent()
        assertTrue(b.timeLeft in 58_900L..59_100L)
        c.quitBlitz()
        assertNull(c.blitz)
        assertNull(c.summary)
    }

    @Test fun lessonsBuildForEveryTopic() {
        for (t in bank.topics) {
            val l = buildLesson(bank, t, Random(5))
            assertTrue(t.id, l.check.options.size >= 2)
            assertEquals(t.id, 1, l.check.options.count { it.ok })
            assertTrue(t.id, l.example.lines.isNotEmpty())
            assertNotNull(l.example.lines.firstOrNull { it.marks.isNotEmpty() } ?: l.example.lines.first())
            assertFalse(l.passed)
            l.answer = l.check.options.indexOfFirst { it.ok }
            assertTrue(l.passed)
        }
    }
}
