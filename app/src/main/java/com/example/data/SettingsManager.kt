package com.example.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("vianbr_settings", Context.MODE_PRIVATE)

    private val _outputUri = MutableStateFlow<Uri?>(null)
    val outputUri: StateFlow<Uri?> = _outputUri.asStateFlow()

    private val _extensions = MutableStateFlow<List<String>>(emptyList())
    val extensions: StateFlow<List<String>> = _extensions.asStateFlow()

    init {
        val uriStr = prefs.getString("output_uri", null)
        if (uriStr != null) {
            _outputUri.value = Uri.parse(uriStr)
        }
        val exts = prefs.getStringSet("extensions", setOf("mp4", "mkv", "mp3"))?.toList() ?: listOf("mp4", "mkv", "mp3")
        _extensions.value = exts
    }

    fun setOutputUri(uri: Uri) {
        _outputUri.value = uri
        prefs.edit().putString("output_uri", uri.toString()).apply()
    }

    fun setExtensions(exts: List<String>) {
        _extensions.value = exts
        prefs.edit().putStringSet("extensions", exts.toSet()).apply()
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
