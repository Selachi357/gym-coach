package com.gymcoach.ui.hero

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.gymcoach.game.Boss
import com.gymcoach.game.Building
import com.gymcoach.game.Castle
import com.gymcoach.game.Island
import com.gymcoach.game.Voyage
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** A full-width pixel picture that keeps its aspect ratio. */
@Composable
fun PixelPicture(rows: List<String>, palette: Map<Char, Color>, modifier: Modifier = Modifier) {
    val height = rows.size
    val width = rows.maxOf { it.length }
    Canvas(modifier.fillMaxWidth().aspectRatio(width.toFloat() / height)) {
        val px = size.width / width
        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, c ->
                val color = palette[c] ?: return@forEachIndexed
                drawRect(color, Offset(x * px, y * px), Size(px + 0.5f, px + 0.5f))
            }
        }
    }
}

/** A character grid to paint scenes into. */
private class Grid(val width: Int, val height: Int, fill: Char) {
    val cells = Array(height) { CharArray(width) { fill } }

    fun set(x: Int, y: Int, c: Char) {
        if (x in 0 until width && y in 0 until height) cells[y][x] = c
    }

    fun rect(x0: Int, y0: Int, x1: Int, y1: Int, c: Char) {
        for (y in y0..y1) for (x in x0..x1) set(x, y, c)
    }

    /** A pointed roof sitting on top of the span x0..x1, with its base on row [base]. */
    fun roof(x0: Int, x1: Int, base: Int, c: Char) {
        var left = x0
        var right = x1
        var y = base
        while (left <= right) {
            for (x in left..right) set(x, y, c)
            left++
            right--
            y--
        }
    }

    /** Battlements: a merlon every other pair of pixels along row [y]. */
    fun crenels(x0: Int, x1: Int, y: Int, c: Char) {
        for (x in x0..x1) if ((x - x0) % 4 < 2) set(x, y, c)
    }

    fun sprite(rows: List<String>, x0: Int, y0: Int) {
        rows.forEachIndexed { dy, row -> row.forEachIndexed { dx, c -> if (c != '.') set(x0 + dx, y0 + dy, c) } }
    }

    fun line(x0: Int, y0: Int, x1: Int, y1: Int, c: Char, dotted: Boolean) {
        val steps = max(abs(x1 - x0), abs(y1 - y0)).coerceAtLeast(1)
        for (i in 0..steps) {
            if (dotted && i % 2 == 1) continue
            set(x0 + ((x1 - x0) * i.toFloat() / steps).roundToInt(), y0 + ((y1 - y0) * i.toFloat() / steps).roundToInt(), c)
        }
    }

    fun rows(): List<String> = cells.map { String(it) }
}

// Bosses, 16 × 16. k outline, a body, b accent, e eye, w teeth/claws, y gold, c water.

