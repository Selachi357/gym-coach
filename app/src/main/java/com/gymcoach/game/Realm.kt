package com.gymcoach.game

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** The six bosses of a campaign, one per week. HP is for 5 workouts a week and scales with your plan. */
enum class Boss(val title: String, val lair: String, val baseHp: Int, val loot: String) {
    RAT_KING("The Bilge Rat King", "Lurks in the bilges, gnawing on the ship's ropes.", 200, "Rat King's Crown"),
    SEA_SERPENT("The Sea Serpent", "Coils around the hull when the swell picks up.", 250, "Serpent Scale Shield"),
    STORM_HARPY("The Storm Harpy", "Screams through the rigging on night watch.", 300, "Harpy Feather Cloak"),
    IRON_GOLEM("The Iron Golem", "Built from old dumbbells and anchor chain.", 350, "Golem Heart Amulet"),
    KRAKEN("The Kraken", "Eight arms, each one stronger than the last.", 400, "Kraken Ink Scroll"),
    DEEP_DRAGON("The Dragon of the Deep", "The final foe. Only a full campaign can beat it.", 450, "Dragonfire Blade"),
}

enum class BossStatus { LOCKED, ACTIVE, DEFEATED, ESCAPED }

data class BossFight(
    val boss: Boss,
    val week: Int,
    val start: LocalDate,
    val hp: Int,
    val damage: Int,
    val status: BossStatus,
) {
    val hpLeft: Int get() = (hp - damage).coerceAtLeast(0)
}

data class Campaign(
    /** 1 for the first six weeks, 2 for the next six, and so on. */
    val number: Int,
    val start: LocalDate,
    /** Days until the campaign starts, 0 when it is running. */
    val daysUntilStart: Int,
    val fights: List<BossFight>,
    val bossesDefeated: Int,
    val loot: List<String>,
) {
    val current: BossFight? get() = fights.firstOrNull { it.status == BossStatus.ACTIVE }
    /** Every boss beaten at least once, in any campaign. */
    val slain: Set<Boss> get() = Boss.entries.filter { it.loot in loot }.toSet()
}

/** Stops on the voyage. [km] is the distance from port; the chest opens after [strengthSessions] strength workouts. */
enum class Island(val title: String, val km: Int, val strengthSessions: Int, val story: String, val treasure: String) {
    HOME_PORT("Home Port", 0, 0, "The crew waves you off. The sea is calm and the map is blank.", "A worn sea chart"),
    GULL_ROCK("Gull Rock", 5, 2, "Gulls circle a bare rock. Someone carved a training log into the stone.", "12 silver coins"),
    SMUGGLERS_COVE("Smugglers' Cove", 12, 4, "Hidden caves full of crates. One is labelled 'protein'.", "A smuggler's spyglass"),
    MERMAID_REEF("Mermaid Reef", 22, 6, "Singing rises from the reef. Keep rowing and don't look back.", "A pearl as big as a fist"),
    FORT_IRONHOLD("Fort Ironhold", 35, 9, "An old fort with a rusty rack of kettlebells. The garrison salutes.", "An iron gauntlet"),
    ISLE_OF_EMBERS("Isle of Embers", 50, 12, "A volcano that rumbles in time with your heartbeat.", "An ember that never goes out"),
    FROST_FJORD("Frost Fjord", 68, 15, "Ice cliffs and a silent fjord. Your breath freezes mid-air.", "A frost giant's horn"),
    SUNKEN_TEMPLE("Sunken Temple", 90, 18, "Columns rise from the waves. The walls show heroes lifting stones.", "A golden idol"),
    DRAGON_ISLE("Dragon Isle", 115, 22, "Bones on the beach, smoke on the peak. The dragon's hoard waits.", "The dragon's hoard"),
    EDGE_OF_MAP("The Edge of the Map", 145, 26, "Here the chart ends. Every km from now on is yours alone.", "A blank map for a new voyage"),
}

data class Voyage(
    val km: Double,
    val strengthSessions: Int,
    val reached: List<Island>,
    val chestsOpened: List<Island>,
    val next: Island?,
) {
    val at: Island get() = reached.last()
}

data class Realm(val campaign: Campaign, val voyage: Voyage)

/**
 * The three games, all derived from the workout log like the hero levels:
 * - Campaign: each week of the six-week campaign has a boss. Workouts deal damage; beat it before the week ends.
 * - Voyage: cardio moves your ship (km, or 1 km per 6 minutes when no distance is logged); strength workouts open chests.
 */
object RealmCalculator {
    const val DAMAGE_WORKOUT = 30
    const val DAMAGE_SET = 5
    const val MAX_CARDIO_DAMAGE = 60
    const val MIN_PER_KM = 6.0

    fun damage(w: WorkoutRecord): Int {
        val cardioMinutes = w.sets.filter { it.cardio }.sumOf { it.reps }
        return DAMAGE_WORKOUT + DAMAGE_SET * w.sets.count { !it.cardio } + minOf(MAX_CARDIO_DAMAGE, cardioMinutes)
    }

