package com.example.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.service.PlayerManager
import androidx.media3.common.MediaItem
import com.example.data.MediaRepository
import com.example.data.MediaFolder
import kotlinx.coroutines.runBlocking

class MediaWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return MediaWidgetFactory(this.applicationContext)
    }
}

class MediaWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var playlist = listOf<MediaItem>()
    private var folders = listOf<MediaFolder>()
    private var folderItems = listOf<com.example.data.MediaItem>()
    
    private var mode = "PLAYLIST"
    private var folderId: String? = null

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        mode = prefs.getString("mode", "PLAYLIST") ?: "PLAYLIST"
        folderId = prefs.getString("folder_id", null)
        
        if (mode == "PLAYLIST") {
            val player = PlayerManager.exoPlayer
            if (player != null) {
                val items = mutableListOf<MediaItem>()
                for (i in 0 until player.currentTimeline.windowCount) {
                    val window = androidx.media3.common.Timeline.Window()
                    player.currentTimeline.getWindow(i, window)
                    items.add(window.mediaItem)
                }
                playlist = items
            } else {
                playlist = emptyList()
            }
        } else if (mode == "FOLDERS") {
            runBlocking {
                val repo = MediaRepository(context)
                val allFolders = repo.getMediaFolders() // simplified
                folders = allFolders
                
                if (folderId != null) {
                    folderItems = folders.find { it.id == folderId }?.mediaItems ?: emptyList()
                } else {
                    folderItems = emptyList()
                }
            }
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int {
        if (mode == "PLAYLIST") return playlist.size
        if (mode == "FOLDERS") {
            if (folderId == null) return folders.size
            else return folderItems.size + 1 // +1 for "Up" button
        }
        return 0
    }

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_list_item)
        
        if (mode == "PLAYLIST") {
            val item = playlist[position]
            views.setTextViewText(R.id.widget_item_title, item.mediaMetadata.title?.toString() ?: item.mediaId)
            val fillInIntent = Intent().putExtra("EXTRA_INDEX", position).putExtra("WIDGET_ACTION", "PLAYLIST_ITEM")
            views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)
        } else if (mode == "FOLDERS") {
            if (folderId == null) {
                val folder = folders[position]
                views.setTextViewText(R.id.widget_item_title, "[Folder] " + folder.name)
                val fillInIntent = Intent().putExtra("FOLDER_ID", folder.id).putExtra("WIDGET_ACTION", "OPEN_FOLDER")
                views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)
            } else {
                if (position == 0) {
                    views.setTextViewText(R.id.widget_item_title, "[Back to Folders]")
                    val fillInIntent = Intent().putExtra("WIDGET_ACTION", "BACK_FOLDER")
                    views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)
                } else {
                    val file = folderItems[position - 1]
                    views.setTextViewText(R.id.widget_item_title, "[Media] " + file.name)
                    val fillInIntent = Intent()
                        .putExtra("MEDIA_URI", file.uri.toString())
                        .putExtra("WIDGET_ACTION", "PLAY_FILE")
                    views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)
                }
            }
        }
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
