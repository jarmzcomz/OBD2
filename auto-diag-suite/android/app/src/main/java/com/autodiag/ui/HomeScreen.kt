package com.autodiag.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autodiag.AutoDiagApplication
import com.autodiag.model.PidData
import com.autodiag.ui.components.DtcCard
import com.autodiag.ui.components.GaugeView
import com.autodiag.viewmodel.DashboardViewModel
import com.autodiag.viewmodel.DtcViewModel

private data class Tab(val label: String, val icon: @Composable () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onDisconnect: () -> Unit,
    dashboardVm: DashboardViewModel = viewModel(),
    dtcVm: DtcViewModel = viewModel()
) {
    LaunchedEffect(Unit) { dashboardVm.initialize() }

    var selectedTab by remember { mutableIntStateOf(0) }

    val pidData by dashboardVm.pidData.collectAsState()
    val isConnected by dashboardVm.isConnected.collectAsState()
    val dashError by dashboardVm.error.collectAsState()
    val dtcList by dtcVm.dtcs.collectAsState()

    val tabs = listOf(
        Tab("Tablero") { Icon(Icons.Default.Dashboard, contentDescription = null) },
        Tab("Sensores") { Icon(Icons.Default.Speed, contentDescription = null) },
        Tab("Fallas") { Icon(Icons.Default.Warning, contentDescription = null) },
        Tab("Info") { Icon(Icons.Default.Info, contentDescription = null) }
    )

    val titles = listOf("Tablero en vivo", "Sensores", "Códigos de falla", "Información")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(titles[selectedTab], color = Color(0xFFE5EAF2), fontWeight = FontWeight.Bold, fontSize = 19.sp)
                        Text(
                            text = if (isConnected) "● Conectado" else "○ Desconectado",
                            color = if (isConnected) Color(0xFF22C55E) else Color(0xFF7C8AA5),
                            fontSize = 12.sp
                        )
                    }
                },
                actions = {
                    if (selectedTab == 2 && dtcList.isNotEmpty()) {
                        androidx.compose.material3.IconButton(onClick = { dtcVm.clearDtcs() }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Borrar DTCs", tint = Color(0xFFEF4444))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF131C2E))
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF131C2E)) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = tab.icon,
                        label = { Text(tab.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF2563EB),
                            selectedTextColor = Color(0xFF2563EB),
                            indicatorColor = Color(0xFF1E3A5F),
                            unselectedIconColor = Color(0xFF7C8AA5),
                            unselectedTextColor = Color(0xFF7C8AA5)
                        )
                    )
                }
            }
        },
        containerColor = Color(0xFF0B1220)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                0 -> DashboardTab(pidData, dashError, onDisconnect)
                1 -> SensorsTab(pidData)
                2 -> FallasTab(dtcVm)
                3 -> InfoTab(onDisconnect)
            }
        }
    }
}

@Composable
private fun DashboardTab(
    pidData: Map<String, PidData>,
    error: String?,
    onDisconnect: () -> Unit
) {
    when {
        error != null -> CenteredMessage(
            title = "Error de conexión",
            titleColor = Color(0xFFEF4444),
            body = error
        )
        pidData.isEmpty() -> CenteredMessage(
            title = "Esperando datos del vehículo",
            titleColor = Color(0xFFE5EAF2),
            body = "Asegúrate de que el motor esté encendido o en contacto (ACC). " +
                "Si tu carro está apagado, la ECU no responde a las consultas OBD-II.",
            spinner = true
        )
        else -> LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 168.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(pidData.values.toList(), key = { it.code }) { pid ->
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.verticalGradient(listOf(Color(0xFF15203A), Color(0xFF0F1729))),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(1.dp, Color(0xFF1E2A3D), RoundedCornerShape(20.dp))
                        .padding(12.dp)
                ) {
                    GaugeView(
                        label = pid.name,
                        value = pid.value.toFloat(),
                        min = pid.min,
                        max = pid.max,
                        unit = pid.unit,
                        dangerThreshold = pid.dangerThreshold
                    )
                }
            }
        }
    }
}

