package com.gymcoach.ui.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcoach.game.Attribute
import com.gymcoach.game.Badge
import com.gymcoach.game.Bonfire
import com.gymcoach.game.BonfireMilestone
import com.gymcoach.game.Contract
import com.gymcoach.game.Contracts
import com.gymcoach.game.Gear
import com.gymcoach.game.Slot
import com.gymcoach.game.Res
import com.gymcoach.game.Shop
import com.gymcoach.game.ShopItem
import com.gymcoach.game.ShopKind
import java.time.LocalDate
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.KeyboardType
import com.gymcoach.game.Boss
import com.gymcoach.game.BossFight
import com.gymcoach.game.BossStatus
import com.gymcoach.game.Campaign
import com.gymcoach.game.City
import com.gymcoach.game.CityState
import com.gymcoach.game.TrainingTotals
import com.gymcoach.game.Island
import com.gymcoach.game.Loadout
import com.gymcoach.game.Stage
import com.gymcoach.game.TowerKind
import com.gymcoach.game.Voyage
import com.gymcoach.game.HeroStats
import com.gymcoach.game.QuestState
import com.gymcoach.game.Rank
import com.gymcoach.ui.GymTopBar
import com.gymcoach.ui.LocalRepository
import com.gymcoach.data.GameState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.gymcoach.ui.theme.RetroButton
import com.gymcoach.ui.theme.RetroOutlinedButton
import androidx.compose.ui.platform.LocalContext
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PixelText(text: String, size: TextUnit = 10.sp, color: Color = Retro.parchment, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = TextStyle(fontFamily = PixelFont, fontSize = size, color = color, lineHeight = size * 1.6f))
}

/** A dark panel with a gold pixel border, like an old RPG menu. */
@Composable
fun RetroPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .background(Retro.night)
            .border(3.dp, Retro.gold)
            .padding(3.dp)
            .border(2.dp, Retro.panel)
            .padding(12.dp),
    ) { content() }
}

/** Segmented XP bar. */
@Composable
fun PixelBar(progress: Float, color: Color, modifier: Modifier = Modifier, segments: Int = 20, height: Dp = 12.dp) {
    val filled = (progress.coerceIn(0f, 1f) * segments).toInt()
    Row(
        modifier.fillMaxWidth().height(height).border(2.dp, Retro.parchment).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(segments) { i ->
            Box(Modifier.weight(1f).fillMaxHeight().background(if (i < filled) color else Retro.panel))
        }
    }
}

/** Compact hero card for the home screen. */
@Composable
fun HeroBanner(onClick: () -> Unit) {
    val repo = LocalRepository.current
    val game by repo.game.collectAsStateWithLifecycle(initialValue = null)
    val city by repo.city.collectAsStateWithLifecycle()
    val h = game?.hero ?: return
    val fight = game?.realm?.campaign?.current
    RetroPanel(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.BottomEnd) {
                HeroSprite(game?.gear.orEmpty(), 72.dp, hair = city.hair)
                city.pet?.let { PixelSprite(petSprite(it), cityPalette, 28.dp) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PixelText(h.rank.title.uppercase(), 11.sp, Retro.gold)
                PixelText("LV ${h.level}  ${h.xpIntoLevel}/${h.xpForNextLevel} XP", 8.sp)
                PixelBar(h.xpIntoLevel.toFloat() / h.xpForNextLevel, Retro.xp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PixelBadge(Badge.FLAME, unlocked = h.weekStreak > 0, size = 16.dp)
                    Spacer(Modifier.width(6.dp))
                    PixelText("${h.weekStreak} WK STREAK · ${h.thisWeek}/${h.weeklyTarget}", 8.sp, Retro.ember)
                }
                game?.bonfire?.let { b ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BonfireSprite(b.tier, 16.dp)
                        Spacer(Modifier.width(6.dp))
                        PixelText(
                            if (b.embers) "EMBERS! TRAIN TODAY" else "${b.days} DAY BONFIRE",
                            8.sp, if (b.embers) Retro.hp else Retro.ember,
                        )
                    }
                }
                if (fight != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BossSprite(fight.boss, 16.dp)
                        Spacer(Modifier.width(6.dp))
                        PixelText("${fight.hpLeft} HP LEFT", 8.sp, Retro.hp)
                    }
                }
            }
        }
    }
}

