import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

content = content.replace("sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)", "sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = false)")

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