private val bossArt: Map<Boss, List<String>> = mapOf(
    Boss.RAT_KING to listOf(
        ".....y.y.y......",
        ".....yyyyy......",
        "..kk.kkkkk.kk...",
        ".kbbkaaaaakbbk..",
        ".kbkaaaaaaakbk..",
        "..kaaeaaaeaak...",
        "..kaaaaaaaaak...",
        "...kaaabaaak....",
        "..kkaawawaakk...",
        ".kaaaaaaaaaaak..",
        ".kaaaaaaaaaaak..",
        ".kaabaaaaabaak..",
        "..kaaaaaaaaak...",
        "...kkaakkaakk...",
        "..kbbk...kbbk..k",
        "..........kkkkk.",
    ),
    Boss.SEA_SERPENT to listOf(
        "................",
        "....kkkk........",
        "...kaaaak.......",
        "..kaeaaaak......",
        "..kaaaaaaak.....",
        "..kwkwkaaak.....",
        "...k.k.kaak.....",
        "........kaak....",
        ".kk......kaak...",
        "kbak.....kaak...",
        "kaaak...kaak....",
        ".kaaakkkaaak....",
        "..kbaaaaaabk....",
        "...kbbbbbbk.....",
        "cccccccccccccccc",
        "c.cc.ccc.cc.cccc",
    ),
    Boss.STORM_HARPY to listOf(
        "......kkkk......",
        ".....kbbbbk.....",
        ".....kbebek.....",
        "k....kbbbbk....k",
        "ak...kkaakk...ka",
        "aak.kaaaaaak.kaa",
        "aaakaaaaaaaakaaa",
        ".aaaaaaaaaaaaaa.",
        "..aaaaaaaaaaaa..",
        "...aak.aa.kaa...",
        "....k.kaak.k....",
        "......kaak......",
        "......k..k......",
        ".....wk..kw.....",
        ".....ww..ww.....",
        "..y.........y...",
    ),
    Boss.IRON_GOLEM to listOf(
        "....kkkkkkkk....",
        "....kaaaaaak....",
        "....kaeaaeak....",
        "....kaaaaaak....",
        "....kkbbbbkk....",
        ".kkkkaaaaaakkkk.",
        "kaaakaabbaakaaak",
        "kaaakaaaaaakaaak",
        "kaaakabbbbakaaak",
        "kbbkkaaaaaakkbbk",
        "kbbk.kaaaak.kbbk",
        ".kk..kaaaak..kk.",
        ".....kak.kak....",
        "....kaak.kaak...",
        "....kaak.kaak...",
        "....kkkk.kkkk...",
    ),
    Boss.KRAKEN to listOf(
        ".....kkkkkk.....",
        "....kaaaaaak....",
        "...kaaaaaaaak...",
        "...kabaaaabak...",
        "...kaeaaaaeak...",
        "...kaaaaaaaak...",
        "....kaaaaaak....",
        "...kakakakakak..",
        "..kak.kakak.kak.",
        ".kak..kak.kak.ka",
        ".kak.kak...kak.k",
        "kak..kak...kak..",
        "kak...kak.kak...",
        ".kk...kak..kk...",
        "cccccccccccccccc",
        "c.cc.cccc.cc.ccc",
    ),
    Boss.DEEP_DRAGON to listOf(
        "..k.........k...",
        ".kak.......kak..",
        ".kaak.kkk.kaak..",
        "..kaakaaakaak...",
        "...kaaeaeaak....",
        "...kaaaaaaak....",
        "....kawwwak.....",
        "bk...kaaak...kb.",
        "bbk.kaaaaak.kbb.",
        "bbbkaaaaaaakbbb.",
        ".bbkaayyyaakbb..",
        "..kaaayyyaaak...",
        "..kaaaayaaaak...",
        "...kaak.kaak...k",
        "...kaak.kaakkkak",
        "..kkkk..kkkkkkk.",
    ),
)

private val bossColors: Map<Boss, Pair<Color, Color>> = mapOf(
    Boss.RAT_KING to (Color(0xFF8A7F74) to Color(0xFFE8A0A8)),
    Boss.SEA_SERPENT to (Color(0xFF2FA37A) to Color(0xFFB8E06A)),
    Boss.STORM_HARPY to (Color(0xFF7A6AB8) to Color(0xFFF1C27D)),
    Boss.IRON_GOLEM to (Color(0xFF8C96A3) to Color(0xFF5A6570)),
    Boss.KRAKEN to (Color(0xFFB0408A) to Color(0xFFE88AC8)),
    Boss.DEEP_DRAGON to (Color(0xFFB03030) to Color(0xFF6A3D9A)),
)

/** [defeated] greys the boss out. */
@Composable
fun BossSprite(boss: Boss, size: androidx.compose.ui.unit.Dp, defeated: Boolean = false, modifier: Modifier = Modifier) {
    val (body, accent) = bossColors.getValue(boss)
    val palette = if (defeated) {
        mapOf('k' to Retro.night, 'a' to Retro.dim, 'b' to Retro.panel, 'e' to Retro.night, 'w' to Retro.dim, 'y' to Retro.dim, 'c' to Retro.panel)
    } else {
        mapOf(
            'k' to Retro.night, 'a' to body, 'b' to accent, 'e' to Retro.ember, 'w' to Color.White,
            'y' to Retro.gold, 'c' to Color(0xFF2E5A9C),
        )
    }
    PixelSprite(bossArt.getValue(boss), palette, size, modifier)
}

// Voyage map, 64 × 40.

private const val MAP_W = 64
private const val MAP_H = 40

