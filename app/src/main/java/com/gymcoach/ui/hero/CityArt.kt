package com.gymcoach.ui.hero

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.gymcoach.game.BuildingType
import com.gymcoach.game.ShopItem
import com.gymcoach.game.Terrain

/**
 * 16 × 16 village sprites. Shapes are painted without outlines; [outlined] then adds a dark outline
 * around every shape, which gives the classic 16-bit look.
 */
internal class Sprite16 {
    val cells = Array(16) { CharArray(16) { '.' } }

    fun set(x: Int, y: Int, c: Char) {
        if (x in 0..15 && y in 0..15) cells[y][x] = c
    }

    fun rect(x0: Int, y0: Int, x1: Int, y1: Int, c: Char) {
        for (y in y0..y1) for (x in x0..x1) set(x, y, c)
    }

    /** A gable roof whose base spans x0..x1 on row [base]; the right half uses the darker [shade]. */
    fun gable(x0: Int, x1: Int, base: Int, c: Char, shade: Char) {
        var l = x0
        var r = x1
        var y = base
        val mid = (x0 + x1) / 2.0
        while (l <= r) {
            for (x in l..r) set(x, y, if (x > mid) shade else c)
            l++
            r--
            y--
        }
    }

    fun outlined(): List<String> {
        val out = Array(16) { y -> cells[y].copyOf() }
        for (y in 0..15) for (x in 0..15) {
            if (cells[y][x] != '.') continue
            val touches = listOf(x - 1 to y, x + 1 to y, x to y - 1, x to y + 1).any { (nx, ny) ->
                nx in 0..15 && ny in 0..15 && cells[ny][nx] != '.' && cells[ny][nx] != 'k'
            }
            if (touches) out[y][x] = 'k'
        }
        return out.map { String(it) }
    }
}

internal fun sprite(block: Sprite16.() -> Unit): List<String> = Sprite16().apply(block).outlined()

