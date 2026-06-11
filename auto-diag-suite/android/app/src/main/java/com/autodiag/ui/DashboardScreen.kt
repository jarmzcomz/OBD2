package com.autodiag.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.autodiag.ui.components.GaugeView
import com.autodiag.viewmodel.DashboardViewModel

private data class GaugeDef(val key: String, val label: String, val min: Float, val max: Float, val unit: String, val danger: Float = 0.85f)

private val GAUGES = listOf(
    GaugeDef("rpm",          "RPM",       0f, 6000f, "rpm",  0.80f),
    GaugeDef("speed",        "Velocidad", 0f, 220f,  "km/h"),
    GaugeDef("coolant_temp", "Temp. Ref", 0f, 120f,  "°C",   0.83f),
    GaugeDef("engine_load",  "Carga",     0f, 100f,  "%"),
    GaugeDef("throttle",     "Mariposa",  0f, 100f,  "%"),
    GaugeDef("intake_temp",  "T. Adm.",   0f, 80f,   "°C"),
)

@Composable
fun DashboardScreen(viewModel: DashboardViewModel, modifier: Modifier = Modifier) {
    val pids by viewModel.pids.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(modifier = modifier.padding(8.dp)) {
        error?.let { Text(text = "⚠ $it", color = Color(0xFFEF4444), modifier = Modifier.padding(8.dp)) }
        LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(4.dp)) {
            items(GAUGES.size) { i ->
                val g = GAUGES[i]
                GaugeView(
                    label = g.label,
                    value = pids[g.key]?.toFloat() ?: 0f,
                    min = g.min, max = g.max, unit = g.unit,
                    dangerThreshold = g.danger,
                )
            }
        }
    }
}
