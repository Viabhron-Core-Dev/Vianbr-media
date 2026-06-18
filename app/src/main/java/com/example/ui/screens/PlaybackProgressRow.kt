package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.session.MediaController
import kotlinx.coroutines.delay

@kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlaybackProgressRow(
    mediaController: MediaController?,
    modifier: Modifier = Modifier
) {
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var showRemainingTime by remember { mutableStateOf(false) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableStateOf(0f) }

    LaunchedEffect(mediaController) {
        while (true) {
            if (mediaController != null && !isScrubbing) {
                currentPosition = mediaController.currentPosition.coerceAtLeast(0L)
                duration = mediaController.duration.coerceAtLeast(1L)
            }
            delay(500)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(
            text = formatTime(if (isScrubbing) (scrubPosition * duration).toLong() else currentPosition),
            color = Color.White,
            fontSize = 12.sp
        )
        Slider(
            value = if (isScrubbing) scrubPosition else (if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f),
            onValueChange = { scale ->
                isScrubbing = true
                scrubPosition = scale
            },
            onValueChangeFinished = {
                mediaController?.seekTo((scrubPosition * duration).toLong())
                isScrubbing = false
            },
            thumb = {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(Color(0xFF2196F3), CircleShape)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color(0xFF2196F3),
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    drawStopIndicator = null,
                    thumbTrackGapSize = 0.dp,
                    trackInsideCornerSize = 0.dp,
                    modifier = Modifier.height(4.dp)
                )
            },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        )
        Text(
            text = if (showRemainingTime && duration > 0) "-" + formatTime(duration - currentPosition) else formatTime(duration),
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.clickable { showRemainingTime = !showRemainingTime }
        )
    }
}
