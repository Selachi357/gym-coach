package com.gymcoach.coach

// The timers keep time from a clock reading instead of counting ticks, so they stay right while
// the screen is off or another screen is open. Every function takes "now" in milliseconds.

/** A rest countdown, e.g. 90 seconds between sets. */
data class Countdown(val totalMs: Long = 90_000, val endsAt: Long? = null, val pausedLeftMs: Long = totalMs) {
    val running: Boolean get() = endsAt != null

    fun leftMs(now: Long): Long = endsAt?.let { maxOf(0L, it - now) } ?: pausedLeftMs

    fun finished(now: Long): Boolean = endsAt != null && now >= endsAt

    fun start(now: Long): Countdown = if (running) this else copy(endsAt = now + (if (pausedLeftMs > 0) pausedLeftMs else totalMs))

    fun pause(now: Long): Countdown = if (!running) this else copy(endsAt = null, pausedLeftMs = leftMs(now))

    /** Back to the full time, stopped. */
    fun reset(totalMs: Long = this.totalMs): Countdown = Countdown(totalMs)

    /** Starts over from the full time, e.g. right after logging a set. */
    fun restart(now: Long, totalMs: Long = this.totalMs): Countdown = Countdown(totalMs).start(now)

    /** Adds (or with a negative value takes off) time; a stopped timer changes its start time too. */
    fun plus(ms: Long, now: Long): Countdown = when {
        endsAt != null -> copy(endsAt = maxOf(now, endsAt + ms))
        pausedLeftMs == totalMs -> Countdown(maxOf(MIN_MS, totalMs + ms))
        else -> copy(pausedLeftMs = maxOf(0L, pausedLeftMs + ms))
    }

    companion object {
        const val MIN_MS = 5_000L
        val PRESETS_S = listOf(30, 60, 90, 120, 180)
    }
}

/** A stopwatch with laps, e.g. for a plank hold or a whole cardio session. */
data class Stopwatch(val startedAt: Long? = null, val bankedMs: Long = 0, val laps: List<Long> = emptyList()) {
    val running: Boolean get() = startedAt != null

    fun elapsedMs(now: Long): Long = bankedMs + (startedAt?.let { maxOf(0L, now - it) } ?: 0L)

    fun start(now: Long): Stopwatch = if (running) this else copy(startedAt = now)

    fun pause(now: Long): Stopwatch = if (!running) this else copy(startedAt = null, bankedMs = elapsedMs(now))

    /** Saves the total time so far as a lap. */
    fun lap(now: Long): Stopwatch = if (elapsedMs(now) == 0L) this else copy(laps = laps + elapsedMs(now))

    fun reset(): Stopwatch = Stopwatch()
}

enum class IntervalPhase(val label: String) { WORK("HARD"), REST("EASY"), DONE("DONE") }

/** Where an interval workout is right now. */
data class IntervalState(val round: Int, val phase: IntervalPhase, val leftMs: Long)

/**
 * Hard/easy intervals, like the rower days: 6 × (1 min hard / 2 min easy).
 * Pausing keeps the time already done in [bankedMs].
 */
data class Intervals(
    val rounds: Int = 6,
    val workS: Int = 60,
    val restS: Int = 120,
    val startedAt: Long? = null,
    val bankedMs: Long = 0,
) {
    val running: Boolean get() = startedAt != null
    val totalMs: Long get() = rounds * (workS + restS) * 1000L

    fun elapsedMs(now: Long): Long = bankedMs + (startedAt?.let { maxOf(0L, now - it) } ?: 0L)

    fun state(now: Long): IntervalState {
        val t = elapsedMs(now)
        if (t >= totalMs) return IntervalState(rounds, IntervalPhase.DONE, 0)
        val roundMs = (workS + restS) * 1000L
        val round = (t / roundMs).toInt() + 1
        val inRound = t % roundMs
        return if (inRound < workS * 1000L) {
            IntervalState(round, IntervalPhase.WORK, workS * 1000L - inRound)
        } else {
            IntervalState(round, IntervalPhase.REST, roundMs - inRound)
        }
    }

    fun start(now: Long): Intervals = when {
        running -> this
        elapsedMs(now) >= totalMs -> copy(startedAt = now, bankedMs = 0)
        else -> copy(startedAt = now)
    }

    fun pause(now: Long): Intervals = if (!running) this else copy(startedAt = null, bankedMs = elapsedMs(now))

    fun reset(): Intervals = copy(startedAt = null, bankedMs = 0)

    /** The settings changed: start again from round 1. */
    fun with(rounds: Int = this.rounds, workS: Int = this.workS, restS: Int = this.restS): Intervals =
        Intervals(rounds.coerceIn(1, 30), workS.coerceIn(5, 600), restS.coerceIn(0, 600))

    companion object {
        /** Reads the rower plan, e.g. "8 × (1 min hard / 1:30 easy)". */
        fun parse(text: String): Intervals? {
            val m = Regex("""(\d+)\s*×\s*\((\d+(?::\d+)?)\s*(?:min)?\s*hard\s*/\s*(\d+(?::\d+)?)\s*(?:min)?\s*easy\)""").find(text)
                ?: return null
            fun seconds(s: String): Int = if (':' in s) s.substringBefore(':').toInt() * 60 + s.substringAfter(':').toInt() else s.toInt() * 60
            return Intervals(m.groupValues[1].toInt(), seconds(m.groupValues[2]), seconds(m.groupValues[3]))
        }
    }
}

/** "1:30", or "1:02:03" past an hour. Countdowns round up so they show 0:00 only at the end. */
fun formatClock(ms: Long, roundUp: Boolean = true): String {
    val total = if (roundUp) (ms + 999) / 1000 else ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
