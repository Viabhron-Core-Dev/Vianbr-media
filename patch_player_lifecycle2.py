import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target = """            currentController?.let { controller ->
                val currentPos = controller.currentPosition
                val dur = controller.duration
                com.example.data.SettingsManager.getInstance(context).savePlaybackState(decodedUriString, currentPos, dur)
                if (!backgroundPlayEnabledRef.value) {
                    controller.pause()
                }
            }
        }"""

replacement = """            currentController?.let { controller ->
                val currentPos = controller.currentPosition
                val dur = controller.duration
                com.example.data.SettingsManager.getInstance(context).savePlaybackState(decodedUriString, currentPos, dur)
                if (!backgroundPlayEnabledRef.value) {
                    controller.stop()
                    controller.clearMediaItems()
                }
            }
        }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
