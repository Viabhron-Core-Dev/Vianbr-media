package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.LogKeeper
import com.example.data.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.ReturnCode

object FFmpegStatus {
    var isRunning by mutableStateOf(false)
    var totalFiles by mutableStateOf(0)
    var currentFile by mutableStateOf(0)
    var currentProgress by mutableStateOf("")
}

class FFmpegService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val CHANNEL_ID = "FFmpegServiceChannel"
    private var isCancelled = false

    override fun onCreate() {
        super.onCreate()
        
        // Cleanup orphaned temp files from previous crashed/killed sessions
        cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("ffmpeg_in_") || file.name.startsWith("ffmpeg_out_")) {
                file.delete()
            }
        }
        
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP") {
            isCancelled = true
            return START_NOT_STICKY
        }

        val uris = intent?.getStringArrayListExtra("uris") ?: return START_NOT_STICKY
        val commandTemplate = intent.getStringExtra("commandTemplate") ?: ""
        val outputExt = intent.getStringExtra("outputExt") ?: "mp4"

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Processing Video(s)")
            .setContentText("0 / ${uris.size} completed")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .addAction(android.R.drawable.ic_delete, "Cancel", 
                android.app.PendingIntent.getService(this, 0, Intent(this, FFmpegService::class.java).apply { setAction("STOP") }, android.app.PendingIntent.FLAG_IMMUTABLE))
            .build()

        startForeground(2, notification)

        FFmpegStatus.isRunning = true
        FFmpegStatus.totalFiles = uris.size
        FFmpegStatus.currentFile = 0
        FFmpegStatus.currentProgress = "Starting..."
        isCancelled = false

        serviceScope.launch {
            LogKeeper.log("Starting FFmpeg batch for ${uris.size} file(s)", "FFmpegService")
            processFiles(uris, commandTemplate, outputExt)
            LogKeeper.log("Completed FFmpeg batch for ${uris.size} file(s)", "FFmpegService")
            FFmpegStatus.isRunning = false
            stopForeground(true)
            stopSelfResult(startId)
        }

        return START_NOT_STICKY
    }

    private fun processFiles(uris: List<String>, commandTemplate: String, outputExt: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val settingsManager = SettingsManager.getInstance(applicationContext)
        val outputUriStr = settingsManager.outputFolderUri.value

        var count = 0
        for (uriStr in uris) {
            if (isCancelled) {
                LogKeeper.log("FFmpeg batch cancelled.", "FFmpegService")
                break
            }
            val uri = Uri.parse(uriStr)
            
            // 1. Copy to cache for FFmpeg processing
            val tempInFile = java.io.File(cacheDir, "ffmpeg_in_${System.currentTimeMillis()}.${getFileExtension(uri)}")
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    tempInFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                LogKeeper.logError("FFmpegService", "Failed to copy input file: $uriStr", e)
                count++
                continue
            }

            val tempOutFile = java.io.File(cacheDir, "ffmpeg_out_${System.currentTimeMillis()}.$outputExt")
            
            // Replace placeholders in command
            val cmd = commandTemplate
                .replace("%INPUT%", "'${tempInFile.absolutePath}'")
                .replace("%OUTPUT%", "'${tempOutFile.absolutePath}'")

            LogKeeper.log("Executing FFmpeg: $cmd", "FFmpegService")

            FFmpegKitConfig.enableStatisticsCallback { statistics ->
                val timeSec = statistics.time / 1000
                val sizeKb = statistics.size / 1024
                val speed = statistics.speed
                FFmpegStatus.currentProgress = "Time: ${timeSec}s | Size: ${sizeKb}kB | Speed: ${speed}x"
            }

            // Execute FFmpeg
            val session = FFmpegKit.execute(cmd)
            val returnCode = session.returnCode
            
            FFmpegKitConfig.enableStatisticsCallback(null) // clear callback

            if (isCancelled) {
                LogKeeper.log("FFmpeg processing cancelled.", "FFmpegService")
                FFmpegKit.cancel(session.sessionId)
                break
            }

            if (ReturnCode.isSuccess(returnCode)) {
                LogKeeper.log("FFmpeg processing succeeded.", "FFmpegService")
            
            // 2. Move out to SAF output folder
            val origName = getOriginalFileName(uri)
            val fileName = "${origName}_edited.$outputExt"
            val outStream = getOutputStream(outputUriStr, fileName, getMimeType(outputExt))
            if (outStream != null) {
                try {
                    FFmpegStatus.currentProgress = "Saving output file..."
                    tempOutFile.inputStream().use { input ->
                        outStream.use { output ->
                            val buffer = ByteArray(8192)
                            var bytesCopied: Long = 0
                            val totalBytes = tempOutFile.length()
                            var lastReportTime = System.currentTimeMillis()
                            while (true) {
                                val bytes = input.read(buffer)
                                if (bytes < 0) break
                                output.write(buffer, 0, bytes)
                                bytesCopied += bytes
                                val now = System.currentTimeMillis()
                                if (now - lastReportTime > 500) {
                                    val percent = if (totalBytes > 0) (bytesCopied * 100 / totalBytes).toInt() else 0
                                    FFmpegStatus.currentProgress = "Saving: $percent%"
                                    lastReportTime = now
                                }
                            }
                        }
                    }
                    LogKeeper.log("Saved to output folder: $fileName", "FFmpegService")
                } catch (e: Exception) {
                    LogKeeper.logError("FFmpegService", "Failed to copy output file to SAF", e)
                }
            }
            } else if (ReturnCode.isCancel(returnCode)) {
                LogKeeper.log("FFmpeg processing cancelled by user.", "FFmpegService")
            } else {
                LogKeeper.logError("FFmpegService", "FFmpeg failed with code ${returnCode?.value ?: "null"}", Exception(session.failStackTrace))
            }

            // Cleanup temps
            if (tempInFile.exists()) tempInFile.delete()
            if (tempOutFile.exists()) tempOutFile.delete()

            count++
            val notification = NotificationCompat.Builder(this@FFmpegService, CHANNEL_ID)
                .setContentTitle("Processing Video(s)")
                .setContentText("$count / ${uris.size} completed")
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setProgress(uris.size, count, false)
                .build()
            notificationManager.notify(2, notification)
            FFmpegStatus.currentFile = count
        }
    }
    
    private fun getFileExtension(uri: Uri): String {
        val mimeType = contentResolver.getType(uri) ?: return "mp4"
        return android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "mp4"
    }

    private fun getMimeType(ext: String): String {
        return android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "video/mp4"
    }

    private fun getOriginalFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            result = cursor.getString(index)
                        }
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        if (result == null) {
            result = uri.path?.let { java.io.File(it).name }
        }
        return result?.substringBeforeLast(".") ?: "edited_${System.currentTimeMillis()}"
    }

    private fun getOutputStream(outputUriStr: String?, fileName: String, mimeType: String): java.io.OutputStream? {
        if (outputUriStr != null) {
            try {
                val treeUri = Uri.parse(outputUriStr)
                val docId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)
                val docUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                val newUri = android.provider.DocumentsContract.createDocument(
                    contentResolver,
                    docUri,
                    mimeType,
                    fileName
                )
                if (newUri != null) {
                    return contentResolver.openOutputStream(newUri)
                }
            } catch (e: Exception) {
                LogKeeper.logError("FFmpegService", "Failed SAF create", e)
            }
        }

        // Fallback to media store
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val relativePath = if (mimeType.startsWith("audio")) android.os.Environment.DIRECTORY_MUSIC else android.os.Environment.DIRECTORY_MOVIES
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "$relativePath/Edited")
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (mimeType.startsWith("audio")) android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI else android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            if (mimeType.startsWith("audio")) android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI else android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val uri = contentResolver.insert(collection, contentValues)
        return uri?.let { contentResolver.openOutputStream(it) }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "FFmpeg Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
