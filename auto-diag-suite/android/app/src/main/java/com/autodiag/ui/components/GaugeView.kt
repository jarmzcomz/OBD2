package com.autodiag.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ColorNormal = Color(0xFF2563EB)
private val ColorDanger = Color(0xFFEF4444)
private val ColorTrack  = Color(0xFF1E2D45)

@Composable
fun GaugeView(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    unit: String,
    modifier: Modifier = Modifier,
    dangerThreshold: Float = 0.85f,
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.coerceIn(min, max),
        animationSpec = tween(400),
        label = "gauge_$label"
    )
    val fraction = if (max > min) (animatedValue - min) / (max - min) else 0f
    val color = if (fraction >= dangerThreshold) ColorDanger else ColorNormal
    val sweepAngle = 240f * fraction

    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                val inset = 10.dp.toPx()
                val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                val topLeft = Offset(inset, inset)
                // track
                drawArc(ColorTrack, 150f, 240f, false, topLeft, arcSize, style = stroke)
                // value arc
                if (sweepAngle > 0f)
                    drawArc(color, 150f, sweepAngle, false, topLeft, arcSize, style = stroke)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (value.isNaN()) "--" else "%.0f".format(animatedValue),
                    fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(text = unit, fontSize = 11.sp, color = Color(0xFF7C8AA5))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(text = label, fontSize = 12.sp, color = Color(0xFFE5EAF2))
    }
}
