import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target1 = """                                playbackSpeed = Math.round(newSpeed * 10.0f) / 10.0f
                                mediaController?.setPlaybackSpeed(playbackSpeed)"""
replacement1 = """                                playbackSpeed = Math.round(newSpeed * 10.0f) / 10.0f
                                mediaController?.setPlaybackSpeed(playbackSpeed)
                                settingsManager.savePlaybackSpeed(decodedUriString, playbackSpeed)"""

target2 = """                                playbackSpeed = newSpeed
                                mediaController?.setPlaybackSpeed(newSpeed)"""
replacement2 = """                                playbackSpeed = newSpeed
                                mediaController?.setPlaybackSpeed(newSpeed)
                                settingsManager.savePlaybackSpeed(decodedUriString, newSpeed)"""

target3 = """                            playbackSpeed = 1.0f
                            mediaController?.setPlaybackSpeed(playbackSpeed)"""
replacement3 = """                            playbackSpeed = 1.0f
                            mediaController?.setPlaybackSpeed(playbackSpeed)
                            settingsManager.savePlaybackSpeed(decodedUriString, playbackSpeed)"""

target4 = """                                        playbackSpeed = speed
                                        mediaController?.setPlaybackSpeed(playbackSpeed)"""
replacement4 = """                                        playbackSpeed = speed
                                        mediaController?.setPlaybackSpeed(playbackSpeed)
                                        settingsManager.savePlaybackSpeed(decodedUriString, playbackSpeed)"""

content = content.replace(target1, replacement1)
content = content.replace(target2, replacement2)
content = content.replace(target3, replacement3)
content = content.replace(target4, replacement4)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
