package com.gymcoach.coach

import kotlin.math.max
import kotlin.math.roundToInt

enum class ProgramFocus(val label: String, val description: String) {
    STRENGTH("Strength", "Heavier weights, fewer reps. Get stronger on the big lifts."),
    MUSCLE("Muscle", "Moderate weights, 8 to 12 reps. Build size and shape."),
    FITNESS("Fitness", "Lighter weights, more reps and cardio. Feel fitter overall."),
}

/** Sets, reps and load for one week of a program. */
data class WeekScheme(
    val sets: Int,
    val reps: Int,
    /** Multiplied with the coach's base weight. */
    val weightFactor: Double,
    /** Multiplied with the coach's base cardio minutes. */
    val cardioFactor: Double,
    val note: String,
)

data class ProgramDayPlan(
    val week: Int,
    val day: Int,
    val name: String,
    /** Exercise names from the library, strength work first, cardio last. */
    val exercises: List<String>,
)

/**
 * Builds a 6-week program from the exercises in the built-in library.
 * Weeks 1–5 build up step by step; week 6 is a lighter deload week to recover.
 * Weights come from the coach's per-exercise base target, so the program keeps
 * adapting when the coach moves you up or down.
 */
object ProgramPlanner {
    const val WEEKS = 6
    const val DELOAD_WEEK = 6
    val DAY_OPTIONS = listOf(2, 3, 4)

    private val fullBodyA = "Full body A" to listOf(
        "Back Squat", "Bench Press", "One-Arm Dumbbell Row", "Lateral Raise", "Dead Bug", "Rowing Machine",
    )
    private val fullBodyB = "Full body B" to listOf(
        "Romanian Deadlift", "Overhead Press", "Dumbbell Pullover", "Walking Lunge", "Dumbbell Curl", "Zwift Bike",
    )
    private val fullBodyC = "Full body C" to listOf(
        "Leg Press", "Incline Dumbbell Press", "Barbell Row", "Overhead Triceps Extension", "Lying Leg Raise", "Treadmill Run",
    )
    private val upperA = "Upper body A" to listOf(
        "Bench Press", "Barbell Row", "Dumbbell Shoulder Press", "Dumbbell Pullover", "Dumbbell Curl", "Overhead Triceps Extension",
    )
    private val lowerA = "Lower body A" to listOf(
        "Back Squat", "Romanian Deadlift", "Leg Press", "Calf Raise", "Dead Bug", "Zwift Bike",
    )
    private val upperB = "Upper body B" to listOf(
        "Incline Dumbbell Press", "One-Arm Dumbbell Row", "Overhead Press", "Lateral Raise", "Push-Up", "Treadmill Run",
    )
    private val lowerB = "Lower body B" to listOf(
        "Deadlift", "Walking Lunge", "Leg Press", "Calf Raise", "Lying Leg Raise", "Rowing Machine",
    )

    fun templates(daysPerWeek: Int): List<Pair<String, List<String>>> = when (daysPerWeek) {
        2 -> listOf(fullBodyA, fullBodyB)
        3 -> listOf(fullBodyA, fullBodyB, fullBodyC)
        else -> listOf(upperA, lowerA, upperB, lowerB)
    }

    /** Every workout of the program in the order you do them. */
    fun plan(daysPerWeek: Int): List<ProgramDayPlan> {
        val days = templates(daysPerWeek)
        return (1..WEEKS).flatMap { week ->
            days.mapIndexed { i, (name, exercises) -> ProgramDayPlan(week, i + 1, name, exercises) }
        }
    }

    fun scheme(focus: ProgramFocus, week: Int): WeekScheme = when (focus) {
        ProgramFocus.STRENGTH -> when (week) {
            1 -> WeekScheme(3, 8, 0.90, 1.0, "Learn the rhythm. Leave 2 to 3 reps in the tank.")
            2 -> WeekScheme(4, 6, 0.95, 1.0, "A bit heavier, fewer reps.")
            3 -> WeekScheme(4, 6, 1.00, 1.0, "Working weight. Every rep clean.")
            4 -> WeekScheme(5, 5, 1.00, 1.0, "More sets. Rest 2 to 3 minutes between sets.")
            5 -> WeekScheme(5, 3, 1.05, 1.0, "Heaviest week. Stop a set if your form breaks.")
            else -> WeekScheme(2, 5, 0.80, 0.7, "Deload: light and easy so your body can recover.")
        }
        ProgramFocus.MUSCLE -> when (week) {
            1 -> WeekScheme(3, 12, 0.85, 1.0, "Learn the rhythm. Control the way down.")
            2 -> WeekScheme(3, 10, 0.90, 1.0, "Slightly heavier.")
            3 -> WeekScheme(4, 10, 0.90, 1.0, "One extra set on everything.")
            4 -> WeekScheme(4, 8, 1.00, 1.0, "Working weight. Squeeze every rep.")
            5 -> WeekScheme(4, 8, 1.00, 1.0, "Push close to failure on the last set.")
            else -> WeekScheme(2, 10, 0.75, 0.7, "Deload: light and easy so your body can recover.")
        }
        ProgramFocus.FITNESS -> when (week) {
            1 -> WeekScheme(2, 12, 0.80, 1.0, "Easy start. Keep rest short, about 60 seconds.")
            2 -> WeekScheme(3, 12, 0.80, 1.0, "One more set.")
            3 -> WeekScheme(3, 12, 0.85, 1.1, "A little heavier and a little more cardio.")
            4 -> WeekScheme(3, 15, 0.80, 1.2, "Higher reps, longer cardio.")
            5 -> WeekScheme(3, 15, 0.85, 1.2, "Toughest week. Keep moving.")
            else -> WeekScheme(2, 12, 0.70, 0.7, "Deload: light and easy so your body can recover.")
        }
    }

    /** Turns the coach's base target into this week's target. */
    fun planned(
        focus: ProgramFocus,
        week: Int,
        isBodyweight: Boolean,
        isCardio: Boolean,
        base: Prescription,
    ): Prescription {
        val s = scheme(focus, week)
        return when {
            isCardio -> base.copy(
                sets = 1,
                reps = max(Coach.MIN_CARDIO_MIN, ((base.reps * s.cardioFactor) / 5).roundToInt() * 5),
            )
            // Bodyweight moves keep the coach's rep target so the coach can keep progressing them.
            isBodyweight -> base.copy(
                sets = s.sets,
                reps = if (week == DELOAD_WEEK) max(Coach.MIN_REPS, (base.reps * 0.7).roundToInt()) else base.reps,
            )
            else -> base.copy(
                sets = s.sets,
                reps = s.reps,
                weightKg = max(0.0, Coach.roundToStep(base.weightKg * s.weightFactor)),
            )
        }
    }
}
