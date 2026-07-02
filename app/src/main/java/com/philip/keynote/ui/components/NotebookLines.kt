package com.philip.keynote.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.notebookLines(
    lineHeight: Dp = 32.dp,
    lineColor: Color = Color.LightGray.copy(alpha = 0.5f),
    marginColor: Color = Color.Transparent
): Modifier = this.drawBehind {
    val lineHeightPx = lineHeight.toPx()
    val offsetPx = 8.dp.toPx()

    // Draw horizontal notebook lines matching text baseline alignment
    var y = lineHeightPx + offsetPx
    while (y < size.height) {
        drawLine(
            color = lineColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.dp.toPx()
        )
        y += lineHeightPx
    }
}
