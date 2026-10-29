package com.gymcoach.game

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Day-streak milestones and what they give. */
enum class BonfireMilestone(val days: Int, val title: String, val xp: Int, val tokens: Int) {
    SPARK(3, "Spark", 50, 0),
    CAMPFIRE(7, "Campfire", 100, 0),
    BONFIRE(14, "Bonfire", 200, 1),
    BEACON(30, "Beacon", 400, 2),
    ETERNAL_FLAME(100, "Eternal Flame", 1000, 5),
}

data class Bonfire(
    /** Active days in a row: workouts and rest days both count. */
    val days: Int,
    val best: Int,
    /** Today already counts (trained, or a planned rest day). */
    val litToday: Boolean,
    /** Today is a planned rest day. */
    val restToday: Boolean,
    /** Yesterday was missed but the fire is still glowing: train today to keep the streak. */
    val embers: Boolean,
    /** A Second Wind is available for the next missed day (one per 7 days). */
    val secondWindReady: Boolean,
    val secondWindsUsed: Int,
    val milestones: List<BonfireMilestone>,
    /** Ember charms that saved the fire so far. */
    val charmsUsed: Int = 0,
) {
    val next: BonfireMilestone? get() = BonfireMilestone.entries.firstOrNull { it.days > days }
    /** Size of the fire, 0 (cold) to 5 (eternal flame). */
    val tier: Int get() = BonfireMilestone.entries.count { days >= it.days }
}

/** Weekly guild contracts. You pick one each week; the default is three workouts. */
enum class Contract(val title: String) {
    THREE_WORKOUTS("Three Trials"),
    TWO_CARDIO("Road Warden"),
    WEEKLY_TARGET("Oath of the Week"),
    ;

    fun description(weeklyTarget: Int) = when (this) {
        THREE_WORKOUTS -> "Complete 3 workouts this week"
        TWO_CARDIO -> "Do 2 cardio sessions this week"
        WEEKLY_TARGET -> "Hit your weekly target of $weeklyTarget workouts"
    }

    fun goal(weeklyTarget: Int) = when (this) {
        THREE_WORKOUTS -> 3
        TWO_CARDIO -> 2
        WEEKLY_TARGET -> weeklyTarget
    }

    fun progress(week: List<WorkoutRecord>) = when (this) {
        TWO_CARDIO -> week.count { w -> w.sets.any { it.cardio } }
        else -> week.size
    }
}

data class ContractState(val week: LocalDate, val contract: Contract, val current: Int, val goal: Int) {
    val done: Boolean get() = current >= goal
}

data class Contracts(val thisWeek: ContractState, val completed: Int)

/**
 * The bonfire streak counts days, not weeks, but never asks you to train every day:
 * - A workout lights the day. So does a rest day in your plan.
 * - Without a calendar plan, up to two rest days after a workout keep the fire going.
 * - Miss a day and the fire drops to embers. Train the next day and Second Wind keeps your streak
 *   (once every 7 days). Miss again and the streak restarts, unless you have an Ember Charm from the shop.
 *   XP, levels and gear are never lost.
 */
object BonfireCalculator {
    const val CONTRACT_XP = 250
    const val CONTRACT_GOLD = 50
    private const val SECOND_WIND_COOLDOWN = 7L

    fun compute(
        workouts: Collection<LocalDate>,
        plannedRest: Set<LocalDate>,
        planDays: Set<LocalDate>,
        today: LocalDate,
        charms: Int = 0,
    ): Bonfire {
        val trained = workouts.toSet()
        val restToday = today in plannedRest
        val first = trained.minOrNull()
        if (first == null) {
            return Bonfire(0, 0, restToday, restToday, embers = false, secondWindReady = true, secondWindsUsed = 0, milestones = emptyList())
        }
        fun active(d: LocalDate): Boolean = when {
            d in trained || d in plannedRest -> true
            d in planDays -> false
            else -> d.minusDays(1) in trained || d.minusDays(2) in trained
        }
        var streak = 0
        var best = 0
        var embers: LocalDate? = null
        var lastSecondWind: LocalDate? = null
        var used = 0
        var charmsUsed = 0
        var d: LocalDate = first
        while (!d.isAfter(today)) {
            if (active(d)) {
                if (embers != null) {
                    lastSecondWind = embers
                    used++
                    embers = null
                }
                streak++
                best = maxOf(best, streak)
            } else if (d != today) {
                val ready = lastSecondWind == null || ChronoUnit.DAYS.between(lastSecondWind, d) >= SECOND_WIND_COOLDOWN
                if (embers == null && ready && streak > 0) {
                    embers = d
                } else if (streak > 0 && charmsUsed < charms) {
                    // An Ember Charm burns instead of the fire: the streak goes on, the missed day doesn't count.
                    charmsUsed++
                    embers = null
                } else {
                    streak = 0
                    embers = null
                }
            }
            d = d.plusDays(1)
        }
        val ready = lastSecondWind == null || ChronoUnit.DAYS.between(lastSecondWind, today) >= SECOND_WIND_COOLDOWN
        return Bonfire(
            days = streak,
            best = best,
            litToday = active(today),
            restToday = restToday,
            embers = embers != null,
            secondWindReady = ready && embers == null,
            secondWindsUsed = used,
            milestones = BonfireMilestone.entries.filter { best >= it.days },
            charmsUsed = charmsUsed,
        )
    }

    private fun weekStart(date: LocalDate) = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** [choices] maps the Monday of a week to the contract picked for it. */
    fun contracts(workouts: List<WorkoutRecord>, choices: Map<LocalDate, Contract>, weeklyTarget: Int, today: LocalDate): Contracts {
        val byWeek = workouts.groupBy { weekStart(it.date) }
        fun state(week: LocalDate): ContractState {
            val c = choices[week] ?: Contract.THREE_WORKOUTS
            val goal = c.goal(weeklyTarget)
            return ContractState(week, c, minOf(goal, c.progress(byWeek[week].orEmpty())), goal)
        }
        val current = weekStart(today)
        val completed = byWeek.keys.filter { !it.isAfter(current) }.count { state(it).done }
        return Contracts(state(current), completed)
    }

    /** Bonus XP from bonfire milestones and finished contracts. */
    fun bonusXp(bonfire: Bonfire, contracts: Contracts) =
        bonfire.milestones.sumOf { it.xp } + contracts.completed * CONTRACT_XP

    fun celebrate(beforeFire: Bonfire, afterFire: Bonfire, beforeContracts: Contracts, afterContracts: Contracts): List<String> {
        val messages = mutableListOf<String>()
        if (beforeFire.embers && !afterFire.embers && afterFire.days > 0) {
            messages += "Second Wind! Your bonfire is burning again: ${afterFire.days} days."
        }
        (afterFire.milestones - beforeFire.milestones.toSet()).forEach {
            messages += "Bonfire milestone: ${it.title}, ${it.days} days! +${it.xp} XP" +
                if (it.tokens > 0) " and ${it.tokens} royal ${if (it.tokens == 1) "token" else "tokens"}." else "."
        }
        if (afterContracts.completed > beforeContracts.completed) {
            messages += "Contract fulfilled: ${afterContracts.thisWeek.contract.title}! +$CONTRACT_XP XP and $CONTRACT_GOLD gold for your village."
        }
        return messages
    }
}
