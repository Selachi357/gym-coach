package com.gymcoach.coach

import kotlin.math.max
import kotlin.math.roundToInt

enum class ProgramFocus(val label: String, val description: String) {
    ONBOARD(
        "Onboard plan",
        "Your 6-week plan: 3 strength and 2 cardio sessions every 7 days, built around watch schedules on a moving ship.",
    ),
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
    /** Short phase name shown in the overview, e.g. "Learn". */
    val phase: String = "",
)

data class ProgramDayPlan(
    val week: Int,
    /** Workout number within the week; for the onboard plan, the day of the 7-day cycle (1–7). */
    val day: Int,
    val name: String,
    /** Exercise names from the library, strength work first, cardio last. */
    val exercises: List<String>,
    /** How long it takes, e.g. "45–60 min". */
    val duration: String = "",
    /** Extra instructions for the day, e.g. the rower intervals. */
    val note: String? = null,
)

/**
 * Builds 6-week programs from the exercises in the built-in library.
 *
 * The generated programs (strength, muscle, fitness) build up over weeks 1–5 and deload in week 6,
 * and are done in order whenever you train. The onboard plan follows a fixed 7-day cycle from its
 * start date with Learn, Build and Push phases.
 * Weights come from the coach's per-exercise base target, so programs keep adapting when the
 * coach moves you up or down.
 */
object ProgramPlanner {
    const val WEEKS = 6
    const val DELOAD_WEEK = 6
    val DAY_OPTIONS = listOf(2, 3, 4)

    /** Training days per 7-day cycle in the onboard plan. */
    const val ONBOARD_DAYS = 5

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

    // Onboard plan

    val onboardStrengthA = listOf("Goblet Squat", "Dumbbell Bench Press", "One-Arm Dumbbell Row", "Glute Bridge", "Plank")
    val onboardStrengthB = listOf(
        "Dumbbell Romanian Deadlift", "Split Squat", "Dumbbell Shoulder Press", "Chest-Supported Dumbbell Row", "Push-Up", "Dead Bug",
    )

    /** Top of each exercise's rep range. Hit it on every set and the coach adds weight. */
    private val onboardReps = mapOf(
        "Goblet Squat" to 12,
        "Dumbbell Bench Press" to 12,
        "One-Arm Dumbbell Row" to 12,
        "Glute Bridge" to 15,
        "Dumbbell Romanian Deadlift" to 12,
        "Split Squat" to 10,
        "Dumbbell Shoulder Press" to 12,
        "Chest-Supported Dumbbell Row" to 12,
        "Dead Bug" to 8,
    )

    /** Rep range text shown next to the target, e.g. "8–12". */
    val onboardRepRange = mapOf(
        "Goblet Squat" to "8–12",
        "Dumbbell Bench Press" to "8–12",
        "One-Arm Dumbbell Row" to "10–12 per arm",
        "Glute Bridge" to "12–15",
        "Plank" to "20–45 s",
        "Dumbbell Romanian Deadlift" to "8–12",
        "Split Squat" to "8–10 per leg",
        "Dumbbell Shoulder Press" to "8–12",
        "Chest-Supported Dumbbell Row" to "10–12",
        "Push-Up" to "6–15",
        "Dead Bug" to "8 per side",
    )

    private fun onboardPhase(week: Int) = when (week) {
        1, 2 -> 0
        3, 4 -> 1
        else -> 2
    }

    fun rowerIntervals(week: Int): String = when (onboardPhase(week)) {
        0 -> "6 × (1 min hard / 2 min easy)"
        1 -> "8 × (1 min hard / 1:30 easy)"
        else -> "6 × (1:30 hard / 1:30 easy)"
    }

    /** Total rower minutes including 5 min easy before and after the intervals. */
    private fun rowerMinutes(week: Int) = when (onboardPhase(week)) {
        0 -> 28
        1 -> 30
        else -> 28
    }

    private fun crosstrainerMinutes(week: Int) = when (onboardPhase(week)) {
        0 -> 25
        1 -> 30
        else -> 40
    }

