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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.autodiag.ui.ConnectScreen
import com.autodiag.ui.DashboardScreen
import com.autodiag.ui.DtcScreen
import com.autodiag.ui.theme.AutoDiagTheme

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

    NavHost(navController = navController, startDestination = "connect") {
        composable("connect") {
            ConnectScreen(onConnected = { mode, address ->
                navController.navigate("dashboard/$mode/$address")
            })
        }
        composable(
            route = "dashboard/{mode}/{address}",
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("address") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "wifi"
            val address = backStackEntry.arguments?.getString("address") ?: ""
            DashboardScreen(
                mode = mode,
                address = address,
                onNavigateToDtc = { navController.navigate("dtc/$mode/$address") },
                onDisconnect = { navController.popBackStack() }
            )
        }
        composable(
            route = "dtc/{mode}/{address}",
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("address") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "wifi"
            val address = backStackEntry.arguments?.getString("address") ?: ""
            DtcScreen(
                mode = mode,
                address = address,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