private enum class RealmTab { HERO, BOSS, VOYAGE, VILLAGE, DEFEND, SHOP }

@Composable
fun HeroScreen(openDefender: () -> Unit, openCity: () -> Unit) {
    val repo = LocalRepository.current
    val game by repo.game.collectAsStateWithLifecycle(initialValue = null)
    val city by repo.city.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf(RealmTab.HERO) }
    Column(Modifier.fillMaxSize().background(Retro.deep)) {
        GymTopBar("Hero")
        PixelTabs(tab, onSelect = { tab = it })
        val g = game ?: return@Column
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (tab) {
                RealmTab.HERO -> heroItems(
                    g,
                    city,
                    onContract = { c -> scope.launch { repo.chooseContract(c) } },
                    onEquip = { gear -> scope.launch { repo.equip(gear) } },
                )
                RealmTab.BOSS -> campaignItems(g.realm.campaign)
                RealmTab.VOYAGE -> voyageItems(g.realm.voyage)
                RealmTab.VILLAGE -> villageItems(city, g.training, openCity, openDefender)
                RealmTab.SHOP -> shopItems(g, city) { action -> scope.launch { repo.updateCity(action) } }
                RealmTab.DEFEND -> defendItems(Loadout.from(g.hero, g.realm, City.defence(city), g.trainedToday), openDefender)
            }
        }
    }
}

@Composable
private fun PixelTabs(selected: RealmTab, onSelect: (RealmTab) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Retro.night).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        RealmTab.entries.forEach { t ->
            val on = t == selected
            Box(
                Modifier
                    .weight(1f)
                    .background(if (on) Retro.gold else Retro.panel)
                    .border(2.dp, if (on) Retro.parchment else Retro.dim)
                    .clickable { onSelect(t) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                PixelText(t.name, 6.sp, if (on) Retro.night else Retro.parchment)
            }
        }
    }
}

private fun LazyListScope.heroItems(g: GameState, city: CityState, onContract: (Contract) -> Unit, onEquip: (Gear) -> Unit) {
    val h = g.hero
    item { CharacterSheet(g, city) }
    item { BonfirePanel(g.bonfire) }
    item { ContractPanel(g.contracts, g.weeklyTarget, onContract) }
    item { AttributesPanel(g) }
    item { StreakPanel(h) }
    item { StatsPanel(h) }
    item { PixelText("GEAR", 12.sp, Retro.gold) }
    item { GearPanel(g, onEquip) }
    item { PixelText("QUESTS", 12.sp, Retro.gold) }
    items(h.quests, key = { it.quest.name }) { QuestRow(it) }
    item { PixelText("RANKS", 12.sp, Retro.gold) }
    item { RankLadder(h) }
    item {
        PixelText(
            "XP: 40 per workout, 4 per set, 1 per cardio minute, +20 plan session, +25 record, " +
                "+50 streak week, +100 quest.",
            7.sp, Retro.dim,
        )
    }
}

private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.UK)

