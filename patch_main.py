import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

target = """    override fun onCreate(savedInstanceState: Bundle?) {"""

replacement = """    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
            val isPlaying = com.example.service.PlayerManager.exoPlayer?.isPlaying == true
            if (isPlaying) {
                try {
                    val builder = android.app.PictureInPictureParams.Builder()
                    enterPictureInPictureMode(builder.build())
                } catch(e: Exception) {}
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
