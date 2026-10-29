package com.gymcoach.ui.hero

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcoach.game.Boss
import com.gymcoach.game.City
import com.gymcoach.game.DefenderGame
import com.gymcoach.game.Foe
import com.gymcoach.game.Loadout
import com.gymcoach.game.Pt
import com.gymcoach.game.Res
import com.gymcoach.game.Spell
import com.gymcoach.game.Stage
import com.gymcoach.game.TowerKind
import com.gymcoach.ui.GymTopBar
import com.gymcoach.ui.LocalRepository
import com.gymcoach.ui.theme.RetroButton
import com.gymcoach.ui.theme.RetroOutlinedButton
import kotlin.math.roundToInt

/** Stars and best waves per stage, kept on the phone. */
object DefenderRecords {
    private const val PREFS = "defender"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun stars(context: Context, stage: Stage) = prefs(context).getInt("stars_${stage.name}", 0)

    fun bestWave(context: Context, stage: Stage) = prefs(context).getInt("wave_${stage.name}", 0)

    fun totalStars(context: Context) = Stage.entries.sumOf { stars(context, it) }

    /** A stage opens once the one before it is won. */
    fun unlocked(context: Context, stage: Stage) = stage.ordinal == 0 || stars(context, Stage.entries[stage.ordinal - 1]) > 0

    /** Saves the battle if it beats the record. Returns true for new stars. */
    fun save(context: Context, stage: Stage, wave: Int, stars: Int): Boolean {
        val p = prefs(context)
        val better = stars > p.getInt("stars_${stage.name}", 0)
        p.edit()
            .putInt("stars_${stage.name}", maxOf(stars, p.getInt("stars_${stage.name}", 0)))
            .putInt("wave_${stage.name}", maxOf(wave, p.getInt("wave_${stage.name}", 0)))
            .apply()
        return better
    }
}

@Composable
fun Stars(count: Int, size: androidx.compose.ui.unit.Dp = 16.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(3) { PixelSprite(starRows(), starPalette(it < count), size) }
    }
}

@Composable
fun DefenderScreen(back: () -> Unit) {
    val repo = LocalRepository.current
    val game by repo.game.collectAsStateWithLifecycle(initialValue = null)
    val city by repo.city.collectAsStateWithLifecycle()
    var stage by remember { mutableStateOf<Stage?>(null) }
    val g = game
    Column(Modifier.fillMaxSize().background(Retro.deep)) {
        GymTopBar(stage?.title ?: "Castle Defender", back = { if (stage != null) stage = null else back() })
        if (g == null) return@Column
        val defence = City.defence(city)
        val loadout = remember(g, city) { Loadout.from(g.hero, g.realm, defence, g.trainedToday) }
        // Boss waves bring this week's campaign boss, or the dragon once the campaign is over.
        val boss = g.realm.campaign.current?.boss ?: Boss.DEEP_DRAGON
        val s = stage
        if (s == null) {
            StageSelect(loadout) { stage = it }
        } else {
            Battle(s, loadout, boss, defence.plunderBonus, toStages = { stage = null })
        }
    }
}

