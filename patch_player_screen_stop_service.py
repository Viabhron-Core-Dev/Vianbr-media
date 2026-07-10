import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target1 = """                        if (!backgroundPlayEnabledRef.value) {
                            try {
                                context.stopService(android.content.Intent(context, com.example.service.PlaybackService::class.java))
                            } catch (e: Exception) {}
                        }"""

replacement1 = """                        // Service will stop itself when STATE_ENDED"""

target2 = """                if (!backgroundPlayEnabledRef.value) {
                    controller.stop()
                    controller.clearMediaItems()
                    try {
                        context.stopService(android.content.Intent(context, com.example.service.PlaybackService::class.java))
                    } catch (e: Exception) {
                        com.example.LogKeeper.logError("PlayerScreen", "Failed to stop PlaybackService", e)
                    }
                }"""

replacement2 = """                if (!backgroundPlayEnabledRef.value) {
                    controller.stop()
                    controller.clearMediaItems()
                }"""

content = content.replace(target1, replacement1)
content = content.replace(target2, replacement2)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
