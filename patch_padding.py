import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

content = content.replace("Column(modifier = Modifier.padding(24.dp)) {", "Column(modifier = Modifier.padding(16.dp)) {")
content = content.replace("Column(\n                modifier = Modifier.padding(24.dp),\n                horizontalAlignment = Alignment.CenterHorizontally\n            ) {", "Column(\n                modifier = Modifier.padding(16.dp),\n                horizontalAlignment = Alignment.CenterHorizontally\n            ) {")

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
