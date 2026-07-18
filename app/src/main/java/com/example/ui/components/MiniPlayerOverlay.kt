package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.example.service.PlayerManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun MiniPlayerOverlay(
    onClose: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onResize: (Float, Float) -> Unit
) {
    var title by remember { mutableStateOf("No Media") }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(100L) }
    var playlist by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(-1) }
    
    val player = PlayerManager.exoPlayer

    LaunchedEffect(player) {
        if (player == null) return@LaunchedEffect
        
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                title = mediaItem?.mediaMetadata?.title?.toString() 
                    ?: mediaItem?.mediaId 
                    ?: "Unknown"
                currentIndex = player.currentMediaItemIndex
                val items = mutableListOf<MediaItem>()
                for (i in 0 until player.mediaItemCount) {
                    items.add(player.getMediaItemAt(i))
                }
                playlist = items
            }

            override fun onIsPlayingChanged(isPlayingState: Boolean) {
                isPlaying = isPlayingState
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                duration = player.duration.coerceAtLeast(100L)
            }
            
            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                val items = mutableListOf<MediaItem>()
                for (i in 0 until player.mediaItemCount) {
                    items.add(player.getMediaItemAt(i))
                }
                playlist = items
                currentIndex = player.currentMediaItemIndex
            }
        }
        player.addListener(listener)
        
        // Initial state
        title = player.currentMediaItem?.mediaMetadata?.title?.toString() 
            ?: player.currentMediaItem?.mediaId 
            ?: "No Media"
        isPlaying = player.isPlaying
        duration = player.duration.coerceAtLeast(100L)
        currentIndex = player.currentMediaItemIndex
        val items = mutableListOf<MediaItem>()
        for (i in 0 until player.mediaItemCount) {
            items.add(player.getMediaItemAt(i))
        }
        playlist = items

        while (isActive) {
            currentPosition = player.currentPosition
            duration = player.duration.coerceAtLeast(100L)
            delay(1000)
        }
    }

    // MaterialTheme isn't strictly available in a standalone compose view unless we wrap it.
    // Let's use a basic MaterialTheme block.
    com.example.ui.theme.MyApplicationTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Topbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.x, dragAmount.y)
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { player?.seekToPreviousMediaItem() }) {
                        Icon(Icons.Filled.SkipPrevious, "Previous")
                    }
                    IconButton(onClick = { player?.seekBack() }) {
                        Icon(Icons.Filled.FastRewind, "Rewind 5s")
                    }
                    IconButton(onClick = {
                        if (isPlaying) player?.pause() else player?.play()
                    }) {
                        Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play/Pause")
                    }
                    IconButton(onClick = { player?.seekForward() }) {
                        Icon(Icons.Filled.FastForward, "Forward 5s")
                    }
                    IconButton(onClick = { player?.seekToNextMediaItem() }) {
                        Icon(Icons.Filled.SkipNext, "Next")
                    }
                }

                // Progress Bar
                var sliderPosition by remember(currentPosition) { mutableFloatStateOf(currentPosition.toFloat()) }
                var isDragging by remember { mutableStateOf(false) }

                Slider(
                    value = if (isDragging) sliderPosition else currentPosition.toFloat(),
                    onValueChange = { 
                        sliderPosition = it
                        isDragging = true
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        player?.seekTo(sliderPosition.toLong())
                    },
                    valueRange = 0f..duration.toFloat(),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Playlist Header
                val context = androidx.compose.ui.platform.LocalContext.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Playlist",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { 
                        player?.shuffleModeEnabled = !(player?.shuffleModeEnabled ?: false)
                    }) {
                        Icon(
                            Icons.Filled.Shuffle, 
                            "Scramble",
                            tint = if (player?.shuffleModeEnabled == true) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = {
                        val intent = android.content.Intent("com.example.ACTION_ENTER_PIP")
                        intent.setPackage(context.packageName)
                        context.sendBroadcast(intent)
                    }) {
                        Icon(
                            Icons.Filled.PictureInPicture,
                            "PiP"
                        )
                    }
                    IconButton(onClick = {
                        val intent = android.content.Intent(context, com.example.MainActivity::class.java).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        context.startActivity(intent)
                        onClose()
                    }) {
                        Icon(
                            Icons.Filled.OpenInNew,
                            "Main Player"
                        )
                    }
                }

                // Playlist
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    itemsIndexed(playlist) { index, item ->
                        val isSelected = index == currentIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { player?.seekToDefaultPosition(index) }
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.mediaMetadata.title?.toString() ?: item.mediaId,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Bottom row with Resize and Close
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, "Close")
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    onResize(dragAmount.x, dragAmount.y)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.ZoomOutMap, "Resize")
                    }
                }
            }
        }
    }
}
