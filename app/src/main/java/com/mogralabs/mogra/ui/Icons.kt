package com.mogralabs.mogra.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The icon set, drawn rather than shipped: five glyphs on a 24-unit grid, one stroke weight,
 * so they recolour with the palette and stay sharp at any density.
 */
private inline fun DrawScope.onGrid(block: DrawScope.(Float) -> Unit) = block(size.width / 24f)

@Composable
fun WaveformIcon(color: Color, size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        onGrid { u ->
            val stroke = Stroke(width = 1.7f * u, cap = StrokeCap.Round)
            listOf(3f to 1.5f, 7.5f to 5.5f, 12f to 8.8f, 16.5f to 4.5f, 21f to 1f)
                .forEach { (x, half) ->
                    drawLine(
                        color = color,
                        start = Offset(x * u, (12f - half) * u),
                        end = Offset(x * u, (12f + half) * u),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round,
                    )
                }
        }
    }
}

@Composable
fun ContourIcon(color: Color, size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        onGrid { u ->
            val pts = listOf(3f to 16.5f, 7.5f to 11f, 12f to 14.2f, 16.5f to 7f, 21f to 9.5f)
            val path = Path().apply {
                moveTo(pts[0].first * u, pts[0].second * u)
                pts.drop(1).forEach { (x, y) -> lineTo(x * u, y * u) }
            }
            drawPath(path, color, style = Stroke(1.6f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
            pts.subList(1, 4).forEach { (x, y) -> drawCircle(color, 1.5f * u, Offset(x * u, y * u)) }
        }
    }
}

@Composable
fun SearchIcon(color: Color, size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        onGrid { u ->
            drawCircle(color, 6.5f * u, Offset(10.5f * u, 10.5f * u), style = Stroke(1.6f * u))
            drawLine(
                color, Offset(15.3f * u, 15.3f * u), Offset(21f * u, 21f * u),
                strokeWidth = 1.6f * u, cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun ChevronIcon(color: Color, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        onGrid { u ->
            val path = Path().apply {
                moveTo(9f * u, 5f * u); lineTo(16f * u, 12f * u); lineTo(9f * u, 19f * u)
            }
            drawPath(path, color, style = Stroke(2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun BackIcon(color: Color, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        onGrid { u ->
            val path = Path().apply {
                moveTo(15f * u, 5f * u); lineTo(8f * u, 12f * u); lineTo(15f * u, 19f * u)
            }
            drawPath(path, color, style = Stroke(1.9f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
