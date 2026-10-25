package com.gymcoach.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcoach.coach.effortLabel
import com.gymcoach.ui.describeSet
import com.gymcoach.ui.GymTopBar
import com.gymcoach.ui.LocalRepository
import com.gymcoach.ui.formatDateTime
import com.gymcoach.ui.formatDuration
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(openSession: (Long) -> Unit) {
    val repo = LocalRepository.current
    val sessions by repo.sessionSummaries.collectAsStateWithLifecycle(initialValue = emptyList())
    Column(Modifier.fillMaxSize()) {
        GymTopBar("History")
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (sessions.isEmpty()) {
                item { Text("Finished workouts show up here.", style = MaterialTheme.typography.bodyLarge) }
            }
            items(sessions, key = { it.id }) { s ->
                Card(Modifier.fillMaxWidth().clickable { openSession(s.id) }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(formatDateTime(s.startedAt), style = MaterialTheme.typography.titleMedium)
                        val duration = s.endedAt?.let { " · ${formatDuration(s.startedAt, it)}" } ?: ""
                        Text(
                            "${s.exerciseCount} exercises · ${s.setCount} sets · ${s.volumeKg.roundToInt()} kg total$duration",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SessionDetailScreen(sessionId: Long, back: () -> Unit) {
    val repo = LocalRepository.current
    val session by repo.session(sessionId).collectAsStateWithLifecycle(initialValue = null)
    val sets by repo.setsForSession(sessionId).collectAsStateWithLifecycle(initialValue = emptyList())
    val exercises by repo.exercises.collectAsStateWithLifecycle(initialValue = emptyList())
    val byId = exercises.associateBy { it.id }

    Column(Modifier.fillMaxSize()) {
        GymTopBar(session?.let { formatDateTime(it.startedAt) } ?: "Workout", back = back)
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val grouped = sets.groupBy { it.exerciseId }
            items(grouped.keys.toList()) { exId ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        val ex = byId[exId]
                        Text(ex?.name ?: "Exercise", style = MaterialTheme.typography.titleMedium)
                        grouped.getValue(exId).forEachIndexed { i, set ->
                            Text(
                                "${if (ex?.isCardio == true) "Bout" else "Set"} ${i + 1}: ${describeSet(ex, set)} · ${effortLabel(set.effort)}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}