@Composable
private fun StageSelect(loadout: Loadout, pick: (Stage) -> Unit) {
    val context = LocalContext.current
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PixelText(
                "Build towers by the road, upgrade them and stop every wave before it reaches your castle.",
                8.sp,
            )
        }
        items(Stage.entries.toList()) { stage ->
            val open = DefenderRecords.unlocked(context, stage)
            RetroPanel(Modifier.clickable(enabled = open) { pick(stage) }) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixelText(stage.title.uppercase(), 11.sp, if (open) Retro.gold else Retro.dim, Modifier.weight(1f))
                        Stars(DefenderRecords.stars(context, stage))
                    }
                    PixelText(if (open) stage.description else "Win ${Stage.entries[stage.ordinal - 1].title} to open.", 7.sp, if (open) Retro.parchment else Retro.dim)
                    PixelText("${stage.waves} WAVES · BEST WAVE ${DefenderRecords.bestWave(context, stage)}", 7.sp, Retro.dim)
                    if (open) RetroButton(onClick = { pick(stage) }, modifier = Modifier.fillMaxWidth()) { Text("Defend") }
                }
            }
        }
        item {
            RetroPanel {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PixelText("YOUR ARMY", 10.sp, Retro.gold)
                    ArmyLine("CASTLE LIVES", "${loadout.lives}")
                    ArmyLine("STARTING GOLD", "${loadout.startGold}")
                    ArmyLine("TOWER DAMAGE", "+${(loadout.damageBonus * 100).roundToInt()}%")
                    ArmyLine("ARCHER DAMAGE", "+${(loadout.archerBonus * 100).roundToInt()}%")
                    ArmyLine("LIVES BACK PER WAVE", "${loadout.regen}")
                    ArmyLine("ENEMY SLOW", "${(loadout.slow * 100).roundToInt()}%")
                    ArmyLine("MAGE TOWERS", if (loadout.mageUnlocked) "YES" else "LOCKED")
                    ArmyLine("METEOR DAMAGE", "${loadout.meteorDamage}")
                    ArmyLine("BATTLE RAGE", if (loadout.rage) "+25%, SPELLS READY" else "TRAIN TODAY")
                }
            }
        }
        item {
            PixelText(
                "Your training is your army. Walls and the town hall add lives, blacksmiths and slain bosses add damage, " +
                    "archer towers sharpen arrows, temples win lives back, stables slow enemies and a mage tower unlocks mage towers " +
                    "and stronger meteors. Hero levels add starting gold. Trained today? Battle rage: +25% damage and spells ready.",
                7.sp, Retro.dim,
            )
        }
    }
}

@Composable
private fun ArmyLine(label: String, value: String) {
    Row {
        PixelText(label, 7.sp, Retro.parchment, Modifier.weight(1f))
        PixelText(value, 7.sp, Retro.gold)
    }
}

private fun towerColor(kind: TowerKind?) = when (kind) {
    TowerKind.ARCHER -> Retro.parchment
    TowerKind.CANNON -> Retro.night
    TowerKind.MAGE -> Color(0xFFB07AF0)
    TowerKind.FROST -> Retro.sea
    null -> Retro.ember
}

