import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target = """        // Start the service for MediaSession features
        val intent = android.content.Intent(context, com.example.service.PlaybackService::class.java)
        try {
            context.startService(intent)
        } catch(e: Exception) {}"""

replacement = """        // Start the service for MediaSession features
        val intent = android.content.Intent(context, com.example.service.PlaybackService::class.java)
        intent.data = decodedUri
        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startService(intent)
        } catch(e: Exception) {}"""

content = content.replace(target, replacement)
with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
