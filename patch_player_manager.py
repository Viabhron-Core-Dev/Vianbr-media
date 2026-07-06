import re

with open("app/src/main/java/com/example/service/PlayerManager.kt", "r") as f:
    content = f.read()

# Add listener
listener_code = """        exoPlayer?.addListener(object : androidx.media3.common.Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    try {
                        loudnessEnhancer?.release()
                        loudnessEnhancer = LoudnessEnhancer(audioSessionId)
                        val settings = com.example.data.SettingsManager.getInstance(context.applicationContext)
                        if (settings.audioBoosterEnabled && settings.boostGainMb > 0) {
                            loudnessEnhancer?.setTargetGain(settings.boostGainMb)
                            loudnessEnhancer?.enabled = true
                        } else {
                            loudnessEnhancer?.enabled = false
                        }
                    } catch (e: Exception) {
                        com.example.LogKeeper.logError("PlayerManager", "Failed to create LoudnessEnhancer on session change", e)
                    }
                }
            }
        })

"""

content = content.replace("        exoPlayer?.audioSessionId?.let { sessionId ->", listener_code + "        exoPlayer?.audioSessionId?.let { sessionId ->")

# Update setBoostGain to use context for settings
set_boost = """    fun setBoostGain(gainMb: Int) {
        val appCtx = com.example.MyApplication.appContext
        val settings = if (appCtx != null) com.example.data.SettingsManager.getInstance(appCtx) else null
        val enabled = settings?.audioBoosterEnabled ?: true
        if (gainMb <= 0 || !enabled) {
            loudnessEnhancer?.enabled = false
        } else {
            loudnessEnhancer?.setTargetGain(gainMb)
            loudnessEnhancer?.enabled = true
        }
    }"""

content = re.sub(r"    fun setBoostGain\(gainMb: Int\) \{.*?\n    \}", set_boost, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/service/PlayerManager.kt", "w") as f:
    f.write(content)
