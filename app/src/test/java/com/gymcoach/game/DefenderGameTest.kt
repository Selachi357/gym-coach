package com.gymcoach.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class DefenderGameTest {
    private val base = Loadout()

    private fun run(game: DefenderGame, seconds: Float) {
        var t = 0f
        while (t < seconds && !game.over) {
            game.step(0.05f)
            t += 0.05f
        }
    }

    /** Road tiles within reach of a spot: a rough measure of how good the spot is. */
    private fun coverage(g: DefenderGame, spot: Int): Int {
        val c = g.spotCenter(spot)
        var n = 0
        var d = 0f
        while (d < g.pathLength) {
            if (g.pointAt(d).dist(c) <= 2.5f) n++
            d += 0.1f
        }
        return n
    }

    /** A simple player: builds on the best spots in turn, then upgrades, and uses spells on crowds. */
    private fun play(stage: Stage, loadout: Loadout, seed: Int): DefenderGame {
        val g = DefenderGame(stage, loadout, Random(seed))
        val order = stage.spots.indices.sortedByDescending { coverage(g, it) }
        val kinds = listOf(TowerKind.ARCHER, TowerKind.CANNON, TowerKind.ARCHER, TowerKind.FROST)
        var built = 0
        g.callWave()
        var t = 0f
        while (!g.over && t < 2000f) {
            g.step(0.05f)
            t += 0.05f
            if ((t * 20).toInt() % 10 != 0) continue
            val free = order.firstOrNull { it !in g.towers }
            val kind = kinds[built % kinds.size]
            if (free != null && g.gold >= kind.costs[0]) {
                if (g.build(free, kind)) built++
            } else if (g.towers.size >= 4 || free == null) {
                g.towers.values.filter { it.upgradeCost != null }.minByOrNull { it.upgradeCost!! }
                    ?.let { if (g.gold >= it.upgradeCost!!) g.upgrade(it.spot) }
            }
            if (g.enemies.size > 6) {
                g.castFreeze()
                g.enemies.maxByOrNull { it.dist }?.let { g.castMeteor(g.position(it)) }
            }
        }
        return g
    }

    @Test
    fun mapsAreValid() {
        for (stage in Stage.entries) {
            assertEquals(12, stage.spots.size)
            assertEquals(stage.spots.size, stage.spots.toSet().size)
            for ((x, y) in stage.spots) {
                assertTrue("${stage.name} spot $x,$y", x in 0 until Stage.COLS && y in 0 until Stage.ROWS)
                assertFalse("${stage.name} spot $x,$y is on the road", (x to y) in stage.roadTiles)
            }
            // The road starts off the map and ends at a gate on the bottom row.
            val (sx, sy) = stage.road.first()
            assertFalse(sx in 0 until Stage.COLS && sy in 0 until Stage.ROWS)
            assertEquals(Stage.ROWS - 1, stage.gate.second)
            // Every turn is a straight line.
            stage.road.zipWithNext().forEach { (a, b) -> assertTrue(a.first == b.first || a.second == b.second) }
        }
    }

    @Test
    fun enemiesWalkTheRoadAndCostLives() {
        val game = DefenderGame(Stage.FOREST_ROAD, base, Random(1))
        assertFalse(game.started)
        game.callWave()
        run(game, 40f)
        assertTrue(game.lives < base.lives)
    }

    @Test
    fun withoutTowersTheCastleFalls() {
        val game = DefenderGame(Stage.FOREST_ROAD, base, Random(1))
        game.callWave()
        run(game, 1000f)
        assertTrue(game.lost)
        assertFalse(game.build(0, TowerKind.ARCHER))
    }

    @Test
    fun buildUpgradeAndSell() {
        val game = DefenderGame(Stage.FOREST_ROAD, base.copy(startGold = 400), Random(1))
        assertTrue(game.build(0, TowerKind.ARCHER))
        assertEquals(330, game.gold)
        assertFalse(game.build(0, TowerKind.CANNON))
        assertFalse(game.build(1, TowerKind.MAGE))
        assertTrue(game.upgrade(0))
        assertTrue(game.upgrade(0))
        assertFalse(game.upgrade(0))
        assertEquals(3, game.towers.getValue(0).level)
        assertTrue(game.sell(0))
        assertEquals(400 - 320 + 320 * 3 / 5, game.gold)
    }

    @Test
    fun towersKillEnemiesForGold() {
        val game = DefenderGame(Stage.FOREST_ROAD, base, Random(2))
        game.build(0, TowerKind.ARCHER)
        game.build(1, TowerKind.ARCHER)
        val gold = game.gold
        game.callWave()
        run(game, 20f)
        assertTrue(game.kills > 0)
        assertTrue(game.gold > gold)
    }

    @Test
    fun cannonsCannotHitBats() {
        val game = DefenderGame(Stage.FOREST_ROAD, base, Random(1))
        assertFalse(TowerKind.CANNON.hitsAir)
        assertTrue(TowerKind.ARCHER.hitsAir)
        assertTrue(Foe.BAT.flying)
        assertTrue(game.waveQueue(5).contains(Foe.BOSS))
        assertFalse(game.waveQueue(4).contains(Foe.BOSS))
        assertTrue(game.waveQueue(Stage.FOREST_ROAD.waves).contains(Foe.BOSS))
    }

    @Test
    fun callingEarlyPaysGold() {
        val game = DefenderGame(Stage.FOREST_ROAD, base, Random(1))
        game.callWave()
        run(game, 10f)
        assertTrue(game.canCallWave)
        val bonus = game.callWave()
        assertTrue(bonus > 0)
        assertEquals(2, game.wave)
    }

    @Test
    fun spellsNeedCooldownUnlessYouTrainedToday() {
        val rested = DefenderGame(Stage.FOREST_ROAD, base, Random(1))
        rested.callWave()
        assertFalse(rested.castFreeze())
        val trained = DefenderGame(Stage.FOREST_ROAD, base.copy(rage = true), Random(1))
        trained.callWave()
        run(trained, 6f)
        assertTrue(trained.castMeteor(trained.position(trained.enemies.first())))
        assertTrue(trained.castFreeze())
        assertFalse(trained.castFreeze())
    }

    @Test
    fun balance() {
        // A plain player wins the first stage without any village or training.
        assertTrue((1..3).count { play(Stage.FOREST_ROAD, base, it).won } >= 2)
        // The last stage needs a stronger village and training.
        assertTrue((1..3).none { play(Stage.DRAGON_PASS, base, it).won })
        val trained = Loadout(lives = 22, startGold = 350, damageBonus = 0.08f, archerBonus = 0.1f, rage = true)
        assertTrue((1..3).all { play(Stage.DRAGON_PASS, trained, it).won })
        assertTrue(play(Stage.HARBOUR_GATE, trained, 1).stars >= 2)
    }

    @Test
    fun trainingAndVillageBuildTheLoadout() {
        val hero = HeroCalculator.compute(emptyList(), LocalDate.of(2026, 11, 11))
        val realm = RealmCalculator.compute(emptyList(), LocalDate.of(2026, 11, 11))
        val city = CityDefence(wallHp = 16, archers = 1, damage = 1, repair = 2, mageLevel = 2, slow = 0.12f)
        val l = Loadout.from(hero, realm, city, trainedToday = true)
        assertEquals(24, l.lives)
        assertEquals(1, l.regen)
        assertTrue(l.mageUnlocked)
        assertTrue(l.canBuild(TowerKind.MAGE))
        assertFalse(Loadout().canBuild(TowerKind.MAGE))
        assertEquals((1f + 0.08f + 0.1f) * 1.25f, l.damageFactor(TowerKind.ARCHER), 0.001f)
        assertTrue(l.rage)
    }
}
