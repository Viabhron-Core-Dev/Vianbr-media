import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

# Add wrapper
wrapper = """@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MyModalBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: androidx.compose.material3.SheetState,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        content = content
    )
}

"""

if "fun MyModalBottomSheet" not in content:
    content = content.replace("fun PlayerScreen(", wrapper + "fun PlayerScreen(")

# Replace all occurrences
content = content.replace("androidx.compose.material3.ModalBottomSheet", "MyModalBottomSheet")
content = content.replace("androidx.compose.material3.rememberModalBottomSheetState", "androidx.compose.material3.rememberModalBottomSheetState")

# Fix recursive call inside wrapper
content = content.replace("""@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MyModalBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: androidx.compose.material3.SheetState,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    MyModalBottomSheet(""", """@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MyModalBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: androidx.compose.material3.SheetState,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    androidx.compose.material3.ModalBottomSheet(""")


with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)

