import re

with open("app/src/main/java/com/example/service/PlaybackService.kt", "r") as f:
    content = f.read()

target = """        val player = mediaSession?.player
        if (player != null && !player.playWhenReady) {
            player.stop()
            stopSelf()
        }"""

replacement = """        val player = mediaSession?.player
        if (player != null && (!player.playWhenReady || player.mediaItemCount == 0 || player.playbackState == androidx.media3.common.Player.STATE_ENDED)) {
            player.stop()
            stopSelf()
        }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/service/PlaybackService.kt", "w") as f:
    f.write(content)
