import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

content = content.replace("insetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())", "insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())")
content = content.replace("insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())", "insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())")

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
