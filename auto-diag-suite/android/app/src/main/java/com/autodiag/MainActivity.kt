package com.autodiag

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.autodiag.ui.ConnectScreen
import com.autodiag.ui.HomeScreen
import com.autodiag.ui.theme.AutoDiagTheme
import com.autodiag.viewmodel.ConnectViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutoDiagTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AutoDiagApp()
                }
            }
        }
    }
}

@Composable
fun AutoDiagApp() {
    val navController = rememberNavController()
    // ConnectViewModel vive al nivel de la actividad: comparte la conexión entre pantallas.
    val connectViewModel: ConnectViewModel = viewModel()

    NavHost(navController = navController, startDestination = "connect") {
        composable("connect") {
            ConnectScreen(
                connectViewModel = connectViewModel,
                onConnected = { _, _ ->
                    navController.navigate("dashboard") {
                        popUpTo("connect") { inclusive = true }
                    }
                }
            )
        }
        composable("dashboard") {
            HomeScreen(
                onDisconnect = {
                    navController.navigate("connect") {
                        popUpTo("connect") { inclusive = true }
                    }
                }
            )
        }
    }
}
