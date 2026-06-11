package com.autodiag.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GaugeView(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    unit: String,
    modifier: Modifier = Modifier,
    dangerThreshold: Float = 0.85f
) {
    val fraction = if (max > min) ((value - min) / (max - min)).coerceIn(0f, 1f) else 0f

    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 400),
        label = "gauge_animation"
    )

    val safeColor = Color(0xFF2563EB)    // blue
    val warnColor = Color(0xFFF59E0B)    // amber
    val dangerColor = Color(0xFFEF4444) // red
    val trackColor = Color(0xFF1E2A3D)

    val gaugeColor = when {
        animatedFraction >= dangerThreshold -> dangerColor
        animatedFraction >= dangerThreshold * 0.75f -> warnColor
        else -> safeColor
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = size.width * 0.08f
                val startAngle = 150f
                val sweepRange = 240f
                val padding = strokeWidth / 2
                val arcSize = Size(
                    width = size.width - padding * 2,
                    height = size.height - padding * 2
                )
                val topLeft = Offset(padding, padding)

                // Track (background arc)
                drawArc(
                    color = trackColor,
                    startAngle = startAngle,
                    sweepAngle = sweepRange,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Value arc
                if (animatedFraction > 0f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            0f to safeColor,
                            dangerThreshold to warnColor,
                            1f to dangerColor,
                            center = Offset(size.width / 2, size.height / 2)
                        ),
                        startAngle = startAngle,
                        sweepAngle = sweepRange * animatedFraction,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            // Center text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (value == value.toLong().toFloat()) {
                        value.toLong().toString()
                    } else {
                        "%.1f".format(value)
                    },
                    color = gaugeColor,
                    fontSize = (32 * (1f - label.length * 0.01f)).coerceIn(20f, 36f).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = unit,
                    color = Color(0xFF7C8AA5),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Text(
            text = label,
            color = Color(0xFFE5EAF2),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }
}
