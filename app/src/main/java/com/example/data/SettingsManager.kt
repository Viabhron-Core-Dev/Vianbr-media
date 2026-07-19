package com.example.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("vianbr_settings", Context.MODE_PRIVATE)

    private val _excludedFolders = MutableStateFlow<Set<String>>(emptySet())
    val excludedFolders: StateFlow<Set<String>> = _excludedFolders.asStateFlow()

    private val _extensions = MutableStateFlow<List<String>>(emptyList())
    val extensions: StateFlow<List<String>> = _extensions.asStateFlow()

    private val _outputFolderUri = MutableStateFlow<String?>(null)
    val outputFolderUri: StateFlow<String?> = _outputFolderUri.asStateFlow()

    private val _showLoggerFab = MutableStateFlow(true)
    val showLoggerFab: StateFlow<Boolean> = _showLoggerFab.asStateFlow()

    init {
        val excludedStrSet = prefs.getStringSet("excluded_folders", emptySet())
        if (!excludedStrSet.isNullOrEmpty()) {
            _excludedFolders.value = excludedStrSet
        }
        
        _outputFolderUri.value = prefs.getString("output_folder_uri", null)
        
        _showLoggerFab.value = prefs.getBoolean("show_logger_fab", true)

        val defaultExts = setOf("mp4", "mkv", "mp3", "webm", "3gp", "avi", "mov", "flv", "wmv", "m4v", "aac", "wav", "flac")
        val savedExts = prefs.getStringSet("extensions", null)
        
        val imageExts = setOf("jpg", "jpeg", "png", "webp", "heic")
        val exts = if (savedExts != null) {
            savedExts.filterNot { it in imageExts }.distinct()
        } else {
            defaultExts.toList()
        }
        
        _extensions.value = exts
    }

    fun addExcludedFolder(bucketId: String) {
        val currentSet = _excludedFolders.value.toMutableSet()
        if (currentSet.add(bucketId)) {
            _excludedFolders.value = currentSet
            prefs.edit().putStringSet("excluded_folders", currentSet).apply()
        }
    }

    fun removeExcludedFolder(bucketId: String) {
        val currentSet = _excludedFolders.value.toMutableSet()
        if (currentSet.remove(bucketId)) {
            _excludedFolders.value = currentSet
            prefs.edit().putStringSet("excluded_folders", currentSet).apply()
        }
    }

    fun setExtensions(exts: List<String>) {
        _extensions.value = exts
        prefs.edit().putStringSet("extensions", exts.toSet()).apply()
    }

    fun setOutputFolderUri(uriStr: String?) {
        _outputFolderUri.value = uriStr
        if (uriStr == null) {
            prefs.edit().remove("output_folder_uri").apply()
        } else {
            prefs.edit().putString("output_folder_uri", uriStr).apply()
        }
    }

    fun setShowLoggerFab(show: Boolean) {
        _showLoggerFab.value = show
        prefs.edit().putBoolean("show_logger_fab", show).apply()
    }

    fun savePlaybackState(uri: String, position: Long, duration: Long) {
        prefs.edit()
            .putLong("pos_$uri", position)
            .putLong("dur_$uri", duration)
            .putLong("time_$uri", System.currentTimeMillis())
            .apply()
    }

    fun savePlaybackSpeed(uri: String, speed: Float) {
        prefs.edit().putFloat("speed_$uri", speed).apply()
    }

    fun getPlaybackSpeed(uri: String): Float {
        return prefs.getFloat("speed_$uri", 1.0f)
    }

    fun saveTrackSelection(uri: String, trackType: Int, trackIndex: Int) {
        prefs.edit().putInt("track_${trackType}_$uri", trackIndex).apply()
    }

    fun getTrackSelection(uri: String, trackType: Int): Int {
        return prefs.getInt("track_${trackType}_$uri", -1)
    }

    fun getLastPlayedTime(uri: String): Long {
        return prefs.getLong("time_$uri", 0L)
    }

    fun getPlaybackPosition(uri: String): Long {
        return prefs.getLong("pos_$uri", 0L)
    }

    fun getStoredDuration(uri: String): Long {
        return prefs.getLong("dur_$uri", -1L)
    }

    // A video is finished if we watched past 99%
    fun isFinished(uri: String): Boolean {
        val pos = getPlaybackPosition(uri)
        val dur = getStoredDuration(uri)
        return if (dur > 0L) pos >= dur * 0.99 else false
    }

    var hasSeenWelcome: Boolean
        get() = prefs.getBoolean("has_seen_welcome", false)
        set(value) = prefs.edit().putBoolean("has_seen_welcome", value).apply()

    var audioBoosterEnabled: Boolean
        get() = prefs.getBoolean("audio_booster_enabled", true)
        set(value) = prefs.edit().putBoolean("audio_booster_enabled", value).apply()
        
    var boostGainMb: Int
        get() = prefs.getInt("boost_gain_mb", 0)
        set(value) = prefs.edit().putInt("boost_gain_mb", value).apply()

    var defaultAudioBackgroundPlay: Boolean
        get() = prefs.getBoolean("default_audio_background_play", true)
        set(value) = prefs.edit().putBoolean("default_audio_background_play", value).apply()

    var decoderPriority: Int
        get() = prefs.getInt("decoder_priority", 1) // 0: Device Only, 1: Prefer Device, 2: Prefer App
        set(value) = prefs.edit().putInt("decoder_priority", value).apply()

    fun getNotificationPriority(): List<String> {
        val defaultPriority = "Loop,Playlist,PiP,Close"
        val saved = prefs.getString("notification_priority", defaultPriority) ?: defaultPriority
        return saved.split(",")
    }
    
    fun setNotificationPriority(priority: List<String>) {
        prefs.edit().putString("notification_priority", priority.joinToString(",")).apply()
    }

    companion object {
        @Volatile
        private var instance: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
