package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import android.provider.DocumentsContract

import com.example.LogKeeper

enum class PlaybackTag {
    NEW, UNSEEN, SEEN, PLAYING
}

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val duration: Long, // in milliseconds
    val dateAdded: Long,
    val mediaType: MediaType,
    val hasSubtitle: Boolean = false,
    val tag: PlaybackTag = PlaybackTag.NEW,
    val size: Long = 0L
)

data class MediaFolder(
    val id: String,
    val name: String,
    val path: String,
    val dateModified: Long,
    val totalSize: Long,
    val mediaItems: List<MediaItem>
) {
    val videoCount: Int get() = mediaItems.size
    val totalDuration: Long get() = mediaItems.sumOf { it.duration }
}

enum class MediaType {
    AUDIO, VIDEO, IMAGE
}

class MediaRepository(private val context: Context) {
    
    fun getMediaFolder(bucketId: String): MediaFolder? {
        val foldersMap = getMediaFolders()
        return foldersMap.find { it.id == bucketId }
    }

    fun getMediaFolders(): List<MediaFolder> {
        val foldersMap = mutableMapOf<String, MutableList<MediaItem>>()
        val folderNames = mutableMapOf<String, String>()
        val folderPaths = mutableMapOf<String, String>()
        val folderDates = mutableMapOf<String, Long>()
        val folderSizes = mutableMapOf<String, Long>()

        val settings = SettingsManager.getInstance(context)
        val excludedFolders = settings.excludedFolders.value
        val exts = settings.extensions.value

        val outputFolderUriVal = settings.outputFolderUri.value
        var customOutputSegment: String? = null
        if (!outputFolderUriVal.isNullOrEmpty()) {
            try {
                val treeUri = Uri.parse(outputFolderUriVal)
                val docId = DocumentsContract.getTreeDocumentId(treeUri)
                val segment = docId.substringAfter(':').trim('/')
                if (segment.isNotEmpty()) {
                    customOutputSegment = segment
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        try {
            val projection = arrayOf(
                android.provider.MediaStore.MediaColumns._ID,
                android.provider.MediaStore.MediaColumns.DISPLAY_NAME,
                android.provider.MediaStore.MediaColumns.DURATION,
                android.provider.MediaStore.MediaColumns.DATE_MODIFIED,
                android.provider.MediaStore.MediaColumns.BUCKET_ID,
                android.provider.MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                android.provider.MediaStore.MediaColumns.DATA,
                android.provider.MediaStore.MediaColumns.SIZE,
                android.provider.MediaStore.MediaColumns.MIME_TYPE
            )

            context.contentResolver.query(
                android.provider.MediaStore.Files.getContentUri("external"),
                projection, null, null, null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
                val durCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DURATION)
                val dateCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATE_MODIFIED)
                val bucketIdCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                val dataCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.MIME_TYPE)
                
                val currentTime = System.currentTimeMillis()
                val fifteenDaysMs = 15L * 24 * 60 * 60 * 1000

                while (cursor.moveToNext()) {
                    val bucketId = cursor.getString(bucketIdCol) ?: continue
                    if (excludedFolders.contains(bucketId)) continue

                    val dataPath = cursor.getString(dataCol) ?: ""
                    val isDefaultOutput = dataPath.lowercase().contains("/download/compressed")
                    val isCustomOutput = !customOutputSegment.isNullOrEmpty() && dataPath.lowercase().contains(customOutputSegment!!.lowercase())
                    if (isDefaultOutput || isCustomOutput) {
                        continue
                    }

                    val name = cursor.getString(nameCol) ?: continue
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (!exts.contains(ext) && !ext.isEmpty()) continue

                    val id = cursor.getLong(idCol)
                    val dur = cursor.getLong(durCol)
                    val dateMs = cursor.getLong(dateCol) * 1000
                    val bucketName = cursor.getString(bucketNameCol) ?: "Unknown Folder"
                    val itemSize = cursor.getLong(sizeCol)
                    val mimeType = cursor.getString(mimeCol) ?: ""

                    val mediaType = when {
                        mimeType.startsWith("video/") || ext in listOf("mp4", "mkv", "webm", "avi", "3gp", "mov", "flv", "wmv", "m4v") -> MediaType.VIDEO
                        mimeType.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "webp", "heic") -> MediaType.IMAGE
                        else -> MediaType.AUDIO
                    }

                    val baseUri = when (mediaType) {
                        MediaType.VIDEO -> android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        MediaType.IMAGE -> android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        MediaType.AUDIO -> android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }
                    val uri = android.content.ContentUris.withAppendedId(baseUri, id)
                    val uriStr = uri.toString()
                    
                    val isFinished = settings.isFinished(uriStr)
                    val playbackPos = settings.getPlaybackPosition(uriStr)
                    
                    val tag = if (isFinished) {
                        PlaybackTag.SEEN
                    } else if (playbackPos > 0L) {
                        PlaybackTag.PLAYING
                    } else {
                        if (currentTime - dateMs < fifteenDaysMs) PlaybackTag.NEW else PlaybackTag.UNSEEN
                    }

                    val item = MediaItem(
                        id = id,
                        uri = uri,
                        name = name,
                        duration = dur,
                        dateAdded = dateMs,
                        mediaType = mediaType,
                        hasSubtitle = false, // Subtitles not easily extracted this way without checking filesystem
                        tag = tag,
                        size = itemSize
                    )

                    foldersMap.getOrPut(bucketId) { mutableListOf() }.add(item)
                    folderNames[bucketId] = bucketName
                    if (folderPaths[bucketId].isNullOrEmpty() && dataPath.isNotEmpty()) {
                        folderPaths[bucketId] = java.io.File(dataPath).parent ?: ""
                    }
                    val existingDate = folderDates[bucketId] ?: 0L
                    if (dateMs > existingDate) {
                        folderDates[bucketId] = dateMs
                    }
                    folderSizes[bucketId] = (folderSizes[bucketId] ?: 0L) + itemSize
                }
            }
        } catch (e: Exception) {
            LogKeeper.logError("MediaRepository", "Error fetching from MediaStore: ${e.message}", e)
        }

