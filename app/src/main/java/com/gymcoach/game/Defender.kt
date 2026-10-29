package com.gymcoach.game

import kotlin.random.Random

/** Enemies in Castle Defender. [speed] is the share of the field crossed per second. */
enum class Foe(val title: String, val hp: Int, val speed: Float, val damage: Int, val size: Float, val points: Int) {
    GOBLIN("Goblin", 2, 0.10f, 1, 1.0f, 1),
    BAT("Bat", 1, 0.16f, 1, 0.8f, 1),
    ORC("Orc", 5, 0.07f, 2, 1.2f, 3),
    TROLL("Troll", 12, 0.045f, 4, 1.6f, 6),
    BOSS("Boss", 20, 0.035f, 10, 2.2f, 25),
}

/** What your training gives you in the game. */
data class Loadout(
    val wallHp: Int,
    val tapDamage: Int,
    val archers: Int,
    /** Trained today: every tap does double damage. */
    val rage: Boolean,
) {
    companion object {
        private val archerBuildings = setOf(Building.WATCHTOWER, Building.BARRACKS, Building.GATEHOUSE, Building.ROYAL_TOWER)

        /**
         * Castle buildings give the wall its HP and the towers and barracks give archers.
         * Hero level and slain bosses make your arrows hit harder; a workout today doubles them.
         */
        fun from(hero: HeroStats, realm: Realm, trainedToday: Boolean): Loadout {
            val base = 1 + hero.level / 5 + realm.campaign.bossesDefeated / 2
            return Loadout(
                wallHp = 10 + 3 * realm.castle.built.size,
                tapDamage = if (trainedToday) base * 2 else base,
                archers = realm.castle.built.count { it in archerBuildings },
                rage = trainedToday,
            )
        }
    }
}

class Enemy(val id: Int, val foe: Foe, val maxHp: Int, val lane: Int, val speed: Float) {
    var hp = maxHp
    /** 1 at the far right where enemies appear, down to [DefenderGame.WALL_X] at the castle wall. */
    var x = 1.05f
    /** Seconds left of the white flash after a hit. */
    var flash = 0f
}

/** An arrow on its way, for drawing only: damage is applied when it is fired. */
data class Shot(val toX: Float, val lane: Int, val fromArcher: Boolean, var ttl: Float)

/**
 * A tap-to-shoot castle defence game. Waves of enemies walk along three lanes towards your wall;
 * tap them to shoot. Archers fire on their own at the enemy closest to the wall.
 * Every fifth wave ends with a boss. The game ends when the wall falls.
 */
class DefenderGame(val loadout: Loadout, private val random: Random = Random.Default) {
    companion object {
        const val LANES = 3
        const val WALL_X = 0.14f
        const val ARCHER_INTERVAL = 1.2f
        const val SHOT_TTL = 0.12f
        const val WAVE_PAUSE = 2.5f
    }

    val enemies = mutableListOf<Enemy>()
    val shots = mutableListOf<Shot>()
    var wave = 1
        private set
    var wallHp = loadout.wallHp
        private set
    var score = 0
        private set
    var kills = 0
        private set
    val over: Boolean get() = wallHp <= 0
    /** Seconds the "WAVE n" banner stays up. */
    var waveBanner = 2f
        private set
    /** Seconds the wall stays red after a hit. */
    var wallFlash = 0f
        private set

    private var nextId = 0
    private var queue = waveQueue(1)
    private var spawnTimer = 1.0f
    private var archerTimer = ARCHER_INTERVAL
    private var pause = 0f

    fun waveQueue(w: Int): MutableList<Foe> {
        val list = mutableListOf<Foe>()
        repeat(3 + 2 * w) {
            val roll = random.nextFloat()
            list += when {
                w >= 5 && roll < 0.15f -> Foe.TROLL
                w >= 3 && roll < 0.40f -> Foe.ORC
                w >= 2 && roll < 0.65f -> Foe.BAT
                else -> Foe.GOBLIN
            }
        }
        if (w % 5 == 0) list += Foe.BOSS
        return list
    }

    private fun spawnInterval() = maxOf(0.5f, 1.6f - 0.08f * wave)

    private fun spawn(foe: Foe) {
        val hp = if (foe == Foe.BOSS) 20 + 8 * wave else foe.hp + (wave - 1) / 4
        enemies += Enemy(nextId++, foe, hp, random.nextInt(LANES), foe.speed * (1 + 0.04f * (wave - 1)))
    }

    /** Advances the game by [dt] seconds. */
    fun step(dt: Float) {
        if (over) return
        waveBanner = (waveBanner - dt).coerceAtLeast(0f)
        wallFlash = (wallFlash - dt).coerceAtLeast(0f)
        shots.forEach { it.ttl -= dt }
        shots.removeAll { it.ttl <= 0f }

        if (queue.isEmpty() && enemies.isEmpty()) {
            pause += dt
            if (pause >= WAVE_PAUSE) {
                score += 10 * wave
                wave++
                queue = waveQueue(wave)
                pause = 0f
                spawnTimer = 0.5f
                waveBanner = 2f
            }
        } else if (queue.isNotEmpty()) {
            spawnTimer -= dt
            if (spawnTimer <= 0f) {
                spawn(queue.removeAt(0))
                spawnTimer = spawnInterval()
            }
        }

        val iterator = enemies.iterator()
        while (iterator.hasNext()) {
            val e = iterator.next()
            e.flash = (e.flash - dt).coerceAtLeast(0f)
            e.x -= e.speed * dt
            if (e.x <= WALL_X) {
                wallHp = (wallHp - e.foe.damage).coerceAtLeast(0)
                wallFlash = 0.3f
                iterator.remove()
            }
        }

        if (loadout.archers > 0) {
            archerTimer -= dt
            if (archerTimer <= 0f) {
                archerTimer = ARCHER_INTERVAL
                repeat(loadout.archers) {
                    val target = enemies.filter { it.x < 1f }.minByOrNull { it.x } ?: return@repeat
                    damage(target, 1, fromArcher = true)
                }
            }
        }
    }

    /** A tap on enemy [id]. Returns true if it hit something. */
    fun hit(id: Int): Boolean {
        if (over) return false
        val target = enemies.firstOrNull { it.id == id } ?: return false
        damage(target, loadout.tapDamage, fromArcher = false)
        return true
    }

    private fun damage(target: Enemy, amount: Int, fromArcher: Boolean) {
        shots += Shot(target.x, target.lane, fromArcher, SHOT_TTL)
        target.hp -= amount
        target.flash = 0.1f
        if (target.hp <= 0) {
            enemies.remove(target)
            kills++
            score += target.foe.points
        }
    }
}
