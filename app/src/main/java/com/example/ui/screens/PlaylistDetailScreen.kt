package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.PlaylistItem
import com.example.data.PlaylistRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val repository = remember {
        val dao = AppDatabase.getDatabase(context).playlistDao()
        PlaylistRepository(dao)
    }

    val playlist by repository.getPlaylistById(playlistId).collectAsStateWithLifecycle(initialValue = null)
    val playlistItems by repository.getItemsForPlaylist(playlistId).collectAsStateWithLifecycle(initialValue = emptyList())

    val selectedItems = remember { mutableStateListOf<PlaylistItem>() }
    val isMultiSelectMode = selectedItems.isNotEmpty()

    BackHandler(enabled = isMultiSelectMode) {
        selectedItems.clear()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (isMultiSelectMode) {
                        Text("${selectedItems.size} Selected")
                    } else {
                        Text(playlist?.name ?: "Playlist")
                    }
                },
                navigationIcon = {
                    if (isMultiSelectMode) {
                        IconButton(onClick = { selectedItems.clear() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear Selection")
                        }
                    } else {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isMultiSelectMode && playlistItems.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .combinedClickable(
                            onClick = {
                                val first = playlistItems.first()
                                onNavigateToPlayer(first.mediaUri)
                            },
                            onLongClick = {
                                android.widget.Toast.makeText(context, "Playing all in background...", android.widget.Toast.LENGTH_SHORT).show()
                                val sessionToken = androidx.media3.session.SessionToken(context, android.content.ComponentName(context, com.example.service.PlaybackService::class.java))
                                val controllerFuture = androidx.media3.session.MediaController.Builder(context, sessionToken).buildAsync()
                                controllerFuture.addListener({
                                    val controller = controllerFuture.get()
                                    val items = playlistItems.map { androidx.media3.common.MediaItem.Builder().setMediaId(it.mediaUri).build() }
                                    controller.setMediaItems(items)
                                    controller.prepare()
                                    controller.play()
                                }, androidx.core.content.ContextCompat.getMainExecutor(context))
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play First / Long Press Play All",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        bottomBar = {
            if (isMultiSelectMode) {
                BottomAppBar {
                    Spacer(modifier = Modifier.weight(1f))
                    if (selectedItems.size == 1) {
                        val selected = selectedItems.first()
                        val index = playlistItems.indexOf(selected)
                        if (index > 0) {
                            IconButton(onClick = {
                                val prev = playlistItems[index - 1]
                                val tempTimestamp = selected.timestamp
                                coroutineScope.launch {
                                    repository.updatePlaylistItem(selected.copy(timestamp = prev.timestamp))
                                    repository.updatePlaylistItem(prev.copy(timestamp = tempTimestamp))
                                    selectedItems.clear()
                                }
                            }) {
                                Icon(Icons.Filled.ArrowUpward, contentDescription = "Move Up")
                            }
                        }
                        if (index < playlistItems.size - 1) {
                            IconButton(onClick = {
                                val next = playlistItems[index + 1]
                                val tempTimestamp = selected.timestamp
                                coroutineScope.launch {
                                    repository.updatePlaylistItem(selected.copy(timestamp = next.timestamp))
                                    repository.updatePlaylistItem(next.copy(timestamp = tempTimestamp))
                                    selectedItems.clear()
                                }
                            }) {
                                Icon(Icons.Filled.ArrowDownward, contentDescription = "Move Down")
                            }
                        }
                    }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            selectedItems.forEach { repository.deletePlaylistItemById(it.id) }
                            selectedItems.clear()
                        }
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    ) { innerPadding ->
        if (playlistItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No items in this playlist", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(playlistItems, key = { it.id }) { item ->
                    val isSelected = selectedItems.contains(item)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .combinedClickable(
                                onClick = {
                                    if (isMultiSelectMode) {
                                        if (isSelected) selectedItems.remove(item) else selectedItems.add(item)
                                    } else {
                                        onNavigateToPlayer(item.mediaUri)
                                    }
                                },
                                onLongClick = {
                                    if (!isMultiSelectMode) {
                                        selectedItems.add(item)
                                    }
                                }
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val name = remember(item.mediaUri) { getDisplayNameFromUri(context, Uri.parse(item.mediaUri)) }
                            
                            Text(
                                name, 
                                style = MaterialTheme.typography.bodyMedium, 
                                modifier = Modifier.weight(1f), 
                                maxLines = 2,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
