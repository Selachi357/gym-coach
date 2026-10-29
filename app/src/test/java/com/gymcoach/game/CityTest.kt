package com.gymcoach.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CityTest {
    private val none = TrainingTotals(0, 0, 0, 0)
    private val week = TrainingTotals(workouts = 5, planWorkouts = 5, strengthSets = 36, cardioMinutes = 70)

    @Test
    fun newCityHasTownHallAndStartingStash() {
        val s = CityState()
        assertEquals(1, City.hallLevel(s))
        assertEquals(City.START, City.treasury(s, none))
    }

    @Test
    fun trainingFillsTheTreasury() {
        // 5 workouts: 75 + 25 gold, 70/3 wood, 72 stone, 25 food on top of the start.
        assertEquals(City.START + Res(100, 23, 72, 25), City.treasury(CityState(), week))
    }

    @Test
    fun buildingCostsAndProducesForLaterWorkouts() {
        var s = City.build(CityState(), none, BuildingType.HOUSE, 3, 5)
        assertEquals(City.START - BuildingType.HOUSE.cost, City.treasury(s, none))
        val later = none.copy(workouts = 2)
        // Two workouts: 2 × 15 training gold + 2 × 3 from the house.
        assertEquals(City.START.gold - 20 + 30 + 6, City.treasury(s, later).gold)
        s = City.build(s, later, BuildingType.WELL, 2, 5)
        assertEquals(Res(gold = 4), City.production(s, City.buildingAt(s, 3, 5)!!))
    }

    @Test
    fun placementRules() {
        val s = CityState()
        assertNotNull(City.whyNot(s, none, BuildingType.FARM, 8, 2)) // water
        assertNotNull(City.whyNot(s, none, BuildingType.FARM, 0, 0)) // tree
        assertNotNull(City.whyNot(s, none, BuildingType.FARM, City.HALL_X, City.HALL_Y)) // taken
        assertNotNull(City.whyNot(s, none, BuildingType.ARCHER_TOWER, 3, 3)) // needs hall 2
        assertNull(City.whyNot(s, none, BuildingType.FARM, 3, 3))
    }

    @Test
    fun adjacencyBonuses() {
        val rich = TrainingTotals(20, 20, 200, 300)
        var s = City.build(CityState(), rich, BuildingType.LUMBER_MILL, 2, 1) // trees to the left and above
        assertEquals(Res(wood = 3 + 2), City.production(s, City.buildingAt(s, 2, 1)!!))
        s = City.build(s, rich, BuildingType.FARM, 6, 3) // water to the right
        assertEquals(Res(food = 4), City.production(s, City.buildingAt(s, 6, 3)!!))
    }

    @Test
    fun clearingATreeGivesWood() {
        val s = City.clear(CityState(), none, 0, 0)
        assertEquals(Terrain.GRASS, City.terrain(s, 0, 0))
        assertEquals(City.START + Res(gold = -5, wood = 10), City.treasury(s, none))
    }

    @Test
    fun upgradesNeedTheTownHall() {
        val rich = TrainingTotals(40, 40, 400, 600)
        var s = City.build(CityState(), rich, BuildingType.WALL, 3, 3)
        val wall = City.buildingAt(s, 3, 3)!!
        assertNotNull(City.whyNotUpgrade(s, rich, wall))
        s = City.upgrade(s, rich, City.buildingAt(s, City.HALL_X, City.HALL_Y)!!)
        assertEquals(2, City.hallLevel(s))
        s = City.upgrade(s, rich, City.buildingAt(s, 3, 3)!!)
        assertEquals(2, City.buildingAt(s, 3, 3)!!.level)
        assertEquals(10 + 8, City.defence(s).wallHp)
    }

    @Test
    fun saveAndLoadRoundTrip() {
        val rich = TrainingTotals(20, 20, 200, 300)
        var s = City.build(CityState(), rich, BuildingType.HOUSE, 3, 3)
        s = City.clear(s, rich, 0, 0).copy(loot = Res(5, 6, 7, 8))
        assertEquals(s, City.decode(City.encode(s)))
        assertEquals(CityState(), City.decode("garbage"))
    }

    @Test
    fun plunderGrowsWithTheLibrary() {
        assertEquals(Res(25, 10, 10, 10), City.plunder(wave = 5, score = 100))
        assertTrue(City.plunder(5, 100, bonus = 0.3f).gold > 25)
    }
}
