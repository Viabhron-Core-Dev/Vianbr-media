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
            }
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady) {
                    inactivityHandler.postDelayed(releaseRunnable, INACTIVITY_TIMEOUT_MS)
                } else {
                    inactivityHandler.removeCallbacks(releaseRunnable)
                }
            }
        })
        
        val bitmapLoader = object : androidx.media3.common.util.BitmapLoader {
            override fun supportsMimeType(mimeType: String): Boolean = true
            
            override fun decodeBitmap(data: ByteArray): ListenableFuture<android.graphics.Bitmap> {
                val future = com.google.common.util.concurrent.SettableFuture.create<android.graphics.Bitmap>()
                try {
                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(data, 0, data.size)
                    future.set(bitmap)
                } catch (e: Exception) {
                    future.setException(e)
                }
                return future
            }
            
            override fun loadBitmap(uri: android.net.Uri): ListenableFuture<android.graphics.Bitmap> {
                val future = com.google.common.util.concurrent.SettableFuture.create<android.graphics.Bitmap>()
                serviceScope.launch(Dispatchers.IO) {
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && uri.scheme == "content") {
                            try {
                                val bitmap = contentResolver.loadThumbnail(uri, android.util.Size(512, 512), null)
                                future.set(bitmap)
                                return@launch
                            } catch (e: Exception) {
                                com.example.LogKeeper.log("loadThumbnail failed, falling back to Coil: ${e.message}", "PlaybackService")
                            }
                        }
                        
                        val request = ImageRequest.Builder(this@PlaybackService)
                            .data(uri)
                            .size(512, 512)
                            .build()
                        val result = Coil.imageLoader(this@PlaybackService).execute(request)
                        val drawable: Drawable? = result.drawable
                        if (drawable is BitmapDrawable) {
                            future.set(drawable.bitmap)
                        } else if (drawable != null) {
                            val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 512
                            val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 512
                            val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bitmap)
                            drawable.setBounds(0, 0, w, h)
                            drawable.draw(canvas)
                            future.set(bitmap)
                        } else {
                            future.setException(Exception("No drawable returned"))
                        }
                    } catch (e: Exception) {
                        future.setException(e)
                    }
                }
                return future
            }
        }

        mediaSession = MediaSession.Builder(this, PlayerManager.exoPlayer!!)
            .setBitmapLoader(bitmapLoader)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val defaultResult = super.onConnect(session, controller)
                    val customCommands = defaultResult.availableSessionCommands.buildUpon()
                        .add(androidx.media3.session.SessionCommand("ADD_SUBTITLE", android.os.Bundle.EMPTY))
                        .add(androidx.media3.session.SessionCommand("SET_BOOST_GAIN", android.os.Bundle.EMPTY))
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
                            .setMediaMetadata(
                                mediaItem.mediaMetadata.buildUpon()
                                    .apply {
                                        if (mediaItem.mediaMetadata.artworkUri == null) {
                                            setArtworkUri(android.net.Uri.parse(uriToUse))
                                        }
                                    }
                                    .build()
                            )
                            .build()
                    }
                    return Futures.immediateFuture(updatedMediaItems)
                }
            }).build()
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