private val islandSpots: Map<Island, Pair<Int, Int>> = mapOf(
    Island.HOME_PORT to (5 to 34),
    Island.GULL_ROCK to (15 to 29),
    Island.SMUGGLERS_COVE to (25 to 34),
    Island.MERMAID_REEF to (35 to 28),
    Island.FORT_IRONHOLD to (47 to 33),
    Island.ISLE_OF_EMBERS to (57 to 23),
    Island.FROST_FJORD to (48 to 14),
    Island.SUNKEN_TEMPLE to (36 to 9),
    Island.DRAGON_ISLE to (23 to 7),
    Island.EDGE_OF_MAP to (8 to 5),
)

private val ship = listOf(
    "..r..",
    "..kw.",
    "..kww",
    "bbbbb",
    ".bbb.",
)

fun voyageMap(voyage: Voyage): List<String> {
    val g = Grid(MAP_W, MAP_H, 'c')
    // Waves.
    for (y in 0 until MAP_H) for (x in 0 until MAP_W) if (y % 4 == 1 && (x + y * 3) % 9 < 2) g.set(x, y, 'v')
    // Route, solid where you have sailed.
    val islands = Island.entries
    islands.zipWithNext().forEach { (a, b) ->
        val (x0, y0) = islandSpots.getValue(a)
        val (x1, y1) = islandSpots.getValue(b)
        g.line(x0, y0, x1, y1, if (b in voyage.reached) 'd' else 'n', dotted = true)
    }
    for (island in islands) {
        val (x, y) = islandSpots.getValue(island)
        val reached = island in voyage.reached
        g.rect(x - 3, y - 1, x + 3, y + 1, 's')
        g.rect(x - 2, y - 2, x + 2, y + 2, 's')
        g.rect(x - 1, y - 1, x + 1, y, if (island == Island.ISLE_OF_EMBERS) 'f' else if (island == Island.FROST_FJORD) 'i' else 'g')
        if (reached) {
            g.rect(x + 1, y - 5, x + 1, y - 2, 'k')
            g.rect(x + 2, y - 5, x + 3, y - 4, if (island in voyage.chestsOpened) 'y' else 'r')
        }
    }
    // The ship sails between the last island reached and the next one.
    val (ax, ay) = islandSpots.getValue(voyage.at)
    val next = voyage.next
    val (sx, sy) = if (next == null) {
        ax to ay
    } else {
        val (bx, by) = islandSpots.getValue(next)
        val t = ((voyage.km - voyage.at.km) / (next.km - voyage.at.km)).coerceIn(0.0, 1.0)
        (ax + (bx - ax) * t).roundToInt() to (ay + (by - ay) * t).roundToInt()
    }
    g.sprite(ship, sx - 2, sy - 6)
    return g.rows()
}

val mapPalette = mapOf(
    'c' to Color(0xFF1E3A6E),
    'v' to Color(0xFF3F6FB0),
    'd' to Retro.gold,
    'n' to Color(0xFF5A7AB0),
    's' to Color(0xFFE0C98A),
    'g' to Color(0xFF3FA34D),
    'f' to Retro.ember,
    'i' to Color(0xFFDDEEFF),
    'k' to Retro.night,
    'r' to Color(0xFFB03030),
    'y' to Retro.gold,
    'w' to Retro.parchment,
    'b' to Color(0xFF8B5A2B),
)

// Castle, 64 × 40. Buildings are painted back to front.

