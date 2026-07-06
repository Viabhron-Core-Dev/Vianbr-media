package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    var defaultAudioBackgroundPlay by remember { mutableStateOf(settingsManager.defaultAudioBackgroundPlay) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Player Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            var audioBoosterEnabled by remember { mutableStateOf(settingsManager.audioBoosterEnabled) }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Audio Booster",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "App-wide setting to allow audio volume boosting",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = audioBoosterEnabled,
                    onCheckedChange = { 
                        audioBoosterEnabled = it
                        settingsManager.audioBoosterEnabled = it
                        com.example.service.PlayerManager.applyAudioBoosterSettings(it, settingsManager.boostGainMb)
                    }
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Background Audio Play",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Play audio files in the background by default",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = defaultAudioBackgroundPlay,
                    onCheckedChange = { 
                        defaultAudioBackgroundPlay = it
                        settingsManager.defaultAudioBackgroundPlay = it
                    }
                )
            }
        }
    }
}
