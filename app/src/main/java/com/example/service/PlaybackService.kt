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

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private val inactivityHandler = Handler(Looper.getMainLooper())
    private val INACTIVITY_TIMEOUT_MS = 5 * 60 * 1000L // 5 mins

    private val releaseRunnable = Runnable {
        com.example.LogKeeper.log("Inactivity timeout reached, releasing ExoPlayer hardware resources.", "PlaybackService")
        exoPlayer?.run {
            if (!playWhenReady) {
                stop()
                clearMediaItems()
                clearVideoSurface()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        initializePlayer()
    }

    private fun initializePlayer() {
        if (exoPlayer != null) return
        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(this)
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(this)
            .setDataSourceFactory(dataSourceFactory)
            
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setAllocator(androidx.media3.exoplayer.upstream.DefaultAllocator(true, androidx.media3.common.C.DEFAULT_BUFFER_SEGMENT_SIZE))
            .setBufferDurationsMs(
                androidx.media3.exoplayer.DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                androidx.media3.exoplayer.DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                androidx.media3.exoplayer.DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                androidx.media3.exoplayer.DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setTargetBufferBytes(androidx.media3.common.C.LENGTH_UNSET)
            .setPrioritizeTimeOverSizeThresholds(false)
            .build()

        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build()
            
        exoPlayer?.addListener(object : Player.Listener {
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

        mediaSession = MediaSession.Builder(this, exoPlayer!!)
            .setCallback(object : MediaSession.Callback {
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: List<MediaItem>
                ): ListenableFuture<List<MediaItem>> {
                    com.example.LogKeeper.log("onAddMediaItems called with ${mediaItems.size} items", "PlaybackService")
                    val updatedMediaItems = mediaItems.map { mediaItem ->
                        val uriStr = mediaItem.mediaId
                        com.example.LogKeeper.log("Transforming mediaItem mediaId to URI: $uriStr", "PlaybackService")
                        mediaItem.buildUpon()
                            .setUri(uriStr)
                            .build()
                    }
                    return Futures.immediateFuture(updatedMediaItems)
                }
            })
            .build()
    }

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        super.onTaskRemoved(rootIntent)
        exoPlayer?.run {
            if (!playWhenReady || playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                stop()
                clearMediaItems()
                clearVideoSurface()
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        inactivityHandler.removeCallbacksAndMessages(null)
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        exoPlayer = null
        super.onDestroy()
    }
}
