package com.example.service

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import android.media.audiofx.LoudnessEnhancer

object PlayerManager {
    var exoPlayer: ExoPlayer? = null
    var loudnessEnhancer: LoudnessEnhancer? = null

    fun initialize(context: Context, skipSilence: Boolean = false) {
        if (exoPlayer != null) return
        
        val dataSourceFactory = DefaultDataSource.Factory(context)
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)
            
        val loadControl = DefaultLoadControl.Builder()
            .setAllocator(androidx.media3.exoplayer.upstream.DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
            .setBufferDurationsMs(
                DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setTargetBufferBytes(C.LENGTH_UNSET)
            .setPrioritizeTimeOverSizeThresholds(false)
            .build()

        exoPlayer = ExoPlayer.Builder(context.applicationContext)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setSkipSilenceEnabled(skipSilence)
            .build()
            
        exoPlayer?.audioSessionId?.let { sessionId ->
            if (sessionId != C.AUDIO_SESSION_ID_UNSET) {
                loudnessEnhancer = LoudnessEnhancer(sessionId)
                loudnessEnhancer?.enabled = false
            }
        }
    }

    fun setBoostGain(gainMb: Int) {
        if (gainMb <= 0) {
            loudnessEnhancer?.enabled = false
        } else {
            loudnessEnhancer?.setTargetGain(gainMb)
            loudnessEnhancer?.enabled = true
        }
    }

    fun addSubtitle(uriStr: String) {
        val player = exoPlayer ?: return
        val currentItem = player.currentMediaItem ?: return
        
        val mimeType = if (uriStr.endsWith(".vtt", true)) androidx.media3.common.MimeTypes.TEXT_VTT
            else if (uriStr.endsWith(".ssa", true) || uriStr.endsWith(".ass", true)) androidx.media3.common.MimeTypes.TEXT_SSA
            else androidx.media3.common.MimeTypes.APPLICATION_SUBRIP
        val subtitleConfig = androidx.media3.common.MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(uriStr))
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
        
        val builder = player.trackSelectionParameters.buildUpon()
        builder.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
        player.trackSelectionParameters = builder.build()
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
        loudnessEnhancer?.release()
        loudnessEnhancer = null
    }
}
