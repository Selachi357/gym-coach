package com.gymcoach.ui.program

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcoach.coach.ProgramFocus
import com.gymcoach.coach.ProgramPlanner
import com.gymcoach.data.Program
import com.gymcoach.ui.GymTopBar
import com.gymcoach.ui.LocalRepository
import kotlinx.coroutines.launch

/** Shows the active 6-week program, or the setup form when there is none. */
@Composable
fun ProgramScreen(back: () -> Unit) {
    val repo = LocalRepository.current
    val program by repo.activeProgram.collectAsStateWithLifecycle(initialValue = null)
    val p = program
    Column(Modifier.fillMaxSize()) {
        GymTopBar("6-week program", back = back)
        if (p == null) ProgramSetup() else ProgramOverview(p)
    }
}

@Composable
private fun ProgramSetup() {
    val repo = LocalRepository.current
    val scope = rememberCoroutineScope()
    var days by remember { mutableIntStateOf(3) }
    var focus by remember { mutableStateOf(ProgramFocus.MUSCLE) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                "The coach builds a 6-week plan from your gym's equipment. Weeks 1 to 5 get gradually harder, " +
                    "week 6 is an easy recovery week. Weights follow your coach targets, so they keep adjusting as you go.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        item {
            Text("Workouts per week", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProgramPlanner.DAY_OPTIONS.forEach { n ->
                    FilterChip(selected = days == n, onClick = { days = n }, label = { Text("$n days") })
                }
            }
            Text(
                ProgramPlanner.templates(days).joinToString(" · ") { it.first },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        item { Text("Focus", style = MaterialTheme.typography.titleMedium) }
        items(ProgramFocus.entries.toList()) { f ->
            Card(
                onClick = { focus = f },
                colors = CardDefaults.cardColors(
                    containerColor = if (focus == f) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(f.label, style = MaterialTheme.typography.titleMedium)
                    Text(f.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Button(
                onClick = { scope.launch { repo.startProgram(days, focus) } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Create my program") }
        }
    }
}

@Composable
private fun ProgramOverview(program: Program) {
    val repo = LocalRepository.current
    val scope = rememberCoroutineScope()
    val finished by remember(program.id) { repo.finishedProgramSessions(program.id) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val focus = ProgramFocus.entries.firstOrNull { it.name == program.focus } ?: ProgramFocus.MUSCLE
    val done = finished.mapNotNull { s -> s.programWeek?.let { w -> s.programDay?.let { d -> w to d } } }.toSet()
    val next = repo.nextProgramDay(program, finished)
    var confirmEnd by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "${focus.label} · ${program.daysPerWeek} workouts a week · ${done.size} of ${ProgramPlanner.WEEKS * program.daysPerWeek} done",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        items((1..ProgramPlanner.WEEKS).toList()) { week ->
            val scheme = ProgramPlanner.scheme(focus, week)
            val current = next?.week == week
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (current) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    val title = if (week == ProgramPlanner.DELOAD_WEEK) "Week $week · Recovery" else "Week $week"
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text("${scheme.sets} × ${scheme.reps} on weights. ${scheme.note}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.size(8.dp))
                    ProgramPlanner.templates(program.daysPerWeek).forEachIndexed { i, (name, exercises) ->
                        val isDone = (week to i + 1) in done
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isDone) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = if (isDone) "Done" else "To do",
                                tint = if (isDone) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(name, style = MaterialTheme.typography.bodyLarge)
                                if (current) {
                                    Text(exercises.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            TextButton(onClick = { confirmEnd = true }, modifier = Modifier.fillMaxWidth()) {
                Text("End program", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            title = { Text("End this program?") },
            text = { Text("Your logged workouts and coach targets stay. You can start a new program any time.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmEnd = false
                    scope.launch { repo.endProgram() }
                }) { Text("End program") }
            },
            dismissButton = { TextButton(onClick = { confirmEnd = false }) { Text("Cancel") } },
        )
    }
}
