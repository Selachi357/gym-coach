package com.gymcoach.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HeroCalculatorTest {
    private val monday = LocalDate.of(2026, 11, 9)

    private fun workout(date: LocalDate, weight: Double = 20.0, sets: Int = 3, program: Boolean = false) =
        WorkoutRecord(date, program, List(sets) { SetRecord(exerciseId = 1, weightKg = weight, reps = 10, cardio = false) })

    @Test
    fun emptyLogIsLevelOnePeasant() {
        val h = HeroCalculator.compute(emptyList(), monday)
        assertEquals(1, h.level)
        assertEquals(Rank.PEASANT, h.rank)
        assertEquals(0, h.weekStreak)
    }

    @Test
    fun firstWorkoutGivesXpAndFirstBlood() {
        val h = HeroCalculator.compute(listOf(workout(monday)), monday)
        // 40 workout + 3 sets × 4 + 100 quest
        assertEquals(152, h.xp)
        assertEquals(2, h.level)
        assertTrue(Quest.FIRST_BLOOD in h.unlocked)
    }

    @Test
    fun heavierWeightCountsAsRecord() {
        val h = HeroCalculator.compute(listOf(workout(monday, 20.0), workout(monday.plusDays(2), 22.0)), monday.plusDays(2))
        assertEquals(1, h.personalRecords)
    }

    @Test
    fun streakCountsFullWeeksAndKeepsRunningThisWeek() {
        val log = (0 until 3).flatMap { w ->
            (0 until 3).map { d -> workout(monday.plusWeeks(w.toLong()).plusDays(d * 2L)) }
        }
        // Wednesday of week 4, nothing logged yet this week: streak still 3.
        val h = HeroCalculator.compute(log, monday.plusWeeks(3).plusDays(2))
        assertEquals(3, h.weekStreak)
        assertEquals(3, h.bestWeekStreak)
        assertTrue(Quest.STREAK_2 in h.unlocked)
    }

    @Test
    fun missedWeekBreaksStreak() {
        val log = listOf(0L, 2L, 4L).map { workout(monday.plusDays(it)) } +
            listOf(0L, 2L, 4L).map { workout(monday.plusWeeks(2).plusDays(it)) }
        val h = HeroCalculator.compute(log, monday.plusWeeks(2).plusDays(5))
        assertEquals(1, h.weekStreak)
    }

    @Test
    fun levelCurveGrows() {
        assertEquals(1, HeroCalculator.levelFor(99).first)
        assertEquals(2, HeroCalculator.levelFor(100).first)
        assertEquals(3, HeroCalculator.levelFor(250).first)
        assertEquals(Rank.KNIGHT, Rank.forLevel(10))
    }
}
