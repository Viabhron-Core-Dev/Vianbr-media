with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target = """    if (showPlayerSettingsDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showPlayerSettingsDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            com.example.ui.screens.PlayerSettingsScreen(onNavigateBack = { showPlayerSettingsDialog = false })
        }
    }
}"""

replacement = """    if (showPlayerSettingsDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showPlayerSettingsDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            com.example.ui.screens.PlayerSettingsScreen(onNavigateBack = { showPlayerSettingsDialog = false })
        }
    }

    if (showSleepTimerDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Sleep Timer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val options = listOf(
                        "Off" to null,
                        "15 minutes" to 15 * 60 * 1000L,
                        "30 minutes" to 30 * 60 * 1000L,
                        "60 minutes" to 60 * 60 * 1000L,
                        "End of video" to (mediaController?.duration?.let { dur -> if (dur > 0) (dur - (mediaController?.currentPosition ?: 0)) else null })
                    )
                    options.forEach { (label, duration) ->
                        androidx.compose.material3.TextButton(
                            onClick = {
                                if (duration == null) {
                                    sleepTimerEndTime = null
                                } else {
                                    sleepTimerEndTime = System.currentTimeMillis() + duration
                                }
                                showSleepTimerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showSleepTimerDialog = false }) { Text("Close", color = Color(0xFF2196F3)) }
            }
        )
    }
}"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
