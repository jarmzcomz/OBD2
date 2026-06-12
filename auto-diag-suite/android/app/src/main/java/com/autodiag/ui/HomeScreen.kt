package com.autodiag.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autodiag.AutoDiagApplication
import com.autodiag.model.PidData
import com.autodiag.ui.components.DtcCard
import com.autodiag.ui.components.GaugeView
import com.autodiag.ui.components.LineChart
import com.autodiag.viewmodel.AdvancedViewModel
import com.autodiag.viewmodel.DashboardViewModel
import com.autodiag.viewmodel.DiagnosticsViewModel
import com.autodiag.viewmodel.DtcViewModel
import com.autodiag.viewmodel.TerminalViewModel

private data class Tab(val label: String, val icon: @Composable () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onDisconnect: () -> Unit,
    dashboardVm: DashboardViewModel = viewModel(),
    dtcVm: DtcViewModel = viewModel(),
    diagVm: DiagnosticsViewModel = viewModel(),
    terminalVm: TerminalViewModel = viewModel(),
    advancedVm: AdvancedViewModel = viewModel()
) {
    LaunchedEffect(Unit) { dashboardVm.initialize() }

    var selectedTab by remember { mutableIntStateOf(0) }

    val pidData by dashboardVm.pidData.collectAsState()
    val history by dashboardVm.history.collectAsState()
    val isConnected by dashboardVm.isConnected.collectAsState()
    val dashError by dashboardVm.error.collectAsState()
    val dtcList by dtcVm.dtcs.collectAsState()

    val tabs = listOf(
        Tab("Tablero") { Icon(Icons.Default.Dashboard, contentDescription = null) },
        Tab("Sensores") { Icon(Icons.Default.Speed, contentDescription = null) },
        Tab("Gráficas") { Icon(Icons.Default.ShowChart, contentDescription = null) },
        Tab("Fallas") { Icon(Icons.Default.Warning, contentDescription = null) },
        Tab("Más") { Icon(Icons.Default.Terminal, contentDescription = null) }
    )
    val titles = listOf("Tablero en vivo", "Sensores", "Gráficas en vivo", "Códigos de falla", "Herramientas")

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
                    if (selectedTab == 3 && dtcList.isNotEmpty()) {
                        IconButton(onClick = { dtcVm.clearDtcs() }) {
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
                        label = { Text(tab.label, fontSize = 10.sp) },
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
                0 -> DashboardTab(pidData, dashError)
                1 -> SensorsTab(pidData)
                2 -> GraphsTab(pidData, history)
                3 -> FallasTab(dtcVm)
                4 -> MoreTab(diagVm, terminalVm, advancedVm, history, onDisconnect)
            }
        }
    }
}

@Composable
private fun DashboardTab(pidData: Map<String, PidData>, error: String?) {
    when {
        error != null -> CenteredMessage("Error de conexión", Color(0xFFEF4444), error)
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
        CenteredMessage("Sin lecturas todavía", Color(0xFFE5EAF2),
            "Cuando lleguen datos del vehículo aparecerán aquí en forma de lista.", spinner = true)
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(pidData.values.toList(), key = { it.code }) { pid -> SensorRow(pid) }
    }
}

@Composable
private fun SensorRow(pid: PidData) {
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
            Box(
                modifier = Modifier.fillMaxWidth().height(5.dp)
                    .background(Color(0xFF1E2A3D), RoundedCornerShape(3.dp))
            ) {
                Box(modifier = Modifier.fillMaxWidth(fraction).height(5.dp)
                    .background(barColor, RoundedCornerShape(3.dp)))
            }
        }
        Spacer(modifier = Modifier.size(16.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(formatValue(pid.value), color = barColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(pid.unit, color = Color(0xFF7C8AA5), fontSize = 11.sp)
        }
    }
}

