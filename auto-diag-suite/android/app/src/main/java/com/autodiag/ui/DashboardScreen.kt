package com.autodiag.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
    mode: String,
    address: String,
    onNavigateToDtc: () -> Unit,
    onDisconnect: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    LaunchedEffect(mode, address) {
        viewModel.initialize(mode, address)
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
                            "AutoDiag Dashboard",
                            color = Color(0xFFE5EAF2),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isConnected) "● Connected" else "○ Disconnected",
                            color = if (isConnected) Color(0xFF22C55E) else Color(0xFF7C8AA5),
                            fontSize = 11.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToDtc) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = "View DTCs",
                            tint = Color(0xFFE5EAF2)
                        )
                    }
                    IconButton(onClick = {
                        viewModel.disconnect()
                        onDisconnect()
                    }) {
                        Icon(
                            Icons.Default.BluetoothDisabled,
                            contentDescription = "Disconnect",
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
            if (error != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Connection Error",
                        color = Color(0xFFEF4444),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = error ?: "",
                        color = Color(0xFF7C8AA5),
                        modifier = Modifier.padding(16.dp)
                    )
                    Button(
                        onClick = onDisconnect,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text("Go Back")
                    }
                }
            } else if (pidData.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Connecting to vehicle...",
                        color = Color(0xFF7C8AA5),
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(pidData.values.toList(), key = { it.code }) { pid ->
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color(0xFF131C2E),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                                )
                                .padding(8.dp)
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
