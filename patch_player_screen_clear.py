import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target = """                if (!backgroundPlayEnabledRef.value) {
                    controller.stop()
                    controller.clearMediaItems()
                }"""

replacement = """                if (!backgroundPlayEnabledRef.value) {
                    controller.clearMediaItems()
                    controller.stop()
                }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
