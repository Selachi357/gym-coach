package com.gymcoach.coach

import kotlin.math.max
import kotlin.math.roundToInt

/** One logged set as the coach sees it. Effort is 1 (very easy) to 10 (could not do another rep). */
data class LoggedSet(
    val weightKg: Double,
    val reps: Int,
    val targetReps: Int,
    val effort: Int,
)

/** What the coach currently asks you to do for an exercise. */
data class Prescription(
    val weightKg: Double,
    val reps: Int,
    val sets: Int,
)

data class ExerciseInfo(
    val name: String,
    val isBodyweight: Boolean,
    val easierName: String?,
    val harderName: String?,
    /** Cardio uses [Prescription.reps] and [LoggedSet.reps] as minutes. */
    val isCardio: Boolean = false,
)

enum class CoachAction { PROGRESS, HOLD, DELOAD, REDUCE_REPS, SWITCH_EASIER, SWITCH_HARDER }

data class CoachDecision(
    val action: CoachAction,
    val prescription: Prescription,
    /** Name of the exercise to switch to, for SWITCH_EASIER / SWITCH_HARDER. */
    val switchTo: String? = null,
    val message: String,
)

/**
 * Offline rule-based coach.
 *
 * Rules:
 * - A session counts as a "struggle" when any set missed its target reps or the average effort was 9+.
 * - Two struggles in a row (or pressing "Too hard") makes things easier: weight drops ~10%,
 *   and when there is no weight to drop it switches to an easier variant or lowers the reps.
 * - Hitting every set with effort 8 or less moves you up: +2.5 kg for weighted lifts,
 *   +1 rep for bodyweight moves, and a harder variant once bodyweight reps reach [MAX_BODYWEIGHT_REPS].
 * - Anything else: keep the same target and build consistency.
 *
 * Cardio (run, row, bike) works in minutes: an easy session (effort 7 or less) adds 5 minutes up to
 * [MAX_CARDIO_MIN], and struggling cuts the time by ~20%.
 */
object Coach {
    const val WEIGHT_STEP_KG = 2.5
    const val MAX_BODYWEIGHT_REPS = 15
    const val MIN_REPS = 3
    const val EASY_EFFORT = 8
    const val HARD_EFFORT = 9.0
    const val EASY_CARDIO_EFFORT = 7
    const val CARDIO_STEP_MIN = 5
    const val MIN_CARDIO_MIN = 10
    const val MAX_CARDIO_MIN = 60

    fun isStruggle(sets: List<LoggedSet>): Boolean {
        if (sets.isEmpty()) return false
        val missedReps = sets.any { it.reps < it.targetReps }
        val avgEffort = sets.map { it.effort }.average()
        return missedReps || avgEffort >= HARD_EFFORT
    }

    fun isEasyWin(sets: List<LoggedSet>, current: Prescription, cardio: Boolean = false): Boolean {
        if (sets.size < current.sets) return false
        val easy = if (cardio) EASY_CARDIO_EFFORT else EASY_EFFORT
        return sets.all { it.reps >= it.targetReps && it.effort <= easy }
    }

    /**
     * Decide the next prescription after a workout.
     * [recentSessions] holds the sets per session for this exercise, newest session first.
     */
    fun evaluate(
        exercise: ExerciseInfo,
        current: Prescription,
        recentSessions: List<List<LoggedSet>>,
    ): CoachDecision {
        val last = recentSessions.firstOrNull().orEmpty()
        if (last.isEmpty()) {
            return CoachDecision(CoachAction.HOLD, current, message = "${exercise.name}: no sets logged, target stays the same.")
        }
        val previous = recentSessions.getOrNull(1).orEmpty()
        if (isStruggle(last) && isStruggle(previous)) {
            return makeEasier(exercise, current, reason = "it was tough two sessions in a row")
        }
        if (isEasyWin(last, current, exercise.isCardio)) {
            return makeHarder(exercise, current)
        }
        val msg = if (isStruggle(last)) {
            "${exercise.name}: that was hard. Same target next time, focus on clean form."
        } else {
            "${exercise.name}: solid work. Repeat the target once more before moving up."
        }
        return CoachDecision(CoachAction.HOLD, current, message = msg)
    }

    /** Called when you press "Too hard" in the middle of a workout. */
    fun tooHard(exercise: ExerciseInfo, current: Prescription): CoachDecision =
        makeEasier(exercise, current, reason = "you said it's too hard")

