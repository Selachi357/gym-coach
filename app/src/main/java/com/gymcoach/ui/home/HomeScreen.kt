package com.gymcoach.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcoach.data.GoalType
import com.gymcoach.ui.GymTopBar
import com.gymcoach.ui.LocalRepository
import com.gymcoach.ui.formatDateTime
import kotlinx.coroutines.launch
import com.gymcoach.ui.theme.RetroCard
import com.gymcoach.ui.theme.RetroButton

@Composable
fun HomeScreen(
    openWorkout: (Long) -> Unit,
    openGoals: () -> Unit,
    openProgram: () -> Unit,
    openHero: () -> Unit,
    openSettings: () -> Unit,
    openTimer: () -> Unit,
) {
    val repo = LocalRepository.current
    val scope = rememberCoroutineScope()
    val active by repo.activeSession.collectAsStateWithLifecycle(initialValue = null)
    val notes by repo.coachNotes(5).collectAsStateWithLifecycle(initialValue = emptyList())
    val thisWeek by repo.sessionsThisWeek().collectAsStateWithLifecycle(initialValue = 0)
    val goals by repo.goals.collectAsStateWithLifecycle(initialValue = emptyList())
    val weeklyGoal = goals.firstOrNull { it.type == GoalType.WEEKLY_SESSIONS && it.achievedAt == null }
    val openGoalCount = goals.count { it.achievedAt == null }
    val program by repo.activeProgram.collectAsStateWithLifecycle(initialValue = null)

    Column(Modifier.fillMaxSize()) {
        GymTopBar("Gym Coach", actions = {
            IconButton(onClick = openTimer) { Icon(Icons.Filled.Timer, contentDescription = "Timer") }
            IconButton(onClick = openSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
        })
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { com.gymcoach.ui.hero.HeroBanner(onClick = openHero) }
            item { TodayCard(program = program, openWorkout = openWorkout, openProgram = openProgram) }
            item {
                RetroCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        val session = active
                        Text(
                            if (session != null) "Workout in progress" else "Ready to train?",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        if (session != null) {
                            Text("Started ${formatDateTime(session.startedAt)}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.size(12.dp))
                        RetroButton(
                            onClick = {
                                if (session != null) openWorkout(session.id)
                                else scope.launch { openWorkout(repo.startSession()) }
                            },
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (session != null) "Continue workout" else if (program != null) "Free workout" else "Start workout")
                        }
                    }
                }
            }
            item {
                val p = program
                if (p == null) {
                    RetroCard(onClick = openProgram) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("Get a 6-week program", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "The coach plans every workout for you, built from the equipment on board.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                } else {
                    ProgramHomeCard(
                        program = p,
                        hasActiveSession = active != null,
                        onStart = { scope.launch { repo.startProgramDay()?.let(openWorkout) } },
                        openProgram = openProgram,
                    )
                }
            }
            item {
                RetroCard {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("This week", style = MaterialTheme.typography.titleMedium)
                        if (weeklyGoal != null) {
                            val target = weeklyGoal.target.toInt()
                            Text("$thisWeek of $target workouts")
                            Spacer(Modifier.size(8.dp))
                            LinearProgressIndicator(
                                progress = { (thisWeek.toFloat() / target).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Text("$thisWeek workout${if (thisWeek == 1) "" else "s"}")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (openGoalCount == 0) "No goals yet" else "$openGoalCount active goal${if (openGoalCount == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = openGoals) { Text(if (openGoalCount == 0) "Set a goal" else "View goals") }
                        }
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.SportsGymnastics, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    Text("Coach says", style = MaterialTheme.typography.titleMedium)
                }
            }
            items(notes, key = { it.id }) { note ->
                RetroCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(note.message, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            formatDateTime(note.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgramHomeCard(
    program: com.gymcoach.data.Program,
    hasActiveSession: Boolean,
    onStart: () -> Unit,
    openProgram: () -> Unit,
) {
    val repo = LocalRepository.current
    val finished by androidx.compose.runtime.remember(program.id) { repo.finishedProgramSessions(program.id) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val status = repo.programStatus(program, finished)
    val next = (status as? com.gymcoach.data.ProgramStatus.Next)?.plan
    val total = com.gymcoach.coach.ProgramPlanner.WEEKS * program.daysPerWeek
    RetroCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            val (title, detail) = when (status) {
                is com.gymcoach.data.ProgramStatus.Next ->
                    "Week ${status.plan.week} of 6 · ${status.plan.name}" to
                        "Today's exercises are at the top of this screen."
                is com.gymcoach.data.ProgramStatus.DoneToday ->
                    "Week ${status.plan.week} of 6 · done for today" to "${status.plan.name} is logged. Recover well."
                is com.gymcoach.data.ProgramStatus.Rest -> "Week ${status.week} of 6 · rest day" to status.note
                is com.gymcoach.data.ProgramStatus.NotStarted ->
                    "Plan starts ${status.startDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMM"))}" to
                        "Get familiar with the exercises and the guide until then."
                com.gymcoach.data.ProgramStatus.Finished -> "Program complete!" to "Start a new one to keep progressing."
            }
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.size(8.dp))
            LinearProgressIndicator(
                progress = { (finished.size.toFloat() / total).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("${finished.size} of $total workouts done", style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = openProgram) { Text("View plan") }
            }
        }
    }
}

private val todayFormat = java.time.format.DateTimeFormatter.ofPattern("EEE d MMM")

/**
 * Today's exercises, ready when the app opens. Ticking one makes the coach log it as planned;
 * unticking takes it back while the workout is still open.
 */
@Composable
private fun TodayCard(program: com.gymcoach.data.Program?, openWorkout: (Long) -> Unit, openProgram: () -> Unit) {
    val repo = LocalRepository.current
    val scope = rememberCoroutineScope()
    val finished by androidx.compose.runtime.remember(program?.id) {
        program?.let { repo.finishedProgramSessions(it.id) } ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val logged by androidx.compose.runtime.remember { repo.loggedToday() }.collectAsStateWithLifecycle(initialValue = emptySet())
    val active by repo.activeSession.collectAsStateWithLifecycle(initialValue = null)
    val plan = repo.todayPlan(program, finished)
    val day = plan.day
    val openToday = active?.takeIf { it.startedAt >= com.gymcoach.data.GymRepository.startOfDay() }
    var note by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var coach by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<List<String>?>(null) }

    RetroCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "TODAY · ${java.time.LocalDate.now().format(todayFormat).uppercase()}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            if (day == null) {
                Text("Rest day", style = MaterialTheme.typography.titleLarge)
                Text(plan.rest ?: "Recover well.", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            Text(day.name, style = MaterialTheme.typography.titleLarge)
            Text(
                (if (plan.fromProgram) "Week ${day.week} of 6 · ${plan.focus.label}" else "Coach's pick from your beginner plan") +
                    if (day.duration.isNotEmpty()) " · ${day.duration}" else "",
                style = MaterialTheme.typography.bodySmall,
            )
            com.gymcoach.ui.program.DayChecklist(
                focus = plan.focus,
                day = day,
                date = null,
                finished = false,
                checked = { it in logged },
                logged = { false },
                onToggle = { name ->
                    scope.launch {
                        note = null
                        if (name in logged) {
                            if (!repo.unlogToday(name)) note = "Today's workout is already saved. Change it in the Log tab."
                        } else {
                            repo.logPlanned(plan, name)
                        }
                    }
                },
                showHeader = false,
            )
            Text(
                "Tick an exercise and the coach logs every set as planned, rated 7 out of 10. Open the workout to change weights or reps.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            note?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                RetroButton(onClick = { scope.launch { repo.startToday(plan)?.let(openWorkout) } }) {
                    Text(if (openToday != null) "Open workout" else "Start")
                }
                if (openToday != null && day.exercises.any { it in logged }) {
                    com.gymcoach.ui.theme.RetroOutlinedButton(onClick = {
                        scope.launch { coach = repo.finishSession(openToday.id) }
                    }) { Text("Finish") }
                }
                Spacer(Modifier.weight(1f))
                if (!plan.fromProgram) TextButton(onClick = openProgram) { Text("Plans") }
            }
        }
    }

    coach?.let { messages ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { coach = null },
            title = { Text("Coach") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { messages.forEach { Text(it) } }
            },
            confirmButton = { TextButton(onClick = { coach = null }) { Text("OK") } },
        )
    }
}
