import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

slider_block = """                    val settings = remember { com.example.data.SettingsManager.getInstance(context) }
                    if (settings.audioBoosterEnabled) {
                        Text(
                            text = "Volume Booster: " + if (boostGainMb == 0) "Off" else "+${boostGainMb / 100}dB",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = boostGainMb.toFloat(),
                            onValueChange = { newVal ->
                                val newGain = newVal.toInt()
                                boostGainMb = newGain
                                settings.boostGainMb = newGain
                                com.example.service.PlayerManager.applyAudioBoosterSettings(settings.audioBoosterEnabled, newGain)
                            },
                            valueRange = 0f..1500f,
                            steps = 14
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        Text(
                            text = "Volume Booster is disabled in Player Settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }"""

old_slider_pattern = r"                    Text\(\s*text = \"Volume Booster.*?steps = 14\s*\)\s*Spacer\(modifier = Modifier\.height\(16\.dp\)\)"

content = re.sub(old_slider_pattern, slider_block, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
