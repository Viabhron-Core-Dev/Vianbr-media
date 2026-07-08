import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

# 1. Restore SkipPrevious and SkipNext
target_center = """                            // Center alignment
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                IconButton(
                                    onClick = {
                                        mediaController?.let { controller ->
                                            if (controller.playbackState == androidx.media3.common.Player.STATE_ENDED || controller.playbackState == androidx.media3.common.Player.STATE_IDLE) {
                                                controller.seekTo(0)
                                                controller.prepare()
                                                controller.play()
                                            } else if (controller.isPlaying) {
                                                controller.pause()
                                            } else {
                                                controller.play()
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }"""

replacement_center = """                            // Center alignment
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                IconButton(onClick = { mediaController?.seekToPrevious() }) {
                                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White)
                                }
                                IconButton(
                                    onClick = {
                                        mediaController?.let { controller ->
                                            if (controller.playbackState == androidx.media3.common.Player.STATE_ENDED || controller.playbackState == androidx.media3.common.Player.STATE_IDLE) {
                                                controller.seekTo(0)
                                                controller.prepare()
                                                controller.play()
                                            } else if (controller.isPlaying) {
                                                controller.pause()
                                            } else {
                                                controller.play()
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                IconButton(onClick = { mediaController?.seekToNext() }) {
                                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White)
                                }
                            }"""

content = content.replace(target_center, replacement_center)

# 2. Inject Sleep Timer Dialog at the very end of PlayerScreen
# Let's find the closing brace of PlayerScreen.
target_end = """        if (showPlayerSettingsDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showPlayerSettingsDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                com.example.ui.screens.PlayerSettingsScreen(onNavigateBack = { showPlayerSettingsDialog = false })
            }
        }
    }
}"""

replacement_end = """        if (showPlayerSettingsDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showPlayerSettingsDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                com.example.ui.screens.PlayerSettingsScreen(onNavigateBack = { showPlayerSettingsDialog = false })
            }
        }
        
        if (showSleepTimerDialog) {
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
    }
}"""

content = content.replace(target_end, replacement_end)

# 3. Replace brightness slider with a horizontal material slider
target_brightness = """                    androidx.compose.animation.AnimatedVisibility(
                        visible = showBrightnessSlider,
                        enter = androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.fadeOut(),
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 48.dp)
                    ) {
                        var brightness by remember { mutableFloatStateOf(context.findActivity()?.window?.attributes?.screenBrightness.takeIf { it != -1f } ?: 0.5f) }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                                .padding(horizontal = 12.dp, vertical = 24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(140.dp)
                                    .width(32.dp)
                                    .pointerInput(Unit) {
                                        var lastAppliedBrightness = -1f
                                        detectVerticalDragGestures(
                                            onVerticalDrag = { change, dragAmount ->
                                                change.consume()
                                                val dragRatio = -dragAmount / 140.dp.toPx()
                                                val newVal = (brightness + dragRatio).coerceIn(0f, 1f)
                                                brightnessInteractionTime = System.currentTimeMillis()
                                                brightness = newVal
                                                
                                                if (kotlin.math.abs(newVal - lastAppliedBrightness) > 0.02f) {
                                                    lastAppliedBrightness = newVal
                                                    val window = context.findActivity()?.window
                                                    window?.let {
                                                        val lp = it.attributes
                                                        lp.screenBrightness = newVal
                                                        it.attributes = lp
                                                    }
                                                }
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(brightness)
                                        .fillMaxWidth()
                                        .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                                )
                                Icon(Icons.Filled.LightMode, contentDescription = null, tint = if (brightness > 0.5f) Color.Black else Color.White, modifier = Modifier.padding(bottom = 8.dp).size(20.dp))
                            }
                        }
                    }"""

replacement_brightness = """                    androidx.compose.animation.AnimatedVisibility(
                        visible = showBrightnessSlider,
                        enter = androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.fadeOut(),
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 48.dp)
                    ) {
                        var brightness by remember { mutableFloatStateOf(context.findActivity()?.window?.attributes?.screenBrightness.takeIf { it != -1f } ?: 0.5f) }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                                .padding(horizontal = 12.dp, vertical = 24.dp)
                        ) {
                            Slider(
                                value = brightness,
                                onValueChange = { newVal ->
                                    brightnessInteractionTime = System.currentTimeMillis()
                                    brightness = newVal
                                    val window = context.findActivity()?.window
                                    window?.let {
                                        val lp = it.attributes
                                        lp.screenBrightness = newVal
                                        it.attributes = lp
                                    }
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier
                                    .height(140.dp)
                                    .androidx.compose.ui.graphics.graphicsLayer {
                                        rotationZ = 270f
                                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
                                    }
                                    .width(140.dp)
                            )
                            Icon(Icons.Filled.LightMode, contentDescription = null, tint = Color.White, modifier = Modifier.padding(top = 8.dp).size(20.dp))
                        }
                    }"""

content = content.replace(target_brightness, replacement_brightness)

if "import androidx.compose.material.icons.filled.SkipNext" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.PlayArrow", "import androidx.compose.material.icons.filled.PlayArrow\nimport androidx.compose.material.icons.filled.SkipNext\nimport androidx.compose.material.icons.filled.SkipPrevious")

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
