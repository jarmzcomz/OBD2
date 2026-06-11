package com.autodiag.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConnectScreen(
    onConnectBluetooth: (BluetoothDevice) -> Unit,
    onConnectWifi: (String) -> Unit,
) {
    var mode by remember { mutableStateOf("wifi") }
    var wifiHost by remember { mutableStateOf("192.168.1.100") }
    val btAdapter = BluetoothAdapter.getDefaultAdapter()
    val bondedDevices = remember { btAdapter?.bondedDevices?.toList() ?: emptyList() }

    Column(Modifier.padding(16.dp)) {
        Text("Modo de conexión", fontSize = 18.sp, color = Color(0xFFE5EAF2))
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = mode == "wifi", onClick = { mode = "wifi" }, label = { Text("WiFi (servidor Python)") })
            FilterChip(selected = mode == "bt",   onClick = { mode = "bt" },   label = { Text("Bluetooth ELM327") })
        }

        Spacer(Modifier.height(16.dp))

        if (mode == "wifi") {
            OutlinedTextField(
                value = wifiHost, onValueChange = { wifiHost = it },
                label = { Text("IP del servidor") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { onConnectWifi(wifiHost) }, modifier = Modifier.fillMaxWidth()) {
                Text("Conectar vía WiFi")
            }
        } else {
            Text("Dispositivos Bluetooth pareados:", color = Color(0xFF7C8AA5))
            Spacer(Modifier.height(8.dp))
            if (bondedDevices.isEmpty()) {
                Text("No hay dispositivos pareados. Parealos en Ajustes de Android.", color = Color(0xFF7C8AA5))
            } else {
                LazyColumn {
                    items(bondedDevices) { device ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onConnectBluetooth(device) },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E))
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(device.name ?: "Desconocido", color = Color(0xFFE5EAF2))
                                Text(device.address, color = Color(0xFF7C8AA5), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
