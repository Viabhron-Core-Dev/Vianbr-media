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
    
    val requiredPermissions = mutableListOf<String>()
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        requiredPermissions.add(android.Manifest.permission.READ_MEDIA_VIDEO)
        requiredPermissions.add(android.Manifest.permission.READ_MEDIA_AUDIO)
        requiredPermissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
    } else {
        requiredPermissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        requiredPermissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
    
    if (requiredPermissions.isNotEmpty()) {
        requestPermissions(requiredPermissions.toTypedArray(), 100)
    }
    
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        if (!android.os.Environment.isExternalStorageManager()) {
            try {
                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = android.net.Uri.parse(String.format("package:%s", packageName))
                startActivity(intent)
            } catch (e: Exception) {
                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        }
    }

    LogKeeper.init(this)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        var isLoggerOpen by remember { mutableStateOf(false) }

        if (isLoggerOpen) {
          LoggerScreen(onClose = { isLoggerOpen = false })
        } else {
          Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
              var initialIntentUri: String? = null
              if (intent?.action == android.content.Intent.ACTION_VIEW) {
                intent?.data?.let { uri ->
                  initialIntentUri = uri.toString()
                }
              }
              AppNavigation(initialIntentUri = initialIntentUri)
              
              // Global Diagnostic FAB
              val logEnabled by LogKeeper.isEnabled.collectAsState()
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

