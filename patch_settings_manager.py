import re

with open("app/src/main/java/com/example/data/SettingsManager.kt", "r") as f:
    content = f.read()

target = """    var defaultAudioBackgroundPlay: Boolean
        get() = prefs.getBoolean("default_audio_background_play", true)
        set(value) = prefs.edit().putBoolean("default_audio_background_play", value).apply()"""

replacement = """    var defaultAudioBackgroundPlay: Boolean
        get() = prefs.getBoolean("default_audio_background_play", true)
        set(value) = prefs.edit().putBoolean("default_audio_background_play", value).apply()

    var decoderPriority: Int
        get() = prefs.getInt("decoder_priority", 1) // 0: Device Only, 1: Prefer Device, 2: Prefer App
        set(value) = prefs.edit().putInt("decoder_priority", value).apply()"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/data/SettingsManager.kt", "w") as f:
    f.write(content)
