package com.gymcoach.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RealmCalculatorTest {
    private val start = LocalDate.of(2026, 11, 11)

    private fun strength(date: LocalDate, sets: Int = 10, program: Boolean = true) =
        WorkoutRecord(date, program, List(sets) { SetRecord(exerciseId = 1, weightKg = 20.0, reps = 10, cardio = false) })

    private fun cardio(date: LocalDate, minutes: Int = 30, km: Double = 0.0) =
        WorkoutRecord(date, true, listOf(SetRecord(exerciseId = 2, weightKg = 0.0, reps = minutes, cardio = true, distanceKm = km)))

    /** A week of the onboard plan: strength on days 1, 3, 5 and cardio on days 2 and 6. */
    private fun planWeek(weekStart: LocalDate, sets: Int = 10) = listOf(
        strength(weekStart, sets), cardio(weekStart.plusDays(1)), strength(weekStart.plusDays(2), sets),
        strength(weekStart.plusDays(4), sets), cardio(weekStart.plusDays(5)),
    )

    @Test
    fun campaignWaitsForThePlanToStart() {
        val c = RealmCalculator.campaign(emptyList(), start.minusDays(3), start, 5)
        assertEquals(3, c.daysUntilStart)
        assertTrue(c.fights.all { it.status == BossStatus.LOCKED })
        assertNull(c.current)
    }

    @Test
    fun firstBossIsActiveAndTakesDamage() {
        val c = RealmCalculator.campaign(listOf(strength(start)), start, start, 5)
        val fight = c.current!!
        assertEquals(Boss.RAT_KING, fight.boss)
        assertEquals(30 + 10 * 5, fight.damage)
        assertEquals(200 - 80, fight.hpLeft)
    }

    @Test
    fun fullPlanWeekBeatsTheBoss() {
        val c = RealmCalculator.campaign(planWeek(start), start.plusDays(6), start, 5)
        assertEquals(BossStatus.DEFEATED, c.fights[0].status)
        assertEquals(listOf("Rat King's Crown"), c.loot)
    }

    @Test
    fun skippedWeekLetsTheBossEscape() {
        val c = RealmCalculator.campaign(planWeek(start), start.plusWeeks(2), start, 5)
        assertEquals(BossStatus.DEFEATED, c.fights[0].status)
        assertEquals(BossStatus.ESCAPED, c.fights[1].status)
        assertEquals(BossStatus.ACTIVE, c.fights[2].status)
        assertEquals(BossStatus.LOCKED, c.fights[3].status)
    }

    @Test
    fun allSixWeeksOfThePlanBeatEveryBoss() {
        val log = (0 until 6).flatMap { w -> planWeek(start.plusWeeks(w.toLong()), sets = when { w < 2 -> 10; w < 4 -> 15; else -> 20 }) }
        val c = RealmCalculator.campaign(log, start.plusWeeks(5).plusDays(6), start, 5)
        assertTrue(c.fights.all { it.status == BossStatus.DEFEATED })
        assertEquals(6, c.bossesDefeated)
    }

    @Test
    fun secondCampaignStartsAfterSixWeeksWithTougherBosses() {
        val log = planWeek(start)
        val c = RealmCalculator.campaign(log, start.plusWeeks(6), start, 5)
        assertEquals(2, c.number)
        assertEquals(1, c.bossesDefeated)
        assertEquals(220, c.fights[0].hp)
    }

    @Test
    fun hpScalesWithWorkoutsPerWeek() {
        assertEquals(120, RealmCalculator.bossHp(Boss.RAT_KING, 1, 3))
    }

    @Test
    fun cardioSailsAndStrengthOpensChests() {
        val log = listOf(cardio(start, 30, km = 6.5), cardio(start.plusDays(1), 36)) +
            List(4) { strength(start.plusDays(it + 2L)) }
        val v = RealmCalculator.voyage(log)
        assertEquals(12.5, v.km, 0.001)
        assertEquals(Island.SMUGGLERS_COVE, v.at)
        assertEquals(Island.MERMAID_REEF, v.next)
        assertTrue(Island.SMUGGLERS_COVE in v.chestsOpened)
    }

    @Test
    fun celebrateReportsNewThings() {
        val before = RealmCalculator.compute(emptyList(), start, start)
        val after = RealmCalculator.compute(planWeek(start), start.plusDays(5), start)
        val messages = RealmCalculator.celebrate(before, after)
        assertTrue(messages.any { it.contains("defeated") })
        assertTrue(messages.any { it.contains("Gull Rock") })
    }
}