@Composable
private fun Battle(stage: Stage, loadout: Loadout, boss: Boss, plunderBonus: Float, toStages: () -> Unit) {
    val repo = LocalRepository.current
    val context = LocalContext.current
    var run by remember { mutableIntStateOf(0) }
    val game = remember(run) { DefenderGame(stage, loadout) }
    val scenery = remember(stage) { scenery(stage) }
    var frame by remember { mutableIntStateOf(0) }
    var speed by remember { mutableIntStateOf(1) }
    var paused by remember(run) { mutableStateOf(false) }
    var selected by remember(run) { mutableStateOf<Int?>(null) }
    var aiming by remember(run) { mutableStateOf(false) }
    var newStars by remember(run) { mutableStateOf(false) }
    var plunder by remember(run) { mutableStateOf<Res?>(null) }
    var toast by remember(run) { mutableStateOf<Pair<String, Int>?>(null) }

    LaunchedEffect(game) {
        var last = withFrameNanos { it }
        while (!game.over) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
            last = now
            if (!paused) repeat(speed) { game.step(dt) }
            frame++
        }
        if (plunder == null) {
            newStars = DefenderRecords.save(context, stage, game.wave, game.stars)
            val loot = City.plunder(game.wave, game.score / 8, plunderBonus)
            plunder = loot
            repo.addPlunder(loot)
        }
    }

    Column(Modifier.fillMaxSize()) {
        frame // Recompose on every game tick.
        // HUD.
        Row(
            Modifier.fillMaxWidth().background(Retro.night).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PixelText("LIVES ${game.lives}", 9.sp, Retro.hp)
            Spacer(Modifier.width(14.dp))
            PixelText("GOLD ${game.gold}", 9.sp, Retro.gold)
            Spacer(Modifier.weight(1f))
            PixelText("WAVE ${game.wave}/${stage.waves}", 9.sp)
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val tileDp = minOf(maxWidth / Stage.COLS, maxHeight / Stage.ROWS)
            Box(Modifier.size(tileDp * Stage.COLS, tileDp * Stage.ROWS)) {
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(game) {
                            detectTapGestures { tap ->
                                val tile = size.width.toFloat() / Stage.COLS
                                val at = Pt(tap.x / tile, tap.y / tile)
                                if (aiming) {
                                    if (game.castMeteor(at)) aiming = false
                                    return@detectTapGestures
                                }
                                val spot = stage.spots.indexOf(at.x.toInt() to at.y.toInt())
                                selected = if (spot >= 0 && spot != selected) spot else null
                            }
                        },
                ) {
                    frame // Redraw on every game tick.
                    val tile = size.width / Stage.COLS
                    drawBattlefield(stage, scenery, tile, game.gateFlash > 0f)

                    stage.spots.forEachIndexed { i, (x, y) ->
                        val topLeft = Offset(x * tile, y * tile)
                        val t = game.towers[i]
                        if (t == null) drawPlot(topLeft, tile) else drawTower(t.kind, t.level, topLeft, tile)
                        if (selected == i) {
                            drawRect(Retro.gold, topLeft, Size(tile, tile), style = Stroke(3.dp.toPx()))
                            if (t != null) {
                                val c = Offset((x + 0.5f) * tile, (y + 0.5f) * tile)
                                drawCircle(Retro.gold.copy(alpha = 0.12f), t.range * tile, c)
                                drawCircle(Retro.gold.copy(alpha = 0.6f), t.range * tile, c, style = Stroke(2.dp.toPx()))
                            }
                        }
                    }

                    // Flyers above walkers, the boss on top.
                    for (e in game.enemies.sortedBy { (if (it.foe.flying) 1 else 0) + if (it.foe == Foe.BOSS) 2 else 0 }) {
                        val p = game.position(e)
                        val rows = foeRows(e.foe, boss)
                        val side = e.foe.size * tile
                        val px = side / rows.size
                        val lift = if (e.foe.flying) tile * 0.35f else 0f
                        val cx = (p.x + e.lane) * tile
                        val cy = (p.y + e.lane * 0.5f) * tile
                        if (e.foe.flying) drawOval(Retro.night.copy(alpha = 0.4f), Offset(cx - side * 0.3f, cy + side * 0.3f), Size(side * 0.6f, side * 0.2f))
                        val topLeft = Offset(cx - side / 2, cy - side / 2 - lift)
                        drawPixels(rows, foePalette(e.foe, boss, e.flash > 0f), topLeft, px)
                        if (e.slowLeft > 0f || game.freezeLeft > 0f) {
                            drawRect(Retro.sea.copy(alpha = 0.35f), topLeft, Size(side, side))
                        }
                        if (e.hp < e.maxHp) {
                            val barH = 3.dp.toPx()
                            drawRect(Retro.night, Offset(topLeft.x, topLeft.y - barH - 2f), Size(side, barH))
                            drawRect(Retro.hp, Offset(topLeft.x, topLeft.y - barH - 2f), Size(side * (e.hp / e.maxHp).coerceIn(0f, 1f), barH))
                        }
                    }

                    // Arrows, cannonballs and bolts fly from the tower to the target.
                    for (s in game.shots) {
                        val t = 1f - s.ttl / s.total
                        val from = Offset(s.from.x * tile, (s.from.y - 0.35f) * tile)
                        val to = Offset(s.to.x * tile, s.to.y * tile)
                        val arc = if (s.kind == TowerKind.CANNON) -tile * 0.8f * 4 * t * (1 - t) else 0f
                        val at = Offset(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t + arc)
                        val color = towerColor(s.kind)
                        when (s.kind) {
                            TowerKind.ARCHER -> {
                                val back = Offset(at.x - (to.x - from.x) * 0.12f, at.y - (to.y - from.y) * 0.12f)
                                drawLine(color, back, at, strokeWidth = 2.dp.toPx())
                            }
                            TowerKind.MAGE -> drawLine(color, from, at, strokeWidth = 3.dp.toPx())
                            else -> drawRect(color, Offset(at.x - 3.dp.toPx(), at.y - 3.dp.toPx()), Size(6.dp.toPx(), 6.dp.toPx()))
                        }
                    }
                    for (b in game.blasts) {
                        val t = 1f - b.ttl / b.total
                        val c = Offset(b.at.x * tile, b.at.y * tile)
                        val color = if (b.frost) Retro.sea else Retro.ember
                        drawCircle(color.copy(alpha = 0.5f * (1 - t)), b.radius * tile * (0.4f + 0.6f * t), c)
                        drawCircle(Color(0xFFFFE08A).copy(alpha = 0.7f * (1 - t)), b.radius * tile * 0.4f * (1 - t), c)
                    }
                    if (game.freezeLeft > 0f) drawRect(Retro.sea.copy(alpha = 0.12f), size = size)
                    if (aiming) drawRect(Retro.ember, Offset.Zero, size, style = Stroke(4.dp.toPx()))
                }

                if (game.waveBanner > 0f && !game.over) {
                    Box(Modifier.align(Alignment.Center)) {
                        PixelText(
                            if (game.wave % 5 == 0 || game.wave == stage.waves) "WAVE ${game.wave}: BOSS!" else "WAVE ${game.wave}",
                            16.sp, Retro.gold,
                        )
                    }
                }
                toast?.let { (text, at) ->
                    if (frame - at < 90) {
                        Box(Modifier.align(Alignment.TopCenter).padding(top = 8.dp)) { PixelText(text, 9.sp, Retro.xp) }
                    }
                }
                if (aiming) {
                    Box(Modifier.align(Alignment.TopCenter).padding(top = 8.dp).background(Retro.night).padding(6.dp)) {
                        PixelText("TAP WHERE THE METEOR FALLS", 8.sp, Retro.ember)
                    }
                }
                if (game.over) {
                    EndPanel(game, newStars, plunder, again = { run++ }, toStages = toStages)
                }
            }
        }
        // Controls.
        Box(
            Modifier.fillMaxWidth().height(132.dp).background(Retro.night).border(2.dp, Retro.gold).padding(8.dp),
        ) {
            val spot = selected
            when {
                spot != null && game.towers[spot] == null -> BuildMenu(game, onBuild = { kind ->
                    if (game.build(spot, kind)) selected = null
                }, onClose = { selected = null })
                spot != null -> TowerMenu(game, spot, onClose = { selected = null })
                else -> Row(
                    Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SpellButton(game, Spell.METEOR, active = aiming) { aiming = !aiming }
                        SpellButton(game, Spell.FREEZE, active = false) { game.castFreeze() }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val waveLabel = when {
                            !game.started -> "START"
                            game.canCallWave -> "NEXT WAVE +${((game.nextWaveIn ?: 0f) * DefenderGame.EARLY_GOLD_PER_SECOND).toInt()}$"
                            else -> "WAVE ${game.wave}"
                        }
                        ControlButton(waveLabel, enabled = game.canCallWave, color = Retro.xp) {
                            val bonus = game.callWave()
                            if (bonus > 0) toast = "+$bonus GOLD FOR CALLING EARLY" to frame
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ControlButton("x$speed", modifier = Modifier.weight(1f)) { speed = if (speed == 1) 2 else 1 }
                            ControlButton(if (paused) "PLAY" else "PAUSE", modifier = Modifier.weight(1f)) { paused = !paused }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Retro.gold,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .border(2.dp, if (enabled) color else Retro.dim)
            .background(Retro.panel)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) { PixelText(label, 8.sp, if (enabled) color else Retro.dim) }
}

@Composable
private fun SpellButton(game: DefenderGame, spell: Spell, active: Boolean, onClick: () -> Unit) {
    val left = game.cooldowns[spell] ?: 0f
    val ready = game.spellReady(spell)
    val label = when {
        active -> "${spell.title.uppercase()}: AIM"
        ready -> spell.title.uppercase()
        !game.started -> "${spell.title.uppercase()} ${left.toInt()}S"
        else -> "${spell.title.uppercase()} ${left.toInt() + 1}S"
    }
    ControlButton(label, enabled = ready, color = if (spell == Spell.METEOR) Retro.ember else Retro.sea, onClick = onClick)
}

@Composable
private fun BuildMenu(game: DefenderGame, onBuild: (TowerKind) -> Unit, onClose: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelText("BUILD A TOWER", 8.sp, Retro.gold, Modifier.weight(1f))
            PixelText("CLOSE", 8.sp, Retro.dim, Modifier.clickable(onClick = onClose).padding(4.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TowerKind.entries.forEach { kind ->
                val cost = kind.costs[0]
                val allowed = game.loadout.canBuild(kind)
                val affordable = game.gold >= cost
                Column(
                    Modifier
                        .weight(1f)
                        .border(2.dp, if (allowed && affordable) Retro.gold else Retro.dim)
                        .background(Retro.panel)
                        .clickable(enabled = allowed && affordable) { onBuild(kind) }
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PixelSprite(towerSprite(kind), cityPalette, 36.dp)
                    PixelText(kind.title.uppercase(), 6.sp, if (allowed) Retro.parchment else Retro.dim)
                    PixelText(if (allowed) "$cost$" else "LOCKED", 7.sp, if (allowed && affordable) Retro.gold else Retro.dim)
                }
            }
        }
    }
}

@Composable
private fun TowerMenu(game: DefenderGame, spot: Int, onClose: () -> Unit) {
    val t = game.towers[spot] ?: return
    val level = t.level - 1
    val damage = (t.kind.damage[level] * game.loadout.damageFactor(t.kind)).roundToInt()
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        PixelSprite(towerSprite(t.kind), cityPalette, 56.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PixelText("${t.kind.title.uppercase()} LV ${t.level}", 9.sp, Retro.gold)
            PixelText("DMG $damage · RANGE ${"%.1f".format(t.range)}", 7.sp)
            PixelText(t.kind.description, 6.sp, Retro.dim)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val cost = t.upgradeCost
                ControlButton(
                    if (cost == null) "MAX" else "UP $cost$",
                    modifier = Modifier.weight(1f),
                    enabled = cost != null && game.gold >= cost,
                ) { game.upgrade(spot) }
                ControlButton("SELL ${t.sellValue}$", modifier = Modifier.weight(1f), color = Retro.hp) {
                    if (game.sell(spot)) onClose()
                }
                ControlButton("X", modifier = Modifier.width(40.dp), color = Retro.dim, onClick = onClose)
            }
        }
    }
}