    private fun makeEasier(exercise: ExerciseInfo, current: Prescription, reason: String): CoachDecision {
        if (exercise.isCardio) {
            val minutes = max(MIN_CARDIO_MIN, roundToStep(current.reps * 0.8, CARDIO_STEP_MIN))
            val tip = "Slow down so you can still talk in short sentences."
            return if (minutes < current.reps) {
                CoachDecision(
                    CoachAction.REDUCE_REPS,
                    current.copy(reps = minutes),
                    message = "${exercise.name}: $reason, so aim for $minutes minutes. $tip",
                )
            } else {
                CoachDecision(CoachAction.HOLD, current, message = "${exercise.name}: $reason. Keep $minutes minutes but lower the speed or resistance. $tip")
            }
        }
        if (!exercise.isBodyweight && current.weightKg > WEIGHT_STEP_KG) {
            val newWeight = max(WEIGHT_STEP_KG, roundToStep(current.weightKg * 0.9))
            val dropped = if (newWeight < current.weightKg) newWeight else current.weightKg - WEIGHT_STEP_KG
            return CoachDecision(
                CoachAction.DELOAD,
                current.copy(weightKg = dropped),
                message = "${exercise.name}: $reason, so weight goes down to ${formatKg(dropped)} kg. " +
                    "Build back up with good form.",
            )
        }
        if (exercise.easierName != null) {
            return CoachDecision(
                CoachAction.SWITCH_EASIER,
                current.copy(weightKg = 0.0, reps = max(current.reps, 8)),
                switchTo = exercise.easierName,
                message = "${exercise.name}: $reason, so switch to ${exercise.easierName} for now. " +
                    "It trains the same muscles with less load.",
            )
        }
        if (current.reps > MIN_REPS) {
            val reps = max(MIN_REPS, current.reps - 2)
            return CoachDecision(
                CoachAction.REDUCE_REPS,
                current.copy(reps = reps),
                message = "${exercise.name}: $reason, so aim for $reps reps per set for now.",
            )
        }
        val sets = max(1, current.sets - 1)
        return CoachDecision(
            CoachAction.REDUCE_REPS,
            current.copy(sets = sets),
            message = "${exercise.name}: $reason, so do $sets set${if (sets == 1) "" else "s"} for now.",
        )
    }

    private fun makeHarder(exercise: ExerciseInfo, current: Prescription): CoachDecision {
        if (exercise.isCardio) {
            if (current.reps >= MAX_CARDIO_MIN) {
                return CoachDecision(
                    CoachAction.HOLD,
                    current,
                    message = "${exercise.name}: strong endurance! Keep $MAX_CARDIO_MIN minutes and try to cover more distance, " +
                        "or add intervals: 1 minute fast, 2 minutes easy.",
                )
            }
            val minutes = minOf(MAX_CARDIO_MIN, current.reps + CARDIO_STEP_MIN)
            return CoachDecision(
                CoachAction.PROGRESS,
                current.copy(reps = minutes),
                message = "${exercise.name}: that felt comfortable. Go for $minutes minutes next time.",
            )
        }
        if (exercise.isBodyweight) {
            if (current.reps >= MAX_BODYWEIGHT_REPS && exercise.harderName != null) {
                return CoachDecision(
                    CoachAction.SWITCH_HARDER,
                    current.copy(reps = 6),
                    switchTo = exercise.harderName,
                    message = "${exercise.name}: you've mastered it! Time to try ${exercise.harderName}.",
                )
            }
            val reps = minOf(current.reps + 1, MAX_BODYWEIGHT_REPS + 5)
            return CoachDecision(
                CoachAction.PROGRESS,
                current.copy(reps = reps),
                message = "${exercise.name}: great job, aim for $reps reps next time.",
            )
        }
        val newWeight = current.weightKg + WEIGHT_STEP_KG
        return CoachDecision(
            CoachAction.PROGRESS,
            current.copy(weightKg = newWeight),
            message = "${exercise.name}: all sets done with reps to spare. Next time: ${formatKg(newWeight)} kg.",
        )
    }

    fun roundToStep(kg: Double): Double = (kg / WEIGHT_STEP_KG).roundToInt() * WEIGHT_STEP_KG

    private fun roundToStep(value: Double, step: Int): Int = (value / step).roundToInt() * step
}

fun formatKg(kg: Double): String =
    if (kg % 1.0 == 0.0) kg.toInt().toString() else String.format(java.util.Locale.US, "%.1f", kg)

fun effortLabel(effort: Int): String = when {
    effort <= 4 -> "Easy"
    effort <= 6 -> "Moderate"
    effort <= 8 -> "Hard"
    effort == 9 -> "Very hard"
    else -> "Max effort"
}
