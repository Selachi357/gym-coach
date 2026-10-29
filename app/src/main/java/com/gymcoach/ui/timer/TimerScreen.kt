package com.gymcoach.ui.timer

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gymcoach.coach.Countdown
import com.gymcoach.coach.IntervalPhase
import com.gymcoach.coach.Intervals
import com.gymcoach.coach.ProgramPlanner
import com.gymcoach.coach.Stopwatch
import com.gymcoach.coach.formatClock
import com.gymcoach.ui.GymTopBar
import com.gymcoach.ui.hero.Retro
import com.gymcoach.ui.theme.RetroButton
import com.gymcoach.ui.theme.RetroCard
import com.gymcoach.ui.theme.RetroOutlinedButton
import kotlinx.coroutines.delay

/**
 * The app's timers. They live as long as the app does, so a rest timer started in a workout keeps
 * running while you look at the plan or the guide.
 */
object Timers {
    var rest by mutableStateOf(Countdown())
    var stopwatch by mutableStateOf(Stopwatch())
    var intervals by mutableStateOf(Intervals.parse(ProgramPlanner.rowerIntervals(1)) ?: Intervals())
    var sound by mutableStateOf(true)

    fun now(): Long = SystemClock.elapsedRealtime()

    /** Starts the rest countdown from its full time, e.g. right after a set is logged. */
    fun startRest() {
        rest = rest.restart(now())
    }
}

/** Ticks once a few times a second while any timer runs and returns the current time. */
@Composable
private fun rememberNow(): Long {
    var now by remember { mutableLongStateOf(Timers.now()) }
    val running = Timers.rest.running || Timers.stopwatch.running || Timers.intervals.running
    LaunchedEffect(running) {
        now = Timers.now()
        while (running) {
            delay(100)
            now = Timers.now()
        }
    }
    return now
}

/**
 * Buzzes and beeps when the rest countdown ends and at every hard/easy switch of the intervals.
 * Placed once at the root so it works on every screen. It checks the clock in its own loop
 * instead of waiting for the screen to redraw, so it also fires while the app is in the background.
 */
@Composable
fun TimerAlarm() {
    val context = LocalContext.current.applicationContext
    LaunchedEffect(Unit) {
        var lastInterval: Pair<Int, IntervalPhase>? = null
        while (true) {
            delay(200)
            val now = Timers.now()
            if (Timers.rest.finished(now)) {
                Timers.rest = Timers.rest.reset()
                alert(context, long = true)
            }
            val intervals = Timers.intervals
            if (intervals.running) {
                val state = intervals.state(now)
                val key = state.round to state.phase
                if (lastInterval != null && key != lastInterval) alert(context, long = state.phase == IntervalPhase.DONE)
                lastInterval = key
                if (state.phase == IntervalPhase.DONE) Timers.intervals = intervals.pause(now)
            } else {
                lastInterval = null
            }
        }
    }
}

private fun alert(context: Context, long: Boolean) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }
    val pattern = if (long) longArrayOf(0, 400, 150, 400, 150, 600) else longArrayOf(0, 300)
    runCatching { vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1)) }
    if (!Timers.sound) return
    runCatching {
        // Media volume, so the phone's volume buttons control it.
        val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
        tone.startTone(if (long) ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD else ToneGenerator.TONE_PROP_BEEP2, if (long) 900 else 250)
        Handler(Looper.getMainLooper()).postDelayed({ tone.release() }, 1500)
    }
}

@Composable
private fun BigClock(text: String, color: androidx.compose.ui.graphics.Color = Retro.gold) {
    Text(
        text,
        style = MaterialTheme.typography.displayLarge,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    )
}

@Composable
private fun PlayPause(running: Boolean, onStart: () -> Unit, onPause: () -> Unit, modifier: Modifier = Modifier) {
    RetroButton(onClick = if (running) onPause else onStart, modifier = modifier) {
        Icon(if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text(if (running) "Pause" else "Start")
    }
}

/** A compact rest timer for the workout screen. It starts by itself when you log a set. */
@Composable
fun RestTimerBar(openTimer: () -> Unit) {
    val now = rememberNow()
    val rest = Timers.rest
    val left = rest.leftMs(now)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Retro.night)
            .border(2.dp, if (rest.running) Retro.xp else Retro.gold)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Column(Modifier.weight(1f).clickable(onClick = openTimer)) {
            Text(if (rest.running) "REST" else "REST TIMER", style = MaterialTheme.typography.labelSmall, color = Retro.dim)
            Text(
                formatClock(left),
                style = MaterialTheme.typography.headlineMedium,
                color = if (rest.running) Retro.xp else Retro.gold,
            )
        }
        TextButton(onClick = { Timers.rest = rest.plus(-15_000, now) }) { Text("-15") }
        TextButton(onClick = { Timers.rest = rest.plus(15_000, now) }) { Text("+15") }
        IconButton(onClick = { Timers.rest = if (rest.running) rest.pause(now) else rest.start(now) }) {
            Icon(if (rest.running) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = if (rest.running) "Pause" else "Start")
        }
        IconButton(onClick = openTimer) { Icon(Icons.Filled.Timer, contentDescription = "All timers") }
    }
}

