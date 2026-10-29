package com.gymcoach.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import com.gymcoach.ui.theme.RetroCard
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcoach.coach.formatKg
import com.gymcoach.data.Exercise
import com.gymcoach.data.Goal
import com.gymcoach.data.GoalType
import com.gymcoach.ui.GymTopBar
import com.gymcoach.ui.LocalRepository
import com.gymcoach.ui.formatDate
import kotlinx.coroutines.launch

@Composable
fun GoalsScreen() {
    val repo = LocalRepository.current
    val scope = rememberCoroutineScope()
    val goals by repo.goals.collectAsStateWithLifecycle(initialValue = emptyList())
    val exercises by repo.exercises.collectAsStateWithLifecycle(initialValue = emptyList())
    val best by repo.bestWeights.collectAsStateWithLifecycle(initialValue = emptyList())
    val thisWeek by repo.sessionsThisWeek().collectAsStateWithLifecycle(initialValue = 0)
    val bestMap = best.associate { it.exerciseId to it.best }
    val exerciseMap = exercises.associateBy { it.id }
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { GymTopBar("Goals") },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New goal") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (goals.isEmpty()) {
                item {
                    Text(
                        "Set a goal, like lifting a certain weight or training a number of times per week. " +
                            "The coach tracks your progress and tells you when you get there.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            items(goals, key = { it.id }) { goal ->
                val (title, current) = when (goal.type) {
                    GoalType.STRENGTH -> {
                        val name = goal.exerciseId?.let { exerciseMap[it]?.name } ?: "Exercise"
                        "$name: ${formatKg(goal.target)} kg" to (goal.exerciseId?.let { bestMap[it] } ?: 0.0)
                    }
                    GoalType.WEEKLY_SESSIONS -> "${goal.target.toInt()} workouts per week" to thisWeek.toDouble()
                }
                GoalCard(goal, title, current, onDelete = { scope.launch { repo.deleteGoal(goal) } })
            }
        }
    }

    if (showAdd) {
        AddGoalDialog(
            exercises = exercises.filter { !it.isBodyweight },
            onDismiss = { showAdd = false },
            onAddStrength = { id, kg -> scope.launch { repo.addStrengthGoal(id, kg) }; showAdd = false },
            onAddWeekly = { n -> scope.launch { repo.addWeeklyGoal(n) }; showAdd = false },
        )
    }
}

@Composable
private fun GoalCard(goal: Goal, title: String, current: Double, onDelete: () -> Unit) {
    val achieved = goal.achievedAt != null
    val progress = if (achieved) 1f else (current / goal.target).toFloat().coerceIn(0f, 1f)
    RetroCard {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (achieved) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Achieved", tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.size(8.dp))
                }
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete goal") }
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.size(6.dp))
            val detail = when {
                achieved -> "Reached on ${formatDate(goal.achievedAt!!)}"
                goal.type == GoalType.STRENGTH -> "Best so far: ${formatKg(current)} kg"
                else -> "This week: ${current.toInt()}"
            }
            Text(detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalDialog(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onAddStrength: (Long, Double) -> Unit,
    onAddWeekly: (Int) -> Unit,
) {
    var strength by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<Exercise?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var valueText by remember { mutableStateOf("") }
    val value = valueText.replace(',', '.').toDoubleOrNull()
    val valid = value != null && value > 0 && (!strength || selected != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = strength, onClick = { strength = true }, label = { Text("Lift a weight") })
                    FilterChip(selected = !strength, onClick = { strength = false }, label = { Text("Train often") })
                }
                if (strength) {
                    ExposedDropdownMenuBox(expanded = menuOpen, onExpandedChange = { menuOpen = it }) {
                        OutlinedTextField(
                            value = selected?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Exercise") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuOpen) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            modifier = Modifier.heightIn(max = 320.dp),
                        ) {
                            exercises.forEach { ex ->
                                DropdownMenuItem(
                                    text = { Text(ex.name) },
                                    onClick = { selected = ex; menuOpen = false },
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it },
                    label = { Text(if (strength) "Target weight (kg)" else "Workouts per week") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (strength) KeyboardType.Decimal else KeyboardType.Number,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    val v = value
                    if (v != null) {
                        if (strength) selected?.let { onAddStrength(it.id, v) } else onAddWeekly(v.toInt().coerceAtLeast(1))
                    }
                },
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
