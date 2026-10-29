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
import androidx.compose.foundation.lazy.items
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
import com.gymcoach.game.HeroStats
import com.gymcoach.game.QuestState
import com.gymcoach.game.Rank
import com.gymcoach.ui.GymTopBar
import com.gymcoach.ui.LocalRepository

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
    val hero by repo.hero.collectAsStateWithLifecycle(initialValue = null)
    val h = hero ?: return
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
            }
        }
    }
}

@Composable
fun HeroScreen() {
    val repo = LocalRepository.current
    val hero by repo.hero.collectAsStateWithLifecycle(initialValue = null)
    Column(Modifier.fillMaxSize()) {
        GymTopBar("Hero")
        val h = hero ?: return@Column
        LazyColumn(
            Modifier.fillMaxSize().background(Color(0xFF12121F)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
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
