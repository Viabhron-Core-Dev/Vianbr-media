package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import android.provider.DocumentsContract

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val size: Long,
    val duration: Long, // in milliseconds
    val dateAdded: Long,
    val mediaType: MediaType,
    val hasSubtitle: Boolean = false
)

data class MediaFolder(
    val id: String,
    val name: String,
    val path: String,
    val dateModified: Long,
    val mediaItems: List<MediaItem>
) {
    val totalSize: Long get() = mediaItems.sumOf { it.size }
    val videoCount: Int get() = mediaItems.size
    val totalDuration: Long get() = mediaItems.sumOf { it.duration }
}

enum class MediaType {
    AUDIO, VIDEO
}

class MediaRepository(private val context: Context) {
    
    fun getMediaFolders(): List<MediaFolder> {
        val folders = mutableListOf<MediaFolder>()
        val settings = SettingsManager.getInstance(context)
        val folderUris = settings.folderUris.value
        val exts = settings.extensions.value

        for (treeUri in folderUris) {
            try {
                val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
                val rootName = rootDocId.substringAfterLast('/', rootDocId.substringAfterLast(':'))
                scanDirectoryForFolders(treeUri, rootDocId, rootName, rootDocId, exts, folders)
            } catch (e: Exception) {
                Log.e("MediaRepository", "Error accessing tree: ${treeUri}, ${e.message}")
            }
        }

        return folders.distinctBy { it.id }.sortedBy { it.name.lowercase() }
    }

    private fun scanDirectoryForFolders(
        treeUri: Uri,
        documentId: String,
        folderName: String,
        folderPath: String,
        extensions: List<String>,
        folders: MutableList<MediaFolder>
    ) {
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
                    val docId = cursor.getString(idCol)
                    val name = cursor.getString(nameCol) ?: ""
                    val mimeType = cursor.getString(mimeCol) ?: ""
                    val size = cursor.getLong(sizeCol)
                    val date = cursor.getLong(dateCol)

                    if (date > latestDate) latestDate = date

                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        subDirs.add(Pair(docId, name))
                    } else {
                        val ext = name.substringAfterLast('.', "").lowercase()
                        if (extensions.contains(ext)) {
                            val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            val mediaType = if (mimeType.startsWith("video/") || ext in listOf("mp4", "mkv", "webm", "avi")) MediaType.VIDEO else MediaType.AUDIO
                            mediaItems.add(MediaItem(docId.hashCode().toLong(), uri, name, size, 0L, date, mediaType, false))
                        } else if (ext in listOf("srt", "vtt", "ass", "sub")) {
                            subtitleFiles.add(name.substringBeforeLast('.').lowercase())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MediaRepository", "Error scanning dir: ${documentId}, ${e.message}")
        }

        if (mediaItems.isNotEmpty()) {
            val updatedItems = mediaItems.map { item ->
                val baseName = item.name.substringBeforeLast('.').lowercase()
                val hasSub = subtitleFiles.contains(baseName)
                
                // Extract duration safely
                val duration = extractDuration(item.uri)

                item.copy(hasSubtitle = hasSub, duration = duration)
            }
            folders.add(MediaFolder(documentId, folderName, folderPath, latestDate, updatedItems.sortedByDescending { it.dateAdded }))
        }

        for ((subDocId, subName) in subDirs) {
            scanDirectoryForFolders(treeUri, subDocId, subName, "$folderPath/$subName", extensions, folders)
        }
    }

    private fun extractDuration(uri: Uri): Long {
        val retriever = android.media.MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            time?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {}
        }
    }
}
