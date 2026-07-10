import re

with open("app/src/main/java/com/example/service/PlaybackService.kt", "r") as f:
    content = f.read()

target = """                if (playbackState == Player.STATE_ENDED) {
                    val player = PlayerManager.exoPlayer
                    if (player?.repeatMode == Player.REPEAT_MODE_OFF) {
                        stopSelf()
                    }
                }"""

replacement = """                if (playbackState == Player.STATE_ENDED) {
                    val player = PlayerManager.exoPlayer
                    if (player?.repeatMode == Player.REPEAT_MODE_OFF) {
                        stopSelf()
                    }
                } else if (playbackState == Player.STATE_IDLE) {
                    val player = PlayerManager.exoPlayer
                    if (player != null && player.mediaItemCount == 0) {
                        stopSelf()
                    }
                }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/service/PlaybackService.kt", "w") as f:
    f.write(content)
