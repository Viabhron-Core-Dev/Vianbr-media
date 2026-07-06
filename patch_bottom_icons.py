import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target = r"                            Row\(modifier = Modifier\.align\(Alignment\.CenterEnd\)\) \{"
replacement = """                            Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                                IconButton(onClick = {
                                    val nextMode = when (repeatMode) {
                                        androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
                                        androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
                                        else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                                    }
                                    repeatMode = nextMode
                                    mediaController?.repeatMode = nextMode
                                }) {
                                    val repeatIcon = when (repeatMode) {
                                        androidx.media3.common.Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
                                        else -> Icons.Filled.Repeat
                                    }
                                    val repeatTint = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_OFF) Color.White else Color(0xFF2196F3)
                                    Icon(repeatIcon, contentDescription = "Repeat", tint = repeatTint)
                                }
                                IconButton(onClick = { 
                                    backgroundPlayEnabled = !backgroundPlayEnabled
                                    Toast.makeText(context, "Background play " + if (backgroundPlayEnabled) "enabled" else "disabled", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Filled.Headphones, contentDescription = "Background play", tint = if (backgroundPlayEnabled) Color(0xFF2196F3) else Color.White)
                                }"""

content = re.sub(target, replacement, content)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
