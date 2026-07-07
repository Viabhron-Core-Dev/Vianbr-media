import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

content = content.replace("import androidx.compose.material.icons.filled.Screenshot", "import androidx.compose.material.icons.filled.Screenshot\nimport androidx.compose.material.icons.filled.SkipNext\nimport androidx.compose.material.icons.filled.SkipPrevious")

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