@Composable
fun TimerScreen(back: () -> Unit) {
    val now = rememberNow()
    Column(Modifier.fillMaxSize()) {
        GymTopBar("Timer", back = back, actions = {
            Text("SOUND", style = MaterialTheme.typography.labelSmall, color = Retro.dim)
            Spacer(Modifier.width(6.dp))
            Switch(checked = Timers.sound, onCheckedChange = { Timers.sound = it })
            Spacer(Modifier.width(8.dp))
        })
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { RestCard(now) }
            item { IntervalsCard(now) }
            item { StopwatchCard(now) }
        }
    }
}

@Composable
private fun RestCard(now: Long) {
    val rest = Timers.rest
    RetroCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Rest between sets", style = MaterialTheme.typography.titleMedium)
            Text(
                "Starts by itself when you log a set. Big lifts 90 s, the rest 60 s.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BigClock(formatClock(rest.leftMs(now)), if (rest.running) Retro.xp else Retro.gold)
            LinearProgressIndicator(
                progress = { 1f - rest.leftMs(now).toFloat() / rest.totalMs.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Countdown.PRESETS_S.forEach { s ->
                    FilterChip(
                        selected = rest.totalMs == s * 1000L,
                        onClick = { Timers.rest = if (rest.running) rest.restart(now, s * 1000L) else rest.reset(s * 1000L) },
                        label = { Text(if (s % 60 == 0) "${s / 60}m" else if (s > 60) "${s / 60}:${s % 60}" else "${s}s") },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PlayPause(rest.running, { Timers.rest = rest.start(now) }, { Timers.rest = rest.pause(now) }, Modifier.weight(1f))
                RetroOutlinedButton(onClick = { Timers.rest = rest.plus(15_000, now) }) { Text("+15") }
                IconButton(onClick = { Timers.rest = rest.reset() }) { Icon(Icons.Filled.Refresh, contentDescription = "Reset") }
            }
        }
    }
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = onMinus) { Text("-") }
        Text(value, style = MaterialTheme.typography.titleMedium, color = Retro.gold)
        TextButton(onClick = onPlus) { Text("+") }
    }
}

@Composable
private fun IntervalsCard(now: Long) {
    val iv = Timers.intervals
    val state = iv.state(now)
    val started = iv.elapsedMs(now) > 0
    RetroCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Intervals", style = MaterialTheme.typography.titleMedium)
            Text(
                "Hard and easy rounds for the rower or bike. It buzzes at every switch.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (started) {
                val color = when (state.phase) {
                    IntervalPhase.WORK -> Retro.hp
                    IntervalPhase.REST -> Retro.sea
                    IntervalPhase.DONE -> Retro.xp
                }
                Text(
                    if (state.phase == IntervalPhase.DONE) "ALL ${iv.rounds} ROUNDS DONE" else "ROUND ${state.round} OF ${iv.rounds} · ${state.phase.label}",
                    style = MaterialTheme.typography.titleMedium,
                    color = color,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                BigClock(formatClock(state.leftMs), color)
            } else {
                Text("Rower plan", style = MaterialTheme.typography.labelMedium, color = Retro.dim)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1 to "Wk 1–2", 3 to "Wk 3–4", 5 to "Wk 5–6").forEach { (week, label) ->
                        val preset = Intervals.parse(ProgramPlanner.rowerIntervals(week)) ?: return@forEach
                        FilterChip(
                            selected = iv.with() == preset,
                            onClick = { Timers.intervals = preset },
                            label = { Text(label) },
                        )
                    }
                }
                Stepper("Rounds", "${iv.rounds}", { Timers.intervals = iv.with(rounds = iv.rounds - 1) }, { Timers.intervals = iv.with(rounds = iv.rounds + 1) })
                Stepper("Hard", formatClock(iv.workS * 1000L), { Timers.intervals = iv.with(workS = iv.workS - 15) }, { Timers.intervals = iv.with(workS = iv.workS + 15) })
                Stepper("Easy", formatClock(iv.restS * 1000L), { Timers.intervals = iv.with(restS = iv.restS - 15) }, { Timers.intervals = iv.with(restS = iv.restS + 15) })
                Text("Total ${formatClock(iv.totalMs)}", style = MaterialTheme.typography.labelMedium, color = Retro.dim)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PlayPause(iv.running, { Timers.intervals = iv.start(now) }, { Timers.intervals = iv.pause(now) }, Modifier.weight(1f))
                IconButton(onClick = { Timers.intervals = iv.reset() }) { Icon(Icons.Filled.Refresh, contentDescription = "Reset") }
            }
        }
    }
}

@Composable
private fun StopwatchCard(now: Long) {
    val sw = Timers.stopwatch
    RetroCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Stopwatch", style = MaterialTheme.typography.titleMedium)
            Text(
                "For plank holds, a run or the whole session.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BigClock(formatClock(sw.elapsedMs(now), roundUp = false), if (sw.running) Retro.xp else Retro.gold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PlayPause(sw.running, { Timers.stopwatch = sw.start(now) }, { Timers.stopwatch = sw.pause(now) }, Modifier.weight(1f))
                RetroOutlinedButton(onClick = { Timers.stopwatch = sw.lap(now) }, enabled = sw.running) { Text("Lap") }
                IconButton(onClick = { Timers.stopwatch = sw.reset() }) { Icon(Icons.Filled.Refresh, contentDescription = "Reset") }
            }
            sw.laps.forEachIndexed { i, total ->
                val split = total - (sw.laps.getOrNull(i - 1) ?: 0L)
                Text(
                    "Lap ${i + 1}: ${formatClock(split, roundUp = false)}  (${formatClock(total, roundUp = false)})",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
