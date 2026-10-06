package app.ergo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressTest {
    private val day = 20_000L

    @Test fun leitnerBoxesFollowOneThreeSevenTwentyOne() {
        var p = Progress()
        p = p.record("hasty", true, day)
        assertEquals(1, p.stat("hasty").box)
        assertEquals(day + 1, p.stat("hasty").due)
        p = p.record("hasty", true, day + 1)
        assertEquals(day + 1 + 3, p.stat("hasty").due)
        p = p.record("hasty", true, day + 4).record("hasty", true, day + 11)
        assertEquals(4, p.stat("hasty").box)
        assertEquals(day + 11 + 21, p.stat("hasty").due)
        p = p.record("hasty", true, day + 32)
        assertEquals(4, p.stat("hasty").box)
    }

    @Test fun mistakeDropsBackToTomorrow() {
        val p = Progress().record("loaded", true, day).record("loaded", true, day + 1).record("loaded", false, day + 4)
        assertEquals(1, p.stat("loaded").box)
        assertEquals(day + 5, p.stat("loaded").due)
        assertEquals(2, p.stat("loaded").right)
        assertEquals(1, p.stat("loaded").wrong)
    }

    @Test fun dueListsOnlyPractisedTopicsOnTime() {
        val p = Progress().record("a", true, day).record("b", false, day).record("c", true, day).record("c", true, day + 1)
        assertEquals(emptyList<String>(), p.dueOn(day))
        assertEquals(setOf("a", "b"), p.dueOn(day + 1).toSet())
        assertEquals(setOf("a", "b", "c"), p.dueOn(day + 4).toSet())
        assertEquals(day + 1, p.nextDueAfter(day))
    }

    @Test fun streakCountsConsecutiveDays() {
        var p = Progress().finish(true, day)
        assertEquals(1, p.streakOn(day))
        p = p.finish(true, day)
        assertEquals(1, p.streakOn(day))
        p = p.finish(false, day + 1)
        assertEquals(2, p.streakOn(day + 1))
        assertEquals(2, p.streakOn(day + 2))
        assertEquals(0, p.streakOn(day + 3))
        p = p.finish(true, day + 5)
        assertEquals(1, p.streakOn(day + 5))
    }

    @Test fun totalsAndToday() {
        val p = Progress().finish(true, day).finish(false, day).finish(true, day + 1)
        assertEquals(3, p.solved)
        assertEquals(2, p.correct)
        assertEquals(1, p.solvedOn(day + 1))
        assertEquals(0, p.solvedOn(day))
        assertEquals(67, p.accuracy)
        assertNull(Progress().accuracy)
    }

    @Test fun lessonPutsTopicIntoReviewTomorrow() {
        val p = Progress().lessonDone("straw_man", day)
        assertTrue("straw_man" in p.lessons)
        assertEquals(listOf("straw_man"), p.dueOn(day + 1))
        assertEquals(1, p.streakOn(day))
    }

    @Test fun blitzKeepsBest() {
        val p = Progress().blitz(120, day).blitz(80, day)
        assertEquals(120, p.blitzBest)
    }

    @Test fun weakSpotsPreferRepeatedMistakes() {
        val p = Progress()
            .record("a", false, day).record("a", false, day)
            .record("b", false, day).record("b", true, day).record("b", true, day).record("b", true, day)
            .record("c", false, day)
        assertEquals(listOf("a", "c"), p.weakSpots())
    }

    @Test fun jsonRoundTripAndGarbage() {
        val p = Progress().lessonDone("hasty", day).record("loaded", false, day).finish(false, day).blitz(70, day)
        assertEquals(p, Progress.fromJson(p.toJson()))
        assertEquals(Progress(), Progress.fromJson("not json"))
        assertEquals(Progress(), Progress.fromJson(null))
    }
}
