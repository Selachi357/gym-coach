package com.gymcoach.data

import com.gymcoach.coach.Coach
import com.gymcoach.coach.CoachAction
import com.gymcoach.coach.CoachDecision
import com.gymcoach.coach.ExerciseInfo
import com.gymcoach.coach.LoggedSet
import com.gymcoach.coach.Prescription
import com.gymcoach.coach.formatKg
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.Flow

class GymRepository(private val db: GymDatabase) {
    private val dao = db.dao()

    val exercises: Flow<List<Exercise>> = dao.exercises()
    val targets: Flow<List<ExerciseTarget>> = dao.targets()
    val activeSession: Flow<WorkoutSession?> = dao.activeSession()
    val sessionSummaries: Flow<List<SessionSummary>> = dao.sessionSummaries()
    val goals: Flow<List<Goal>> = dao.goals()
    val bestWeights: Flow<List<BestWeight>> = dao.bestWeights()
    fun coachNotes(limit: Int = 5): Flow<List<CoachNote>> = dao.coachNotes(limit)
    fun exercise(id: Long): Flow<Exercise?> = dao.exercise(id)
    fun session(id: Long): Flow<WorkoutSession?> = dao.session(id)
    fun sessionExercises(sessionId: Long): Flow<List<Exercise>> = dao.sessionExercises(sessionId)
    fun setsForSession(sessionId: Long): Flow<List<SetEntry>> = dao.setsForSession(sessionId)
    fun setsForExercise(exerciseId: Long): Flow<List<SetEntry>> = dao.setsForExercise(exerciseId)
    fun sessionsThisWeek(): Flow<Int> = dao.finishedSessionsSince(startOfWeek())

    suspend fun seedIfEmpty() {
        if (dao.exerciseCount() == 0) {
            dao.insertExercises(SeedExercises.all)
            dao.insertCoachNote(
                CoachNote(
                    message = "Welcome! Start a workout, add exercises, and log each set with how hard it felt. " +
                        "I'll adjust your targets after every session.",
                    createdAt = System.currentTimeMillis(),
                )
            )
        }
    }

    // Workouts

    suspend fun startSession(): Long = dao.insertSession(WorkoutSession(startedAt = System.currentTimeMillis()))

    suspend fun addExerciseToSession(sessionId: Long, exerciseId: Long) {
        dao.insertSessionExercise(SessionExercise(sessionId, exerciseId, dao.maxPosition(sessionId) + 1))
    }

    suspend fun removeExerciseFromSession(sessionId: Long, exerciseId: Long) {
        dao.getSessionExercise(sessionId, exerciseId)?.let { dao.deleteSessionExercise(it) }
    }

