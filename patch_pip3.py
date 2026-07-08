import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target = """                                IconButton(onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        try {
                                            val builder = PictureInPictureParams.Builder()
                                            val width = mediaController?.videoSize?.width
                                            val height = mediaController?.videoSize?.height
                                            if (width != null && height != null && width > 0 && height > 0) {
                                                val aspect = width.toFloat() / height.toFloat()
                                                val validAspect = aspect.coerceIn(10000f/23900f, 23900f/10000f)
                                                builder.setAspectRatio(android.util.Rational((validAspect * 10000).toInt(), 10000))
                                            }
                                            context.findActivity()?.enterPictureInPictureMode(builder.build())
                                        } catch (e: Exception) {
                                            // Ignore
                                        }
                                    }
                                }) {"""

replacement = """                                IconButton(onClick = {
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

content = content.replace(target, replacement)
with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
