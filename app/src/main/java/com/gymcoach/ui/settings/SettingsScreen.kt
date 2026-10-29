package com.gymcoach.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gymcoach.notify.ReminderSettings
import com.gymcoach.notify.Reminders
import com.gymcoach.notify.Watch
import com.gymcoach.ui.GymTopBar
import com.gymcoach.ui.LocalRepository
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import com.gymcoach.ui.theme.RetroCard
import com.gymcoach.ui.theme.RetroOutlinedButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(back: () -> Unit) {
    val context = LocalContext.current
    val repo = LocalRepository.current
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(Reminders.load(context)) }
    var pickTime by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }

    fun update(new: ReminderSettings) {
        settings = new
        Reminders.save(context, new)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        update(settings.copy(enabled = granted))
    }

    Column(Modifier.fillMaxSize()) {
        GymTopBar("Settings", back = back)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Workout reminders", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "A daily notification with today's session. No notification on rest days.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(
                    checked = settings.enabled,
                    onCheckedChange = { on ->
                        if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Reminders.canNotify(context)) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            update(settings.copy(enabled = on))
                        }
                    },
                )
            }
            if (permissionDenied) {
                Text(
                    "Notifications are blocked. Allow them for Gym Coach in Android Settings → Apps.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Text("Your watch", style = MaterialTheme.typography.titleMedium)
            Watch.entries.forEach { watch ->
                RetroCard(
                    onClick = { update(settings.copy(watch = watch)) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (settings.watch == watch) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(watch.label, style = MaterialTheme.typography.titleSmall)
                        Text(watch.detail, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (settings.watch == Watch.CUSTOM) {
                RetroOutlinedButton(onClick = { pickTime = true }) {
                    Text("Reminder at ${settings.customTime.format(DateTimeFormatter.ofPattern("HH:mm"))}")
                }
            }
            Text(
                "Switched watch? Change it here, and use \"Shift one day\" on the plan screen so the switch day becomes a rest day.",
                style = MaterialTheme.typography.bodySmall,
            )

            RetroOutlinedButton(onClick = {
                scope.launch {
                    val (title, text) = Reminders.reminderText(repo.currentProgramStatus())
                        ?: ("No session today" to "Rest day. Reminders skip days like this.")
                    if (Reminders.canNotify(context)) Reminders.show(context, title, text)
                    else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }) { Text("Send a test reminder") }
        }
    }

    if (pickTime) {
        val state = rememberTimePickerState(settings.customTime.hour, settings.customTime.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            title = { Text("Reminder time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    update(settings.copy(customTime = LocalTime.of(state.hour, state.minute)))
                    pickTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Cancel") } },
        )
    }
}
