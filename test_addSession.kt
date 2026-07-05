import androidx.media3.session.MediaSessionService
class Test : MediaSessionService() {
    override fun onGetSession(c: androidx.media3.session.MediaSession.ControllerInfo) = null
    fun test(s: androidx.media3.session.MediaSession) {
        addSession(s)
    }
}