private val buildingArt: Map<BuildingType, List<String>> by lazy {
    mapOf(
        BuildingType.TOWN_HALL to sprite {
            rect(1, 5, 3, 14, 's'); rect(12, 5, 14, 14, 's')
            set(1, 4, 's'); set(3, 4, 's'); set(12, 4, 's'); set(14, 4, 's')
            rect(4, 7, 11, 14, 's')
            gable(4, 11, 6, 'u', 'U')
            rect(8, 0, 8, 2, 'k'); rect(9, 0, 10, 1, 'y')
            rect(7, 11, 8, 14, 'd')
            set(5, 9, 'n'); set(10, 9, 'n'); set(2, 7, 'n'); set(13, 7, 'n')
            for (y in listOf(8, 12)) for (x in 4..11 step 3) if (cells[y][x] == 's') set(x, y, 'S')
        },
        BuildingType.HOUSE to sprite {
            rect(3, 8, 12, 14, 'p')
            gable(2, 13, 7, 'o', 'O')
            rect(7, 11, 8, 14, 'd')
            rect(4, 9, 5, 10, 'n'); rect(10, 9, 11, 10, 'n')
            rect(10, 2, 11, 4, 's')
        },
        BuildingType.FARM to sprite {
            for (y in 8..14) rect(1, y, 14, y, if (y % 2 == 0) 'c' else 'P')
            rect(2, 4, 7, 7, 'o')
            gable(1, 8, 3, 'P', 'b')
            rect(4, 6, 5, 7, 'd')
            rect(10, 4, 13, 6, 'h')
        },
        BuildingType.LUMBER_MILL to sprite {
            rect(2, 7, 10, 13, 'p')
            gable(1, 11, 6, 'P', 'b')
            rect(5, 10, 6, 13, 'd')
            rect(11, 11, 14, 14, 'b')
            set(11, 11, 'h'); set(13, 11, 'h'); set(12, 13, 'h'); set(14, 13, 'h')
            rect(12, 7, 14, 9, 'x'); set(13, 8, 'S')
        },
        BuildingType.QUARRY to sprite {
            rect(1, 9, 7, 14, 'R'); rect(2, 8, 6, 9, 'r')
            rect(6, 6, 13, 14, 'r'); rect(10, 6, 13, 14, 'R'); rect(7, 5, 11, 6, 'r')
            rect(10, 2, 14, 2, 'x'); set(9, 3, 'x')
            set(12, 3, 'b'); set(12, 4, 'b')
        },
        BuildingType.WELL to sprite {
            rect(4, 4, 4, 9, 'b'); rect(11, 4, 11, 9, 'b')
            gable(3, 12, 4, 'o', 'O')
            rect(7, 5, 8, 5, 'k'); rect(7, 6, 8, 7, 'P')
            rect(3, 9, 12, 13, 's'); rect(5, 10, 10, 11, 'w')
            set(4, 12, 'S'); set(8, 12, 'S'); set(11, 10, 'S')
        },
        BuildingType.WALL to sprite {
            rect(1, 6, 14, 14, 's')
            for (x in listOf(1, 5, 9, 13)) rect(x, 4, x + 1, 5, 's')
            for (y in listOf(8, 11, 14)) for (x in 1..14) if ((x + y) % 4 == 0) set(x, y, 'S')
        },
        BuildingType.ARCHER_TOWER to sprite {
            rect(4, 4, 11, 14, 's')
            for (x in listOf(4, 7, 10)) rect(x, 2, x + 1, 3, 's')
            rect(7, 6, 8, 8, 'd')
            rect(5, 10, 6, 12, 'o'); rect(9, 10, 10, 12, 'o')
            for (y in listOf(5, 9, 13)) set(4 + y % 7, y, 'S')
        },
        BuildingType.BLACKSMITH to sprite {
            gable(1, 14, 6, 'S', 'R')
            rect(2, 7, 2, 14, 'P'); rect(13, 7, 13, 14, 'P')
            rect(4, 11, 8, 11, 'R'); rect(5, 12, 7, 12, 'R'); rect(6, 13, 6, 14, 'R')
            rect(9, 9, 12, 14, 's'); rect(10, 10, 11, 12, 'e'); set(10, 10, 'n')
            rect(11, 1, 12, 3, 's')
        },
        BuildingType.MARKET to sprite {
            for (x in 1..14) rect(x, 4, x, 6, if ((x / 2) % 2 == 0) 'o' else 'x')
            rect(2, 7, 2, 14, 'P'); rect(13, 7, 13, 14, 'P')
            rect(3, 10, 12, 12, 'p')
            set(4, 9, 'y'); set(6, 9, 'c'); set(7, 9, 'c'); set(9, 9, 'e'); set(11, 9, 'y')
        },
        BuildingType.TAVERN to sprite {
            rect(2, 7, 12, 14, 'x')
            for (x in listOf(2, 7, 12)) rect(x, 7, x, 14, 'P')
            rect(2, 10, 12, 10, 'P')
            gable(1, 13, 6, 'O', 'b')
            rect(9, 12, 10, 14, 'd'); rect(4, 8, 5, 9, 'n'); rect(4, 12, 5, 13, 'n')
            rect(13, 8, 14, 8, 'b'); rect(13, 9, 14, 10, 'y')
        },
        BuildingType.STABLE to sprite {
            rect(2, 8, 13, 14, 'P')
            gable(1, 14, 7, 'o', 'O')
            rect(3, 10, 5, 14, 'd'); rect(10, 10, 12, 14, 'd')
            rect(7, 11, 8, 14, 'h')
            rect(4, 9, 5, 10, 'b'); set(5, 9, 'k')
        },
        BuildingType.LIBRARY to sprite {
            rect(2, 7, 13, 14, 'x')
            gable(1, 14, 6, 'u', 'U')
            for (x in listOf(3, 6, 9, 12)) rect(x, 8, x, 14, 'S')
            rect(7, 11, 8, 14, 'd')
            rect(4, 9, 5, 10, 'n'); rect(10, 9, 11, 10, 'n')
            rect(7, 3, 8, 4, 'o')
        },
        BuildingType.TEMPLE to sprite {
            rect(2, 8, 13, 14, 'x')
            rect(5, 4, 10, 7, 'y'); rect(6, 3, 9, 3, 'y'); rect(9, 4, 10, 7, 'n')
            rect(7, 1, 8, 2, 'y')
            for (x in listOf(3, 5, 10, 12)) rect(x, 9, x, 14, 'S')
            rect(7, 11, 8, 14, 'd')
        },
        BuildingType.MAGE_TOWER to sprite {
            rect(5, 5, 10, 14, 'v'); rect(8, 5, 10, 14, 'V')
            gable(4, 11, 4, 'V', 'v')
            set(7, 0, 'y'); set(8, 0, 'y')
            rect(7, 7, 8, 8, 'W'); rect(7, 11, 8, 14, 'd')
            set(3, 3, 'W'); set(12, 6, 'W'); set(13, 2, 'y')
        },
    )
}

