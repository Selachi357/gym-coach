package com.gymcoach.game

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** A point on the map in tiles; (0.5, 0.5) is the middle of the top-left tile. */
data class Pt(val x: Float, val y: Float) {
    fun dist(o: Pt) = hypot(x - o.x, y - o.y)
}

/**
 * A battlefield: a road winding from the edge of the map to your castle, with build spots beside it.
 * The map is [COLS] × [ROWS] tiles. [road] lists the road's corners as tiles; the first one lies
 * just off the map, where the enemies come from, and the last one is the castle gate.
 */
enum class Stage(
    val title: String,
    val description: String,
    val waves: Int,
    val difficulty: Float,
    val road: List<Pair<Int, Int>>,
    val spots: List<Pair<Int, Int>>,
) {
    FOREST_ROAD(
        "Forest Road", "Goblins come down the old forest road. A good place to learn.", 10, 0.85f,
        listOf(1 to -1, 1 to 3, 7 to 3, 7 to 7, 2 to 7, 2 to 10, 5 to 10, 5 to 12),
        listOf(2 to 1, 4 to 2, 6 to 2, 3 to 5, 5 to 5, 8 to 5, 4 to 8, 6 to 8, 1 to 9, 3 to 9, 4 to 11, 6 to 11),
    ),
    HARBOUR_GATE(
        "Harbour Gate", "Raiders land at the harbour and march on the gate. Bats fly over the walls.", 12, 1.1f,
        listOf(4 to -1, 4 to 2, 1 to 2, 1 to 6, 7 to 6, 7 to 9, 4 to 9, 4 to 12),
        listOf(2 to 1, 5 to 1, 0 to 4, 2 to 4, 4 to 5, 6 to 5, 3 to 8, 6 to 8, 8 to 7, 3 to 10, 5 to 10, 5 to 11),
    ),
    DRAGON_PASS(
        "Dragon Pass", "The long mountain pass. Trolls, shamans and the worst bosses of the realm.", 15, 1.35f,
        listOf(-1 to 1, 6 to 1, 6 to 4, 2 to 4, 2 to 7, 7 to 7, 7 to 10, 4 to 10, 4 to 12),
        listOf(1 to 2, 3 to 2, 5 to 2, 7 to 3, 1 to 5, 3 to 5, 5 to 5, 5 to 8, 8 to 8, 6 to 9, 3 to 11, 5 to 11),
    ),
    ;

    val path: List<Pt> get() = road.map { (x, y) -> Pt(x + 0.5f, y + 0.5f) }

    /** Every map tile the road covers. */
    val roadTiles: Set<Pair<Int, Int>> by lazy {
        val tiles = mutableSetOf<Pair<Int, Int>>()
        road.zipWithNext().forEach { (a, b) ->
            val dx = (b.first - a.first).coerceIn(-1, 1)
            val dy = (b.second - a.second).coerceIn(-1, 1)
            var x = a.first
            var y = a.second
            while (true) {
                if (x in 0 until COLS && y in 0 until ROWS) tiles += x to y
                if (x == b.first && y == b.second) break
                x += dx
                y += dy
            }
        }
        tiles
    }

    val gate: Pair<Int, Int> get() = road.last()

    companion object {
        const val COLS = 9
        const val ROWS = 13
    }
}

/** Enemies. [speed] is in tiles per second, [armor] the share of arrow and cannon damage it blocks. */
enum class Foe(
    val title: String,
    val hp: Int,
    val speed: Float,
    val armor: Float,
    val gold: Int,
    val lives: Int,
    val size: Float,
    val flying: Boolean = false,
    /** Heals other enemies close by, HP per second. */
    val heal: Float = 0f,
    /** Heals itself, HP per second. */
    val regen: Float = 0f,
) {
    GOBLIN("Goblin", 22, 1.3f, 0f, 8, 1, 0.55f),
    WOLF("Wolf rider", 20, 2.3f, 0f, 10, 1, 0.6f),
    BAT("Bat", 16, 1.7f, 0f, 9, 1, 0.5f, flying = true),
    ORC("Orc", 70, 0.95f, 0.45f, 18, 1, 0.7f),
    SHAMAN("Shaman", 45, 1.0f, 0f, 20, 1, 0.6f, heal = 6f),
    TROLL("Troll", 180, 0.7f, 0.2f, 40, 2, 0.85f, regen = 4f),
    BOSS("Boss", 700, 0.5f, 0.3f, 200, 6, 1.3f),
}

