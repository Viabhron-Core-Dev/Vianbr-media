package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class MediaWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        try {
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
            com.example.LogKeeper.log("Widget updated successfully for ${appWidgetIds.size} widgets", "MediaWidgetProvider")
        } catch (e: Exception) {
            com.example.LogKeeper.logError("MediaWidgetProvider", "Error in onUpdate", e)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_media)

        // Pending intent to launch main app
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                action = "com.example.ACTION_OPEN_PLAYER"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)

        // Intents for playback controls
        views.setOnClickPendingIntent(R.id.widget_btn_prev, getPendingIntent(context, "ACTION_PREV"))
        views.setOnClickPendingIntent(R.id.widget_btn_play, getPendingIntent(context, "ACTION_PLAY_PAUSE"))
        views.setOnClickPendingIntent(R.id.widget_btn_next, getPendingIntent(context, "ACTION_NEXT"))
        views.setOnClickPendingIntent(R.id.widget_btn_loop, getPendingIntent(context, "ACTION_LOOP"))
        views.setOnClickPendingIntent(R.id.widget_btn_shuffle, getPendingIntent(context, "ACTION_SHUFFLE"))
        views.setOnClickPendingIntent(R.id.widget_btn_mode, getPendingIntent(context, "ACTION_TOGGLE_MODE"))

        val searchIntent = Intent(context, MainActivity::class.java).apply {
            action = "ACTION_SEARCH"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val searchPendingIntent = PendingIntent.getActivity(
            context,
            2,
            searchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_search, searchPendingIntent)

        // Set up the collection (ListView)
        val serviceIntent = Intent(context, MediaWidgetService::class.java)
        views.setRemoteAdapter(R.id.widget_list, serviceIntent)
        
        val clickPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, MediaWidgetProvider::class.java).setAction("ACTION_PLAY_ITEM"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setPendingIntentTemplate(R.id.widget_list, clickPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getPendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, MediaWidgetProvider::class.java).setAction(action)
        return PendingIntent.getBroadcast(context, action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            super.onReceive(context, intent)
            val action = intent.action
            com.example.LogKeeper.log("onReceive action: $action", "MediaWidgetProvider")
        if (action == "ACTION_TOGGLE_MODE") {
            val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            val currentMode = prefs.getString("mode", "PLAYLIST")
            val nextMode = if (currentMode == "PLAYLIST") "FOLDERS" else "PLAYLIST"
            prefs.edit().putString("mode", nextMode).putString("folder_id", null).apply()
            
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = android.content.ComponentName(context, MediaWidgetProvider::class.java)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetManager.getAppWidgetIds(componentName), R.id.widget_list)
        } else if (action in listOf("ACTION_PLAY_PAUSE", "ACTION_PREV", "ACTION_NEXT", "ACTION_LOOP", "ACTION_SHUFFLE")) {
            val serviceIntent = Intent("com.example.ACTION_WIDGET_COMMAND")
            serviceIntent.setPackage(context.packageName)
            serviceIntent.putExtra("command", action)
            context.sendBroadcast(serviceIntent)
        } else if (action == "ACTION_PLAY_ITEM") {
            val widgetAction = intent.getStringExtra("WIDGET_ACTION")
            val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = android.content.ComponentName(context, MediaWidgetProvider::class.java)

            if (widgetAction == "OPEN_FOLDER") {
                val folderId = intent.getStringExtra("FOLDER_ID")
                prefs.edit().putString("folder_id", folderId).apply()
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetManager.getAppWidgetIds(componentName), R.id.widget_list)
            } else if (widgetAction == "BACK_FOLDER") {
                prefs.edit().putString("folder_id", null).apply()
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetManager.getAppWidgetIds(componentName), R.id.widget_list)
            } else if (widgetAction == "PLAY_FILE") {
                val uriStr = intent.getStringExtra("MEDIA_URI")
                val serviceIntent = Intent("com.example.ACTION_WIDGET_COMMAND")
                serviceIntent.setPackage(context.packageName)
                serviceIntent.putExtra("command", "ACTION_PLAY_FILE")
                serviceIntent.putExtra("uri", uriStr)
                context.sendBroadcast(serviceIntent)
            } else if (widgetAction == "PLAYLIST_ITEM") {
                val index = intent.getIntExtra("EXTRA_INDEX", 0)
                val serviceIntent = Intent("com.example.ACTION_WIDGET_COMMAND")
                serviceIntent.setPackage(context.packageName)
                serviceIntent.putExtra("command", "ACTION_PLAY_ITEM")
                serviceIntent.putExtra("index", index)
                context.sendBroadcast(serviceIntent)
            } else {
                // Fallback for older intents
                val index = intent.getIntExtra("EXTRA_INDEX", 0)
                val serviceIntent = Intent("com.example.ACTION_WIDGET_COMMAND")
                serviceIntent.setPackage(context.packageName)
                serviceIntent.putExtra("command", "ACTION_PLAY_ITEM")
                serviceIntent.putExtra("index", index)
                context.sendBroadcast(serviceIntent)
            }
        }
        } catch (e: Exception) {
            com.example.LogKeeper.logError("MediaWidgetProvider", "Error in onReceive", e)
        }
    }
}
