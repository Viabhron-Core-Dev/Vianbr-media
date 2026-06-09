package com.example.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("vianbr_settings", Context.MODE_PRIVATE)

    private val _folderUris = MutableStateFlow<List<Uri>>(emptyList())
    val folderUris: StateFlow<List<Uri>> = _folderUris.asStateFlow()

    private val _extensions = MutableStateFlow<List<String>>(emptyList())
    val extensions: StateFlow<List<String>> = _extensions.asStateFlow()

    init {
        val urisStrSet = prefs.getStringSet("folder_uris", emptySet())
        if (!urisStrSet.isNullOrEmpty()) {
            _folderUris.value = urisStrSet.map { Uri.parse(it) }
        }
        val exts = prefs.getStringSet("extensions", setOf("mp4", "mkv", "mp3"))?.toList() ?: listOf("mp4", "mkv", "mp3")
        _extensions.value = exts
    }

    fun addFolderUri(uri: Uri) {
        val currentList = _folderUris.value.toMutableList()
        if (!currentList.contains(uri)) {
            currentList.add(uri)
            _folderUris.value = currentList
            prefs.edit().putStringSet("folder_uris", currentList.map { it.toString() }.toSet()).apply()
        }
    }

    fun removeFolderUri(uri: Uri) {
        val currentList = _folderUris.value.toMutableList()
        currentList.remove(uri)
        _folderUris.value = currentList
        prefs.edit().putStringSet("folder_uris", currentList.map { it.toString() }.toSet()).apply()
    }

    fun setExtensions(exts: List<String>) {
        _extensions.value = exts
        prefs.edit().putStringSet("extensions", exts.toSet()).apply()
    }

    var hasSeenWelcome: Boolean
        get() = prefs.getBoolean("has_seen_welcome", false)
        set(value) = prefs.edit().putBoolean("has_seen_welcome", value).apply()

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