/** Towers you can build on a spot, each with three levels. */
enum class TowerKind(
    val title: String,
    val description: String,
    val costs: List<Int>,
    val range: List<Float>,
    val damage: List<Int>,
    val cooldown: List<Float>,
    /** Radius of the blast around the target, in tiles. 0 hits only the target. */
    val splash: Float = 0f,
    /** Magic goes straight through armour. */
    val magic: Boolean = false,
    val hitsAir: Boolean = true,
    /** Share of speed taken off whatever it hits, for 2 seconds. */
    val slow: List<Float> = listOf(0f, 0f, 0f),
) {
    ARCHER(
        "Archers", "Fast arrows. Hits bats.",
        listOf(70, 100, 150), listOf(2.6f, 2.9f, 3.2f), listOf(6, 11, 17), listOf(0.8f, 0.7f, 0.6f),
    ),
    CANNON(
        "Cannon", "Slow, heavy blasts that hit a whole group. Can't hit bats.",
        listOf(110, 150, 220), listOf(2.3f, 2.5f, 2.7f), listOf(15, 28, 45), listOf(2.2f, 2.0f, 1.8f),
        splash = 0.9f, hitsAir = false,
    ),
    MAGE(
        "Mage", "Arcane bolts that ignore armour. Needs a mage tower in your village.",
        listOf(100, 150, 230), listOf(2.5f, 2.7f, 3.0f), listOf(18, 32, 52), listOf(1.5f, 1.4f, 1.2f),
        magic = true,
    ),
    FROST(
        "Frost", "Slows everything it hits.",
        listOf(80, 110, 160), listOf(2.2f, 2.4f, 2.6f), listOf(3, 5, 8), listOf(1.0f, 1.0f, 0.9f),
        splash = 0.7f, magic = true, slow = listOf(0.35f, 0.45f, 0.55f),
    ),
    ;

    val maxLevel: Int get() = costs.size
}

enum class Spell(val title: String, val cooldown: Float) {
    METEOR("Meteor", 35f),
    FREEZE("Freeze", 45f),
}

/** What your training and your village bring to the battle. */
data class Loadout(
    val lives: Int = 20,
    val startGold: Int = 300,
    /** Extra tower damage from blacksmiths and slain bosses, e.g. 0.16 is +16%. */
    val damageBonus: Float = 0f,
    /** Extra damage for archer towers from the village's archer towers. */
    val archerBonus: Float = 0f,
    /** Lives won back after every wave, from temples. */
    val regen: Int = 0,
    val mageUnlocked: Boolean = false,
    /** Share of enemy speed taken off by the stable's patrols. */
    val slow: Float = 0f,
    /** Makes meteors hit harder. */
    val spellPower: Int = 0,
    /** Trained today: +25% damage and spells ready from the start. */
    val rage: Boolean = false,
) {
    fun damageFactor(kind: TowerKind): Float =
        (1f + damageBonus + if (kind == TowerKind.ARCHER) archerBonus else 0f) * if (rage) 1.25f else 1f

    val meteorDamage: Int get() = 80 + 25 * spellPower

    fun canBuild(kind: TowerKind) = kind != TowerKind.MAGE || mageUnlocked

    companion object {
        fun from(hero: HeroStats, realm: Realm, city: CityDefence, trainedToday: Boolean) = Loadout(
            lives = 20 + city.wallHp / 4,
            startGold = 300 + 10 * min(hero.level, 30),
            damageBonus = 0.08f * city.damage + 0.03f * realm.campaign.bossesDefeated,
            archerBonus = 0.1f * city.archers,
            regen = city.repair / 2,
            mageUnlocked = city.mageLevel > 0,
            slow = city.slow,
            spellPower = city.mageLevel + realm.campaign.bossesDefeated / 2,
            rage = trainedToday,
        )
    }
}

