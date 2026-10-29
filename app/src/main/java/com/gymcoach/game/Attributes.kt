package com.gymcoach.game

import kotlin.math.sqrt

/** The five attributes, each grown by a different kind of training. */
enum class Attribute(val short: String, val title: String, val source: String) {
    STR("STR", "Strength", "Strength sets"),
    VIT("VIT", "Vitality", "Cardio minutes"),
    AGI("AGI", "Agility", "Mobility holds"),
    WIS("WIS", "Discipline", "Plan workouts and contracts"),
    CON("CON", "Constitution", "Weeks on target and your best bonfire"),
}

data class Attributes(val values: Map<Attribute, Int>, val progress: Map<Attribute, Float>) {
    operator fun get(a: Attribute) = values.getValue(a)
}

/** Hero classes follow your training mix, so they change as your training does. */
enum class HeroClass(val title: String, val perk: String) {
    WARRIOR("Warrior", "Lives for heavy iron. Strength builds the legend."),
    RANGER("Ranger", "Goes the distance. Endurance is the weapon."),
    ROGUE("Rogue", "Moves like water. Mobility keeps the blade quick."),
    PALADIN("Paladin", "Balanced in all things. Strength, stamina and suppleness as one."),
    VANGUARD("Vanguard", "Warrior and Ranger: strong and tireless."),
    DUELIST("Duelist", "Warrior and Rogue: powerful and precise."),
    SCOUT("Scout", "Ranger and Rogue: fast and far-reaching."),
    RECRUIT("Recruit", "Every class starts here. Train to find your path."),
}

/** Hero tiers from the design: Recruit, Adventurer, Knight, Legend. */
enum class HeroTier(val title: String, val minLevel: Int) {
    RECRUIT("The Recruit", 1),
    ADVENTURER("The Adventurer", 6),
    KNIGHT("The Knight", 16),
    LEGEND("The Legend", 31),
    ;

    companion object {
        fun forLevel(level: Int) = entries.last { level >= it.minLevel }
    }
}

/**
 * Attributes come from recorded training only, never from guesses about your real strength.
 * Each one grows with the square root of its training, so early gains come fast and later ones
 * take steady work: value = 1 + floor(sqrt(points)).
 */
object AttributeCalculator {
    fun value(points: Int) = 1 + sqrt(points.coerceAtLeast(0).toDouble()).toInt()

    /** Share of the way to the next point. */
    fun progress(points: Int): Float {
        val v = value(points) - 1
        val from = v * v
        val to = (v + 1) * (v + 1)
        return (points - from).toFloat() / (to - from)
    }

    fun points(workouts: List<WorkoutRecord>, contracts: Int, keptWeeks: Int, bestBonfire: Int): Map<Attribute, Int> {
        val sets = workouts.flatMap { it.sets }
        return mapOf(
            Attribute.STR to sets.count { !it.cardio && !it.mobility },
            Attribute.VIT to sets.filter { it.cardio }.sumOf { it.reps } / 5,
            Attribute.AGI to sets.count { it.mobility } * 2,
            Attribute.WIS to workouts.count { it.inProgram } * 2 + contracts * 3,
            Attribute.CON to keptWeeks * 3 + bestBonfire,
        )
    }

    fun compute(workouts: List<WorkoutRecord>, contracts: Int, keptWeeks: Int, bestBonfire: Int): Attributes {
        val p = points(workouts, contracts, keptWeeks, bestBonfire)
        return Attributes(p.mapValues { value(it.value) }, p.mapValues { progress(it.value) })
    }

    /** Picks a class from the three training attributes. Two close ones make a hybrid; all three close make a Paladin. */
    fun heroClass(a: Attributes): HeroClass {
        val str = a[Attribute.STR]
        val vit = a[Attribute.VIT]
        val agi = a[Attribute.AGI]
        if (maxOf(str, vit, agi) < 4) return HeroClass.RECRUIT
        val sorted = listOf(Attribute.STR to str, Attribute.VIT to vit, Attribute.AGI to agi).sortedByDescending { it.second }
        val (top, second, third) = sorted
        fun close(x: Int, y: Int) = y >= x * 0.8
        if (close(top.second, third.second)) return HeroClass.PALADIN
        if (close(top.second, second.second)) {
            val pair = setOf(top.first, second.first)
            return when (pair) {
                setOf(Attribute.STR, Attribute.VIT) -> HeroClass.VANGUARD
                setOf(Attribute.STR, Attribute.AGI) -> HeroClass.DUELIST
                else -> HeroClass.SCOUT
            }
        }
        return when (top.first) {
            Attribute.STR -> HeroClass.WARRIOR
            Attribute.VIT -> HeroClass.RANGER
            else -> HeroClass.ROGUE
        }
    }
}

