package com.gymcoach.game

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** One logged set as the level-up system sees it. */
data class SetRecord(
    val exerciseId: Long,
    val weightKg: Double,
    /** Reps, seconds for holds, or minutes for cardio. */
    val reps: Int,
    val cardio: Boolean,
    val distanceKm: Double = 0.0,
)

data class WorkoutRecord(
    val date: LocalDate,
    val inProgram: Boolean,
    val sets: List<SetRecord>,
)

/** Medieval ranks, earned by level. */
enum class Rank(val title: String, val minLevel: Int, val flavor: String) {
    PEASANT("Peasant", 1, "A humble start. Every legend began in the mud."),
    SQUIRE("Squire", 3, "You carry a wooden sword and real ambition."),
    FOOTMAN("Footman", 5, "Leather armour, iron blade, steady feet."),
    MAN_AT_ARMS("Man-at-Arms", 7, "Chainmail and shield. The ship's crew takes notice."),
    KNIGHT("Knight", 10, "Full plate. You have earned your spurs."),
    KNIGHT_ERRANT("Knight Errant", 13, "You roam the seas in search of heavier iron."),
    PALADIN("Paladin", 16, "A red cape and an unbreakable routine."),
    CHAMPION("Champion", 20, "The realm's finest. Gold trims your steel."),
    WARLORD("Warlord", 25, "Armies would follow you to the rowing machine."),
    LEGEND("Legend of the Realm", 30, "Bards sing of your deadlifts."),
    ;

    companion object {
        fun forLevel(level: Int): Rank = entries.last { level >= it.minLevel }
    }
}

enum class Badge { SWORD, FLAME, SHIELD, STAR, CROWN, BOOT, WAVE, CUP, SCROLL, DRAGON, CHEST }

/** Quests that unlock rewards. [progress] returns (current, goal). */
enum class Quest(val title: String, val description: String, val badge: Badge) {
    FIRST_BLOOD("First Blood", "Finish your first workout", Badge.SWORD),
    TEN_WORKOUTS("Iron Will", "Finish 10 workouts", Badge.SHIELD),
    FIFTY_WORKOUTS("Veteran of the Hall", "Finish 50 workouts", Badge.CROWN),
    CENTURION("Centurion", "Log 100 sets", Badge.STAR),
    STREAK_2("Kindled", "Keep a 2-week streak", Badge.FLAME),
    STREAK_4("Steadfast", "Keep a 4-week streak", Badge.FLAME),
    STREAK_8("Unbreakable", "Keep an 8-week streak", Badge.FLAME),
    PR_HUNTER("Trophy Hunter", "Set 10 personal records", Badge.CUP),
    FULL_CYCLE("Seafarer's Week", "Train 5 times in 7 days", Badge.WAVE),
    LONG_HAUL("Long Haul", "Cover 50 km of cardio", Badge.BOOT),
    CAMPAIGN("Campaign Veteran", "Finish 15 plan sessions", Badge.SCROLL),
    DRAGON_SLAYER("Dragon Slayer", "Finish all 30 sessions of a 6-week plan", Badge.DRAGON),
}

data class QuestState(val quest: Quest, val current: Int, val goal: Int) {
    val done: Boolean get() = current >= goal
}

data class HeroStats(
    val xp: Int,
    val level: Int,
    val xpIntoLevel: Int,
    val xpForNextLevel: Int,
    val rank: Rank,
    val weekStreak: Int,
    val bestWeekStreak: Int,
    /** Weeks (in total, not in a row) that hit the weekly target. */
    val keptWeeks: Int,
    /** Workouts so far this week, towards [weeklyTarget]. */
    val thisWeek: Int,
    val weeklyTarget: Int,
    val workouts: Int,
    val totalSets: Int,
    val personalRecords: Int,
    val cardioKm: Double,
    val quests: List<QuestState>,
) {
    val unlocked: List<Quest> get() = quests.filter { it.done }.map { it.quest }
}

/**
 * Turns the workout log into XP, levels, streaks and quests. Everything is derived from history,
 * so it works for workouts logged before the system existed and can never get out of sync.
 *
 * XP: 40 per workout, 4 per strength set, 1 per cardio minute (max 60), +20 for a plan session,
 * +25 per personal record, +50 per week the streak is kept, +100 per finished quest.
 * A streak week is a Monday–Sunday week with at least [weeklyTarget] workouts; the current week
 * never breaks the streak while it is still going.
 */
object HeroCalculator {
    const val XP_WORKOUT = 40
    const val XP_SET = 4
    const val XP_PROGRAM = 20
    const val XP_PR = 25
    const val XP_STREAK_WEEK = 50
    const val XP_QUEST = 100

    /** XP needed to go from [level] to the next one. */
    fun xpToNext(level: Int): Int = 100 + 50 * (level - 1)

    fun levelFor(xp: Int): Triple<Int, Int, Int> {
        var level = 1
        var remaining = xp
        while (remaining >= xpToNext(level)) {
            remaining -= xpToNext(level)
            level++
        }
        return Triple(level, remaining, xpToNext(level))
    }

