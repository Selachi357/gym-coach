package com.gymcoach.ui.program

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcoach.coach.ProgramFocus
import com.gymcoach.coach.ProgramDayPlan
import com.gymcoach.coach.ProgramPlanner
import com.gymcoach.coach.formatKg
import com.gymcoach.data.Program
import com.gymcoach.data.ProgramStatus
import com.gymcoach.data.ProgramTicks
import com.gymcoach.data.focusEnum
import com.gymcoach.data.isTimed
import com.gymcoach.data.toPrescription
import com.gymcoach.data.weightStepKg
import com.gymcoach.ui.formatTarget
import com.gymcoach.ui.hero.Retro
import com.gymcoach.ui.theme.RetroCheckbox
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

/** The onboard plan's first day from the PDF, the beginner plan's next Monday, or today once that date has passed. */
private fun defaultStartDate(focus: ProgramFocus): LocalDate {
    val today = LocalDate.now()
    if (focus == ProgramFocus.BEGINNER) return today.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY))
    val planned = LocalDate.of(2026, 11, 11)
    return if (planned.isBefore(today)) today else planned
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
    var startDate by remember(focus) { mutableStateOf(defaultStartDate(focus)) }
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
        if (ProgramPlanner.isCalendarBased(focus)) {
            item {
                Text("First day", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (focus == ProgramFocus.BEGINNER) "Day 1 is Strength A, best on a Monday. The plan runs 6 weeks from here."
                    else "Day 1 of the 7-day cycle. The plan runs 6 weeks from here.",
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
    val ticks by remember(program.id) { repo.programTicks(program.id) }.collectAsStateWithLifecycle(initialValue = ProgramTicks())
    // Weeks opened or closed by hand; the current week starts open.
    val expanded = remember { mutableStateMapOf<Int, Boolean>() }

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
            val open = expanded[week] ?: (week == (currentWeek ?: 1))
            val weekDays = plan[week].orEmpty()
            RetroCard(
                colors = CardDefaults.cardColors(
                    containerColor = if (current) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { expanded[week] = !open },
                    ) {
                        Column(Modifier.weight(1f)) {
                            val title = when {
                                ProgramPlanner.isDeload(focus, week) -> "Week $week · Recovery"
                                scheme.phase.isNotEmpty() -> "Week $week · ${scheme.phase}"
                                else -> "Week $week"
                            }
                            Text(title + if (current) " · now" else "", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${weekDays.count { (week to it.day) in done }} of ${weekDays.size} workouts done",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        Icon(
                            if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (open) "Hide days" else "Show days",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (open) {
                        val setsText = if (calendar) "${scheme.sets} sets per exercise." else "${scheme.sets} × ${scheme.reps} on weights."
                        Text("$setsText ${scheme.note}", style = MaterialTheme.typography.bodyMedium)
                        val dayNumbers = if (calendar) (1..7).toList() else weekDays.map { it.day }
                        dayNumbers.forEach { dayNumber ->
                            Spacer(Modifier.size(12.dp))
                            val date = if (calendar) start.plusDays((week - 1) * 7L + dayNumber - 1) else null
                            val day = weekDays.firstOrNull { it.day == dayNumber }
                            if (day == null) {
                                RestDayRow(dayNumber, date, ProgramPlanner.restNote(focus, dayNumber))
                            } else {
                                DayChecklist(
                                    focus = focus,
                                    day = day,
                                    date = date,
                                    finished = (week to day.day) in done,
                                    checked = { ticks.isTicked(day.week, day.day, it) },
                                    logged = { ticks.isLogged(day.week, day.day, it) },
                                    onToggle = { name ->
                                        if (!ticks.isLogged(day.week, day.day, name)) {
                                            scope.launch { repo.toggleCheck(program.id, day.week, day.day, name) }
                                        }
                                    },
                                )
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

@Composable
private fun RestDayRow(dayNumber: Int, date: LocalDate?, note: String) {
    Column(
        Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant).padding(10.dp),
    ) {
        Text(
            listOfNotNull("Day $dayNumber", date?.format(dayMonth), "Rest").joinToString(" · ").uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = Retro.dim,
        )
        Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * One plan day as a checklist: every exercise on its own line with its target and a box to tick.
 * [logged] exercises have sets in the log; their box shows a softer tick.
 */
@Composable
fun DayChecklist(
    focus: ProgramFocus,
    day: ProgramDayPlan,
    date: LocalDate?,
    finished: Boolean,
    checked: (String) -> Boolean,
    logged: (String) -> Boolean,
    onToggle: (String) -> Unit,
    showHeader: Boolean = true,
) {
    val repo = LocalRepository.current
    val exercises by repo.exercises.collectAsStateWithLifecycle(initialValue = emptyList())
    val targets by repo.targets.collectAsStateWithLifecycle(initialValue = emptyList())
    val byName = exercises.associateBy { it.name }
    val targetMap = targets.associateBy { it.exerciseId }
    val today = date == LocalDate.now()
    val tickedCount = day.exercises.count(checked)

    Column(
        Modifier
            .fillMaxWidth()
            .border(if (today) 2.dp else 1.dp, if (today) Retro.gold else MaterialTheme.colorScheme.outlineVariant)
            .background(if (today) Retro.deep else androidx.compose.ui.graphics.Color.Transparent)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (showHeader) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    listOfNotNull("Day ${day.day}", date?.format(dayMonth)).joinToString(" · ").uppercase() +
                        if (today) " · TODAY" else "",
                    style = MaterialTheme.typography.labelLarge,
                    color = Retro.gold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (finished) "DONE" else "$tickedCount/${day.exercises.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (finished || tickedCount == day.exercises.size) Retro.xp else Retro.dim,
                )
            }
            Text(
                day.name + if (day.duration.isNotEmpty()) " · ${day.duration}" else "",
                style = MaterialTheme.typography.titleSmall,
            )
        }
        day.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        day.exercises.forEach { name ->
            val ticked = checked(name)
            val isLogged = logged(name)
            val ex = byName[name]
            val target = ex?.let {
                val base = (targetMap[it.id] ?: repo.defaultTarget(it)).toPrescription()
                val p = ProgramPlanner.planned(focus, day.week, it.isBodyweight, it.isCardio, base, it.name, it.weightStepKg, day.day)
                val range = ProgramPlanner.repRange(focus, name)
                if (range != null && !it.isCardio) {
                    "${p.sets} × $range" + if (p.weightKg > 0) " @ ${formatKg(p.weightKg)} kg" else ""
                } else {
                    formatTarget(p.sets, p.reps, p.weightKg, it.isCardio, it.isTimed)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(name) }
                    .padding(vertical = 4.dp),
            ) {
                RetroCheckbox(checked = ticked, faded = false)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (ticked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (ticked) TextDecoration.LineThrough else null,
                    )
                    val detail = listOfNotNull(target, if (isLogged) "logged" else null).joinToString(" · ")
                    if (detail.isNotEmpty()) {
                        Text(detail, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