    /** The 7-day cycle; null on rest days (4 and 7). */
    fun onboardDay(week: Int, day: Int): ProgramDayPlan? {
        val odd = week % 2 == 1
        val strengthA = ProgramDayPlan(week, day, "Strength A", onboardStrengthA, "45–60 min")
        val strengthB = ProgramDayPlan(week, day, "Strength B", onboardStrengthB, "45–60 min")
        return when (day) {
            1, 5 -> if (odd) strengthA else strengthB
            3 -> if (odd) strengthB else strengthA
            2 -> ProgramDayPlan(
                week, day, "Cardio 1: rower intervals", listOf("Rowing Machine"), "25–35 min",
                note = "5 min easy, then ${rowerIntervals(week)}, then 5 min easy. Hard: you can only say a few words. " +
                    "Easy: you could chat. Stroke rate 26–30 hard, 20–24 easy.",
            )
            6 -> ProgramDayPlan(
                week, day, "Cardio 2: crosstrainer steady", listOf("Crosstrainer"), "${crosstrainerMinutes(week)} min",
                note = "One continuous effort: you can talk in full sentences but would rather not sing.",
            )
            else -> null
        }
    }

    fun onboardRestNote(day: Int): String =
        if (day == 4) "Rest day. Optional: 15–20 min easy crosstrainer and stretching." else "Full rest day. Sleep well."

    fun isCalendarBased(focus: ProgramFocus) = focus == ProgramFocus.ONBOARD

    fun isDeload(focus: ProgramFocus, week: Int) = focus != ProgramFocus.ONBOARD && week == DELOAD_WEEK

    fun workoutsPerWeek(focus: ProgramFocus, daysPerWeek: Int) =
        if (focus == ProgramFocus.ONBOARD) ONBOARD_DAYS else daysPerWeek

    fun templates(daysPerWeek: Int): List<Pair<String, List<String>>> = when (daysPerWeek) {
        2 -> listOf(fullBodyA, fullBodyB)
        3 -> listOf(fullBodyA, fullBodyB, fullBodyC)
        else -> listOf(upperA, lowerA, upperB, lowerB)
    }

    /** Every workout of the program in order. */
    fun plan(daysPerWeek: Int, focus: ProgramFocus = ProgramFocus.MUSCLE): List<ProgramDayPlan> {
        if (focus == ProgramFocus.ONBOARD) {
            return (1..WEEKS).flatMap { week -> (1..7).mapNotNull { day -> onboardDay(week, day) } }
        }
        val days = templates(daysPerWeek)
        return (1..WEEKS).flatMap { week ->
            days.mapIndexed { i, (name, exercises) -> ProgramDayPlan(week, i + 1, name, exercises, "45–60 min") }
        }
    }

    fun scheme(focus: ProgramFocus, week: Int): WeekScheme = when (focus) {
        ProgramFocus.ONBOARD -> when (week) {
            1, 2 -> WeekScheme(2, 12, 1.0, 1.0, "Learn: easy, stop with 3–4 reps left in the tank. Technique first.", "Learn")
            3, 4 -> WeekScheme(3, 12, 1.0, 1.0, "Build: moderate, 2–3 reps left. Add weight each week.", "Build")
            5 -> WeekScheme(3, 12, 1.0, 1.0, "Push: hard, 1–2 reps left. Beat your week 4 numbers.", "Push")
            else -> WeekScheme(4, 12, 1.0, 1.0, "Push: hard, 1–2 reps left. Never to failure on a moving ship.", "Push")
        }
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
        exerciseName: String = "",
        weightStepKg: Double = Coach.WEIGHT_STEP_KG,
    ): Prescription {
        val s = scheme(focus, week)
        if (focus == ProgramFocus.ONBOARD) {
            return when {
                isCardio -> base.copy(
                    sets = 1,
                    reps = if (exerciseName == "Crosstrainer") crosstrainerMinutes(week) else rowerMinutes(week),
                )
                // Weight stays the coach's; reps sit at the top of the range so hitting them earns more weight.
                else -> base.copy(sets = s.sets, reps = onboardReps[exerciseName] ?: base.reps)
            }
        }
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
                weightKg = if (s.weightFactor == 1.0) base.weightKg
                else max(0.0, Coach.roundToStep(base.weightKg * s.weightFactor, weightStepKg)),
            )
        }
    }
}
