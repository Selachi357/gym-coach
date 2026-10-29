package com.gymcoach.ui.hero

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcoach.game.BuildingType
import com.gymcoach.game.City
import com.gymcoach.game.CityState
import com.gymcoach.game.Res
import com.gymcoach.game.Terrain
import com.gymcoach.game.TrainingTotals
import com.gymcoach.ui.GymTopBar
import com.gymcoach.ui.LocalRepository
import com.gymcoach.ui.theme.RetroButton
import com.gymcoach.ui.theme.RetroOutlinedButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Resource icons, 8 × 8.
private val coinIcon = listOf("..kkkk..", ".kyyyyk.", "kyxyyyyk", "kxyyyyyk", "kyyyyyyk", "kyyyyPyk", ".kyPPyk.", "..kkkk..")
private val woodIcon = listOf("........", ".kkkkkk.", "kbbbbbhk", "kbbbbhhk", "kbbbbhhk", "kbbbbbhk", ".kkkkkk.", "........")
private val stoneIcon = listOf("........", "...kkk..", "..krrrk.", ".krxrRRk", "krrrRRRk", "krrRRRRk", ".kkkkkk.", "........")
private val foodIcon = listOf("...k....", "..khk.k.", "..khkkhk", "...khkhk", "..kkhhk.", ".khhkk..", "..kkPk..", "...kPk..")

@Composable
private fun ResIcon(rows: List<String>, size: Dp = 16.dp) = PixelSprite(rows, cityPalette, size)

/** "12 [coin] 4 [wood] ..." for the non-zero parts of a cost or gain. */
@Composable
fun ResLine(res: Res, color: Color = Retro.parchment, prefix: String = "", showZero: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(res.gold to coinIcon, res.wood to woodIcon, res.stone to stoneIcon, res.food to foodIcon).forEach { (n, icon) ->
            if (n != 0 || showZero) {
                PixelText("$prefix$n", 8.sp, color)
                ResIcon(icon, 12.dp)
                Spacer(Modifier.width(4.dp))
            }
        }
    }
}

/** The village map. [onTap] gets the tile; null makes the map read-only. */
@Composable
fun CityMap(state: CityState, selected: Pair<Int, Int>?, modifier: Modifier = Modifier, onTap: ((Int, Int) -> Unit)? = null) {
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(350)
            tick++
        }
    }
    val tapModifier = if (onTap == null) Modifier else Modifier.pointerInput(onTap) {
        detectTapGestures { p ->
            val tile = size.width / City.WIDTH.toFloat()
            val x = (p.x / tile).toInt()
            val y = (p.y / tile).toInt()
            if (x in 0 until City.WIDTH && y in 0 until City.HEIGHT) onTap(x, y)
        }
    }
    Canvas(
        modifier
            .aspectRatio(City.WIDTH / City.HEIGHT.toFloat(), matchHeightConstraintsFirst = true)
            .border(3.dp, Retro.gold)
            .then(tapModifier),
    ) {
        val tile = size.width / City.WIDTH
        for (y in 0 until City.HEIGHT) for (x in 0 until City.WIDTH) {
            val b = City.buildingAt(state, x, y)
            drawCityTile(x, y, City.terrain(state, x, y), b?.type, b?.level ?: 0, Offset(x * tile, y * tile), tile, tick)
        }
        if (selected != null) {
            val (sx, sy) = selected
            val blink = if (tick % 2 == 0) Retro.gold else Retro.parchment
            drawRect(blink, Offset(sx * tile + 2f, sy * tile + 2f), Size(tile - 4f, tile - 4f), style = Stroke(width = 6f))
        }
    }
}

@Composable
fun CityScreen(back: () -> Unit) {
    val repo = LocalRepository.current
    val scope = rememberCoroutineScope()
    val game by repo.game.collectAsStateWithLifecycle(initialValue = null)
    val city by repo.city.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    Column(Modifier.fillMaxSize().background(Retro.deep)) {
        GymTopBar("Village", back = back)
        val training = game?.training ?: return@Column
        val treasury = City.treasury(city, training)

        Column(Modifier.fillMaxWidth().background(Retro.night).padding(horizontal = 12.dp, vertical = 8.dp)) {
            ResLine(treasury, Retro.gold, showZero = true)
            Spacer(Modifier.size(4.dp))
            Row {
                PixelText("PER WORKOUT ", 7.sp, Retro.dim)
                ResLine(City.perWorkout(city), Retro.xp, prefix = "+")
            }
            Spacer(Modifier.size(4.dp))
            PixelText(
                "HALL LV ${City.hallLevel(city)} · PLOTS ${City.buildingCount(city)}/${City.maxBuildings(city)}",
                7.sp, Retro.dim,
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
            CityMap(city, selected) { x, y -> selected = if (selected == (x to y)) null else x to y }
        }

        val sel = selected
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
                .background(Retro.night)
                .border(2.dp, Retro.gold)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
        ) {
            if (sel == null) {
                PixelText(
                    "Tap a plot to build, a building to upgrade it, or a tree or rock to clear it. " +
                        "Every workout fills your treasury.",
                    8.sp,
                )
            } else {
                TilePanel(city, training, treasury, sel.first, sel.second) { action ->
                    scope.launch { repo.updateCity(action) }
                }
            }
        }
    }
}

