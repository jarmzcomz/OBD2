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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
import com.autodiag.viewmodel.CodingViewModel
import com.autodiag.viewmodel.CodingStep
import com.autodiag.viewmodel.DashboardViewModel
import com.autodiag.viewmodel.FeatureState
import com.autodiag.viewmodel.ModuleStatus
import com.autodiag.viewmodel.VagViewModel
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
    advancedVm: AdvancedViewModel = viewModel(),
    codingVm: CodingViewModel = viewModel(),
    vagVm: VagViewModel = viewModel()
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
        Tab("Mi Auto") { Icon(Icons.Default.Tune, contentDescription = null) },
        Tab("Más") { Icon(Icons.Default.Terminal, contentDescription = null) }
    )
    val titles = listOf("Tablero en vivo", "Sensores", "Gráficas en vivo", "Códigos de falla", "Mi Auto (modo fácil)", "Herramientas")

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
                4 -> VagTab(vagVm)
                5 -> MoreTab(diagVm, terminalVm, advancedVm, codingVm, history, onDisconnect)
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

    Column(modifier = Modifier.fillMaxSize()) {
        // Botones de acción siempre visibles arriba.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { dtcVm.initialize() },
                enabled = !isLoading,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                else Text("🔍 Escanear fallas", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = { dtcVm.clearDtcs() },
                enabled = !isLoading && dtcs.isNotEmpty(),
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444),
                    disabledContainerColor = Color(0xFF1E2A3D)
                )
            ) { Text("🗑 Borrar fallas", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        }

        // Contenido según estado.
        when {
            isLoading -> Box(Modifier.weight(1f), Alignment.Center) { CircularProgressIndicator(color = Color(0xFF2563EB)) }
            error != null -> Box(Modifier.weight(1f)) { CenteredMessage("Error al leer DTCs", Color(0xFFEF4444), error ?: "") }
            dtcs.isEmpty() -> Box(Modifier.weight(1f)) { CenteredMessage("✓ Sin códigos de falla", Color(0xFF22C55E), "Tu vehículo está funcionando limpio.") }
            else -> Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${dtcs.size} código${if (dtcs.size != 1) "s" else ""} almacenado${if (dtcs.size != 1) "s" else ""}",
                    color = Color(0xFF7C8AA5), fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(dtcs, key = { it.code }) { dtc -> DtcCard(dtc = dtc) }
                }
            }
        }
    }
}