@Composable
private fun GraphsTab(pidData: Map<String, PidData>, history: Map<String, List<Float>>) {
    if (pidData.isEmpty()) {
        CenteredMessage("Sin datos para graficar", Color(0xFFE5EAF2),
            "Las gráficas se construyen con los datos en vivo conforme llegan.", spinner = true)
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(pidData.values.toList(), key = { it.code }) { pid ->
            val series = history[pid.code] ?: emptyList()
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFF131C2E), RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFF1E2A3D), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(pid.name, color = Color(0xFFE5EAF2), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("${formatValue(pid.value)} ${pid.unit}", color = Color(0xFF2563EB), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LineChart(values = series, min = pid.min, max = pid.max)
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
        isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Color(0xFF2563EB)) }
        error != null -> CenteredMessage("Error al leer DTCs", Color(0xFFEF4444), error ?: "")
        dtcs.isEmpty() -> CenteredMessage("✓ Sin códigos de falla", Color(0xFF22C55E), "Tu vehículo está funcionando limpio.")
        else -> Column {
            Text(
                "${dtcs.size} código${if (dtcs.size != 1) "s" else ""} almacenado${if (dtcs.size != 1) "s" else ""}",
                color = Color(0xFF7C8AA5), fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                items(dtcs, key = { it.code }) { dtc -> DtcCard(dtc = dtc) }
            }
        }
    }
}

