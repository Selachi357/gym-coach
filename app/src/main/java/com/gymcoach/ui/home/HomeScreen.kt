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
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@Composable
fun HomeScreen(openWorkout: (Long) -> Unit, openGoals: () -> Unit, openProgram: () -> Unit) {
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
        GymTopBar("Gym Coach")
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
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
                        Button(
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
                    Card(onClick = openProgram) {
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
                Card {
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
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
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
    val next = repo.nextProgramDay(program, finished)
    val total = com.gymcoach.coach.ProgramPlanner.WEEKS * program.daysPerWeek
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                if (next != null) "Week ${next.week} of 6 · ${next.name}" else "Program complete!",
                style = MaterialTheme.typography.titleMedium,
            )
            if (next != null) {
                Text(next.exercises.joinToString(", "), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.size(8.dp))
            LinearProgressIndicator(
                progress = { (finished.size.toFloat() / total).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("${finished.size} of $total workouts done", style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (next != null && !hasActiveSession) {
                    Button(onClick = onStart) { Text("Start today's workout") }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = openProgram) { Text("View plan") }
            }
        }
    }
}
