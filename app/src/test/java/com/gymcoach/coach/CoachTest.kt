package com.gymcoach.coach

import org.junit.Assert.assertEquals
import org.junit.Test

class CoachTest {
    private val bench = ExerciseInfo("Bench Press", isBodyweight = false, easierName = "Dumbbell Bench Press", harderName = null)
    private val pushUp = ExerciseInfo("Push-Up", isBodyweight = true, easierName = "Knee Push-Up", harderName = "Decline Push-Up")
    private val target = Prescription(weightKg = 60.0, reps = 8, sets = 3)

    private fun sets(reps: Int, effort: Int, n: Int = 3, weight: Double = 60.0, targetReps: Int = 8) =
        List(n) { LoggedSet(weight, reps, targetReps, effort) }

    @Test
    fun easySessionAddsWeight() {
        val d = Coach.evaluate(bench, target, listOf(sets(8, 7)))
        assertEquals(CoachAction.PROGRESS, d.action)
        assertEquals(62.5, d.prescription.weightKg, 0.0)
    }

    @Test
    fun oneHardSessionHolds() {
        val d = Coach.evaluate(bench, target, listOf(sets(6, 10), sets(8, 7)))
        assertEquals(CoachAction.HOLD, d.action)
    }

    @Test
    fun twoHardSessionsDeload() {
        val d = Coach.evaluate(bench, target, listOf(sets(6, 10), sets(7, 9)))
        assertEquals(CoachAction.DELOAD, d.action)
        assertEquals(55.0, d.prescription.weightKg, 0.0)
    }

    @Test
    fun tooHardOnBodyweightSwitchesToEasier() {
        val d = Coach.tooHard(pushUp, Prescription(0.0, 10, 3))
        assertEquals(CoachAction.SWITCH_EASIER, d.action)
        assertEquals("Knee Push-Up", d.switchTo)
    }

    @Test
    fun bodyweightAtMaxRepsSwitchesHarder() {
        val p = Prescription(0.0, 15, 3)
        val d = Coach.evaluate(pushUp, p, listOf(sets(15, 6, weight = 0.0, targetReps = 15)))
        assertEquals(CoachAction.SWITCH_HARDER, d.action)
        assertEquals("Decline Push-Up", d.switchTo)
    }

    @Test
    fun lightWeightDeloadAlwaysDrops() {
        val d = Coach.tooHard(bench, Prescription(5.0, 8, 3))
        assertEquals(CoachAction.DELOAD, d.action)
        assertEquals(2.5, d.prescription.weightKg, 0.0)
    }

    @Test
    fun noEasierOptionReducesReps() {
        val plank = ExerciseInfo("Plank", isBodyweight = true, easierName = null, harderName = null)
        val d = Coach.tooHard(plank, Prescription(0.0, 8, 3))
        assertEquals(CoachAction.REDUCE_REPS, d.action)
        assertEquals(6, d.prescription.reps)
    }

    private val run = ExerciseInfo("Treadmill Run", isBodyweight = true, easierName = null, harderName = null, isCardio = true)

    @Test
    fun easyCardioAddsFiveMinutes() {
        val d = Coach.evaluate(run, Prescription(0.0, 20, 1), listOf(listOf(LoggedSet(0.0, 20, 20, 6))))
        assertEquals(CoachAction.PROGRESS, d.action)
        assertEquals(25, d.prescription.reps)
    }

    @Test
    fun tooHardCardioCutsTime() {
        val d = Coach.tooHard(run, Prescription(0.0, 30, 1))
        assertEquals(CoachAction.REDUCE_REPS, d.action)
        assertEquals(25, d.prescription.reps)
    }

    @Test
    fun cardioNeverDropsBelowTenMinutes() {
        val d = Coach.tooHard(run, Prescription(0.0, 10, 1))
        assertEquals(CoachAction.HOLD, d.action)
        assertEquals(10, d.prescription.reps)
    }

    @Test
    fun dumbbellsProgressInTwoKiloSteps() {
        val db = ExerciseInfo("Goblet Squat", isBodyweight = false, easierName = null, harderName = null, weightStepKg = 2.0)
        val d = Coach.evaluate(db, Prescription(14.0, 12, 2), listOf(sets(12, 6, n = 2, weight = 14.0, targetReps = 12)))
        assertEquals(16.0, d.prescription.weightKg, 0.0)
    }

    @Test
    fun plankAddsFiveSeconds() {
        val plank = ExerciseInfo("Plank", isBodyweight = true, easierName = "Dead Bug", harderName = null, isTimed = true)
        val d = Coach.evaluate(plank, Prescription(0.0, 20, 2), listOf(sets(20, 6, n = 2, weight = 0.0, targetReps = 20)))
        assertEquals(25, d.prescription.reps)
        assertEquals(15, Coach.tooHard(plank, Prescription(0.0, 20, 2)).prescription.reps)
    }
}
