package com.autodiag.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@SuppressLint("MissingPermission")
@Composable
fun ConnectScreen(onConnected: (mode: String, address: String) -> Unit) {
    var selectedMode by remember { mutableStateOf("wifi") }
    var wifiHost by remember { mutableStateOf("192.168.1.100") }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }

    val context = LocalContext.current
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    // En Android 12+ se necesita BLUETOOTH_CONNECT concedido en runtime; sin él
    // bondedDevices lanza SecurityException y la lista de dongles sale vacía
    // (síntoma: "no reconoce el dongle"). Pedimos el permiso al entrar.
    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun hasAllPermissions(): Boolean = requiredPermissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    var hasBtPermission by remember { mutableStateOf(hasAllPermissions()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasBtPermission = result.values.all { it } || hasAllPermissions()
    }

    LaunchedEffect(Unit) {
        if (!hasBtPermission) permissionLauncher.launch(requiredPermissions)
    }

    // Se recalcula cuando se concede el permiso, refrescando la lista de dongles.
    val pairedDevices: Set<BluetoothDevice> = remember(hasBtPermission) {
        if (!hasBtPermission) emptySet()
        else try {
            bluetoothAdapter?.bondedDevices ?: emptySet()
        } catch (_: SecurityException) {
            emptySet()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1220))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            "AutoDiag",
            color = Color(0xFF2563EB),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "OBD-II Diagnostic Suite",
            color = Color(0xFF7C8AA5),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
        )

        // Mode selector
        Text(
            "Connection Mode",
            color = Color(0xFFE5EAF2),
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModeButton(
                label = "WiFi / WebSocket",
                icon = { Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(20.dp)) },
                selected = selectedMode == "wifi",
                onClick = { selectedMode = "wifi" },
                modifier = Modifier.weight(1f)
            )
            ModeButton(
                label = "Bluetooth ELM327",
                icon = { Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(20.dp)) },
                selected = selectedMode == "bluetooth",
                onClick = { selectedMode = "bluetooth" },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (selectedMode == "wifi") {
            // WiFi input
            Text(
                "Server IP Address",
                color = Color(0xFF7C8AA5),
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = wifiHost,
                onValueChange = { wifiHost = it },
                placeholder = { Text("192.168.1.100", color = Color(0xFF7C8AA5)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = Color(0xFF1E2A3D),
                    focusedTextColor = Color(0xFFE5EAF2),
                    unfocusedTextColor = Color(0xFFE5EAF2),
                    cursorColor = Color(0xFF2563EB),
                    focusedContainerColor = Color(0xFF131C2E),
                    unfocusedContainerColor = Color(0xFF131C2E)
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text(
                "FastAPI server runs on port 8000. WebSocket endpoint: ws://IP:8000/ws/live",
                color = Color(0xFF7C8AA5),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            // Bluetooth device list
            Text(
                "Paired Bluetooth Devices",
                color = Color(0xFF7C8AA5),
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            if (!hasBtPermission) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF131C2E), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        "Bluetooth permission required to list your ELM327 adapter.",
                        color = Color(0xFF7C8AA5),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { permissionLauncher.launch(requiredPermissions) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text("Grant Bluetooth permission", color = Color.White)
                    }
                }
            } else if (pairedDevices.isEmpty()) {
                Text(
                    "No paired devices found. Pair your ELM327 adapter in Android Settings → Bluetooth first.",
                    color = Color(0xFF7C8AA5),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF131C2E), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color(0xFF131C2E), RoundedCornerShape(8.dp))
                ) {
                    items(pairedDevices.toList()) { device ->
                        val name = try { device.name ?: device.address } catch (_: SecurityException) { device.address }
                        val isSelected = selectedDevice?.address == device.address
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDevice = device }
                                .background(
                                    if (isSelected) Color(0xFF1E3A5F) else Color.Transparent
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF2563EB) else Color(0xFF7C8AA5),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(name, color = Color(0xFFE5EAF2), fontSize = 14.sp)
                                Text(device.address, color = Color(0xFF7C8AA5), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        val canConnect = when (selectedMode) {
            "wifi" -> wifiHost.isNotBlank()
            "bluetooth" -> selectedDevice != null
            else -> false
        }

        Button(
            onClick = {
                val address = when (selectedMode) {
                    "wifi" -> wifiHost.trim()
                    "bluetooth" -> selectedDevice!!.address
                    else -> ""
                }
                onConnected(selectedMode, address)
            },
            enabled = canConnect,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2563EB),
                disabledContainerColor = Color(0xFF1E2A3D)
            )
        ) {
            Text(
                "Connect",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

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
            .background(
                color = if (selected) Color(0xFF1E3A5F) else Color(0xFF131C2E),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.5.dp,
                color = if (selected) Color(0xFF2563EB) else Color(0xFF1E2A3D),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        icon()
        Text(
            label,
            color = if (selected) Color(0xFF2563EB) else Color(0xFF7C8AA5),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
