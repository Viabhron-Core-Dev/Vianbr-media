package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.LogKeeper
import com.example.data.SettingsManager

import com.example.ui.screens.MediaViewModel
import com.example.data.MediaFolder
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    var showPlayerSettingsDialog by remember { mutableStateOf(false) }
    
    val viewModel: MediaViewModel = viewModel()
    val mediaFolders by viewModel.mediaFolders.collectAsState()
    
    val excludedFolders by settingsManager.excludedFolders.collectAsState()
    val extensions by settingsManager.extensions.collectAsState()
    val showLoggerFab by settingsManager.showLoggerFab.collectAsState()
    val isLoggerEnabled by LogKeeper.isEnabled.collectAsState()
    val outputFolderUri by settingsManager.outputFolderUri.collectAsState()

    var showExcludeDialog by remember { mutableStateOf(false) }

    val dirPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(it, flags)
            settingsManager.setOutputFolderUri(it.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Storage Configuration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Output Folder", style = MaterialTheme.typography.labelLarge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(
                    outputFolderUri ?: "Default (Pictures/Video)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { dirPickerLauncher.launch(null) }) {
                    Text("Select")
                }
            }
            if (outputFolderUri != null) {
                TextButton(onClick = { settingsManager.setOutputFolderUri(null) }) {
                    Text("Reset to Default", color = MaterialTheme.colorScheme.error)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Excluded Folders (Hidden)", style = MaterialTheme.typography.labelLarge)
            
            if (excludedFolders.isEmpty()) {
                Text("No folders excluded", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
                ) {
                    items(excludedFolders.toList()) { bucketId ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text(
                                "Folder ID: $bucketId",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { settingsManager.removeExcludedFolder(bucketId) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Restore Folder", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = { showExcludeDialog = true }) {
                Text("Add Excluded Folder")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Media Configuration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Inclusion Extensions: ${extensions.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Player Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { showPlayerSettingsDialog = true }) {
                Text("Open Player Settings")
            }
            
            if (showPlayerSettingsDialog) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { showPlayerSettingsDialog = false },
                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    PlayerSettingsScreen(onNavigateBack = { showPlayerSettingsDialog = false })
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Developer Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text("Enable Background Logger", modifier = Modifier.weight(1f))
                Switch(checked = isLoggerEnabled, onCheckedChange = { LogKeeper.toggleLogger() })
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text("Show Logger FAB", modifier = Modifier.weight(1f))
                Switch(checked = showLoggerFab, onCheckedChange = { settingsManager.setShowLoggerFab(it) })
            }
        }
        
        if (showExcludeDialog) {
            AlertDialog(
                onDismissRequest = { showExcludeDialog = false },
                title = { Text("Select Folder to Exclude") },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(mediaFolders) { folder ->
                            TextButton(
                                onClick = { 
                                    settingsManager.addExcludedFolder(folder.id)
                                    showExcludeDialog = false 
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(folder.name, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Start)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showExcludeDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
