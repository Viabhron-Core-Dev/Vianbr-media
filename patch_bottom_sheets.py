import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

# Replace showAudioDialog
audio_dialog_pattern = r"if \(showAudioDialog\) \{.*?Dialog\(onDismissRequest = \{ showAudioDialog = false \}\) \{.*?Card\([^\{]+\{.*?Column\(.*?\) \{(.*?)\}\s+\}\s+\}\s+\}"
audio_sheet = """if (showAudioDialog) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showAudioDialog = false },
            sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {\\1}
        }
    }"""
content = re.sub(audio_dialog_pattern, audio_sheet, content, flags=re.DOTALL)

# Replace showSubtitleDialog
subtitle_dialog_pattern = r"if \(showSubtitleDialog\) \{.*?Dialog\(onDismissRequest = \{ showSubtitleDialog = false \}\) \{.*?Card\([^\{]+\{.*?Column\(.*?\) \{(.*?)\}\s+\}\s+\}\s+\}"
subtitle_sheet = """if (showSubtitleDialog) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showSubtitleDialog = false },
            sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {\\1}
        }
    }"""
content = re.sub(subtitle_dialog_pattern, subtitle_sheet, content, flags=re.DOTALL)

# Replace showSpeedDialog
speed_dialog_pattern = r"if \(showSpeedDialog\) \{.*?Dialog\(onDismissRequest = \{ showSpeedDialog = false \}\) \{.*?Card\([^\{]+\{.*?Column\(.*?\) \{(.*?)\}\s+\}\s+\}\s+\}"
speed_sheet = """if (showSpeedDialog) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showSpeedDialog = false },
            sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {\\1}
        }
    }"""
content = re.sub(speed_dialog_pattern, speed_sheet, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)