    private fun weekStart(date: LocalDate) = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** [bonusXp] comes from outside the log, e.g. bonfire milestones and weekly contracts. */
    fun compute(workouts: List<WorkoutRecord>, today: LocalDate, weeklyTarget: Int = 3, bonusXp: Int = 0): HeroStats {
        val sorted = workouts.sortedBy { it.date }
        var xp = 0
        var prs = 0
        var totalSets = 0
        var cardioKm = 0.0
        val best = mutableMapOf<Long, Double>()

        for (w in sorted) {
            xp += XP_WORKOUT
            if (w.inProgram) xp += XP_PROGRAM
            val cardioMinutes = w.sets.filter { it.cardio }.sumOf { it.reps }
            xp += minOf(60, cardioMinutes)
            val strength = w.sets.filter { !it.cardio }
            xp += strength.size * XP_SET
            totalSets += w.sets.size
            cardioKm += w.sets.filter { it.cardio }.sumOf { it.distanceKm }
            // A record is a heavier weight than ever before on an exercise you've done before.
            for ((exerciseId, sets) in strength.groupBy { it.exerciseId }) {
                val top = sets.filter { it.reps > 0 }.maxOfOrNull { it.weightKg } ?: continue
                val previous = best[exerciseId]
                if (previous != null && top > previous && top > 0) {
                    prs++
                    xp += XP_PR
                }
                if (previous == null || top > previous) best[exerciseId] = top
            }
        }

        // Streaks by Monday–Sunday week.
        val perWeek = sorted.groupingBy { weekStart(it.date) }.eachCount()
        val currentWeek = weekStart(today)
        val kept = perWeek.filter { it.value >= weeklyTarget }.keys
        xp += kept.size * XP_STREAK_WEEK
        var bestStreak = 0
        var run = 0
        var week = perWeek.keys.minOrNull()
        while (week != null && !week.isAfter(currentWeek)) {
            if (week in kept) {
                run++
                bestStreak = maxOf(bestStreak, run)
            } else if (week != currentWeek) {
                run = 0
            }
            week = week.plusWeeks(1)
        }
        // The current streak counts back from this week (or last week, if this one isn't done yet).
        var streak = 0
        var cursor = if (currentWeek in kept) currentWeek else currentWeek.minusWeeks(1)
        while (cursor in kept) {
            streak++
            cursor = cursor.minusWeeks(1)
        }

        // Most workouts in any 7-day window, for the Seafarer's Week quest.
        val dates = sorted.map { it.date }
        var bestWindow = 0
        for ((i, d) in dates.withIndex()) {
            val count = dates.subList(i, dates.size).count { it.isBefore(d.plusDays(7)) }
            bestWindow = maxOf(bestWindow, count)
        }

        val programSessions = sorted.count { it.inProgram }
        val quests = Quest.entries.map { q ->
            val (current, goal) = when (q) {
                Quest.FIRST_BLOOD -> sorted.size to 1
                Quest.TEN_WORKOUTS -> sorted.size to 10
                Quest.FIFTY_WORKOUTS -> sorted.size to 50
                Quest.CENTURION -> totalSets to 100
                Quest.STREAK_2 -> bestStreak to 2
                Quest.STREAK_4 -> bestStreak to 4
                Quest.STREAK_8 -> bestStreak to 8
                Quest.PR_HUNTER -> prs to 10
                Quest.FULL_CYCLE -> bestWindow to 5
                Quest.LONG_HAUL -> cardioKm.toInt() to 50
                Quest.CAMPAIGN -> programSessions to 15
                Quest.DRAGON_SLAYER -> programSessions to 30
            }
            QuestState(q, minOf(current, goal), goal)
        }
        xp += quests.count { it.done } * XP_QUEST
        xp += bonusXp

        val (level, into, next) = levelFor(xp)
        return HeroStats(
            xp = xp,
            level = level,
            xpIntoLevel = into,
            xpForNextLevel = next,
            rank = Rank.forLevel(level),
            weekStreak = streak,
            bestWeekStreak = bestStreak,
            keptWeeks = kept.size,
            thisWeek = perWeek[currentWeek] ?: 0,
            weeklyTarget = weeklyTarget,
            workouts = sorted.size,
            totalSets = totalSets,
            personalRecords = prs,
            cardioKm = cardioKm,
            quests = quests,
        )
    }

    /** Messages for what changed between two snapshots, e.g. after finishing a workout. */
    fun celebrate(before: HeroStats, after: HeroStats): List<String> {
        val messages = mutableListOf<String>()
        val gained = after.xp - before.xp
        if (gained > 0) messages += "+$gained XP"
        if (after.level > before.level) {
            messages += "LEVEL UP! You reached level ${after.level}."
        }
        if (after.rank != before.rank) {
            messages += "New rank: ${after.rank.title}. ${after.rank.flavor}"
        }
        if (after.weekStreak > before.weekStreak) {
            messages += "Streak: ${after.weekStreak} week${if (after.weekStreak == 1) "" else "s"} in a row!"
        }
        val newQuests = after.unlocked - before.unlocked.toSet()
        newQuests.forEach { messages += "Quest complete: ${it.title}! (${it.description})" }
        return messages
    }
}