private fun LazyListScope.campaignItems(c: Campaign) {
    item {
        RetroPanel {
            val fight = c.current
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                when {
                    c.daysUntilStart > 0 -> {
                        BossSprite(Boss.RAT_KING, 128.dp)
                        Spacer(Modifier.size(12.dp))
                        PixelText("THE CAMPAIGN BEGINS", 11.sp, Retro.gold)
                        Spacer(Modifier.size(6.dp))
                        PixelText(
                            "IN ${c.daysUntilStart} ${if (c.daysUntilStart == 1) "DAY" else "DAYS"} · ${c.start.format(dayMonth).uppercase()}",
                            8.sp,
                        )
                    }
                    fight != null -> {
                        PixelText("CAMPAIGN ${c.number} · WEEK ${fight.week} OF 6", 8.sp, Retro.dim)
                        Spacer(Modifier.size(10.dp))
                        BossSprite(fight.boss, 128.dp)
                        Spacer(Modifier.size(12.dp))
                        PixelText(fight.boss.title.uppercase(), 11.sp, Retro.gold)
                        Spacer(Modifier.size(6.dp))
                        PixelText(fight.boss.lair, 8.sp, Retro.dim)
                        Spacer(Modifier.size(12.dp))
                        PixelBar(fight.hpLeft.toFloat() / fight.hp, Retro.hp, height = 16.dp)
                        Spacer(Modifier.size(6.dp))
                        PixelText("${fight.hpLeft} / ${fight.hp} HP", 9.sp)
                        Spacer(Modifier.size(6.dp))
                        PixelText(
                            "DEFEAT IT BY SUNDAY ${fight.start.plusDays(6).format(dayMonth).uppercase()}",
                            7.sp, Retro.ember,
                        )
                    }
                    else -> {
                        PixelText("CAMPAIGN ${c.number}", 11.sp, Retro.gold)
                        Spacer(Modifier.size(6.dp))
                        PixelText("THE WEEK IS OVER. A NEW FOE COMES ON MONDAY.", 8.sp)
                    }
                }
            }
        }
    }
    item { PixelText("THE SIX FOES", 12.sp, Retro.gold) }
    items(c.fights, key = { it.boss.name }) { BossRow(it) }
    item { PixelText("LOOT", 12.sp, Retro.gold) }
    item {
        RetroPanel {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (c.loot.isEmpty()) PixelText("NO LOOT YET. BEAT A BOSS TO CLAIM ITS TREASURE.", 8.sp, Retro.dim)
                c.loot.groupingBy { it }.eachCount().forEach { (item, count) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixelBadge(Badge.CUP, unlocked = true, size = 16.dp)
                        Spacer(Modifier.width(8.dp))
                        PixelText(if (count > 1) "$item ×$count" else item, 8.sp)
                    }
                }
            }
        }
    }
    item {
        PixelText(
            "DAMAGE: 30 per workout, 5 per strength set, 1 per cardio minute (max 60). " +
                "Each week brings a new boss. Beat it before Sunday ends or it escapes. " +
                "After 6 weeks a new campaign starts with 10% tougher bosses.",
            7.sp, Retro.dim,
        )
    }
}

@Composable
private fun BossRow(f: BossFight) {
    RetroPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val hidden = f.status == BossStatus.LOCKED
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                if (hidden) PixelText("?", 18.sp, Retro.dim) else BossSprite(f.boss, 44.dp, defeated = f.status == BossStatus.ESCAPED)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                PixelText("WEEK ${f.week}", 7.sp, Retro.dim)
                PixelText(if (hidden) "???" else f.boss.title.uppercase(), 8.sp, if (f.status == BossStatus.DEFEATED) Retro.gold else Retro.parchment)
                if (f.status == BossStatus.ACTIVE) PixelBar(f.hpLeft.toFloat() / f.hp, Retro.hp, segments = 10, height = 10.dp)
            }
            Spacer(Modifier.width(8.dp))
            val (label, color) = when (f.status) {
                BossStatus.DEFEATED -> "SLAIN" to Retro.xp
                BossStatus.ESCAPED -> "FLED" to Retro.dim
                BossStatus.ACTIVE -> "${f.hpLeft} HP" to Retro.hp
                BossStatus.LOCKED -> "${f.hp} HP" to Retro.dim
            }
            PixelText(label, 8.sp, color)
        }
    }
}

private fun formatKmShort(km: Double) = "%.1f".format(Locale.UK, km)

