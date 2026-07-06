import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target_block = """        if (controller.currentMediaItem?.mediaId != decodedUri.toString()) {
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

            val mediaItem = MediaItem.Builder()
                .setUri(decodedUri)
                .setMediaId(decodedUri.toString())
                .setMediaMetadata(mediaMetadataBuilder.build())
                .build()

            controller.setMediaItem(mediaItem)
            controller.prepare()
            
            val lastPos = settingsManager.getPlaybackPosition(decodedUriString)
            if (lastPos > 0 && !settingsManager.isFinished(decodedUriString)) {
                controller.seekTo(lastPos)
            }
        } else if (controller.playbackState == androidx.media3.common.Player.STATE_ENDED || controller.playbackState == androidx.media3.common.Player.STATE_IDLE) {
            controller.seekTo(0)
            controller.prepare()
        }
        
        controller.play()"""

if target_block in content:
    content = content.replace(target_block, "")
else:
    print("Could not find target block to delete")

launched_effect_code = """
    LaunchedEffect(uriString) {
        val controller = mediaController ?: return@LaunchedEffect
        val settingsManager = com.example.data.SettingsManager.getInstance(context)
        
        if (controller.currentMediaItem?.mediaId != decodedUri.toString()) {
            val repository = com.example.data.MediaRepository(context.applicationContext as android.app.Application)
            val folders = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                repository.getMediaFolders()
            }
            
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
            
            if (found && playlistItems.isNotEmpty()) {
                controller.setMediaItems(playlistItems, startIndex, androidx.media3.common.C.TIME_UNSET)
            } else {
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

                val mediaItem = MediaItem.Builder()
                    .setUri(decodedUri)
                    .setMediaId(decodedUri.toString())
                    .setMediaMetadata(mediaMetadataBuilder.build())
                    .build()

                controller.setMediaItem(mediaItem)
            }
            controller.prepare()
            
            val lastPos = settingsManager.getPlaybackPosition(decodedUriString)
            if (lastPos > 0 && !settingsManager.isFinished(decodedUriString)) {
                controller.seekTo(lastPos)
            }
        } else if (controller.playbackState == androidx.media3.common.Player.STATE_ENDED || controller.playbackState == androidx.media3.common.Player.STATE_IDLE) {
            controller.seekTo(0)
            controller.prepare()
        }
        
        controller.play()
    }
"""

# Insert LaunchedEffect just above DisposableEffect(uriString)
content = content.replace("    DisposableEffect(uriString) {", launched_effect_code + "\n    DisposableEffect(uriString) {")

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)