@Composable
private fun EndPanel(game: DefenderGame, newStars: Boolean, plunder: Res?, again: () -> Unit, toStages: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Retro.deep.copy(alpha = 0.6f)).padding(20.dp), contentAlignment = Alignment.Center) {
        RetroPanel {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                PixelText(if (game.won) "VICTORY!" else "THE CASTLE HAS FALLEN", 13.sp, if (game.won) Retro.xp else Retro.hp)
                Spacer(Modifier.size(10.dp))
                if (game.won) Stars(game.stars, 24.dp)
                Spacer(Modifier.size(8.dp))
                PixelText("WAVE ${game.wave} · ${game.kills} KILLS · ${game.lives} LIVES", 8.sp)
                Spacer(Modifier.size(4.dp))
                PixelText("SCORE ${game.score}", 10.sp, Retro.gold)
                plunder?.let {
                    Spacer(Modifier.size(8.dp))
                    PixelText("PLUNDER FOR YOUR VILLAGE", 7.sp, Retro.dim)
                    Spacer(Modifier.size(4.dp))
                    ResLine(it, Retro.xp, prefix = "+")
                }
                if (newStars) {
                    Spacer(Modifier.size(6.dp))
                    PixelText("NEW RECORD!", 9.sp, Retro.xp)
                }
                Spacer(Modifier.size(8.dp))
                PixelText(
                    if (game.won) "Three stars means almost nothing got through." else "Train to grow your village: more lives, gold and damage.",
                    7.sp, Retro.dim,
                )
                Spacer(Modifier.size(12.dp))
                Row {
                    RetroButton(onClick = again) { Text("Again") }
                    Spacer(Modifier.width(12.dp))
                    RetroOutlinedButton(onClick = toStages) { Text("Stages") }
                }
            }
        }
    }
}