private fun LazyListScope.voyageItems(v: Voyage) {
    item {
        RetroPanel {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VoyageMap(v, Modifier.border(2.dp, Retro.night))
                PixelText("ANCHORED NEAR ${v.at.title.uppercase()}", 9.sp, Retro.gold)
                PixelText("${formatKmShort(v.km)} KM SAILED · ${v.strengthSessions} STRENGTH WORKOUTS", 8.sp)
                val next = v.next
                if (next != null) {
                    val leg = (next.km - v.at.km).toFloat()
                    PixelBar(((v.km - v.at.km) / leg).toFloat(), Retro.sea, segments = 16)
                    PixelText("${formatKmShort(next.km - v.km)} KM TO ${next.title.uppercase()}", 8.sp, Retro.dim)
                } else {
                    PixelText("YOU SAILED OFF THE EDGE OF THE MAP!", 8.sp, Retro.xp)
                }
            }
        }
    }
    item { PixelText("SHIP'S LOG", 12.sp, Retro.gold) }
    items(v.reached.reversed(), key = { it.name }) { island ->
        RetroPanel {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PixelText("${island.title.uppercase()} · ${island.km} KM", 9.sp, Retro.gold)
                PixelText(island.story, 8.sp)
                if (island != Island.HOME_PORT) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val open = island in v.chestsOpened
                        PixelBadge(Badge.CHEST, unlocked = open, size = 16.dp)
                        Spacer(Modifier.width(8.dp))
                        PixelText(
                            if (open) "CHEST: ${island.treasure}" else
                                "CHEST LOCKED · ${island.strengthSessions - v.strengthSessions} MORE STRENGTH WORKOUTS",
                            7.sp, if (open) Retro.xp else Retro.dim,
                        )
                    }
                }
            }
        }
    }
    item {
        PixelText(
            "Cardio sails your ship: every km you log (or 1 km per 6 minutes when you log no distance). " +
                "Strength workouts open the treasure chests on the islands you reach.",
            7.sp, Retro.dim,
        )
    }
}

private fun LazyListScope.defendItems(l: Loadout, openDefender: () -> Unit) {
    item {
        RetroPanel {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TowerKind.entries.forEach { PixelSprite(towerSprite(it), cityPalette, 48.dp) }
                }
                Spacer(Modifier.size(12.dp))
                PixelText("CASTLE DEFENDER", 13.sp, Retro.gold)
                Spacer(Modifier.size(8.dp))
                PixelText(
                    "Goblins, wolves, bats, orcs, shamans and trolls march down the road. Build archers, cannons, " +
                        "mages and frost towers beside it, upgrade them, and call down meteors. Every fifth wave brings a boss.",
                    8.sp,
                )
                Spacer(Modifier.size(14.dp))
                RetroButton(onClick = openDefender, modifier = Modifier.fillMaxWidth()) { Text("Play") }
            }
        }
    }
    item {
        val context = LocalContext.current
        RetroPanel {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelText("YOUR ARMY", 10.sp, Retro.gold)
                StatLine("CASTLE LIVES", l.lives.toString())
                StatLine("STARTING GOLD", l.startGold.toString())
                StatLine("TOWER DAMAGE", "+${(l.damageBonus * 100).toInt()}%")
                StatLine("MAGE TOWERS", if (l.mageUnlocked) "YES" else "LOCKED")
                StatLine("BATTLE RAGE", if (l.rage) "ON +25%" else "OFF")
                Stage.entries.forEach { stage ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixelText(stage.title.uppercase(), 8.sp, Retro.parchment, Modifier.weight(1f))
                        Stars(DefenderRecords.stars(context, stage), 12.dp)
                    }
                }
            }
        }
    }
    item {
        PixelText(
            "Your training is your army. Walls and the town hall add castle lives, blacksmiths and slain bosses add damage, " +
                "temples win lives back, stables slow enemies, a mage tower unlocks mage towers and libraries add plunder. " +
                "Trained today? Battle rage gives +25% damage and spells ready from the start. Plunder goes to your village.",
            7.sp, Retro.dim,
        )
    }
}

