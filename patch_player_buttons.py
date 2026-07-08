import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target = """                            // Center alignment
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

replacement = """                            // Center alignment
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                            
                                IconButton(
                                    onClick = {
                                        mediaController?.let { controller ->
                                            if (controller.hasPreviousMediaItem()) {
                                                controller.seekToPreviousMediaItem()
                                            } else {
                                                controller.seekTo(0)
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipPrevious,
                                        contentDescription = "Previous",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
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
                                
                                IconButton(
                                    onClick = {
                                        mediaController?.let { controller ->
                                            if (controller.hasNextMediaItem()) {
                                                controller.seekToNextMediaItem()
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipNext,
                                        contentDescription = "Next",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                            }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
