package com.gymcoach.game

/** Gold, wood, stone and food. */
data class Res(val gold: Int = 0, val wood: Int = 0, val stone: Int = 0, val food: Int = 0) {
    operator fun plus(o: Res) = Res(gold + o.gold, wood + o.wood, stone + o.stone, food + o.food)
    operator fun minus(o: Res) = Res(gold - o.gold, wood - o.wood, stone - o.stone, food - o.food)
    operator fun times(n: Int) = Res(gold * n, wood * n, stone * n, food * n)
    fun covers(cost: Res) = gold >= cost.gold && wood >= cost.wood && stone >= cost.stone && food >= cost.food
    val isZero: Boolean get() = this == Res()
}

/** Totals from the workout log that pay for the city. */
data class TrainingTotals(
    val workouts: Int,
    val planWorkouts: Int,
    val strengthSets: Int,
    val cardioMinutes: Int,
    /** Weekly contracts fulfilled, 50 gold each. */
    val contracts: Int = 0,
) {
    companion object {
        fun of(workouts: List<WorkoutRecord>, contracts: Int = 0) = TrainingTotals(
            contracts = contracts,
            workouts = workouts.size,
            planWorkouts = workouts.count { it.inProgram },
            strengthSets = workouts.sumOf { w -> w.sets.count { !it.cardio } },
            cardioMinutes = workouts.sumOf { w -> w.sets.filter { it.cardio }.sumOf { it.reps } },
        )
    }
}

enum class Terrain { GRASS, TREE, ROCK, WATER }

/**
 * City buildings. [cost] is for level 1; level n costs n times as much. [hall] is the town hall level
 * needed. [yield] is what one level makes per workout; adjacency bonuses come on top.
 */
enum class BuildingType(
    val title: String,
    val cost: Res,
    val maxLevel: Int,
    val hall: Int,
    val yield: Res,
    val description: String,
) {
    TOWN_HALL("Town Hall", Res(100, 60, 60, 20), 5, 1, Res(), "Heart of the city. Each level unlocks buildings, adds building plots and +5 wall HP."),
    HOUSE("House", Res(20, 15, 0, 0), 3, 1, Res(gold = 3), "Taxes: +3 gold per workout per level. +1 next to a well or market."),
    FARM("Farm", Res(15, 10, 0, 0), 3, 1, Res(food = 3), "+3 food per workout per level. +1 next to water."),
    LUMBER_MILL("Lumber Mill", Res(20, 0, 10, 0), 3, 1, Res(wood = 3), "+3 wood per workout per level. +1 for every tree next to it."),
    QUARRY("Quarry", Res(25, 15, 0, 0), 3, 1, Res(stone = 3), "+3 stone per workout per level. +1 for every rock next to it."),
    WELL("Well", Res(10, 0, 10, 0), 1, 1, Res(), "Fresh water. Houses and farms next to it make +1 more."),
    WALL("Wall", Res(5, 0, 10, 0), 3, 1, Res(), "Defender: +4 wall HP per level."),
    ARCHER_TOWER("Archer Tower", Res(30, 20, 20, 10), 3, 2, Res(), "Defender: +1 archer per level."),
    BLACKSMITH("Blacksmith", Res(40, 20, 30, 0), 3, 2, Res(), "Defender: +1 arrow damage per level."),
    MARKET("Market", Res(50, 30, 20, 20), 3, 2, Res(gold = 4), "+4 gold per workout per level. +1 for every house next to it."),
    TAVERN("Tavern", Res(35, 25, 10, 15), 3, 2, Res(gold = 3, food = 1), "+3 gold and +1 food per workout per level. Travellers bring news of new quests."),
    STABLE("Stable", Res(40, 35, 15, 25), 3, 2, Res(), "Defender: mounted patrols slow enemies by 6% per level."),
    LIBRARY("Library", Res(50, 40, 30, 0), 3, 3, Res(), "Scholars count the spoils: +15% plunder from Castle Defender per level."),
    TEMPLE("Temple", Res(60, 30, 50, 30), 3, 3, Res(), "Defender: repairs 2 wall HP per level after every wave."),
    MAGE_TOWER("Mage Tower", Res(80, 40, 60, 40), 3, 4, Res(), "Defender: lightning hits every enemy. Faster with each level."),
    ;

    fun costFor(level: Int) = cost * level
}

data class Placed(val type: BuildingType, val x: Int, val y: Int, val level: Int, val builtAt: Int)

