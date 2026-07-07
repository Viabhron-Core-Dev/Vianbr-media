import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target1 = """                                IconButton(onClick = { mediaController?.seekToPrevious() }) {
                                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White)
                                }"""

target2 = """                                IconButton(onClick = { mediaController?.seekToNext() }) {
                                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White)
                                }"""

content = content.replace(target1, "")
content = content.replace(target2, "")

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
