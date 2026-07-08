with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target = """                        PlaybackProgressRow(
                            mediaController = mediaController, 
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        )"""

replacement = """                        PlaybackProgressRow(
                            mediaController = mediaController,
                            abRepeatStart = abRepeatStart,
                            abRepeatEnd = abRepeatEnd,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        )"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
