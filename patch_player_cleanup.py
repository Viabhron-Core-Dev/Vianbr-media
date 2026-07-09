import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target_dispose = """                if (!backgroundPlayEnabledRef.value) {
                    controller.stop()
                    controller.clearMediaItems()
                }"""

replacement_dispose = """                if (!backgroundPlayEnabledRef.value) {
                    controller.stop()
                    controller.clearMediaItems()
                    try {
                        context.stopService(android.content.Intent(context, com.example.service.PlaybackService::class.java))
                    } catch (e: Exception) {
                        com.example.LogKeeper.logError("PlayerScreen", "Failed to stop PlaybackService", e)
                    }
                }"""

content = content.replace(target_dispose, replacement_dispose)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
