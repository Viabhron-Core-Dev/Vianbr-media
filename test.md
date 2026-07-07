1. **Background Loading for Playlist**: To make the "Next" and "Previous" buttons available in the player and notification bar, we load the entire folder of videos into ExoPlayer as a playlist using `controller.setMediaItems(...)` starting at the index of the clicked video.
2. **Preventing Auto-Advance**: We instruct ExoPlayer to *not* automatically jump to the next video when the current one finishes by setting `player.pauseAtEndOfMediaItems = true`. This is the exact property that NextPlayer uses for this behavior.
3. **Handling End-of-Media Event**: Because ExoPlayer is now paused at the end of the item instead of reaching the end of the whole playlist, it triggers `onPlayWhenReadyChanged` with a specific reason: `PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM`. 
4. **Clean Dismissal**: We listen for that specific event. When it fires, we execute two actions simultaneously: 
   - We close the `PlayerScreen` (navigate back).
   - We send an `ACTION_CLOSE` command to our `PlaybackService` which executes `player.stop()`, `player.clearMediaItems()`, and `stopSelf()`. This ensures the notification bar player completely disappears just like in NextPlayer.
