package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import kotlinx.coroutines.withContext
import java.io.OutputStream

class CompressionService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val CHANNEL_ID = "CompressionServiceChannel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uris = intent?.getStringArrayListExtra("uris") ?: return START_NOT_STICKY

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Compressing Images")
            .setContentText("0 / ${uris.size} completed")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .build()

        startForeground(1, notification)

        serviceScope.launch {
            processImages(uris)
            stopForeground(true)
            stopSelfResult(startId)
        }

        return START_NOT_STICKY
    }

    private suspend fun processImages(uris: List<String>) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val settingsManager = SettingsManager.getInstance(applicationContext)
        val outputUriStr = settingsManager.outputFolderUri.value

        var count = 0
        for (uriStr in uris) {
            try {
                val uri = Uri.parse(uriStr)
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val outStream: OutputStream? = getOutputStream(outputUriStr, "compressed_${System.currentTimeMillis()}.jpg")
                    if (outStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outStream)
                        outStream.close()
                    }
                }
            } catch (e: Exception) {
                LogKeeper.logError("CompressionService", "Failed to compress $uriStr", e)
            }
            count++
            val notification = NotificationCompat.Builder(this@CompressionService, CHANNEL_ID)
                .setContentTitle("Compressing Images")
                .setContentText("$count / ${uris.size} completed")
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setProgress(uris.size, count, false)
                .build()
            notificationManager.notify(1, notification)
        }
    }

    private fun getOutputStream(outputUriStr: String?, fileName: String): OutputStream? {
        if (outputUriStr != null) {
            try {
                val treeUri = Uri.parse(outputUriStr)
                val docId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)
                val docUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                val newUri = android.provider.DocumentsContract.createDocument(
                    contentResolver,
                    docUri,
                    "image/jpeg",
                    fileName
                )
                if (newUri != null) {
                    return contentResolver.openOutputStream(newUri)
                }
            } catch (e: Exception) {
                LogKeeper.logError("CompressionService", "Failed SAF create", e)
            }
        }

        // Fallback to media store
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/Compressed")
            }
        }
        val uri = contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        return uri?.let { contentResolver.openOutputStream(it) }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Compression Service Channel",
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
