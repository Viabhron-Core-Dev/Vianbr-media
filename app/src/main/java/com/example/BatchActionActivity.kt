package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.service.CompressionStatus
import com.example.service.FFmpegStatus
import com.example.ui.components.FFmpegBatchDialog
import com.example.ui.components.CompressionOptionsDialog
import com.example.ui.theme.MyApplicationTheme

class BatchActionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        var initialUris: List<String> = emptyList()
        if (intent?.action == Intent.ACTION_SEND_MULTIPLE) {
            val arrayList = intent?.getParcelableArrayListExtra<android.os.Parcelable>(Intent.EXTRA_STREAM)
            if (arrayList != null) {
                val uris = mutableListOf<String>()
                for (parcel in arrayList) {
                    (parcel as? Uri)?.let { uris.add(it.toString()) }
                }
                initialUris = uris
            }
        } else if (intent?.action == Intent.ACTION_SEND) {
            (intent?.getParcelableExtra<android.os.Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let { uri ->
                initialUris = listOf(uri.toString())
            }
        }

        if (initialUris.isEmpty()) {
            finish()
            return
        }

        val mimeType = intent?.type ?: ""
        val isVideoOrAudio = mimeType.startsWith("video/") || mimeType.startsWith("audio/") || initialUris.any { 
            it.lowercase().endsWith(".mp4") || it.lowercase().endsWith(".gif") || it.lowercase().endsWith(".webp") 
        }

        setContent {
            MyApplicationTheme {
                var startedProcessing by remember { mutableStateOf(false) }
                
                LaunchedEffect(CompressionStatus.isRunning, FFmpegStatus.isRunning) {
                    if (startedProcessing && !CompressionStatus.isRunning && !FFmpegStatus.isRunning) {
                        finish()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (startedProcessing) {
                        AlertDialog(
                            onDismissRequest = { },
                            title = { Text("Processing...") },
                            text = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    if (CompressionStatus.isRunning) {
                                        Text("${CompressionStatus.currentFile} / ${CompressionStatus.totalFiles} Completed")
                                    } else if (FFmpegStatus.isRunning) {
                                        Text("${FFmpegStatus.currentFile} / ${FFmpegStatus.totalFiles} Completed")
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(FFmpegStatus.currentProgress)
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    val stopIntent = Intent(this@BatchActionActivity, com.example.service.FFmpegService::class.java).apply {
                                        action = "STOP"
                                    }
                                    startService(stopIntent)
                                    finish()
                                }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    } else {
                        if (isVideoOrAudio) {
                            FFmpegBatchDialog(
                                uris = initialUris,
                                onDismiss = { finish() },
                                onStartProcessing = { uris, cmdTemplate, format ->
                                    val serviceIntent = Intent(this@BatchActionActivity, com.example.service.FFmpegService::class.java).apply {
                                        action = "START_BATCH"
                                        putStringArrayListExtra("uris", ArrayList(uris))
                                        putExtra("outputExt", format)
                                        putExtra("commandTemplate", cmdTemplate)
                                    }
                                    androidx.core.content.ContextCompat.startForegroundService(this@BatchActionActivity, serviceIntent)
                                    startedProcessing = true
                                }
                            )
                        } else {
                            CompressionOptionsDialog(
                                uris = initialUris,
                                onDismiss = { finish() },
                                onStartCompression = { uris, maxWidth, maxHeight ->
                                    val serviceIntent = Intent(this@BatchActionActivity, com.example.service.CompressionService::class.java).apply {
                                        action = "START_BATCH"
                                        putStringArrayListExtra("uris", ArrayList(uris))
                                        putExtra("maxWidth", maxWidth)
                                        putExtra("maxHeight", maxHeight)
                                    }
                                    androidx.core.content.ContextCompat.startForegroundService(this@BatchActionActivity, serviceIntent)
                                    startedProcessing = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
