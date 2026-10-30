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
        for (focus in ProgramFocus.entries.filter { !ProgramPlanner.isCalendarBased(it) }) {
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

    @Test
    fun onboardPlanAlternatesStrengthAAndB() {
        val plan = ProgramPlanner.plan(5, ProgramFocus.ONBOARD)
        assertEquals(30, plan.size)
        val week1 = plan.filter { it.week == 1 }.map { it.name }
        assertEquals(listOf("Strength A", "Cardio 1: rower intervals", "Strength B", "Strength A", "Cardio 2: crosstrainer steady"), week1)
        val week2 = plan.filter { it.week == 2 }.filter { it.name.startsWith("Strength") }.map { it.name }
        assertEquals(listOf("Strength B", "Strength A", "Strength B"), week2)
        assertEquals(null, ProgramPlanner.onboardDay(1, 4))
        assertEquals(null, ProgramPlanner.onboardDay(1, 7))
    }

    @Test
    fun onboardPhasesSetSetsAndTopOfRange() {
        val base = Prescription(14.0, 8, 3)
        val learn = ProgramPlanner.planned(ProgramFocus.ONBOARD, 1, false, false, base, "Goblet Squat", 2.0)
        assertEquals(2, learn.sets)
        assertEquals(12, learn.reps)
        assertEquals(14.0, learn.weightKg, 0.0)
        assertEquals(3, ProgramPlanner.planned(ProgramFocus.ONBOARD, 3, false, false, base, "Goblet Squat").sets)
        assertEquals(10, ProgramPlanner.planned(ProgramFocus.ONBOARD, 3, false, false, base, "Split Squat").reps)
        val cross = ProgramPlanner.planned(ProgramFocus.ONBOARD, 5, true, true, Prescription(0.0, 25, 1), "Crosstrainer")
        assertEquals(40, cross.reps)
        assertTrue(!ProgramPlanner.isDeload(ProgramFocus.ONBOARD, 6))
    }

    @Test
    fun beginnerPlanFollowsThePdfWeek() {
        val plan = ProgramPlanner.plan(6, ProgramFocus.BEGINNER)
        assertEquals(6 * 6, plan.size)
        assertEquals(null, ProgramPlanner.beginnerDay(1, 7))
        assertEquals("Goblet Squat", ProgramPlanner.beginnerDay(1, 1)!!.exercises.first())
        assertTrue(ProgramPlanner.beginnerDay(1, 3)!!.exercises.contains("Leg Press"))
        assertTrue(ProgramPlanner.beginnerDay(1, 5)!!.exercises.contains("Farmer's Carry"))
        assertEquals(6, ProgramPlanner.workoutsPerWeek(ProgramFocus.BEGINNER, 3))
        assertTrue(ProgramPlanner.isCalendarBased(ProgramFocus.BEGINNER))
        assertTrue(!ProgramPlanner.isDeload(ProgramFocus.BEGINNER, 6))
    }

    @Test
    fun beginnerSetsRepsAndCardioByWeek() {
        val base = Prescription(16.0, 8, 3)
        assertEquals(2, ProgramPlanner.planned(ProgramFocus.BEGINNER, 1, false, false, base, "Goblet Squat").sets)
        assertEquals(10, ProgramPlanner.planned(ProgramFocus.BEGINNER, 1, false, false, base, "Goblet Squat").reps)
        assertEquals(16.0, ProgramPlanner.planned(ProgramFocus.BEGINNER, 1, false, false, base, "Goblet Squat").weightKg, 0.0)
        assertEquals(4, ProgramPlanner.planned(ProgramFocus.BEGINNER, 5, false, false, base, "Goblet Squat").sets)
        assertEquals(3, ProgramPlanner.planned(ProgramFocus.BEGINNER, 5, false, false, base, "Plank").sets)
        val cardio = Prescription(0.0, 20, 1)
        assertEquals(30, ProgramPlanner.planned(ProgramFocus.BEGINNER, 1, false, true, cardio, "Crosstrainer", day = 2).reps)
        assertEquals(50, ProgramPlanner.planned(ProgramFocus.BEGINNER, 5, false, true, cardio, "Crosstrainer", day = 2).reps)
        assertEquals(10, ProgramPlanner.planned(ProgramFocus.BEGINNER, 1, false, true, cardio, "Rowing Machine", day = 1).reps)
        assertEquals(10, ProgramPlanner.planned(ProgramFocus.BEGINNER, 1, false, true, cardio, "Rowing Machine", day = 6).reps)
        assertEquals(20, ProgramPlanner.planned(ProgramFocus.BEGINNER, 2, false, true, cardio, "Rowing Machine", day = 6).reps)
        assertEquals("8–10", ProgramPlanner.repRange(ProgramFocus.BEGINNER, "Goblet Squat"))
        // The interval notes work with the timer.
        assertEquals(Intervals(5, 60, 60), Intervals.parse(ProgramPlanner.beginnerDay(1, 1)!!.note!!))
        assertEquals(Intervals(6, 30, 90), Intervals.parse(ProgramPlanner.beginnerDay(1, 5)!!.note!!))
    }
}
