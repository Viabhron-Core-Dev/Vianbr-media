import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target = """    var showPlayPauseFlash by remember { mutableStateOf(false) }
    var flashIsPlaying by remember { mutableStateOf(false) }"""

replacement = """    var showPlayPauseFlash by remember { mutableStateOf(false) }
    var flashIsPlaying by remember { mutableStateOf(false) }
    
    LaunchedEffect(showPlayPauseFlash) {
        if (showPlayPauseFlash) {
            kotlinx.coroutines.delay(400)
            showPlayPauseFlash = false
        }
    }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
