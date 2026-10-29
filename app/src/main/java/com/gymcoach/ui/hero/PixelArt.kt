package com.gymcoach.ui.hero

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import com.gymcoach.R
import com.gymcoach.game.Badge
import com.gymcoach.game.Rank

val PixelFont = FontFamily(Font(R.font.press_start_2p))

object Retro {
    val night = Color(0xFF1B1B2F)
    val panel = Color(0xFF2A2A4A)
    val gold = Color(0xFFE8B923)
    val parchment = Color(0xFFF3E3C3)
    val ember = Color(0xFFFF7A1A)
    val xp = Color(0xFF4CD964)
    val dim = Color(0xFF6B6B8A)
    val deep = Color(0xFF12121F)
    val hp = Color(0xFFE04040)
    val sea = Color(0xFF3FC1E0)
    val stone = Color(0xFFB0B8C8)
}

/** Paints a character grid as square pixels of [px] each, starting at [topLeft]. */
fun DrawScope.drawPixels(rows: List<String>, palette: Map<Char, Color>, topLeft: Offset, px: Float) {
    rows.forEachIndexed { y, row ->
        row.forEachIndexed { x, c ->
            val color = palette[c] ?: return@forEachIndexed
            drawRect(color, Offset(topLeft.x + x * px, topLeft.y + y * px), Size(px + 0.5f, px + 0.5f))
        }
    }
}

/** Draws a grid of characters as square pixels; '.' is transparent. */
@Composable
fun PixelSprite(rows: List<String>, palette: Map<Char, Color>, size: Dp, modifier: Modifier = Modifier) {
    val height = rows.size
    val width = rows.maxOf { it.length }
    Canvas(modifier.size(size)) {
        val px = minOf(this.size.width / width, this.size.height / height)
        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, c ->
                val color = palette[c] ?: return@forEachIndexed
                // Slight overlap avoids hairline gaps between pixels.
                drawRect(color, Offset(x * px, y * px), Size(px + 0.5f, px + 0.5f))
            }
        }
    }
}

// Hero sprite, 16 × 16. k outline, s skin, e eye, h hair, t torso, b belt, l legs, f feet.
private val baseHero = listOf(
    "................",
    ".....kkkkkk.....",
    "....khhhhhhk....",
    "....khsssshk....",
    "....ksessesk....",
    "....kssssssk....",
    ".....kssssk.....",
    "...kkttttttkk...",
    "..ksttttttttsk..",
    "..ksttttttttsk..",
    "...kbbbbbbbbk...",
    "...kttttttttk...",
    "....kllllllk....",
    "....kllkkllk....",
    "....kll..llk....",
    "...kffk..kffk...",
)

private val capLayer = listOf(
    "................",
    ".....kkkkkk.....",
    "....kcccccck....",
)

private val helmetLayer = listOf(
    "................",
    ".....kkkkkk.....",
    "....kmmmmmmk....",
    "....kmmmmmmk....",
    "....kmkmmkmk....",
    "....kmmmmmmk....",
)

private val plumeLayer = listOf(".......pp.......")

private val crownLayer = listOf(
    ".....g.gg.g.....",
    ".....gggggg.....",
)

private val swordLayer = listOf(
    "................",
    "................",
    "................",
    "..............w.",
    "..............w.",
    "..............w.",
    "..............w.",
    "..............w.",
    "..............w.",
    ".............ggg",
    "..............r.",
)

private val shieldLayer = listOf(
    "", "", "", "", "", "", "",
    "kkkk",
    "kdxk",
    "kxxk",
    "kdxk",
    ".kk.",
)

private data class Look(
    val torso: Color,
    val legs: Color,
    val belt: Color = Color(0xFF4A2F1B),
    val cap: Color? = null,
    val helmet: Color? = null,
    val plume: Color? = null,
    val crown: Boolean = false,
    val blade: Color? = null,
    val guard: Color = Color(0xFF8B5A2B),
    val shield: Color? = null,
    val crest: Color = Retro.gold,
    val cape: Color? = null,
)

