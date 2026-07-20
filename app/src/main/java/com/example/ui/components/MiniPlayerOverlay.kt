package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.delay

@Composable
fun MiniPlayerOverlay(
    player: Player?,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onResize: (Float, Float) -> Unit
) {
    var isPlaying by remember { mutableStateOf(player?.isPlaying == true) }
    var currentPosition by remember { mutableLongStateOf(player?.currentPosition ?: 0L) }
    var duration by remember { mutableLongStateOf(player?.duration?.coerceAtLeast(0L) ?: 0L) }
    var title by remember { mutableStateOf(player?.currentMediaItem?.mediaMetadata?.title?.toString() ?: "Unknown") }
    var playlist by remember { mutableStateOf(emptyList<MediaItem>()) }
    var isReversed by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableIntStateOf(player?.currentMediaItemIndex ?: -1) }
    var isExpanded by remember { mutableStateOf(false) }
    var loopMode by remember { mutableIntStateOf(player?.repeatMode ?: Player.REPEAT_MODE_OFF) }
    var shuffleMode by remember { mutableStateOf(player?.shuffleModeEnabled == true) }

    LaunchedEffect(player) {
        if (player == null) return@LaunchedEffect
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingChange: Boolean) {
                isPlaying = isPlayingChange
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                title = mediaItem?.mediaMetadata?.title?.toString() ?: "Unknown"
                currentIndex = player.currentMediaItemIndex
            }
            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                val newPlaylist = mutableListOf<MediaItem>()
                for (i in 0 until timeline.windowCount) {
                    newPlaylist.add(player.getMediaItemAt(i))
                }
                playlist = newPlaylist
                currentIndex = player.currentMediaItemIndex
            }
            override fun onRepeatModeChanged(repeatMode: Int) {
                loopMode = repeatMode
            }
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                shuffleMode = shuffleModeEnabled
            }
        }
        player.addListener(listener)
        // initial state
        val newPlaylist = mutableListOf<MediaItem>()
        for (i in 0 until player.mediaItemCount) {
            newPlaylist.add(player.getMediaItemAt(i))
        }
        playlist = newPlaylist
        currentIndex = player.currentMediaItemIndex
        loopMode = player.repeatMode
        shuffleMode = player.shuffleModeEnabled

        while (true) {
            currentPosition = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.coerceAtLeast(0L)
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.9f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Player Video / Visualizer space
            Box(
                modifier = Modifier
                    .weight(if (isExpanded) 1f else 3f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x, dragAmount.y)
                        }
                    }
                    .background(Color.Black)
            ) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { context ->
                        androidx.media3.ui.PlayerView(context).apply {
                            this.player = player
                            useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Progress bar
                val progress = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.DarkGray
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { 
                        player?.let {
                            it.shuffleModeEnabled = !it.shuffleModeEnabled
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (shuffleMode) Color(0xFF2196F3) else Color.White
                        )
                    }
                    IconButton(onClick = { isReversed = !isReversed }) {
                        Icon(
                            imageVector = Icons.Filled.Sort,
                            contentDescription = "Sort",
                            tint = if (isReversed) Color(0xFF2196F3) else Color.White
                        )
                    }
                    IconButton(onClick = { 
                        player?.let {
                            it.repeatMode = when (it.repeatMode) {
                                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                else -> Player.REPEAT_MODE_OFF
                            }
                        }
                    }) {
                        Icon(
                            imageVector = if (loopMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                            contentDescription = "Loop",
                            tint = if (loopMode != Player.REPEAT_MODE_OFF) Color(0xFF2196F3) else Color.White
                        )
                    }
                    IconButton(onClick = { player?.seekToPreviousMediaItem() }) {
                        Icon(Icons.Filled.SkipPrevious, "Previous", tint = Color.White)
                    }
                    IconButton(onClick = {
                        if (isPlaying) player?.pause() else player?.play()
                    }) {
                        Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play/Pause", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    IconButton(onClick = { player?.seekToNextMediaItem() }) {
                        Icon(Icons.Filled.SkipNext, "Next", tint = Color.White)
                    }
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, "Toggle Playlist", tint = Color.White)
                    }
                }
            }
            
            if (isExpanded) {
                // The playlist view
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                    Text("Now Playing", style = MaterialTheme.typography.titleSmall, color = Color.White)
                }
                val displayList = if (isReversed) {
                    playlist.mapIndexed { index, item -> Pair(index, item) }.reversed()
                } else {
                    playlist.mapIndexed { index, item -> Pair(index, item) }
                }
                
                LazyColumn(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxWidth()
                ) {
                    items(displayList) { pair ->
                        val originalIndex = pair.first
                        val item = pair.second
                        val isSelected = originalIndex == currentIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { player?.seekToDefaultPosition(originalIndex) }
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.mediaMetadata.title?.toString() ?: item.mediaId,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        
        // Floating Close, Minimize, and Resize buttons at bottom right
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(16.dp)),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMinimize, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Remove, "Minimize", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Close, "Close completely", modifier = Modifier.size(20.dp))
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
