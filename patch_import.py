import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

content = content.replace("import androidx.compose.foundation.layout.widthIn\n@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)\npackage com.example.ui.screens", "@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)\npackage com.example.ui.screens\nimport androidx.compose.foundation.layout.widthIn")

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
