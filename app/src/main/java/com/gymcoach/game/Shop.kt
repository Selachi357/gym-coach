package com.gymcoach.game

enum class ShopKind { PET, HAIR, CHARM }

/** Things gold can buy: looks and companions, never fitness progress. */
enum class ShopItem(val kind: ShopKind, val title: String, val gold: Int, val description: String) {
    WOLF(ShopKind.PET, "Wolf Pup", 300, "A loyal grey wolf that follows you everywhere."),
    RAVEN(ShopKind.PET, "Raven", 250, "Clever, black and always watching the horizon."),
    FOX(ShopKind.PET, "Fox", 250, "Quick, red and a little bit sly."),
    CAT(ShopKind.PET, "Ship's Cat", 200, "Every good ship has one. Catches rats, judges your form."),
    DRAGON(ShopKind.PET, "Dragon Hatchling", 1200, "Hatched from an egg found on Dragon Isle. Breathes tiny sparks."),
    HAIR_BLOND(ShopKind.HAIR, "Golden Hair", 100, "Sun-bleached from long watches on deck."),
    HAIR_RED(ShopKind.HAIR, "Red Hair", 100, "Fiery, like your bonfire."),
    HAIR_BLACK(ShopKind.HAIR, "Raven Hair", 100, "Dark as a night watch."),
    HAIR_WHITE(ShopKind.HAIR, "Silver Hair", 150, "The wise look of a veteran."),
    EMBER_CHARM(ShopKind.CHARM, "Ember Charm", 150, "Saves your bonfire once when a missed day would put it out."),
}

/** A real-life reward you set yourself, paid for with royal tokens. */
data class RealReward(val name: String, val tokens: Int, val claimedOn: String? = null)

/**
 * Royal tokens are rare: they come from bonfire milestones and every 5 fulfilled contracts.
 * They pay for real-life rewards you choose, like new gear or a treat in port.
 */
object Shop {
    fun tokensEarned(bonfire: Bonfire, contracts: Contracts) = bonfire.milestones.sumOf { it.tokens } + contracts.completed / 5

    fun tokensLeft(earned: Int, rewards: List<RealReward>) = earned - rewards.filter { it.claimedOn != null }.sumOf { it.tokens }

    fun addReward(state: CityState, name: String, tokens: Int): CityState {
        val clean = name.trim().replace('\n', ' ')
        if (clean.isEmpty() || tokens < 1) return state
        return state.copy(rewards = state.rewards + RealReward(clean, tokens))
    }

    /** Claims reward [index] on [date] if enough tokens are left. */
    fun claim(state: CityState, index: Int, tokensEarned: Int, date: String): CityState {
        val reward = state.rewards.getOrNull(index) ?: return state
        if (reward.claimedOn != null || tokensLeft(tokensEarned, state.rewards) < reward.tokens) return state
        return state.copy(rewards = state.rewards.mapIndexed { i, r -> if (i == index) r.copy(claimedOn = date) else r })
    }

    fun removeReward(state: CityState, index: Int): CityState {
        val reward = state.rewards.getOrNull(index) ?: return state
        // Claimed rewards stay as a record of what you earned.
        if (reward.claimedOn != null) return state
        return state.copy(rewards = state.rewards.filterIndexed { i, _ -> i != index })
    }

    val SUGGESTIONS = listOf(
        RealReward("A new resistance band", 2),
        RealReward("Favourite meal at the next port", 1),
        RealReward("New training shoes", 5),
        RealReward("A massage on leave", 3),
    )
}