private fun LazyListScope.shopItems(g: GameState, city: CityState, act: ((CityState, TrainingTotals) -> CityState) -> Unit) {
    val gold = City.treasury(city, g.training).gold
    val tokens = Shop.tokensLeft(g.tokensEarned, city.rewards)
    item {
        RetroPanel {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PixelText("THE ROYAL MARKET", 12.sp, Retro.gold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ResLine(Res(gold = gold), Retro.gold)
                    Spacer(Modifier.width(12.dp))
                    PixelBadge(Badge.CROWN, unlocked = true, size = 14.dp)
                    Spacer(Modifier.width(4.dp))
                    PixelText("$tokens ROYAL TOKENS", 8.sp, Retro.parchment)
                }
                PixelText(
                    "Gold is plentiful: it buys looks and companions, never progress. Royal tokens are rare: " +
                        "they come from bonfire milestones and every 5 contracts, and pay for real-life rewards you set yourself.",
                    7.sp, Retro.dim,
                )
            }
        }
    }
    item { PixelText("COMPANIONS", 12.sp, Retro.gold) }
    items(ShopItem.entries.filter { it.kind == ShopKind.PET }, key = { it.name }) { item ->
        ShopRow(item, city, g.training, act) { PixelSprite(petSprite(item), cityPalette, 40.dp) }
    }
    item { PixelText("BARBER", 12.sp, Retro.gold) }
    items(ShopItem.entries.filter { it.kind == ShopKind.HAIR }, key = { it.name }) { item ->
        ShopRow(item, city, g.training, act) { HeroSprite(g.gear, 40.dp, hair = item) }
    }
    item { PixelText("CHARMS", 12.sp, Retro.gold) }
    item {
        ShopRow(ShopItem.EMBER_CHARM, city, g.training, act) { BonfireSprite(1, 40.dp) }
    }
    item {
        PixelText("CHARMS LEFT: ${city.charms - g.bonfire.charmsUsed} · USED: ${g.bonfire.charmsUsed}", 7.sp, Retro.dim)
    }
    item { PixelText("REAL-LIFE REWARDS", 12.sp, Retro.gold) }
    item { RewardsPanel(city, g.tokensEarned, act) }
}

@Composable
private fun ShopRow(
    item: ShopItem,
    city: CityState,
    training: TrainingTotals,
    act: ((CityState, TrainingTotals) -> CityState) -> Unit,
    picture: @Composable () -> Unit,
) {
    val owned = item in city.owned
    val worn = city.pet == item || city.hair == item
    RetroPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { picture() }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PixelText(item.title.uppercase(), 8.sp, if (worn) Retro.gold else Retro.parchment)
                PixelText(item.description, 6.sp, Retro.dim)
                if (!owned || item.kind == ShopKind.CHARM) ResLine(Res(gold = item.gold), Retro.gold)
            }
            Spacer(Modifier.width(6.dp))
            when {
                worn -> RetroOutlinedButton(onClick = { act { s, _ -> City.wear(s, item.kind, null) } }) { Text("Off") }
                owned && item.kind != ShopKind.CHARM -> RetroButton(onClick = { act { s, _ -> City.wear(s, item.kind, item) } }) { Text("Use") }
                else -> RetroButton(
                    onClick = { act { s, t -> if (City.whyNotBuy(s, t, item) == null) City.buy(s, t, item) else s } },
                    enabled = City.whyNotBuy(city, training, item) == null,
                ) { Text("Buy") }
            }
        }
    }
}

