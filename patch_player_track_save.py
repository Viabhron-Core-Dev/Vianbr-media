import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target_audio = """                                        modifier = Modifier.fillMaxWidth().clickable {
                                            val builder = mediaController?.trackSelectionParameters?.buildUpon()
                                            builder?.setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                                            builder?.let { mediaController?.trackSelectionParameters = it.build() }
                                            showAudioDialog = false
                                        }.padding(vertical = 12.dp)"""

replacement_audio = """                                        modifier = Modifier.fillMaxWidth().clickable {
                                            val builder = mediaController?.trackSelectionParameters?.buildUpon()
                                            builder?.setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                                            builder?.let { mediaController?.trackSelectionParameters = it.build() }
                                            var totalIdx = 0
                                            for (g in 0 until groupIndex) {
                                                totalIdx += audioGroups[g].length
                                            }
                                            totalIdx += trackIndex
                                            settingsManager.saveTrackSelection(decodedUriString, androidx.media3.common.C.TRACK_TYPE_AUDIO, totalIdx)
                                            showAudioDialog = false
                                        }.padding(vertical = 12.dp)"""

target_sub_off = """                                    modifier = Modifier.fillMaxWidth().clickable {
                                        val builder = mediaController?.trackSelectionParameters?.buildUpon()
                                        builder?.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true)
                                        builder?.clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_TEXT)
                                        builder?.let { mediaController?.trackSelectionParameters = it.build() }
                                        showSubtitleDialog = false
                                    }.padding(vertical = 12.dp)"""

replacement_sub_off = """                                    modifier = Modifier.fillMaxWidth().clickable {
                                        val builder = mediaController?.trackSelectionParameters?.buildUpon()
                                        builder?.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true)
                                        builder?.clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_TEXT)
                                        builder?.let { mediaController?.trackSelectionParameters = it.build() }
                                        settingsManager.saveTrackSelection(decodedUriString, androidx.media3.common.C.TRACK_TYPE_TEXT, -1)
                                        showSubtitleDialog = false
                                    }.padding(vertical = 12.dp)"""

target_sub = """                                        modifier = Modifier.fillMaxWidth().clickable {
                                            val builder = mediaController?.trackSelectionParameters?.buildUpon()
                                            builder?.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                                            builder?.setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                                            builder?.let { mediaController?.trackSelectionParameters = it.build() }
                                            showSubtitleDialog = false
                                        }.padding(vertical = 12.dp)"""

replacement_sub = """                                        modifier = Modifier.fillMaxWidth().clickable {
                                            val builder = mediaController?.trackSelectionParameters?.buildUpon()
                                            builder?.setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                                            builder?.setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
                                            builder?.let { mediaController?.trackSelectionParameters = it.build() }
                                            var totalIdx = 0
                                            for (g in 0 until groupIndex) {
                                                totalIdx += textGroups[g].length
                                            }
                                            totalIdx += trackIndex
                                            settingsManager.saveTrackSelection(decodedUriString, androidx.media3.common.C.TRACK_TYPE_TEXT, totalIdx)
                                            showSubtitleDialog = false
                                        }.padding(vertical = 12.dp)"""

content = content.replace(target_audio, replacement_audio)
content = content.replace(target_sub_off, replacement_sub_off)
content = content.replace(target_sub, replacement_sub)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
