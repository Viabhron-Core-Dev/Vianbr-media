with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target = """        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {"""

replacement = """        Column(
            modifier = Modifier.fillMaxWidth().androidx.compose.foundation.layout.navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