class Enemy(val id: Int, val foe: Foe, val maxHp: Int, val speed: Float, val lane: Float) {
    var hp = maxHp.toFloat()
    /** Distance walked along the road, in tiles. */
    var dist = 0f
    var slowLeft = 0f
    var slowBy = 0f
    var flash = 0f
}

class Tower(val spot: Int, val kind: TowerKind) {
    var level = 1
        internal set
    var spent = kind.costs[0]
        internal set
    internal var reload = 0f
    /** Where the last shot went, for drawing the aim. */
    var aim: Pt? = null
        internal set
    val range: Float get() = kind.range[level - 1]
    val upgradeCost: Int? get() = kind.costs.getOrNull(level)
    val sellValue: Int get() = spent * 3 / 5
}

/** Something in the air, for drawing only: damage is dealt when it is fired. */
data class Shot(val from: Pt, val to: Pt, val kind: TowerKind?, var ttl: Float, val total: Float)

/** An explosion or frost burst, for drawing only. */
data class Blast(val at: Pt, val radius: Float, val frost: Boolean, var ttl: Float, val total: Float)

private class Meteor(val at: Pt, var delay: Float)

/**
 * A tower defence battle in the style of the classic mobile castle defence games.
 * Enemies walk the road to your castle; every one that gets through costs lives. Build towers on
 * the spots by the road with the gold you start with and earn from kills, upgrade them, and use
 * your spells. Call the next wave early for bonus gold. Survive every wave to win the stage.
 */
class DefenderGame(val stage: Stage, val loadout: Loadout, private val random: Random = Random.Default) {
    companion object {
        const val WAVE_GAP = 14f
        const val SHOT_TIME = 0.18f
        const val FREEZE_TIME = 4f
        const val FREEZE_SLOW = 0.75f
        const val METEOR_RADIUS = 1.4f
        const val EARLY_GOLD_PER_SECOND = 2
    }

    val path = stage.path
    private val segments = path.zipWithNext()
    val pathLength = segments.sumOf { (a, b) -> a.dist(b).toDouble() }.toFloat()

    val enemies = mutableListOf<Enemy>()
    val towers = mutableMapOf<Int, Tower>()
    val shots = mutableListOf<Shot>()
    val blasts = mutableListOf<Blast>()
    private val meteors = mutableListOf<Meteor>()

    var gold = loadout.startGold
        private set
    var lives = loadout.lives
        private set
    /** The wave in progress, 0 before the first. */
    var wave = 0
        private set
    var kills = 0
        private set
    var score = 0
        private set
    /** Seconds until the next wave comes by itself, or null while one is still arriving or before the start. */
    var nextWaveIn: Float? = null
        private set
    var freezeLeft = 0f
        private set
    /** Seconds the castle stays red after an enemy gets through. */
    var gateFlash = 0f
        private set
    /** Seconds the "WAVE n" banner stays up. */
    var waveBanner = 0f
        private set
    val cooldowns = mutableMapOf<Spell, Float>().apply {
        Spell.entries.forEach { put(it, if (loadout.rage) 0f else it.cooldown / 2) }
    }

    val lost: Boolean get() = lives <= 0
    val won: Boolean get() = !lost && wave == stage.waves && queue.isEmpty() && enemies.isEmpty()
    val over: Boolean get() = lost || won
    val started: Boolean get() = wave > 0
    /** The next wave can be called: the current one has fully arrived. */
    val canCallWave: Boolean get() = !over && wave < stage.waves && queue.isEmpty()

    /** 3 stars for a near-perfect defence, 2 for keeping half your lives, 1 for any win. */
    val stars: Int get() = when {
        !won -> 0
        lives >= loadout.lives * 0.9f -> 3
        lives >= loadout.lives * 0.5f -> 2
        else -> 1
    }

    private var nextId = 0
    private var queue = mutableListOf<Foe>()
    private var spawnTimer = 0f
    private var healedWave = 0
    private var winPaid = false

