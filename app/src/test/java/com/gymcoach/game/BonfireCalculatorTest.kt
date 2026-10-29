package com.gymcoach.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BonfireCalculatorTest {
    private val start = LocalDate.of(2026, 11, 11)

    private fun days(vararg offsets: Long) = offsets.map { start.plusDays(it) }.toSet()

    @Test
    fun noWorkoutsMeansColdFire() {
        val b = BonfireCalculator.compute(emptyList(), emptySet(), emptySet(), start)
        assertEquals(0, b.days)
        assertEquals(0, b.tier)
    }

    @Test
    fun restDaysAfterWorkoutsKeepTheFireWithoutAPlan() {
        // Train every third day: two rest days in between always count.
        val b = BonfireCalculator.compute(days(0, 3, 6, 9), emptySet(), emptySet(), start.plusDays(9))
        assertEquals(10, b.days)
        assertTrue(BonfireMilestone.CAMPFIRE in b.milestones)
    }

    @Test
    fun plannedRestDaysCountButMissedPlanDaysDoNot() {
        // Onboard week: train days 1,2,3,5,6; rest days 4 and 7.
        val plan = days(0, 1, 2, 4, 5)
        val rest = days(3, 6)
        val b = BonfireCalculator.compute(plan, rest, plan, start.plusDays(6))
        assertEquals(7, b.days)
        assertTrue(b.litToday)
        assertTrue(b.restToday)
    }

    @Test
    fun missedDayLeavesEmbersAndSecondWindSavesTheStreak() {
        val plan = days(0, 1, 2, 4, 5)
        // Day 2 missed (a plan day), trained again on day 4 after the planned rest on day 3.
        val trained = days(0, 1, 4)
        val embers = BonfireCalculator.compute(trained - start.plusDays(4), days(3), plan, start.plusDays(3))
        assertTrue(embers.embers || embers.days > 0)
        val b = BonfireCalculator.compute(trained, days(3), plan, start.plusDays(4))
        assertEquals(4, b.days) // days 0, 1, 3 (rest), 4; the missed day doesn't count
        assertEquals(1, b.secondWindsUsed)
        assertFalse(b.secondWindReady)
    }

    @Test
    fun twoMissedDaysRestartTheStreak() {
        val plan = days(0, 1, 2, 3, 4, 5)
        val b = BonfireCalculator.compute(days(0, 1, 4, 5), emptySet(), plan, start.plusDays(5))
        assertEquals(2, b.days)
        assertEquals(2, b.best)
    }

    @Test
    fun todayNeverBreaksTheStreak() {
        val plan = days(0, 1, 2)
        val b = BonfireCalculator.compute(days(0, 1), emptySet(), plan, start.plusDays(2))
        assertEquals(2, b.days)
        assertFalse(b.litToday)
    }

    @Test
    fun contractsCountTheChosenGoal() {
        val monday = LocalDate.of(2026, 11, 9)
        val cardio = WorkoutRecord(monday, true, listOf(SetRecord(2, 0.0, 30, true)))
        val lift = WorkoutRecord(monday.plusDays(1), true, listOf(SetRecord(1, 20.0, 10, false)))
        val log = listOf(cardio, lift, cardio.copy(date = monday.plusDays(2)))
        val road = BonfireCalculator.contracts(log, mapOf(monday to Contract.TWO_CARDIO), 4, monday.plusDays(2))
        assertTrue(road.thisWeek.done)
        assertEquals(1, road.completed)
        val oath = BonfireCalculator.contracts(log, mapOf(monday to Contract.WEEKLY_TARGET), 4, monday.plusDays(2))
        assertFalse(oath.thisWeek.done)
        assertEquals(3, oath.thisWeek.current)
    }

    @Test
    fun milestonesAndContractsGiveBonusXp() {
        val b = BonfireCalculator.compute(days(0, 3, 6, 9), emptySet(), emptySet(), start.plusDays(9))
        val c = Contracts(ContractState(start, Contract.THREE_WORKOUTS, 3, 3), completed = 2)
        assertEquals(50 + 100 + 2 * 250, BonfireCalculator.bonusXp(b, c))
    }
}
