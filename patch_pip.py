import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target = """        val pipListener = object : androidx.media3.common.Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        val width = videoSize.width
                        val height = videoSize.height
                        if (width > 0 && height > 0) {
                            val aspect = width.toFloat() / height.toFloat()
                            val validAspect = aspect.coerceIn(10000f/23900f, 23900f/10000f)
                            context.findActivity()?.setPictureInPictureParams(
                                PictureInPictureParams.Builder()
                                    .setAutoEnterEnabled(true)
                                    .setAspectRatio(android.util.Rational((validAspect * 10000).toInt(), 10000))
                                    .build()
                            )
                        }
                    } catch(e: Exception) {}
                }
            }
        }
        controller.addListener(pipListener)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.findActivity()?.setPictureInPictureParams(
                PictureInPictureParams.Builder()
                    .setAutoEnterEnabled(true)
                    .build()
            )
        }"""

replacement = """        val pipListener = object : androidx.media3.common.Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                PipHelper.updatePipParams(context, controller, videoSize.width, videoSize.height)
            }
            override fun onEvents(player: androidx.media3.common.Player, events: androidx.media3.common.Player.Events) {
                if (events.contains(androidx.media3.common.Player.EVENT_IS_PLAYING_CHANGED) || events.contains(androidx.media3.common.Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                    PipHelper.updatePipParams(context, player, player.videoSize.width, player.videoSize.height)
                }
            }
        }
        controller.addListener(pipListener)
        PipHelper.updatePipParams(context, controller, controller.videoSize.width, controller.videoSize.height)
        
        val pipReceiver = PipActionReceiver(controller)
        val filter = android.content.IntentFilter(PipActionReceiver.ACTION_PIP_CONTROL)
        androidx.core.content.ContextCompat.registerReceiver(
            context,
            pipReceiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )
"""

target_dispose = """        onDispose {
            controller.removeListener(mainListener)
            controller.removeListener(pipListener)
        }"""

replacement_dispose = """        onDispose {
            controller.removeListener(mainListener)
            controller.removeListener(pipListener)
            try {
                context.unregisterReceiver(pipReceiver)
            } catch(e: Exception) {}
        }"""

content = content.replace(target, replacement)
content = content.replace(target_dispose, replacement_dispose)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
