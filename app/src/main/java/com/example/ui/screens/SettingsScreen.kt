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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.example.LogKeeper
import com.example.data.SettingsManager
import kotlinx.coroutines.launch

import com.example.ui.screens.MediaViewModel
import com.example.data.MediaFolder
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
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

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val backupStr = com.example.data.BackupManager.createBackup(context)
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        os.write(backupStr.toByteArray())
                    }
                    android.widget.Toast.makeText(context, "Backup saved successfully!", android.widget.Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    com.example.LogKeeper.logError("SettingsScreen", "Failed to save backup", e)
                    android.widget.Toast.makeText(context, "Failed to save backup", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val backupStr = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                        reader.readText()
                    }
                    if (backupStr != null) {
                        val success = com.example.data.BackupManager.restoreBackup(context, backupStr)
                        if (success) {
                            android.widget.Toast.makeText(context, "Backup restored successfully! Updating UI...", android.widget.Toast.LENGTH_LONG).show()
                            onNavigateBack()
                        } else {
                            android.widget.Toast.makeText(context, "Failed to restore backup (invalid format)", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    com.example.LogKeeper.logError("SettingsScreen", "Failed to read backup file", e)
                    android.widget.Toast.makeText(context, "Error reading backup file", android.widget.Toast.LENGTH_LONG).show()
                }
            }
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .imePadding()
                .navigationBarsPadding()
        ) {
            Text("Storage Configuration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Output Folder", style = MaterialTheme.typography.labelLarge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(
                    outputFolderUri ?: "Default (Downloads/Compressed)",
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
            Text("Included Extensions:", style = MaterialTheme.typography.labelLarge)
            
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                extensions.forEach { ext ->
                    InputChip(
                        selected = false,
                        onClick = { },
                        label = { Text(ext) },
                        trailingIcon = {
                            IconButton(
                                onClick = { 
                                    settingsManager.setExtensions(extensions.filter { it != ext }) 
                                },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(Icons.Filled.Clear, contentDescription = "Remove")
                            }
                        }
                    )
                }
            }
            
            var newExtension by remember { mutableStateOf("") }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = newExtension,
                    onValueChange = { newExtension = it },
                    label = { Text("Add extension (e.g. mkv)") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    val ext = newExtension.trim().removePrefix(".").lowercase()
                    if (ext.isNotEmpty() && !extensions.contains(ext)) {
                        settingsManager.setExtensions(extensions + ext)
                        newExtension = ""
                    }
                }) {
                    Text("Add")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Player Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { showPlayerSettingsDialog = true }) {
                Text("Open Player Settings")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            var showPriorityDialog by remember { mutableStateOf(false) }
            Button(onClick = { showPriorityDialog = true }) {
                Text("Notification Priority")
            }
            
            if (showPriorityDialog) {
                var selectedActions by remember { mutableStateOf(settingsManager.getNotificationPriority().toSet()) }
                AlertDialog(
                    onDismissRequest = { showPriorityDialog = false },
                    title = { Text("Notification Actions") },
                    text = {
                        Column {
                            Text("Select custom actions to show in the notification (Playback and Close are permanent):", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            val availableActions = listOf("Loop", "Playlist", "PiP")
                            availableActions.forEach { action ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (selectedActions.contains(action)) {
                                                selectedActions = selectedActions - action
                                            } else {
                                                selectedActions = selectedActions + action
                                            }
                                        }
                                        .padding(vertical = 8.dp)
                                ) {
                                    androidx.compose.material3.Checkbox(
                                        checked = selectedActions.contains(action),
                                        onCheckedChange = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(action)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { 
                            val toSave = selectedActions.toMutableList()
                            if (!toSave.contains("Close")) toSave.add("Close")
                            settingsManager.setNotificationPriority(toSave)
                            
                            val intent = android.content.Intent("com.example.ACTION_UPDATE_NOTIFICATION")
                            intent.setPackage(context.packageName)
                            context.sendBroadcast(intent)
                            
                            showPriorityDialog = false 
                        }) { Text("Save") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPriorityDialog = false }) { Text("Cancel") }
                    }
                )
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
            
            Text("Data Management", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Backup and Restore", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Export or import all application settings, excluded folders, preferred extensions, and custom sidebar elements/playlists structure to/from a separate backup file.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { createBackupLauncher.launch("vianbr_backup.json") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Backup (Export)")
                }
                FilledTonalButton(
                    onClick = { restoreBackupLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*")) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Restore (Import)")
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
            
            Spacer(modifier = Modifier.height(100.dp))
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
