import re

with open("app/src/main/java/com/example/service/PlaybackService.kt", "r") as f:
    content = f.read()

target1 = """    private val inactivityHandler = Handler(Looper.getMainLooper())
    private val INACTIVITY_TIMEOUT_MS = 5 * 60 * 1000L // 5 mins
    private val releaseRunnable = Runnable {
        com.example.LogKeeper.log("Inactivity timeout reached, releasing ExoPlayer hardware resources.", "PlaybackService")
        PlayerManager.exoPlayer?.run {
            if (!playWhenReady) {
                stop()
                clearMediaItems()
            }
        }
    }"""

replacement1 = """    // Removed inactivity timeout"""

target2 = """            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady) {
                    inactivityHandler.postDelayed(releaseRunnable, INACTIVITY_TIMEOUT_MS)
                } else {
                    inactivityHandler.removeCallbacks(releaseRunnable)
                }
                
                if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {"""

replacement2 = """            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {"""

content = content.replace(target1, replacement1)
content = content.replace(target2, replacement2)

with open("app/src/main/java/com/example/service/PlaybackService.kt", "w") as f:
    f.write(content)
