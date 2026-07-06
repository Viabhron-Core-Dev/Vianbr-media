with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

content = content.replace("com.example.service.PlayerManager.setBoostGain(savedGain)", "com.example.service.PlayerManager.applyAudioBoosterSettings(settingsManager.audioBoosterEnabled, savedGain)")
content = content.replace("com.example.service.PlayerManager.setBoostGain(newBoost)", "com.example.service.PlayerManager.applyAudioBoosterSettings(settings.audioBoosterEnabled, newBoost)")
content = content.replace("com.example.service.PlayerManager.setBoostGain(newGain)", "com.example.service.PlayerManager.applyAudioBoosterSettings(settings.audioBoosterEnabled, newGain)")

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
