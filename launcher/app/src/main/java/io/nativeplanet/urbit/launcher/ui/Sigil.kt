package io.nativeplanet.urbit.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.nativeplanet.urbit.launcher.theme.LauncherTheme

/**
 * Sigil — 4-quadrant geometric ship symbol.
 * Seed is a 4-element list of ints [0..12], each indexing a glyph shape.
 * Deterministically derived from ship name for real use; hardcoded seeds for mock.
 */
@Composable
fun Sigil(
    seed: List<Int> = listOf(2, 4, 9, 1),
    size: Dp = 32.dp,
    bg: Color = LauncherTheme.tokens.sigilBg,
    fg: Color = LauncherTheme.tokens.sigilFg,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val half = w / 2f
        val cellW = half
        val cellH = h / 2f
        val pad = w * 0.08f

        // Background
        drawRect(color = bg, size = this.size)

        // Draw 4 quadrants
        val quadrants = listOf(
            Offset(0f, 0f),
            Offset(half, 0f),
            Offset(0f, cellH),
            Offset(half, cellH),
        )
        seed.take(4).forEachIndexed { i, glyphIdx ->
            drawGlyph(glyphIdx % 13, quadrants[i], cellW, cellH, pad, fg)
        }
    }
}

private fun DrawScope.drawGlyph(
    idx: Int,
    origin: Offset,
    w: Float,
    h: Float,
    pad: Float,
    color: Color
) {
    val cx = origin.x + w / 2
    val cy = origin.y + h / 2
    val iw = w - pad * 2
    val ih = h - pad * 2

    when (idx) {
        0 -> { // filled rect
            drawRect(color, Offset(origin.x + pad, origin.y + pad), Size(iw, ih))
        }
        1 -> { // circle
            drawCircle(color, radius = iw.coerceAtMost(ih) / 2f, center = Offset(cx, cy))
        }
        2 -> { // triangle pointing up
            val path = Path().apply {
                moveTo(cx, origin.y + pad)
                lineTo(origin.x + pad, origin.y + h - pad)
                lineTo(origin.x + w - pad, origin.y + h - pad)
                close()
            }
            drawPath(path, color)
        }
        3 -> { // triangle pointing down
            val path = Path().apply {
                moveTo(origin.x + pad, origin.y + pad)
                lineTo(origin.x + w - pad, origin.y + pad)
                lineTo(cx, origin.y + h - pad)
                close()
            }
            drawPath(path, color)
        }
        4 -> { // X shape (two diagonals)
            val stroke = iw * 0.18f
            drawLine(color, Offset(origin.x + pad, origin.y + pad), Offset(origin.x + w - pad, origin.y + h - pad), strokeWidth = stroke)
            drawLine(color, Offset(origin.x + w - pad, origin.y + pad), Offset(origin.x + pad, origin.y + h - pad), strokeWidth = stroke)
        }
        5 -> { // diamond
            val path = Path().apply {
                moveTo(cx, origin.y + pad)
                lineTo(origin.x + w - pad, cy)
                lineTo(cx, origin.y + h - pad)
                lineTo(origin.x + pad, cy)
                close()
            }
            drawPath(path, color)
        }
        6 -> { // wide rect (half height)
            drawRect(color, Offset(origin.x + pad, cy - ih / 4), Size(iw, ih / 2))
        }
        7 -> { // nested rect outline
            val stroke = iw * 0.12f
            drawRect(color, Offset(origin.x + pad, origin.y + pad), Size(iw, ih), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        }
        8 -> { // two horizontal lines
            val stroke = ih * 0.14f
            val y1 = origin.y + pad + ih * 0.33f
            val y2 = origin.y + pad + ih * 0.67f
            drawLine(color, Offset(origin.x + pad, y1), Offset(origin.x + w - pad, y1), strokeWidth = stroke)
            drawLine(color, Offset(origin.x + pad, y2), Offset(origin.x + w - pad, y2), strokeWidth = stroke)
        }
        9 -> { // two vertical lines
            val stroke = iw * 0.14f
            val x1 = origin.x + pad + iw * 0.33f
            val x2 = origin.x + pad + iw * 0.67f
            drawLine(color, Offset(x1, origin.y + pad), Offset(x1, origin.y + h - pad), strokeWidth = stroke)
            drawLine(color, Offset(x2, origin.y + pad), Offset(x2, origin.y + h - pad), strokeWidth = stroke)
        }
        10 -> { // circle outline
            val stroke = iw * 0.14f
            drawCircle(color, radius = iw.coerceAtMost(ih) / 2f - stroke / 2, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        }
        11 -> { // stepped square (quarter fill)
            drawRect(color, Offset(origin.x + pad, origin.y + pad), Size(iw / 2, ih / 2))
            drawRect(color, Offset(cx, cy), Size(iw / 2, ih / 2))
        }
        12 -> { // small centered dot
            drawCircle(color, radius = iw.coerceAtMost(ih) * 0.2f, center = Offset(cx, cy))
        }
    }
}

/** Generate a deterministic seed from a ship name */
fun seedFromShipName(name: String): List<Int> {
    val clean = name.removePrefix("~").replace("-", "")
    if (clean.isEmpty()) return listOf(0, 0, 0, 0)
    val hash = clean.fold(0) { acc, c -> acc * 31 + c.code }
    return listOf(
        (hash and 0xF) % 13,
        ((hash shr 4) and 0xF) % 13,
        ((hash shr 8) and 0xF) % 13,
        ((hash shr 12) and 0xF) % 13,
    )
}
