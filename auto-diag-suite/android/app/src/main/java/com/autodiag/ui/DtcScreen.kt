package com.autodiag.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.autodiag.viewmodel.DashboardViewModel

@Composable
fun DtcScreen(viewModel: DashboardViewModel) {
    val dtcs by viewModel.dtcs.collectAsState()
    var showConfirm by remember { mutableStateOf(false) }

    Column(Modifier.padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.loadDtcs() }) { Text("Leer DTCs") }
            Button(
                onClick = { showConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
            ) { Text("Borrar DTCs") }
        }
        Spacer(Modifier.height(12.dp))
        if (dtcs.isEmpty()) {
            Text("Sin códigos de falla", color = Color(0xFF7C8AA5))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dtcs) { code ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E))) {
                        Text(text = code, modifier = Modifier.padding(12.dp), color = Color(0xFFE5EAF2))
                    }
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("¿Borrar DTCs?") },
            text = { Text("Se borrarán todos los códigos de falla y la luz MIL.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearDtcs(); showConfirm = false }) { Text("Borrar", color = Color(0xFFEF4444)) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancelar") }
            }
        )
    }
}
