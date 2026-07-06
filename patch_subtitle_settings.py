import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

# Let's add state variables for Subtitle Settings right after showSubtitleDialog
state_vars = """        var showSubtitleDialog by remember { mutableStateOf(false) }
        var showTopMenu by remember { mutableStateOf(false) }
        
        var subtitleColor by remember { mutableStateOf(androidx.compose.ui.graphics.Color.White) }
        var subtitleSize by remember { mutableStateOf(16f) }
        var subtitleDelay by remember { mutableStateOf(0f) }"""

content = content.replace("        var showSubtitleDialog by remember { mutableStateOf(false) }\n        var showTopMenu by remember { mutableStateOf(false) }", state_vars)

# Apply settings to PlayerView. We can do it inside the update block of AndroidView.
android_view_update = """            update = { view ->
                view.player = mediaController
                view.resizeMode = resizeMode
                view.subtitleView?.let { subtitleView ->
                    val colorInt = android.graphics.Color.argb(
                        (subtitleColor.alpha * 255).toInt(),
                        (subtitleColor.red * 255).toInt(),
                        (subtitleColor.green * 255).toInt(),
                        (subtitleColor.blue * 255).toInt()
                    )
                    val style = androidx.media3.ui.CaptionStyleCompat(
                        colorInt,
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                        androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                        android.graphics.Color.BLACK,
                        null
                    )
                    subtitleView.setStyle(style)
                    subtitleView.setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, subtitleSize)
                }
            },"""

content = re.sub(r"            update = \{ view ->\n                view\.player = mediaController\n                view\.resizeMode = resizeMode\n            \},", android_view_update, content)

# Now enhance the Subtitle BottomSheet to include tabs for "Tracks" and "Settings"
subtitle_sheet_replacement = """if (showSubtitleDialog) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showSubtitleDialog = false },
            sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            var selectedTab by remember { mutableStateOf(0) }
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                androidx.compose.material3.TabRow(selectedTabIndex = selectedTab) {
                    androidx.compose.material3.Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Tracks") })
                    androidx.compose.material3.Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Settings") })
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                if (selectedTab == 0) {
                    val currentTracks = mediaController?.currentTracks
                    val textGroups = currentTracks?.groups?.filter { it.type == androidx.media3.common.C.TRACK_TYPE_TEXT } ?: emptyList()
                    val trackSelectionParameters = mediaController?.trackSelectionParameters
                    val isTextDisabled = trackSelectionParameters?.disabledTrackTypes?.contains(androidx.media3.common.C.TRACK_TYPE_TEXT) ?: false

                    if (textGroups.isEmpty()) {
                        Text("No subtitles available", style = MaterialTheme.typography.bodyLarge)
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        val builder = mediaController?.trackSelectionParameters?.buildUpon()
                                        builder?.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true)
                                        builder?.clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_TEXT)
                                        builder?.let { mediaController?.trackSelectionParameters = it.build() }
                                        showSubtitleDialog = false
                                    }.padding(vertical = 12.dp)
                                ) {
                                    Text("Off", fontWeight = if (isTextDisabled) FontWeight.Bold else FontWeight.Normal, color = if (isTextDisabled) Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            
                            items(textGroups.size) { groupIndex ->
                                val group = textGroups[groupIndex]
                                for (trackIndex in 0 until group.length) {
                                    val format = group.getTrackFormat(trackIndex)
                                    val isSelected = !isTextDisabled && group.isSelected
                                    val title = format.language ?: format.label ?: "Track ${trackIndex + 1}"
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            val builder = mediaController?.trackSelectionParameters?.buildUpon()
                                            builder?.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                                            builder?.setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                                            builder?.let { mediaController?.trackSelectionParameters = it.build() }
                                            showSubtitleDialog = false
                                        }.padding(vertical = 12.dp)
                                    ) {
                                        Text(title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            subtitlePickerLauncher.launch("application/octet-stream")
                            showSubtitleDialog = false
                        }.padding(vertical = 12.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add More", tint = Color(0xFF2196F3))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Subtitle File...", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        Text("Subtitle Delay (ms): ${subtitleDelay.toInt()}", fontWeight = FontWeight.Bold)
                        androidx.compose.material3.Slider(
                            value = subtitleDelay,
                            onValueChange = { subtitleDelay = it },
                            valueRange = -5000f..5000f,
                            steps = 100
                        )
                        Text("Delay is not natively supported by this ExoPlayer version without reloading.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Font Size: ${subtitleSize.toInt()}sp", fontWeight = FontWeight.Bold)
                        androidx.compose.material3.Slider(
                            value = subtitleSize,
                            onValueChange = { subtitleSize = it },
                            valueRange = 8f..48f
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Subtitle Color", fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            val colors = listOf(Color.White, Color.Yellow, Color.Cyan, Color.Green, Color.Magenta)
                            colors.forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(2.dp, if (subtitleColor == c) Color(0xFF2196F3) else Color.Transparent, CircleShape)
                                        .clickable { subtitleColor = c }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }"""

# Find the old subtitle sheet
old_subtitle_sheet_pattern = r"if \(showSubtitleDialog\) \{.*?androidx\.compose\.material3\.ModalBottomSheet\(.*?onDismissRequest = \{ showSubtitleDialog = false \}.*?sheetState = androidx\.compose\.material3\.rememberModalBottomSheetState\(skipPartiallyExpanded = true\).*?\) \{.*?Column\(modifier = Modifier\.padding\(24\.dp\)\) \{.*?(?:Text\(\"Select Subtitles\".*?Row\(modifier = Modifier\.fillMaxWidth\(\), horizontalArrangement = Arrangement\.End\) \{.*?\}\s*\}|Text\(\"Select Subtitles\".*?androidx\.compose\.material3\.TextButton.*?\}\s*\}\s*\}\s*\}\s*\}\s*\})\s*\}\s*\}"

content = re.sub(old_subtitle_sheet_pattern, subtitle_sheet_replacement, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)