private fun lookFor(rank: Rank): Look = when (rank) {
    Rank.PEASANT -> Look(torso = Color(0xFF8B7355), legs = Color(0xFF5C4A3A))
    Rank.SQUIRE -> Look(Color(0xFF4F7CAC), Color(0xFF5C4A3A), cap = Color(0xFF8B5A2B), blade = Color(0xFF9C6B3C))
    Rank.FOOTMAN -> Look(Color(0xFF8B5A2B), Color(0xFF4A3B2A), cap = Color(0xFF6B4423), blade = Color(0xFFC0C8D0))
    Rank.MAN_AT_ARMS -> Look(
        Color(0xFF9AA3AD), Color(0xFF5A6570), cap = Color(0xFF9AA3AD), blade = Color(0xFFC0C8D0),
        shield = Color(0xFF8B5A2B), crest = Color(0xFFB03030),
    )
    Rank.KNIGHT -> Look(
        Color(0xFFD5DCE3), Color(0xFFB0B8C0), helmet = Color(0xFFD5DCE3), blade = Color(0xFFE8EEF4),
        guard = Retro.gold, shield = Color(0xFF2E5A9C),
    )
    Rank.KNIGHT_ERRANT -> Look(
        Color(0xFFD5DCE3), Color(0xFFB0B8C0), helmet = Color(0xFFD5DCE3), plume = Color(0xFF2E5A9C),
        blade = Color(0xFFE8EEF4), guard = Retro.gold, shield = Color(0xFF2E5A9C), cape = Color(0xFF2E5A9C),
    )
    Rank.PALADIN -> Look(
        Color(0xFFE6ECF2), Color(0xFFB0B8C0), helmet = Color(0xFFE6ECF2), plume = Color(0xFFB03030),
        blade = Color(0xFFFFFFFF), guard = Retro.gold, shield = Color(0xFFF3E3C3), crest = Color(0xFFB03030),
        cape = Color(0xFFB03030),
    )
    Rank.CHAMPION -> Look(
        Color(0xFFE6ECF2), Color(0xFFB0B8C0), belt = Retro.gold, helmet = Color(0xFFE6ECF2), plume = Retro.gold,
        blade = Color(0xFFFFFFFF), guard = Retro.gold, shield = Color(0xFFB03030), cape = Color(0xFFB03030),
    )
    Rank.WARLORD -> Look(
        Color(0xFF5B6270), Color(0xFF3E4450), belt = Retro.gold, helmet = Color(0xFF5B6270), plume = Color(0xFFB03030),
        blade = Color(0xFFE8EEF4), guard = Retro.gold, shield = Color(0xFF3E4450), crest = Color(0xFFB03030),
        cape = Color(0xFF6A3D9A),
    )
    Rank.LEGEND -> Look(
        Retro.gold, Color(0xFFC9981B), belt = Color(0xFFB03030), helmet = Retro.gold, crown = true,
        blade = Color(0xFFFFFFFF), guard = Color(0xFFB03030), shield = Color(0xFF6A3D9A), cape = Color(0xFF6A3D9A),
    )
}

private fun overlay(grid: Array<CharArray>, layer: List<String>, onlyEmpty: Boolean = false) {
    layer.forEachIndexed { y, row ->
        row.forEachIndexed { x, c ->
            if (c != '.' && y < grid.size && x < grid[y].size && (!onlyEmpty || grid[y][x] == '.')) grid[y][x] = c
        }
    }
}

/** The hero for a rank: gear is added as you rank up. */
@Composable
fun HeroSprite(rank: Rank, size: Dp, modifier: Modifier = Modifier) {
    val look = lookFor(rank)
    val grid = Array(16) { y -> baseHero[y].toCharArray() }
    when {
        look.helmet != null -> overlay(grid, helmetLayer)
        look.cap != null -> overlay(grid, capLayer)
    }
    if (look.plume != null) overlay(grid, plumeLayer)
    if (look.crown) overlay(grid, crownLayer)
    if (look.blade != null) overlay(grid, swordLayer.map { it.replace('g', 'q') })
    if (look.shield != null) overlay(grid, shieldLayer)
    if (look.cape != null) {
        // Cape shows behind the body: fill empty pixels around the torso and legs.
        val cape = (0 until 16).map { y ->
            if (y in 8..14) "...pppppppppp..." else "................"
        }
        overlay(grid, cape.map { it.replace('p', 'v') }, onlyEmpty = true)
    }
    val palette = mapOf(
        'k' to Retro.night,
        's' to Color(0xFFF1C27D),
        'e' to Retro.night,
        'h' to Color(0xFF6B3E26),
        't' to look.torso,
        'b' to look.belt,
        'l' to look.legs,
        'f' to Color(0xFF3B2A1E),
        'c' to (look.cap ?: Color.Transparent),
        'm' to (look.helmet ?: Color.Transparent),
        'p' to (look.plume ?: Color.Transparent),
        'g' to Retro.gold,
        'w' to (look.blade ?: Color.Transparent),
        'q' to look.guard,
        'r' to Color(0xFF6B4423),
        'd' to (look.shield ?: Color.Transparent),
        'x' to look.crest,
        'v' to (look.cape ?: Color.Transparent),
    )
    PixelSprite(grid.map { String(it) }, palette, size, modifier)
}