@Composable
private fun VagTab(vm: VagViewModel) {
    if (!vm.available) {
        CenteredMessage(
            "Conéctate primero", Color(0xFFE5EAF2),
            "Esta sección lee y configura tu auto VAG. Conéctate por Bluetooth a tu adaptador ELM327 para usarla."
        )
        return
    }
    val modules by vm.modules.collectAsState()
    val features by vm.features.collectAsState()
    val status by vm.status.collectAsState()
    val busy by vm.busy.collectAsState()
    val securityCode by vm.securityCode.collectAsState()
    val sa2Bytecode by vm.sa2Bytecode.collectAsState()
    val diag by vm.diag.collectAsState()
    val lightSensorValue by vm.lightSensorValue.collectAsState()
    val detectedSensorDid by vm.detectedSensorDid.collectAsState()
    val clipboard = LocalClipboardManager.current
    // Valor del slider (se inicializa del valor leído del auto).
    var sliderValue by rememberSaveable { mutableStateOf(0f) }
    // Sincroniza el slider cuando llega el valor del auto.
    androidx.compose.runtime.LaunchedEffect(lightSensorValue) {
        if (lightSensorValue >= 0) sliderValue = lightSensorValue.toFloat()
    }

    // Conversión a porcentaje del umbral del sensor: 0 = 0% (día), 0x40000000 = 100%.
    val fullScale = 0x40000000.toFloat()
    fun toPct(v: Float): String = "%.1f%%".format((v / fullScale * 100f).coerceIn(0f, 100f))

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Barra de estado.
        item {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFF131C2E), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF1E2A3D), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (busy) {
                    CircularProgressIndicator(color = Color(0xFF2563EB), strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(12.dp))
                }
                Text(status, color = Color(0xFFC8D2E0), fontSize = 13.sp)
            }
        }

        // Diagnóstico crudo (hex) — aparece tras una operación, para copiar/compartir.
        if (diag.isNotBlank()) {
            item {
                VagCard("🔎 Diagnóstico", Color(0xFF93B4E8), bg = Color(0xFF0E1626), border = Color(0xFF2A3A52)) {
                    Text(diag, color = Color(0xFF9FB3C8), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton("Copiar", Modifier.weight(1f)) { clipboard.setText(AnnotatedString(diag)) }
                        PillButton("Ocultar", Modifier.weight(1f), bg = Color(0xFF1A2438), fg = Color(0xFF7C8AA5)) { vm.clearDiag() }
                    }
                }
            }
        }

        // Acciones principales.
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { vm.scanModules() }, enabled = !busy,
                    modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) { Text("Escanear el auto", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                Button(
                    onClick = { vm.readVin() }, enabled = !busy,
                    modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A5F))
                ) { Text("Leer VIN", color = Color(0xFF93B4E8), fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
            }
        }

        // Módulos encontrados.
        if (modules.isNotEmpty()) {
            item { SectionTitle("Módulos encontrados") }
            items(modules, key = { it.address }) { m -> ModuleRow(m) }
        }

        // ───────── Sensor de luz (todo en una tarjeta: buscar + ajustar) ─────────
        item {
            VagCard(
                "💡 Sensor de luz (polarizado)", Color(0xFFFBD44A),
                bg = Color(0xFF0F1E2D), border = Color(0xFF1E4060),
                help = "Para tu polarizado oscuro: si el auto cree que está de noche y prende las " +
                    "luces de día, baja el %. Paso 1 (opcional pero recomendado): a pleno sol toca " +
                    "'Capturar LUZ', tapa el sensor y toca 'Capturar SOMBRA' — la app halla solo el " +
                    "byte correcto. Paso 2: 'Leer actual', mueve el slider y 'Aplicar'. Tras aplicar, " +
                    "apaga y prende el contacto para que el módulo tome el valor."
            ) {
                // Paso 1 — detección.
                Text("1 · Detectar el byte del sensor", color = Color(0xFF7FD0E0), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PillButton("☀ Capturar LUZ", Modifier.weight(1f), enabled = !busy) { vm.captureLightSnapshot(true) }
                    PillButton("🌑 Capturar SOMBRA", Modifier.weight(1f), bg = Color(0xFF24344A), fg = Color(0xFF9FB3C8), enabled = !busy) { vm.captureLightSnapshot(false) }
                }
                if (detectedSensorDid != null) {
                    Spacer(Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF14331F), RoundedCornerShape(8.dp)).padding(10.dp)) {
                        Text("✓ Sensor detectado: DID $detectedSensorDid", color = Color(0xFF6EE7A0), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Borrar capturas", color = Color(0xFF7C8AA5), fontSize = 10.sp,
                    modifier = Modifier.clickable(enabled = !busy) { vm.resetLightSnapshots() }.padding(vertical = 2.dp))

                Spacer(Modifier.height(14.dp))
                // Paso 2 — ajuste.
                Text("2 · Ajustar el umbral", color = Color(0xFF7FD0E0), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Actual del auto:", color = Color(0xFF7C8AA5), fontSize = 11.sp)
                    Text(
                        if (lightSensorValue < 0) "—  (toca Leer actual)"
                        else "${toPct(lightSensorValue.toFloat())}   0x${"%08X".format(lightSensorValue)}",
                        color = Color(0xFF93B4E8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    toPct(sliderValue),
                    color = Color(0xFFFBD44A), fontSize = 26.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
                )
                Text(
                    "0x${"%08X".format(sliderValue.toInt())}",
                    color = Color(0xFF7C8AA5), fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 0f..0x40000000.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFBD44A),
                        activeTrackColor = Color(0xFFFBD44A),
                        inactiveTrackColor = Color(0xFF24344A)
                    )
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("0% · día", color = Color(0xFF7C8AA5), fontSize = 9.sp)
                    Text("100% · oscuro", color = Color(0xFF7C8AA5), fontSize = 9.sp)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    PillButton("Leer actual", Modifier.weight(1f), enabled = !busy) { vm.readLightSensor() }
                    PillButton("Aplicar", Modifier.weight(1f), bg = Color(0xFF7A5C00), fg = Color(0xFFFBD44A), enabled = !busy && lightSensorValue >= 0) {
                        vm.writeLightSensorThreshold(sliderValue.toInt())
                    }
                    PillButton("Fábrica", Modifier.weight(1f), bg = Color(0xFF4B3A1F), fg = Color(0xFFCDC8B1), enabled = !busy) {
                        sliderValue = 0x315B34D3.toFloat()
                    }
                }
            }
        }

        // ───────── Funciones ocultas (interruptores) ─────────
        item {
            VagCard(
                "🎛 Funciones ocultas", Color(0xFFE5EAF2),
                help = "Funciones del VW T-Cross (MQB A0). La app lee el estado real antes de cambiar " +
                    "nada — toca 'Leer estado'. Si un módulo no la soporta o pide acceso de seguridad, " +
                    "el interruptor lo dice en texto en vez de fallar en silencio.",
                trailing = {
                    PillButton("Leer estado", enabled = !busy) { vm.loadFeatures() }
                }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PillButton("Leer confort (09)", Modifier.weight(1f), bg = Color(0xFF142235), fg = Color(0xFF9FB3C8), enabled = !busy) { vm.dumpCoding("09") }
                    PillButton("Leer tablero (17)", Modifier.weight(1f), bg = Color(0xFF142235), fg = Color(0xFF9FB3C8), enabled = !busy) { vm.dumpCoding("17") }
                }
                Spacer(Modifier.height(10.dp))
                if (features.isEmpty()) {
                    Text("Toca 'Leer estado' para ver qué funciones tiene tu auto.", color = Color(0xFF7C8AA5), fontSize = 12.sp)
                } else {
                    features.forEach { fs ->
                        FeatureRow(fs, busy, vm)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        // ───────── Acceso de seguridad (código + SA2 + probar) ─────────
        item {
            VagCard(
                "🔐 Acceso de seguridad", Color(0xFF6EE7A0),
                bg = Color(0xFF0E1B14), border = Color(0xFF1F4030),
                help = "Para escribir codificación, el módulo pide una clave. La app prueba los " +
                    "niveles del MQB (03 = codificación, luego 01/05/07) con los logins estáticos " +
                    "(09→31347, 17→47115, 19→20103), un solo intento para no bloquear. Para la clave " +
                    "REAL pega el bytecode SA2 de tu módulo (ver guía). Si tu T-Cross tiene SFD " +
                    "(post-2021), escribir exige token online de VW; leer siempre funciona."
            ) {
                OutlinedTextField(
                    value = securityCode,
                    onValueChange = { vm.setSecurityCode(it) },
                    label = { Text("Código de seguridad (opcional)", fontSize = 12.sp) },
                    placeholder = { Text("Ej. 31347 — vacío usa el conocido", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE5EAF2), unfocusedTextColor = Color(0xFFE5EAF2),
                        focusedBorderColor = Color(0xFF2563EB), unfocusedBorderColor = Color(0xFF1E2A3D),
                        focusedLabelColor = Color(0xFF93B4E8), unfocusedLabelColor = Color(0xFF7C8AA5)
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = sa2Bytecode,
                    onValueChange = { vm.setSa2Bytecode(it) },
                    label = { Text("🧬 Bytecode SA2 (clave real, opcional)", fontSize = 12.sp) },
                    placeholder = { Text("68 03 81 93 54 41 4A 49 …", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE5EAF2), unfocusedTextColor = Color(0xFFE5EAF2),
                        focusedBorderColor = Color(0xFF22C55E), unfocusedBorderColor = Color(0xFF1E2A3D),
                        focusedLabelColor = Color(0xFF6EE7A0), unfocusedLabelColor = Color(0xFF7C8AA5)
                    )
                )
                Spacer(Modifier.height(12.dp))
                Text("Probar cálculo de clave (sin escribir):", color = Color(0xFF9FB3C8), fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    for (addr in listOf("09", "17", "19")) {
                        PillButton("Módulo $addr", Modifier.weight(1f), enabled = !busy) { vm.testSecurityAccess(addr) }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

/** Botón "pastilla" reutilizable: reemplaza el patrón Box+clickable repetido. */
@Composable
private fun PillButton(
    label: String,
    modifier: Modifier = Modifier,
    bg: Color = Color(0xFF1E3A5F),
    fg: Color = Color(0xFF93B4E8),
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(if (enabled) bg else Color(0xFF18222F), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (enabled) fg else Color(0xFF55627A),
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center
        )
    }
}

/**
 * Tarjeta de sección VAG con título, ayuda colapsable (ⓘ) y contenido. Mantener el
 * texto explicativo dentro de [help] evita los muros de texto: el usuario lo abre
 * solo si lo necesita.
 */
@Composable
private fun VagCard(
    title: String,
    accent: Color,
    help: String? = null,
    bg: Color = Color(0xFF0F1A28),
    border: Color = Color(0xFF1E2A3D),
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var showHelp by rememberSaveable(title) { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(bg, RoundedCornerShape(14.dp))
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (help != null) {
                Text(
                    if (showHelp) "✕" else "ⓘ",
                    color = Color(0xFF7C8AA5), fontSize = 14.sp,
                    modifier = Modifier.clickable { showHelp = !showHelp }.padding(horizontal = 6.dp)
                )
            }
            trailing?.invoke()
        }
        if (help != null && showHelp) {
            Spacer(Modifier.height(6.dp))
            Text(help, color = Color(0xFF8597AD), fontSize = 10.sp, lineHeight = 14.sp)
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun ModuleRow(m: ModuleStatus) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(Color(0xFF131C2E), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF1E2A3D), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (m.present) "✓" else "○", color = if (m.present) Color(0xFF22C55E) else Color(0xFF55627A), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("${m.name}  (${m.address})", color = Color(0xFFE5EAF2), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(m.detail, color = Color(0xFF7C8AA5), fontSize = 11.sp)
        }
    }
}

@Composable
private fun FeatureRow(fs: FeatureState, busy: Boolean, vm: VagViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(Color(0xFF131C2E), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF1E2A3D), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(fs.feature.name, color = Color(0xFFE5EAF2), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(3.dp))
                Text(fs.feature.description, color = Color(0xFF7C8AA5), fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.size(12.dp))
            Switch(
                checked = fs.enabled,
                onCheckedChange = { vm.toggleFeature(fs.feature, it) },
                enabled = fs.known && !busy,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF22C55E),
                    uncheckedThumbColor = Color(0xFF7C8AA5),
                    uncheckedTrackColor = Color(0xFF1E2A3D)
                )
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            if (fs.known) "Estado: ${fs.note}" else "ⓘ ${fs.note}",
            color = if (fs.known) Color(0xFF93B4E8) else Color(0xFFF59E0B),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun MoreTab(
    diagVm: DiagnosticsViewModel,
    terminalVm: TerminalViewModel,
    advancedVm: AdvancedViewModel,
    codingVm: CodingViewModel,
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

        // --- Modificaciones del vehículo (escritura directa BT/BLE) ---
        if (codingVm.available) {
            Spacer(modifier = Modifier.height(8.dp))
            SectionTitle("Modificaciones del vehículo")
            CodingPanel(codingVm)
        }

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
private fun CodingPanel(vm: CodingViewModel) {
    val log by vm.log.collectAsState()
    val busy by vm.busy.collectAsState()
    var ecu by remember { mutableStateOf("7E0") }
    var did by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var rawCmd by remember { mutableStateOf("") }
    // Acción pendiente de confirmar: (descripción, ejecutar).
    var pending by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    fun confirm(desc: String, action: () -> Unit) { pending = desc to action }

    Text(
        "⚠ Escribe directamente en la ECU de tu auto. Una operación incorrecta " +
            "puede dejar inservible un módulo. Úsalo solo si sabes lo que haces y, " +
            "de preferencia, con el motor encendido y la batería con buena carga.",
        color = Color(0xFFF59E0B), fontSize = 11.sp
    )
    Spacer(modifier = Modifier.height(10.dp))

    // --- Paso 1: seleccionar ECU y abrir sesión ---
    Text("1 · ECU y sesión", color = Color(0xFF93B4E8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = ecu, onValueChange = { ecu = it },
            label = { Text("Header ECU (ej. 7E0)", color = Color(0xFF7C8AA5), fontSize = 11.sp) },
            singleLine = true, modifier = Modifier.weight(1f), colors = monoFieldColors()
        )
        Spacer(modifier = Modifier.size(8.dp))
        Box(
            modifier = Modifier.background(Color(0xFF1E3A5F), RoundedCornerShape(8.dp))
                .clickable(enabled = !busy) { vm.setEcuHeader(ecu) }.padding(horizontal = 14.dp, vertical = 12.dp)
        ) { Text("Fijar", color = Color(0xFF93B4E8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        AdvChip("Sesión ext.", Modifier.weight(1f)) { vm.openExtendedSession() }
        AdvChip("Sesión prog.", Modifier.weight(1f)) { vm.openProgrammingSession() }
        AdvChip("Reset ECU", Modifier.weight(1f)) { confirm("Reiniciar la ECU seleccionada") { vm.ecuReset() } }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // --- Paso 2: acceso de seguridad ---
    Text("2 · Acceso de seguridad", color = Color(0xFF93B4E8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(6.dp))
    var key by remember { mutableStateOf("") }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        AdvChip("Pedir semilla", Modifier.weight(1f)) { vm.requestSeed() }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = key, onValueChange = { key = it },
            label = { Text("Llave calculada (hex)", color = Color(0xFF7C8AA5), fontSize = 11.sp) },
            singleLine = true, modifier = Modifier.weight(1f), colors = monoFieldColors()
        )
        Spacer(modifier = Modifier.size(8.dp))
        IconButton(onClick = { vm.sendKey(key) }, enabled = !busy && key.isNotBlank()) {
            Icon(Icons.Default.Send, contentDescription = "Enviar llave", tint = if (busy) Color(0xFF55627A) else Color(0xFF2563EB))
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // --- Paso 3: escritura / codificación ---
    Text("3 · Escribir configuración", color = Color(0xFF93B4E8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = did, onValueChange = { did = it },
            label = { Text("DID", color = Color(0xFF7C8AA5), fontSize = 11.sp) },
            singleLine = true, modifier = Modifier.weight(1f), colors = monoFieldColors()
        )
        Spacer(modifier = Modifier.size(8.dp))
        OutlinedTextField(
            value = value, onValueChange = { value = it },
            label = { Text("Valor (hex)", color = Color(0xFF7C8AA5), fontSize = 11.sp) },
            singleLine = true, modifier = Modifier.weight(1.4f), colors = monoFieldColors()
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Button(
        onClick = { confirm("Escribir DID $did = $value en ECU $ecu") { vm.writeDataByIdentifier(did, value) } },
        enabled = !busy && did.isNotBlank() && value.isNotBlank(),
        modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), disabledContainerColor = Color(0xFF1E2A3D))
    ) { Text("Escribir a la ECU (2E)", color = Color.White, fontWeight = FontWeight.SemiBold) }

    Spacer(modifier = Modifier.height(10.dp))

    // --- Acciones rápidas comunes ---
    Text("Acciones rápidas", color = Color(0xFF93B4E8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        AdvChip("Reset adapt.", Modifier.weight(1f)) {
            confirm("Borrar adaptaciones / aprendizajes de la ECU") { vm.resetAdaptations() }
        }
        AdvChip("Rutina FF00", Modifier.weight(1f)) {
            confirm("Ejecutar rutina FF00 (arranque)") { vm.routineControl("01", "FF00") }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // --- Comando crudo libre ---
    Text("Comando manual (avanzado)", color = Color(0xFF93B4E8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = rawCmd, onValueChange = { rawCmd = it },
            placeholder = { Text("ej. 2E F190 ...", color = Color(0xFF55627A)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            singleLine = true, modifier = Modifier.weight(1f), colors = monoFieldColors()
        )
        Spacer(modifier = Modifier.size(8.dp))
        IconButton(
            onClick = { val c = rawCmd; confirm("Enviar comando manual: $c") { vm.sendRawWrite(c); rawCmd = "" } },
            enabled = !busy && rawCmd.isNotBlank()
        ) {
            Icon(Icons.Default.Send, contentDescription = "Enviar", tint = if (busy) Color(0xFF55627A) else Color(0xFFEF4444))
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // --- Bitácora de operaciones ---
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Bitácora", color = Color(0xFFE5EAF2), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (log.isNotEmpty()) {
            Box(modifier = Modifier.clickable { vm.clearLog() }.padding(6.dp)) {
                Icon(Icons.Default.DeleteForever, contentDescription = "Limpiar", tint = Color(0xFF7C8AA5), modifier = Modifier.size(18.dp))
            }
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
    Column(
        modifier = Modifier.fillMaxWidth().height(180.dp)
            .background(Color(0xFF0A0F1A), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF1E2A3D), RoundedCornerShape(10.dp))
            .verticalScroll(rememberScrollState()).padding(12.dp)
    ) {
        if (log.isEmpty()) {
            Text("Las operaciones aparecerán aquí…", color = Color(0xFF55627A), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        log.forEach { step -> CodingLogRow(step) }
    }

    // Diálogo de confirmación para cualquier escritura.
    pending?.let { (desc, action) ->
        AlertDialog(
            onDismissRequest = { pending = null },
            containerColor = Color(0xFF131C2E),
            title = { Text("Confirmar escritura", color = Color(0xFFE5EAF2), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "$desc\n\nEsta operación modifica tu vehículo y no siempre es reversible. " +
                        "¿Continuar?",
                    color = Color(0xFFC8D2E0), fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { action(); pending = null }) {
                    Text("Ejecutar", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text("Cancelar", color = Color(0xFF7C8AA5)) }
            }
        )
    }
}

@Composable
private fun CodingLogRow(step: CodingStep) {
    if (step.response.isBlank()) {
        Text(step.request, color = Color(0xFF93B4E8), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(4.dp))
        return
    }
    Text("> ${step.request}", color = Color(0xFF22C55E), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    Text(
        step.response,
        color = if (step.ok) Color(0xFFC8D2E0) else Color(0xFFEF4444),
        fontSize = 11.sp, fontFamily = FontFamily.Monospace
    )
    Spacer(modifier = Modifier.height(6.dp))
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
