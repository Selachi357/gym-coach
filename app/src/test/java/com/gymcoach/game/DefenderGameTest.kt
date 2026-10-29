package com.gymcoach.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class DefenderGameTest {
    private val loadout = Loadout(wallHp = 10, tapDamage = 1, archers = 0, rage = false)

    private fun run(game: DefenderGame, seconds: Float) {
        var t = 0f
        while (t < seconds) {
            game.step(0.05f)
            t += 0.05f
        }
    }

    @Test
    fun enemiesWalkToTheWallAndDamageIt() {
        val game = DefenderGame(loadout, Random(1))
        run(game, 30f)
        assertTrue(game.wallHp < 10)
    }

    @Test
    fun tappingKillsEnemiesAndScores() {
        val game = DefenderGame(loadout, Random(1))
        run(game, 1.1f)
        val e = game.enemies.first()
        repeat(e.maxHp) { assertTrue(game.hit(e.id)) }
        assertTrue(game.enemies.none { it.id == e.id })
        assertEquals(1, game.kills)
        assertTrue(game.score > 0)
    }

    @Test
    fun wallFallsAndGameEnds() {
        val game = DefenderGame(loadout.copy(wallHp = 1), Random(1))
        run(game, 30f)
        assertTrue(game.over)
        assertFalse(game.hit(0))
    }

    @Test
    fun archersClearWavesOnTheirOwn() {
        val game = DefenderGame(loadout.copy(archers = 4, wallHp = 100), Random(2))
        run(game, 40f)
        assertTrue(game.wave >= 2)
        assertTrue(game.kills > 0)
    }

    @Test
    fun everyFifthWaveHasABoss() {
        val game = DefenderGame(loadout, Random(3))
        assertTrue(Foe.BOSS in game.waveQueue(5))
        assertFalse(Foe.BOSS in game.waveQueue(4))
    }

    @Test
    fun trainingAndVillageBuildTheLoadout() {
        val hero = HeroCalculator.compute(emptyList(), LocalDate.of(2026, 11, 11))
        val realm = RealmCalculator.compute(emptyList(), LocalDate.of(2026, 11, 11))
        val city = CityDefence(wallHp = 15, archers = 1, damage = 1, repair = 2, mageLevel = 2, slow = 0.12f)
        val l = Loadout.from(hero, realm, city, trainedToday = true)
        assertEquals(25, l.wallHp)
        assertEquals(1, l.archers)
        assertEquals(4, l.tapDamage)
        assertEquals(9f, l.lightningInterval!!, 0.001f)
        assertTrue(l.rage)
    }

    @Test
    fun templeRepairsTheWallBetweenWaves() {
        val game = DefenderGame(loadout.copy(wallHp = 100, archers = 6, repair = 5), Random(4))
        run(game, 25f)
        assertTrue(game.wave >= 2)
        assertEquals(100, game.wallHp)
    }

    @Test
    fun lightningHitsEveryEnemy() {
        val game = DefenderGame(loadout.copy(wallHp = 100, mageLevel = 3), Random(5))
        run(game, 7f)
        assertTrue(game.kills > 0)
    }
}