@Composable
private fun MoreTab(
    diagVm: DiagnosticsViewModel,
    terminalVm: TerminalViewModel,
    advancedVm: AdvancedViewModel,
    history: Map<String, List<Float>>,
    onDisconnect: () -> Unit
) {
    LaunchedEffect(Unit) { diagVm.refresh() }
    val info by diagVm.info.collectAsState()
    val loading by diagVm.loading.collectAsState()
    val context = LocalContext.current
    val label = (context.applicationContext as? AutoDiagApplication)?.connectionLabel ?: "—"

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- Información del vehículo / adaptador ---
        SectionTitle("Información del vehículo")
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Lectura VIN, protocolo, voltaje y monitores", color = Color(0xFF7C8AA5), fontSize = 12.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { diagVm.refresh() }) {
                if (loading) CircularProgressIndicator(color = Color(0xFF2563EB), strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                else Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = Color(0xFF2563EB))
            }
        }
        InfoRow("Conexión", label)
        InfoRow("VIN", info.vin ?: "—")
        InfoRow("Calibración", info.calId ?: "—")
        InfoRow("Adaptador", info.elmVersion ?: "—")
        InfoRow("Protocolo", info.protocol ?: "—")
        InfoRow("Voltaje batería", info.voltage ?: "—")
        if (info.pendingDtcs.isNotEmpty()) InfoRow("DTC pendientes", info.pendingDtcs.joinToString { it.code })
        if (info.permanentDtcs.isNotEmpty()) InfoRow("DTC permanentes", info.permanentDtcs.joinToString { it.code })

        Spacer(modifier = Modifier.height(8.dp))

        // --- Exportar datos ---
        SectionTitle("Registro de datos")
        Button(
            onClick = {
                val file = com.autodiag.util.CsvExporter.export(context, history)
                if (file != null) com.autodiag.util.CsvExporter.share(context, file)
            },
            enabled = history.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A), disabledContainerColor = Color(0xFF1E2A3D))
        ) {
            Text(
                if (history.isEmpty()) "Sin datos para exportar" else "Exportar sesión a CSV",
                color = Color.White, fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- Terminal de ingeniero ---
        SectionTitle("Terminal AT / OBD")
        TerminalPanel(terminalVm)

        // --- Panel avanzado (solo en modo WiFi/backend) ---
        if (advancedVm.available) {
            Spacer(modifier = Modifier.height(8.dp))
            SectionTitle("Panel avanzado (UDS / CAN / VAG)")
            AdvancedPanel(advancedVm)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onDisconnect,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
        ) {
            Text("Desconectar", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun TerminalPanel(vm: TerminalViewModel) {
    val lines by vm.lines.collectAsState()
    val busy by vm.busy.collectAsState()
    var cmd by remember { mutableStateOf("") }

    if (!vm.supported) {
        Text(
            "El terminal no está disponible en este modo de conexión.",
            color = Color(0xFF7C8AA5), fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().background(Color(0xFF131C2E), RoundedCornerShape(10.dp)).padding(14.dp)
        )
        return
    }

    // Comandos rápidos comunes.
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("ATI", "ATRV", "0100", "0902").forEach { q ->
            Box(
                modifier = Modifier.weight(1f)
                    .background(Color(0xFF1E3A5F), RoundedCornerShape(8.dp))
                    .clickable { vm.send(q) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) { Text(q, color = Color(0xFF93B4E8), fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Log de respuestas.
    Column(
        modifier = Modifier.fillMaxWidth().height(180.dp)
            .background(Color(0xFF0A0F1A), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF1E2A3D), RoundedCornerShape(10.dp))
            .verticalScroll(rememberScrollState()).padding(12.dp)
    ) {
        if (lines.isEmpty()) {
            Text("Envía un comando para ver la respuesta…", color = Color(0xFF55627A), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        lines.forEach { line ->
            Text("> ${line.command}", color = Color(0xFF22C55E), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Text(line.response, color = Color(0xFFC8D2E0), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(6.dp))
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = cmd,
            onValueChange = { cmd = it },
            placeholder = { Text("Comando (ej. 010C)", color = Color(0xFF55627A)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2563EB),
                unfocusedBorderColor = Color(0xFF1E2A3D),
                focusedTextColor = Color(0xFFE5EAF2),
                unfocusedTextColor = Color(0xFFE5EAF2),
                cursorColor = Color(0xFF2563EB),
                focusedContainerColor = Color(0xFF131C2E),
                unfocusedContainerColor = Color(0xFF131C2E)
            ),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.size(8.dp))
        IconButton(
            onClick = { vm.send(cmd); cmd = "" },
            enabled = !busy
        ) {
            Icon(Icons.Default.Send, contentDescription = "Enviar", tint = if (busy) Color(0xFF55627A) else Color(0xFF2563EB))
        }
    }
}

@Composable
private fun AdvancedPanel(vm: AdvancedViewModel) {
    val output by vm.output.collectAsState()
    val busy by vm.busy.collectAsState()
    var ecu by remember { mutableStateOf("0x7E0") }
    var did by remember { mutableStateOf("0xF190") }

    Text(
        "Operaciones servidas por el backend Python con Zero-Brick. Las escrituras " +
            "exigen backup verificado en el servidor.",
        color = Color(0xFF7C8AA5), fontSize = 11.sp
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        AdvChip("CAN", Modifier.weight(1f)) { vm.loadCanFrames() }
        AdvChip("VAG", Modifier.weight(1f)) { vm.loadVagModules() }
        AdvChip("Estado", Modifier.weight(1f)) { vm.loadHealth() }
    }
    Spacer(modifier = Modifier.height(8.dp))

    // Lectura UDS por DID (solo lectura).
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = ecu, onValueChange = { ecu = it },
            label = { Text("ECU", color = Color(0xFF7C8AA5), fontSize = 11.sp) },
            singleLine = true, modifier = Modifier.weight(1f),
            colors = monoFieldColors()
        )
        Spacer(modifier = Modifier.size(8.dp))
        OutlinedTextField(
            value = did, onValueChange = { did = it },
            label = { Text("DID", color = Color(0xFF7C8AA5), fontSize = 11.sp) },
            singleLine = true, modifier = Modifier.weight(1f),
            colors = monoFieldColors()
        )
        Spacer(modifier = Modifier.size(8.dp))
        IconButton(onClick = { vm.udsRead(ecu, did) }, enabled = !busy) {
            Icon(Icons.Default.Send, contentDescription = "Leer UDS", tint = if (busy) Color(0xFF55627A) else Color(0xFF2563EB))
        }
    }
    Spacer(modifier = Modifier.height(8.dp))

    Column(
        modifier = Modifier.fillMaxWidth().height(180.dp)
            .background(Color(0xFF0A0F1A), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF1E2A3D), RoundedCornerShape(10.dp))
            .verticalScroll(rememberScrollState()).padding(12.dp)
    ) {
        Text(output, color = Color(0xFFC8D2E0), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun AdvChip(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .background(Color(0xFF1E3A5F), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, color = Color(0xFF93B4E8), fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun monoFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF2563EB),
    unfocusedBorderColor = Color(0xFF1E2A3D),
    focusedTextColor = Color(0xFFE5EAF2),
    unfocusedTextColor = Color(0xFFE5EAF2),
    cursorColor = Color(0xFF2563EB),
    focusedContainerColor = Color(0xFF131C2E),
    unfocusedContainerColor = Color(0xFF131C2E)
)

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Color(0xFFE5EAF2), fontSize = 16.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun InfoRow(key: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(Color(0xFF131C2E), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF1E2A3D), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(key, color = Color(0xFF7C8AA5), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = Color(0xFFE5EAF2), fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun CenteredMessage(title: String, titleColor: Color, body: String, spinner: Boolean = false) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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

private fun formatValue(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else "%.1f".format(value)
