package com.gymcoach.ui.hero

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcoach.game.Boss
import com.gymcoach.game.City
import com.gymcoach.game.Res
import com.gymcoach.game.DefenderGame
import com.gymcoach.game.Enemy
import com.gymcoach.game.Foe
import com.gymcoach.game.Loadout
import com.gymcoach.ui.GymTopBar
import com.gymcoach.ui.LocalRepository
import com.gymcoach.ui.theme.RetroButton
import com.gymcoach.ui.theme.RetroOutlinedButton

/** Best wave and score, kept on the phone. */
object DefenderRecords {
    private const val PREFS = "defender"

    fun bestWave(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("best_wave", 0)
    fun bestScore(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("best_score", 0)

    /** Saves the run if it beats the record. Returns true for a new best score. */
    fun save(context: Context, wave: Int, score: Int): Boolean {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val newBest = score > p.getInt("best_score", 0)
        p.edit()
            .putInt("best_wave", maxOf(wave, p.getInt("best_wave", 0)))
            .putInt("best_score", maxOf(score, p.getInt("best_score", 0)))
            .apply()
        return newBest
    }
}

// Enemies, 8 × 8. k outline, a body, e eye, w teeth or wings, x weapon.
private val foeArt: Map<Foe, List<String>> = mapOf(
    Foe.GOBLIN to listOf(
        "..kkkk..",
        ".kaaaak.",
        "kaeaaeak",
        "kaaaaaak",
        ".kawwak.",
        "x.kaak..",
        "xkaaaak.",
        ".kk..kk.",
    ),
    Foe.BAT to listOf(
        "k......k",
        "kk.kk.kk",
        "kaakkaak",
        ".kaeeak.",
        "..kaak..",
        "...kk...",
        "........",
        "........",
    ),
    Foe.ORC to listOf(
        ".kkkkk.x",
        "kaaaaakx",
        "kaeaeakx",
        "kaaaaakx",
        "kawwwak.",
        "kaaaaaak",
        ".kaaaak.",
        ".kk..kk.",
    ),
    Foe.TROLL to listOf(
        ".kkkkkk.",
        "kaaaaaak",
        "kaeaaeak",
        "kaaaaaak",
        "kkawwakk",
        "kaaaaaak",
        ".ka..ak.",
        ".kk..kk.",
    ),
)

private val foeColors = mapOf(
    Foe.GOBLIN to Color(0xFF6BBF3A),
    Foe.BAT to Color(0xFF7A5AA8),
    Foe.ORC to Color(0xFF3F7A3A),
    Foe.TROLL to Color(0xFF7A8AA0),
)

private fun foePalette(foe: Foe, flash: Boolean): Map<Char, Color> = if (flash) {
    mapOf('k' to Color.White, 'a' to Color.White, 'e' to Color.White, 'w' to Color.White, 'x' to Color.White)
} else {
    mapOf('k' to Retro.night, 'a' to foeColors.getValue(foe), 'e' to Retro.ember, 'w' to Retro.parchment, 'x' to Color(0xFFC0C8D0))
}

private val archerArt = listOf(
    ".kk.",
    "kssk",
    "kttk",
    "kttk",
    ".kk.",
)

private val sky = Color(0xFF151530)
private val ground = Color(0xFF2D3B22)
private val path = Color(0xFF4A3B2A)
private val wallStone = Color(0xFF8C8FA3)

private fun laneY(lane: Int, h: Float) = h * (0.47f + 0.19f * lane)

/** Size of one enemy pixel and the sprite's width in pixels. */
private fun spriteOf(e: Enemy, boss: Boss, h: Float): Triple<List<String>, Int, Float> {
    val unit = h * 0.13f * e.foe.size
    return if (e.foe == Foe.BOSS) Triple(bossRows(boss), 16, unit / 16) else Triple(foeArt.getValue(e.foe), 8, unit / 8)
}

private fun enemyRect(e: Enemy, boss: Boss, w: Float, h: Float): Rect {
    val (_, cells, px) = spriteOf(e, boss, h)
    val side = cells * px
    val cx = e.x * w
    val cy = laneY(e.lane, h)
    return Rect(cx - side / 2, cy - side / 2, cx + side / 2, cy + side / 2)
}

@Composable
fun DefenderScreen(back: () -> Unit) {
    val repo = LocalRepository.current
    val context = LocalContext.current
    val game by repo.game.collectAsStateWithLifecycle(initialValue = null)
    val city by repo.city.collectAsStateWithLifecycle()
    val g = game
    Column(Modifier.fillMaxSize().background(Retro.deep)) {
        GymTopBar("Castle Defender", back = back)
        if (g == null) return@Column
        val defence = City.defence(city)
        val loadout = remember(g, city) { Loadout.from(g.hero, g.realm, defence, g.trainedToday) }
        // The boss waves bring this week's campaign boss, or the dragon once the campaign is over.
        val boss = g.realm.campaign.current?.boss ?: Boss.DEEP_DRAGON
        var run by remember { mutableIntStateOf(0) }
        val defender = remember(run) { DefenderGame(loadout) }
        var frame by remember { mutableIntStateOf(0) }
        var newBest by remember(run) { mutableStateOf(false) }
        var plunder by remember(run) { mutableStateOf<Res?>(null) }

        LaunchedEffect(defender) {
            var last = withFrameNanos { it }
            while (!defender.over) {
                val now = withFrameNanos { it }
                defender.step(((now - last) / 1_000_000_000f).coerceAtMost(0.05f))
                last = now
                frame++
            }
            if (plunder == null) {
                newBest = DefenderRecords.save(context, defender.wave, defender.score)
                val loot = City.plunder(defender.wave, defender.score, defence.plunderBonus)
                plunder = loot
                repo.addPlunder(loot)
            }
        }

        Box(Modifier.fillMaxSize()) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(defender) {
                        detectTapGestures { tap ->
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            // Fat-finger friendly: a little slack around every enemy, closest to the wall first.
                            val slack = 16.dp.toPx()
                            defender.enemies.sortedBy { it.x }
                                .firstOrNull { enemyRect(it, boss, w, h).inflate(slack).contains(tap) }
                                ?.let { defender.hit(it.id) }
                        }
                    },
            ) {
                frame // Redraw on every game tick.
                val w = size.width
                val h = size.height
                drawRect(sky, size = Size(w, h * 0.3f))
                drawRect(ground, Offset(0f, h * 0.3f), Size(w, h * 0.7f))
                for (lane in 0 until DefenderGame.LANES) {
                    drawRect(path, Offset(0f, laneY(lane, h) - h * 0.04f), Size(w, h * 0.08f))
                }
                // Stars.
                for (i in 0 until 14) {
                    drawRect(Retro.parchment, Offset((i * 97 % 100) / 100f * w, (i * 37 % 26) / 100f * h), Size(4f, 4f))
                }

                // The wall, with a battlement and stone pattern. It flashes red when hit.
                val wallW = DefenderGame.WALL_X * w
                val block = wallW / 6
                val wallColor = if (defender.wallFlash > 0f) Retro.hp else wallStone
                drawRect(wallColor, Offset(0f, h * 0.22f), Size(wallW, h * 0.78f))
                for (i in 0 until 3) drawRect(wallColor, Offset(i * 2 * block, h * 0.22f - block), Size(block, block))
                var row = 0
                var y = h * 0.22f + block
                while (y < h) {
                    var x = if (row % 2 == 0) 0f else block
                    while (x < wallW) {
                        drawRect(Retro.night, Offset(x, y), Size(block * 0.25f, block * 0.25f))
                        x += block * 2
                    }
                    y += block
                    row++
                }
                // Archers on the battlement.
                val archerPx = block * 0.6f
                for (i in 0 until defender.loadout.archers) {
                    drawPixels(
                        archerArt,
                        mapOf('k' to Retro.night, 's' to Color(0xFFF1C27D), 't' to Color(0xFF2E5A9C)),
                        Offset(wallW + 4f + i * archerPx * 5f, h * 0.22f - archerPx * 5f - 2f),
                        archerPx,
                    )
                }

                for (e in defender.enemies) {
                    val (rows, _, px) = spriteOf(e, boss, h)
                    val r = enemyRect(e, boss, w, h)
                    val palette = if (e.foe == Foe.BOSS) {
                        if (e.flash > 0f) bossPalette(boss).mapValues { Color.White } else bossPalette(boss)
                    } else {
                        foePalette(e.foe, e.flash > 0f)
                    }
                    drawPixels(rows, palette, r.topLeft, px)
                    if (e.hp < e.maxHp) {
                        val barH = 6f
                        drawRect(Retro.night, Offset(r.left, r.top - barH - 4f), Size(r.width, barH))
                        drawRect(Retro.hp, Offset(r.left, r.top - barH - 4f), Size(r.width * e.hp / e.maxHp, barH))
                    }
                }

                if (defender.lightningFlash > 0f) {
                    drawRect(Color.White.copy(alpha = 0.35f), size = size)
                    for (e in defender.enemies) {
                        val cx = e.x * w
                        val top = laneY(e.lane, h)
                        // A zig-zag bolt from the sky.
                        var y0 = 0f
                        var x0 = cx
                        var i = 0
                        while (y0 < top) {
                            val y1 = minOf(top, y0 + h * 0.08f)
                            val x1 = cx + if (i % 2 == 0) 14f else -14f
                            drawLine(Color(0xFFDDEEFF), Offset(x0, y0), Offset(x1, y1), strokeWidth = 6f)
                            x0 = x1
                            y0 = y1
                            i++
                        }
                    }
                }
                for (s in defender.shots) {
                    val from = Offset(wallW, h * 0.2f)
                    val to = Offset(s.toX * w, laneY(s.lane, h))
                    drawLine(if (s.fromArcher) Retro.parchment else Retro.gold, from, to, strokeWidth = 5f)
                }
            }

            // HUD.
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PixelText("WAVE ${defender.wave}", 10.sp, Retro.gold, Modifier.weight(1f))
                    PixelText("SCORE ${defender.score}", 10.sp)
                }
                frame // Recompose the HUD on every game tick.
                PixelBar(defender.wallHp.toFloat() / loadout.wallHp, Retro.hp, segments = 20, height = 12.dp)
                Row {
                    PixelText("WALL ${defender.wallHp}/${loadout.wallHp}", 7.sp, Retro.parchment, Modifier.weight(1f))
                    if (loadout.rage) PixelText("BATTLE RAGE ×2", 7.sp, Retro.ember)
                }
            }

