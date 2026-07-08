import re

with open("app/src/main/java/com/example/ui/screens/PipHelper.kt", "r") as f:
    content = f.read()

target = """    fun updatePipParams(context: Context, player: Player?, width: Int = 0, height: Int = 0) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val builder = PictureInPictureParams.Builder()
            
            if (width > 0 && height > 0) {
                val aspect = width.toFloat() / height.toFloat()
                val validAspect = aspect.coerceIn(10000f/23900f, 23900f/10000f)
                builder.setAspectRatio(android.util.Rational((validAspect * 10000).toInt(), 10000))
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setAutoEnterEnabled(true)
            }
            
            val actions = mutableListOf<RemoteAction>()
            
            if (player != null) {
                actions.add(
                    createRemoteAction(
                        context,
                        R.drawable.ic_pip_prev,
                        "Previous",
                        PipActionReceiver.CONTROL_TYPE_PREV
                    )
                )

                if (player.isPlaying) {
                    actions.add(
                        createRemoteAction(
                            context,
                            R.drawable.ic_pip_pause,
                            "Pause",
                            PipActionReceiver.CONTROL_TYPE_PAUSE
                        )
                    )
                } else {
                    actions.add(
                        createRemoteAction(
                            context,
                            R.drawable.ic_pip_play,
                            "Play",
                            PipActionReceiver.CONTROL_TYPE_PLAY
                        )
                    )
                }

                actions.add(
                    createRemoteAction(
                        context,
                        R.drawable.ic_pip_next,
                        "Next",
                        PipActionReceiver.CONTROL_TYPE_NEXT
                    )
                )
            }
            
            builder.setActions(actions)
            
            try {
                val activity = (context as? android.app.Activity) ?: return
                activity.setPictureInPictureParams(builder.build())
            } catch (e: Exception) {
                com.example.LogKeeper.logError("PipHelper", "Error setting PIP params: ${e.message}", e)
            }
        }
    }"""

replacement = """    @RequiresApi(Build.VERSION_CODES.O)
    fun buildPipParams(context: Context, player: Player?, width: Int = 0, height: Int = 0): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
        
        if (width > 0 && height > 0) {
            val aspect = width.toFloat() / height.toFloat()
            val validAspect = aspect.coerceIn(10000f/23900f, 23900f/10000f)
            builder.setAspectRatio(android.util.Rational((validAspect * 10000).toInt(), 10000))
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(true)
        }
        
        val actions = mutableListOf<RemoteAction>()
        
        if (player != null) {
            actions.add(
                createRemoteAction(
                    context,
                    R.drawable.ic_pip_prev,
                    "Previous",
                    PipActionReceiver.CONTROL_TYPE_PREV
                )
            )

            if (player.isPlaying) {
                actions.add(
                    createRemoteAction(
                        context,
                        R.drawable.ic_pip_pause,
                        "Pause",
                        PipActionReceiver.CONTROL_TYPE_PAUSE
                    )
                )
            } else {
                actions.add(
                    createRemoteAction(
                        context,
                        R.drawable.ic_pip_play,
                        "Play",
                        PipActionReceiver.CONTROL_TYPE_PLAY
                    )
                )
            }

            actions.add(
                createRemoteAction(
                    context,
                    R.drawable.ic_pip_next,
                    "Next",
                    PipActionReceiver.CONTROL_TYPE_NEXT
                )
            )
        }
        
        builder.setActions(actions)
        return builder.build()
    }

    fun updatePipParams(context: Context, player: Player?, width: Int = 0, height: Int = 0) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val activity = (context as? android.app.Activity) ?: return
                activity.setPictureInPictureParams(buildPipParams(context, player, width, height))
            } catch (e: Exception) {
                com.example.LogKeeper.logError("PipHelper", "Error setting PIP params: ${e.message}", e)
            }
        }
    }"""

content = content.replace(target, replacement)
with open("app/src/main/java/com/example/ui/screens/PipHelper.kt", "w") as f:
    f.write(content)