private val badgeArt: Map<Badge, List<String>> = mapOf(
    Badge.SWORD to listOf(
        "......##", ".....###", "....###.", "+..###..", ".+###...", "..++....", ".+..+...", "+.......",
    ),
    Badge.FLAME to listOf(
        "...#....", "...##...", "..###.#.", "..####..", ".######.", ".##++##.", ".#++++#.", "..####..",
    ),
    Badge.SHIELD to listOf(
        ".######.", ".#++++#.", ".#+##+#.", ".#+##+#.", ".#++++#.", "..#++#..", "...##...", "........",
    ),
    Badge.STAR to listOf(
        "...##...", "...##...", "########", ".##++##.", "..####..", ".##..##.", ".#....#.", "........",
    ),
    Badge.CROWN to listOf(
        "........", "#..##..#", "##.##.##", "########", "#++##++#", "########", "........", "........",
    ),
    Badge.BOOT to listOf(
        "..###...", "..#+#...", "..#+#...", "..#+#...", "..#++##.", "..#++++#", "..######", "........",
    ),
    Badge.WAVE to listOf(
        "........", "..##....", ".#..#..#", "#....##.", "........", "..++....", ".+..+..+", "+....++.",
    ),
    Badge.CUP to listOf(
        "########", "#+####+#", ".######.", "..####..", "...##...", "...##...", "..####..", ".######.",
    ),
    Badge.SCROLL to listOf(
        ".######.", "#+####+#", ".#++++#.", ".#+##+#.", ".#++++#.", ".#+##+#.", "#+####+#", ".######.",
    ),
    Badge.CHEST to listOf(
        "........", ".######.", "#++++++#", "########", "#++##++#", "#++++++#", "########", "........",
    ),
    Badge.DRAGON to listOf(
        "#.....#.", "##...##.", ".#####..", "##+####.", "#######.", ".###..##", ".##.....", "##......",
    ),
)

private val badgeColors: Map<Badge, Pair<Color, Color>> = mapOf(
    Badge.SWORD to (Color(0xFFC0C8D0) to Color(0xFF8B5A2B)),
    Badge.FLAME to (Retro.ember to Color(0xFFFFD84A)),
    Badge.SHIELD to (Color(0xFF2E5A9C) to Retro.gold),
    Badge.STAR to (Retro.gold to Color(0xFFFFF2A8)),
    Badge.CROWN to (Retro.gold to Color(0xFFB03030)),
    Badge.BOOT to (Color(0xFF8B5A2B) to Color(0xFFD2A26B)),
    Badge.WAVE to (Color(0xFF3FC1E0) to Color(0xFF2E5A9C)),
    Badge.CUP to (Retro.gold to Color(0xFFFFF2A8)),
    Badge.SCROLL to (Retro.parchment to Color(0xFF8B5A2B)),
    Badge.DRAGON to (Color(0xFF3FA34D) to Color(0xFFB03030)),
    Badge.CHEST to (Color(0xFF8B5A2B) to Retro.gold),
)

@Composable
fun PixelBadge(badge: Badge, unlocked: Boolean, size: Dp, modifier: Modifier = Modifier) {
    val (main, accent) = badgeColors.getValue(badge)
    val palette = if (unlocked) mapOf('#' to main, '+' to accent) else mapOf('#' to Retro.dim, '+' to Retro.panel)
    PixelSprite(badgeArt.getValue(badge), palette, size, modifier)
}

/** Icons for the bottom bar, drawn in one color with a darker accent. */
enum class PixelIcon { HOME, DUMBBELL, SHIELD, CUP, SCROLL }

private val iconArt: Map<PixelIcon, List<String>> = mapOf(
    PixelIcon.HOME to listOf(
        "...##...", "..####..", ".######.", "########", ".#+##+#.", ".#+##+#.", ".##++##.", ".##++##.",
    ),
    PixelIcon.DUMBBELL to listOf(
        "........", "#......#", "##....##", "########", "########", "##....##", "#......#", "........",
    ),
    PixelIcon.SHIELD to badgeArt.getValue(Badge.SHIELD),
    PixelIcon.CUP to badgeArt.getValue(Badge.CUP),
    PixelIcon.SCROLL to badgeArt.getValue(Badge.SCROLL),
)

@Composable
fun PixelIconImage(icon: PixelIcon, color: Color, size: Dp, modifier: Modifier = Modifier) {
    PixelSprite(iconArt.getValue(icon), mapOf('#' to color, '+' to Retro.night), size, modifier)
}
