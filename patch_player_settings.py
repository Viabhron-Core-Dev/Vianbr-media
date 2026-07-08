import re

with open("app/src/main/java/com/example/ui/screens/PlayerSettingsScreen.kt", "r") as f:
    content = f.read()

target = """                Switch(
                    checked = defaultAudioBackgroundPlay,
                    onCheckedChange = { 
                        defaultAudioBackgroundPlay = it
                        settingsManager.defaultAudioBackgroundPlay = it
                    }
                )
            }
        }
    }
}"""

replacement = """                Switch(
                    checked = defaultAudioBackgroundPlay,
                    onCheckedChange = { 
                        defaultAudioBackgroundPlay = it
                        settingsManager.defaultAudioBackgroundPlay = it
                    }
                )
            }
            
            var decoderPriority by remember { mutableStateOf(settingsManager.decoderPriority) }
            var showDecoderPriorityDialog by remember { mutableStateOf(false) }
            val decoderPriorityOptions = listOf("Device Only", "Prefer Device", "Prefer App")

            Spacer(modifier = Modifier.height(16.dp))
            Text("Advanced", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .androidx.compose.foundation.clickable { showDecoderPriorityDialog = true }
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Hardware Decoder Priority",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Current: ${decoderPriorityOptions.getOrNull(decoderPriority) ?: "Prefer Device"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (showDecoderPriorityDialog) {
                AlertDialog(
                    onDismissRequest = { showDecoderPriorityDialog = false },
                    title = { Text("Hardware Decoder Priority") },
                    text = {
                        Column {
                            decoderPriorityOptions.forEachIndexed { index, title ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .androidx.compose.foundation.clickable {
                                            decoderPriority = index
                                            settingsManager.decoderPriority = index
                                            showDecoderPriorityDialog = false
                                        }
                                        .padding(vertical = 12.dp)
                                ) {
                                    RadioButton(
                                        selected = decoderPriority == index,
                                        onClick = {
                                            decoderPriority = index
                                            settingsManager.decoderPriority = index
                                            showDecoderPriorityDialog = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(title)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showDecoderPriorityDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/PlayerSettingsScreen.kt", "w") as f:
    f.write(content)
