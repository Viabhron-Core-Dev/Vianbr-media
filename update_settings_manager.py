import re

with open("app/src/main/java/com/example/data/SettingsManager.kt", "r") as f:
    content = f.read()

target = """    fun savePlaybackState(uri: String, position: Long, duration: Long) {
        prefs.edit()
            .putLong("pos_$uri", position)
            .putLong("dur_$uri", duration)
            .putLong("time_$uri", System.currentTimeMillis())
            .apply()
    }"""

replacement = """    fun savePlaybackState(uri: String, position: Long, duration: Long) {
        prefs.edit()
            .putLong("pos_$uri", position)
            .putLong("dur_$uri", duration)
            .putLong("time_$uri", System.currentTimeMillis())
            .apply()
    }

    fun savePlaybackSpeed(uri: String, speed: Float) {
        prefs.edit().putFloat("speed_$uri", speed).apply()
    }

    fun getPlaybackSpeed(uri: String): Float {
        return prefs.getFloat("speed_$uri", 1.0f)
    }

    fun saveTrackSelection(uri: String, trackType: Int, trackIndex: Int) {
        prefs.edit().putInt("track_${trackType}_$uri", trackIndex).apply()
    }

    fun getTrackSelection(uri: String, trackType: Int): Int {
        return prefs.getInt("track_${trackType}_$uri", -1)
    }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/data/SettingsManager.kt", "w") as f:
    f.write(content)