fun castleScene(castle: Castle): List<String> {
    val g = Grid(64, 40, 'n')
    val has = castle.built.toSet()
    for (i in 0 until 18) g.set((i * 37 + 5) % 64, (i * 13 + 2) % 22, 'x')
    g.rect(52, 3, 55, 6, 'm')
    g.set(52, 3, 'n'); g.set(55, 6, 'n')

    if (Building.GREAT_HALL in has) {
        g.rect(18, 18, 45, 33, 'h')
        // A long, low roof: four rows that step in by two pixels.
        for (i in 0..3) g.rect(17 + i * 2, 17 - i, 46 - i * 2, 17 - i, 'r')
        for (x in listOf(21, 42)) g.rect(x, 22, x + 1, 24, 'f')
    }
    if (Building.KEEP in has) {
        g.rect(25, 12, 38, 33, 'a')
        g.crenels(25, 38, 11, 'a')
        for (y in listOf(15, 21)) {
            g.rect(28, y, 29, y + 2, 'k')
            g.rect(34, y, 35, y + 2, 'k')
        }
    }
    if (Building.ROYAL_TOWER in has) {
        g.rect(29, 4, 34, 11, 'a')
        g.roof(28, 35, 3, 'y')
        g.rect(31, 6, 32, 8, 'f')
    }
    if (Building.WATCHTOWER in has) {
        g.rect(5, 10, 11, 33, 'a')
        g.roof(4, 12, 9, 'r')
        g.rect(8, 14, 8, 16, 'k')
    }
    if (Building.BARRACKS in has) {
        g.rect(13, 21, 22, 33, 'w')
        g.roof(12, 23, 20, 'r')
        g.rect(16, 24, 18, 25, 'k')
    }
    if (Building.FORGE in has) {
        g.rect(44, 22, 51, 33, 'a')
        g.roof(43, 52, 21, 'k')
        g.rect(49, 14, 50, 18, 'a')
        g.set(50, 12, 'o'); g.set(49, 10, 'o'); g.set(51, 9, 'o')
        g.rect(46, 25, 48, 27, 'f')
    }
    if (Building.CHAPEL in has) {
        g.rect(54, 18, 60, 33, 'w')
        g.roof(53, 61, 17, 'r')
        g.rect(57, 9, 57, 13, 'y')
        g.rect(56, 10, 58, 10, 'y')
        g.rect(56, 22, 58, 25, 'b')
    }

    if (Building.STONE_WALL in has) {
        g.rect(2, 27, 61, 33, 'a')
        g.crenels(2, 61, 26, 'a')
        for (y in 28..33 step 2) for (x in 2..61) if ((x + y) % 6 == 0) g.set(x, y, 'k')
    } else if (Building.PALISADE in has) {
        for (x in 2..61 step 2) {
            g.rect(x, 28, x, 33, 'p')
            g.set(x, 27, 'q')
        }
    }
    if (Building.GATEHOUSE in has) {
        g.rect(27, 22, 36, 33, 'a')
        g.crenels(27, 36, 21, 'a')
        g.rect(29, 26, 34, 33, 'k')
        for (x in 30..33 step 2) g.rect(x, 27, x, 33, 'q')
    }
    // One banner per kept week along the wall top.
    if (Building.PALISADE in has || Building.STONE_WALL in has) {
        val spots = listOf(4, 10, 16, 22, 40, 46, 52, 58, 25, 38)
        spots.take(castle.banners.coerceAtMost(spots.size)).forEachIndexed { i, x ->
            g.rect(x, 21, x, 25, 'k')
            g.rect(x + 1, 21, x + 2, 23, if (i % 2 == 0) 'r' else 'y')
        }
    }

    g.rect(0, 34, 63, 39, 'e')
    g.rect(0, 34, 63, 34, 'g')
    for (x in 0 until 64 step 5) g.set(x, 37 + (x / 5) % 2, 'g')
    // The campfire is always there.
    g.rect(30, 37, 34, 37, 'b')
    g.set(32, 34, 'o'); g.rect(31, 35, 33, 36, 'f'); g.set(32, 35, 'o')
    return g.rows()
}

val castlePalette = mapOf(
    'n' to Color(0xFF151530),
    'x' to Retro.parchment,
    'm' to Color(0xFFF3E9C0),
    'a' to Color(0xFF8C8FA3),
    'h' to Color(0xFF6B5A4A),
    'w' to Color(0xFFB09878),
    'r' to Color(0xFFB03030),
    'y' to Retro.gold,
    'k' to Color(0xFF1B1B2F),
    'f' to Retro.ember,
    'o' to Color(0xFFFFD84A),
    'b' to Color(0xFF6B4423),
    'p' to Color(0xFF8B5A2B),
    'q' to Color(0xFF5C3A1E),
    'e' to Color(0xFF3B2A1E),
    'g' to Color(0xFF3FA34D),
)

@Composable
fun VoyageMap(voyage: Voyage, modifier: Modifier = Modifier) {
    val rows = remember(voyage) { voyageMap(voyage) }
    PixelPicture(rows, mapPalette, modifier)
}

@Composable
fun CastleScene(castle: Castle, modifier: Modifier = Modifier) {
    val rows = remember(castle) { castleScene(castle) }
    PixelPicture(rows, castlePalette, modifier)
}
