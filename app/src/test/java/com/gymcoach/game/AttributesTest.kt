package com.gymcoach.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AttributesTest {
    private val day = LocalDate.of(2026, 11, 11)

    private fun lifts(n: Int) = List(n) { SetRecord(1, 20.0, 10, cardio = false) }
    private fun cardio(minutes: Int) = SetRecord(2, 0.0, minutes, cardio = true)
    private fun mobility(n: Int) = List(n) { SetRecord(3, 0.0, 30, cardio = false, mobility = true) }

    @Test
    fun valuesGrowWithTheSquareRoot() {
        assertEquals(1, AttributeCalculator.value(0))
        assertEquals(2, AttributeCalculator.value(1))
        assertEquals(11, AttributeCalculator.value(100))
        assertEquals(0.4f, AttributeCalculator.progress(6), 0.01f) // 2 of the 5 points from 4 to 9
    }

    @Test
    fun eachKindOfTrainingFeedsItsAttribute() {
        val log = listOf(WorkoutRecord(day, true, lifts(16) + cardio(100) + mobility(8)))
        val a = AttributeCalculator.compute(log, contracts = 1, keptWeeks = 1, bestBonfire = 6)
        assertEquals(5, a[Attribute.STR]) // 16 sets
        assertEquals(5, a[Attribute.VIT]) // 20 points
        assertEquals(5, a[Attribute.AGI]) // 16 points
        assertEquals(3, a[Attribute.WIS]) // 2 + 3
        assertEquals(4, a[Attribute.CON]) // 3 + 6
    }

    @Test
    fun classFollowsTheTrainingMix() {
        fun attrs(str: Int, vit: Int, agi: Int) = Attributes(
            mapOf(Attribute.STR to str, Attribute.VIT to vit, Attribute.AGI to agi, Attribute.WIS to 1, Attribute.CON to 1),
            emptyMap(),
        )
        assertEquals(HeroClass.RECRUIT, AttributeCalculator.heroClass(attrs(2, 2, 1)))
        assertEquals(HeroClass.WARRIOR, AttributeCalculator.heroClass(attrs(10, 4, 2)))
        assertEquals(HeroClass.RANGER, AttributeCalculator.heroClass(attrs(3, 9, 2)))
        assertEquals(HeroClass.VANGUARD, AttributeCalculator.heroClass(attrs(10, 9, 2)))
        assertEquals(HeroClass.PALADIN, AttributeCalculator.heroClass(attrs(8, 8, 7)))
    }

    @Test
    fun gearUnlocksFromAchievementsAndRespectsPicks() {
        val log = List(10) { WorkoutRecord(day.plusDays(it.toLong()), true, lifts(8)) }
        val hero = HeroCalculator.compute(log, day.plusDays(10))
        val attrs = AttributeCalculator.compute(log, contracts = 0, keptWeeks = 0, bestBonfire = 0)
        val progress = Progress(hero, attrs, bestBonfire = 14, contracts = 0, slain = setOf(Boss.SEA_SERPENT), planWorkouts = 10)
        assertTrue(Gear.LEATHER.unlocked(progress))
        assertTrue(Gear.IRON_SWORD.unlocked(progress)) // 80 sets → STR 9
        assertFalse(Gear.PLATE.unlocked(progress))
        val auto = Gear.equipped(progress, emptyMap())
        assertEquals(Gear.SERPENT_SHIELD, auto[Slot.SHIELD])
        assertEquals(Gear.BLUE_CAPE, auto[Slot.CAPE])
        val picked = Gear.equipped(progress, mapOf(Slot.CAPE to Gear.NO_CAPE, Slot.ARMOUR to Gear.PLATE))
        assertEquals(Gear.NO_CAPE, picked[Slot.CAPE])
        assertEquals(Gear.LEATHER, picked[Slot.ARMOUR]) // plate is still locked
    }
}
