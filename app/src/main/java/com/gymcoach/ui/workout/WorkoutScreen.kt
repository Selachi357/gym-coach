package com.gymcoach.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcoach.coach.ProgramFocus
import com.gymcoach.coach.ProgramPlanner
import com.gymcoach.coach.effortLabel
import com.gymcoach.data.focusEnum
import com.gymcoach.data.isTimed
import com.gymcoach.data.withPlan
import com.gymcoach.ui.guide.Guide
import com.gymcoach.coach.formatKg
import com.gymcoach.data.Exercise
import com.gymcoach.data.ExerciseTarget
import com.gymcoach.data.SetEntry
import com.gymcoach.ui.GymTopBar
import com.gymcoach.ui.LocalRepository
import com.gymcoach.ui.describeSet
import com.gymcoach.ui.formatTarget
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.gymcoach.ui.theme.RetroButton
import com.gymcoach.ui.theme.RetroCard
import com.gymcoach.ui.theme.RetroOutlinedButton

@Composable
fun WorkoutScreen(
    sessionId: Long,
    addExercise: () -> Unit,
    openExercise: (Long) -> Unit,
    openTimer: () -> Unit,
    close: () -> Unit,
) {
    val repo = LocalRepository.current
    val scope = rememberCoroutineScope()
    val exercises by repo.sessionExercises(sessionId).collectAsStateWithLifecycle(initialValue = emptyList())
    val sets by repo.setsForSession(sessionId).collectAsStateWithLifecycle(initialValue = emptyList())
    val targets by repo.targets.collectAsStateWithLifecycle(initialValue = emptyList())
    val targetMap = targets.associateBy { it.exerciseId }
    val plans by repo.sessionPlans(sessionId).collectAsStateWithLifecycle(initialValue = emptyList())
    val planMap = plans.associateBy { it.exerciseId }
    val session by repo.session(sessionId).collectAsStateWithLifecycle(initialValue = null)
    val program by repo.activeProgram.collectAsStateWithLifecycle(initialValue = null)
    val week = session?.programWeek
    val dayPlan = session?.let { s ->
        program?.takeIf { it.id == s.programId }?.let { p ->
            ProgramPlanner.plan(p.daysPerWeek, p.focusEnum()).firstOrNull { it.week == s.programWeek && it.day == s.programDay }
        }
    }
    val focus = program?.focusEnum()
    val onboard = focus == ProgramFocus.ONBOARD && dayPlan != null

    var coachMessages by remember { mutableStateOf<List<String>?>(null) }
    var closeAfterMessages by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        GymTopBar(
            title = dayPlan?.let { "Week ${it.week} · ${it.name}" } ?: "Workout",
            back = close,
            actions = {
                TextButton(onClick = {
                    scope.launch {
                        coachMessages = repo.finishSession(sessionId)
                        closeAfterMessages = true
                    }
                }) { Text("Finish") }
            },
        )
        Box(Modifier.padding(horizontal = 16.dp).padding(top = 8.dp)) {
            com.gymcoach.ui.timer.RestTimerBar(openTimer)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (week != null && focus != null && dayPlan != null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val isStrength = dayPlan.exercises.size > 1
                        if (isStrength || !onboard) {
                            Text(
                                ProgramPlanner.scheme(focus, week).note,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        dayPlan.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        if (onboard && isStrength) {
                            Text(
                                "Warm-up first (8 min): ${Guide.WARM_UP.joinToString(" ")} " +
                                    "Rest 90 s between sets of big lifts, 60 s for the rest.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
            if (exercises.isEmpty()) {
                item {
                    Text(
                        "Add your first exercise. The coach will suggest weight and reps, " +
                            "and you rate how hard each set felt.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            items(exercises, key = { it.id }) { exercise ->
                ExerciseCard(
                    exercise = exercise,
                    repRange = if (onboard) ProgramPlanner.onboardRepRange[exercise.name] else null,
                    target = (targetMap[exercise.id] ?: repo.defaultTarget(exercise)).let { base ->
                        planMap[exercise.id]?.let { base.withPlan(it) } ?: base
                    },
                    sets = sets.filter { it.exerciseId == exercise.id },
                    onLog = { weight, reps, effort, distance ->
                        scope.launch { repo.logSet(sessionId, exercise.id, weight, reps, effort, distance) }
                        // Rest starts by itself after a strength set.
                        if (!exercise.isCardio) com.gymcoach.ui.timer.Timers.startRest()
                    },
                    onDeleteSet = { set -> scope.launch { repo.deleteSet(set) } },
                    onTooHard = {
                        scope.launch { coachMessages = listOf(repo.tooHard(sessionId, exercise.id)) }
                    },
                    onInfo = { openExercise(exercise.id) },
                    onRemove = { scope.launch { repo.removeExerciseFromSession(sessionId, exercise.id) } },
                )
            }
            item {
                RetroButton(onClick = addExercise, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add exercise")
                }
            }
            item {
                TextButton(onClick = { confirmDiscard = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Discard workout", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    coachMessages?.let { messages ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Coach") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    messages.forEach { Text(it) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    coachMessages = null
                    if (closeAfterMessages) close()
                }) { Text("OK") }
            },
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard workout?") },
            text = { Text("All sets from this workout will be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    scope.launch {
                        repo.discardSession(sessionId)
                        close()
                    }
                }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ExerciseCard(
    exercise: Exercise,
    repRange: String?,
    target: ExerciseTarget,
    sets: List<SetEntry>,
    onLog: (weightKg: Double, reps: Int, effort: Int, distanceKm: Double) -> Unit,
    onDeleteSet: (SetEntry) -> Unit,
    onTooHard: () -> Unit,
    onInfo: () -> Unit,
    onRemove: () -> Unit,
) {
    var weightText by remember(target.weightKg) { mutableStateOf(formatKg(target.weightKg)) }
    var repsText by remember(target.reps) { mutableStateOf(target.reps.toString()) }
    var effort by remember { mutableFloatStateOf(7f) }
    var distanceText by remember { mutableStateOf("") }
    val cardio = exercise.isCardio
    val weight = if (cardio) 0.0 else weightText.replace(',', '.').toDoubleOrNull()
    val distance = if (distanceText.isBlank()) 0.0 else distanceText.replace(',', '.').toDoubleOrNull()
    val reps = repsText.toIntOrNull()

    RetroCard {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Coach target: ${formatTarget(target.sets, target.reps, target.weightKg, cardio, exercise.isTimed)}" +
                            (repRange?.let { " (range $it)" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = onInfo) { Icon(Icons.Filled.Info, contentDescription = "How to") }
                if (sets.isEmpty()) {
                    IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, contentDescription = "Remove") }
                }
            }

            if (sets.isNotEmpty()) {
                Spacer(Modifier.size(8.dp))
                sets.forEachIndexed { index, set ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${if (cardio) "Bout" else "Set"} ${index + 1}: ${describeSet(exercise, set)} · ${effortLabel(set.effort)} (${set.effort}/10)",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        IconButton(onClick = { onDeleteSet(set) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Delete set", modifier = Modifier.size(16.dp))
                        }
                    }
                }
                if (!cardio) Text(
                    "${sets.size} of ${target.sets} sets done",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (cardio) {
                    OutlinedTextField(
                        value = distanceText,
                        onValueChange = { distanceText = it },
                        label = { Text("Distance km") },
                        singleLine = true,
                        isError = distance == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text(if (exercise.isBodyweight) "Extra kg" else "kg") },
                        singleLine = true,
                        isError = weight == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = repsText,
                    onValueChange = { repsText = it.filter(Char::isDigit) },
                    label = { Text(if (cardio) "Minutes" else if (exercise.isTimed) "Seconds" else "Reps") },
                    singleLine = true,
                    isError = reps == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.size(8.dp))
            val effortInt = effort.roundToInt()
            Text("How hard was it? $effortInt/10 · ${effortLabel(effortInt)}", style = MaterialTheme.typography.bodyMedium)
            Slider(value = effort, onValueChange = { effort = it }, valueRange = 1f..10f, steps = 8)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RetroButton(
                    onClick = { if (weight != null && reps != null && distance != null) onLog(weight, reps, effortInt, distance) },
                    enabled = weight != null && distance != null && reps != null && reps > 0,
                    modifier = Modifier.weight(1f),
                ) { Text(if (cardio) "Log" else "Log set") }
                RetroOutlinedButton(onClick = onTooHard, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.SentimentDissatisfied, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Too hard")
                }
            }
        }
    }
}

@Composable
fun ExercisePickerScreen(sessionId: Long, done: () -> Unit) {
    val repo = LocalRepository.current
    val scope = rememberCoroutineScope()
    val exercises by repo.exercises.collectAsStateWithLifecycle(initialValue = emptyList())
    val inSession by repo.sessionExercises(sessionId).collectAsStateWithLifecycle(initialValue = emptyList())
    val inSessionIds = inSession.map { it.id }.toSet()
    var query by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        GymTopBar("Add exercise", back = done)
        com.gymcoach.ui.exercises.ExerciseList(
            exercises = exercises.filter { it.id !in inSessionIds },
            query = query,
            onQueryChange = { query = it },
            onClick = { ex ->
                scope.launch {
                    repo.addExerciseToSession(sessionId, ex.id)
                    done()
                }
            },
        )
    }
}

