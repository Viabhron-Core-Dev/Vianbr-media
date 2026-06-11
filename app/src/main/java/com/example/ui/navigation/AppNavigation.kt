package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.SettingsManager
import com.example.ui.screens.MainScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.screens.PlayerScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import android.net.Uri

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = if (settingsManager.hasSeenWelcome) "main" else "welcome") {
        composable("welcome") {
            WelcomeScreen(
                onNavigateToMain = {
                    settingsManager.hasSeenWelcome = true
                    navController.navigate("main") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }
        composable("main") {
            MainScreen(
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToPlayer = { uri ->
                    val encodedUri = Uri.encode(uri)
                    navController.navigate("player/$encodedUri")
                }
            )
        }
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "player/{uri}",
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) { backStackEntry ->
            val uriString = backStackEntry.arguments?.getString("uri") ?: ""
            PlayerScreen(
                uriString = uriString,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