/** What the city adds to Castle Defender. */
data class CityDefence(
    val wallHp: Int = 0,
    val archers: Int = 0,
    val damage: Int = 0,
    val repair: Int = 0,
    val mageLevel: Int = 0,
    /** Share of enemy speed taken off, 0 to 1. */
    val slow: Float = 0f,
    /** Extra share of plunder, 0.15 per library level. */
    val plunderBonus: Float = 0f,
)

/** Everything the player has done in the city. Resources themselves are derived, never stored. */
data class CityState(
    val buildings: List<Placed> = listOf(Placed(BuildingType.TOWN_HALL, City.HALL_X, City.HALL_Y, 1, 0)),
    val cleared: Set<Pair<Int, Int>> = emptySet(),
    val spent: Res = Res(),
    /** Plunder brought home from Castle Defender. */
    val loot: Res = Res(),
    /** Shop items bought (not counting charms). */
    val owned: Set<ShopItem> = emptySet(),
    /** Ember charms bought in total; the bonfire works out how many are used. */
    val charms: Int = 0,
    val pet: ShopItem? = null,
    val hair: ShopItem? = null,
    val rewards: List<RealReward> = emptyList(),
)

/**
 * A medieval city on a 9 × 12 map. Training earns the resources: every workout brings gold and food,
 * strength sets bring stone and cardio brings wood. Production buildings add more for every workout
 * after they are built, with bonuses for good placement. Defence buildings power Castle Defender.
 */
object City {
    const val WIDTH = 9
    const val HEIGHT = 12
    const val HALL_X = 4
    const val HALL_Y = 5

    val START = Res(60, 40, 30, 20)
    val CLEAR_TREE_COST = Res(gold = 5)
    val CLEAR_TREE_GAIN = Res(wood = 10)
    val CLEAR_ROCK_COST = Res(gold = 10)
    val CLEAR_ROCK_GAIN = Res(stone = 15)

    // T tree, R rock, W water, G grass.
    private val MAP = listOf(
        "TTTGGGRRT",
        "TTGGGGGRT",
        "TGGGGGGGW",
        "GGGGGGGWW",
        "GGGGGGGWG",
        "RGGGGGGWG",
        "RGGGGGGWG",
        "GGGGGGWWG",
        "GGGGGGWGG",
        "TGGGGGWGG",
        "TTGGGWWGT",
        "TTTGGWGTT",
    )

    fun baseTerrain(x: Int, y: Int): Terrain = when (MAP[y][x]) {
        'T' -> Terrain.TREE
        'R' -> Terrain.ROCK
        'W' -> Terrain.WATER
        else -> Terrain.GRASS
    }

    fun terrain(state: CityState, x: Int, y: Int): Terrain =
        if ((x to y) in state.cleared) Terrain.GRASS else baseTerrain(x, y)

    fun buildingAt(state: CityState, x: Int, y: Int): Placed? = state.buildings.firstOrNull { it.x == x && it.y == y }

    fun hallLevel(state: CityState) = state.buildings.firstOrNull { it.type == BuildingType.TOWN_HALL }?.level ?: 1

    /** Buildings allowed besides the town hall. */
    fun maxBuildings(state: CityState) = 3 + 3 * hallLevel(state)

    fun buildingCount(state: CityState) = state.buildings.count { it.type != BuildingType.TOWN_HALL }

    private fun neighbours(x: Int, y: Int) = listOf(x - 1 to y, x + 1 to y, x to y - 1, x to y + 1)
        .filter { (nx, ny) -> nx in 0 until WIDTH && ny in 0 until HEIGHT }

    /** What a building makes per workout, including its placement bonus. */
    fun production(state: CityState, b: Placed): Res {
        val near = neighbours(b.x, b.y)
        val nearTypes = near.mapNotNull { (nx, ny) -> buildingAt(state, nx, ny)?.type }
        val nearTerrain = near.map { (nx, ny) -> terrain(state, nx, ny) }
        val bonus = when (b.type) {
            BuildingType.HOUSE -> Res(gold = nearTypes.count { it == BuildingType.WELL || it == BuildingType.MARKET })
            BuildingType.FARM -> Res(food = (if (Terrain.WATER in nearTerrain) 1 else 0) + nearTypes.count { it == BuildingType.WELL })
            BuildingType.LUMBER_MILL -> Res(wood = nearTerrain.count { it == Terrain.TREE })
            BuildingType.QUARRY -> Res(stone = nearTerrain.count { it == Terrain.ROCK })
            BuildingType.MARKET -> Res(gold = nearTypes.count { it == BuildingType.HOUSE })
            else -> Res()
        }
        return if (b.type.yield.isZero) Res() else b.type.yield * b.level + bonus
    }