    fun seaKm(w: WorkoutRecord): Double =
        w.sets.filter { it.cardio }.sumOf { if (it.distanceKm > 0) it.distanceKm else it.reps / MIN_PER_KM }

    fun bossHp(boss: Boss, campaign: Int, sessionsPerWeek: Int): Int {
        val scaled = boss.baseHp * sessionsPerWeek.coerceIn(2, 7) / 5.0 * (1 + 0.1 * (campaign - 1))
        return (Math.round(scaled / 10) * 10).toInt().coerceAtLeast(50)
    }

    private fun weekStart(date: LocalDate) = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** [campaignStart] is the first day of your plan, or null to start on the Monday of your first workout. */
    fun compute(
        workouts: List<WorkoutRecord>,
        today: LocalDate,
        campaignStart: LocalDate? = null,
        sessionsPerWeek: Int = 5,
    ): Realm = Realm(
        campaign(workouts, today, campaignStart, sessionsPerWeek),
        voyage(workouts),
    )

    fun campaign(workouts: List<WorkoutRecord>, today: LocalDate, campaignStart: LocalDate?, sessionsPerWeek: Int): Campaign {
        val origin = campaignStart ?: workouts.minOfOrNull { it.date }?.let(::weekStart) ?: weekStart(today)
        if (today.isBefore(origin)) {
            val fights = Boss.entries.mapIndexed { i, boss ->
                BossFight(boss, i + 1, origin.plusWeeks(i.toLong()), bossHp(boss, 1, sessionsPerWeek), 0, BossStatus.LOCKED)
            }
            return Campaign(1, origin, ChronoUnit.DAYS.between(today, origin).toInt(), fights, 0, emptyList())
        }
        val weeksIn = ChronoUnit.DAYS.between(origin, today) / 7
        val campaignIndex = (weeksIn / 6).toInt()
        val damageByWeek = workouts.filter { !it.date.isBefore(origin) }
            .groupBy { ChronoUnit.DAYS.between(origin, it.date) / 7 }
            .mapValues { (_, list) -> list.sumOf(::damage) }

        // Earlier campaigns count towards loot too.
        var defeated = 0
        val loot = mutableListOf<String>()
        for (week in 0 until weeksIn) {
            if (week >= campaignIndex * 6L) break
            val boss = Boss.entries[(week % 6).toInt()]
            if ((damageByWeek[week] ?: 0) >= bossHp(boss, (week / 6).toInt() + 1, sessionsPerWeek)) {
                defeated++
                loot += boss.loot
            }
        }

        val fights = Boss.entries.mapIndexed { i, boss ->
            val week = campaignIndex * 6L + i
            val hp = bossHp(boss, campaignIndex + 1, sessionsPerWeek)
            val damage = damageByWeek[week] ?: 0
            val status = when {
                week > weeksIn -> BossStatus.LOCKED
                damage >= hp -> BossStatus.DEFEATED
                week == weeksIn -> BossStatus.ACTIVE
                else -> BossStatus.ESCAPED
            }
            if (status == BossStatus.DEFEATED) {
                defeated++
                loot += boss.loot
            }
            BossFight(boss, i + 1, origin.plusWeeks(week), hp, if (status == BossStatus.LOCKED) 0 else damage, status)
        }
        return Campaign(campaignIndex + 1, origin.plusWeeks(campaignIndex * 6L), 0, fights, defeated, loot)
    }

    fun voyage(workouts: List<WorkoutRecord>): Voyage {
        val km = workouts.sumOf(::seaKm)
        val strength = workouts.count { w -> w.sets.any { !it.cardio } }
        val reached = Island.entries.filter { km >= it.km }
        return Voyage(
            km = km,
            strengthSessions = strength,
            reached = reached,
            chestsOpened = reached.filter { strength >= it.strengthSessions },
            next = Island.entries.firstOrNull { km < it.km },
        )
    }

    /** Messages for what changed in the realm, e.g. after finishing a workout. */
    fun celebrate(before: Realm, after: Realm): List<String> {
        val messages = mutableListOf<String>()
        val hit = after.campaign.current
        val wasHit = before.campaign.current
        if (after.campaign.bossesDefeated > before.campaign.bossesDefeated) {
            val beaten = after.campaign.fights.lastOrNull { it.status == BossStatus.DEFEATED }
            if (beaten != null) messages += "${beaten.boss.title} is defeated! Loot: ${beaten.boss.loot}."
        } else if (hit != null && wasHit != null && hit.boss == wasHit.boss && hit.damage > wasHit.damage) {
            messages += "Strike! You hit ${hit.boss.title} for ${hit.damage - wasHit.damage} damage. ${hit.hpLeft} HP left."
        }
        (after.voyage.reached - before.voyage.reached.toSet()).forEach {
            messages += "Land ho! Your ship reached ${it.title}."
        }
        (after.voyage.chestsOpened - before.voyage.chestsOpened.toSet()).filter { it != Island.HOME_PORT }.forEach {
            messages += "Chest opened on ${it.title}: ${it.treasure}."
        }
        return messages
    }
}