private val treeArt: List<String> by lazy {
    sprite {
        rect(4, 2, 11, 9, 'f'); rect(3, 4, 12, 8, 'f'); rect(5, 1, 10, 10, 'f')
        rect(5, 3, 7, 5, 'F'); set(9, 4, 'F'); set(4, 6, 'F')
        rect(7, 11, 8, 14, 'b')
    }
}

private val rockArt: List<String> by lazy {
    sprite {
        rect(3, 7, 12, 13, 'r'); rect(5, 5, 10, 7, 'r'); rect(9, 6, 12, 13, 'R')
        set(5, 8, 'x'); set(6, 7, 'x')
        rect(1, 11, 3, 13, 'R')
    }
}

val cityPalette = mapOf(
    'k' to Color(0xFF1B1B2F),
    'g' to Color(0xFF4C9A3A),
    'G' to Color(0xFF3E7F30),
    'l' to Color(0xFF6BBF4A),
    'f' to Color(0xFF25592A),
    'F' to Color(0xFF3F8A3A),
    'b' to Color(0xFF6B4423),
    'r' to Color(0xFF9CA0B3),
    'R' to Color(0xFF6A6E80),
    'w' to Color(0xFF2E5A9C),
    'W' to Color(0xFF3FC1E0),
    's' to Color(0xFFB0B8C8),
    'S' to Color(0xFF7A8090),
    'o' to Color(0xFFB03030),
    'O' to Color(0xFF7A2020),
    'p' to Color(0xFFC08850),
    'P' to Color(0xFF8B5A2B),
    'y' to Retro.gold,
    'd' to Color(0xFF4A2F1B),
    'n' to Color(0xFFFFD84A),
    'h' to Color(0xFFE0C060),
    'c' to Color(0xFF8BD050),
    'u' to Color(0xFF2E5A9C),
    'U' to Color(0xFF1E3A6E),
    'v' to Color(0xFF6A3D9A),
    'V' to Color(0xFF9A6AD0),
    'e' to Retro.ember,
    'x' to Color(0xFFF3E3C3),
)

/** Grass with a few lighter and darker blades, different on every tile. */
private fun DrawScope.drawGrass(x: Int, y: Int, topLeft: Offset, px: Float) {
    drawRect(cityPalette.getValue('g'), topLeft, Size(px * 16 + 0.5f, px * 16 + 0.5f))
    var seed = x * 73 + y * 151 + 17
    repeat(7) {
        seed = (seed * 1103515245 + 12345) and 0x7fffffff
        val gx = seed % 16
        val gy = (seed / 16) % 16
        val c = cityPalette.getValue(if (it % 2 == 0) 'l' else 'G')
        drawRect(c, Offset(topLeft.x + gx * px, topLeft.y + gy * px), Size(px + 0.5f, px * 2 + 0.5f))
    }
}

private fun DrawScope.drawWater(x: Int, y: Int, topLeft: Offset, px: Float, tick: Int) {
    drawRect(cityPalette.getValue('w'), topLeft, Size(px * 16 + 0.5f, px * 16 + 0.5f))
    for (row in listOf(3, 9, 14)) {
        val shift = (tick + x * 5 + row) % 16
        for (i in 0 until 3) {
            val wx = (shift + i) % 16
            drawRect(cityPalette.getValue('W'), Offset(topLeft.x + wx * px, topLeft.y + ((row + y) % 16) * px), Size(px + 0.5f, px + 0.5f))
        }
    }
}

