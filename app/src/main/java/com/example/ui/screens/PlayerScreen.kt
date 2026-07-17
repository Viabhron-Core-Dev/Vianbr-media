@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.widthIn

import androidx.compose.foundation.layout.heightIn
import kotlinx.coroutines.launch
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Loop
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryChargingFull
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

@Composable
fun CompactPlayerDialog(
    onDismissRequest: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            modifier = Modifier.widthIn(max = 360.dp)
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    .padding(16.dp)
            ) {
                content()
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.media3.common.util.UnstableApi::class)
@Composable
fun PlayerScreen(
    uriString: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var mediaController by remember { mutableStateOf(com.example.service.PlayerManager.exoPlayer) }
    var showPlayerSettingsDialog by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var resizeMode by remember { androidx.compose.runtime.mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var showBrightnessSlider by remember { mutableStateOf(false) }
    var brightnessInteractionTime by remember { mutableLongStateOf(0L) }
    var boostGainMb by remember { androidx.compose.runtime.mutableIntStateOf(0) }

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
    var seekOffsetSec by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var showPlayPauseFlash by remember { mutableStateOf(false) }
    var flashIsPlaying by remember { mutableStateOf(false) }
    
    LaunchedEffect(showPlayPauseFlash) {
        if (showPlayPauseFlash) {
            kotlinx.coroutines.delay(400)
            showPlayPauseFlash = false
        }
    }
    var isPlaying by remember { mutableStateOf(false) }
    
    var abRepeatStart by remember { mutableStateOf<Long?>(null) }
    var abRepeatEnd by remember { mutableStateOf<Long?>(null) }
    var sleepTimerEndTime by remember { mutableStateOf<Long?>(null) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(mediaController) {
        while (true) {
            isPlaying = mediaController?.isPlaying == true
            
            // A-B repeat check
            if (abRepeatStart != null && abRepeatEnd != null && isPlaying) {
                val currentPos = mediaController?.currentPosition ?: 0L
                if (currentPos >= abRepeatEnd!!) {
                    mediaController?.seekTo(abRepeatStart!!)
                }
            }
            
            // Sleep timer check
            if (sleepTimerEndTime != null && isPlaying) {
                if (System.currentTimeMillis() >= sleepTimerEndTime!!) {
                    mediaController?.pause()
                    sleepTimerEndTime = null
                }
            }
            
            kotlinx.coroutines.delay(300)
        }
    }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var backgroundPlayEnabled by remember { mutableStateOf(false) }
    val backgroundPlayEnabledRef = androidx.compose.runtime.rememberUpdatedState(backgroundPlayEnabled)
    val settingsManager = com.example.data.SettingsManager.getInstance(context)
    val decodedUriStringForInit = remember(uriString) { String(android.util.Base64.decode(uriString, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)) }
    var playbackSpeed by remember { mutableFloatStateOf(settingsManager.getPlaybackSpeed(decodedUriStringForInit)) }
    var skipSilence by remember { mutableStateOf(false) }
    var repeatMode by remember { androidx.compose.runtime.mutableIntStateOf(androidx.media3.common.Player.REPEAT_MODE_OFF) }
        var showSpeedDialog by remember { mutableStateOf(false) }
        var showAudioDialog by remember { mutableStateOf(false) }
        var showSubtitleDialog by remember { mutableStateOf(false) }
        var showTopMenu by remember { mutableStateOf(false) }
        
        var subtitleColor by remember { mutableStateOf(androidx.compose.ui.graphics.Color.White) }
        var subtitleSize by remember { mutableStateOf(16f) }
        var subtitleDelay by remember { mutableStateOf(0f) }

        val audioPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                Toast.makeText(context, "Added audio: $uri", Toast.LENGTH_SHORT).show()
                // In a full implementation, you'd send this URI to the PlaybackService to add to the MergingMediaSource
            }
        }

        val subtitlePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                Toast.makeText(context, "Added subtitle: $uri", Toast.LENGTH_SHORT).show()
                com.example.service.PlayerManager.addSubtitle(uri.toString())
            }
        }
    val playerViewRef = remember { mutableStateOf<PlayerView?>(null) }
    val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

    LaunchedEffect(showControls) {
        val window = context.findActivity()?.window
        if (window != null) {
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (showControls) {
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
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
                repeatMode = mediaController!!.repeatMode
            }
            delay(500)
        }
    }

    LaunchedEffect(showControls) {
        if (!showControls) {
            showBrightnessSlider = false
        } else {
            kotlinx.coroutines.delay(4000)
            showControls = false
        }
    }

    val decodedUriString = remember(uriString) { String(android.util.Base64.decode(uriString, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)) }
    val decodedUri = remember(uriString) { Uri.parse(decodedUriString) }

    LaunchedEffect(decodedUri) {
        val mimeType = context.contentResolver.getType(decodedUri)
        val isAudio = mimeType?.startsWith("audio/") == true
        if (isAudio) {
            val settingsManager = com.example.data.SettingsManager.getInstance(context)
            backgroundPlayEnabled = settingsManager.defaultAudioBackgroundPlay
        }
    }


    LaunchedEffect(uriString) {
        val controller = mediaController ?: return@LaunchedEffect
        val settingsManager = com.example.data.SettingsManager.getInstance(context)
        
        if (controller.currentMediaItem?.mediaId != decodedUri.toString()) {
            val mediaMetadataBuilder = androidx.media3.common.MediaMetadata.Builder()
            var fileName = decodedUri.lastPathSegment ?: "Unknown"
            if (decodedUri.scheme == "file") {
                try { fileName = java.io.File(decodedUri.path!!).name } catch (e: Exception) {}
            } else if (decodedUri.scheme == "content") {
                try {
                    context.contentResolver.query(decodedUri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameCol = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
                            if (nameCol != -1) cursor.getString(nameCol)?.let { fileName = it }
                        }
                    }
                } catch (e: Exception) {}
            }
            mediaMetadataBuilder.setTitle(fileName)
            mediaMetadataBuilder.setDisplayTitle(fileName)
            mediaMetadataBuilder.setArtworkUri(decodedUri)

            val initialMediaItem = MediaItem.Builder()
                .setUri(decodedUri)
                .setMediaId(decodedUri.toString())
                .setMediaMetadata(mediaMetadataBuilder.build())
                .build()

            controller.setMediaItem(initialMediaItem)
            controller.prepare()
            
            val lastPos = settingsManager.getPlaybackPosition(decodedUriString)
            if (lastPos > 0 && !settingsManager.isFinished(decodedUriString)) {
                controller.seekTo(lastPos)
            }
            controller.play()
            
            // Load playlist in background
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                val repository = com.example.data.MediaRepository(context.applicationContext as android.app.Application)
                val folders = repository.getMediaFolders()
                var playlistItems = emptyList<MediaItem>()
                var startIndex = 0
                var found = false
                
                for (folder in folders) {
                    val index = folder.mediaItems.indexOfFirst { it.uri == decodedUri }
                    if (index != -1) {
                        playlistItems = folder.mediaItems.map { item ->
                            val meta = androidx.media3.common.MediaMetadata.Builder()
                                .setTitle(item.name)
                                .setDisplayTitle(item.name)
                                .setArtworkUri(item.uri)
                                .build()
                            MediaItem.Builder()
                                .setUri(item.uri)
                                .setMediaId(item.uri.toString())
                                .setMediaMetadata(meta)
                                .build()
                        }
                        startIndex = index
                        found = true
                        break
                    }
                }
                
                if (found && playlistItems.size > 1) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (controller.currentMediaItem?.mediaId == decodedUri.toString()) {
                            val currentPos = controller.currentPosition
                            val isPlaying = controller.isPlaying
                            controller.setMediaItems(playlistItems, startIndex, currentPos)
                            if (isPlaying) {
                                controller.play()
                            }
                        }
                    }
                }
            }
            

        } else if (controller.playbackState == androidx.media3.common.Player.STATE_ENDED || controller.playbackState == androidx.media3.common.Player.STATE_IDLE) {
            controller.seekTo(0)
            controller.prepare()
            controller.play()
        } else {
            controller.play()
        }
    }

    var hoistedMainListener by remember { mutableStateOf<androidx.media3.common.Player.Listener?>(null) }

    DisposableEffect(uriString) {
        val settingsManager = com.example.data.SettingsManager.getInstance(context)
        com.example.LogKeeper.log("Starting player for $decodedUri", "PlayerScreen")
        
        // Ensure player is initialized
        com.example.service.PlayerManager.initialize(context, false)
        mediaController = com.example.service.PlayerManager.exoPlayer
        
        // Start the service for MediaSession features
        val intent = android.content.Intent(context, com.example.service.PlaybackService::class.java)
        try {
            context.startService(intent)
        } catch(e: Exception) {}

        val controller = mediaController ?: return@DisposableEffect onDispose {}
        
        val savedGain = settingsManager.boostGainMb
        boostGainMb = savedGain
        if (savedGain > 0) {
            com.example.service.PlayerManager.applyAudioBoosterSettings(settingsManager.audioBoosterEnabled, savedGain)
        }
        controller.setPlaybackSpeed(playbackSpeed)
        
        val mainListener = object : androidx.media3.common.Player.Listener {
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                // Restore track selection
                val audioTrackIdx = settingsManager.getTrackSelection(decodedUriString, androidx.media3.common.C.TRACK_TYPE_AUDIO)
                val subtitleTrackIdx = settingsManager.getTrackSelection(decodedUriString, androidx.media3.common.C.TRACK_TYPE_TEXT)
                
                var builder = controller.trackSelectionParameters.buildUpon()
                var changed = false
                
                if (audioTrackIdx != -1) {
                    val audioGroups = tracks.groups.filter { it.type == androidx.media3.common.C.TRACK_TYPE_AUDIO }
                    var found = false
                    var totalIdx = 0
                    for (group in audioGroups) {
                        for (i in 0 until group.length) {
                            if (totalIdx == audioTrackIdx) {
                                builder.setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, i))
                                changed = true
                                found = true
                                break
                            }
                            totalIdx++
                        }
                        if (found) break
                    }
                }
                
                if (subtitleTrackIdx != -1) {
                    val subtitleGroups = tracks.groups.filter { it.type == androidx.media3.common.C.TRACK_TYPE_TEXT }
                    var found = false
                    var totalIdx = 0
                    for (group in subtitleGroups) {
                        for (i in 0 until group.length) {
                            if (totalIdx == subtitleTrackIdx) {
                                builder.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                                builder.setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, i))
                                changed = true
                                found = true
                                break
                            }
                            totalIdx++
                        }
                        if (found) break
                    }
                }
                
                if (changed) {
                    controller.trackSelectionParameters = builder.build()
                }
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                    val currentMode = controller.repeatMode
                    val hasNext = controller.hasNextMediaItem()
                    if (currentMode == androidx.media3.common.Player.REPEAT_MODE_OFF && !hasNext) {
                        // Service will stop itself when STATE_ENDED
                        onNavigateBack()
                    }
                }
            }
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (reason == androidx.media3.common.Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                    if (controller.repeatMode == androidx.media3.common.Player.REPEAT_MODE_OFF) {
                        onNavigateBack()
                    }
                }
            }
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
        }
        hoistedMainListener = mainListener
        controller.addListener(mainListener)
        
        val currentVideoSize = controller.videoSize
        if (currentVideoSize.width > 0 && currentVideoSize.height > 0) {
            context.findActivity()?.requestedOrientation = if (currentVideoSize.width > currentVideoSize.height) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }
        }
        

        
        val pipListener = object : androidx.media3.common.Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                PipHelper.updatePipParams(context, controller, videoSize.width, videoSize.height)
            }
            override fun onEvents(player: androidx.media3.common.Player, events: androidx.media3.common.Player.Events) {
                if (events.contains(androidx.media3.common.Player.EVENT_IS_PLAYING_CHANGED) || events.contains(androidx.media3.common.Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                    PipHelper.updatePipParams(context, player, player.videoSize.width, player.videoSize.height)
                }
            }
        }
        controller.addListener(pipListener)
        PipHelper.updatePipParams(context, controller, controller.videoSize.width, controller.videoSize.height)
        
        val pipReceiver = PipActionReceiver(controller)
        val filter = android.content.IntentFilter(PipActionReceiver.ACTION_PIP_CONTROL)
        androidx.core.content.ContextCompat.registerReceiver(
            context,
            pipReceiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_EXPORTED
        )

        
        onDispose {
            controller.removeListener(mainListener)
            controller.removeListener(pipListener)
            try {
                context.unregisterReceiver(pipReceiver)
            } catch(e: Exception) {}
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, mediaController) {
        val currentController = mediaController
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            com.example.LogKeeper.log("LifecycleEventObserver received: $event", "PlayerScreen")
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                currentController?.let { controller ->
                    val currentPos = controller.currentPosition
                    val dur = controller.duration
                    com.example.data.SettingsManager.getInstance(context).savePlaybackState(decodedUriString, currentPos, dur)
                    
                    val activity = context.findActivity()
                    val isPip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) activity?.isInPictureInPictureMode == true else false
                    // We don't call stop() here anymore. The PlaybackService will handle
                    // inactivity timeouts (5 mins) to release resources gracefully.
                    if (!isPip && !backgroundPlayEnabledRef.value) {
                        controller.pause()
                    }
                }
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                currentController?.let { controller ->
                    // No need to prepare() explicitly since we are not stopping the player.
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
                hoistedMainListener?.let { controller.removeListener(it) }
                val currentPos = controller.currentPosition
                val dur = controller.duration
                com.example.data.SettingsManager.getInstance(context).savePlaybackState(decodedUriString, currentPos, dur)
                if (!backgroundPlayEnabledRef.value) {
                    controller.clearMediaItems()
                    controller.stop()
                }
            }
        }
    }

    var isInPipMode by remember { 
        val activity = context.findActivity() as? androidx.activity.ComponentActivity
        val isPipInitially = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) activity?.isInPictureInPictureMode == true else false
        mutableStateOf(isPipInitially) 
    }

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
                        if (controller.playbackState == androidx.media3.common.Player.STATE_ENDED || controller.playbackState == androidx.media3.common.Player.STATE_IDLE) {
                            controller.seekTo(0)
                            controller.prepare()
                            controller.play()
                            showControls = false
                        } else if (controller.isPlaying) {
                            controller.pause()
                            showControls = true
                        } else {
                            controller.play()
                            showControls = false
                        }
                        flashIsPlaying = controller.isPlaying
                        showPlayPauseFlash = true
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
                var lastVirtualVolume = -1
                
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                val startVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                val startBoost = boostGainMb
                
                val window = context.findActivity()?.window
                var startBrightness = window?.attributes?.screenBrightness ?: -1f
                if (startBrightness < 0) startBrightness = 0.5f
                
                val startPosition = mediaController?.currentPosition?.coerceAtLeast(0L) ?: 0L

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
                                    val currentDuration = mediaController?.duration?.coerceAtLeast(1L) ?: 1L
                                    val seekOffsetMs = (dragDistanceX / size.width) * 120_000
                                    val targetPos = (startPosition + seekOffsetMs.toLong()).coerceIn(0L, currentDuration)
                                    mediaController?.seekTo(targetPos)
                                    seekOffsetSec = ((dragDistanceX / size.width) * 120).toInt()
                                    change.consume()
                                }
                                GestureType.VOLUME -> {
                                    val virtualMaxVolume = maxVolume * 2
                                    val virtualStartVolume = startVolume + (startBoost.toFloat() / 1500f * maxVolume).toInt()
                                    val volumeChange = -(dragDistanceY / size.height) * virtualMaxVolume
                                    val virtualNewVolume = (virtualStartVolume + volumeChange).toInt().coerceIn(0, virtualMaxVolume)
                                    
                                    if (virtualNewVolume != lastVirtualVolume) {
                                        lastVirtualVolume = virtualNewVolume
                                        val newVolume = virtualNewVolume.coerceAtMost(maxVolume)
                                        val newBoost = if (virtualNewVolume > maxVolume) {
                                            ((virtualNewVolume - maxVolume).toFloat() / maxVolume * 1500f).toInt()
                                        } else {
                                            0
                                        }
                                        
                                        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVolume, 0)
                                        boostGainMb = newBoost
                                        val settings = com.example.data.SettingsManager.getInstance(context)
                                        settings.boostGainMb = newBoost
                                        com.example.service.PlayerManager.applyAudioBoosterSettings(settings.audioBoosterEnabled, newBoost)
                                    }
                                    gestureVolumeRatio = virtualNewVolume.toFloat() / virtualMaxVolume.toFloat()
                                    gestureText = "Volume: ${(gestureVolumeRatio * 200).roundToInt()}%"
                                    change.consume()
                                }
                                else -> {}
                            }
                        }
                    }
                } while (event.changes.any { it.pressed })
                
                if (currentGesture == GestureType.SEEK) {
                    val currentDuration = mediaController?.duration?.coerceAtLeast(1L) ?: 1L
                    val seekOffsetMs = (dragDistanceX / size.width) * 120_000
                    val targetPos = (startPosition + seekOffsetMs.toLong()).coerceIn(0L, currentDuration)
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
                view.subtitleView?.let { subtitleView ->
                    val colorInt = android.graphics.Color.argb(
                        (subtitleColor.alpha * 255).toInt(),
                        (subtitleColor.red * 255).toInt(),
                        (subtitleColor.green * 255).toInt(),
                        (subtitleColor.blue * 255).toInt()
                    )
                    val style = androidx.media3.ui.CaptionStyleCompat(
                        colorInt,
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                        androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                        android.graphics.Color.BLACK,
                        null
                    )
                    subtitleView.setStyle(style)
                    subtitleView.setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, subtitleSize)
                }
            },
            onRelease = { view ->
                view.player = null
            },
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offsetX
                translationY = offsetY
            }
        )

        androidx.compose.animation.AnimatedVisibility(
            visible = showPlayPauseFlash,
            enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(100)),
            exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = if (flashIsPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(72.dp)
            )
        }

        if (activeGesture != GestureType.NONE && !isInPipMode) {
            if (activeGesture == GestureType.VOLUME) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(start = 24.dp)
                            .height(160.dp)
                            .width(42.dp)
                            .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(21.dp))
                            .padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = "${(gestureVolumeRatio * 200).roundToInt()}%",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .width(4.dp)
                                .background(Color.DarkGray.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight((gestureVolumeRatio * 2f).coerceIn(0f, 1f))
                                    .width(4.dp)
                                    .background(Color(0xFF2196F3), androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                            )
                            val whiteRatio = ((gestureVolumeRatio - 0.5f) * 2f).coerceIn(0f, 1f)
                            if (whiteRatio > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(whiteRatio)
                                        .width(4.dp)
                                        .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Icon(androidx.compose.material.icons.Icons.Filled.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            } else if (activeGesture == GestureType.SEEK) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val sign = if (seekOffsetSec >= 0) "+" else ""
                    Text(
                        text = "$sign${seekOffsetSec}s",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).padding(16.dp)
                    )
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
            enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(200)),
            exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(200)),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 32.dp)
            ) {
                var timeStr by remember { mutableStateOf("") }
                var batteryPct by remember { mutableStateOf(100) }
                var isCharging by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    val intentFilter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
                    val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
                    while (true) {
                        timeStr = sdf.format(java.util.Date()).lowercase(java.util.Locale.US)
                        val batteryStatus = context.registerReceiver(null, intentFilter)
                        val level = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: 100
                        val scale = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: 100
                        val status = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
                        
                        isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING || status == android.os.BatteryManager.BATTERY_STATUS_FULL
                        batteryPct = if (scale > 0) (level * 100) / scale else 100
                        delay(10000) // Update every 10 seconds
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp, top = 0.dp)
                ) {
                    Text(
                        text = timeStr,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    )
                    Icon(
                        imageVector = if (isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryFull,
                        contentDescription = if (isCharging) "Charging" else "Battery",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "$batteryPct%",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    )
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showControls && !isInPipMode,
            enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(200)),
            exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(200)),
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
                        .height(56.dp)
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
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 0.dp)
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
                            Box {
                                var showDetailsDialog by remember { mutableStateOf(false) }
                                
                                var detailsName by remember { mutableStateOf("Unknown") }
                                var detailsSize by remember { mutableStateOf("Unknown") }
                                var detailsDate by remember { mutableStateOf("Unknown") }
                                var detailsPath by remember { mutableStateOf("Unknown") }
                                
                                LaunchedEffect(decodedUri) {
                                    detailsName = decodedUri.lastPathSegment ?: "Unknown"
                                    detailsPath = decodedUri.toString()
                                    if (decodedUri.scheme == "file") {
                                        try {
                                            val file = java.io.File(decodedUri.path!!)
                                            detailsName = file.name
                                            val size = file.length()
                                            detailsSize = if (size > 1024 * 1024) "${size / (1024 * 1024)} MB" else "${size / 1024} KB"
                                            detailsDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(file.lastModified()))
                                            detailsPath = file.absolutePath
                                        } catch (e: Exception) {}
                                    } else if (decodedUri.scheme == "content") {
                                        try {
                                            context.contentResolver.query(decodedUri, null, null, null, null)?.use { cursor ->
                                                if (cursor.moveToFirst()) {
                                                    val nameCol = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
                                                    if (nameCol != -1) cursor.getString(nameCol)?.let { detailsName = it }
                                                    val sizeCol = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.SIZE)
                                                    if (sizeCol != -1) {
                                                        val size = cursor.getLong(sizeCol)
                                                        detailsSize = if (size > 1024 * 1024) "${size / (1024 * 1024)} MB" else "${size / 1024} KB"
                                                    }
                                                    val dateCol = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATE_ADDED)
                                                    if (dateCol != -1) {
                                                        val dateAdded = cursor.getLong(dateCol)
                                                        val dateMs = if (dateAdded < 10000000000L) dateAdded * 1000 else dateAdded
                                                        if (dateMs > 0) {
                                                            detailsDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(dateMs))
                                                        }
                                                    }
                                                    val dataCol = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                                                    if (dataCol != -1) cursor.getString(dataCol)?.let { detailsPath = it }
                                                }
                                            }
                                        } catch (e: Exception) {}
                                    }
                                }

                                IconButton(onClick = { showTopMenu = true }) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "More options", tint = Color.White)
                                }
                                androidx.compose.material3.DropdownMenu(
                                    expanded = showTopMenu,
                                    onDismissRequest = { showTopMenu = false }
                                ) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Details") },
                                        onClick = { 
                                            showTopMenu = false
                                            showDetailsDialog = true 
                                        }
                                    )
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Edit") },
                                        onClick = {
                                            showTopMenu = false
                                            val mimeType = context.contentResolver.getType(decodedUri)
                                            val isAudio = mimeType?.startsWith("audio/") == true
                                            val isVideo = mimeType?.startsWith("video/") == true
                                            val isAnimatedImage = mimeType == "image/gif" || mimeType == "image/webp"
                                            val isImage = mimeType?.startsWith("image/") == true

                                            val route = if (isAudio) "audio_trimmer/$uriString"
                                            else if (isVideo) "video_editor/$uriString"
                                            else if (isImage && !isAnimatedImage) "photo_editor/$uriString"
                                            else "video_editor/$uriString"
                                            
                                            onNavigateToEdit(route)
                                        }
                                    )
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Player settings") },
                                        onClick = {
                                            showTopMenu = false
                                            showPlayerSettingsDialog = true
                                        }
                                    )
                                }
                                
                                if (showDetailsDialog) {
                                    val duration = mediaController?.duration ?: 0L
                                    val durationStr = if (duration > 0) String.format(java.util.Locale.US, "%02d:%02d", java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(duration), java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(duration) % 60) else "Unknown"
                                    
                                    androidx.compose.material3.AlertDialog(
                                        onDismissRequest = { showDetailsDialog = false },
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        title = { Text("Properties", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                                        text = { 
                                            Column {
                                                Text("Name: $detailsName", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Size: $detailsSize", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Duration: $durationStr", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Date Added: $detailsDate", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Path: $detailsPath", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                            }
                                        },
                                        confirmButton = {
                                            androidx.compose.material3.TextButton(onClick = { showDetailsDialog = false }) { Text("OK", color = Color(0xFF2196F3)) }
                                        }
                                    )
                                }
                            }
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            // A-B Repeat Icon
                            IconButton(onClick = {
                                val currentPos = mediaController?.currentPosition ?: 0L
                                if (abRepeatStart == null) {
                                    abRepeatStart = currentPos
                                } else if (abRepeatEnd == null) {
                                    abRepeatEnd = currentPos
                                } else {
                                    abRepeatStart = null
                                    abRepeatEnd = null
                                }
                            }) {
                                val tint = if (abRepeatStart != null) Color(0xFF2196F3) else Color.White
                                val text = if (abRepeatStart != null && abRepeatEnd == null) "A" else if (abRepeatEnd != null) "A-B" else ""
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Loop, contentDescription = "A-B Repeat", tint = tint)
                                    if (text.isNotEmpty()) {
                                        Text(
                                            text = text,
                                            color = tint,
                                            fontSize = 8.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            modifier = Modifier.align(Alignment.BottomEnd)
                                        )
                                    }
                                }
                            }
                            
                            // Sleep Timer Icon
                            IconButton(onClick = { showSleepTimerDialog = true }) {
                                Icon(Icons.Filled.Timer, contentDescription = "Sleep Timer", tint = if (sleepTimerEndTime != null) Color(0xFF2196F3) else Color.White)
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
                                            bitmap.recycle()
                                        } else {
                                            Toast.makeText(context, "Screenshot failed (PixelCopy error)", Toast.LENGTH_SHORT).show()
                                            bitmap.recycle()
                                        }
                                    }, android.os.Handler(android.os.Looper.getMainLooper()))
                                } else {
                                    Toast.makeText(context, "Screenshot failed: Surface not ready or unsupported OS", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Filled.Screenshot, contentDescription = "Screenshot", tint = Color.White)
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = showBrightnessSlider,
                        enter = androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.fadeOut(),
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 48.dp)
                    ) {
                        var brightness by remember { mutableFloatStateOf(context.findActivity()?.window?.attributes?.screenBrightness.takeIf { it != -1f } ?: 0.5f) }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                                .padding(horizontal = 12.dp, vertical = 24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(140.dp)
                                    .width(32.dp)
                                    .pointerInput(Unit) {
                                        var lastAppliedBrightness = -1f
                                        detectVerticalDragGestures(
                                            onVerticalDrag = { change, dragAmount ->
                                                change.consume()
                                                val dragRatio = -dragAmount / 140.dp.toPx()
                                                val newVal = (brightness + dragRatio).coerceIn(0f, 1f)
                                                brightnessInteractionTime = System.currentTimeMillis()
                                                brightness = newVal
                                                
                                                if (kotlin.math.abs(newVal - lastAppliedBrightness) > 0.02f) {
                                                    lastAppliedBrightness = newVal
                                                    val window = context.findActivity()?.window
                                                    window?.let {
                                                        val lp = it.attributes
                                                        lp.screenBrightness = newVal
                                                        it.attributes = lp
                                                    }
                                                }
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
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
                                            .fillMaxHeight(brightness.coerceIn(0f, 1f))
                                            .width(4.dp)
                                            .background(Color(0xFF2196F3), androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Icon(Icons.Filled.LightMode, contentDescription = null, tint = Color.White)
                        }
                    }

                    // Bottom controls background
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                    )

                    // Bottom controls overlay
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.systemBars)
                            .padding(bottom = 4.dp)
                    ) {
                        com.example.ui.screens.PlaybackProgressRow(
                            mediaController = mediaController,
                            abRepeatStart = abRepeatStart,
                            abRepeatEnd = abRepeatEnd,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        )
                        
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            // Left alignment
                            Row(modifier = Modifier.align(Alignment.CenterStart)) {
                                IconButton(onClick = { isLocked = true }) {
                                    Icon(Icons.Filled.LockOpen, contentDescription = "Lock", tint = Color.White)
                                }
                            }
                            
                            // Center alignment
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                            
                                IconButton(
                                    onClick = {
                                        mediaController?.let { controller ->
                                            if (controller.hasPreviousMediaItem()) {
                                                controller.seekToPreviousMediaItem()
                                            } else {
                                                controller.seekTo(0)
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipPrevious,
                                        contentDescription = "Previous",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        mediaController?.let { controller ->
                                            if (controller.playbackState == androidx.media3.common.Player.STATE_ENDED || controller.playbackState == androidx.media3.common.Player.STATE_IDLE) {
                                                controller.seekTo(0)
                                                controller.prepare()
                                                controller.play()
                                            } else if (controller.isPlaying) {
                                                controller.pause()
                                            } else {
                                                controller.play()
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                
                                IconButton(
                                    onClick = {
                                        mediaController?.let { controller ->
                                            if (controller.hasNextMediaItem()) {
                                                controller.seekToNextMediaItem()
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipNext,
                                        contentDescription = "Next",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                            }
                            
                            // Right alignment
                            Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                                IconButton(onClick = {
                                    val nextMode = when (repeatMode) {
                                        androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
                                        androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
                                        else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                                    }
                                    repeatMode = nextMode
                                    mediaController?.repeatMode = nextMode
                                }) {
                                    val repeatIcon = when (repeatMode) {
                                        androidx.media3.common.Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
                                        else -> Icons.Filled.Repeat
                                    }
                                    val repeatTint = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_OFF) Color.White else Color(0xFF2196F3)
                                    Icon(repeatIcon, contentDescription = "Repeat", tint = repeatTint)
                                }
                                IconButton(onClick = { 
                                    backgroundPlayEnabled = !backgroundPlayEnabled
                                    Toast.makeText(context, "Background play " + if (backgroundPlayEnabled) "enabled" else "disabled", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Filled.Headphones, contentDescription = "Background play", tint = if (backgroundPlayEnabled) Color(0xFF2196F3) else Color.White)
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
                                    val appOps = context.getSystemService(android.content.Context.APP_OPS_SERVICE) as android.app.AppOpsManager
                                    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        appOps.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_PICTURE_IN_PICTURE, android.os.Process.myUid(), context.packageName)
                                    } else {
                                        appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_PICTURE_IN_PICTURE, android.os.Process.myUid(), context.packageName)
                                    }
                                    
                                    if (mode != android.app.AppOpsManager.MODE_ALLOWED) {
                                        com.example.LogKeeper.log("PiP permission not granted, redirecting to settings", "PlayerScreen")
                                        val intent = android.content.Intent("android.settings.PICTURE_IN_PICTURE_SETTINGS").apply {
                                            data = android.net.Uri.fromParts("package", context.packageName, null)
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            com.example.LogKeeper.logError("PlayerScreen", "Could not open PiP settings", e)
                                        }
                                    } else {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            try {
                                                val width = mediaController?.videoSize?.width ?: 0
                                                val height = mediaController?.videoSize?.height ?: 0
                                                val params = PipHelper.buildPipParams(context, mediaController, width, height)
                                                val activity = context.findActivity()
                                                if (activity != null) {
                                                    val entered = activity.enterPictureInPictureMode(params)
                                                    com.example.LogKeeper.log("PiP enter result: $entered", "PlayerScreen")
                                                    if (!entered) {
                                                        com.example.LogKeeper.logError("PlayerScreen", "enterPictureInPictureMode returned false", null)
                                                    }
                                                } else {
                                                    com.example.LogKeeper.logError("PlayerScreen", "Activity is null for PiP", null)
                                                }
                                            } catch (e: Exception) {
                                                com.example.LogKeeper.logError("PlayerScreen", "Exception entering PiP", e)
                                            }
                                        } else {
                                            com.example.LogKeeper.logError("PlayerScreen", "PiP not supported on this SDK", null)
                                        }
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

    if (showAudioDialog) {
        CompactPlayerDialog(
            onDismissRequest = { showAudioDialog = false }
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                    Text("Select Audio Track", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val settings = remember { com.example.data.SettingsManager.getInstance(context) }
                    if (settings.audioBoosterEnabled) {
                        Text(
                            text = "Volume Booster: " + if (boostGainMb == 0) "Off" else "+${boostGainMb / 100}dB",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = boostGainMb.toFloat(),
                            onValueChange = { newVal ->
                                val newGain = newVal.toInt()
                                boostGainMb = newGain
                                settings.boostGainMb = newGain
                                com.example.service.PlayerManager.applyAudioBoosterSettings(settings.audioBoosterEnabled, newGain)
                            },
                            valueRange = 0f..1500f,
                            steps = 14
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        Text(
                            text = "Volume Booster is disabled in Player Settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    val currentTracks = mediaController?.currentTracks
                    val audioGroups = currentTracks?.groups?.filter { it.type == androidx.media3.common.C.TRACK_TYPE_AUDIO } ?: emptyList()
                    
                    if (audioGroups.isEmpty()) {
                        Text("No audio tracks available", style = MaterialTheme.typography.bodyLarge)
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp)
                        ) {
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
                                            var totalIdx = 0
                                            for (g in 0 until groupIndex) {
                                                totalIdx += audioGroups[g].length
                                            }
                                            totalIdx += trackIndex
                                            settingsManager.saveTrackSelection(decodedUriString, androidx.media3.common.C.TRACK_TYPE_AUDIO, totalIdx)
                                            showAudioDialog = false
                                        }.padding(vertical = 12.dp)
                                    ) {
                                        Text(title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
        }
    }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            audioPickerLauncher.launch("audio/*")
                            showAudioDialog = false
                        }.padding(vertical = 12.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add More", tint = Color(0xFF2196F3))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add More...", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        androidx.compose.material3.TextButton(onClick = { showAudioDialog = false }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showSubtitleDialog) {
        CompactPlayerDialog(
            onDismissRequest = { showSubtitleDialog = false }
        ) {
            var selectedTab by remember { mutableStateOf(0) }
            Column(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                androidx.compose.material3.TabRow(selectedTabIndex = selectedTab) {
                    androidx.compose.material3.Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Tracks") })
                    androidx.compose.material3.Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Settings") })
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                if (selectedTab == 0) {
                    val currentTracks = mediaController?.currentTracks
                    val textGroups = currentTracks?.groups?.filter { it.type == androidx.media3.common.C.TRACK_TYPE_TEXT } ?: emptyList()
                    val trackSelectionParameters = mediaController?.trackSelectionParameters
                    val isTextDisabled = trackSelectionParameters?.disabledTrackTypes?.contains(androidx.media3.common.C.TRACK_TYPE_TEXT) ?: false

                    if (textGroups.isEmpty()) {
                        Text("No subtitles available", style = MaterialTheme.typography.bodyLarge)
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        val builder = mediaController?.trackSelectionParameters?.buildUpon()
                                        builder?.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true)
                                        builder?.clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_TEXT)
                                        builder?.let { mediaController?.trackSelectionParameters = it.build() }
                                        settingsManager.saveTrackSelection(decodedUriString, androidx.media3.common.C.TRACK_TYPE_TEXT, -1)
                                        showSubtitleDialog = false
                                    }.padding(vertical = 12.dp)
                                ) {
                                    Text("Off", fontWeight = if (isTextDisabled) FontWeight.Bold else FontWeight.Normal, color = if (isTextDisabled) Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurfaceVariant)
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
                                            var totalIdx = 0
                                            for (g in 0 until groupIndex) {
                                                totalIdx += textGroups[g].length
                                            }
                                            totalIdx += trackIndex
                                            settingsManager.saveTrackSelection(decodedUriString, androidx.media3.common.C.TRACK_TYPE_TEXT, totalIdx)
                                            showSubtitleDialog = false
                                        }.padding(vertical = 12.dp)
                                    ) {
                                        Text(title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            subtitlePickerLauncher.launch("application/octet-stream")
                            showSubtitleDialog = false
                        }.padding(vertical = 12.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add More", tint = Color(0xFF2196F3))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Subtitle File...", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        Text("Subtitle Delay (ms): ${subtitleDelay.toInt()}", fontWeight = FontWeight.Bold)
                        androidx.compose.material3.Slider(
                            value = subtitleDelay,
                            onValueChange = { subtitleDelay = it },
                            valueRange = -5000f..5000f,
                            steps = 100
                        )
                        Text("Delay is not natively supported by this ExoPlayer version without reloading.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("Font Size: ${subtitleSize.toInt()}sp", fontWeight = FontWeight.Bold)
                        androidx.compose.material3.Slider(
                            value = subtitleSize,
                            onValueChange = { subtitleSize = it },
                            valueRange = 8f..48f
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("Subtitle Color", fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            val colors = listOf(Color.White, Color.Yellow, Color.Cyan, Color.Green, Color.Magenta)
                            colors.forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(2.dp, if (subtitleColor == c) Color(0xFF2196F3) else Color.Transparent, CircleShape)
                                        .clickable { subtitleColor = c }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    if (showSpeedDialog) {
        CompactPlayerDialog(
            onDismissRequest = { showSpeedDialog = false }
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                    Text("Select playback speed", style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
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
                                settingsManager.savePlaybackSpeed(decodedUriString, playbackSpeed)
                            },
                            modifier = Modifier.background(Color(0xFF2196F3).copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                        }
                        
                        Text(String.format("%.1f", playbackSpeed), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        
                        IconButton(
                            onClick = { 
                                val newSpeed = minOf(3.0f, playbackSpeed + 0.1f)
                                playbackSpeed = Math.round(newSpeed * 10.0f) / 10.0f
                                mediaController?.setPlaybackSpeed(playbackSpeed)
                                settingsManager.savePlaybackSpeed(decodedUriString, playbackSpeed)
                            },
                            modifier = Modifier.background(Color(0xFF2196F3).copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Increase")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SpeedSliderRow(
                            playbackSpeed = playbackSpeed,
                            onSpeedChange = { newSpeed ->
                                playbackSpeed = newSpeed
                                mediaController?.setPlaybackSpeed(newSpeed)
                                settingsManager.savePlaybackSpeed(decodedUriString, newSpeed)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { 
                            playbackSpeed = 1.0f
                            mediaController?.setPlaybackSpeed(playbackSpeed)
                            settingsManager.savePlaybackSpeed(decodedUriString, playbackSpeed)
                        }) {
                            Icon(Icons.Filled.Restore, contentDescription = "Reset")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
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
                                        settingsManager.savePlaybackSpeed(decodedUriString, playbackSpeed)
                                    }
                                    .border(1.dp, if (playbackSpeed == speed) Color(0xFF2196F3) else MaterialTheme.colorScheme.outline, CircleShape)
                                    .background(if (playbackSpeed == speed) Color(0xFF2196F3).copy(alpha = 0.2f) else Color.Transparent)
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("${speed}x", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
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
                                mediaController?.skipSilenceEnabled = skipSilence
                            }
                        )
                    }
        }
    }
    }

    if (showPlayerSettingsDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showPlayerSettingsDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            com.example.ui.screens.PlayerSettingsScreen(onNavigateBack = { showPlayerSettingsDialog = false })
        }
    }

    if (showSleepTimerDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Sleep Timer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val options = listOf(
                        "Off" to null,
                        "15 minutes" to 15 * 60 * 1000L,
                        "30 minutes" to 30 * 60 * 1000L,
                        "60 minutes" to 60 * 60 * 1000L,
                        "End of video" to (mediaController?.duration?.let { dur -> if (dur > 0) (dur - (mediaController?.currentPosition ?: 0)) else null })
                    )
                    options.forEach { (label, duration) ->
                        androidx.compose.material3.TextButton(
                            onClick = {
                                if (duration == null) {
                                    sleepTimerEndTime = null
                                } else {
                                    sleepTimerEndTime = System.currentTimeMillis() + duration
                                }
                                showSleepTimerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showSleepTimerDialog = false }) { Text("Close", color = Color(0xFF2196F3)) }
            }
        )
    }
}

@kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SpeedSliderRow(
    playbackSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Slider(
        value = playbackSpeed,
        onValueChange = { 
            onSpeedChange(Math.round(it * 10.0f) / 10.0f)
        },
        valueRange = 0.1f..3.0f,
        thumb = {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color(0xFF2196F3), CircleShape)
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                colors = SliderDefaults.colors(
                    activeTrackColor = Color(0xFF2196F3),
                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                ),
                drawStopIndicator = null,
                thumbTrackGapSize = 0.dp,
                trackInsideCornerSize = 0.dp,
                modifier = Modifier.height(4.dp)
            )
        },
        modifier = modifier
    )
}


