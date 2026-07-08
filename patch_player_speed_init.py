import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target = """    var playbackSpeed by remember { mutableFloatStateOf(1f) }"""
replacement = """    val settingsManager = com.example.data.SettingsManager.getInstance(context)
    val decodedUriStringForInit = remember(uriString) { String(android.util.Base64.decode(uriString, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)) }
    var playbackSpeed by remember { mutableFloatStateOf(settingsManager.getPlaybackSpeed(decodedUriStringForInit)) }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
