package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log

import android.provider.DocumentsContract

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
        val settings = SettingsManager.getInstance(context)
        val folderUris = settings.folderUris.value
        val exts = settings.extensions.value

        for (treeUri in folderUris) {
            try {
                val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
                scanDirectory(treeUri, rootDocId, exts, mediaList)
            } catch (e: Exception) {
                Log.e("MediaRepository", "Error accessing tree: ${treeUri}, ${e.message}")
            }
        }

        return mediaList.sortedByDescending { it.dateAdded }
    }

    private fun scanDirectory(treeUri: Uri, documentId: String, extensions: List<String>, list: MutableList<MediaItem>) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

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

                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        scanDirectory(treeUri, docId, extensions, list)
                    } else {
                        val ext = name.substringAfterLast('.', "").lowercase()
                        if (extensions.contains(ext)) {
                            val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            val mediaType = if (mimeType.startsWith("video/")) MediaType.VIDEO else MediaType.AUDIO
                            list.add(MediaItem(docId.hashCode().toLong(), uri, name, size, 0L, date, mediaType))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MediaRepository", "Error scanning dir: ${documentId}, ${e.message}")
        }
    }
}
