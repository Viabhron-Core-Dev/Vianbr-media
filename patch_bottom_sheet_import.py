with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

content = content.replace("Modifier.fillMaxWidth().androidx.compose.foundation.layout.navigationBarsPadding()", "Modifier.fillMaxWidth().navigationBarsPadding()")

if "import androidx.compose.foundation.layout.navigationBarsPadding" not in content:
    content = content.replace("import androidx.compose.foundation.layout.Row", "import androidx.compose.foundation.layout.Row\nimport androidx.compose.foundation.layout.navigationBarsPadding")

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
