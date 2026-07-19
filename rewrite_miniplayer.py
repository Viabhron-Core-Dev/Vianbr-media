import re

with open("app/src/main/java/com/example/ui/components/MiniPlayerOverlay.kt", "r") as f:
    bottom_half = f.read()

# Reconstruct the top part:
top_half = """package com.example.ui.components

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
    var loopMode by remember { mutableIntStateOf(player?.repeatMode ?: Player.REPEAT_MODE_OFF) }
    var shuffleMode by remember { mutableStateOf(player?.shuffleModeEnabled ?: false) }

    LaunchedEffect(player) {
        if (player == null) return@LaunchedEffect

        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingChange: Boolean) {
                isPlaying = isPlayingChange
            }
            override fun onRepeatModeChanged(repeatMode: Int) {
                loopMode = repeatMode
            }
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                shuffleMode = shuffleModeEnabled
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
        loopMode = player.repeatMode
        shuffleMode = player.shuffleModeEnabled

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
"""

# Splice bottom half!
# I can find the place where `bottom_half` starts. From the output, it starts around:
# IconButton(
#     onClick = {
#         val intent = android.content.Intent("com.example.ACTION_ENTER_PIP")

bottom_index = bottom_half.find('IconButton(\n                        onClick = {\n                            val intent = android.content.Intent("com.example.ACTION_ENTER_PIP")')

if bottom_index == -1:
    print("Could not find bottom half marker!")
    exit(1)

with open("app/src/main/java/com/example/ui/components/MiniPlayerOverlay.kt", "w") as f:
    f.write(top_half + "                        " + bottom_half[bottom_index:])

