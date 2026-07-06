with open("app/src/main/java/com/example/ui/screens/PlayerSettingsScreen.kt", "r") as f:
    content = f.read()

new_setting = """            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Background Audio Play","""

audio_booster = """            var audioBoosterEnabled by remember { mutableStateOf(settingsManager.audioBoosterEnabled) }

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
            
"""

content = content.replace("            Row(\n                verticalAlignment = Alignment.CenterVertically,\n                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)\n            ) {\n                Column(modifier = Modifier.weight(1f)) {\n                    Text(\n                        text = \"Background Audio Play\",", audio_booster + new_setting)

with open("app/src/main/java/com/example/ui/screens/PlayerSettingsScreen.kt", "w") as f:
    f.write(content)
