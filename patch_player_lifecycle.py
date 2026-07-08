import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target = """                    val activity = context.findActivity()
                    val isPip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) activity?.isInPictureInPictureMode == true else false
                    // We don't call stop() here anymore. The PlaybackService will handle
                    // inactivity timeouts (5 mins) to release resources gracefully.
                }
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {"""

replacement = """                    val activity = context.findActivity()
                    val isPip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) activity?.isInPictureInPictureMode == true else false
                    // We don't call stop() here anymore. The PlaybackService will handle
                    // inactivity timeouts (5 mins) to release resources gracefully.
                    if (!isPip && !backgroundPlayEnabledRef.value) {
                        controller.pause()
                    }
                }
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
