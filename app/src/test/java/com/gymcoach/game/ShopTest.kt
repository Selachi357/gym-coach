package com.gymcoach.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ShopTest {
    private val rich = TrainingTotals(30, 30, 300, 300)

    @Test
    fun buyingAPetCostsGoldAndPutsItOn() {
        val before = City.treasury(CityState(), rich).gold
        val s = City.buy(CityState(), rich, ShopItem.WOLF)
        assertEquals(before - 300, City.treasury(s, rich).gold)
        assertEquals(ShopItem.WOLF, s.pet)
        assertNotNull(City.whyNotBuy(s, rich, ShopItem.WOLF))
        assertEquals(null, City.wear(s, ShopKind.PET, null).pet)
    }

    @Test
    fun notEnoughGold() {
        assertNotNull(City.whyNotBuy(CityState(), TrainingTotals(0, 0, 0, 0), ShopItem.DRAGON))
    }

    @Test
    fun charmsStackAndSaveTheBonfire() {
        var s = City.buy(CityState(), rich, ShopItem.EMBER_CHARM)
        s = City.buy(s, rich, ShopItem.EMBER_CHARM)
        assertEquals(2, s.charms)
        val start = LocalDate.of(2026, 11, 11)
        val plan = (0L..5L).map { start.plusDays(it) }.toSet()
        val trained = listOf(0L, 1L, 4L, 5L).map { start.plusDays(it) }
        // Days 2 and 3 missed: Second Wind covers one, a charm the other.
        val b = BonfireCalculator.compute(trained, emptySet(), plan, start.plusDays(5), charms = s.charms)
        assertEquals(4, b.days)
        assertEquals(1, b.charmsUsed)
    }

    @Test
    fun rewardsCostTokens() {
        var s = Shop.addReward(CityState(), "New shoes", 2)
        assertEquals(s, Shop.claim(s, 0, tokensEarned = 1, date = "2026-12-01"))
        s = Shop.claim(s, 0, tokensEarned = 3, date = "2026-12-01")
        assertEquals("2026-12-01", s.rewards[0].claimedOn)
        assertEquals(1, Shop.tokensLeft(3, s.rewards))
        assertEquals(s, Shop.removeReward(s, 0))
    }

    @Test
    fun shopStateSurvivesSaveAndLoad() {
        var s = City.buy(CityState(), rich, ShopItem.FOX)
        s = City.buy(s, rich, ShopItem.HAIR_RED)
        s = City.buy(s, rich, ShopItem.EMBER_CHARM)
        s = Shop.addReward(s, "Favourite meal at the next port", 1)
        assertEquals(s, City.decode(City.encode(s)))
        assertNull(City.decode(City.encode(CityState())).pet)
    }
}
