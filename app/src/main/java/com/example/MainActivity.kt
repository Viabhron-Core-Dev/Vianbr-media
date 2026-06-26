package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.navigation.AppNavigation
import com.example.ui.screens.LoggerScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    coil.Coil.setImageLoader(
        coil.ImageLoader.Builder(this)
            .components {
                add(coil.decode.VideoFrameDecoder.Factory())
            }
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(0.15)
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(this.cacheDir.resolve("thumbnail_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100 MB max
                    .build()
            }
            .crossfade(true)
            .build()
    )

    LogKeeper.init(this)
    enableEdgeToEdge(
        statusBarStyle = androidx.activity.SystemBarStyle.light(
            android.graphics.Color.TRANSPARENT,
            android.graphics.Color.TRANSPARENT
        ),
        navigationBarStyle = androidx.activity.SystemBarStyle.light(
            android.graphics.Color.TRANSPARENT,
            android.graphics.Color.TRANSPARENT
        )
    )
    setContent {
      MyApplicationTheme {
        var isLoggerOpen by remember { mutableStateOf(false) }

        if (isLoggerOpen) {
          LoggerScreen(onClose = { isLoggerOpen = false })
        } else {
          Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
              var initialUris: List<String> = emptyList()
              if (intent?.action == android.content.Intent.ACTION_VIEW) {
                intent?.data?.let { uri ->
                  initialUris = listOf(uri.toString())
                }
              } else if (intent?.action == android.content.Intent.ACTION_SEND) {
                (intent?.getParcelableExtra<android.os.Parcelable>(android.content.Intent.EXTRA_STREAM) as? android.net.Uri)?.let { uri ->
                  initialUris = listOf(uri.toString())
                }
              } else if (intent?.action == android.content.Intent.ACTION_SEND_MULTIPLE) {
                val arrayList = intent?.getParcelableArrayListExtra<android.os.Parcelable>(android.content.Intent.EXTRA_STREAM)
                if (arrayList != null) {
                    val uris = mutableListOf<String>()
                    for (parcel in arrayList) {
                        (parcel as? android.net.Uri)?.let { uris.add(it.toString()) }
                    }
                    initialUris = uris
                }
              }
              
              val forceAction = intent?.component?.className?.let { className ->
                  if (className.endsWith("PlayMediaActivity")) "play"
                  else if (className.endsWith("EditMediaActivity")) "edit"
                  else null
              }
              
              AppNavigation(initialUris = initialUris, forceAction = forceAction)
              
              // Global Diagnostic FAB
              val logEnabled by LogKeeper.isEnabled.collectAsState()
              val settingsManager = remember { com.example.data.SettingsManager.getInstance(applicationContext) }
              val showLoggerFab by settingsManager.showLoggerFab.collectAsState()
              
              if (showLoggerFab) {
                  FloatingActionButton(
                    onClick = { isLoggerOpen = true },
                    modifier = Modifier
                      .align(Alignment.BottomStart)
                      .padding(innerPadding)
                      .padding(16.dp),
                    containerColor = if (logEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                  ) {
                    Icon(Icons.Filled.BugReport, contentDescription = "Open Logger")
                  }
              }
            }
          }
        }
      }
    }
  }
}

