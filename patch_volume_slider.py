import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target = """        if (activeGesture != GestureType.NONE && !isInPipMode) {
            if (activeGesture == GestureType.VOLUME) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(start = 32.dp)
                            .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                            .padding(vertical = 12.dp, horizontal = 12.dp)
                    ) {
                        Text(
                            text = "${(gestureVolumeRatio * 200).roundToInt()}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .height(140.dp)
                                .width(4.dp)
                                .background(Color.DarkGray.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(gestureVolumeRatio.coerceIn(0f, 1f))
                                    .width(4.dp)
                                    .background(Color(0xFF2196F3), androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Icon(androidx.compose.material.icons.Icons.Filled.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            } else if (activeGesture == GestureType.SEEK) {"""

replacement = """        if (activeGesture != GestureType.NONE && !isInPipMode) {
            if (activeGesture == GestureType.VOLUME) {
                Box(modifier = Modifier.fillMaxSize().padding(top = 64.dp, bottom = 104.dp), contentAlignment = Alignment.CenterStart) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(start = 32.dp)
                            .fillMaxHeight()
                            .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                            .padding(vertical = 12.dp, horizontal = 12.dp)
                    ) {
                        Text(
                            text = "${(gestureVolumeRatio * 200).roundToInt()}%",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .width(4.dp)
                                .background(Color.DarkGray.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight((gestureVolumeRatio * 2f).coerceIn(0f, 1f))
                                    .width(4.dp)
                                    .background(Color(0xFF2196F3), androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                            )
                            val whiteRatio = ((gestureVolumeRatio - 0.5f) * 2f).coerceIn(0f, 1f)
                            if (whiteRatio > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(whiteRatio)
                                        .width(4.dp)
                                        .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Icon(androidx.compose.material.icons.Icons.Filled.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            } else if (activeGesture == GestureType.SEEK) {"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
