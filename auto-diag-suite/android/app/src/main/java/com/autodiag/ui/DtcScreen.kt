package com.autodiag.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autodiag.ui.components.DtcCard
import com.autodiag.viewmodel.DtcViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DtcScreen(
    onBack: () -> Unit,
    viewModel: DtcViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    val dtcs by viewModel.dtcs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Fault Codes (DTC)",
                        color = Color(0xFFE5EAF2),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFFE5EAF2)
                        )
                    }
                },
                actions = {
                    if (dtcs.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearDtcs() }) {
                            Icon(
                                Icons.Default.DeleteForever,
                                contentDescription = "Clear DTCs",
                                tint = Color(0xFFEF4444)
                            )
                        }
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
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF2563EB)
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Error reading DTCs",
                            color = Color(0xFFEF4444),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            error ?: "",
                            color = Color(0xFF7C8AA5),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                dtcs.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "✓ No fault codes found",
                            color = Color(0xFF22C55E),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Your vehicle is running clean",
                            color = Color(0xFF7C8AA5),
                            fontSize = 14.sp
                        )
                    }
                }
                else -> {
                    Column {
                        Text(
                            text = "${dtcs.size} fault code${if (dtcs.size != 1) "s" else ""} detected",
                            color = Color(0xFF7C8AA5),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        LazyColumn {
                            items(dtcs, key = { it.code }) { dtc ->
                                DtcCard(dtc = dtc)
                            }
                        }
                    }
                }
            }
        }
    }
}
