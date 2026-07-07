import re

with open("app/src/main/java/com/example/service/PlayerManager.kt", "r") as f:
    content = f.read()

target = """            .setHandleAudioBecomingNoisy(true)
            .setSkipSilenceEnabled(skipSilence)
            .build()
        """

replacement = """            .setHandleAudioBecomingNoisy(true)
            .setSkipSilenceEnabled(skipSilence)
            .build()
        
        exoPlayer?.pauseAtEndOfMediaItems = true
        """

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/service/PlayerManager.kt", "w") as f:
    f.write(content)
