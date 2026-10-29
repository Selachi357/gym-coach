package com.gymcoach.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimersTest {
    @Test
    fun countdownRunsPausesAndFinishes() {
        var c = Countdown(90_000).start(1_000)
        assertEquals(60_000, c.leftMs(31_000))
        c = c.pause(31_000)
        assertEquals(60_000, c.leftMs(500_000))
        c = c.start(100_000)
        assertFalse(c.finished(159_999))
        assertTrue(c.finished(160_000))
        assertEquals(0, c.leftMs(200_000))
    }

    @Test
    fun countdownRestartAndPlus() {
        val c = Countdown(60_000).restart(0, 120_000)
        assertEquals(120_000, c.leftMs(0))
        assertEquals(135_000, c.plus(15_000, 0).leftMs(0))
        // A stopped timer at full time changes its length, but never below 5 s.
        assertEquals(75_000, Countdown(60_000).plus(15_000, 0).totalMs)
        assertEquals(Countdown.MIN_MS, Countdown(10_000).plus(-15_000, 0).totalMs)
    }

    @Test
    fun stopwatchKeepsTimeAcrossPauses() {
        var s = Stopwatch().start(0).pause(10_000).start(50_000)
        assertEquals(15_000, s.elapsedMs(55_000))
        s = s.lap(55_000)
        assertEquals(listOf(15_000L), s.laps)
        assertEquals(Stopwatch(), s.reset())
    }

    @Test
    fun intervalsSwitchBetweenHardAndEasy() {
        val i = Intervals(rounds = 2, workS = 60, restS = 120).start(0)
        assertEquals(IntervalState(1, IntervalPhase.WORK, 60_000), i.state(0))
        assertEquals(IntervalState(1, IntervalPhase.REST, 120_000), i.state(60_000))
        assertEquals(IntervalState(2, IntervalPhase.WORK, 30_000), i.state(210_000))
        assertEquals(IntervalPhase.DONE, i.state(360_000).phase)
    }

    @Test
    fun intervalsReadTheRowerPlan() {
        for (week in 1..6) {
            assertTrue(Intervals.parse(ProgramPlanner.rowerIntervals(week)) != null)
        }
        assertEquals(Intervals(8, 60, 90), Intervals.parse("8 × (1 min hard / 1:30 easy)"))
        assertEquals(Intervals(6, 90, 90), Intervals.parse("6 × (1:30 hard / 1:30 easy)"))
    }

    @Test
    fun clockFormat() {
        assertEquals("1:30", formatClock(90_000))
        assertEquals("0:01", formatClock(1))
        assertEquals("0:00", formatClock(999, roundUp = false))
        assertEquals("1:02:03", formatClock(3_723_000))
    }
}
