package com.gymcoach.ui.hero

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.gymcoach.game.Boss
import com.gymcoach.game.BuildingType
import com.gymcoach.game.Foe
import com.gymcoach.game.Stage
import com.gymcoach.game.Terrain
import com.gymcoach.game.TowerKind

// Pixel art for Castle Defender: the battlefield, towers and enemies.

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
    Foe.WOLF to listOf(
        "........",
        "kk......",
        "kakkkkk.",
        "keaaaaak",
        ".kwaaaak",
        "..kaaaak",
        "..kak.ka",
        "..kk..kk",
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
    Foe.SHAMAN to listOf(
        "...kk..x",
        "..kwwk.x",
        ".kaeeakx",
        ".kaaaakx",
        "kwwaawwx",
        "kwaaaawk",
        ".kwwwwk.",
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
    Foe.WOLF to Color(0xFF8A8A9A),
    Foe.BAT to Color(0xFF7A5AA8),
    Foe.ORC to Color(0xFF3F7A3A),
    Foe.SHAMAN to Color(0xFFB04A8A),
    Foe.TROLL to Color(0xFF7A8AA0),
)

fun foeRows(foe: Foe, boss: Boss): List<String> = if (foe == Foe.BOSS) bossRows(boss) else foeArt.getValue(foe)

fun foePalette(foe: Foe, boss: Boss, flash: Boolean): Map<Char, Color> {
    val palette = if (foe == Foe.BOSS) {
        bossPalette(boss)
    } else {
        mapOf(
            'k' to Retro.night, 'a' to foeColors.getValue(foe), 'e' to Retro.ember,
            'w' to if (foe == Foe.SHAMAN) Color(0xFF6A3D9A) else Retro.parchment, 'x' to Color(0xFFC0C8D0),
        )
    }
    return if (flash) palette.mapValues { Color.White } else palette
}

private val cannonArt: List<String> by lazy {
    sprite {
        rect(3, 7, 12, 14, 'r'); rect(9, 7, 12, 14, 'R')
        for (x in listOf(3, 6, 9, 12)) rect(x, 5, x, 6, 'r')
        rect(5, 3, 10, 6, 'd'); rect(6, 2, 9, 3, 'k')
        rect(10, 4, 14, 5, 'k')
        rect(6, 11, 9, 14, 'd')
        set(4, 9, 'x'); set(11, 9, 'x')
    }
}

private val frostArt: List<String> by lazy {
    sprite {
        rect(4, 6, 11, 14, 's'); rect(9, 6, 11, 14, 'S')
        rect(6, 1, 9, 5, 'W'); rect(7, 0, 8, 6, 'W'); set(5, 3, 'W'); set(10, 3, 'W')
        rect(7, 2, 8, 3, 'x')
        rect(6, 10, 9, 14, 'u')
        set(5, 8, 'W'); set(10, 8, 'W')
    }
}

fun towerSprite(kind: TowerKind): List<String> = when (kind) {
    TowerKind.ARCHER -> buildingSprite(BuildingType.ARCHER_TOWER)
    TowerKind.MAGE -> buildingSprite(BuildingType.MAGE_TOWER)
    TowerKind.CANNON -> cannonArt
    TowerKind.FROST -> frostArt
}

private val plotArt: List<String> by lazy {
    sprite {
        rect(3, 9, 12, 13, 'p'); rect(9, 9, 12, 13, 'P')
        rect(7, 2, 7, 9, 'b')
        rect(8, 2, 11, 4, 'o'); rect(8, 4, 10, 5, 'O')
    }
}

private val road = Color(0xFF8A6A43)
private val roadDark = Color(0xFF6E5233)
private val roadLight = Color(0xFFA8865A)

/** Dirt road with pebbles, a darker rim where it meets the grass. */
private fun DrawScope.drawRoad(x: Int, y: Int, roads: Set<Pair<Int, Int>>, topLeft: Offset, tile: Float) {
    val px = tile / 16f
    drawRect(road, topLeft, Size(tile + 0.5f, tile + 0.5f))
    val edge = px * 1.5f
    if ((x - 1 to y) !in roads && x > 0) drawRect(roadDark, topLeft, Size(edge, tile))
    if ((x + 1 to y) !in roads && x < Stage.COLS - 1) drawRect(roadDark, Offset(topLeft.x + tile - edge, topLeft.y), Size(edge, tile))
    if ((x to y - 1) !in roads && y > 0) drawRect(roadDark, topLeft, Size(tile, edge))
    if ((x to y + 1) !in roads && y < Stage.ROWS - 1) drawRect(roadDark, Offset(topLeft.x, topLeft.y + tile - edge), Size(tile, edge))
    var seed = x * 31 + y * 97 + 5
    repeat(4) {
        seed = (seed * 1103515245 + 12345) and 0x7fffffff
        val gx = 2 + seed % 12
        val gy = 2 + (seed / 16) % 12
        drawRect(if (it % 2 == 0) roadLight else roadDark, Offset(topLeft.x + gx * px, topLeft.y + gy * px), Size(px + 0.5f, px + 0.5f))
    }
}

/** Trees and rocks on the grass, the same every time for a stage. */
fun scenery(stage: Stage): Map<Pair<Int, Int>, Terrain> {
    val blocked = stage.roadTiles + stage.spots + castleTiles(stage)
    val out = mutableMapOf<Pair<Int, Int>, Terrain>()
    for (y in 0 until Stage.ROWS) for (x in 0 until Stage.COLS) {
        if ((x to y) in blocked) continue
        val h = ((x * 7349 + y * 1931 + stage.ordinal * 4421) * 2654435761L ushr 7).toInt() and 0xff
        when {
            h < 70 -> out[x to y] = Terrain.TREE
            h < 90 -> out[x to y] = Terrain.ROCK
        }
    }
    return out
}

/** The gate and the wall either side of it. */
fun castleTiles(stage: Stage): Set<Pair<Int, Int>> {
    val (gx, gy) = stage.gate
    return setOf(gx - 1 to gy, gx to gy, gx + 1 to gy)
}

/** Grass, scenery, the road, empty build plots and the castle. Towers and enemies are drawn on top. */
fun DrawScope.drawBattlefield(stage: Stage, scenery: Map<Pair<Int, Int>, Terrain>, tile: Float, gateHit: Boolean) {
    val roads = stage.roadTiles
    val castle = castleTiles(stage)
    for (y in 0 until Stage.ROWS) for (x in 0 until Stage.COLS) {
        val topLeft = Offset(x * tile, y * tile)
        if ((x to y) in roads && (x to y) != stage.gate) {
            drawRoad(x, y, roads, topLeft, tile)
        } else {
            drawCityTile(x, y, scenery[x to y] ?: Terrain.GRASS, null, 0, topLeft, tile, 0)
        }
    }
    val (gx, gy) = stage.gate
    for ((x, y) in castle) {
        val type = if (x == gx) BuildingType.TOWN_HALL else BuildingType.WALL
        if (x !in 0 until Stage.COLS) continue
        drawPixels(buildingSprite(type), cityPalette, Offset(x * tile, y * tile), tile / 16f)
    }
    if (gateHit) drawRect(Retro.hp.copy(alpha = 0.45f), Offset((gx - 1) * tile, gy * tile), Size(tile * 3, tile))
}

fun DrawScope.drawPlot(topLeft: Offset, tile: Float) = drawPixels(plotArt, cityPalette, topLeft, tile / 16f)

fun DrawScope.drawTower(kind: TowerKind, level: Int, topLeft: Offset, tile: Float) {
    val px = tile / 16f
    drawPixels(towerSprite(kind), cityPalette, topLeft, px)
    for (i in 0 until level) {
        drawRect(Retro.night, Offset(topLeft.x + (1 + i * 3) * px, topLeft.y + 13 * px), Size(px * 3, px * 3))
        drawRect(Retro.gold, Offset(topLeft.x + (1.5f + i * 3) * px, topLeft.y + 13.5f * px), Size(px * 2, px * 2))
    }
}

private val starArt = listOf(
    "...k...",
    "..kyk..",
    "kkkyykk",
    "kyyyyyk",
    ".kyyyk.",
    ".kykyk.",
    "kk...kk",
)

fun starRows(): List<String> = starArt

fun starPalette(lit: Boolean) = mapOf('k' to Retro.night, 'y' to if (lit) Retro.gold else Retro.dim)
