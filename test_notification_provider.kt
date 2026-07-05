import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaNotification
import androidx.media3.session.DefaultMediaNotificationProvider

class TestService : MediaSessionService() {
    override fun onGetSession(c: androidx.media3.session.MediaSession.ControllerInfo) = null
    override fun onCreate() {
        super.onCreate()
        val defaultProvider = DefaultMediaNotificationProvider(this)
        setMediaNotificationProvider(object : MediaNotification.Provider {
            override fun createNotification(
                session: androidx.media3.session.MediaSession,
                customLayout: com.google.common.collect.ImmutableList<androidx.media3.session.CommandButton>,
                actionFactory: MediaNotification.ActionFactory,
                onNotificationChangedCallback: MediaNotification.Provider.Callback
            ): MediaNotification {
                try {
                    val notification = defaultProvider.createNotification(session, customLayout, actionFactory, onNotificationChangedCallback)
                    com.example.LogKeeper.log("Created MediaNotification successfully.", "PlaybackService")
                    return notification
                } catch(e: Exception) {
                    com.example.LogKeeper.logError("PlaybackService", "Failed to create MediaNotification", e)
                    throw e
                }
            }
            override fun handleCustomCommand(
                session: androidx.media3.session.MediaSession,
                action: String,
                extras: android.os.Bundle
            ): Boolean {
                return defaultProvider.handleCustomCommand(session, action, extras)
            }
        })
    }
}