    fun waveQueue(w: Int): MutableList<Foe> {
        val list = mutableListOf<Foe>()
        repeat(5 + 2 * w) {
            val roll = random.nextFloat()
            list += when {
                w >= 6 && roll < 0.10f -> Foe.TROLL
                w >= 4 && roll < 0.20f -> Foe.SHAMAN
                w >= 3 && roll < 0.38f -> Foe.ORC
                w >= 2 && roll < 0.55f -> Foe.BAT
                w >= 2 && roll < 0.70f -> Foe.WOLF
                else -> Foe.GOBLIN
            }
        }
        if (w % 5 == 0 || w == stage.waves) list += Foe.BOSS
        return list
    }

    private fun hpScale(w: Int) = (1f + 0.10f * (w - 1)) * stage.difficulty

    /** Starts the next wave. Called early, it pays 2 gold for every second left. Returns the bonus. */
    fun callWave(): Int {
        if (!canCallWave) return 0
        val bonus = ((nextWaveIn ?: 0f) * EARLY_GOLD_PER_SECOND).toInt()
        gold += bonus
        wave++
        queue = waveQueue(wave)
        spawnTimer = 0f
        nextWaveIn = null
        waveBanner = 2f
        return bonus
    }

    private fun spawn(foe: Foe) {
        val hp = (foe.hp * hpScale(wave)).toInt()
        val speed = foe.speed * (1f + 0.02f * (wave - 1)) * (1f - loadout.slow)
        enemies += Enemy(nextId++, foe, hp, speed, (random.nextFloat() - 0.5f) * 0.3f)
    }

    /** Where an enemy is on the map. */
    fun position(e: Enemy): Pt = pointAt(e.dist)

