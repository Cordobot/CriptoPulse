package com.cordobot.criptopulse.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cordobot.criptopulse.presentation.theme.Down
import com.cordobot.criptopulse.presentation.theme.Up
import com.cordobot.criptopulse.presentation.util.formatChange

fun trendColor(change: Double?): Color = if ((change ?: 0.0) >= 0) Up else Down

/** Line chart of [points]; with [fill] the area under the line gets a fading gradient. */
@Composable
fun Sparkline(
    points: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
    fill: Boolean = false,
    strokeWidth: Float = 3f
) {
    if (points.size < 2) return
    Canvas(modifier = modifier) {
        val min = points.min()
        val max = points.max()
        val range = (max - min).takeIf { it > 0 } ?: 1.0
        val stepX = size.width / (points.size - 1)
        fun point(index: Int) = Offset(
            x = index * stepX,
            y = (size.height * (1 - (points[index] - min) / range)).toFloat()
        )

        val line = Path().apply {
            moveTo(point(0).x, point(0).y)
            for (i in 1 until points.size) lineTo(point(i).x, point(i).y)
        }
        if (fill) {
            val area = Path().apply {
                addPath(line)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent)))
        }
        drawPath(line, color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun ChangeChip(change: Double?, modifier: Modifier = Modifier, fontSize: TextUnit = 13.sp) {
    val color = trendColor(change)
    Text(
        text = formatChange(change),
        color = color,
        fontSize = fontSize,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