@Composable
private fun TilePanel(
    city: CityState,
    training: TrainingTotals,
    treasury: Res,
    x: Int,
    y: Int,
    act: ((CityState, TrainingTotals) -> CityState) -> Unit,
) {
    val building = City.buildingAt(city, x, y)
    val terrain = City.terrain(city, x, y)
    var confirmDemolish by remember(x, y) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            building != null -> {
                val type = building.type
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PixelSprite(buildingSprite(type), cityPalette, 40.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        PixelText(type.title.uppercase(), 10.sp, Retro.gold)
                        PixelText("LEVEL ${building.level}/${type.maxLevel}", 8.sp)
                    }
                }
                PixelText(type.description, 7.sp, Retro.dim)
                val makes = City.production(city, building)
                if (!makes.isZero) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixelText("MAKES ", 7.sp, Retro.dim)
                        ResLine(makes, Retro.xp, prefix = "+")
                        PixelText("/WORKOUT", 7.sp, Retro.dim)
                    }
                }
                if (building.level < type.maxLevel) {
                    val reason = City.whyNotUpgrade(city, training, building)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixelText("UPGRADE ", 7.sp, Retro.dim)
                        ResLine(type.costFor(building.level + 1), if (treasury.covers(type.costFor(building.level + 1))) Retro.parchment else Retro.hp)
                    }
                    RetroButton(
                        onClick = { act { s, t -> City.buildingAt(s, x, y)?.takeIf { City.whyNotUpgrade(s, t, it) == null }?.let { City.upgrade(s, t, it) } ?: s } },
                        enabled = reason == null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(reason ?: "Upgrade to level ${building.level + 1}") }
                }
                if (type != BuildingType.TOWN_HALL) {
                    RetroOutlinedButton(onClick = { confirmDemolish = true }, modifier = Modifier.fillMaxWidth()) { Text("Demolish") }
                }
            }
            terrain == Terrain.WATER -> {
                PixelText("RIVER", 10.sp, Retro.gold)
                PixelText("You can't build on water, but farms next to the river grow more food.", 8.sp)
            }
            terrain == Terrain.TREE || terrain == Terrain.ROCK -> {
                val (cost, gain) = City.clearCost(terrain)!!
                PixelText(if (terrain == Terrain.TREE) "FOREST" else "BOULDERS", 10.sp, Retro.gold)
                PixelText(
                    if (terrain == Terrain.TREE) "Lumber mills next to trees make more wood. Or chop it down for a building plot."
                    else "Quarries next to rocks make more stone. Or break it up for a building plot.",
                    8.sp,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PixelText("COSTS ", 7.sp, Retro.dim); ResLine(cost)
                    PixelText("GIVES ", 7.sp, Retro.dim); ResLine(gain, Retro.xp, prefix = "+")
                }
                RetroButton(
                    onClick = { act { s, t -> if (City.canClear(s, t, x, y)) City.clear(s, t, x, y) else s } },
                    enabled = City.canClear(city, training, x, y),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (terrain == Terrain.TREE) "Chop down" else "Break up") }
            }
            else -> {
                PixelText("BUILD HERE", 10.sp, Retro.gold)
                BuildingType.entries.filter { it != BuildingType.TOWN_HALL }.forEach { type ->
                    val reason = City.whyNot(city, training, type, x, y)
                    val locked = City.hallLevel(city) < type.hall
                    Row(
                        Modifier.fillMaxWidth().background(Retro.panel).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PixelSprite(buildingSprite(type), if (locked) cityPalette.mapValues { Retro.dim } else cityPalette, 36.dp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            PixelText(type.title.uppercase(), 8.sp, if (locked) Retro.dim else Retro.parchment)
                            if (locked) {
                                PixelText("NEEDS HALL LV ${type.hall}", 7.sp, Retro.dim)
                            } else {
                                ResLine(type.cost, if (treasury.covers(type.cost)) Retro.parchment else Retro.hp)
                                PixelText(type.description, 6.sp, Retro.dim)
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                        RetroButton(
                            onClick = { act { s, t -> if (City.whyNot(s, t, type, x, y) == null) City.build(s, t, type, x, y) else s } },
                            enabled = reason == null,
                        ) { Text("Build") }
                    }
                }
            }
        }
    }
    if (confirmDemolish && building != null) {
        AlertDialog(
            onDismissRequest = { confirmDemolish = false },
            title = { Text("Demolish ${building.type.title}?") },
            text = { Text("You get back half of what the first level cost.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDemolish = false
                    act { s, _ -> City.buildingAt(s, x, y)?.let { City.demolish(s, it) } ?: s }
                }) { Text("Demolish") }
            },
            dismissButton = { TextButton(onClick = { confirmDemolish = false }) { Text("Cancel") } },
        )
    }
}