@Composable
private fun RewardsPanel(city: CityState, tokensEarned: Int, act: ((CityState, TrainingTotals) -> CityState) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("1") }
    val left = Shop.tokensLeft(tokensEarned, city.rewards)
    RetroPanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (city.rewards.isEmpty()) PixelText("Set a reward worth working for. Claim it when you have the tokens.", 7.sp, Retro.dim)
            city.rewards.forEachIndexed { i, r ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        PixelText(r.name.uppercase(), 8.sp, if (r.claimedOn != null) Retro.xp else Retro.parchment)
                        PixelText(
                            if (r.claimedOn != null) "CLAIMED ${r.claimedOn}" else "${r.tokens} TOKEN${if (r.tokens > 1) "S" else ""}",
                            7.sp, Retro.dim,
                        )
                    }
                    if (r.claimedOn == null) {
                        TextButton(onClick = { act { s, _ -> Shop.removeReward(s, i) } }) { Text("×") }
                        RetroButton(
                            onClick = { act { s, _ -> Shop.claim(s, i, tokensEarned, LocalDate.now().toString()) } },
                            enabled = left >= r.tokens,
                        ) { Text("Claim") }
                    }
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                label = { Text("Your reward") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter(Char::isDigit).take(2) },
                    label = { Text("Tokens") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(110.dp),
                )
                Spacer(Modifier.width(10.dp))
                RetroButton(
                    onClick = {
                        val n = price.toIntOrNull() ?: 0
                        act { s, _ -> Shop.addReward(s, name, n) }
                        name = ""
                    },
                    enabled = name.isNotBlank() && (price.toIntOrNull() ?: 0) > 0,
                ) { Text("Add") }
            }
            PixelText("IDEAS", 7.sp, Retro.dim)
            Shop.SUGGESTIONS.filter { idea -> city.rewards.none { it.name == idea.name } }.forEach { idea ->
                PixelText(
                    "+ ${idea.name} (${idea.tokens})",
                    7.sp, Retro.parchment,
                    Modifier.clickable { act { s, _ -> Shop.addReward(s, idea.name, idea.tokens) } }.padding(vertical = 4.dp),
                )
            }
        }
    }
}

private fun LazyListScope.villageItems(city: CityState, training: TrainingTotals, openCity: () -> Unit, openDefender: () -> Unit) {
    item {
        RetroPanel {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CityMap(city, null, Modifier.fillMaxWidth().clickable(onClick = openCity))
                ResLine(City.treasury(city, training), Retro.gold, showZero = true)
                PixelText("HALL LV ${City.hallLevel(city)} · ${City.buildingCount(city)}/${City.maxBuildings(city)} BUILDINGS", 8.sp)
                RetroButton(onClick = openCity, modifier = Modifier.fillMaxWidth()) { Text("Build your village") }
                RetroOutlinedButton(onClick = openDefender, modifier = Modifier.fillMaxWidth()) { Text("Defend the castle") }
            }
        }
    }
    item {
        PixelText(
            "Every workout pays 15 gold and 5 food (+5 gold in your plan). Every strength set pays 2 stone, " +
                "every 3 cardio minutes 1 wood. Farms, mills, quarries, houses, markets and taverns make more for every " +
                "workout after they are built, so build early. Walls, towers, smiths, temples, stables, libraries and " +
                "mage towers power Castle Defender.",
            7.sp, Retro.dim,
        )
    }
}

@Composable
private fun BonfirePanel(b: Bonfire) {
    RetroPanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BonfireSprite(b.tier, 72.dp)
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PixelText("${b.days} DAY BONFIRE", 11.sp, Retro.ember)
                    PixelText("BEST ${b.best} DAYS", 8.sp, Retro.dim)
                    val status = when {
                        b.embers -> "EMBERS! TRAIN TODAY FOR A SECOND WIND."
                        b.restToday -> "REST DAY. RECOVERY IS PART OF THE QUEST."
                        b.litToday -> "TODAY IS LIT."
                        else -> "TRAIN TODAY TO FEED THE FIRE."
                    }
                    PixelText(status, 7.sp, if (b.embers) Retro.hp else Retro.parchment)
                }
            }
            b.next?.let { next ->
                PixelBar(b.days.toFloat() / next.days, Retro.ember, segments = 16)
                PixelText(
                    "NEXT: ${next.title.uppercase()} AT ${next.days} DAYS · +${next.xp} XP" +
                        if (next.tokens > 0) " · ${next.tokens} TOKEN${if (next.tokens > 1) "S" else ""}" else "",
                    7.sp, Retro.dim,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BonfireMilestone.entries.forEach { m ->
                    val reached = m in b.milestones
                    Box(
                        Modifier.weight(1f).background(if (reached) Retro.ember else Retro.panel).padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) { PixelText("${m.days}", 7.sp, if (reached) Retro.night else Retro.dim) }
                }
            }
            PixelText(
                "Workouts and planned rest days both feed the fire. Without a calendar plan, two rest days after a " +
                    "workout are fine. Miss a day and you get a Second Wind: train the next day and your streak survives " +
                    "(once a week). " + if (b.secondWindReady) "Second Wind is ready." else "Second Wind is recharging.",
                7.sp, Retro.dim,
            )
        }
    }
}