    suspend fun logSet(sessionId: Long, exerciseId: Long, weightKg: Double, reps: Int, effort: Int, distanceKm: Double = 0.0) {
        val target = targetFor(exerciseId)
        dao.insertSet(
            SetEntry(
                sessionId = sessionId,
                exerciseId = exerciseId,
                weightKg = weightKg,
                reps = reps,
                targetReps = target.reps,
                effort = effort,
                distanceKm = distanceKm,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun deleteSet(set: SetEntry) = dao.deleteSet(set)

    suspend fun discardSession(sessionId: Long) = dao.deleteSession(sessionId)

    /** The coach's current target, or the exercise's starting target when there is none yet. */
    suspend fun targetFor(exerciseId: Long): ExerciseTarget {
        dao.getTarget(exerciseId)?.let { return it }
        val ex = dao.getExercise(exerciseId)
        return ExerciseTarget(
            exerciseId = exerciseId,
            weightKg = ex?.startWeightKg ?: 0.0,
            reps = ex?.startReps ?: 8,
            sets = ex?.startSets ?: 3,
            updatedAt = System.currentTimeMillis(),
        )
    }

    fun defaultTarget(exercise: Exercise) = ExerciseTarget(
        exerciseId = exercise.id,
        weightKg = exercise.startWeightKg,
        reps = exercise.startReps,
        sets = exercise.startSets,
        updatedAt = 0,
    )

    /**
     * "Too hard" pressed mid-workout. Lowers the target right away, or swaps the exercise in this
     * session for an easier one. Returns the coach's message.
     */
    suspend fun tooHard(sessionId: Long, exerciseId: Long): String {
        val ex = dao.getExercise(exerciseId) ?: return "Exercise not found."
        val decision = Coach.tooHard(ex.toInfo(), targetFor(exerciseId).toPrescription())
        applyDecision(ex, decision, sessionId)
        return decision.message
    }

    /** Ends the workout and lets the coach review every exercise. Returns the coach's messages. */
    suspend fun finishSession(sessionId: Long): List<String> {
        val session = dao.getSession(sessionId) ?: return emptyList()
        val sets = dao.getSetsForSession(sessionId)
        if (sets.isEmpty()) {
            dao.deleteSession(sessionId)
            return listOf("No sets were logged, so I didn't save this workout.")
        }
        dao.updateSession(session.copy(endedAt = System.currentTimeMillis()))

        val messages = mutableListOf<String>()
        for (exerciseId in sets.map { it.exerciseId }.distinct()) {
            val ex = dao.getExercise(exerciseId) ?: continue
            val recent = dao.recentSetsForExercise(exerciseId, limit = 2)
                .groupBy { it.sessionId }
                .values
                .map { group -> group.map { LoggedSet(it.weightKg, it.reps, it.targetReps, it.effort) } }
            val decision = Coach.evaluate(ex.toInfo(), targetFor(exerciseId).toPrescription(), recent)
            applyDecision(ex, decision, sessionId = null)
            messages += decision.message
        }
        messages += checkGoals()
        return messages
    }

    private suspend fun applyDecision(ex: Exercise, decision: CoachDecision, sessionId: Long?) {
        val now = System.currentTimeMillis()
        val switchTo = decision.switchTo?.let { dao.getExerciseByName(it) }
        if (switchTo != null && (decision.action == CoachAction.SWITCH_EASIER || decision.action == CoachAction.SWITCH_HARDER)) {
            // Keep the old target so going back later starts where you left off; seed the new one.
            if (dao.getTarget(switchTo.id) == null) {
                dao.upsertTarget(defaultTarget(switchTo).copy(updatedAt = now))
            }
            if (sessionId != null) {
                val position = dao.getSessionExercise(sessionId, ex.id)?.position ?: (dao.maxPosition(sessionId) + 1)
                dao.insertSessionExercise(SessionExercise(sessionId, switchTo.id, position))
                // Drop the old exercise from today's list unless sets were already logged for it.
                if (dao.getSetsForSession(sessionId).none { it.exerciseId == ex.id }) {
                    removeExerciseFromSession(sessionId, ex.id)
                }
            }
        } else {
            val p = decision.prescription
            dao.upsertTarget(ExerciseTarget(ex.id, p.weightKg, p.reps, p.sets, now))
        }
        dao.insertCoachNote(CoachNote(message = decision.message, createdAt = now))
    }

    // Goals

    suspend fun addStrengthGoal(exerciseId: Long, targetKg: Double) {
        dao.insertGoal(Goal(type = GoalType.STRENGTH, exerciseId = exerciseId, target = targetKg, createdAt = System.currentTimeMillis()))
    }

    suspend fun addWeeklyGoal(sessionsPerWeek: Int) {
        dao.insertGoal(Goal(type = GoalType.WEEKLY_SESSIONS, target = sessionsPerWeek.toDouble(), createdAt = System.currentTimeMillis()))
    }

    suspend fun deleteGoal(goal: Goal) = dao.deleteGoal(goal)

    private suspend fun checkGoals(): List<String> {
        val messages = mutableListOf<String>()
        val now = System.currentTimeMillis()
        for (goal in dao.openGoals()) {
            when (goal.type) {
                GoalType.STRENGTH -> {
                    val exId = goal.exerciseId ?: continue
                    val best = dao.bestWeight(exId) ?: 0.0
                    if (best >= goal.target) {
                        dao.updateGoal(goal.copy(achievedAt = now))
                        val name = dao.getExercise(exId)?.name ?: "your lift"
                        messages += "Goal reached: $name at ${formatKg(goal.target)} kg! Set a new one."
                    }
                }
                GoalType.WEEKLY_SESSIONS -> {
                    val count = dao.countFinishedSessionsSince(startOfWeek())
                    if (count == goal.target.toInt()) {
                        messages += "You hit your goal of ${goal.target.toInt()} workouts this week. Nice consistency!"
                    }
                }
            }
        }
        messages.forEach { dao.insertCoachNote(CoachNote(message = it, createdAt = now)) }
        return messages
    }

    companion object {
        fun startOfWeek(): Long = LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}

fun Exercise.toInfo() = ExerciseInfo(name, isBodyweight, easierName, harderName, isCardio)

fun ExerciseTarget.toPrescription() = Prescription(weightKg, reps, sets)
