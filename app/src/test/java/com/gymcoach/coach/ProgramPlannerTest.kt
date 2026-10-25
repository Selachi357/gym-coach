package com.gymcoach.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramPlannerTest {
    @Test
    fun planHasSixWeeksOfWorkouts() {
        for (days in ProgramPlanner.DAY_OPTIONS) {
            val plan = ProgramPlanner.plan(days)
            assertEquals(6 * days, plan.size)
            assertEquals(1, plan.first().week)
            assertEquals(6, plan.last().week)
        }
    }

    @Test
    fun deloadWeekIsLighter() {
        val base = Prescription(weightKg = 100.0, reps = 8, sets = 3)
        for (focus in ProgramFocus.entries) {
            val peak = ProgramPlanner.planned(focus, 5, isBodyweight = false, isCardio = false, base = base)
            val deload = ProgramPlanner.planned(focus, 6, isBodyweight = false, isCardio = false, base = base)
            assertTrue(deload.weightKg < peak.weightKg)
            assertTrue(deload.sets < peak.sets)
        }
    }

    @Test
    fun strengthPeakWeekIsHeavierThanBase() {
        val p = ProgramPlanner.planned(ProgramFocus.STRENGTH, 5, false, false, Prescription(100.0, 8, 3))
        assertEquals(105.0, p.weightKg, 0.0)
        assertEquals(3, p.reps)
    }

    @Test
    fun cardioKeepsCoachMinutesAndRoundsToFive() {
        val p = ProgramPlanner.planned(ProgramFocus.FITNESS, 4, isBodyweight = true, isCardio = true, base = Prescription(0.0, 20, 1))
        assertEquals(25, p.reps)
        assertEquals(1, p.sets)
    }
}
