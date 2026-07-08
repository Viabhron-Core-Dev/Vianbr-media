import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

# Revert onDispose in AndroidView
content = content.replace("""        onDispose {
            controller.removeListener(mainListener)
            controller.removeListener(pipListener)
            playerViewRef.value?.player = null
        }""", """        onDispose {
            controller.removeListener(mainListener)
            controller.removeListener(pipListener)
        }""")

# Revert startService intent
target_intent = """        // Start the service for MediaSession features
        val intent = android.content.Intent(context, com.example.service.PlaybackService::class.java)
        intent.data = decodedUri
        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startService(intent)
        } catch(e: Exception) {}"""

replacement_intent = """        // Start the service for MediaSession features
        val intent = android.content.Intent(context, com.example.service.PlaybackService::class.java)
        try {
            context.startService(intent)
        } catch(e: Exception) {}"""

content = content.replace(target_intent, replacement_intent)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
