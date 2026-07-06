import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

content = content.replace("@Composable\n@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)\nfun MyModalBottomSheet(", "@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)\n@Composable\nfun MyModalBottomSheet(")

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)