    fun trainingIncome(t: TrainingTotals) = Res(
        gold = 15 * t.workouts + 5 * t.planWorkouts + BonfireCalculator.CONTRACT_GOLD * t.contracts,
        wood = t.cardioMinutes / 3,
        stone = 2 * t.strengthSets,
        food = 5 * t.workouts,
    )

    /** Resources in the treasury right now. */
    fun treasury(state: CityState, t: TrainingTotals): Res {
        val produced = state.buildings.fold(Res()) { sum, b ->
            sum + production(state, b) * (t.workouts - b.builtAt).coerceAtLeast(0)
        }
        return START + trainingIncome(t) + produced + state.loot - state.spent
    }

    /** What every workout from now on brings in. */
    fun perWorkout(state: CityState) = state.buildings.fold(Res(gold = 15, food = 5)) { sum, b -> sum + production(state, b) }

    /** Null when the building can go here, otherwise the reason it can't. */
    fun whyNot(state: CityState, t: TrainingTotals, type: BuildingType, x: Int, y: Int): String? = when {
        type == BuildingType.TOWN_HALL -> "There is only one town hall"
        buildingAt(state, x, y) != null -> "This plot is taken"
        terrain(state, x, y) == Terrain.WATER -> "You can't build on water"
        terrain(state, x, y) != Terrain.GRASS -> "Clear this plot first"
        hallLevel(state) < type.hall -> "Needs town hall level ${type.hall}"
        buildingCount(state) >= maxBuildings(state) -> "No free plots: upgrade the town hall"
        !treasury(state, t).covers(type.costFor(1)) -> "Not enough resources"
        else -> null
    }

    fun build(state: CityState, t: TrainingTotals, type: BuildingType, x: Int, y: Int): CityState {
        require(whyNot(state, t, type, x, y) == null)
        return state.copy(buildings = state.buildings + Placed(type, x, y, 1, t.workouts), spent = state.spent + type.costFor(1))
    }

    fun whyNotUpgrade(state: CityState, t: TrainingTotals, b: Placed): String? = when {
        b.level >= b.type.maxLevel -> "Already at the highest level"
        b.type != BuildingType.TOWN_HALL && b.level >= hallLevel(state) -> "Upgrade the town hall first"
        !treasury(state, t).covers(b.type.costFor(b.level + 1)) -> "Not enough resources"
        else -> null
    }

    fun upgrade(state: CityState, t: TrainingTotals, b: Placed): CityState {
        require(whyNotUpgrade(state, t, b) == null)
        val up = b.copy(level = b.level + 1)
        return state.copy(buildings = state.buildings.map { if (it == b) up else it }, spent = state.spent + b.type.costFor(b.level + 1))
    }

    /** Tearing down gives back half of what the first level cost. */
    fun demolish(state: CityState, b: Placed): CityState {
        require(b.type != BuildingType.TOWN_HALL)
        val refund = Res(b.type.cost.gold / 2, b.type.cost.wood / 2, b.type.cost.stone / 2, b.type.cost.food / 2)
        return state.copy(buildings = state.buildings - b, spent = state.spent - refund)
    }

    fun clearCost(terrain: Terrain): Pair<Res, Res>? = when (terrain) {
        Terrain.TREE -> CLEAR_TREE_COST to CLEAR_TREE_GAIN
        Terrain.ROCK -> CLEAR_ROCK_COST to CLEAR_ROCK_GAIN
        else -> null
    }

    fun canClear(state: CityState, t: TrainingTotals, x: Int, y: Int): Boolean {
        val (cost, _) = clearCost(terrain(state, x, y)) ?: return false
        return treasury(state, t).covers(cost)
    }

    /** Chopping a tree or breaking a rock costs a little gold and gives wood or stone. */
    fun clear(state: CityState, t: TrainingTotals, x: Int, y: Int): CityState {
        require(canClear(state, t, x, y))
        val (cost, gain) = clearCost(terrain(state, x, y))!!
        return state.copy(cleared = state.cleared + (x to y), spent = state.spent + cost - gain)
    }

    fun defence(state: CityState): CityDefence {
        fun levels(type: BuildingType) = state.buildings.filter { it.type == type }.sumOf { it.level }
        return CityDefence(
            wallHp = 5 * hallLevel(state) + 4 * levels(BuildingType.WALL),
            archers = levels(BuildingType.ARCHER_TOWER),
            damage = levels(BuildingType.BLACKSMITH),
            repair = 2 * levels(BuildingType.TEMPLE),
            mageLevel = state.buildings.filter { it.type == BuildingType.MAGE_TOWER }.maxOfOrNull { it.level } ?: 0,
            slow = (0.06f * levels(BuildingType.STABLE)).coerceAtMost(0.5f),
            plunderBonus = 0.15f * levels(BuildingType.LIBRARY),
        )
    }

