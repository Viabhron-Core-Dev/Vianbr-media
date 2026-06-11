package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.LogKeeper

import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoggerScreen(onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val logs by LogKeeper.logs.collectAsState()
    val isEnabled by LogKeeper.isEnabled.collectAsState()

    var selectedFilterHours by remember { mutableLongStateOf(0L) } // 0 means 'All'

    val currentMillis = System.currentTimeMillis()
    val filteredLogs = remember(logs, selectedFilterHours) {
        if (selectedFilterHours == 0L) {
            logs.reversed() // show latest at top or bottom? Latest at bottom is nice, or top. Let's do top.
        } else {
            val threshold = currentMillis - (selectedFilterHours * 60 * 60 * 1000)
            logs.filter { it.timestampMs >= threshold }.reversed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Keeper") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                    }
                },
                actions = {
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { LogKeeper.toggleLogger() },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    IconButton(onClick = {
                        val allFormatted = filteredLogs.joinToString("\n") { it.formattedString }
                        clipboardManager.setText(AnnotatedString(allFormatted))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy to Clipboard")
                    }
                    IconButton(onClick = {
                        LogKeeper.dumpCurrentLogs(context)
                        Toast.makeText(context, "Dumped to Downloads", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.Download, contentDescription = "Download Logs")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filters
            ScrollableTabRow(
                selectedTabIndex = getTabIndex(selectedFilterHours),
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterTab("1h", selectedFilterHours == 1L) { selectedFilterHours = 1L }
                FilterTab("6h", selectedFilterHours == 6L) { selectedFilterHours = 6L }
                FilterTab("12h", selectedFilterHours == 12L) { selectedFilterHours = 12L }
                FilterTab("24h", selectedFilterHours == 24L) { selectedFilterHours = 24L }
                FilterTab("All", selectedFilterHours == 0L) { selectedFilterHours = 0L }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredLogs) { entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (entry.isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = entry.formattedTime,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = entry.tag,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = if (entry.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = entry.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (entry.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (entry.stackTrace != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = entry.stackTrace,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 10,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getTabIndex(hours: Long): Int {
    return when (hours) {
        1L -> 0
        6L -> 1
        12L -> 2
        24L -> 3
        else -> 4
    }
}

@Composable
private fun FilterTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Tab(
        selected = selected,
        onClick = onClick,
        text = { Text(text) }
    )
}
