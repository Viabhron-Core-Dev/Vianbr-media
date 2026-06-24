package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FFmpegBatchDialog(
    uris: List<String>,
    onDismiss: () -> Unit,
    onStartProcessing: (List<String>, String, String) -> Unit
) {
    var format by remember { mutableStateOf("mp4") }
    // More options could be added here (resolution, quality)
    // For now, we use a basic command template that just transcodes to the target format
    var quality by remember { mutableFloatStateOf(0.5f) } // 0..1

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export & Quality Control (${uris.size} files)") },
        text = {
            Column {
                Text("Estimated file size: ~ MB", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Quality")
                Slider(value = quality, onValueChange = { quality = it })
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Converter / Format")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilterChip(selected = format == "mp4", onClick = { format = "mp4" }, label= { Text("mp4")})
                    FilterChip(selected = format == "mp3", onClick = { format = "mp3" }, label= { Text("mp3")})
                    FilterChip(selected = format == "gif", onClick = { format = "gif" }, label= { Text("gif")})
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                // Determine FFmpeg command
                // Basic conversion: ffmpeg -i input -vcodec libx264 -crf (quality) output
                // For this mock, we just map quality to CRF 18-35.
                val crf = (35 - (quality * 17)).toInt()
                val cmd = when(format) {
                    "mp4" -> "-y -i %INPUT% -vcodec libx264 -crf $crf -preset fast %OUTPUT%"
                    "mp3" -> "-y -i %INPUT% -vn -acodec libmp3lame -q:a 2 %OUTPUT%"
                    "gif" -> "-y -i %INPUT% -vf \"fps=15,scale=320:-1:flags=lanczos,split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse\" -loop 0 %OUTPUT%"
                    else -> "-y -i %INPUT% %OUTPUT%"
                }
                onStartProcessing(uris, cmd, format)
            }) {
                Text("SAVE (Render)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