@Composable
private fun ContractPanel(c: Contracts, weeklyTarget: Int, onContract: (Contract) -> Unit) {
    val now = c.thisWeek
    RetroPanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelBadge(Badge.SCROLL, unlocked = now.done, size = 32.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PixelText("GUILD CONTRACT", 7.sp, Retro.dim)
                    PixelText(now.contract.title.uppercase(), 10.sp, Retro.gold)
                    PixelText(now.contract.description(weeklyTarget), 7.sp)
                }
                PixelText(if (now.done) "DONE" else "${now.current}/${now.goal}", 9.sp, if (now.done) Retro.xp else Retro.parchment)
            }
            PixelBar(now.current.toFloat() / now.goal, Retro.gold, segments = 16)
            if (!now.done) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Contract.entries.forEach { option ->
                        val on = option == now.contract
                        Box(
                            Modifier
                                .weight(1f)
                                .background(if (on) Retro.gold else Retro.panel)
                                .border(2.dp, if (on) Retro.parchment else Retro.dim)
                                .clickable { onContract(option) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) { PixelText(option.title.uppercase(), 6.sp, if (on) Retro.night else Retro.parchment) }
                    }
                }
            }
            PixelText(
                "One contract a week, Monday to Sunday: +250 XP and 50 gold for your village. ${c.completed} fulfilled so far.",
                7.sp, Retro.dim,
            )
        }
    }
}

@Composable
private fun CharacterSheet(g: GameState, city: CityState) {
    val h = g.hero
    RetroPanel {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            PixelText(g.tier.title.uppercase(), 8.sp, Retro.dim)
            Spacer(Modifier.size(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                HeroSprite(g.gear, 160.dp, hair = city.hair)
                city.pet?.let { PixelSprite(petSprite(it), cityPalette, 64.dp) }
            }
            Spacer(Modifier.size(12.dp))
            PixelText(h.rank.title.uppercase(), 14.sp, Retro.gold)
            Spacer(Modifier.size(6.dp))
            PixelText("LEVEL ${h.level} ${g.heroClass.title.uppercase()}", 11.sp)
            Spacer(Modifier.size(10.dp))
            PixelBar(h.xpIntoLevel.toFloat() / h.xpForNextLevel, Retro.xp, height = 16.dp)
            Spacer(Modifier.size(6.dp))
            PixelText("${h.xpIntoLevel} / ${h.xpForNextLevel} XP · ${h.xp} TOTAL", 8.sp)
            Spacer(Modifier.size(10.dp))
            PixelText(h.rank.flavor, 8.sp, Retro.dim)
        }
    }
}

private val attributeColors = mapOf(
    Attribute.STR to Color(0xFFE04040),
    Attribute.VIT to Retro.xp,
    Attribute.AGI to Color(0xFF3FC1E0),
    Attribute.WIS to Color(0xFF9A6AD0),
    Attribute.CON to Retro.gold,
)

