package com.example

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogKeeper {
    private const val TAG = "LogKeeper"
    private const val PREFS_NAME = "log_keeper_prefs"
    private const val KEY_LOGGER_ENABLED = "logger_enabled"
    private lateinit var prefs: SharedPreferences

    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()
    
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isEnabled.value = prefs.getBoolean(KEY_LOGGER_ENABLED, true)

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logError("CRASH", "Uncaught exception in thread ${thread.name}", throwable)
            dumpCrash(context, throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
        
        log("LogKeeper initialized")
    }

    fun toggleLogger() {
        val newState = !_isEnabled.value
        _isEnabled.value = newState
        prefs.edit().putBoolean(KEY_LOGGER_ENABLED, newState).apply()
        log("Logger state changed to: $newState")
    }

    fun log(message: String) {
        if (!_isEnabled.value) return
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val formatted = "[$timestamp] $message"
        Log.d(TAG, formatted)
        _logs.value = _logs.value + formatted
    }

    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        if (!_isEnabled.value) return
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        var formatted = "[$timestamp] ERROR [$tag]: $message"
        if (throwable != null) {
            formatted += "\n${Log.getStackTraceString(throwable)}"
        }
        Log.e(TAG, formatted)
        _logs.value = _logs.value + formatted
    }

    private fun dumpCrash(context: Context, throwable: Throwable) {
        if (!_isEnabled.value) return
        try {
            val dateStr = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
            val fileName = "Vianbrplay_crash_$dateStr.txt"
            
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            
            val file = File(downloadsDir, fileName)
            val crashData = """
                Crash Dump - ${'$'}dateStr
                Message: ${'$'}{throwable.message}
                Stacktrace:
                ${'$'}{Log.getStackTraceString(throwable)}
            """.trimIndent()
            
            file.writeText(crashData)
            Log.d(TAG, "Crash dumped to: ${'$'}{file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash dump", e)
        }
    }
}
