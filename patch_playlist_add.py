import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target = """            controller.setMediaItem(initialMediaItem)
            controller.prepare()
            
            val lastPos = settingsManager.getPlaybackPosition(decodedUriString)
            if (lastPos > 0 && !settingsManager.isFinished(decodedUriString)) {
                controller.seekTo(lastPos)
            }
            controller.play()"""

replacement = """            controller.setMediaItem(initialMediaItem)
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
            }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
