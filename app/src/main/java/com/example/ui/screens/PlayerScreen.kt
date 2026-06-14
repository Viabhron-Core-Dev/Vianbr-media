package com.example.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Build
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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

    var activeGesture by remember { mutableStateOf(GestureType.NONE) }
    var gestureText by remember { mutableStateOf("") }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

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
            controller.setMediaItem(MediaItem.Builder().setMediaId(decodedUri.toString()).build())
            controller.prepare()
            
            val lastPos = settingsManager.getPlaybackPosition(decodedUriString)
            if (lastPos > 0 && !settingsManager.isFinished(decodedUriString)) {
                controller.seekTo(lastPos)
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
                controller.stop()
                controller.release()
            }
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
                                    gestureText = "Volume: $newVolume / $maxVolume"
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

        if (activeGesture != GestureType.NONE && gestureText.isNotEmpty()) {
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

        androidx.compose.animation.AnimatedVisibility(
            visible = !showControls,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Box(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(16.dp)
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
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showControls,
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
                        .height(140.dp)
                        .align(Alignment.TopCenter)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)))
                    )
                    
                    // Top controls overlay
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .windowInsetsPadding(WindowInsets.systemBars)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Text(
                                text = java.io.File(decodedUriString).nameWithoutExtension,
                                color = Color.White,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            )
                            IconButton(onClick = { /* Speed */ }) {
                                Icon(Icons.Filled.Speed, contentDescription = "Speed", tint = Color.White)
                            }
                            IconButton(onClick = { /* Audio */ }) {
                                Icon(Icons.Filled.Headphones, contentDescription = "Audio track", tint = Color.White)
                            }
                            IconButton(onClick = { /* Subtitle */ }) {
                                Icon(Icons.Filled.Subtitles, contentDescription = "Subtitles", tint = Color.White)
                            }
                            IconButton(onClick = { /* More */ }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Color.White)
                            }
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            IconButton(onClick = { /* Repeat */ }) {
                                Icon(Icons.Filled.Repeat, contentDescription = "Repeat", tint = Color.White)
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { showBrightnessSlider = !showBrightnessSlider }) {
                                Icon(Icons.Filled.LightMode, contentDescription = "Brightness", tint = Color.White)
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { /* Screenshot */ }) {
                                Icon(Icons.Filled.Screenshot, contentDescription = "Screenshot", tint = Color.White)
                            }
                            Spacer(Modifier.weight(1f))
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
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { /* Background play */ }) {
                                Icon(Icons.Filled.Headphones, contentDescription = "Background play", tint = Color.White)
                            }
                        }
                        
                        androidx.compose.animation.AnimatedVisibility(visible = showBrightnessSlider) {
                            var brightness by remember { mutableFloatStateOf(context.findActivity()?.window?.attributes?.screenBrightness.takeIf { it != -1f } ?: 0.5f) }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Icon(Icons.Filled.LightMode, contentDescription = null, tint = Color.White)
                                Slider(
                                    value = brightness,
                                    onValueChange = { newVal ->
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
                    }

                    // Bottom controls background
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                    )

                    // Bottom controls overlay
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.systemBars)
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = formatTime(currentPosition),
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Slider(
                                value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                                onValueChange = { scale ->
                                    mediaController?.seekTo((scale * duration).toLong())
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                            )
                            Text(
                                text = if (showRemainingTime && duration > 0) "-" + formatTime(duration - currentPosition) else formatTime(duration),
                                color = Color.White,
                                fontSize = 14.sp,
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
                                Icon(Icons.Filled.AspectRatio, contentDescription = "Aspect Ratio", tint = Color.White)
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
}


