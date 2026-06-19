package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp

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
                    "main"
                }
            } else {
                val encodedUri = android.util.Base64.encodeToString(initialUris.first().toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                "player/$encodedUri"
            }
        } else if (settingsManager.hasSeenWelcome) "main" else "welcome"
    }

    var batchCompressionUris by remember { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(initialUris) {
        if (initialUris.size > 1) {
            val isImage = initialUris.first().let { uri ->
                val mimeType = context.contentResolver.getType(android.net.Uri.parse(uri))
                mimeType?.startsWith("image/") == true
            }
            if (isImage) {
                batchCompressionUris = initialUris
            }
        }
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
            val decodedUri = try {
                String(android.util.Base64.decode(uriString, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))
            } catch (e: Exception) { uriString }
            com.example.ui.screens.PhotoEditorScreen(
                uriString = decodedUri,
                onNavigateBack = { 
                    if (!navController.popBackStack()) {
                        (context as? android.app.Activity)?.finish()
                    }
                }
            )
        }
    }

    batchCompressionUris?.let { uris ->
        com.example.ui.components.CompressionOptionsDialog(
            uris = uris,
            onDismiss = { 
                batchCompressionUris = null
                if (initialUris.isNotEmpty()) { (context as? android.app.Activity)?.finish() }
            },
            onStartCompression = { urisToCompress, w, h ->
                val intent = android.content.Intent(context, com.example.service.CompressionService::class.java).apply {
                    putStringArrayListExtra("uris", java.util.ArrayList(urisToCompress))
                    putExtra("maxWidth", w)
                    putExtra("maxHeight", h)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                batchCompressionUris = null
            }
        )
    }

    if (com.example.service.CompressionStatus.isRunning) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { },
            title = { androidx.compose.material3.Text("Compressing Images") },
            text = {
                androidx.compose.foundation.layout.Column {
                    val total = com.example.service.CompressionStatus.totalFiles
                    val current = com.example.service.CompressionStatus.currentFile
                    val progressRatio = if (total > 0) current.toFloat() / total else 0f
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                    androidx.compose.material3.Text("$current / $total files processed", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {}
        )
    }
    
    var wasCompressing by remember { mutableStateOf(false) }
    LaunchedEffect(com.example.service.CompressionStatus.isRunning) {
        if (com.example.service.CompressionStatus.isRunning) {
            wasCompressing = true
        } else if (wasCompressing) {
            wasCompressing = false
            android.widget.Toast.makeText(context, "Compression complete!", android.widget.Toast.LENGTH_SHORT).show()
            if (initialUris.isNotEmpty()) {
                (context as? android.app.Activity)?.finish()
            }
        }
    }
}
