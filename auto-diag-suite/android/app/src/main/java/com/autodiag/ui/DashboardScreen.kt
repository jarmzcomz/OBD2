package com.autodiag.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autodiag.ui.components.GaugeView
import com.autodiag.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToDtc: () -> Unit,
    onDisconnect: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    val pidData by viewModel.pidData.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "AutoDiag",
                            color = Color(0xFFE5EAF2),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = if (isConnected) "● Conectado" else "○ Desconectado",
                            color = if (isConnected) Color(0xFF22C55E) else Color(0xFF7C8AA5),
                            fontSize = 12.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToDtc) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = "Ver códigos de falla",
                            tint = Color(0xFFE5EAF2)
                        )
                    }
                    IconButton(onClick = {
                        viewModel.disconnect()
                        onDisconnect()
                    }) {
                        Icon(
                            Icons.Default.BluetoothDisabled,
                            contentDescription = "Desconectar",
                            tint = Color(0xFFEF4444)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF131C2E)
                )
            )
        },
        containerColor = Color(0xFF0B1220)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Error de conexión",
                            color = Color(0xFFEF4444),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = error ?: "",
                            color = Color(0xFF7C8AA5),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        Button(
                            onClick = onDisconnect,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("Volver")
                        }
                    }
                }

                pidData.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF2563EB), strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            "Leyendo datos del vehículo...",
                            color = Color(0xFF7C8AA5),
                            fontSize = 16.sp
                        )
                    }
                }

                else -> {
                    // Grid adaptativo: en el S25 Ultra (pantalla ancha) muestra 2-3
                    // columnas según el espacio, con celdas de tamaño cómodo.
                    LazyVerticalGrid(
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
                                        brush = Brush.verticalGradient(
                                            listOf(Color(0xFF15203A), Color(0xFF0F1729))
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = Color(0xFF1E2A3D),
                                        shape = RoundedCornerShape(20.dp)
                                    )
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
        }
    }
}
