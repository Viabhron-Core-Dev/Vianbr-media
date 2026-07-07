import re

with open("app/src/main/java/com/example/service/PlaybackService.kt", "r") as f:
    content = f.read()

target = """            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady) {
                    inactivityHandler.postDelayed(releaseRunnable, INACTIVITY_TIMEOUT_MS)
                } else {
                    inactivityHandler.removeCallbacks(releaseRunnable)
                }
            }"""

replacement = """            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady) {
                    inactivityHandler.postDelayed(releaseRunnable, INACTIVITY_TIMEOUT_MS)
                } else {
                    inactivityHandler.removeCallbacks(releaseRunnable)
                }
                
                if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                    val player = PlayerManager.exoPlayer
                    if (player?.repeatMode == Player.REPEAT_MODE_OFF) {
                        player.stop()
                        player.clearMediaItems()
                        stopSelf()
                    }
                }
            }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/service/PlaybackService.kt", "w") as f:
    f.write(content)