@Composable
private fun AttributesPanel(g: GameState) {
    RetroPanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PixelText("CLASS: ${g.heroClass.title.uppercase()}", 10.sp, Retro.gold)
            PixelText(g.heroClass.perk, 7.sp, Retro.dim)
            Spacer(Modifier.size(2.dp))
            Attribute.entries.forEach { a ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PixelText(a.short, 9.sp, attributeColors.getValue(a), Modifier.width(40.dp))
                    PixelText(g.attributes[a].toString().padStart(2), 9.sp, Retro.parchment, Modifier.width(32.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        PixelBar(g.attributes.progress[a] ?: 0f, attributeColors.getValue(a), segments = 10, height = 10.dp)
                        PixelText(a.source, 6.sp, Retro.dim)
                    }
                }
            }
            PixelText(
                "Attributes only count what you log, and each point takes a little more than the last. " +
                    "Your class follows your training mix: train all three of STR, VIT and AGI evenly to become a Paladin.",
                7.sp, Retro.dim,
            )
        }
    }
}

@Composable
private fun GearPanel(g: GameState, onEquip: (Gear) -> Unit) {
    RetroPanel {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Slot.entries.forEach { slot ->
                PixelText(slot.title.uppercase(), 8.sp, Retro.dim)
                Gear.forSlot(slot).forEach { gear ->
                    val unlocked = gear in g.unlockedGear
                    val worn = g.gear[slot] == gear
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(if (worn) Retro.panel else Color.Transparent)
                            .then(if (unlocked && !worn) Modifier.clickable { onEquip(gear) } else Modifier)
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            PixelText(
                                if (unlocked) gear.title.uppercase() else "???",
                                8.sp, if (worn) Retro.gold else if (unlocked) Retro.parchment else Retro.dim,
                            )
                            PixelText(gear.rule, 6.sp, Retro.dim)
                        }
                        PixelText(
                            when {
                                worn -> "WORN"
                                unlocked -> "EQUIP"
                                else -> "LOCKED"
                            },
                            7.sp, if (worn) Retro.xp else if (unlocked) Retro.gold else Retro.dim,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakPanel(h: HeroStats) {
    RetroPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelBadge(Badge.FLAME, unlocked = h.weekStreak > 0, size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PixelText("${h.weekStreak}-WEEK STREAK", 11.sp, Retro.ember)
                PixelText("THIS WEEK ${h.thisWeek}/${h.weeklyTarget} WORKOUTS", 8.sp)
                PixelText("BEST ${h.bestWeekStreak} WEEKS", 8.sp, Retro.dim)
            }
        }
    }
}

@Composable
private fun StatsPanel(h: HeroStats) {
    RetroPanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatLine("WORKOUTS", h.workouts.toString())
            StatLine("SETS", h.totalSets.toString())
            StatLine("RECORDS", h.personalRecords.toString())
            StatLine("CARDIO", "${"%.1f".format(h.cardioKm)} KM")
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        PixelText(label, 9.sp, Retro.dim, Modifier.weight(1f))
        PixelText(value, 9.sp)
    }
}

@Composable
private fun QuestRow(q: QuestState) {
    RetroPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelBadge(q.quest.badge, unlocked = q.done, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                PixelText(q.quest.title.uppercase(), 9.sp, if (q.done) Retro.gold else Retro.parchment)
                PixelText(q.quest.description, 7.sp, Retro.dim)
                if (!q.done) PixelBar(q.current.toFloat() / q.goal, Retro.gold, segments = 10, height = 10.dp)
            }
            Spacer(Modifier.width(8.dp))
            PixelText(if (q.done) "DONE" else "${q.current}/${q.goal}", 8.sp, if (q.done) Retro.xp else Retro.parchment)
        }
    }
}

@Composable
private fun RankLadder(h: HeroStats) {
    RetroPanel {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Rank.entries.forEach { rank ->
                val reached = h.level >= rank.minLevel
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HeroSprite(rank, 32.dp, Modifier.background(if (rank == h.rank) Retro.panel else Color.Transparent))
                    Spacer(Modifier.width(10.dp))
                    PixelText(
                        "LV ${rank.minLevel}  ${if (reached) rank.title.uppercase() else "???"}",
                        8.sp,
                        when {
                            rank == h.rank -> Retro.gold
                            reached -> Retro.parchment
                            else -> Retro.dim
                        },
                    )
                }
            }
        }
    }
}
