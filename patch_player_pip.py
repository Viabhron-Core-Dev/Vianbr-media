import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target_pip = """                                IconButton(onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        try {
                                            val width = mediaController?.videoSize?.width ?: 0
                                            val height = mediaController?.videoSize?.height ?: 0
                                            val params = PipHelper.buildPipParams(context, mediaController, width, height)
                                            context.findActivity()?.enterPictureInPictureMode(params)
                                        } catch (e: Exception) {
                                            // Ignore
                                        }
                                    }
                                }) {"""

replacement_pip = """                                IconButton(onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        try {
                                            val width = mediaController?.videoSize?.width ?: 0
                                            val height = mediaController?.videoSize?.height ?: 0
                                            val params = PipHelper.buildPipParams(context, mediaController, width, height)
                                            val activity = context.findActivity()
                                            if (activity != null) {
                                                val entered = activity.enterPictureInPictureMode(params)
                                                com.example.LogKeeper.log("PiP enter result: $entered", "PlayerScreen")
                                                if (!entered) {
                                                    com.example.LogKeeper.logError("PlayerScreen", "enterPictureInPictureMode returned false", null)
                                                }
                                            } else {
                                                com.example.LogKeeper.logError("PlayerScreen", "Activity is null for PiP", null)
                                            }
                                        } catch (e: Exception) {
                                            com.example.LogKeeper.logError("PlayerScreen", "Exception entering PiP", e)
                                        }
                                    } else {
                                        com.example.LogKeeper.logError("PlayerScreen", "PiP not supported on this SDK", null)
                                    }
                                }) {"""

content = content.replace(target_pip, replacement_pip)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
