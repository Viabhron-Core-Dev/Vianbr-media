with open("app/src/main/java/com/example/data/SettingsManager.kt", "r") as f:
    content = f.read()

new_prop = """    var audioBoosterEnabled: Boolean
        get() = prefs.getBoolean("audio_booster_enabled", true)
        set(value) = prefs.edit().putBoolean("audio_booster_enabled", value).apply()
        
"""

content = content.replace("    var boostGainMb: Int", new_prop + "    var boostGainMb: Int")

with open("app/src/main/java/com/example/data/SettingsManager.kt", "w") as f:
    f.write(content)