@Composable
private fun SensorsTab(pidData: Map<String, PidData>) {
    if (pidData.isEmpty()) {
        CenteredMessage(
            title = "Sin lecturas todavía",
            titleColor = Color(0xFFE5EAF2),
            body = "Cuando lleguen datos del vehículo aparecerán aquí en forma de lista.",
            spinner = true
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(pidData.values.toList(), key = { it.code }) { pid ->
            val fraction = if (pid.max > pid.min)
                ((pid.value.toFloat() - pid.min) / (pid.max - pid.min)).coerceIn(0f, 1f) else 0f
            val barColor = when {
                fraction >= pid.dangerThreshold -> Color(0xFFEF4444)
                fraction >= pid.dangerThreshold * 0.75f -> Color(0xFFF59E0B)
                else -> Color(0xFF2563EB)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131C2E), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF1E2A3D), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(pid.name, color = Color(0xFFE5EAF2), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    // Mini barra de progreso del valor.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .background(Color(0xFF1E2A3D), RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .height(5.dp)
                                .background(barColor, RoundedCornerShape(3.dp))
                        )
                    }
                }
                Spacer(modifier = Modifier.size(16.dp))
                Column(horizontalAlignment = Alignment.End) {
                    val display = if (pid.value == pid.value.toLong().toDouble())
                        pid.value.toLong().toString() else "%.1f".format(pid.value)
                    Text(display, color = barColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(pid.unit, color = Color(0xFF7C8AA5), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun FallasTab(dtcVm: DtcViewModel) {
    LaunchedEffect(Unit) { dtcVm.initialize() }
    val dtcs by dtcVm.dtcs.collectAsState()
    val isLoading by dtcVm.isLoading.collectAsState()
    val error by dtcVm.error.collectAsState()

    when {
        isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF2563EB))
        }
        error != null -> CenteredMessage("Error al leer DTCs", Color(0xFFEF4444), error ?: "")
        dtcs.isEmpty() -> CenteredMessage(
            title = "✓ Sin códigos de falla",
            titleColor = Color(0xFF22C55E),
            body = "Tu vehículo está funcionando limpio."
        )
        else -> Column {
            Text(
                "${dtcs.size} código${if (dtcs.size != 1) "s" else ""} de falla detectado${if (dtcs.size != 1) "s" else ""}",
                color = Color(0xFF7C8AA5),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                items(dtcs, key = { it.code }) { dtc -> DtcCard(dtc = dtc) }
            }
        }
    }
}

@Composable
private fun InfoTab(onDisconnect: () -> Unit) {
    val context = LocalContext.current
    val label = (context.applicationContext as? AutoDiagApplication)?.connectionLabel ?: "—"

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        InfoRow("Conexión", label)
        InfoRow("App", "AutoDiag · OBD-II Diagnostic Suite")
        InfoRow("Protocolo", "ELM327 (modo 01 PIDs, modo 03 DTCs)")
        InfoRow("Transportes", "Bluetooth Clásico · Bluetooth LE · WiFi · Demo")

        Spacer(modifier = Modifier.weight(1f))

        androidx.compose.material3.Button(
            onClick = onDisconnect,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
        ) {
            Text("Desconectar", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun InfoRow(key: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF131C2E), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF1E2A3D), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(key, color = Color(0xFF7C8AA5), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = Color(0xFFE5EAF2), fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CenteredMessage(
    title: String,
    titleColor: Color,
    body: String,
    spinner: Boolean = false
) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (spinner) {
                CircularProgressIndicator(color = Color(0xFF2563EB), strokeWidth = 3.dp)
                Spacer(modifier = Modifier.height(20.dp))
            }
            Text(title, color = titleColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(body, color = Color(0xFF7C8AA5), fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}