    /** Plunder from a Castle Defender run: gold from the score, a bit of everything for every wave. */
    fun plunder(wave: Int, score: Int, bonus: Float = 0f): Res {
        val base = Res(gold = score / 4, wood = 2 * wave, stone = 2 * wave, food = 2 * wave)
        fun more(n: Int) = (n * (1 + bonus)).toInt()
        return Res(more(base.gold), more(base.wood), more(base.stone), more(base.food))
    }

    /** Null when [item] can be bought, otherwise the reason it can't. */
    fun whyNotBuy(state: CityState, t: TrainingTotals, item: ShopItem): String? = when {
        item.kind != ShopKind.CHARM && item in state.owned -> "Already yours"
        treasury(state, t).gold < item.gold -> "Not enough gold"
        else -> null
    }

    /** Buys [item] with village gold. Pets and hair are put on straight away. */
    fun buy(state: CityState, t: TrainingTotals, item: ShopItem): CityState {
        require(whyNotBuy(state, t, item) == null)
        val paid = state.copy(spent = state.spent + Res(gold = item.gold))
        return when (item.kind) {
            ShopKind.CHARM -> paid.copy(charms = state.charms + 1)
            ShopKind.PET -> paid.copy(owned = state.owned + item, pet = item)
            ShopKind.HAIR -> paid.copy(owned = state.owned + item, hair = item)
        }
    }

    /** Switches pet or hair to an owned item, or back to none with [kind] and null. */
    fun wear(state: CityState, kind: ShopKind, item: ShopItem?): CityState {
        if (item != null && (item !in state.owned || item.kind != kind)) return state
        return when (kind) {
            ShopKind.PET -> state.copy(pet = item)
            ShopKind.HAIR -> state.copy(hair = item)
            ShopKind.CHARM -> state
        }
    }

    fun encode(state: CityState): String = buildString {
        state.buildings.forEach { appendLine("b ${it.type.name} ${it.x} ${it.y} ${it.level} ${it.builtAt}") }
        state.cleared.forEach { (x, y) -> appendLine("c $x $y") }
        appendLine("s ${state.spent.gold} ${state.spent.wood} ${state.spent.stone} ${state.spent.food}")
        appendLine("l ${state.loot.gold} ${state.loot.wood} ${state.loot.stone} ${state.loot.food}")
        state.owned.forEach { appendLine("o ${it.name}") }
        appendLine("m ${state.charms}")
        state.pet?.let { appendLine("p ${it.name}") }
        state.hair?.let { appendLine("h ${it.name}") }
        // Reward names can hold spaces, so they go last on the line.
        state.rewards.forEach { appendLine("r ${it.tokens} ${it.claimedOn ?: "-"} ${it.name}") }
    }

    /** Reads [encode]'s format. Unknown or broken lines are skipped so a bad save never crashes the app. */
    fun decode(text: String): CityState {
        val buildings = mutableListOf<Placed>()
        val cleared = mutableSetOf<Pair<Int, Int>>()
        var spent = Res()
        var loot = Res()
        val owned = mutableSetOf<ShopItem>()
        var charms = 0
        var pet: ShopItem? = null
        var hair: ShopItem? = null
        val rewards = mutableListOf<RealReward>()
        for (line in text.lines()) {
            val p = line.trim().split(' ')
            runCatching {
                when (p[0]) {
                    "b" -> buildings += Placed(BuildingType.valueOf(p[1]), p[2].toInt(), p[3].toInt(), p[4].toInt(), p[5].toInt())
                    "c" -> cleared += p[1].toInt() to p[2].toInt()
                    "s" -> spent = Res(p[1].toInt(), p[2].toInt(), p[3].toInt(), p[4].toInt())
                    "l" -> loot = Res(p[1].toInt(), p[2].toInt(), p[3].toInt(), p[4].toInt())
                    "o" -> owned += ShopItem.valueOf(p[1])
                    "m" -> charms = p[1].toInt()
                    "p" -> pet = ShopItem.valueOf(p[1])
                    "h" -> hair = ShopItem.valueOf(p[1])
                    "r" -> rewards += RealReward(p.drop(3).joinToString(" "), p[1].toInt(), p[2].takeIf { it != "-" })
                }
            }
        }
        if (buildings.none { it.type == BuildingType.TOWN_HALL }) return CityState()
        return CityState(buildings, cleared, spent, loot, owned, charms, pet, hair, rewards)
    }
}
