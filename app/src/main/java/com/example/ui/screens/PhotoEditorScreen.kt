package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.LogKeeper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Line(val start: Offset, val end: Offset, val color: Color, val strokeWidth: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(uriString: String, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    
    val lines = remember { mutableStateListOf<Line>() }
    var mode by remember { mutableStateOf("VIEW") } // "VIEW", "DRAW", "CROP", "TEXT"
    
    var showCompressionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uriString) {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(Uri.parse(uriString))
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    withContext(Dispatchers.Main) {
                        imageBitmap = bitmap.asImageBitmap()
                    }
                }
            } catch (e: Exception) {
                LogKeeper.logError("PhotoEditor", "Failed to load image", e)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photo Editor") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showCompressionDialog = true
                    }) {
                        Icon(Icons.Filled.Save, contentDescription = "Save/Distribute")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { mode = if (mode == "DRAW") "VIEW" else "DRAW" }) {
                    Icon(
                        Icons.Filled.Brush, 
                        contentDescription = "Draw",
                        tint = if (mode == "DRAW") MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { mode = if (mode == "CROP") "VIEW" else "CROP" }) {
                    Icon(
                        Icons.Filled.Crop, 
                        contentDescription = "Crop",
                        tint = if (mode == "CROP") MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            if (imageBitmap != null) {
                var dragStart by remember { mutableStateOf(Offset.Zero) }
                
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(mode) {
                            if (mode == "DRAW") {
                                detectDragGestures(
                                    onDragStart = { offset -> dragStart = offset },
                                    onDrag = { change, dragAmount -> 
                                        change.consume()
                                        lines.add(Line(dragStart, dragStart + dragAmount, Color.Red, 10f))
                                        dragStart += dragAmount
                                    }
                                )
                            }
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    val imageAspect = imageBitmap!!.width.toFloat() / imageBitmap!!.height.toFloat()
                    val canvasAspect = canvasWidth / canvasHeight
                    
                    var drawWidth = canvasWidth
                    var drawHeight = canvasHeight
                    
                    if (imageAspect > canvasAspect) {
                        drawHeight = canvasWidth / imageAspect
                    } else {
                        drawWidth = canvasHeight * imageAspect
                    }
                    
                    val left = (canvasWidth - drawWidth) / 2f
                    val top = (canvasHeight - drawHeight) / 2f
                    
                    drawImage(
                        image = imageBitmap!!,
                        dstSize = androidx.compose.ui.unit.IntSize(drawWidth.toInt(), drawHeight.toInt()),
                        dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt())
                    )
                    
                    lines.forEach { line ->
                        drawLine(
                            color = line.color,
                            start = line.start,
                            end = line.end,
                            strokeWidth = line.strokeWidth,
                            cap = StrokeCap.Round
                        )
                    }
                    
                    if (mode == "CROP") {
                        // Draw a temporary visual crop overlay hint
                        drawRect(
                            color = Color.White.copy(alpha = 0.5f),
                            topLeft = Offset(left + drawWidth * 0.1f, top + drawHeight * 0.1f),
                            size = Size(drawWidth * 0.8f, drawHeight * 0.8f),
                            style = Stroke(width = 5f)
                        )
                    }
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
    
    if (showCompressionDialog) {
        com.example.ui.components.CompressionOptionsDialog(
            uris = listOf(uriString),
            onDismiss = { showCompressionDialog = false },
            onStartCompression = { uris, w, h ->
                val intent = android.content.Intent(context, com.example.service.CompressionService::class.java).apply {
                    putStringArrayListExtra("uris", java.util.ArrayList(uris))
                    putExtra("maxWidth", w)
                    putExtra("maxHeight", h)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                showCompressionDialog = false
                onNavigateBack()
            }
        )
    }
}
