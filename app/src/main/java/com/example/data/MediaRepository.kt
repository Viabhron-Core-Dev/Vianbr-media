package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val size: Long,
    val duration: Long, // in milliseconds
    val dateAdded: Long,
    val mediaType: MediaType
)

enum class MediaType {
    AUDIO, VIDEO
}

class MediaRepository(private val context: Context) {
    
    fun getLocalMedia(): List<MediaItem> {
        val mediaList = mutableListOf<MediaItem>()
        
        // Query Videos
        mediaList.addAll(queryMedia(
            uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            mediaType = MediaType.VIDEO
        ))
        
        // Query Audio
        mediaList.addAll(queryMedia(
            uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            mediaType = MediaType.AUDIO
        ))

        return mediaList.sortedByDescending { it.dateAdded }
    }

    private fun queryMedia(uri: Uri, mediaType: MediaType): List<MediaItem> {
        val list = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            if (mediaType == MediaType.VIDEO) MediaStore.Video.VideoColumns.DURATION else MediaStore.Audio.AudioColumns.DURATION,
            MediaStore.MediaColumns.DATE_ADDED
        )

        try {
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val durationColumn = if (mediaType == MediaType.VIDEO) {
                    cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION)
                } else {
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION)
                }

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Unknown"
                    val size = cursor.getLong(sizeColumn)
                    val duration = cursor.getLong(durationColumn)
                    val dateAdded = cursor.getLong(dateColumn)

                    val contentUri = Uri.withAppendedPath(uri, id.toString())

                    list.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            name = name,
                            size = size,
                            duration = duration,
                            dateAdded = dateAdded,
                            mediaType = mediaType
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MediaRepository", "Error querying media: ${e.message}")
        }
        return list
    }
}
