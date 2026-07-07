import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

# 1. Add state variables
state_vars = """    var showPlayPauseFlash by remember { mutableStateOf(false) }
    var flashIsPlaying by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    
    var abRepeatStart by remember { mutableStateOf<Long?>(null) }
    var abRepeatEnd by remember { mutableStateOf<Long?>(null) }
    var sleepTimerEndTime by remember { mutableStateOf<Long?>(null) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(mediaController) {
        while (true) {
            isPlaying = mediaController?.isPlaying == true
            
            // A-B repeat check
            if (abRepeatStart != null && abRepeatEnd != null && isPlaying) {
                val currentPos = mediaController?.currentPosition ?: 0L
                if (currentPos >= abRepeatEnd!!) {
                    mediaController?.seekTo(abRepeatStart!!)
                }
            }
            
            // Sleep timer check
            if (sleepTimerEndTime != null && isPlaying) {
                if (System.currentTimeMillis() >= sleepTimerEndTime!!) {
                    mediaController?.pause()
                    sleepTimerEndTime = null
                }
            }
            
            kotlinx.coroutines.delay(300)
        }
    }"""

content = re.sub(
    r'    var showPlayPauseFlash by remember \{ mutableStateOf\(false\) \}.*?LaunchedEffect\(mediaController\) \{.*?while \(true\) \{.*?isPlaying = mediaController\?\.isPlaying == true.*?kotlinx\.coroutines\.delay\(300\).*?\}.*?\}',
    state_vars,
    content,
    flags=re.DOTALL
)

# 2. Add Icons to the row below topbar
target_row = """                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {"""

replacement_row = """                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            // A-B Repeat Icon
                            IconButton(onClick = {
                                val currentPos = mediaController?.currentPosition ?: 0L
                                if (abRepeatStart == null) {
                                    abRepeatStart = currentPos
                                } else if (abRepeatEnd == null) {
                                    abRepeatEnd = currentPos
                                } else {
                                    abRepeatStart = null
                                    abRepeatEnd = null
                                }
                            }) {
                                val tint = if (abRepeatStart != null) Color(0xFF2196F3) else Color.White
                                val text = if (abRepeatStart != null && abRepeatEnd == null) "A" else if (abRepeatEnd != null) "A-B" else ""
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Loop, contentDescription = "A-B Repeat", tint = tint)
                                    if (text.isNotEmpty()) {
                                        Text(
                                            text = text,
                                            color = tint,
                                            fontSize = 8.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            modifier = Modifier.align(Alignment.BottomEnd)
                                        )
                                    }
                                }
                            }
                            
                            // Sleep Timer Icon
                            IconButton(onClick = { showSleepTimerDialog = true }) {
                                Icon(Icons.Filled.Timer, contentDescription = "Sleep Timer", tint = if (sleepTimerEndTime != null) Color(0xFF2196F3) else Color.White)
                            }"""

content = content.replace(target_row, replacement_row)

# 3. Add Sleep Timer Dialog
dialog = """            if (showPlaylistDialog) {
                // ... handled already
            }"""

dialog_replacement = """            if (showSleepTimerDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showSleepTimerDialog = false },
                    title = { Text("Sleep Timer") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val options = listOf(
                                "Off" to null,
                                "15 minutes" to 15 * 60 * 1000L,
                                "30 minutes" to 30 * 60 * 1000L,
                                "60 minutes" to 60 * 60 * 1000L,
                                "End of video" to (mediaController?.duration?.let { dur -> if (dur > 0) (dur - (mediaController?.currentPosition ?: 0)) else null })
                            )
                            options.forEach { (label, duration) ->
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        if (duration == null) {
                                            sleepTimerEndTime = null
                                        } else {
                                            sleepTimerEndTime = System.currentTimeMillis() + duration
                                        }
                                        showSleepTimerDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(label, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = { showSleepTimerDialog = false }) { Text("Close", color = Color(0xFF2196F3)) }
                    }
                )
            }
            
            if (showPlaylistDialog) {"""

content = content.replace("            if (showPlaylistDialog) {", dialog_replacement)

# 4. Add missing imports
imports = """import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Timer"""

content = content.replace("import androidx.compose.material.icons.filled.Warning", imports)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)

