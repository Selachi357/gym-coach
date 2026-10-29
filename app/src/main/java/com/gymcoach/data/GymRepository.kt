package com.gymcoach.data

import com.gymcoach.coach.Coach
import com.gymcoach.coach.CoachAction
import com.gymcoach.coach.CoachDecision
import com.gymcoach.coach.ExerciseInfo
import com.gymcoach.coach.LoggedSet
import com.gymcoach.coach.Prescription
import com.gymcoach.coach.ProgramDayPlan
import com.gymcoach.coach.ProgramFocus
import com.gymcoach.coach.ProgramPlanner
import com.gymcoach.coach.formatKg
import com.gymcoach.game.HeroCalculator
import com.gymcoach.game.Bonfire
import com.gymcoach.game.BonfireCalculator
import com.gymcoach.game.City
import com.gymcoach.game.Contract
import com.gymcoach.game.Contracts
import com.gymcoach.game.CityState
import com.gymcoach.game.HeroStats
import com.gymcoach.game.Res
import com.gymcoach.game.TrainingTotals
import com.gymcoach.game.Realm
import com.gymcoach.game.RealmCalculator
import com.gymcoach.game.SetRecord
import com.gymcoach.game.WorkoutRecord
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class GymRepository(private val db: GymDatabase, saveDir: File) {
    private val dao = db.dao()

    // The village and weekly contracts are the only game state the player chooses, so they are saved
    // to their own small files and the workout database stays as it is.
    private val cityFile = File(saveDir, "city.txt")
    private val contractsFile = File(saveDir, "contracts.txt")

    private val contractChoices = MutableStateFlow(
        runCatching {
            contractsFile.readLines().mapNotNull { line ->
                val p = line.trim().split(' ')
                runCatching { LocalDate.parse(p[0]) to Contract.valueOf(p[1]) }.getOrNull()
            }.toMap()
        }.getOrDefault(emptyMap()),
    )

    /** Picks this week's contract. */
    suspend fun chooseContract(contract: Contract) {
        val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val next = contractChoices.value + (monday to contract)
        withContext(Dispatchers.IO) { contractsFile.writeText(next.entries.joinToString("\n") { "${it.key} ${it.value.name}" }) }
        contractChoices.value = next
    }

    private val cityState = MutableStateFlow(runCatching { City.decode(cityFile.readText()) }.getOrDefault(CityState()))
    val city: StateFlow<CityState> = cityState

    private val cityLock = Mutex()

    /** Applies a village action against the current training totals and saves the result. */
    suspend fun updateCity(action: (CityState, TrainingTotals) -> CityState) = cityLock.withLock {
        val next = action(cityState.value, gameNow().training)
        withContext(Dispatchers.IO) { cityFile.writeText(City.encode(next)) }
        cityState.value = next
    }

    /** Brings plunder home from Castle Defender. */
    suspend fun addPlunder(plunder: Res) = updateCity { state, _ -> state.copy(loot = state.loot + plunder) }

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
    fun sessionPlans(sessionId: Long): Flow<List<SessionExercise>> = dao.sessionPlans(sessionId)
    val activeProgram: Flow<Program?> = dao.activeProgram()

    private data class GameInputs(
        val sessions: List<WorkoutSession>,
        val sets: List<SetEntry>,
        val exercises: List<Exercise>,
        val goals: List<Goal>,
        val program: Program?,
    )

    /**
     * XP, level, rank, streaks, bonfire, contracts and quests, plus the campaign, voyage and village income,
     * recomputed whenever the log changes.
     */
    val game: Flow<GameState> = combine(
        combine(dao.finishedSessions(), dao.finishedSets(), dao.exercises(), dao.goals(), dao.activeProgram()) { a, b, c, d, e ->
            GameInputs(a, b, c, d, e)
        },
        contractChoices,
    ) { inputs, choices -> buildGame(inputs, choices) }

    val hero: Flow<HeroStats> = game.map { it.hero }

    suspend fun gameNow(): GameState = buildGame(
        GameInputs(dao.getFinishedSessions(), dao.getFinishedSets(), dao.allExercises(), dao.allGoals(), dao.getActiveProgram()),
        contractChoices.value,
    )

    private fun buildGame(inputs: GameInputs, choices: Map<LocalDate, Contract>): GameState {
        val (sessions, sets, exercises, goals, program) = inputs
        val cardio = exercises.filter { it.isCardio }.map { it.id }.toSet()
        val setsBySession = sets.groupBy { it.sessionId }
        val zone = ZoneId.systemDefault()
        val records = sessions.map { s ->
            WorkoutRecord(
                date = Instant.ofEpochMilli(s.startedAt).atZone(zone).toLocalDate(),
                inProgram = s.programId != null,
                sets = setsBySession[s.id].orEmpty().map {
                    SetRecord(it.exerciseId, it.weightKg, it.reps, it.exerciseId in cardio, it.distanceKm)
                },
            )
        }
        val weeklyTarget = goals.firstOrNull { it.type == GoalType.WEEKLY_SESSIONS && it.achievedAt == null }
            ?.target?.toInt()?.coerceAtLeast(1) ?: 3
        val today = LocalDate.now()
        val programStart = program?.let { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }

        // A calendar plan says which days are for training and which for rest.
        val planDays = mutableSetOf<LocalDate>()
        val restDays = mutableSetOf<LocalDate>()
        if (program != null && programStart != null && ProgramPlanner.isCalendarBased(program.focusEnum())) {
            for (i in 0 until ProgramPlanner.WEEKS * 7) {
                val date = programStart.plusDays(i.toLong())
                if (ProgramPlanner.onboardDay(i / 7 + 1, i % 7 + 1) == null) restDays += date else planDays += date
            }
        }
        val bonfire = BonfireCalculator.compute(records.map { it.date }, restDays, planDays, today)
        val contracts = BonfireCalculator.contracts(records, choices, weeklyTarget, today)

        val hero = HeroCalculator.compute(records, today, weeklyTarget, BonfireCalculator.bonusXp(bonfire, contracts))
        // The campaign follows the plan's weeks; without a plan it starts on the Monday of your first workout.
        val realm = RealmCalculator.compute(
            records,
            today,
            campaignStart = programStart,
            sessionsPerWeek = program?.let { ProgramPlanner.workoutsPerWeek(it.focusEnum(), it.daysPerWeek) } ?: weeklyTarget,
        )
        return GameState(
            hero = hero,
            realm = realm,
            trainedToday = records.any { it.date == today },
            training = TrainingTotals.of(records, contracts.completed),
            bonfire = bonfire,
            contracts = contracts,
            weeklyTarget = weeklyTarget,
        )
    }
    fun finishedProgramSessions(programId: Long): Flow<List<WorkoutSession>> = dao.finishedProgramSessions(programId)

    suspend fun seedIfEmpty() {
        val firstRun = dao.exerciseCount() == 0
        // App updates add new library exercises and refresh the how-to text of existing ones.
        for (seed in SeedExercises.all) {
            val existing = dao.getExerciseByName(seed.name)
            if (existing == null) dao.insertExercises(listOf(seed)) else if (existing != seed.copy(id = existing.id)) {
                dao.updateExercise(seed.copy(id = existing.id))
            }
        }
        if (firstRun) {
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
        val target = currentTarget(sessionId, exerciseId)
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

    /** Today's target: the program's planned target when this is a program workout, else the coach's base. */
    suspend fun currentTarget(sessionId: Long, exerciseId: Long): ExerciseTarget {
        val base = targetFor(exerciseId)
        val plan = dao.getSessionExercise(sessionId, exerciseId) ?: return base
        return base.withPlan(plan)
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
        refreshPlans(sessionId)
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
        val gameBefore = gameNow()
        dao.updateSession(session.copy(endedAt = System.currentTimeMillis()))

        val messages = mutableListOf<String>()
        val sessionProgram = session.programId?.let { dao.getProgram(it) }
        val week = session.programWeek
        if (sessionProgram != null && week != null && ProgramPlanner.isDeload(sessionProgram.focusEnum(), week)) {
            messages += "Deload week: your targets stay the same so your body can recover."
        } else {
            for (exerciseId in sets.map { it.exerciseId }.distinct()) {
                val ex = dao.getExercise(exerciseId) ?: continue
                val recent = dao.recentSetsForExercise(exerciseId, limit = 2)
                    .groupBy { it.sessionId }
                    .values
                    .map { group -> group.map { LoggedSet(it.weightKg, it.reps, it.targetReps, it.effort) } }
                val base = targetFor(exerciseId)
                val today = currentTarget(sessionId, exerciseId)
                val decision = Coach.evaluate(ex.toInfo(), today.toPrescription(), recent)
                // In a program the coach judges today's target and moves the base target by the same step.
                val p = decision.prescription
                val adjusted = decision.copy(
                    prescription = Prescription(
                        weightKg = maxOf(0.0, base.weightKg + (p.weightKg - today.weightKg)),
                        reps = maxOf(1, base.reps + (p.reps - today.reps)),
                        sets = maxOf(1, base.sets + (p.sets - today.sets)),
                    )
                )
                applyDecision(ex, adjusted, sessionId = null)
                messages += decision.message
            }
        }
        messages += programProgress(session)
        messages += checkGoals()
        val gameAfter = gameNow()
        val village = cityState.value
        val earned = City.treasury(village, gameAfter.training) - City.treasury(village, gameBefore.training)
        val celebration = HeroCalculator.celebrate(gameBefore.hero, gameAfter.hero) +
            BonfireCalculator.celebrate(gameBefore.bonfire, gameAfter.bonfire, gameBefore.contracts, gameAfter.contracts) +
            RealmCalculator.celebrate(gameBefore.realm, gameAfter.realm) +
            listOfNotNull(
                "Village: +${earned.gold} gold, +${earned.wood} wood, +${earned.stone} stone, +${earned.food} food."
                    .takeIf { !earned.isZero },
            )
        celebration.filter { !it.startsWith("+") && !it.startsWith("Strike!") && !it.startsWith("Village:") }.forEach {
            dao.insertCoachNote(CoachNote(message = it, createdAt = System.currentTimeMillis()))
        }
        return celebration + messages
    }

    // Programs

    suspend fun startProgram(daysPerWeek: Int, focus: ProgramFocus, startDate: LocalDate = LocalDate.now()) {
        dao.deactivatePrograms()
        val start = if (ProgramPlanner.isCalendarBased(focus)) startDate.toEpochMillis() else System.currentTimeMillis()
        val days = ProgramPlanner.workoutsPerWeek(focus, daysPerWeek)
        dao.insertProgram(Program(startedAt = start, daysPerWeek = days, focus = focus.name, active = true))
        val message = if (focus == ProgramFocus.ONBOARD) {
            "Your onboard plan starts ${startDate.format(DateTimeFormatter.ofPattern("d MMM"))}: 3 strength and 2 cardio sessions " +
                "every 7 days. Weeks 1–2 Learn, 3–4 Build, 5–6 Push. Missing a session costs nothing; training exhausted does."
        } else {
            "Your 6-week ${focus.label.lowercase()} program is ready: $days workouts a week. " +
                "Weeks 1 to 5 build up, week 6 is an easy recovery week."
        }
        dao.insertCoachNote(CoachNote(message = message, createdAt = System.currentTimeMillis()))
    }

    suspend fun endProgram() = dao.deactivatePrograms()

    /** Moves the whole onboard plan one day later, for a watch switch day. */
    suspend fun shiftProgram(program: Program) {
        val shifted = Instant.ofEpochMilli(program.startedAt).atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1)
        dao.updateProgram(program.copy(startedAt = shifted.toEpochMillis()))
    }

    fun programStatus(program: Program, finished: List<WorkoutSession>, today: LocalDate = LocalDate.now()): ProgramStatus {
        val focus = program.focusEnum()
        val done = finished.mapNotNull { s -> s.programWeek?.let { w -> s.programDay?.let { d -> w to d } } }.toSet()
        if (!ProgramPlanner.isCalendarBased(focus)) {
            val next = ProgramPlanner.plan(program.daysPerWeek, focus).firstOrNull { (it.week to it.day) !in done }
            return if (next == null) ProgramStatus.Finished else ProgramStatus.Next(next)
        }
        val start = Instant.ofEpochMilli(program.startedAt).atZone(ZoneId.systemDefault()).toLocalDate()
        val index = ChronoUnit.DAYS.between(start, today)
        if (index < 0) return ProgramStatus.NotStarted(start)
        val week = (index / 7).toInt() + 1
        val day = (index % 7).toInt() + 1
        if (week > ProgramPlanner.WEEKS) return ProgramStatus.Finished
        val plan = ProgramPlanner.onboardDay(week, day)
            ?: return ProgramStatus.Rest(week, day, ProgramPlanner.onboardRestNote(day))
        return if ((week to day) in done) ProgramStatus.DoneToday(plan) else ProgramStatus.Next(plan)
    }

    suspend fun currentProgramStatus(): ProgramStatus? {
        val program = dao.getActiveProgram() ?: return null
        return programStatus(program, dao.getFinishedProgramSessions(program.id))
    }

    /** Starts the next program workout with its exercises and this week's targets. Returns the session id. */
    suspend fun startProgramDay(): Long? {
        val program = dao.getActiveProgram() ?: return null
        val day = (programStatus(program, dao.getFinishedProgramSessions(program.id)) as? ProgramStatus.Next)?.plan
            ?: return null
        val sessionId = dao.insertSession(
            WorkoutSession(
                startedAt = System.currentTimeMillis(),
                programId = program.id,
                programWeek = day.week,
                programDay = day.day,
            )
        )
        day.exercises.forEachIndexed { i, name ->
            val ex = dao.getExerciseByName(name) ?: return@forEachIndexed
            dao.insertSessionExercise(SessionExercise(sessionId, ex.id, i))
        }
        refreshPlans(sessionId)
        return sessionId
    }

    /** Recomputes the planned targets of a program workout from the coach's current base targets. */
    private suspend fun refreshPlans(sessionId: Long) {
        val session = dao.getSession(sessionId) ?: return
        val week = session.programWeek ?: return
        val program = session.programId?.let { dao.getProgram(it) } ?: return
        val focus = program.focusEnum()
        dao.sessionPlansOnce(sessionId).forEach { se ->
            val ex = dao.getExercise(se.exerciseId) ?: return@forEach
            val planned = ProgramPlanner.planned(
                focus, week, ex.isBodyweight, ex.isCardio, targetFor(ex.id).toPrescription(), ex.name, ex.weightStepKg,
            )
            dao.updateSessionExercise(
                se.copy(plannedSets = planned.sets, plannedReps = planned.reps, plannedWeightKg = planned.weightKg)
            )
        }
    }

    private suspend fun programProgress(session: WorkoutSession): List<String> {
        val programId = session.programId ?: return emptyList()
        val program = dao.getActiveProgram()?.takeIf { it.id == programId } ?: return emptyList()
        if (ProgramPlanner.isCalendarBased(program.focusEnum())) {
            return if (session.programWeek == ProgramPlanner.WEEKS && session.programDay == 6) {
                listOf("That was the last session of your 6-week plan. Compare your log with week 1 and see how far you've come!")
            } else {
                emptyList()
            }
        }
        return when (val status = programStatus(program, dao.getFinishedProgramSessions(programId))) {
            is ProgramStatus.Finished -> {
                dao.deactivatePrograms()
                listOf("You finished your 6-week program! Start a new one to keep progressing; it will use your new weights.")
            }
            is ProgramStatus.Next ->
                if (status.plan.week != session.programWeek) {
                    listOf("Week ${session.programWeek} done. Next up: week ${status.plan.week}.")
                } else {
                    emptyList()
                }
            else -> emptyList()
        }
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

fun Exercise.toInfo() = ExerciseInfo(name, isBodyweight, easierName, harderName, isCardio, isTimed, weightStepKg)

/** Plank and other holds log seconds instead of reps. */
val Exercise.isTimed: Boolean get() = name in SeedExercises.TIMED

/** 2 kg per dumbbell, 2.5 kg for barbells and machines. */
val Exercise.weightStepKg: Double
    get() = if (equipment.contains("Dumbbell", ignoreCase = true)) Coach.DUMBBELL_STEP_KG else Coach.WEIGHT_STEP_KG

fun Program.focusEnum(): ProgramFocus =
    ProgramFocus.entries.firstOrNull { it.name == focus } ?: ProgramFocus.MUSCLE

private fun LocalDate.toEpochMillis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

sealed interface ProgramStatus {
    /** Onboard plan before its start date. */
    data class NotStarted(val startDate: LocalDate) : ProgramStatus
    /** The workout to do now. */
    data class Next(val plan: ProgramDayPlan) : ProgramStatus
    /** Onboard plan: today's session is already logged. */
    data class DoneToday(val plan: ProgramDayPlan) : ProgramStatus
    /** Onboard plan rest day. */
    data class Rest(val week: Int, val day: Int, val note: String) : ProgramStatus
    data object Finished : ProgramStatus
}

fun ExerciseTarget.toPrescription() = Prescription(weightKg, reps, sets)

/** The base target with a program workout's planned values on top, where set. */
fun ExerciseTarget.withPlan(plan: SessionExercise) = copy(
    weightKg = plan.plannedWeightKg ?: weightKg,
    reps = plan.plannedReps ?: reps,
    sets = plan.plannedSets ?: sets,
)

data class GameState(
    val hero: HeroStats,
    val realm: Realm,
    val trainedToday: Boolean,
    val training: TrainingTotals,
    val bonfire: Bonfire,
    val contracts: Contracts,
    val weeklyTarget: Int,
)
