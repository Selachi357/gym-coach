package com.gymcoach.ui.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcoach.coach.effortLabel
import com.gymcoach.coach.formatKg
import com.gymcoach.data.Exercise
import com.gymcoach.data.isTimed
import com.gymcoach.ui.GymTopBar
import com.gymcoach.ui.LocalRepository
import com.gymcoach.ui.describeSet
import com.gymcoach.ui.formatDate
import com.gymcoach.ui.formatTarget

@Composable
fun ExercisesScreen(openExercise: (Long) -> Unit) {
    val repo = LocalRepository.current
    val exercises by repo.exercises.collectAsStateWithLifecycle(initialValue = emptyList())
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        GymTopBar("Exercises")
        ExerciseList(exercises, query, { query = it }, onClick = { openExercise(it.id) })
    }
}

/** Searchable exercise list grouped by muscle group. */
@Composable
fun ExerciseList(
    exercises: List<Exercise>,
    query: String,
    onQueryChange: (String) -> Unit,
    onClick: (Exercise) -> Unit,
) {
    val filtered = exercises.filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true) || it.muscleGroup.contains(query, ignoreCase = true)
    }
    val groups = filtered.groupBy { it.muscleGroup }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search exercises or muscles") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }
        groups.forEach { (group, list) ->
            item(key = "header-$group") {
                Text(
                    group,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                )
            }
            items(list, key = { it.id }) { ex ->
                ListItem(
                    headlineContent = { Text(ex.name) },
                    supportingContent = { Text(ex.equipment) },
                    modifier = Modifier.clickable { onClick(ex) },
                )
            }
        }
    }
}

@Composable
fun ExerciseDetailScreen(exerciseId: Long, openExercise: (Long) -> Unit, back: () -> Unit) {
    val repo = LocalRepository.current
    val exercise by repo.exercise(exerciseId).collectAsStateWithLifecycle(initialValue = null)
    val all by repo.exercises.collectAsStateWithLifecycle(initialValue = emptyList())
    val targets by repo.targets.collectAsStateWithLifecycle(initialValue = emptyList())
    val history by repo.setsForExercise(exerciseId).collectAsStateWithLifecycle(initialValue = emptyList())
    val ex = exercise

    Column(Modifier.fillMaxSize()) {
        GymTopBar(ex?.name ?: "", back = back)
        if (ex == null) return@Column
        val target = targets.firstOrNull { it.exerciseId == ex.id } ?: repo.defaultTarget(ex)
        val byName = all.associateBy { it.name }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("${ex.muscleGroup} · ${ex.equipment}", style = MaterialTheme.typography.bodyLarge)

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Your coach target", style = MaterialTheme.typography.labelLarge)
                    Text(formatTarget(target.sets, target.reps, target.weightKg, ex.isCardio, ex.isTimed), style = MaterialTheme.typography.headlineSmall)
                }
            }

            Section("How to do it") {
                ex.instructions.lines().filter { it.isNotBlank() }.forEachIndexed { i, step ->
                    Text("${i + 1}. $step", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Section("Tips") {
                ex.tips.lines().filter { it.isNotBlank() }.forEach { tip ->
                    Text("• $tip", style = MaterialTheme.typography.bodyMedium)
                }
            }

            val easier = ex.easierName?.let { byName[it] }
            val harder = ex.harderName?.let { byName[it] }
            if (easier != null || harder != null) {
                Section("Variations") {
                    Column {
                        easier?.let { AssistChip(onClick = { openExercise(it.id) }, label = { Text("Easier: ${it.name}") }) }
                        harder?.let { AssistChip(onClick = { openExercise(it.id) }, label = { Text("Harder: ${it.name}") }) }
                    }
                }
            }

            Section("History") {
                if (history.isEmpty()) {
                    Text("No sets logged yet.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    val best = history.maxOf { it.weightKg }
                    if (best > 0) Text("Heaviest: ${formatKg(best)} kg", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.size(4.dp))
                    history.groupBy { it.sessionId }.values.take(10).forEach { sessionSets ->
                        val first = sessionSets.minBy { it.createdAt }
                        val summary = sessionSets.sortedBy { it.createdAt }.joinToString(", ") { s ->
                            when {
                                ex.isCardio -> describeSet(ex, s)
                                s.weightKg > 0 -> "${formatKg(s.weightKg)}×${s.reps}"
                                else -> "${s.reps}"
                            }
                        }
                        val avgEffort = sessionSets.map { it.effort }.average().toInt()
                        Text(
                            "${formatDate(first.createdAt)}: $summary (${effortLabel(avgEffort).lowercase()})",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}
