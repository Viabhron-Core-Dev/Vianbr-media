import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

content = content.replace(
    "rememberModalBottomSheetState(skipPartiallyExpanded = false)",
    "rememberModalBottomSheetState(skipPartiallyExpanded = true)"
)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
