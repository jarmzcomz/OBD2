package com.autodiag.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Gráfica de línea simple basada en Compose Canvas (sin librerías externas).
 * Dibuja la serie [values] escalada entre [min] y [max].
 */
@Composable
fun LineChart(
    values: List<Float>,
    min: Float,
    max: Float,
    color: Color = Color(0xFF2563EB),
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        if (values.size < 2) return@Canvas
        val range = (max - min).takeIf { it != 0f } ?: 1f
        val stepX = size.width / (values.size - 1)
        val gridColor = Color(0xFF1E2A3D)

        // Línea base inferior.
        drawLine(
            color = gridColor,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1f
        )

        val path = Path()
        values.forEachIndexed { i, v ->
            val fraction = ((v - min) / range).coerceIn(0f, 1f)
            val x = i * stepX
            val y = size.height - fraction * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )

        // Punto final destacado.
        val last = values.last()
        val lf = ((last - min) / range).coerceIn(0f, 1f)
        drawCircle(
            color = color,
            radius = 4f,
            center = Offset(size.width, size.height - lf * size.height)
        )
    }
}
