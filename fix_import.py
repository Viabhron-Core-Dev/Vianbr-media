import re

with open("app/src/main/java/com/example/ui/screens/PlayerSettingsScreen.kt", "r") as f:
    content = f.read()

content = content.replace("import androidx.compose.foundation.clickable\npackage com.example.ui.screens", "package com.example.ui.screens\nimport androidx.compose.foundation.clickable")

with open("app/src/main/java/com/example/ui/screens/PlayerSettingsScreen.kt", "w") as f:
    f.write(content)
