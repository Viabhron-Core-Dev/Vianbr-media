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
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.PlaylistDetailScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import android.net.Uri

@Composable
fun AppNavigation(initialUris: List<String> = emptyList()) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val navController = rememberNavController()
    
    val startDest = remember(initialUris) {
        if (initialUris.isNotEmpty()) {
            val isImage = initialUris.first().let { uri ->
                val mimeType = context.contentResolver.getType(android.net.Uri.parse(uri))
                mimeType?.startsWith("image/") == true
            }
            if (isImage) {
                if (initialUris.size == 1) {
                    val encodedUri = android.util.Base64.encodeToString(initialUris.first().toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                    "photo_editor/$encodedUri"
                } else {
                    // Start compression queue service and default back to main
                    val intent = android.content.Intent(context, com.example.service.CompressionService::class.java).apply {
                        putStringArrayListExtra("uris", java.util.ArrayList(initialUris))
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                    "main"
                }
            } else {
                val encodedUri = android.util.Base64.encodeToString(initialUris.first().toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                "player/$encodedUri"
            }
        } else if (settingsManager.hasSeenWelcome) "main" else "welcome"
    }
    
    NavHost(navController = navController, startDestination = startDest) {
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
                onNavigateToPlayer = { uri ->
                    val encodedUri = android.util.Base64.encodeToString(uri.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                    navController.navigate("player/$encodedUri")
                },
                onNavigateToPhotoEditor = { uri ->
                    val encodedUri = android.util.Base64.encodeToString(uri.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                    navController.navigate("photo_editor/$encodedUri")
                },
                onNavigateToPlaylists = {
                    navController.navigate("playlists")
                }
            )
        }
        composable("playlists") {
            PlaylistsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlaylistDetail = { id ->
                    navController.navigate("playlist/$id")
                }
            )
        }
        composable(
            route = "playlist/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            PlaylistDetailScreen(
                playlistId = id,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { uri ->
                    val encodedUri = android.util.Base64.encodeToString(uri.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                    navController.navigate("player/$encodedUri")
                }
            )
        }
        composable(
            route = "player/{uri}",
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) { backStackEntry ->
            val uriString = backStackEntry.arguments?.getString("uri") ?: ""
            PlayerScreen(
                uriString = uriString,
                onNavigateBack = { 
                    if (!navController.popBackStack()) {
                        (context as? android.app.Activity)?.finish()
                    }
                }
            )
        }
        composable(
            route = "photo_editor/{uri}",
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) { backStackEntry ->
            val uriString = backStackEntry.arguments?.getString("uri") ?: ""
            com.example.ui.screens.PhotoEditorScreen(
                uriString = uriString,
                onNavigateBack = { 
                    if (!navController.popBackStack()) {
                        (context as? android.app.Activity)?.finish()
                    }
                }
            )
        }
    }
}