            if (defender.waveBanner > 0f && !defender.over) {
                Box(Modifier.align(Alignment.Center)) {
                    PixelText(
                        if (defender.wave % 5 == 0) "WAVE ${defender.wave}: BOSS!" else "WAVE ${defender.wave}",
                        18.sp, Retro.gold,
                    )
                }
            }

            if (defender.over) {
                Box(Modifier.align(Alignment.Center).padding(24.dp)) {
                    RetroPanel {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            PixelText("THE WALL HAS FALLEN", 12.sp, Retro.hp)
                            Spacer(Modifier.size(12.dp))
                            PixelText("WAVE ${defender.wave} · ${defender.kills} KILLS", 9.sp)
                            Spacer(Modifier.size(6.dp))
                            PixelText("SCORE ${defender.score}", 11.sp, Retro.gold)
                            plunder?.let {
                                Spacer(Modifier.size(8.dp))
                                PixelText("PLUNDER FOR YOUR VILLAGE", 7.sp, Retro.dim)
                                Spacer(Modifier.size(4.dp))
                                ResLine(it, Retro.xp, prefix = "+")
                            }
                            if (newBest) {
                                Spacer(Modifier.size(6.dp))
                                PixelText("NEW BEST!", 9.sp, Retro.xp)
                            }
                            Spacer(Modifier.size(10.dp))
                            PixelText("Train to grow your village and hero: a stronger wall, more archers and harder arrows.", 7.sp, Retro.dim)
                            Spacer(Modifier.size(14.dp))
                            Row {
                                RetroButton(onClick = { run++ }) { Text("Again") }
                                Spacer(Modifier.width(12.dp))
                                RetroOutlinedButton(onClick = back) { Text("Back") }
                            }
                        }
                    }
                }
            }
        }
    }
}
