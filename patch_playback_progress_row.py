import re

with open("app/src/main/java/com/example/ui/screens/PlaybackProgressRow.kt", "r") as f:
    content = f.read()

target_fun = """fun PlaybackProgressRow(
    mediaController: androidx.media3.common.Player?,
    modifier: Modifier = Modifier
) {"""

replacement_fun = """fun PlaybackProgressRow(
    mediaController: androidx.media3.common.Player?,
    abRepeatStart: Long? = null,
    abRepeatEnd: Long? = null,
    modifier: Modifier = Modifier
) {"""

content = content.replace(target_fun, replacement_fun)

target_track = """            track = { sliderState ->
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
            },"""

replacement_track = """            track = { sliderState ->
                Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxWidth()) {
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
                    
                    if (duration > 0) {
                        if (abRepeatStart != null) {
                            val fraction = (abRepeatStart.toFloat() / duration).coerceIn(0f, 1f)
                            if (fraction > 0f) {
                                Box(modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .height(4.dp)) {
                                    Box(modifier = Modifier.align(Alignment.CenterEnd).size(8.dp).background(Color(0xFFFF9800), CircleShape))
                                }
                            } else {
                                Box(modifier = Modifier.size(8.dp).background(Color(0xFFFF9800), CircleShape))
                            }
                        }
                        if (abRepeatEnd != null) {
                            val fraction = (abRepeatEnd.toFloat() / duration).coerceIn(0f, 1f)
                            if (fraction > 0f) {
                                Box(modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .height(4.dp)) {
                                    Box(modifier = Modifier.align(Alignment.CenterEnd).size(8.dp).background(Color(0xFFFF9800), CircleShape))
                                }
                            } else {
                                Box(modifier = Modifier.size(8.dp).background(Color(0xFFFF9800), CircleShape))
                            }
                        }
                    }
                }
            },"""

content = content.replace(target_track, replacement_track)

with open("app/src/main/java/com/example/ui/screens/PlaybackProgressRow.kt", "w") as f:
    f.write(content)
