import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target = """        val savedGain = settingsManager.boostGainMb
        boostGainMb = savedGain
        if (savedGain > 0) {
            com.example.service.PlayerManager.applyAudioBoosterSettings(settingsManager.audioBoosterEnabled, savedGain)
        }"""
replacement = """        val savedGain = settingsManager.boostGainMb
        boostGainMb = savedGain
        if (savedGain > 0) {
            com.example.service.PlayerManager.applyAudioBoosterSettings(settingsManager.audioBoosterEnabled, savedGain)
        }
        controller.setPlaybackSpeed(playbackSpeed)"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
