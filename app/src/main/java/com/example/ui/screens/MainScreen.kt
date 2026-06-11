package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Folder
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.decode.VideoFrameDecoder
import com.example.data.MediaFolder
import com.example.data.MediaItem
import com.example.data.PlaybackTag

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToPlayer: (String) -> Unit = {}
) {
    val viewModel: MediaViewModel = viewModel()
    val mediaFolders by viewModel.mediaFolders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedFolder by remember { mutableStateOf<MediaFolder?>(null) }
    val selectedMediaItems = remember { mutableStateListOf<MediaItem>() }
    val isMultiSelectMode = selectedMediaItems.isNotEmpty()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (isMultiSelectMode) {
                        Text("${selectedMediaItems.size}/${selectedFolder?.mediaItems?.size ?: 0} Selected")
                    } else {
                        Text(selectedFolder?.name ?: "Vianbr Play")
                    }
                },
                navigationIcon = {
                    if (isMultiSelectMode) {
                        IconButton(onClick = { selectedMediaItems.clear() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear Selection")
                        }
                    } else if (selectedFolder != null) {
                        IconButton(onClick = { selectedFolder = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (isMultiSelectMode) {
                        IconButton(onClick = { Toast.makeText(context, "Play selected", Toast.LENGTH_SHORT).show() }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                        }
                        IconButton(onClick = { Toast.makeText(context, "Add to Playlist", Toast.LENGTH_SHORT).show() }) {
                            Icon(Icons.Filled.PlaylistAdd, contentDescription = "Add to Playlist")
                        }
                    } else {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (isMultiSelectMode) {
                BottomAppBar {
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { Toast.makeText(context, "Rename selected", Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Rename")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { Toast.makeText(context, "Mark as...", Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.Filled.Label, contentDescription = "Mark as...")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { Toast.makeText(context, "Properties", Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.Filled.Info, contentDescription = "Properties")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { Toast.makeText(context, "Delete selected", Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && mediaFolders.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                PullToRefreshBox(
                    isRefreshing = isLoading,
                    onRefresh = { viewModel.loadMedia() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (mediaFolders.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            Text(
                                text = "Media library empty. Pull down to refresh.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    } else if (selectedFolder == null) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            item {
                                Text(
                                    text = "Found ${mediaFolders.size} Folders",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 16.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(mediaFolders) { folder ->
                                FolderCard(folder) {
                                    selectedFolder = folder
                                }
                            }
                        }
                    } else {
                        val folder = selectedFolder!!
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            item {
                                Text(
                                    text = "Found ${folder.mediaItems.size} Media Files",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 16.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(folder.mediaItems) { media ->
                                val isSelected = selectedMediaItems.contains(media)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .combinedClickable(
                                            onClick = {
                                                if (isMultiSelectMode) {
                                                    if (isSelected) selectedMediaItems.remove(media) else selectedMediaItems.add(media)
                                                } else {
                                                    onNavigateToPlayer(media.uri.toString())
                                                }
                                            },
                                            onLongClick = {
                                                if (!isMultiSelectMode) {
                                                    selectedMediaItems.add(media)
                                                }
                                            }
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                    )
                                ) {
                                    Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                                        Box(
                                            modifier = Modifier
                                                .width(140.dp)
                                                .height(80.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(media.uri)
                                                    .decoderFactory(VideoFrameDecoder.Factory())
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = "Thumbnail",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            if (media.tag == PlaybackTag.NEW) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopStart)
                                                        .padding(4.dp)
                                                        .background(Color.Red, RoundedCornerShape(2.dp))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text("NEW", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 8.sp)
                                                }
                                            }
                                            val durationSeconds = media.duration / 1000
                                            val hours = durationSeconds / 3600
                                            val minutes = (durationSeconds % 3600) / 60
                                            val seconds = durationSeconds % 60
                                            val durationStr = if (hours > 0) {
                                                String.format("%d:%02d:%02d", hours, minutes, seconds)
                                            } else {
                                                String.format("%02d:%02d", minutes, seconds)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(4.dp)
                                                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(durationStr, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = media.name, 
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else if (media.tag == PlaybackTag.SEEN) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val pathStr = folder.path.replace("primary:", "/storage/emulated/0/") + "/" + folder.name
                                            Text(
                                                text = pathStr,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                val sizeMb = media.size.toFloat() / (1024f * 1024f)
                                                val sizeStr = String.format("%.2f MB", sizeMb)
                                                Text(
                                                    text = sizeStr,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FolderCard(folder: MediaFolder, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                // Duration chip at bottom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    val durationSeconds = folder.totalDuration / 1000
                    val hours = durationSeconds / 3600
                    val minutes = (durationSeconds % 3600) / 60
                    val seconds = durationSeconds % 60
                    val durationStr = if (hours > 0) {
                        String.format("%d:%02d:%02d", hours, minutes, seconds)
                    } else {
                        String.format("%02d:%02d", minutes, seconds)
                    }
                    Text(
                        text = durationStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name, 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                val displayPath = folder.path.replace("primary:", "/storage/emulated/0/")
                Text(
                    text = displayPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${folder.videoCount} Videos",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        val sizeMb = folder.totalSize / (1024 * 1024)
                        val sizeStr = if (sizeMb > 1024) String.format("%.2f GB", sizeMb / 1024f) else "$sizeMb MB"
                        Text(
                            text = sizeStr,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
