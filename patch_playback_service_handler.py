import re

with open("app/src/main/java/com/example/service/PlaybackService.kt", "r") as f:
    content = f.read()

target = """        mediaSession = null
        inactivityHandler.removeCallbacksAndMessages(null)
        super.onDestroy()"""

replacement = """        mediaSession = null
        super.onDestroy()"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/service/PlaybackService.kt", "w") as f:
    f.write(content)
