package com.example.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import kotlin.math.roundToInt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Switch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.VideoSize
import android.content.pm.ActivityInfo
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import com.example.service.PlaybackService
import kotlinx.coroutines.delay

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

enum class GestureType { NONE, SEEK, BRIGHTNESS, VOLUME, ZOOM_PAN }

fun getDisplayNameFromUri(context: android.content.Context, uri: Uri): String {
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    val name = cursor.getString(index)
                    if (name != null) return name.substringBeforeLast('.')
                }
            }
        }
    }
    return uri.lastPathSegment?.substringBeforeLast('.') ?: "Unknown"
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    uriString: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var mediaController by remember { mutableStateOf<MediaController?>(null) }
    var showControls by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isLocked by remember { mutableStateOf(false) }
    var showRemainingTime by remember { mutableStateOf(false) }
    var resizeMode by remember { androidx.compose.runtime.mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var showBrightnessSlider by remember { mutableStateOf(false) }
    var brightnessInteractionTime by remember { mutableLongStateOf(0L) }

    // Auto-hide brightness slider after inactivity
    LaunchedEffect(showBrightnessSlider, brightnessInteractionTime) {
        if (showBrightnessSlider) {
            kotlinx.coroutines.delay(3000)
            showBrightnessSlider = false
        }
    }

    var activeGesture by remember { mutableStateOf(GestureType.NONE) }
    var gestureText by remember { mutableStateOf("") }
    var gestureVolumeRatio by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var backgroundPlayEnabled by remember { mutableStateOf(false) }
    val backgroundPlayEnabledRef = androidx.compose.runtime.rememberUpdatedState(backgroundPlayEnabled)
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var skipSilence by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    val playerViewRef = remember { mutableStateOf<PlayerView?>(null) }
    val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

    LaunchedEffect(showControls) {
        val window = context.findActivity()?.window
        if (window != null) {
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (showControls) {
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            } else {
                insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            context.findActivity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            val window = context.findActivity()?.window
            if (window != null) {
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(mediaController) {
        while (true) {
            if (mediaController != null) {
                currentPosition = mediaController!!.currentPosition.coerceAtLeast(0L)
                duration = mediaController!!.duration.coerceAtLeast(1L)
            }
            delay(500)
        }
    }

    LaunchedEffect(showControls) {
        if (showControls) {
            delay(5000)
            showControls = false
        }
    }

    LaunchedEffect(uriString) {
        val decodedUriString = String(android.util.Base64.decode(uriString, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))
        val decodedUri = Uri.parse(decodedUriString)
        val settingsManager = com.example.data.SettingsManager.getInstance(context)
        com.example.LogKeeper.log("Starting player for $decodedUri", "PlayerScreen")
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        
        controllerFuture.addListener({
            val controller = controllerFuture.get()
            mediaController = controller
            controller.addListener(object : androidx.media3.common.Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    com.example.LogKeeper.logError("PlayerScreen", "ExoPlayer Error: ${error.message}", error)
                }
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        context.findActivity()?.requestedOrientation = if (videoSize.width > videoSize.height) {
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                        }
                    }
                }
            })
            if (controller.currentMediaItem?.mediaId != decodedUri.toString()) {
                controller.setMediaItem(MediaItem.Builder().setMediaId(decodedUri.toString()).build())
                controller.prepare()
                
                val lastPos = settingsManager.getPlaybackPosition(decodedUriString)
                if (lastPos > 0 && !settingsManager.isFinished(decodedUriString)) {
                    controller.seekTo(lastPos)
                }
            }
            
            controller.play()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.findActivity()?.setPictureInPictureParams(
                    PictureInPictureParams.Builder()
                        .setAutoEnterEnabled(true)
                        .build()
                )
            }
        }, ContextCompat.getMainExecutor(context))
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val decodedUriString = String(android.util.Base64.decode(uriString, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))

    DisposableEffect(lifecycleOwner, mediaController) {
        val currentController = mediaController
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                currentController?.let { controller ->
                    val currentPos = controller.currentPosition
                    val dur = controller.duration
                    com.example.data.SettingsManager.getInstance(context).savePlaybackState(decodedUriString, currentPos, dur)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.findActivity()?.setPictureInPictureParams(
                    PictureInPictureParams.Builder()
                        .setAutoEnterEnabled(false)
                        .build()
                )
            }
            currentController?.let { controller ->
                val currentPos = controller.currentPosition
                val dur = controller.duration
                com.example.data.SettingsManager.getInstance(context).savePlaybackState(decodedUriString, currentPos, dur)
                if (!backgroundPlayEnabledRef.value) {
                    controller.stop()
                }
                controller.release()
            }
        }
    }

    var isInPipMode by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val activity = context.findActivity() as? androidx.activity.ComponentActivity
        val pipListener = androidx.core.util.Consumer<androidx.core.app.PictureInPictureModeChangedInfo> { info ->
            isInPipMode = info.isInPictureInPictureMode
        }
        activity?.addOnPictureInPictureModeChangedListener(pipListener)
        onDispose {
            activity?.removeOnPictureInPictureModeChangedListener(pipListener)
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .pointerInput(mediaController, isLocked) {
            detectTapGestures(
                onDoubleTap = {
                    if (isLocked) return@detectTapGestures
                    mediaController?.let { controller ->
                        if (controller.isPlaying) {
                            controller.pause()
                            showControls = true
                        } else {
                            controller.play()
                            showControls = false
                        }
                    }
                },
                onTap = {
                    showControls = !showControls
                }
            )
        }
        .pointerInput(isLocked) {
            if (isLocked) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var currentGesture = GestureType.NONE
                var dragDistanceX = 0f
                var dragDistanceY = 0f
                
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                val startVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                
                val window = context.findActivity()?.window
                var startBrightness = window?.attributes?.screenBrightness ?: -1f
                if (startBrightness < 0) startBrightness = 0.5f
                
                val startPosition = currentPosition

                do {
                    val event = awaitPointerEvent()
                    val changes = event.changes
                    
                    if (changes.size > 1) {
                        currentGesture = GestureType.ZOOM_PAN
                        activeGesture = GestureType.ZOOM_PAN
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        
                        scale = (scale * zoomChange).coerceIn(0.25f, 4f)
                        offsetX += panChange.x
                        offsetY += panChange.y
                        
                        changes.forEach { if (it.positionChanged()) it.consume() }
                    } else if (currentGesture != GestureType.ZOOM_PAN) {
                        val change = changes.firstOrNull()
                        if (change != null) {
                            val posChange = change.positionChange()
                            dragDistanceX += posChange.x
                            dragDistanceY += posChange.y
                            
                            if (currentGesture == GestureType.NONE) {
                                if (kotlin.math.abs(dragDistanceX) > 20f || kotlin.math.abs(dragDistanceY) > 20f) {
                                    if (kotlin.math.abs(dragDistanceX) > kotlin.math.abs(dragDistanceY)) {
                                        currentGesture = GestureType.SEEK
                                    } else {
                                        currentGesture = GestureType.VOLUME
                                    }
                                    activeGesture = currentGesture
                                }
                            }
                            
                            when (currentGesture) {
                                GestureType.SEEK -> {
                                    val seekOffsetMs = (dragDistanceX / size.width) * 120_000
                                    val targetPos = (startPosition + seekOffsetMs.toLong()).coerceIn(0L, duration)
                                    gestureText = "Seek: ${formatTime(targetPos)} / ${formatTime(duration)}"
                                    change.consume()
                                }
                                GestureType.VOLUME -> {
                                    val volumeChange = -(dragDistanceY / size.height) * maxVolume
                                    val newVolume = (startVolume + volumeChange).toInt().coerceIn(0, maxVolume)
                                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVolume, 0)
                                    gestureVolumeRatio = newVolume.toFloat() / maxVolume.toFloat()
                                    gestureText = "Volume: ${(gestureVolumeRatio * 100).roundToInt()}%"
                                    change.consume()
                                }
                                else -> {}
                            }
                        }
                    }
                } while (event.changes.any { it.pressed })
                
                if (currentGesture == GestureType.SEEK) {
                    val seekOffsetMs = (dragDistanceX / size.width) * 120_000
                    val targetPos = (startPosition + seekOffsetMs.toLong()).coerceIn(0L, duration)
                    mediaController?.seekTo(targetPos)
                }
                
                activeGesture = GestureType.NONE
                gestureText = ""
            }
        }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    fitsSystemWindows = false
                    setOnApplyWindowInsetsListener { _, insets -> insets }
                    playerViewRef.value = this
                }
            },
            update = { view ->
                view.player = mediaController
                view.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offsetX
                translationY = offsetY
            }
        )

        if (activeGesture != GestureType.NONE && !isInPipMode) {
            if (activeGesture == GestureType.VOLUME) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(start = 32.dp)
                            .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                            .padding(vertical = 12.dp, horizontal = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .height(140.dp)
                                .width(4.dp)
                                .background(Color.DarkGray.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(gestureVolumeRatio.coerceIn(0f, 1f))
                                    .width(4.dp)
                                    .background(Color(0xFF2196F3), androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Icon(androidx.compose.material.icons.Icons.Filled.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            } else if (gestureText.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = gestureText,
                        color = Color.White,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).padding(16.dp)
                    )
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = !showControls && !isInPipMode,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Box(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(top = 4.dp, end = 8.dp)
            ) {
                var timeInfo by remember { mutableStateOf("") }
                LaunchedEffect(Unit) {
                    val intentFilter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
                    while (true) {
                        val cal = java.util.Calendar.getInstance()
                        val timeStr = String.format(java.util.Locale.US, "%02d:%02d", cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
                        val batteryStatus = context.registerReceiver(null, intentFilter)
                        val level = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: 100
                        val scale = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: 100
                        val percentage = if (scale > 0) (level * 100) / scale else 100
                        timeInfo = "$timeStr • $percentage%"
                        delay(10000) // Update every 10 seconds
                    }
                }
                Text(
                    text = timeInfo,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                )
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showControls && !isInPipMode,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLocked) {
                Box(modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = { isLocked = false },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = "Unlock", tint = Color.White)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Top controls background
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.TopCenter)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
                    )
                    
                    // Top controls overlay
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .windowInsetsPadding(WindowInsets.systemBars)
                    ) {
                        val displayName = remember(decodedUriString) { getDisplayNameFromUri(context, Uri.parse(decodedUriString)) }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Text(
                                text = displayName,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            )
                            IconButton(onClick = { 
                                showSpeedDialog = true
                            }) {
                                Icon(Icons.Filled.Speed, contentDescription = "Speed", tint = if (playbackSpeed != 1f) Color(0xFF2196F3) else Color.White)
                            }
                            IconButton(onClick = { showAudioDialog = true }) {
                                Icon(Icons.Filled.Headphones, contentDescription = "Audio track", tint = Color.White)
                            }
                            IconButton(onClick = { showSubtitleDialog = true }) {
                                Icon(Icons.Filled.Subtitles, contentDescription = "Subtitles", tint = Color.White)
                            }
                            IconButton(onClick = { 
                                val surfaceView = playerViewRef.value?.videoSurfaceView as? android.view.SurfaceView
                                if (surfaceView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    val bitmap = android.graphics.Bitmap.createBitmap(surfaceView.width, surfaceView.height, android.graphics.Bitmap.Config.ARGB_8888)
                                    android.view.PixelCopy.request(surfaceView, bitmap, { result ->
                                        if (result == android.view.PixelCopy.SUCCESS) {
                                            val filename = "Screenshot_${System.currentTimeMillis()}.png"
                                            val values = android.content.ContentValues().apply {
                                                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                                                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES)
                                                }
                                            }
                                            val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                                            uri?.let {
                                                context.contentResolver.openOutputStream(it)?.use { out ->
                                                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                                                }
                                                Toast.makeText(context, "Screenshot saved to Photos", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Screenshot failed (PixelCopy error)", Toast.LENGTH_SHORT).show()
                                        }
                                    }, android.os.Handler(android.os.Looper.getMainLooper()))
                                } else {
                                    Toast.makeText(context, "Screenshot failed: Surface not ready or unsupported OS", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Filled.Screenshot, contentDescription = "Screenshot", tint = Color.White)
                            }
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            IconButton(onClick = { /* Repeat */ }) {
                                Icon(Icons.Filled.Repeat, contentDescription = "Repeat", tint = Color.White)
                            }
                            IconButton(onClick = { 
                                showBrightnessSlider = !showBrightnessSlider 
                                if (showBrightnessSlider) {
                                    brightnessInteractionTime = System.currentTimeMillis()
                                }
                            }) {
                                Icon(Icons.Filled.LightMode, contentDescription = "Brightness", tint = Color.White)
                            }
                            IconButton(onClick = {
                                val currentOrientation = context.findActivity()?.requestedOrientation
                                if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                                    context.findActivity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                                } else {
                                    context.findActivity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                }
                            }) {
                                Icon(Icons.Filled.ScreenRotation, contentDescription = "Rotation", tint = Color.White)
                            }
                            IconButton(onClick = { 
                                backgroundPlayEnabled = !backgroundPlayEnabled
                                Toast.makeText(context, "Background play " + if (backgroundPlayEnabled) "enabled" else "disabled", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Filled.Headphones, contentDescription = "Background play", tint = if (backgroundPlayEnabled) Color(0xFF2196F3) else Color.White)
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = showBrightnessSlider,
                        enter = androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.fadeOut(),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        var brightness by remember { mutableFloatStateOf(context.findActivity()?.window?.attributes?.screenBrightness.takeIf { it != -1f } ?: 0.5f) }
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Filled.LightMode, contentDescription = null, tint = Color.White)
                            Slider(
                                value = brightness,
                                onValueChange = { newVal ->
                                    brightnessInteractionTime = System.currentTimeMillis()
                                    brightness = newVal
                                    val window = context.findActivity()?.window
                                    window?.let {
                                        val lp = it.attributes
                                        lp.screenBrightness = newVal
                                        it.attributes = lp
                                    }
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                            )
                        }
                    }

                    // Bottom controls background
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                    )

                    // Bottom controls overlay
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.systemBars)
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = formatTime(currentPosition),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Slider(
                                value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                                onValueChange = { scale ->
                                    mediaController?.seekTo((scale * duration).toLong())
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF2196F3),
                                    activeTrackColor = Color(0xFF2196F3),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                                    .height(24.dp)
                            )
                            Text(
                                text = if (showRemainingTime && duration > 0) "-" + formatTime(duration - currentPosition) else formatTime(duration),
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable { showRemainingTime = !showRemainingTime }
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            IconButton(onClick = { isLocked = true }) {
                                Icon(Icons.Filled.LockOpen, contentDescription = "Lock", tint = Color.White)
                            }
                            IconButton(onClick = { mediaController?.seekToPrevious() }) {
                                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White)
                            }
                            IconButton(
                                onClick = {
                                    mediaController?.let { controller ->
                                        if (controller.isPlaying) controller.pause() else controller.play()
                                    }
                                }
                            ) {
                                Icon(
                                    if (mediaController?.isPlaying == true) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = { mediaController?.seekToNext() }) {
                                Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White)
                            }
                            IconButton(onClick = {
                                resizeMode = when (resizeMode) {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                    AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                }
                            }) {
                                val resizeIcon = when (resizeMode) {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT -> Icons.Filled.FullscreenExit
                                    AspectRatioFrameLayout.RESIZE_MODE_FILL -> Icons.Filled.Fullscreen
                                    else -> Icons.Filled.Crop
                                }
                                Icon(resizeIcon, contentDescription = "Aspect Ratio", tint = Color.White)
                            }
                            IconButton(onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.findActivity()?.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                                }
                            }) {
                                Icon(Icons.Filled.PictureInPictureAlt, contentDescription = "PiP", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAudioDialog) {
        Dialog(onDismissRequest = { showAudioDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Select Audio Track", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val currentTracks = mediaController?.currentTracks
                    val audioGroups = currentTracks?.groups?.filter { it.type == androidx.media3.common.C.TRACK_TYPE_AUDIO } ?: emptyList()
                    
                    if (audioGroups.isEmpty()) {
                        Text("No audio tracks available", style = MaterialTheme.typography.bodyLarge)
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn {
                            items(audioGroups.size) { groupIndex ->
                                val group = audioGroups[groupIndex]
                                for (trackIndex in 0 until group.length) {
                                    val format = group.getTrackFormat(trackIndex)
                                    val isSelected = group.isSelected
                                    val title = format.language ?: format.label ?: "Track ${trackIndex + 1}"
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            val builder = mediaController?.trackSelectionParameters?.buildUpon()
                                            builder?.setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                                            builder?.let { mediaController?.trackSelectionParameters = it.build() }
                                            showAudioDialog = false
                                        }.padding(vertical = 12.dp)
                                    ) {
                                        Text(title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        androidx.compose.material3.TextButton(onClick = { showAudioDialog = false }) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    if (showSubtitleDialog) {
        Dialog(onDismissRequest = { showSubtitleDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Select Subtitles", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val currentTracks = mediaController?.currentTracks
                    val textGroups = currentTracks?.groups?.filter { it.type == androidx.media3.common.C.TRACK_TYPE_TEXT } ?: emptyList()
                    val trackSelectionParameters = mediaController?.trackSelectionParameters
                    val isTextDisabled = trackSelectionParameters?.disabledTrackTypes?.contains(androidx.media3.common.C.TRACK_TYPE_TEXT) ?: false
                    
                    if (textGroups.isEmpty()) {
                        Text("No subtitles available", style = MaterialTheme.typography.bodyLarge)
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        val builder = mediaController?.trackSelectionParameters?.buildUpon()
                                        builder?.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true)
                                        builder?.clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_TEXT)
                                        builder?.let { mediaController?.trackSelectionParameters = it.build() }
                                        showSubtitleDialog = false
                                    }.padding(vertical = 12.dp)
                                ) {
                                    Text("Off", fontWeight = if (isTextDisabled) FontWeight.Bold else FontWeight.Normal, color = if (isTextDisabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            
                            items(textGroups.size) { groupIndex ->
                                val group = textGroups[groupIndex]
                                for (trackIndex in 0 until group.length) {
                                    val format = group.getTrackFormat(trackIndex)
                                    val isSelected = !isTextDisabled && group.isSelected
                                    val title = format.language ?: format.label ?: "Track ${trackIndex + 1}"
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            val builder = mediaController?.trackSelectionParameters?.buildUpon()
                                            builder?.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                                            builder?.setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                                            builder?.let { mediaController?.trackSelectionParameters = it.build() }
                                            showSubtitleDialog = false
                                        }.padding(vertical = 12.dp)
                                    ) {
                                        Text(title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        androidx.compose.material3.TextButton(onClick = { showSubtitleDialog = false }) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    if (showSpeedDialog) {
        Dialog(onDismissRequest = { showSpeedDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Select playback speed", style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { 
                                val newSpeed = maxOf(0.1f, playbackSpeed - 0.1f)
                                playbackSpeed = Math.round(newSpeed * 10.0f) / 10.0f
                                mediaController?.setPlaybackSpeed(playbackSpeed)
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                        }
                        
                        Text(String.format("%.1f", playbackSpeed), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        
                        IconButton(
                            onClick = { 
                                val newSpeed = minOf(3.0f, playbackSpeed + 0.1f)
                                playbackSpeed = Math.round(newSpeed * 10.0f) / 10.0f
                                mediaController?.setPlaybackSpeed(playbackSpeed)
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Increase")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = playbackSpeed,
                            onValueChange = { 
                                playbackSpeed = Math.round(it * 10.0f) / 10.0f
                                mediaController?.setPlaybackSpeed(playbackSpeed)
                            },
                            valueRange = 0.1f..3.0f,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { 
                            playbackSpeed = 1.0f
                            mediaController?.setPlaybackSpeed(playbackSpeed)
                        }) {
                            Icon(Icons.Filled.Restore, contentDescription = "Reset")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val predefinedSpeeds = listOf(0.2f, 0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f)
                        predefinedSpeeds.forEach { speed ->
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        playbackSpeed = speed
                                        mediaController?.setPlaybackSpeed(playbackSpeed)
                                    }
                                    .border(1.dp, if (playbackSpeed == speed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                                    .background(if (playbackSpeed == speed) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("${speed}x", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Skip silence", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = skipSilence,
                            onCheckedChange = { 
                                skipSilence = it
                                // In Media3, use setSkipSilenceEnabled on Player
                                try {
                                    val method = mediaController?.javaClass?.getMethod("setSkipSilenceEnabled", Boolean::class.javaPrimitiveType)
                                    method?.invoke(mediaController, skipSilence)
                                } catch (e: Exception) {
                                    // Ignore if not supported
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}


