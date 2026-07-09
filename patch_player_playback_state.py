import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target_playback_state = """            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                    val currentMode = controller.repeatMode
                    val hasNext = controller.hasNextMediaItem()
                    if (currentMode == androidx.media3.common.Player.REPEAT_MODE_OFF && !hasNext) {
                        onNavigateBack()
                    }
                }
            }"""

replacement_playback_state = """            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                    val currentMode = controller.repeatMode
                    val hasNext = controller.hasNextMediaItem()
                    if (currentMode == androidx.media3.common.Player.REPEAT_MODE_OFF && !hasNext) {
                        if (!backgroundPlayEnabledRef.value) {
                            try {
                                context.stopService(android.content.Intent(context, com.example.service.PlaybackService::class.java))
                            } catch (e: Exception) {}
                        }
                        onNavigateBack()
                    }
                }
            }"""

content = content.replace(target_playback_state, replacement_playback_state)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
