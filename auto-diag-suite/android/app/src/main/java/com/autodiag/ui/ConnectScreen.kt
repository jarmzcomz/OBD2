package com.autodiag.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autodiag.viewmodel.ConnectState
import com.autodiag.viewmodel.ConnectViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@Composable
fun ConnectScreen(
    onConnected: (mode: String, address: String) -> Unit,
    connectViewModel: ConnectViewModel = viewModel()
) {
    // --- TODOS los remember/LaunchedEffect DEBEN ir aquí arriba, sin excepciones ---

    val connectState by connectViewModel.state.collectAsState()

    var selectedMode by remember { mutableStateOf("wifi") }
    var wifiHost by remember { mutableStateOf("192.168.1.100") }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var manualMac by remember { mutableStateOf("") }
    var refreshTick by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    val needsRuntimePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val requiredPermissions = remember {
        if (needsRuntimePermission)
            arrayOf(
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN
            )
        else emptyArray()
    }

    fun hasBtPermission(): Boolean =
        !needsRuntimePermission || requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    var hasBtPermission by remember { mutableStateOf(hasBtPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        hasBtPermission = hasBtPermission()
        refreshTick++
    }

    val enableBtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshTick++ }

    val scope = rememberCoroutineScope()
    val scannedBle = remember { mutableStateListOf<BluetoothDevice>() }
    var isScanning by remember { mutableStateOf(false) }

    val scanPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            arrayOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT
            )
        else
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val scanCallback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val dev = result.device ?: return
                if (scannedBle.none { it.address == dev.address }) scannedBle.add(dev)
            }
        }
    }

    fun scanPermsGranted() = scanPermissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun startBleScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        scannedBle.clear()
        isScanning = true
        try { scanner.startScan(scanCallback) } catch (_: SecurityException) { isScanning = false; return }
        scope.launch {
            delay(10_000)
            try { scanner.stopScan(scanCallback) } catch (_: SecurityException) {}
            isScanning = false
        }
    }

    val scanPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> if (scanPermsGranted()) startBleScan() }

    // Pedir permisos al inicio si hacen falta
    LaunchedEffect(Unit) {
        if (needsRuntimePermission && !hasBtPermission()) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    // Navegar cuando la conexión sea exitosa
    LaunchedEffect(connectState) {
        if (connectState is ConnectState.Connected) {
            connectViewModel.resetState()
            onConnected("connected", "")
        }
    }

    // --- A partir de aquí: UI condicional (sin early return) ---

    val btEnabled = bluetoothAdapter?.isEnabled == true

    val pairedDevices: Set<BluetoothDevice> = remember(hasBtPermission, btEnabled, refreshTick) {
        if (!hasBtPermission || bluetoothAdapter == null || !btEnabled) emptySet()
        else try { bluetoothAdapter.bondedDevices ?: emptySet() }
        catch (_: SecurityException) { emptySet() }
    }

    val isConnecting = connectState is ConnectState.Connecting
    val connectError = (connectState as? ConnectState.Error)?.message

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B1220))) {

        // Pantalla de selección (siempre compuesta, solo visible cuando no conectando)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text("AutoDiag", color = Color(0xFF2563EB), fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text(
                "OBD-II Diagnostic Suite",
                color = Color(0xFF7C8AA5),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            // Banner de error
            if (connectError != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2D1515), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No se pudo conectar", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(connectError, color = Color(0xFFB0B8C8), fontSize = 12.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { connectViewModel.resetState() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) { Text("Reintentar", color = Color.White) }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Selector de modo
            Text(
                "Modo de conexión",
                color = Color(0xFFE5EAF2),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModeButton(
                    label = "WiFi\nWebSocket",
                    icon = { Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    selected = selectedMode == "wifi",
                    onClick = { selectedMode = "wifi"; selectedDevice = null },
                    modifier = Modifier.weight(1f)
                )
                ModeButton(
                    label = "BT Clásico\niCar Pro 3.0",
                    icon = { Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    selected = selectedMode == "bluetooth",
                    onClick = { selectedMode = "bluetooth"; selectedDevice = null },
                    modifier = Modifier.weight(1f)
                )
                ModeButton(
                    label = "BT LE\niCar Pro 4.0",
                    icon = { Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    selected = selectedMode == "ble",
                    onClick = { selectedMode = "ble"; selectedDevice = null },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (selectedMode) {
                "wifi" -> {
                    Text("IP del servidor", color = Color(0xFF7C8AA5), fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                    OutlinedTextField(
                        value = wifiHost,
                        onValueChange = { wifiHost = it },
                        placeholder = { Text("192.168.1.100:8000", color = Color(0xFF7C8AA5)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text("WebSocket: ws://IP:8000/ws/live", color = Color(0xFF7C8AA5), fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                }

                "ble" -> {
                    Text("Dispositivos Bluetooth LE", color = Color(0xFF7C8AA5), fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                    Button(
                        onClick = {
                            if (!btEnabled) {
                                enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                            } else if (scanPermsGranted()) {
                                startBleScan()
                            } else {
                                scanPermLauncher.launch(scanPermissions)
                            }
                        },
                        enabled = !isScanning,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Escaneando...", color = Color.White)
                        } else {
                            Text("Escanear adaptador BLE", color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (scannedBle.isEmpty()) {
                        Text(
                            "Presiona Escanear. Tu iCar Pro 4.0 aparecerá como " +
                                "\"Vlink\", \"iCar\" u \"OBD\". El dongle debe estar en el carro.",
                            color = Color(0xFF7C8AA5), fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth().background(Color(0xFF131C2E), RoundedCornerShape(8.dp)).padding(16.dp)
                        )
                    } else {
                        DeviceList(devices = scannedBle.toList(), selectedDevice = selectedDevice, onSelect = { selectedDevice = it })
                    }
                }

                else -> {
                    // Bluetooth Clásico
                    Text("Dispositivos emparejados", color = Color(0xFF7C8AA5), fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                    when {
                        !hasBtPermission -> InfoBox {
                            Text("Se necesita permiso Bluetooth.", color = Color(0xFF7C8AA5), fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { permissionLauncher.launch(requiredPermissions) }, colors = ButtonDefaults.buttonColors(Color(0xFF2563EB))) {
                                Text("Conceder permiso", color = Color.White)
                            }
                        }
                        !btEnabled -> InfoBox {
                            Text("Bluetooth está apagado.", color = Color(0xFF7C8AA5), fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) }, colors = ButtonDefaults.buttonColors(Color(0xFF2563EB))) {
                                Text("Activar Bluetooth", color = Color.White)
                            }
                        }
                        pairedDevices.isEmpty() -> InfoBox {
                            Text("No hay dispositivos emparejados. Ve a Configuración → Bluetooth, empareja el adaptador y vuelve.", color = Color(0xFF7C8AA5), fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { refreshTick++ }, colors = ButtonDefaults.buttonColors(Color(0xFF2563EB))) {
                                Text("Actualizar lista", color = Color.White)
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("O ingresa la MAC manualmente:", color = Color(0xFF7C8AA5), fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
                            OutlinedTextField(
                                value = manualMac,
                                onValueChange = { manualMac = it.uppercase() },
                                placeholder = { Text("00:1D:A5:68:98:8B", color = Color(0xFF7C8AA5)) },
                                colors = fieldColors(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            if (BluetoothAdapter.checkBluetoothAddress(manualMac)) {
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { connectViewModel.connect("bluetooth", manualMac) },
                                    colors = ButtonDefaults.buttonColors(Color(0xFF2563EB)),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Conectar a este MAC", color = Color.White) }
                            }
                        }
                        else -> DeviceList(devices = pairedDevices.toList(), selectedDevice = selectedDevice, onSelect = { selectedDevice = it })
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Dispositivo seleccionado
            if (selectedDevice != null) {
                val devName = try { selectedDevice!!.name ?: selectedDevice!!.address } catch (_: SecurityException) { selectedDevice!!.address }
                Text("Seleccionado: $devName", color = Color(0xFF22C55E), fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
            }

            val canConnect = when (selectedMode) {
                "wifi" -> wifiHost.isNotBlank()
                else -> selectedDevice != null
            }

            Button(
                onClick = {
                    val address = if (selectedMode == "wifi") wifiHost.trim() else selectedDevice!!.address
                    connectViewModel.connect(selectedMode, address)
                },
                enabled = canConnect && !isConnecting,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
                    disabledContainerColor = Color(0xFF1E2A3D)
                )
            ) {
                Text("Conectar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Overlay de "Conectando..." encima de todo, sin interrumpir el árbol de composables
        if (isConnecting) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xE60B1220)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF2563EB), modifier = Modifier.size(56.dp), strokeWidth = 4.dp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Conectando al adaptador...", color = Color(0xFFE5EAF2), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Estableciendo conexión Bluetooth.\nMantén el vehículo en contacto (ACC o motor).",
                        color = Color(0xFF7C8AA5), fontSize = 13.sp, textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceList(
    devices: List<BluetoothDevice>,
    selectedDevice: BluetoothDevice?,
    onSelect: (BluetoothDevice) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(Color(0xFF131C2E), RoundedCornerShape(8.dp))
    ) {
        items(devices, key = { it.address }) { device ->
            val name = try { device.name ?: device.address } catch (_: SecurityException) { device.address }
            val isSelected = selectedDevice?.address == device.address
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(device) }
                    .background(if (isSelected) Color(0xFF1E3A5F) else Color.Transparent)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = if (isSelected) Color(0xFF2563EB) else Color(0xFF7C8AA5), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, color = Color(0xFFE5EAF2), fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    Text(device.address, color = Color(0xFF7C8AA5), fontSize = 11.sp)
                }
                if (isSelected) Text("✓", color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun InfoBox(content: @Composable Column.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF131C2E), RoundedCornerShape(8.dp)).padding(16.dp),
        content = content
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF2563EB),
    unfocusedBorderColor = Color(0xFF1E2A3D),
    focusedTextColor = Color(0xFFE5EAF2),
    unfocusedTextColor = Color(0xFFE5EAF2),
    cursorColor = Color(0xFF2563EB),
    focusedContainerColor = Color(0xFF131C2E),
    unfocusedContainerColor = Color(0xFF131C2E)
)

@Composable
private fun ModeButton(
    label: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(if (selected) Color(0xFF1E3A5F) else Color(0xFF131C2E), RoundedCornerShape(12.dp))
            .border(1.dp, if (selected) Color(0xFF2563EB) else Color(0xFF1E2A3D), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon()
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            label,
            color = if (selected) Color(0xFF2563EB) else Color(0xFF7C8AA5),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