        return foldersMap.map { (bucketId, items) ->
            MediaFolder(
                id = bucketId,
                name = folderNames[bucketId] ?: "Unknown",
                path = folderPaths[bucketId] ?: "",
                dateModified = folderDates[bucketId] ?: 0L,
                totalSize = folderSizes[bucketId] ?: 0L,
                mediaItems = items.sortedByDescending { it.dateAdded }
            )
        }.sortedBy { it.name.lowercase() }
    }

    private fun scanDirectoryForFolders(
        treeUri: Uri,
        documentId: String,
        folderName: String,
        folderPath: String,
        extensions: List<String>,
        folders: MutableList<MediaFolder>,
        scannedDocIds: MutableSet<String>,
        durationMap: Map<String, Long>
    ) {
        if (!scannedDocIds.add(documentId)) return

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        val mediaItems = mutableListOf<MediaItem>()
        val subtitleFiles = mutableSetOf<String>()
        val subDirs = mutableListOf<Pair<String, String>>()
        var latestDate = 0L

        try {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idCol) ?: continue
                    val name = cursor.getString(nameCol) ?: ""
                    val mimeType = cursor.getString(mimeCol) ?: ""
                    val size = cursor.getLong(sizeCol)
                    val date = cursor.getLong(dateCol)

                    if (date > latestDate) latestDate = date

                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        subDirs.add(Pair(docId, name))
                    } else {
                        val ext = name.substringAfterLast('.', "").lowercase()
                        if (extensions.contains(ext) || (ext.isEmpty() && mimeType.startsWith("video/"))) {
                            val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            val mediaType = when {
                                mimeType.startsWith("video/") || ext in listOf("mp4", "mkv", "webm", "avi", "3gp", "mov", "flv", "wmv", "m4v") -> MediaType.VIDEO
                                mimeType.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "webp", "heic") -> MediaType.IMAGE
                                else -> MediaType.AUDIO
                            }
                            mediaItems.add(MediaItem(id = docId.hashCode().toLong(), uri = uri, name = name, duration = 0L, dateAdded = date, mediaType = mediaType, hasSubtitle = false, size = size))
                        } else if (ext in listOf("srt", "vtt", "ass", "sub")) {
                            subtitleFiles.add(name.substringBeforeLast('.').lowercase())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LogKeeper.logError("MediaRepository", "Error scanning dir: ${documentId}, ${e.message}", e)
        }

        if (mediaItems.isNotEmpty()) {
            val currentTime = System.currentTimeMillis()
            val fifteenDaysMs = 15L * 24 * 60 * 60 * 1000
            val settingsManager = SettingsManager.getInstance(context)
            
            val updatedItems = mediaItems.map { item ->
                val baseName = item.name.substringBeforeLast('.').lowercase()
                val hasSub = subtitleFiles.contains(baseName)
                
                // Extract duration safely from map
                val duration = durationMap[item.name] ?: 0L
                
                val uriStr = item.uri.toString()
                val isFinished = settingsManager.isFinished(uriStr)
                val playbackPos = settingsManager.getPlaybackPosition(uriStr)
                
                val tag = if (isFinished) {
                    PlaybackTag.SEEN
                } else if (playbackPos > 0L) {
                    PlaybackTag.PLAYING
                } else {
                    if (currentTime - item.dateAdded < fifteenDaysMs) PlaybackTag.NEW else PlaybackTag.UNSEEN
                }

                item.copy(hasSubtitle = hasSub, duration = duration, tag = tag)
            }
            folders.add(MediaFolder(documentId, folderName, folderPath, latestDate, updatedItems.sumOf { 0L }, updatedItems.sortedByDescending { it.dateAdded }))
        }

        // Non-recursive: do not scan sub-directories
        // Removed subDirs loop per Alternative A
    }
}
