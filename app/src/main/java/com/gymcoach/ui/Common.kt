package com.gymcoach.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.RowScope
import com.gymcoach.data.isTimed
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymTopBar(
    title: String,
    back: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (back != null) {
                IconButton(onClick = back) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = actions,
    )
}

fun formatDate(millis: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))

fun formatDateTime(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))

fun formatDuration(startMillis: Long, endMillis: Long): String {
    val minutes = ((endMillis - startMillis) / 60_000).coerceAtLeast(0)
    return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
}

/** "3 × 8 @ 60 kg", "3 × 12" for bodyweight, or "30 min" for cardio. */
fun formatTarget(sets: Int, reps: Int, weightKg: Double, cardio: Boolean = false, timed: Boolean = false): String = when {
    cardio -> "$reps min"
    timed -> "$sets × $reps s"
    weightKg > 0 -> "$sets × $reps @ ${com.gymcoach.coach.formatKg(weightKg)} kg"
    else -> "$sets × $reps"
}

/** Pace in the units each machine shows: min/km for running, /500 m for rowing, km/h for cycling. */
fun formatPace(exerciseName: String, minutes: Int, distanceKm: Double): String? {
    if (minutes <= 0 || distanceKm <= 0) return null
    fun minSec(totalMin: Double): String {
        val secs = (totalMin * 60).toInt()
        return "%d:%02d".format(secs / 60, secs % 60)
    }
    return when {
        exerciseName.contains("Row", ignoreCase = true) -> "${minSec(minutes / (distanceKm * 2))} /500 m"
        exerciseName.contains("Bike", ignoreCase = true) || exerciseName.contains("Cross", ignoreCase = true) -> "%.1f km/h".format(distanceKm / (minutes / 60.0))
        else -> "${minSec(minutes / distanceKm)} /km"
    }
}

fun formatKm(km: Double): String = "%.2f km".format(km)

/** "60 kg × 8 reps", "12 reps", or for cardio "25 min · 4.20 km · 5:57 /km". */
fun describeSet(exercise: com.gymcoach.data.Exercise?, set: com.gymcoach.data.SetEntry): String {
    if (exercise?.isCardio == true) {
        val parts = mutableListOf("${set.reps} min")
        if (set.distanceKm > 0) parts += formatKm(set.distanceKm)
        formatPace(exercise.name, set.reps, set.distanceKm)?.let { parts += it }
        return parts.joinToString(" · ")
    }
    val load = if (set.weightKg > 0) "${com.gymcoach.coach.formatKg(set.weightKg)} kg × " else ""
    if (exercise?.isTimed == true) return "$load${set.reps} s"
    return "$load${set.reps} reps"
}
