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

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val inactivityHandler = Handler(Looper.getMainLooper())
    private val INACTIVITY_TIMEOUT_MS = 5 * 60 * 1000L // 5 mins
    private val releaseRunnable = Runnable {
        com.example.LogKeeper.log("Inactivity timeout reached, releasing ExoPlayer hardware resources.", "PlaybackService")
        PlayerManager.exoPlayer?.run {
            if (!playWhenReady) {
                stop()
                clearMediaItems()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        try {
            val defaultProvider = androidx.media3.session.DefaultMediaNotificationProvider(this)
            setMediaNotificationProvider(object : androidx.media3.session.MediaNotification.Provider {
                override fun createNotification(
                    session: MediaSession,
                    customLayout: com.google.common.collect.ImmutableList<androidx.media3.session.CommandButton>,
                    actionFactory: androidx.media3.session.MediaNotification.ActionFactory,
                    onNotificationChangedCallback: androidx.media3.session.MediaNotification.Provider.Callback
                ): androidx.media3.session.MediaNotification {
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
                    session: MediaSession,
                    action: String,
                    extras: android.os.Bundle
                ): Boolean {
                    return defaultProvider.handleCustomCommand(session, action, extras)
                }
            })
        } catch (e: Exception) {
            com.example.LogKeeper.logError("PlaybackService", "Failed to set up MediaNotificationProvider", e)
        }
        
        val settings = com.example.data.SettingsManager.getInstance(this)
        PlayerManager.initialize(this, false)
        
        PlayerManager.exoPlayer?.addListener(object : Player.Listener {
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
                }
            }
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady) {
                    inactivityHandler.postDelayed(releaseRunnable, INACTIVITY_TIMEOUT_MS)
                } else {
                    inactivityHandler.removeCallbacks(releaseRunnable)
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
            
        mediaSession?.setCustomLayout(
            com.google.common.collect.ImmutableList.of(
                androidx.media3.session.CommandButton.Builder()
                    .setDisplayName("Close")
                    .setIconResId(android.R.drawable.ic_menu_close_clear_cancel)
                    .setSessionCommand(androidx.media3.session.SessionCommand("ACTION_CLOSE", android.os.Bundle.EMPTY))
                    .build()
            )
        )
        
        addSession(mediaSession!!)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        super.onTaskRemoved(rootIntent)
        com.example.LogKeeper.log("onTaskRemoved called, cleaning up.", "PlaybackService")
        val player = mediaSession?.player
        if (player != null && !player.playWhenReady) {
            player.stop()
            stopSelf()
        }
    }

    override fun onDestroy() {
        com.example.LogKeeper.log("onDestroy called.", "PlaybackService")
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        PlayerManager.release()
        inactivityHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
