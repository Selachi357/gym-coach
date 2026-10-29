package com.gymcoach.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gymcoach.GymCoachApp
import com.gymcoach.MainActivity
import com.gymcoach.R
import com.gymcoach.data.ProgramStatus
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.launch

/** Which watch you are on decides when the reminder fires. */
enum class Watch(val label: String, val detail: String, val time: LocalTime?) {
    DAY("Day watch", "06:00–18:00 · reminder at 18:30", LocalTime.of(18, 30)),
    NIGHT("Night watch", "18:00–06:00 · reminder at 14:00", LocalTime.of(14, 0)),
    CUSTOM("Custom time", "Pick your own reminder time", null),
}

data class ReminderSettings(
    val enabled: Boolean,
    val watch: Watch,
    val customTime: LocalTime,
) {
    val time: LocalTime get() = watch.time ?: customTime
}

object Reminders {
    private const val PREFS = "reminders"
    private const val CHANNEL_ID = "workout_reminders"
    private const val NOTIFICATION_ID = 1001
    private const val REQUEST_CODE = 2001

    fun load(context: Context): ReminderSettings {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return ReminderSettings(
            enabled = p.getBoolean("enabled", false),
            watch = runCatching { Watch.valueOf(p.getString("watch", Watch.DAY.name)!!) }.getOrDefault(Watch.DAY),
            customTime = LocalTime.of(p.getInt("hour", 18), p.getInt("minute", 30)),
        )
    }

    fun save(context: Context, settings: ReminderSettings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("enabled", settings.enabled)
            .putString("watch", settings.watch.name)
            .putInt("hour", settings.customTime.hour)
            .putInt("minute", settings.customTime.minute)
            .apply()
        schedule(context)
    }

    /** Sets the alarm for the next reminder time, or cancels it when reminders are off. */
    fun schedule(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarms.cancel(pending)
        val settings = load(context)
        if (!settings.enabled) return
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(settings.time)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // Inexact is fine for a workout nudge and needs no special alarm permission.
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
    }

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Workout reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "A daily nudge with today's session from your plan"
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Builds the reminder text from today's plan. Returns null on days without a session. */
    fun reminderText(status: ProgramStatus?): Pair<String, String>? = when (status) {
        null -> "Time to train" to "Open Gym Coach and log today's workout."
        is ProgramStatus.Next -> "Today: ${status.plan.name}" to
            listOfNotNull(
                status.plan.duration.takeIf { it.isNotEmpty() },
                "Eat a snack 1–2 h before, finish 2 h before sleep.",
            ).joinToString(" · ")
        is ProgramStatus.NotStarted, is ProgramStatus.DoneToday, is ProgramStatus.Rest, ProgramStatus.Finished -> null
    }

    fun show(context: Context, title: String, text: String) {
        if (!canNotify(context)) return
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission was revoked between the check and the call.
        }
    }
}

/** Fires at the reminder time, shows today's session, and sets tomorrow's alarm. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as GymCoachApp
        val pending = goAsync()
        app.appScope.launch {
            try {
                Reminders.reminderText(app.repository.currentProgramStatus())?.let { (title, text) ->
                    Reminders.show(context, title, text)
                }
            } finally {
                Reminders.schedule(context)
                pending.finish()
            }
        }
    }
}

/** Alarms are cleared on reboot and app updates; set the next one again. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.schedule(context)
    }
}
