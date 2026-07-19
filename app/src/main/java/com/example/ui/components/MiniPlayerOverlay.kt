package com.example.ui.components

import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayerOverlay(
    player: Player?,
    onClose: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onResize: (Float, Float) -> Unit
) {
    var isPlaying by remember { mutableStateOf(player?.isPlaying == true) }
    var currentPosition by remember { mutableLongStateOf(player?.currentPosition ?: 0L) }
    var duration by remember { mutableLongStateOf(player?.duration?.coerceAtLeast(1L) ?: 1L) }
    var title by remember { mutableStateOf(player?.currentMediaItem?.mediaMetadata?.title?.toString() ?: "No Media") }
    var playlist by remember { mutableStateOf(emptyList<MediaItem>()) }
    var currentIndex by remember { mutableIntStateOf(player?.currentMediaItemIndex ?: 0) }

    LaunchedEffect(player) {
        if (player == null) return@LaunchedEffect
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingChange: Boolean) {
                isPlaying = isPlayingChange
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                title = mediaItem?.mediaMetadata?.title?.toString() ?: "No Media"
                currentIndex = player.currentMediaItemIndex
                duration = player.duration.coerceAtLeast(1L)
            }
            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                val items = mutableListOf<MediaItem>()
                for (i in 0 until timeline.windowCount) {
                    val window = androidx.media3.common.Timeline.Window()
                    timeline.getWindow(i, window)
                    items.add(window.mediaItem)
                }
                playlist = items
                currentIndex = player.currentMediaItemIndex
                duration = player.duration.coerceAtLeast(1L)
            }
        }
        player.addListener(listener)
        // Initial setup
        title = player.currentMediaItem?.mediaMetadata?.title?.toString() ?: "No Media"
        val items = mutableListOf<MediaItem>()
        for (i in 0 until player.currentTimeline.windowCount) {
            val window = androidx.media3.common.Timeline.Window()
            player.currentTimeline.getWindow(i, window)
            items.add(window.mediaItem)
        }
        playlist = items
        currentIndex = player.currentMediaItemIndex
        duration = player.duration.coerceAtLeast(1L)

        while (true) {
            currentPosition = player.currentPosition.coerceAtLeast(0L)
            delay(1000)
        }
    }

    com.example.ui.theme.MyApplicationTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 8.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                // Topbar (draggable)
                val context = LocalContext.current
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
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val intent = android.content.Intent("com.example.ACTION_ENTER_PIP")
                            intent.setPackage(context.packageName)
                            context.sendBroadcast(intent)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.PictureInPicture, "PiP", modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = {
                            val intent = android.content.Intent(context, com.example.MainActivity::class.java).apply {
                                action = "com.example.ACTION_OPEN_PLAYER"
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                            context.startActivity(intent)
                            onClose()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.OpenInNew, "Main Player", modifier = Modifier.size(20.dp))
                    }

                }

                // Playback Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { player?.seekToPreviousMediaItem() }) {
                        Icon(Icons.Filled.SkipPrevious, "Previous")
                    }
                    IconButton(onClick = { player?.seekBack() }) {
                        Icon(Icons.Filled.FastRewind, "Rewind")
                    }
                    IconButton(onClick = {
                        if (isPlaying) player?.pause() else player?.play()
                    }) {
                        Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play/Pause")
                    }
                    IconButton(onClick = { player?.stop() }) {
                        Icon(Icons.Filled.Stop, "Stop")
                    }
                    IconButton(onClick = { player?.seekForward() }) {
                        Icon(Icons.Filled.FastForward, "Forward")
                    }
                    IconButton(onClick = { player?.seekToNextMediaItem() }) {
                        Icon(Icons.Filled.SkipNext, "Next")
                    }
                }

                // Seek Line
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
                    modifier = Modifier.padding(horizontal = 16.dp).height(24.dp)
                )

                // Separator Bar: Loop, Shuffle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var loopMode by remember { mutableIntStateOf(player?.repeatMode ?: Player.REPEAT_MODE_OFF) }
                    IconButton(onClick = { 
                        loopMode = when (loopMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }
                        player?.repeatMode = loopMode
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (loopMode == Player.REPEAT_MODE_ONE) ImageVector.vectorResource(id = com.example.R.drawable.ic_loop_one_active)
                            else if (loopMode == Player.REPEAT_MODE_ALL) ImageVector.vectorResource(id = com.example.R.drawable.ic_loop_all_active)
                            else ImageVector.vectorResource(id = com.example.R.drawable.ic_loop_all_inactive),
                            "Loop",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    var shuffleMode by remember { mutableStateOf(player?.shuffleModeEnabled ?: false) }
                    IconButton(onClick = { 
                        shuffleMode = !shuffleMode
                        player?.shuffleModeEnabled = shuffleMode
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Filled.Shuffle, 
                            "Shuffle",
                            tint = if (shuffleMode) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                androidx.compose.material3.HorizontalDivider()

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
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.mediaMetadata.title?.toString() ?: item.mediaId,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
                
            // Floating Close and Resize buttons at bottom right
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(16.dp)),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, "Close", modifier = Modifier.size(20.dp))
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onResize(dragAmount.x, dragAmount.y)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.ZoomOutMap, "Resize", modifier = Modifier.size(20.dp))
                }
            }
            
            }
        }
    }
}

