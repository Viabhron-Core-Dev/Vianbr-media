import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target = """            // Load playlist in background
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
                            // Instead of setting entire items, we can just add the items before and after
                            // Or use setMediaItems with a known start position and keep playback state.
                            // ExoPlayer handles setMediaItems(list, index, pos) seamlessly if the item is the same.
                            val currentPos = controller.currentPosition
                            val isPlaying = controller.isPlaying
                            controller.setMediaItems(playlistItems, startIndex, currentPos)
                            if (isPlaying) {
                                controller.play()
                            }
                        }
                    }
                }
            }"""

content = content.replace(target, "")

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
