package com.example.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import com.example.service.PlaybackService

import android.content.pm.ActivityInfo
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.VideoSize

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    uriString: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    var mediaController by remember { mutableStateOf<MediaController?>(null) }

    DisposableEffect(Unit) {
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            val window = activity?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
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
                        activity?.requestedOrientation = if (videoSize.width > videoSize.height) {
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                        }
                    }
                }
            })
            controller.setMediaItem(MediaItem.Builder().setMediaId(decodedUri.toString()).build())
            controller.prepare()
            
            // Resume from last position if it exists and is not finished
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
                    val currentPosition = controller.currentPosition
                    val duration = controller.duration
                    com.example.data.SettingsManager.getInstance(context).savePlaybackState(decodedUriString, currentPosition, duration)
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
                val currentPosition = controller.currentPosition
                val duration = controller.duration
                com.example.data.SettingsManager.getInstance(context).savePlaybackState(decodedUriString, currentPosition, duration)
                controller.stop()
                controller.release()
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = true
                setShowNextButton(false)
                setShowPreviousButton(false)
            }
        },
        update = { view ->
            view.player = mediaController
        },
        modifier = Modifier.fillMaxSize()
    )
}

