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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    val diag by vm.diag.collectAsState()
    val clipboard = LocalClipboardManager.current

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Barra de estado en texto plano.
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

        // Panel de diagnóstico crudo (hex). Aparece tras un intento de cambio para
        // que el usuario lo copie y lo comparta — así se afina sin adivinar.
        if (diag.isNotBlank()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(Color(0xFF0E1626), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF2A3a52), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text("🔎 Diagnóstico (compártelo para afinar)", color = Color(0xFF93B4E8),
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(diag, color = Color(0xFF9FB3C8), fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier.background(Color(0xFF1E3A5F), RoundedCornerShape(8.dp))
                                .clickable { clipboard.setText(AnnotatedString(diag)) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) { Text("Copiar", color = Color(0xFF93B4E8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        Box(
                            modifier = Modifier.background(Color(0xFF1A2438), RoundedCornerShape(8.dp))
                                .clickable { vm.clearDiag() }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) { Text("Ocultar", color = Color(0xFF7C8AA5), fontSize = 12.sp) }
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

        // Resultado del escaneo de módulos.
        if (modules.isNotEmpty()) {
            item { SectionTitle("Módulos encontrados") }
            items(modules, key = { it.address }) { m -> ModuleRow(m) }
        }

        // Interruptores de funciones.
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Funciones (interruptores)", color = Color(0xFFE5EAF2), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.background(Color(0xFF1E3A5F), RoundedCornerShape(8.dp))
                        .clickable(enabled = !busy) { vm.loadFeatures() }.padding(horizontal = 12.dp, vertical = 8.dp)
                ) { Text("Leer estado", color = Color(0xFF93B4E8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
        item {
            Text(
                "⚠ Funciones para VW T-Cross (MQB A0). La app lee el estado real antes de " +
                    "cambiar nada. Si tu módulo no la soporta o pide acceso de seguridad, el " +
                    "interruptor te lo dirá en texto en vez de fallar a ciegas.",
                color = Color(0xFFF59E0B), fontSize = 11.sp
            )
        }
        // Volcado de codificación (solo lectura) para mapear con datos reales.
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("🔍 Leer codificación real (solo lectura, sin riesgo)", color = Color(0xFF93B4E8),
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.weight(1f).background(Color(0xFF142235), RoundedCornerShape(8.dp))
                            .clickable(enabled = !busy) { vm.dumpCoding("09") }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("Volcar 09 (luces/confort)", color = Color(0xFF9FB3C8), fontSize = 11.sp) }
                    Box(
                        modifier = Modifier.weight(1f).background(Color(0xFF142235), RoundedCornerShape(8.dp))
                            .clickable(enabled = !busy) { vm.dumpCoding("17") }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("Volcar 17 (tablero)", color = Color(0xFF9FB3C8), fontSize = 11.sp) }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Para el sensor de luz, vuelca el 09 y compártelo: ahí está el umbral a ajustar.",
                    color = Color(0xFF7C8AA5), fontSize = 10.sp)
            }
        }
        // Campo opcional para pegar el código/llave de seguridad.
        item {
            OutlinedTextField(
                value = securityCode,
                onValueChange = { vm.setSecurityCode(it) },
                label = { Text("Código de seguridad (opcional, anula el de fábrica)", fontSize = 12.sp) },
                placeholder = { Text("Ej. 31347 — déjalo vacío para usar el conocido", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFFE5EAF2),
                    unfocusedTextColor = Color(0xFFE5EAF2),
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = Color(0xFF1E2A3D),
                    focusedLabelColor = Color(0xFF93B4E8),
                    unfocusedLabelColor = Color(0xFF7C8AA5)
                )
            )
        }
        item {
            Text(
                "La app ya trae los logins conocidos del MQB (módulo 09 → 31347, etc.) e intenta " +
                    "el acceso una sola vez (sin reintentos, para no bloquear el módulo). Nota: muchos " +
                    "ELM327 económicos fallan al ESCRIBIR en estos autos, y si tu T-Cross trae SFD, la " +
                    "escritura requiere desbloqueo online de VW. Leer siempre funciona.",
                color = Color(0xFF7C8AA5), fontSize = 10.sp
            )
        }
        if (features.isEmpty()) {
            item {
                Text("Toca \"Leer estado\" para ver qué funciones tiene tu auto.", color = Color(0xFF7C8AA5), fontSize = 13.sp)
            }
        } else {
            items(features, key = { it.feature.id }) { fs -> FeatureRow(fs, busy, vm) }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
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
