import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

content = content.replace("Modifier.padding(16.dp)", "Modifier.padding(8.dp)")
content = content.replace("Modifier.height(16.dp)", "Modifier.height(8.dp)")
content = content.replace("Modifier.height(24.dp)", "Modifier.height(12.dp)")
content = content.replace("Modifier.padding(vertical = 12.dp)", "Modifier.padding(vertical = 8.dp)")
content = content.replace("Modifier.padding(vertical = 16.dp)", "Modifier.padding(vertical = 8.dp)")
content = content.replace("Modifier.heightIn(max = 300.dp)", "Modifier.heightIn(max = 200.dp)")

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
