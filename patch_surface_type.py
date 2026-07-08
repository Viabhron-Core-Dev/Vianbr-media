import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target = """        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    fitsSystemWindows = false
                    setOnApplyWindowInsetsListener { _, insets -> insets }
                    playerViewRef.value = this
                }"""

replacement = """        AndroidView(
            factory = { ctx ->
                val playerView = PlayerView(ctx)
                // Use TextureView to prevent SurfaceView detach timeout crashes
                val field = playerView.javaClass.getDeclaredField("surfaceView")
                field.isAccessible = true
                
                // Wait, PlayerView has no easy way to set surface_type programmatically.
                // But we can inflate it from XML or just not do this.
                // Instead, we can use app:surface_type="texture_view" in XML.
"""

# Let's check how to set surface type.
