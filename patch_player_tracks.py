import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target = """        val mainListener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {"""

replacement = """        val mainListener = object : androidx.media3.common.Player.Listener {
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                // Restore track selection
                val audioTrackIdx = settingsManager.getTrackSelection(decodedUriString, androidx.media3.common.C.TRACK_TYPE_AUDIO)
                val subtitleTrackIdx = settingsManager.getTrackSelection(decodedUriString, androidx.media3.common.C.TRACK_TYPE_TEXT)
                
                var builder = controller.trackSelectionParameters.buildUpon()
                var changed = false
                
                if (audioTrackIdx != -1) {
                    val audioGroups = tracks.groups.filter { it.type == androidx.media3.common.C.TRACK_TYPE_AUDIO }
                    var found = false
                    var totalIdx = 0
                    for (group in audioGroups) {
                        for (i in 0 until group.length) {
                            if (totalIdx == audioTrackIdx) {
                                builder.setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, i))
                                changed = true
                                found = true
                                break
                            }
                            totalIdx++
                        }
                        if (found) break
                    }
                }
                
                if (subtitleTrackIdx != -1) {
                    val subtitleGroups = tracks.groups.filter { it.type == androidx.media3.common.C.TRACK_TYPE_TEXT }
                    var found = false
                    var totalIdx = 0
                    for (group in subtitleGroups) {
                        for (i in 0 until group.length) {
                            if (totalIdx == subtitleTrackIdx) {
                                builder.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                                builder.setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, i))
                                changed = true
                                found = true
                                break
                            }
                            totalIdx++
                        }
                        if (found) break
                    }
                }
                
                if (changed) {
                    controller.trackSelectionParameters = builder.build()
                }
            }
            override fun onPlaybackStateChanged(playbackState: Int) {"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