    fun pointAt(d: Float): Pt {
        var left = d
        for ((a, b) in segments) {
            val len = a.dist(b)
            if (left <= len) {
                val t = if (len == 0f) 0f else left / len
                return Pt(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
            }
            left -= len
        }
        return path.last()
    }

    fun spotCenter(index: Int): Pt = stage.spots[index].let { (x, y) -> Pt(x + 0.5f, y + 0.5f) }

    fun build(spot: Int, kind: TowerKind): Boolean {
        if (over || spot !in stage.spots.indices || spot in towers || !loadout.canBuild(kind)) return false
        val cost = kind.costs[0]
        if (gold < cost) return false
        gold -= cost
        towers[spot] = Tower(spot, kind)
        return true
    }

    fun upgrade(spot: Int): Boolean {
        val t = towers[spot] ?: return false
        val cost = t.upgradeCost ?: return false
        if (over || gold < cost) return false
        gold -= cost
        t.level++
        t.spent += cost
        return true
    }

    fun sell(spot: Int): Boolean {
        if (over) return false
        val t = towers.remove(spot) ?: return false
        gold += t.sellValue
        return true
    }

    fun spellReady(spell: Spell) = !over && started && (cooldowns[spell] ?: 0f) <= 0f

    /** Drops a meteor shower on [at]; it lands half a second later. */
    fun castMeteor(at: Pt): Boolean {
        if (!spellReady(Spell.METEOR)) return false
        meteors += Meteor(at, 0.5f)
        cooldowns[Spell.METEOR] = Spell.METEOR.cooldown
        return true
    }

    /** Freezes every enemy on the map for a few seconds. */
    fun castFreeze(): Boolean {
        if (!spellReady(Spell.FREEZE)) return false
        freezeLeft = FREEZE_TIME
        cooldowns[Spell.FREEZE] = Spell.FREEZE.cooldown
        return true
    }

    /** Advances the battle by [dt] seconds. */
    fun step(dt: Float) {
        if (over) return
        waveBanner = max(0f, waveBanner - dt)
        gateFlash = max(0f, gateFlash - dt)
        freezeLeft = max(0f, freezeLeft - dt)
        shots.forEach { it.ttl -= dt }
        shots.removeAll { it.ttl <= 0f }
        blasts.forEach { it.ttl -= dt }
        blasts.removeAll { it.ttl <= 0f }
        if (started) cooldowns.keys.forEach { cooldowns[it] = max(0f, cooldowns.getValue(it) - dt) }

        // Spawning and the countdown to the next wave.
        if (queue.isNotEmpty()) {
            spawnTimer -= dt
            if (spawnTimer <= 0f) {
                val foe = queue.removeAt(0)
                spawn(foe)
                spawnTimer = when (foe) {
                    Foe.WOLF -> 0.6f
                    Foe.BOSS -> 1.5f
                    else -> 0.9f
                }
            }
        } else if (started && wave < stage.waves) {
            val left = (nextWaveIn ?: WAVE_GAP) - dt
            nextWaveIn = left
            if (left <= 0f) callWave()
        }
        // Temples heal the castle once a wave is beaten.
        if (started && queue.isEmpty() && enemies.isEmpty() && healedWave < wave) {
            healedWave = wave
            lives = min(loadout.lives, lives + loadout.regen)
            score += 25 * wave
        }

        moveEnemies(dt)
        if (lost) return

        meteors.forEach { it.delay -= dt }
        meteors.filter { it.delay <= 0f }.forEach { m ->
            blasts += Blast(m.at, METEOR_RADIUS, frost = false, ttl = 0.5f, total = 0.5f)
            enemies.filter { position(it).dist(m.at) <= METEOR_RADIUS }
                .forEach { damage(it, loadout.meteorDamage.toFloat(), magic = true) }
        }
        meteors.removeAll { it.delay <= 0f }

        towers.values.forEach { fire(it, dt) }
        if (won && !winPaid) {
            winPaid = true
            score += 10 * lives
        }
    }

    private fun moveEnemies(dt: Float) {
        val iterator = enemies.iterator()
        while (iterator.hasNext()) {
            val e = iterator.next()
            e.flash = max(0f, e.flash - dt)
            e.slowLeft = max(0f, e.slowLeft - dt)
            var speed = e.speed
            if (e.slowLeft > 0f) speed *= 1f - e.slowBy
            if (freezeLeft > 0f) speed *= 1f - FREEZE_SLOW
            e.dist += speed * dt
            if (e.foe.regen > 0f) e.hp = min(e.maxHp.toFloat(), e.hp + e.foe.regen * dt)
            if (e.dist >= pathLength) {
                lives = max(0, lives - e.foe.lives)
                gateFlash = 0.4f
                iterator.remove()
            }
        }
        enemies.filter { it.foe.heal > 0f }.forEach { healer ->
            val at = position(healer)
            enemies.filter { it !== healer && position(it).dist(at) <= 1.5f }
                .forEach { it.hp = min(it.maxHp.toFloat(), it.hp + healer.foe.heal * dt) }
        }
    }

    private fun fire(t: Tower, dt: Float) {
        t.reload = max(0f, t.reload - dt)
        if (t.reload > 0f) return
        val center = spotCenter(t.spot)
        // Like the classics: shoot the enemy furthest along the road, the one closest to the castle.
        val target = enemies
            .filter { (t.kind.hitsAir || !it.foe.flying) && position(it).dist(center) <= t.range }
            .maxByOrNull { it.dist } ?: return
        val level = t.level - 1
        t.reload = t.kind.cooldown[level]
        val at = position(target)
        t.aim = at
        val amount = t.kind.damage[level] * loadout.damageFactor(t.kind)
        shots += Shot(center, at, t.kind, SHOT_TIME, SHOT_TIME)
        if (t.kind.splash > 0f) {
            blasts += Blast(at, t.kind.splash, frost = t.kind == TowerKind.FROST, ttl = 0.3f, total = 0.3f)
            enemies.filter { (t.kind.hitsAir || !it.foe.flying) && position(it).dist(at) <= t.kind.splash }
                .forEach { hit(it, amount, t.kind, level) }
        } else {
            hit(target, amount, t.kind, level)
        }
    }

    private fun hit(e: Enemy, amount: Float, kind: TowerKind, level: Int) {
        val slow = kind.slow[level]
        if (slow > 0f) {
            e.slowBy = max(if (e.slowLeft > 0f) e.slowBy else 0f, slow)
            e.slowLeft = 2f
        }
        damage(e, amount, kind.magic)
    }

    private fun damage(e: Enemy, amount: Float, magic: Boolean) {
        if (e !in enemies) return
        e.hp -= if (magic) amount else max(1f, amount * (1f - e.foe.armor))
        e.flash = 0.1f
        if (e.hp <= 0f) {
            enemies.remove(e)
            kills++
            gold += e.foe.gold
            score += e.foe.gold
        }
    }
}
