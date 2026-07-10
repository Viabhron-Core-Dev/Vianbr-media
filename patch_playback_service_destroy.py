import re

with open("app/src/main/java/com/example/service/PlaybackService.kt", "r") as f:
    content = f.read()

target = """    override fun onDestroy() {
        com.example.LogKeeper.log("onDestroy called.", "PlaybackService")
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        PlayerManager.release()
        inactivityHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }"""

replacement = """    override fun onDestroy() {
        com.example.LogKeeper.log("onDestroy called.", "PlaybackService")
        serviceScope.cancel()
        mediaSession?.run {
            // Do NOT release the player here, so it can survive activity recreation
            // and rapid back/forward navigation.
            release()
        }
        mediaSession = null
        inactivityHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/service/PlaybackService.kt", "w") as f:
    f.write(content)
