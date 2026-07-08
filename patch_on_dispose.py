with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target = """        onDispose {
            controller.removeListener(mainListener)
            controller.removeListener(pipListener)
        }"""

replacement = """        onDispose {
            controller.removeListener(mainListener)
            controller.removeListener(pipListener)
            playerViewRef.value?.player = null
        }"""

content = content.replace(target, replacement)
with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
