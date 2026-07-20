package com.example.service

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.Futures
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import coil.Coil
import coil.request.ImageRequest
import android.graphics.drawable.Drawable
import android.graphics.drawable.BitmapDrawable

import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class PlaybackService : MediaSessionService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Removed inactivity timeout

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(android.content.Context.WINDOW_SERVICE) as WindowManager
        
        try {
            val defaultProvider = androidx.media3.session.DefaultMediaNotificationProvider(this).apply {
                // Media3 DefaultMediaNotificationProvider automatically adds custom layout commands
            }
            setMediaNotificationProvider(defaultProvider)
        } catch (e: Exception) {
            com.example.LogKeeper.logError("PlaybackService", "Failed to set up MediaNotificationProvider", e)
        }
        
        val settings = com.example.data.SettingsManager.getInstance(this)
        PlayerManager.initialize(this, false)
        
        val filter = android.content.IntentFilter("com.example.ACTION_WIDGET_COMMAND")
        filter.addAction("com.example.ACTION_UPDATE_NOTIFICATION")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(widgetCommandReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(widgetCommandReceiver, filter)
        }

        PlayerManager.exoPlayer?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { updateWidgetUI() }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) { updateWidgetUI() }
            override fun onRepeatModeChanged(repeatMode: Int) { updateWidgetUI() }
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) { updateWidgetUI() }
            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) { updateWidgetUI() }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                val cause = error.cause?.message ?: "Unknown"
                com.example.LogKeeper.logError("PlaybackService", "Error: ${error.errorCodeName} - ${error.message} - Cause: $cause", error)
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                val stateName = when (playbackState) {
                    Player.STATE_IDLE -> "STATE_IDLE"
                    Player.STATE_BUFFERING -> "STATE_BUFFERING"
                    Player.STATE_READY -> "STATE_READY"
                    Player.STATE_ENDED -> "STATE_ENDED"
                    else -> "UNKNOWN"
                }
                com.example.LogKeeper.log("Playback state changed to: $stateName", "PlaybackService")
                if (playbackState == Player.STATE_ENDED) {
                    val player = PlayerManager.exoPlayer
                    if (player?.repeatMode == Player.REPEAT_MODE_OFF) {
                        stopSelf()
                    }
                } else if (playbackState == Player.STATE_IDLE) {
                    val player = PlayerManager.exoPlayer
                    if (player != null && player.mediaItemCount == 0) {
                        stopSelf()
                    }
                }
            }
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                    val player = PlayerManager.exoPlayer
                    if (player?.repeatMode == Player.REPEAT_MODE_OFF) {
                        player.stop()
                        player.clearMediaItems()
                        stopSelf()
                    }
                }
            }
        })
        

        val intent = android.content.Intent(this, com.example.MainActivity::class.java).apply {
            action = "com.example.ACTION_OPEN_PLAYER"
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        mediaSession = MediaSession.Builder(this, PlayerManager.exoPlayer!!)
            .setSessionActivity(pendingIntent)
            .setBitmapLoader(com.example.MyBitmapLoader(this))
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val defaultResult = super.onConnect(session, controller)
                    val customCommands = defaultResult.availableSessionCommands.buildUpon()
                        .add(androidx.media3.session.SessionCommand("ADD_SUBTITLE", android.os.Bundle.EMPTY))
                        .add(androidx.media3.session.SessionCommand("SET_BOOST_GAIN", android.os.Bundle.EMPTY))
                        .add(androidx.media3.session.SessionCommand("ACTION_CLOSE", android.os.Bundle.EMPTY))
                        .add(androidx.media3.session.SessionCommand("ACTION_MORE", android.os.Bundle.EMPTY))
                        .add(androidx.media3.session.SessionCommand("ACTION_LESS", android.os.Bundle.EMPTY))
                        .add(androidx.media3.session.SessionCommand("ACTION_LOOP", android.os.Bundle.EMPTY))
                        .add(androidx.media3.session.SessionCommand("ACTION_OVERLAY", android.os.Bundle.EMPTY))
                        .add(androidx.media3.session.SessionCommand("ACTION_PIP", android.os.Bundle.EMPTY))
                        .build()
                    return MediaSession.ConnectionResult.accept(customCommands, defaultResult.availablePlayerCommands)
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: androidx.media3.session.SessionCommand,
                    args: android.os.Bundle
                ): ListenableFuture<androidx.media3.session.SessionResult> {
                    if (customCommand.customAction == "SET_BOOST_GAIN") {
                        val gainMb = args.getInt("gainMb", 0)
                        PlayerManager.setBoostGain(gainMb)
                        return Futures.immediateFuture(androidx.media3.session.SessionResult(androidx.media3.session.SessionResult.RESULT_SUCCESS))
                    }

                    if (customCommand.customAction == "ACTION_CLOSE") {
                        val player = session.player
                        player.stop()
                        player.clearMediaItems()
                        stopSelf()
                        return Futures.immediateFuture(androidx.media3.session.SessionResult(androidx.media3.session.SessionResult.RESULT_SUCCESS))
                    }

                    if (customCommand.customAction == "ACTION_LOOP") {
                        val player = session.player
                        player.repeatMode = when (player.repeatMode) {
                            androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
                            androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
                            else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                        }
                        updateCustomLayout()
                        return Futures.immediateFuture(androidx.media3.session.SessionResult(androidx.media3.session.SessionResult.RESULT_SUCCESS))
                    }
                    if (customCommand.customAction == "ACTION_OVERLAY") {
                        if (!android.provider.Settings.canDrawOverlays(this@PlaybackService)) {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:$packageName"))
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        } else {
                            if (composeView == null) {
                                showOverlay()
                            } else {
                                hideOverlay()
                            }
                        }
                        return Futures.immediateFuture(androidx.media3.session.SessionResult(androidx.media3.session.SessionResult.RESULT_SUCCESS))
                    }
                    if (customCommand.customAction == "ACTION_PIP") {
                        // Broadcast to MainActivity to enter PiP
                        val intent = android.content.Intent("com.example.ACTION_ENTER_PIP")
                        sendBroadcast(intent)
                        return Futures.immediateFuture(androidx.media3.session.SessionResult(androidx.media3.session.SessionResult.RESULT_SUCCESS))
                    }

                    if (customCommand.customAction == "ADD_SUBTITLE") {
                        val uriStr = args.getString("subtitle_uri")
                        if (uriStr != null) {
                            val player = session.player
                            val currentItem = player.currentMediaItem
                            if (currentItem != null) {
                                val mimeType = if (uriStr.endsWith(".vtt", true)) androidx.media3.common.MimeTypes.TEXT_VTT
                                    else if (uriStr.endsWith(".ssa", true) || uriStr.endsWith(".ass", true)) androidx.media3.common.MimeTypes.TEXT_SSA
                                    else androidx.media3.common.MimeTypes.APPLICATION_SUBRIP

                                val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(uriStr))
                                    .setMimeType(mimeType)
                                    .setLanguage(null)
                                    .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
                                    .build()
                                
                                val newItemBuilder = currentItem.buildUpon()
                                val oldConfigs = currentItem.localConfiguration?.subtitleConfigurations
                                if (oldConfigs != null) {
                                    newItemBuilder.setSubtitleConfigurations(oldConfigs + subtitleConfig)
                                } else {
                                    newItemBuilder.setSubtitleConfigurations(listOf(subtitleConfig))
                                }
                                
                                val newItem = newItemBuilder.build()
                                val currentItemIndex = player.currentMediaItemIndex
                                player.replaceMediaItem(currentItemIndex, newItem)
                                
                                // Reset the track selection to enable text tracks
                                val builder = player.trackSelectionParameters.buildUpon()
                                builder.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                                player.trackSelectionParameters = builder.build()
                            }
                        }
                        return Futures.immediateFuture(androidx.media3.session.SessionResult(androidx.media3.session.SessionResult.RESULT_SUCCESS))
                    }
                    return super.onCustomCommand(session, controller, customCommand, args)
                }

                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: List<MediaItem>
                ): ListenableFuture<List<MediaItem>> {
                    com.example.LogKeeper.log("onAddMediaItems called with ${mediaItems.size} items", "PlaybackService")
                    val updatedMediaItems = mediaItems.map { mediaItem ->
                        val uriToUse = mediaItem.localConfiguration?.uri?.toString() ?: mediaItem.mediaId
                        com.example.LogKeeper.log("Transforming mediaItem to use URI: $uriToUse", "PlaybackService")
                        mediaItem.buildUpon()
                            .setUri(uriToUse)
                            .build()
                    }
                    return Futures.immediateFuture(updatedMediaItems)
                }
            }).build()
            
        updateCustomLayout()
        
        addSession(mediaSession!!)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        super.onTaskRemoved(rootIntent)
        com.example.LogKeeper.log("onTaskRemoved called, cleaning up.", "PlaybackService")
        val player = mediaSession?.player
        if (player != null && (!player.playWhenReady || player.mediaItemCount == 0 || player.playbackState == androidx.media3.common.Player.STATE_ENDED)) {
            player.stop()
            stopSelf()
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun showOverlay() {
        if (composeView != null) return
        val cv = ComposeView(this)
        composeView = cv
        cv.setViewTreeLifecycleOwner(this@PlaybackService)
        cv.setViewTreeViewModelStoreOwner(this@PlaybackService)
        cv.setViewTreeSavedStateRegistryOwner(this@PlaybackService)
        
        val prefs = getSharedPreferences("MiniPlayerPrefs", android.content.Context.MODE_PRIVATE)

        cv.setContent {
            com.example.ui.components.MiniPlayerOverlay(
                player = com.example.service.PlayerManager.exoPlayer,
                onClose = {
                    val player = com.example.service.PlayerManager.exoPlayer
                    player?.stop()
                    player?.clearMediaItems()
                    hideOverlay()
                    stopSelf()
                },
                onMinimize = {
                    hideOverlay()
                },
                onDrag = { dx, dy ->
                    val lp = layoutParams
                    if (lp != null) {
                        lp.x += dx.toInt()
                        lp.y += dy.toInt()
                        windowManager.updateViewLayout(cv, lp)
                        prefs.edit().putInt("x", lp.x).putInt("y", lp.y).apply()
                    }
                },
                onResize = { dw, dh ->
                    val lp = layoutParams
                    if (lp != null) {
                        lp.width = (lp.width + dw.toInt()).coerceAtLeast(400)
                        lp.height = (lp.height + dh.toInt()).coerceAtLeast(400)
                        windowManager.updateViewLayout(cv, lp)
                        prefs.edit().putInt("width", lp.width).putInt("height", lp.height).apply()
                    }
                }
            )
        }
        val type = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val metrics = resources.displayMetrics
        val widthPx = prefs.getInt("width", (300 * metrics.density).toInt())
        val heightPx = prefs.getInt("height", (200 * metrics.density).toInt())
        layoutParams = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt("x", 100)
            y = prefs.getInt("y", 100)
        }
        windowManager.addView(composeView, layoutParams)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    private fun hideOverlay() {
        composeView?.let {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            windowManager.removeView(it)
        }
        composeView = null
    }

    private fun updateCustomLayout() {

        val loopMode = PlayerManager.exoPlayer?.repeatMode ?: androidx.media3.common.Player.REPEAT_MODE_OFF
        val loopIcon = when (loopMode) {
            androidx.media3.common.Player.REPEAT_MODE_ONE -> com.example.R.drawable.ic_loop_one
            androidx.media3.common.Player.REPEAT_MODE_ALL -> com.example.R.drawable.ic_loop_all
            else -> com.example.R.drawable.ic_loop_none
        }

        val priority = com.example.data.SettingsManager.getInstance(this).getNotificationPriority()
        val buttons = mutableListOf<androidx.media3.session.CommandButton>()
        
        if (priority.contains("Loop")) {
            buttons.add(androidx.media3.session.CommandButton.Builder()
                .setDisplayName("Loop")
                .setIconResId(loopIcon)
                .setSessionCommand(androidx.media3.session.SessionCommand("ACTION_LOOP", android.os.Bundle.EMPTY))
                .build())
        }
        if (priority.contains("Playlist")) {
            buttons.add(androidx.media3.session.CommandButton.Builder()
                .setDisplayName("Playlist")
                .setIconResId(com.example.R.drawable.ic_playlist)
                .setSessionCommand(androidx.media3.session.SessionCommand("ACTION_OVERLAY", android.os.Bundle.EMPTY))
                .build())
        }
        if (priority.contains("PiP")) {
            buttons.add(androidx.media3.session.CommandButton.Builder()
                .setDisplayName("PiP")
                .setIconResId(com.example.R.drawable.ic_pip)
                .setSessionCommand(androidx.media3.session.SessionCommand("ACTION_PIP", android.os.Bundle.EMPTY))
                .build())
        }
        
        buttons.add(androidx.media3.session.CommandButton.Builder()
            .setDisplayName("Close")
            .setIconResId(android.R.drawable.ic_menu_close_clear_cancel)
            .setSessionCommand(androidx.media3.session.SessionCommand("ACTION_CLOSE", android.os.Bundle.EMPTY))
            .build())

        mediaSession?.setCustomLayout(buttons)
    }

    override fun onDestroy() {
        try { unregisterReceiver(widgetCommandReceiver) } catch (e: Exception) {}
        com.example.LogKeeper.log("onDestroy called.", "PlaybackService")
        serviceScope.cancel()
        mediaSession?.run {
            // Do NOT release the player here, so it can survive activity recreation
            // and rapid back/forward navigation.
            release()
        }
        mediaSession = null
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        hideOverlay()
        super.onDestroy()
    }

    private fun updateWidgetUI() {
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(this)
        val componentName = android.content.ComponentName(this, com.example.widget.MediaWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (appWidgetIds.isNotEmpty()) {
            val player = PlayerManager.exoPlayer ?: return
            for (appWidgetId in appWidgetIds) {
                val views = android.widget.RemoteViews(packageName, com.example.R.layout.widget_media)
                views.setTextViewText(com.example.R.id.widget_title, player.currentMediaItem?.mediaMetadata?.title?.toString() ?: "No Media")
                views.setImageViewResource(com.example.R.id.widget_btn_play, if (player.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
                val loopIcon = when (player.repeatMode) {
                    androidx.media3.common.Player.REPEAT_MODE_ONE -> com.example.R.drawable.ic_loop_one_active
                    androidx.media3.common.Player.REPEAT_MODE_ALL -> com.example.R.drawable.ic_loop_all_active
                    else -> com.example.R.drawable.ic_loop_all_inactive
                }
                views.setImageViewResource(com.example.R.id.widget_btn_loop, loopIcon)
                appWidgetManager.updateAppWidget(appWidgetId, views)
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, com.example.R.id.widget_list)
            }
        }
    }

    private val widgetCommandReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
            if (intent.action == "com.example.ACTION_UPDATE_NOTIFICATION") {
                updateCustomLayout()
                return
            }
            val player = PlayerManager.exoPlayer ?: return
            when (intent.getStringExtra("command")) {
                "ACTION_OVERLAY" -> showOverlay()
                "ACTION_PLAY_PAUSE" -> if (player.isPlaying) player.pause() else player.play()
                "ACTION_PREV" -> player.seekToPreviousMediaItem()
                "ACTION_NEXT" -> player.seekToNextMediaItem()
                "ACTION_LOOP" -> {
                    val nextMode = when (player.repeatMode) {
                        androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
                        androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
                        else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                    }
                    player.repeatMode = nextMode
                }
                "ACTION_SHUFFLE" -> player.shuffleModeEnabled = !player.shuffleModeEnabled
                "ACTION_PLAY_ITEM" -> {
                    val index = intent.getIntExtra("index", -1)
                    if (index >= 0) player.seekToDefaultPosition(index)
                }
                "ACTION_PLAY_FILE" -> {
                    val uriStr = intent.getStringExtra("uri")
                    if (uriStr != null) {
                        val mediaItem = androidx.media3.common.MediaItem.Builder()
                            .setUri(uriStr)
                            .setMediaId(uriStr)
                            .setMediaMetadata(
                                androidx.media3.common.MediaMetadata.Builder()
                                    .setTitle(android.net.Uri.parse(uriStr).lastPathSegment ?: "Unknown")
                                    .build()
                            )
                            .build()
                        player.setMediaItem(mediaItem)
                        player.prepare()
                        player.play()
                    }
                }
            }
            updateWidgetUI()
        }
    }
}