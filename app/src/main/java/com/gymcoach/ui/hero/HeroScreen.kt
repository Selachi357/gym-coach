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
import com.gymcoach.game.Badge
import com.gymcoach.game.Boss
import com.gymcoach.game.BossFight
import com.gymcoach.game.BossStatus
import com.gymcoach.game.Building
import com.gymcoach.game.Campaign
import com.gymcoach.game.Castle
import com.gymcoach.game.Island
import com.gymcoach.game.Voyage
import com.gymcoach.game.HeroStats
import com.gymcoach.game.QuestState
import com.gymcoach.game.Rank
import com.gymcoach.ui.GymTopBar
import com.gymcoach.ui.LocalRepository
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
    val h = game?.hero ?: return
    val fight = game?.realm?.campaign?.current
    RetroPanel(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HeroSprite(h.rank, 72.dp)
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

private enum class RealmTab { HERO, BOSS, VOYAGE, CASTLE }

@Composable
fun HeroScreen() {
    val repo = LocalRepository.current
    val game by repo.game.collectAsStateWithLifecycle(initialValue = null)
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
                RealmTab.HERO -> heroItems(g.hero)
                RealmTab.BOSS -> campaignItems(g.realm.campaign)
                RealmTab.VOYAGE -> voyageItems(g.realm.voyage)
                RealmTab.CASTLE -> castleItems(g.realm.castle)
            }
        }
    }
}

@Composable
private fun PixelTabs(selected: RealmTab, onSelect: (RealmTab) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Retro.night).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                PixelText(t.name, 7.sp, if (on) Retro.night else Retro.parchment)
            }
        }
    }
}

private fun LazyListScope.heroItems(h: HeroStats) {
    item { CharacterSheet(h) }
    item { StreakPanel(h) }
    item { StatsPanel(h) }
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

private fun LazyListScope.castleItems(c: Castle) {
    item {
        RetroPanel {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CastleScene(c, Modifier.border(2.dp, Retro.night))
                Row {
                    PixelText("GOLD ${c.gold}", 9.sp, Retro.gold, Modifier.weight(1f))
                    PixelText("STONE ${c.stone}", 9.sp, Retro.stone, Modifier.weight(1f))
                    PixelText("BANNERS ${c.banners}", 9.sp, Retro.hp)
                }
            }
        }
    }
    item {
        RetroPanel {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val next = c.next
                if (next == null) {
                    PixelText("YOUR CASTLE IS COMPLETE!", 10.sp, Retro.xp)
                    PixelText("Keep training: banners keep rising for every week you hit your target.", 8.sp)
                } else {
                    PixelText("NEXT: ${next.title.uppercase()}", 10.sp, Retro.gold)
                    PixelText(next.description, 8.sp, Retro.dim)
                    PixelText("GOLD ${minOf(c.gold, next.gold)}/${next.gold}", 8.sp)
                    PixelBar(if (next.gold == 0) 1f else c.gold.toFloat() / next.gold, Retro.gold, segments = 16)
                    PixelText("STONE ${minOf(c.stone, next.stone)}/${next.stone}", 8.sp)
                    PixelBar(if (next.stone == 0) 1f else c.stone.toFloat() / next.stone, Retro.stone, segments = 16)
                }
            }
        }
    }
    item { PixelText("BUILDINGS", 12.sp, Retro.gold) }
    item {
        RetroPanel {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Building.entries.forEach { b ->
                    val built = b in c.built
                    Row {
                        Box(Modifier.padding(top = 1.dp).size(8.dp).background(if (built) Retro.xp else Retro.panel))
                        Spacer(Modifier.width(8.dp))
                        PixelText(
                            if (built || b == c.next) b.title.uppercase() else "???",
                            8.sp, if (built) Retro.parchment else Retro.dim, Modifier.weight(1f),
                        )
                        if (!built) PixelText("${b.gold}G ${b.stone}S", 7.sp, Retro.dim)
                    }
                }
            }
        }
    }
    item {
        PixelText(
            "Every workout earns 10 gold (+5 in your plan). Every strength set earns 2 stone, every 5 cardio minutes 1 stone. " +
                "Buildings go up by themselves once you can pay. A banner rises for every week you hit your weekly target.",
            7.sp, Retro.dim,
        )
    }
}

@Composable
private fun CharacterSheet(h: HeroStats) {
    RetroPanel {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            HeroSprite(h.rank, 160.dp)
            Spacer(Modifier.size(12.dp))
            PixelText(h.rank.title.uppercase(), 14.sp, Retro.gold)
            Spacer(Modifier.size(6.dp))
            PixelText("LEVEL ${h.level}", 11.sp)
            Spacer(Modifier.size(10.dp))
            PixelBar(h.xpIntoLevel.toFloat() / h.xpForNextLevel, Retro.xp, height = 16.dp)
            Spacer(Modifier.size(6.dp))
            PixelText("${h.xpIntoLevel} / ${h.xpForNextLevel} XP · ${h.xp} TOTAL", 8.sp)
            Spacer(Modifier.size(10.dp))
            PixelText(h.rank.flavor, 8.sp, Retro.dim)
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