/** Draws one map tile: ground, then the tree, rock or building on it, then level pips. */
fun DrawScope.drawCityTile(
    x: Int,
    y: Int,
    terrain: Terrain,
    building: BuildingType?,
    level: Int,
    topLeft: Offset,
    tile: Float,
    tick: Int,
) {
    val px = tile / 16f
    if (terrain == Terrain.WATER) drawWater(x, y, topLeft, px, tick) else drawGrass(x, y, topLeft, px)
    when {
        building != null -> drawPixels(buildingArt.getValue(building), cityPalette, topLeft, px)
        terrain == Terrain.TREE -> drawPixels(treeArt, cityPalette, topLeft, px)
        terrain == Terrain.ROCK -> drawPixels(rockArt, cityPalette, topLeft, px)
        else -> Unit
    }
    if (building != null && building.maxLevel > 1) {
        for (i in 0 until level) {
            drawRect(Retro.night, Offset(topLeft.x + (1 + i * 3) * px, topLeft.y + 13 * px), Size(px * 3, px * 3))
            drawRect(Retro.gold, Offset(topLeft.x + (1.5f + i * 3) * px, topLeft.y + 13.5f * px), Size(px * 2, px * 2))
        }
    }
}

/** A single building sprite for menus. */
fun buildingSprite(type: BuildingType): List<String> = buildingArt.getValue(type)

private val petArt: Map<ShopItem, List<String>> by lazy {
    mapOf(
        ShopItem.WOLF to sprite {
            rect(3, 8, 11, 11, 'r'); rect(4, 9, 10, 11, 'R')
            rect(10, 5, 13, 8, 'r'); set(10, 4, 'r'); set(12, 4, 'r'); set(12, 6, 'k'); set(14, 7, 'k'); set(13, 7, 'r')
            rect(3, 12, 3, 14, 'R'); rect(5, 12, 5, 14, 'R'); rect(9, 12, 9, 14, 'R'); rect(11, 12, 11, 14, 'R')
            rect(1, 7, 2, 8, 'r'); set(1, 6, 'r')
            rect(4, 11, 10, 11, 'x')
        },
        ShopItem.RAVEN to sprite {
            rect(5, 7, 10, 11, 'U'); rect(9, 4, 12, 7, 'U'); rect(13, 5, 14, 6, 'S'); set(11, 5, 'y')
            rect(3, 8, 7, 10, 'u'); rect(2, 10, 4, 11, 'u')
            rect(6, 12, 6, 14, 'S'); rect(9, 12, 9, 14, 'S')
        },
        ShopItem.FOX to sprite {
            rect(3, 8, 10, 11, 'e'); rect(10, 5, 13, 8, 'e'); set(10, 3, 'e'); set(10, 4, 'e'); set(13, 4, 'e')
            set(12, 6, 'k'); rect(13, 7, 14, 8, 'x'); rect(4, 11, 9, 11, 'x')
            rect(0, 6, 3, 8, 'e'); set(0, 6, 'x'); set(0, 7, 'x')
            rect(4, 12, 4, 14, 'O'); rect(9, 12, 9, 14, 'O')
        },
        ShopItem.CAT to sprite {
            rect(4, 8, 10, 12, 'R'); rect(9, 4, 13, 8, 'R'); set(9, 3, 'R'); set(13, 3, 'R')
            set(10, 5, 'c'); set(12, 5, 'c'); set(11, 7, 'o')
            rect(2, 5, 2, 10, 'R'); set(3, 10, 'R')
            rect(5, 13, 5, 14, 'R'); rect(9, 13, 9, 14, 'R'); rect(5, 9, 8, 10, 'r')
        },
        ShopItem.DRAGON to sprite {
            rect(4, 8, 10, 12, 'c'); rect(5, 10, 9, 12, 'h')
            rect(9, 4, 13, 8, 'c'); set(12, 5, 'e'); rect(13, 7, 14, 7, 'x')
            rect(2, 5, 5, 8, 'F'); set(3, 4, 'F')
            set(10, 3, 'y'); set(12, 3, 'y')
            rect(0, 11, 3, 11, 'c'); set(0, 10, 'c')
            rect(5, 13, 5, 14, 'F'); rect(9, 13, 9, 14, 'F')
            set(15, 6, 'n')
        },
    )
}

/** A pet sprite for the shop and the hero's side. */
fun petSprite(pet: ShopItem): List<String> = petArt.getValue(pet)
