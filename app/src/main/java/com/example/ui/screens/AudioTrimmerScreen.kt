package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.EditedMediaItem
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTrimmerScreen(
    uriString: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uri = Uri.parse(uriString)
    val coroutineScope = rememberCoroutineScope()
    
    // We would extract the actual duration, for now we will assume 180 seconds or mock it.
    // In a real app we'd load the metadata.
    var durationMs by remember { mutableLongStateOf(0L) }
    var durationLoaded by remember { mutableStateOf(false) }
    
    LaunchedEffect(uri) {
        try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationMs = time?.toLongOrNull() ?: 180000L
            retriever.release()
            durationLoaded = true
        } catch (e: Exception) {
            durationMs = 180000L
            durationLoaded = true
        }
    }
    
    var startMs by remember { mutableLongStateOf(0L) }
    var endMs by remember { mutableLongStateOf(durationMs) }
    
    LaunchedEffect(durationLoaded) {
        if (durationLoaded) endMs = durationMs
    }

    var isExporting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trim Audio") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (endMs > startMs) {
                                exportTrimmedAudio(context, uri, startMs, endMs) { success ->
                                    isExporting = false
                                    if (success) {
                                        Toast.makeText(context, "Trimmed audio saved to Downloads", Toast.LENGTH_SHORT).show()
                                        onNavigateBack()
                                    } else {
                                        Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                isExporting = true
                            } else {
                                Toast.makeText(context, "Invalid range", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isExporting
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = "Save Trim")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            if (!durationLoaded) {
                CircularProgressIndicator()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Timeline", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Plain Clean Timeline
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        // Drawing simple waveform or lines
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val startX = (startMs.toFloat() / durationMs) * w
                            val endX = (endMs.toFloat() / durationMs) * w
                            
                            // Draw base timeline
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.5f),
                                start = Offset(0f, h/2),
                                end = Offset(w, h/2),
                                strokeWidth = 4.dp.toPx()
                            )
                            
                            // Draw selected region
                            drawRect(
                                color = Color(0xFF2196F3).copy(alpha = 0.3f),
                                topLeft = Offset(startX, 0f),
                                size = androidx.compose.ui.geometry.Size(endX - startX, h)
                            )
                            
                            // Draw active timeline part
                            drawLine(
                                color = Color(0xFF2196F3),
                                start = Offset(startX, h/2),
                                end = Offset(endX, h/2),
                                strokeWidth = 6.dp.toPx()
                            )

                            // Start and End markers
                            drawLine(color = Color.White, start = Offset(startX, 0f), end = Offset(startX, h), strokeWidth = 2.dp.toPx())
                            drawLine(color = Color.White, start = Offset(endX, 0f), end = Offset(endX, h), strokeWidth = 2.dp.toPx())
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Selection: ${formatTrimmerTime(startMs)} - ${formatTrimmerTime(endMs)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    // Dials
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DialControl(
                            label = "Start",
                            value = startMs.toFloat(),
                            maxValue = durationMs.toFloat(),
                            onValueChange = { newVal -> 
                                if (newVal < endMs) startMs = newVal.toLong() 
                            }
                        )
                        DialControl(
                            label = "End",
                            value = endMs.toFloat(),
                            maxValue = durationMs.toFloat(),
                            onValueChange = { newVal -> 
                                if (newVal > startMs) endMs = newVal.toLong() 
                            }
                        )
                    }
                }
            }
            
            if (isExporting) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(modifier = Modifier.padding(32.dp)) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("Exporting...")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DialControl(
    label: String,
    value: Float,
    maxValue: Float,
    onValueChange: (Float) -> Unit
) {
    // Current angle based on value (0 to 360)
    val angle = (value / maxValue.coerceAtLeast(1f)) * 360f
    
    var center by remember { mutableStateOf(Offset.Zero) }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            center = Offset(size.width / 2f, size.height / 2f)
                        },
                        onDrag = { change, _ ->
                            val dragOffset = change.position
                            val dx = dragOffset.x - center.x
                            val dy = dragOffset.y - center.y
                            // Math.atan2 gives angle from -PI to PI
                            var newAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            // Shift so 0 is top
                            newAngle += 90f
                            if (newAngle < 0) newAngle += 360f
                            
                            val portion = newAngle / 360f
                            val newVal = portion * maxValue
                            onValueChange(newVal)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.width / 2f * 0.8f
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                
                // Draw Dial track
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.3f),
                    radius = radius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 8.dp.toPx())
                )
                
                // Draw Dial indicator
                val indicatorAngleRad = Math.toRadians((angle - 90f).toDouble())
                val endX = centerX + radius * cos(indicatorAngleRad).toFloat()
                val endY = centerY + radius * sin(indicatorAngleRad).toFloat()
                
                drawLine(
                    color = Color(0xFF2196F3),
                    start = Offset(centerX, centerY),
                    end = Offset(endX, endY),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                
                // Center knob center hole
                drawCircle(
                    color = Color(0xFF2196F3),
                    radius = 8.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(formatTrimmerTime(value.toLong()), style = MaterialTheme.typography.bodyMedium, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    }
}

private fun formatTrimmerTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = ms % 1000
    return String.format("%02d:%02d.%03d", minutes, seconds, millis)
}

// Media3 Transformer API
private fun exportTrimmedAudio(context: Context, inputUri: Uri, startMs: Long, endMs: Long, onComplete: (Boolean) -> Unit) {
    val transformer = Transformer.Builder(context).build()
    
    val clippingConfiguration = MediaItem.ClippingConfiguration.Builder()
        .setStartPositionMs(startMs)
        .setEndPositionMs(endMs)
        .build()

    val mediaItem = MediaItem.Builder()
        .setUri(inputUri)
        .setClippingConfiguration(clippingConfiguration)
        .build()
        
    val editedMediaItem = EditedMediaItem.Builder(mediaItem).build()
    
    val outputDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
    if (!outputDir.exists()) outputDir.mkdirs()
    val outputFile = java.io.File(outputDir, "Trimmed_Audio_${System.currentTimeMillis()}.m4a")

    transformer.addListener(object : Transformer.Listener {
        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
            super.onCompleted(composition, exportResult)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onComplete(true)
            }
        }

        override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
            super.onError(composition, exportResult, exportException)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onComplete(false)
            }
        }
    })

    transformer.start(editedMediaItem, outputFile.absolutePath)
}
