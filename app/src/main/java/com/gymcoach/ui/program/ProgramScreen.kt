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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.gymcoach.data.ProgramStatus
import com.gymcoach.data.focusEnum
import com.gymcoach.ui.GymTopBar
import com.gymcoach.ui.LocalRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import com.gymcoach.ui.theme.RetroCard
import com.gymcoach.ui.theme.RetroButton
import com.gymcoach.ui.theme.RetroOutlinedButton

private val dayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM")

/** The onboard plan's first day from the PDF, or today once that date has passed. */
private fun defaultStartDate(): LocalDate {
    val planned = LocalDate.of(2026, 11, 11)
    return if (planned.isBefore(LocalDate.now())) LocalDate.now() else planned
}

/** Shows the active 6-week program, or the setup form when there is none. */
@Composable
fun ProgramScreen(back: () -> Unit, openGuide: () -> Unit) {
    val repo = LocalRepository.current
    val program by repo.activeProgram.collectAsStateWithLifecycle(initialValue = null)
    val p = program
    Column(Modifier.fillMaxSize()) {
        GymTopBar("6-week program", back = back, actions = { TextButton(onClick = openGuide) { Text("Guide") } })
        if (p == null) ProgramSetup() else ProgramOverview(p)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgramSetup() {
    val repo = LocalRepository.current
    val scope = rememberCoroutineScope()
    var days by remember { mutableIntStateOf(3) }
    var focus by remember { mutableStateOf(ProgramFocus.ONBOARD) }
    var startDate by remember { mutableStateOf(defaultStartDate()) }
    var pickDate by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text("Choose a plan", style = MaterialTheme.typography.titleMedium) }
        items(ProgramFocus.entries.toList()) { f ->
            RetroCard(
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
        if (focus == ProgramFocus.ONBOARD) {
            item {
                Text("First day", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Day 1 of the 7-day cycle. The plan runs 6 weeks from here.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                RetroOutlinedButton(onClick = { pickDate = true }) { Text(startDate.format(dayMonth)) }
            }
        } else {
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
        }
        item {
            RetroButton(
                onClick = { scope.launch { repo.startProgram(days, focus, startDate) } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Create my program") }
        }
    }

    if (pickDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        startDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    pickDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun ProgramOverview(program: Program) {
    val repo = LocalRepository.current
    val scope = rememberCoroutineScope()
    val finished by remember(program.id) { repo.finishedProgramSessions(program.id) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val focus = program.focusEnum()
    val calendar = ProgramPlanner.isCalendarBased(focus)
    val done = finished.mapNotNull { s -> s.programWeek?.let { w -> s.programDay?.let { d -> w to d } } }.toSet()
    val status = repo.programStatus(program, finished)
    val currentWeek = when (status) {
        is ProgramStatus.Next -> status.plan.week
        is ProgramStatus.DoneToday -> status.plan.week
        is ProgramStatus.Rest -> status.week
        else -> null
    }
    val plan = ProgramPlanner.plan(program.daysPerWeek, focus).groupBy { it.week }
    val start = Instant.ofEpochMilli(program.startedAt).atZone(ZoneId.systemDefault()).toLocalDate()
    var confirmEnd by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${focus.label} · ${program.daysPerWeek} sessions a week · ${done.size} of ${ProgramPlanner.WEEKS * program.daysPerWeek} done",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (calendar) {
                    Text(
                        "Started ${start.format(dayMonth)}, ends ${start.plusDays(ProgramPlanner.WEEKS * 7L - 1).format(dayMonth)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        when (status) {
                            is ProgramStatus.NotStarted -> "Starts ${status.startDate.format(dayMonth)}."
                            is ProgramStatus.Next -> "Today: day ${status.plan.day}, ${status.plan.name}."
                            is ProgramStatus.DoneToday -> "Today's ${status.plan.name} is done. Nice work."
                            is ProgramStatus.Rest -> "Today: day ${status.day}. ${status.note}"
                            ProgramStatus.Finished -> "The 6 weeks are done."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    RetroOutlinedButton(onClick = { scope.launch { repo.shiftProgram(program) } }) {
                        Text("Shift one day (watch switch)")
                    }
                }
            }
        }
        items((1..ProgramPlanner.WEEKS).toList()) { week ->
            val scheme = ProgramPlanner.scheme(focus, week)
            val current = currentWeek == week
            RetroCard(
                colors = CardDefaults.cardColors(
                    containerColor = if (current) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    val title = when {
                        ProgramPlanner.isDeload(focus, week) -> "Week $week · Recovery"
                        scheme.phase.isNotEmpty() -> "Week $week · ${scheme.phase}"
                        else -> "Week $week"
                    }
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    val setsText = if (calendar) "${scheme.sets} sets per exercise." else "${scheme.sets} × ${scheme.reps} on weights."
                    Text("$setsText ${scheme.note}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.size(8.dp))
                    plan[week].orEmpty().forEach { day ->
                        val isDone = (week to day.day) in done
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isDone) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = if (isDone) "Done" else "To do",
                                tint = if (isDone) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                val label = if (calendar) {
                                    val date = start.plusDays((week - 1) * 7L + day.day - 1)
                                    "Day ${day.day} · ${date.format(dayMonth)} · ${day.name}"
                                } else {
                                    day.name
                                }
                                Text(label, style = MaterialTheme.typography.bodyLarge)
                                if (current) {
                                    Text(
                                        day.exercises.joinToString(", ") + if (day.duration.isNotEmpty()) " · ${day.duration}" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
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
