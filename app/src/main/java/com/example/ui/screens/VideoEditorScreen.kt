package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

import com.example.LogKeeper

data class VideoEditState(

    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val isCutMode: Boolean = false,
    val cutStartMs: Long = 0L,
    val cutEndMs: Long = 0L,
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val cropRect: String = "",
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f,
    val aspectRatio: String = "Original",
    val rotateConfig: Int = 0,
    val hasCaptions: Boolean = false,
    val captionText: String = "Sample Text"
)

enum class VideoEditorTool {
    NONE, TRIM, SPEED, CROP, AUDIO, ASPECT_RATIO, ROTATE, CAPTIONS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorScreen(
    uriString: String,
    onNavigateBack: () -> Unit
) {
    var editState by remember { mutableStateOf(VideoEditState()) }
    var currentTool by remember { mutableStateOf(VideoEditorTool.NONE) }
    var backupEditState by remember { mutableStateOf<VideoEditState?>(null) }
    var showExportPanel by remember { mutableStateOf(false) }
    var durationMs by remember { mutableLongStateOf(1L) }

    val context = LocalContext.current
    val initialUri = Uri.parse(uriString)
    val mimeType = remember { context.contentResolver.getType(initialUri) }

    var convertedUri by remember { mutableStateOf<String?>(null) }
    var isConverting by remember { mutableStateOf(false) }
    val effectiveUri = convertedUri ?: uriString
    val effectiveMimeType = if (convertedUri != null) "video/mp4" else mimeType
    val uri = Uri.parse(effectiveUri)

    LaunchedEffect(uriString) {
        LogKeeper.log("Starting pre-conversion for mimeType: $mimeType uri: $uriString", "VideoEditor")
        if (mimeType == "image/gif" || mimeType == "image/webp") {
            isConverting = true
            try {
                // Step 1: Copy URI to cache file on IO thread
                val ext = if (mimeType == "image/gif") "gif" else "webp"
                val inputFile = withContext(Dispatchers.IO) {
                    val f = java.io.File(context.cacheDir, "editor_in_${System.currentTimeMillis()}.$ext")
                    context.contentResolver.openInputStream(Uri.parse(uriString))?.use { input ->
                        f.outputStream().use { output -> input.copyTo(output) }
                    }
                    f
                }
                val outputFile = java.io.File(context.cacheDir, "editor_converted_${System.currentTimeMillis()}.mp4")
                
                if (mimeType == "image/webp") {
                    val framesDir = java.io.File(context.cacheDir, "editor_frames_${System.currentTimeMillis()}")
                    framesDir.mkdirs()
                    var frameCount = 0

                    var calculatedFps = 30
                    withContext(Dispatchers.IO) {
                        try {
                            val bytes = inputFile.readBytes()
                            val webpImage = com.facebook.animated.webp.WebPImage.createFromByteArray(bytes, com.facebook.imagepipeline.common.ImageDecodeOptions.defaults())
                            frameCount = webpImage.frameCount
                            val durations = webpImage.frameDurations
                            val averageDurationMs = if (frameCount > 0) durations.sum() / frameCount else 33
                            calculatedFps = if (averageDurationMs > 0) 1000 / averageDurationMs else 30
                            
                            
                            var lastCachedFrame: android.graphics.Bitmap? = null
                            var lastCachedFrameIndex = -1

                            val result = com.facebook.imagepipeline.animated.base.AnimatedImageResult.forAnimatedImage(webpImage)
                            val backend = com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendImpl(
                                com.facebook.imagepipeline.animated.util.AnimatedDrawableUtil(), 
                                result, 
                                android.graphics.Rect(0, 0, webpImage.width, webpImage.height), 
                                false
                            )
                            val compositor = com.facebook.imagepipeline.animated.impl.AnimatedImageCompositor(
                                backend, 
                                false, 
                                object : com.facebook.imagepipeline.animated.impl.AnimatedImageCompositor.Callback {
                                    override fun onIntermediateResult(frameNumber: Int, bitmap: android.graphics.Bitmap) {}
                                    override fun getCachedBitmap(frameNumber: Int): com.facebook.common.references.CloseableReference<android.graphics.Bitmap>? {
                                        return if (frameNumber == lastCachedFrameIndex && lastCachedFrame != null) {
                                            com.facebook.common.references.CloseableReference.of(lastCachedFrame!!, com.facebook.common.references.ResourceReleaser { })
                                        } else null
                                    }
                                }
                            )

                            var previousFrameToRecycle: android.graphics.Bitmap? = null
                            for (i in 0 until frameCount) {
                                val w = webpImage.width
                                val h = webpImage.height
                                val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
                                compositor.renderFrame(i, bmp)
                                java.io.File(framesDir, "frame_%04d.png".format(i))
                                    .outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                                
                                previousFrameToRecycle?.recycle()
                                lastCachedFrame = bmp
                                lastCachedFrameIndex = i
                                previousFrameToRecycle = bmp
                            }
                            previousFrameToRecycle?.recycle()
                            webpImage.dispose()
                        } catch (e: Exception) {
                            LogKeeper.logError("VideoEditor", "Frame extraction failed: ${e.message}", e)
                        }
                    }

                    if (frameCount > 0) {
                        withContext(Dispatchers.IO) {
                            val cmd = "-y -framerate $calculatedFps -i '${framesDir.absolutePath}/frame_%04d.png' -vf \"scale=trunc(iw/2)*2:trunc(ih/2)*2\" -vcodec libx264 -crf 23 -preset ultrafast -pix_fmt yuv420p '${outputFile.absolutePath}'"
                            val session = com.arthenica.ffmpegkit.FFmpegKit.execute(cmd)
                            if (com.arthenica.ffmpegkit.ReturnCode.isSuccess(session.returnCode) && outputFile.exists()) {
                                convertedUri = outputFile.toURI().toString()
                                LogKeeper.log("Pre-conversion complete. convertedUri: $convertedUri", "VideoEditor")
                            } else {
                                LogKeeper.logError("VideoEditor", "FFmpeg PNG→MP4 failed: ${session.returnCode}\nLogs: ${session.allLogsAsString}", Exception())
                            }
                            framesDir.deleteRecursively()
                        }
                    }
                } else if (mimeType == "image/gif") {
                    withContext(Dispatchers.IO) {
                        val cmd = "-y -i '${inputFile.absolutePath}' -vf \"scale=trunc(iw/2)*2:trunc(ih/2)*2\" -vcodec libx264 -crf 23 -preset ultrafast -pix_fmt yuv420p '${outputFile.absolutePath}'"
                        val session = com.arthenica.ffmpegkit.FFmpegKit.execute(cmd)
                        if (com.arthenica.ffmpegkit.ReturnCode.isSuccess(session.returnCode) && outputFile.exists()) {
                            convertedUri = outputFile.toURI().toString()
                            LogKeeper.log("Pre-conversion complete. convertedUri: $convertedUri", "VideoEditor")
                        } else {
                            LogKeeper.logError("VideoEditor", "FFmpeg GIF/WEBP→MP4 failed: ${session.returnCode}\nLogs: ${session.allLogsAsString}", Exception())
                        }
                    }
                }
            } catch (e: Exception) {
                LogKeeper.logError("VideoEditor", "Pre-conversion failed: ${e.message}", e)
            }
            isConverting = false
        }
    }

    // ExoPlayer for Live Preview
    var videoWidth by remember { mutableIntStateOf(1) }
    var videoHeight by remember { mutableIntStateOf(1) }
    val exoPlayer = remember(effectiveUri) {
        if (mimeType == "image/gif" || mimeType == "image/webp") {
            if (convertedUri == null) null
            else ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(uri))
                repeatMode = Player.REPEAT_MODE_ONE
                prepare()
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                        if (videoSize.width > 0 && videoSize.height > 0) {
                            videoWidth = videoSize.width
                            videoHeight = videoSize.height
                        }
                    }
                })
            }
        } else {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(uri))
                repeatMode = Player.REPEAT_MODE_ONE
                prepare()
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                        if (videoSize.width > 0 && videoSize.height > 0) {
                            videoWidth = videoSize.width
                            videoHeight = videoSize.height
                        }
                    }
                })
            }
        }
    }

    if (exoPlayer != null) {
        DisposableEffect(exoPlayer) {
            LogKeeper.log("ExoPlayer initialized for video editor", "VideoEditor")
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        val dur = exoPlayer.duration
                        if (dur > 0) {
                            durationMs = dur
                            if (editState.trimEndMs == 0L) {
                                editState = editState.copy(trimEndMs = dur)
                            }
                            LogKeeper.log("ExoPlayer is READY. Loaded video duration: ${formatMs(dur)}", "VideoEditor")
                        }
                    }
                }
            }
            exoPlayer.addListener(listener)
            onDispose {
                LogKeeper.log("ExoPlayer released from video editor", "VideoEditor")
                exoPlayer.removeListener(listener)
                exoPlayer.release()
            }
        }
    }

    // Live preview updates based on edit state
    LaunchedEffect(editState.speed) {
        LogKeeper.log("Video playback speed adjusted to: ${editState.speed}x", "VideoEditor")
        exoPlayer?.setPlaybackSpeed(editState.speed)
    }
    LaunchedEffect(editState.volume) {
        LogKeeper.log("Video playback volume adjusted to: ${editState.volume * 100}%", "VideoEditor")
        exoPlayer?.volume = editState.volume
    }

    if (isConverting) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CircularProgressIndicator(color = Color.White)
                Text("Converting for editing...", color = Color.White)
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editor", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Undo */ }) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = { /* Redo */ }) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }
                    Button(onClick = { showExportPanel = true }) {
                        Text("SAVE")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Main UI: Player Preview
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                val ratio = if (editState.cropRect == "Center Crop" && currentTool != VideoEditorTool.CROP) {
                    1f
                } else {
                    when (editState.aspectRatio) {
                        "16:9" -> 16f / 9f
                        "9:16" -> 9f / 16f
                        "1:1" -> 1f
                        "4:3" -> 4f / 3f
                        "21:9" -> 21f / 9f
                        else -> null
                    }
                }
                val previewModifier = if (ratio != null) {
                    Modifier
                        .aspectRatio(ratio)
                        .background(Color.DarkGray)
                } else {
                    Modifier.fillMaxSize()
                }
                if (exoPlayer != null) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = true
                            }
                        },
                        update = { view ->
                            view.resizeMode = if (ratio != null) 3 else 0
                            view.rotation = editState.rotateConfig.toFloat()
                        },
                        modifier = previewModifier.graphicsLayer {
                            clip = true
                            if (currentTool != VideoEditorTool.CROP && editState.cropRect == "Custom") {
                                val cw = editState.cropRight - editState.cropLeft
                                val ch = editState.cropBottom - editState.cropTop
                                if (cw > 0 && ch > 0) {
                                    scaleX = 1f / cw
                                    scaleY = 1f / ch
                                    val cx = (editState.cropLeft + editState.cropRight) / 2f
                                    val cy = (editState.cropTop + editState.cropBottom) / 2f
                                    translationX = (0.5f - cx) * size.width * scaleX
                                    translationY = (0.5f - cy) * size.height * scaleY
                                }
                            } else if (currentTool != VideoEditorTool.CROP && editState.cropRect == "Center Crop") {
                                // Center Crop is effectively a 1:1 ratio. If they don't have aspect ratio 1:1 selected, we simulate it
                                // by scaling the shorter dimension. Actually, ExoPlayer resizeMode handles this if we just let it.
                            }
                        }
                    )
                } else {
                    Box(modifier = previewModifier.background(Color.Black))
                }
                
                if (currentTool == VideoEditorTool.CROP && editState.cropRect == "Custom") {
                    var resizeCorner by remember { mutableIntStateOf(0) }
                    Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                if (videoWidth == 0 || videoHeight == 0) return@detectDragGestures
                                val canvasAspect = size.width.toFloat() / size.height.toFloat()
                                val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()
                                var drawWidth = size.width.toFloat()
                                var drawHeight = size.height.toFloat()
                                if (videoAspect > canvasAspect) {
                                    drawHeight = size.width / videoAspect
                                } else {
                                    drawWidth = size.height * videoAspect
                                }
                                val left = (size.width - drawWidth) / 2f
                                val top = (size.height - drawHeight) / 2f
                                
                                val cL = left + editState.cropLeft * drawWidth
                                val cT = top + editState.cropTop * drawHeight
                                val cR = left + editState.cropRight * drawWidth
                                val cB = top + editState.cropBottom * drawHeight
                                
                                val touchRadius = 60f
                                if (abs(offset.x - cL) < touchRadius && abs(offset.y - cT) < touchRadius) resizeCorner = 1
                                else if (abs(offset.x - cR) < touchRadius && abs(offset.y - cT) < touchRadius) resizeCorner = 2
                                else if (abs(offset.x - cL) < touchRadius && abs(offset.y - cB) < touchRadius) resizeCorner = 3
                                else if (abs(offset.x - cR) < touchRadius && abs(offset.y - cB) < touchRadius) resizeCorner = 4
                                else if (offset.x > cL && offset.x < cR && offset.y > cT && offset.y < cB) resizeCorner = 5
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (videoWidth == 0 || videoHeight == 0) return@detectDragGestures
                                val canvasAspect = size.width.toFloat() / size.height.toFloat()
                                val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()
                                var drawWidth = size.width.toFloat()
                                var drawHeight = size.height.toFloat()
                                if (videoAspect > canvasAspect) {
                                    drawHeight = size.width / videoAspect
                                } else {
                                    drawWidth = size.height * videoAspect
                                }
                                val dx = dragAmount.x / drawWidth
                                val dy = dragAmount.y / drawHeight
                                
                                var nL = editState.cropLeft
                                var nT = editState.cropTop
                                var nR = editState.cropRight
                                var nB = editState.cropBottom
                                
                                when (resizeCorner) {
                                    5 -> {
                                        nL = (nL + dx).coerceIn(0f, 1f - (nR - editState.cropLeft))
                                        nR = nL + (editState.cropRight - editState.cropLeft)
                                        nT = (nT + dy).coerceIn(0f, 1f - (nB - editState.cropTop))
                                        nB = nT + (editState.cropBottom - editState.cropTop)
                                    }
                                    1 -> {
                                        nL = (nL + dx).coerceIn(0f, nR - 0.05f)
                                        nT = (nT + dy).coerceIn(0f, nB - 0.05f)
                                    }
                                    2 -> {
                                        nR = (nR + dx).coerceIn(nL + 0.05f, 1f)
                                        nT = (nT + dy).coerceIn(0f, nB - 0.05f)
                                    }
                                    3 -> {
                                        nL = (nL + dx).coerceIn(0f, nR - 0.05f)
                                        nB = (nB + dy).coerceIn(nT + 0.05f, 1f)
                                    }
                                    4 -> {
                                        nR = (nR + dx).coerceIn(nL + 0.05f, 1f)
                                        nB = (nB + dy).coerceIn(nT + 0.05f, 1f)
                                    }
                                }
                                editState = editState.copy(cropLeft = nL, cropTop = nT, cropRight = nR, cropBottom = nB)
                            },
                            onDragEnd = { resizeCorner = 0 },
                            onDragCancel = { resizeCorner = 0 }
                        )
                    }) {
                        if (videoWidth == 0 || videoHeight == 0) return@Canvas
                        val canvasAspect = size.width / size.height
                        val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()
                        var drawWidth = size.width
                        var drawHeight = size.height
                        if (videoAspect > canvasAspect) {
                            drawHeight = size.width / videoAspect
                        } else {
                            drawWidth = size.height * videoAspect
                        }
                        val left = (size.width - drawWidth) / 2f
                        val top = (size.height - drawHeight) / 2f
                        
                        val cL = left + editState.cropLeft * drawWidth
                        val cT = top + editState.cropTop * drawHeight
                        val cR = left + editState.cropRight * drawWidth
                        val cB = top + editState.cropBottom * drawHeight
                        
                        drawRect(color = Color.Black.copy(alpha = 0.5f), topLeft = Offset(left, top), size = Size(drawWidth, cT - top))
                        drawRect(color = Color.Black.copy(alpha = 0.5f), topLeft = Offset(left, cB), size = Size(drawWidth, top + drawHeight - cB))
                        drawRect(color = Color.Black.copy(alpha = 0.5f), topLeft = Offset(left, cT), size = Size(cL - left, cB - cT))
                        drawRect(color = Color.Black.copy(alpha = 0.5f), topLeft = Offset(cR, cT), size = Size(left + drawWidth - cR, cB - cT))
                        
                        drawRect(color = Color.White, topLeft = Offset(cL, cT), size = Size(cR - cL, cB - cT), style = Stroke(width = 5f))
                        
                        val cornerLen = 40f
                        drawLine(Color.Green, Offset(cL, cT), Offset(cL + cornerLen, cT), 12f)
                        drawLine(Color.Green, Offset(cL, cT), Offset(cL, cT + cornerLen), 12f)
                        
                        drawLine(Color.Green, Offset(cR, cT), Offset(cR - cornerLen, cT), 12f)
                        drawLine(Color.Green, Offset(cR, cT), Offset(cR, cT + cornerLen), 12f)
                        
                        drawLine(Color.Green, Offset(cL, cB), Offset(cL + cornerLen, cB), 12f)
                        drawLine(Color.Green, Offset(cL, cB), Offset(cL, cB - cornerLen), 12f)
                        
                        drawLine(Color.Green, Offset(cR, cB), Offset(cR - cornerLen, cB), 12f)
                        drawLine(Color.Green, Offset(cR, cB), Offset(cR, cB - cornerLen), 12f)
                    }
                }
            }

            // Timeline / Progress Bar Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 16.dp)
            ) {
                var currentPositionMs by remember { mutableLongStateOf(0L) }
                var isDragging by remember { mutableStateOf(false) }

                LaunchedEffect(exoPlayer, editState.trimStartMs, editState.trimEndMs, editState.isCutMode) {
                    while (true) {
                        if (!isDragging) {
                            currentPositionMs = exoPlayer?.currentPosition ?: 0L
                            if (!editState.isCutMode) {
                                if (editState.trimEndMs > 0 && currentPositionMs >= editState.trimEndMs) {
                                    exoPlayer?.seekTo(editState.trimStartMs)
                                    currentPositionMs = editState.trimStartMs
                                } else if (currentPositionMs < editState.trimStartMs) {
                                    exoPlayer?.seekTo(editState.trimStartMs)
                                    currentPositionMs = editState.trimStartMs
                                }
                            } else {
                                // In cut mode, we skip the middle
                                if (currentPositionMs >= editState.trimStartMs && currentPositionMs < editState.trimEndMs) {
                                    exoPlayer?.seekTo(editState.trimEndMs)
                                    currentPositionMs = editState.trimEndMs
                                }
                            }
                        }
                        kotlinx.coroutines.delay(50L) // Poll 20 times a second
                    }
                }

                Slider(
                    value = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                    onValueChange = { 
                        isDragging = true
                        currentPositionMs = (it * durationMs).toLong()
                        exoPlayer?.seekTo(currentPositionMs)
                    },
                    onValueChangeFinished = {
                        isDragging = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatMs(currentPositionMs), style = MaterialTheme.typography.labelSmall)
                    Text("Total: ${formatMs(durationMs)}", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Tools Bottom Bar / Partial UI
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (currentTool == VideoEditorTool.NONE) {
                    // Main Tools Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        ToolIcon(Icons.Filled.ContentCut, "Trim") { backupEditState = editState.copy(); currentTool = VideoEditorTool.TRIM }
                        ToolIcon(Icons.Filled.Speed, "Speed") { backupEditState = editState.copy(); currentTool = VideoEditorTool.SPEED }
                        ToolIcon(Icons.Filled.Crop, "Crop") { backupEditState = editState.copy(); currentTool = VideoEditorTool.CROP }
                        ToolIcon(Icons.Filled.VolumeUp, "Audio") { backupEditState = editState.copy(); currentTool = VideoEditorTool.AUDIO }
                        ToolIcon(Icons.Filled.AspectRatio, "Aspect Ratio") { backupEditState = editState.copy(); currentTool = VideoEditorTool.ASPECT_RATIO }
                        ToolIcon(Icons.Filled.RotateRight, "Rotate") { backupEditState = editState.copy(); currentTool = VideoEditorTool.ROTATE }
                        ToolIcon(Icons.Filled.ClosedCaption, "Captions") { backupEditState = editState.copy(); currentTool = VideoEditorTool.CAPTIONS }
                    }
                } else {
                    // Partial Tool UI Panel
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(currentTool.name, style = MaterialTheme.typography.titleSmall)
                            Row {
                                IconButton(onClick = { 
                                    backupEditState?.let { editState = it }
                                    currentTool = VideoEditorTool.NONE 
                                    backupEditState = null
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Cancel")
                                }
                                IconButton(onClick = { 
                                    currentTool = VideoEditorTool.NONE 
                                    backupEditState = null
                                }) {
                                    Icon(Icons.Filled.Check, contentDescription = "Done")
                                }
                            }
                        }
                        
                        // Tool specific sliders/buttons placeholder
                        when (currentTool) {
                            VideoEditorTool.TRIM -> {
                                val start = editState.trimStartMs.toFloat().coerceIn(0f, durationMs.toFloat())
                                val end = editState.trimEndMs.toFloat().coerceIn(start, durationMs.toFloat()).takeIf { it > 0 } ?: durationMs.toFloat()
                                
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(formatMs(start.toLong()), style = MaterialTheme.typography.labelMedium)
                                        Text(formatMs(end.toLong()), style = MaterialTheme.typography.labelMedium)
                                    }
                                    RangeSlider(
                                        value = start..end,
                                        onValueChange = { range ->
                                            val oldStart = editState.trimStartMs
                                            editState = editState.copy(
                                                trimStartMs = range.start.toLong(),
                                                trimEndMs = range.endInclusive.toLong()
                                            )
                                            if (Math.abs(range.start.toLong() - oldStart) > 100) {
                                                exoPlayer?.seekTo(range.start.toLong())
                                            } else {
                                                exoPlayer?.seekTo(range.endInclusive.toLong())
                                            }
                                        },
                                        valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(modifier = Modifier.padding(top = 8.dp)) {
                                        FilterChip(selected = !editState.isCutMode, onClick = { editState = editState.copy(isCutMode = false) }, label = { Text("Trim (Keep Middle)") })
                                        Spacer(Modifier.width(8.dp))
                                        FilterChip(selected = editState.isCutMode, onClick = { editState = editState.copy(isCutMode = true) }, label = { Text("Cut (Remove Middle)") })
                                    }
                                }
                            }
                            VideoEditorTool.SPEED -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Speed", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = String.format("%.2fx", editState.speed),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Slider(
                                        value = editState.speed,
                                        onValueChange = { editState = editState.copy(speed = it) },
                                        valueRange = 0.25f..16f,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(0.25f, 0.5f, 1f, 2f, 4f, 8f, 12f, 16f).forEach { preset ->
                                            FilterChip(
                                                selected = Math.abs(editState.speed - preset) < 0.05f,
                                                onClick = { editState = editState.copy(speed = preset) },
                                                label = { Text(if (preset == 1f) "1x (Normal)" else "${preset}x") }
                                            )
                                        }
                                    }
                                }
                            }
                            VideoEditorTool.AUDIO -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Volume", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "${(editState.volume * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Slider(
                                        value = editState.volume,
                                        onValueChange = { editState = editState.copy(volume = it) },
                                        valueRange = 0f..3f,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = editState.volume == 0f,
                                            onClick = { editState = editState.copy(volume = 0f) },
                                            label = { Text("Mute") }
                                        )
                                        FilterChip(
                                            selected = editState.volume == 1f,
                                            onClick = { editState = editState.copy(volume = 1f) },
                                            label = { Text("Normal") }
                                        )
                                        FilterChip(
                                            selected = editState.volume == 2f,
                                            onClick = { editState = editState.copy(volume = 2f) },
                                            label = { Text("Boost (200%)") }
                                        )
                                        FilterChip(
                                            selected = editState.volume == 3f,
                                            onClick = { editState = editState.copy(volume = 3f) },
                                            label = { Text("Max (300%)") }
                                        )
                                    }
                                }
                            }
                            VideoEditorTool.ASPECT_RATIO -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Aspect Ratio", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = editState.aspectRatio,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val ratios = listOf("Original", "16:9", "9:16", "1:1", "4:3", "21:9")
                                        ratios.forEach { ratio ->
                                            FilterChip(
                                                selected = editState.aspectRatio == ratio,
                                                onClick = { editState = editState.copy(aspectRatio = ratio) },
                                                label = { Text(ratio) }
                                            )
                                        }
                                    }
                                }
                            }
                            VideoEditorTool.ROTATE -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Rotate Video", style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val angles = listOf(0, 90, 180, 270)
                                        angles.forEach { angle ->
                                            FilterChip(
                                                selected = editState.rotateConfig == angle,
                                                onClick = { editState = editState.copy(rotateConfig = angle) },
                                                label = { Text(if (angle == 0) "Normal" else "${angle}°") }
                                            )
                                        }
                                    }
                                }
                            }
                            VideoEditorTool.CROP -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Crop Video Presets", style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("None", "Center Crop", "Custom").forEach { crop ->
                                            FilterChip(
                                                selected = editState.cropRect == crop,
                                                onClick = { editState = editState.copy(cropRect = crop) },
                                                label = { Text(crop) }
                                            )
                                        }
                                    }
                                }
                            }
                            VideoEditorTool.CAPTIONS -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Burn-in Custom Text", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                                        Switch(
                                            checked = editState.hasCaptions,
                                            onCheckedChange = { editState = editState.copy(hasCaptions = it) }
                                        )
                                    }
                                    if (editState.hasCaptions) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = editState.captionText,
                                            onValueChange = { editState = editState.copy(captionText = it) },
                                            label = { Text("Caption Text") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                            else -> Text("${currentTool.name} options here")
                        }
                    }
                }
            }
        }

        // Export Panel Overlay
        var format by remember { mutableStateOf("mp4") }
        var resolutionIndex by remember { mutableIntStateOf(0) } // 0 -> Original, 1 -> 144p, 2 -> 240p, 3 -> 360p, 4 -> 480p, 5 -> 720p, 6 -> 1080p
        var fpsIndex by remember { mutableIntStateOf(1) } // 0 -> 24fps, 1 -> 30fps, 2 -> 60fps
        var quality by remember { mutableFloatStateOf(0.7f) }
        var fastExport by remember { mutableStateOf(true) }

        // Calculate estimated size
        val baseKbps = when (resolutionIndex) {
            0 -> 5000f
            1 -> 100f
            2 -> 250f
            3 -> 500f
            4 -> 1000f
            5 -> 2500f
            else -> 5000f
        }
        val fpsMult = when(fpsIndex) {
            0 -> 0.8f
            1 -> 1.0f
            else -> 1.5f
        }
        val qualityMult = 0.5f + (quality * 1.0f)
        val estimatedKbps = baseKbps * fpsMult * qualityMult
        
        val effectiveStartMs = editState.trimStartMs.coerceAtLeast(0L)
        val effectiveEndMs = if (editState.trimEndMs > 0L) editState.trimEndMs else durationMs
        val trimmedDurationMs = if (editState.isCutMode) {
            durationMs - (effectiveEndMs - effectiveStartMs).coerceAtLeast(0L)
        } else {
            (effectiveEndMs - effectiveStartMs).coerceAtLeast(0L)
        }
        val durationSec = trimmedDurationMs / 1000f
        val estimatedSizeMb = (estimatedKbps * durationSec) / 8192f
        val estimatedSizeStr = String.format(java.util.Locale.US, "%.1f", estimatedSizeMb)

        if (showExportPanel) {
            AlertDialog(
                onDismissRequest = { showExportPanel = false },
                title = { Text("Export & Quality Control") },
                text = {
                    Column {
                        Text("Estimated file size: ~$estimatedSizeStr MB", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Resolution")
                        Slider(
                            value = resolutionIndex.toFloat(),
                            onValueChange = { resolutionIndex = it.toInt() },
                            valueRange = 0f..6f,
                            steps = 5
                        )
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Og", style = if (resolutionIndex == 0) MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.primary) else MaterialTheme.typography.labelSmall)
                            Text("144p", style = if (resolutionIndex == 1) MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.primary) else MaterialTheme.typography.labelSmall)
                            Text("240p", style = if (resolutionIndex == 2) MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.primary) else MaterialTheme.typography.labelSmall)
                            Text("360p", style = if (resolutionIndex == 3) MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.primary) else MaterialTheme.typography.labelSmall)
                            Text("480p", style = if (resolutionIndex == 4) MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.primary) else MaterialTheme.typography.labelSmall)
                            Text("720p", style = if (resolutionIndex == 5) MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.primary) else MaterialTheme.typography.labelSmall)
                            Text("1080p", style = if (resolutionIndex == 6) MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.primary) else MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Frame Rate")
                        Slider(
                            value = fpsIndex.toFloat(),
                            onValueChange = { fpsIndex = it.toInt() },
                            valueRange = 0f..2f,
                            steps = 1
                        )
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("24 fps", style = if (fpsIndex == 0) MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.primary) else MaterialTheme.typography.labelSmall)
                            Text("30 fps", style = if (fpsIndex == 1) MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.primary) else MaterialTheme.typography.labelSmall)
                            Text("60 fps", style = if (fpsIndex == 2) MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.primary) else MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Quality")
                        Slider(value = quality, onValueChange = { quality = it })
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = fastExport, onCheckedChange = { fastExport = it })
                            Text("Fast Export (ultrafast preset)")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Converter Format")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            FilterChip(selected = format == "mp4", onClick = { format = "mp4" }, label= { Text("mp4")})
                            FilterChip(selected = format == "mp3", onClick = { format = "mp3" }, label= { Text("mp3")})
                            FilterChip(selected = format == "gif", onClick = { format = "gif" }, label= { Text("gif")})
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        showExportPanel = false
                        
                        // 1. Determine parameters
                        val res = when (resolutionIndex) {
                            0 -> "Original"
                            1 -> "256x144"
                            2 -> "426x240"
                            3 -> "640x360"
                            4 -> "854x480"
                            5 -> "1280x720"
                            else -> "1920x1080"
                        }
                        val fps = when (fpsIndex) {
                            0 -> "24"
                            1 -> "30"
                            else -> "60"
                        }
                        val crf = (35 - (quality * 17)).toInt()
                        
                        // 2. Build FFmpeg command template based on edits
                        val filterList = mutableListOf<String>()
                        val audioFilterList = mutableListOf<String>()
                        var trimArgs = ""

                        if (editState.isCutMode) {
                            // Cut (Remove Middle) mode
                            val startS = editState.trimStartMs / 1000f
                            val endS = editState.trimEndMs / 1000f
                            filterList.add("select='not(between(t,$startS,$endS))'")
                            filterList.add("setpts=N/FRAME_RATE/TB")
                            audioFilterList.add("aselect='not(between(t,$startS,$endS))'")
                            audioFilterList.add("asetpts=N/SR/TB")
                        } else {
                            // Trim (Keep Middle) mode
                            trimArgs = "-ss ${editState.trimStartMs / 1000f} -to ${editState.trimEndMs / 1000f}"
                        }

                        if (editState.speed != 1.0f) {
                            filterList.add("setpts=PTS/${editState.speed}")
                            audioFilterList.add("atempo=${editState.speed}")
                        }
                        if (editState.volume != 1.0f) {
                            audioFilterList.add("volume=${editState.volume}")
                        }

                        if (editState.rotateConfig != 0) {
                            val rotFilter = when (editState.rotateConfig) {
                                90 -> "transpose=1"
                                180 -> "transpose=2,transpose=2"
                                270 -> "transpose=2"
                                else -> ""
                            }
                            if (rotFilter.isNotEmpty()) filterList.add(rotFilter)
                        }
                        if (editState.cropRect != "None" && editState.cropRect.isNotBlank()) {
                            if (editState.cropRect == "Center Crop") {
                                filterList.add("crop='min(iw,ih)':'min(iw,ih)'")
                            } else if (editState.cropRect == "Custom") {
                                val cw = "iw*${editState.cropRight - editState.cropLeft}"
                                val ch = "ih*${editState.cropBottom - editState.cropTop}"
                                val cx = "iw*${editState.cropLeft}"
                                val cy = "ih*${editState.cropTop}"
                                filterList.add("crop=$cw:$ch:$cx:$cy")
                            }
                        }
                        if (editState.aspectRatio != "Original") {
                            val cropFilter = when (editState.aspectRatio) {
                                "16:9" -> "crop=w='min(iw,ih*16/9)':h='min(ih,iw*9/16)'"
                                "9:16" -> "crop=w='min(iw,ih*9/16)':h='min(ih,iw*16/9)'"
                                "1:1" -> "crop=w='min(iw,ih)':h='min(ih,iw)'"
                                "4:3" -> "crop=w='min(iw,ih*4/3)':h='min(ih,iw*3/4)'"
                                "21:9" -> "crop=w='min(iw,ih*21/9)':h='min(ih,iw*9/21)'"
                                else -> ""
                            }
                            if (cropFilter.isNotEmpty()) filterList.add(cropFilter)
                        }
                        if (editState.hasCaptions && editState.captionText.isNotBlank()) {
                            val safeText = editState.captionText.replace("'", "")
                            filterList.add("drawtext=text='$safeText':fontcolor=white:fontsize=48:x=(w-text_w)/2:y=h-th-50:fontfile=/system/fonts/Roboto-Regular.ttf")
                        }
                        
                        if (res != "Original") {
                            val parts = res.split("x")
                            val targetW = parts[0].toInt()
                            val targetH = parts[1].toInt()
                            filterList.add("scale=w=$targetW:h=$targetH:force_original_aspect_ratio=decrease:flags=lanczos,pad=$targetW:$targetH:(ow-iw)/2:(oh-ih)/2")
                        }
                        
                        val videoFilterArgs = if (filterList.isNotEmpty()) {
                            "-vf \"${filterList.joinToString(",")}\""
                        } else {
                            ""
                        }
                        
                        val audioFilterArgs = if (audioFilterList.isNotEmpty()) {
                            "-af \"${audioFilterList.joinToString(",")}\""
                        } else {
                            ""
                        }
                        
                        // GIF Filters
                        val gifFilters = mutableListOf<String>()
                        if (filterList.isNotEmpty()) {
                            gifFilters.addAll(filterList)
                        }
                        if (res == "Original") {
                            gifFilters.add("fps=$fps,scale=-2:480:flags=lanczos")
                        } else {
                            gifFilters.add("fps=$fps")
                        }
                        val gifFilterArgs = "-vf \"${gifFilters.joinToString(",")}\""

                        val presetArg = if (fastExport) "ultrafast" else "medium"

                        val cmd = when (format) {
                            "mp4" -> "-y $trimArgs -i %INPUT% $videoFilterArgs $audioFilterArgs -r $fps -vcodec libx264 -crf $crf -preset $presetArg %OUTPUT%"
                            "mp3" -> "-y $trimArgs -i %INPUT% -vn $audioFilterArgs -acodec libmp3lame -q:a 2 %OUTPUT%"
                            "gif" -> "-y $trimArgs -i %INPUT% $gifFilterArgs -loop 0 %OUTPUT%"
                            else -> "-y -i %INPUT% %OUTPUT%"
                        }
                        
                        LogKeeper.log("Starting Render job for video file. Output Format: $format, Resolution: $res, FPS: $fps, Preset: $presetArg, Quality level: $quality (CRF $crf)", "VideoEditor")
                        LogKeeper.log("Constructed FFmpeg Command: $cmd", "VideoEditor")
                        
                        // 3. Start FFmpegService
                        val intent = android.content.Intent(context, com.example.service.FFmpegService::class.java).apply {
                            putStringArrayListExtra("uris", arrayListOf(effectiveUri))
                            putStringArrayListExtra("original_names", arrayListOf(com.example.ui.screens.getDisplayNameFromUri(context, initialUri)))
                            putExtra("commandTemplate", cmd)
                            putExtra("outputExt", format)
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    }) {
                        Text("SAVE (Render)")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportPanel = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ToolIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
