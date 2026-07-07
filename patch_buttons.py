import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target = """                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                IconButton("""

replacement = """                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                IconButton(onClick = { mediaController?.seekToPrevious() }) {
                                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White)
                                }
                                IconButton("""

target2 = """                                    )
                                }
                            }
                            
                            // Right alignment"""

replacement2 = """                                    )
                                }
                                IconButton(onClick = { mediaController?.seekToNext() }) {
                                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White)
                                }
                            }
                            
                            // Right alignment"""

content = content.replace(target, replacement)
content = content.replace(target2, replacement2)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
