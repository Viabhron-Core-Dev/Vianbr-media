import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target = """            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {"""

replacement = """            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (reason == androidx.media3.common.Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                    if (controller.repeatMode == androidx.media3.common.Player.REPEAT_MODE_OFF) {
                        onNavigateBack()
                    }
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
