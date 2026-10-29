package com.gymcoach.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gymcoach.ui.hero.Retro
import com.gymcoach.ui.hero.drawPixels

// Material buttons are always pill-shaped, so these give them square corners and a pixel border.

private val Square = RoundedCornerShape(0)

@Composable
fun RetroButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = Square,
        border = BorderStroke(2.dp, if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outlineVariant),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        content = content,
    )
}

@Composable
fun RetroOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = Square,
        border = BorderStroke(2.dp, if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        content = content,
    )
}

@Composable
fun RetroCard(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = Square,
        colors = colors,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(0.dp),
        content = content,
    )
}

@Composable
fun RetroCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = Square,
        colors = colors,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(0.dp),
        content = content,
    )
}

private val CheckMark = listOf(
    "......##",
    ".....##.",
    "##..##..",
    ".####...",
    "..##....",
)

/** A square pixel checkbox: gold frame, green tick. [faded] is for boxes the log ticked for you. */
@Composable
fun RetroCheckbox(checked: Boolean, modifier: Modifier = Modifier, size: Dp = 22.dp, faded: Boolean = false) {
    Canvas(modifier.size(size)) {
        val border = 2.dp.toPx()
        drawRect(Retro.deep)
        drawRect(
            if (checked) Retro.xp else Retro.gold,
            topLeft = Offset(border / 2, border / 2),
            size = Size(this.size.width - border, this.size.height - border),
            style = Stroke(border),
        )
        if (checked) {
            val px = (this.size.width - border * 4) / 8f
            drawPixels(
                CheckMark,
                mapOf('#' to if (faded) Retro.xp.copy(alpha = 0.6f) else Retro.xp),
                Offset(border * 2, (this.size.height - px * CheckMark.size) / 2),
                px,
            )
        }
    }
}