enum class Slot(val title: String) { WEAPON("Weapon"), ARMOUR("Armour"), HEAD("Head"), SHIELD("Shield"), CAPE("Cape") }

/** What gear unlocks look at. */
data class Progress(
    val hero: HeroStats,
    val attributes: Attributes,
    val bestBonfire: Int,
    val contracts: Int,
    val slain: Set<Boss>,
    val planWorkouts: Int,
)

/** Gear is earned through achievements, never bought. [rule] says how. */
enum class Gear(val slot: Slot, val title: String, val rule: String, val unlocked: (Progress) -> Boolean) {
    WOODEN_SWORD(Slot.WEAPON, "Wooden Sword", "Every hero starts with one", { true }),
    IRON_SWORD(Slot.WEAPON, "Iron Sword", "STR 8", { it.attributes[Attribute.STR] >= 8 }),
    KNIGHTS_BLADE(Slot.WEAPON, "Knight's Blade", "Reach level 16 (The Knight)", { it.hero.level >= 16 }),
    DRAGONFIRE_BLADE(Slot.WEAPON, "Dragonfire Blade", "Slay the Dragon of the Deep", { Boss.DEEP_DRAGON in it.slain }),

    TUNIC(Slot.ARMOUR, "Linen Tunic", "Every hero starts with one", { true }),
    LEATHER(Slot.ARMOUR, "Leather Jerkin", "Complete 10 workouts", { it.hero.workouts >= 10 }),
    RANGER_GARB(Slot.ARMOUR, "Ranger's Garb", "VIT 8", { it.attributes[Attribute.VIT] >= 8 }),
    ROGUE_LEATHERS(Slot.ARMOUR, "Shadow Leathers", "AGI 6", { it.attributes[Attribute.AGI] >= 6 }),
    CHAINMAIL(Slot.ARMOUR, "Chainmail", "STR 10 and CON 6", { it.attributes[Attribute.STR] >= 10 && it.attributes[Attribute.CON] >= 6 }),
    PLATE(Slot.ARMOUR, "Plate Armour", "Complete 30 plan workouts", { it.planWorkouts >= 30 }),
    GOLDEN_PLATE(Slot.ARMOUR, "Golden Plate", "Reach level 31 (The Legend)", { it.hero.level >= 31 }),

    BARE(Slot.HEAD, "Bare Head", "Your own hair", { true }),
    LEATHER_CAP(Slot.HEAD, "Leather Cap", "Complete 5 workouts", { it.hero.workouts >= 5 }),
    IRON_HELM(Slot.HEAD, "Iron Helm", "Complete 25 workouts", { it.hero.workouts >= 25 }),
    PLUMED_HELM(Slot.HEAD, "Plumed Helm", "Keep a 30-day bonfire", { it.bestBonfire >= 30 }),
    CROWN(Slot.HEAD, "Crown of the Realm", "Reach level 31 (The Legend)", { it.hero.level >= 31 }),

    NO_SHIELD(Slot.SHIELD, "No Shield", "Hands free", { true }),
    WOODEN_SHIELD(Slot.SHIELD, "Wooden Shield", "Fulfil a guild contract", { it.contracts >= 1 }),
    KITE_SHIELD(Slot.SHIELD, "Kite Shield", "Fulfil 5 guild contracts", { it.contracts >= 5 }),
    SERPENT_SHIELD(Slot.SHIELD, "Serpent Scale Shield", "Slay the Sea Serpent", { Boss.SEA_SERPENT in it.slain }),

    NO_CAPE(Slot.CAPE, "No Cape", "Travel light", { true }),
    BLUE_CAPE(Slot.CAPE, "Sea-Blue Cape", "Keep a 14-day bonfire", { it.bestBonfire >= 14 }),
    CRIMSON_CAPE(Slot.CAPE, "Crimson Cape", "Slay all six campaign bosses", { it.slain.size == Boss.entries.size }),
    ROYAL_CAPE(Slot.CAPE, "Royal Purple Cape", "Keep a 100-day bonfire", { it.bestBonfire >= 100 }),
    ;

    companion object {
        fun forSlot(slot: Slot) = entries.filter { it.slot == slot }

        /** What the hero wears: your pick if it is unlocked, otherwise the best unlocked piece. */
        fun equipped(progress: Progress, picks: Map<Slot, Gear>): Map<Slot, Gear> = Slot.entries.associateWith { slot ->
            picks[slot]?.takeIf { it.slot == slot && it.unlocked(progress) }
                ?: forSlot(slot).last { it.unlocked(progress) }
        }
    }
}
