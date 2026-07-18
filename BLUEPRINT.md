# Vianbr Media - App Blueprint

## Architecture
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Media Engine**: ExoPlayer (AndroidX Media3)

## Features
- **Media Library**: View local images, audio, and video files.
- **Video Player**: NextPlayer-inspired UI, gesture controls (volume/brightness), seeking.
- **Image Viewer**: Basic image viewing and editing (cropping, filters).
- **Audio/Video Trimming**: Trim and save media.
- **Batch Processing**: Batch convert and compress media files.

## Ledger of Changes
- Replaced PIP button with Close and Mini Player buttons in notification bar.
- Reduced MiniPlayer overlay size to 300x200 dp to fit on screen.
- Updated notification bar loop icon to show active (blue) state for "Loop All" and "Loop One".
- Adjusted PlayerScreen padding so video draws behind the navigation bar without shrinking, while keeping controls padded.
- Configured navigation bar to hide/show in sync with player controls.
- Implemented video player gestures (volume, brightness, seek).
- Updated launcher and app icons to use the NextPlayer-inspired "play" icon.
- Grouped "Sort" and "Settings" actions into a 3-dots overflow menu in the MainScreen top app bar.
- Added a "Batch Convert" launcher alias with a layers/stack icon.
- Implemented A-B Repeat and Sleep Timer functionality in the player.
- Fixed player bottom sheet panels appearing half hidden by forcing them to fully expand and adding navigation bar padding.
- Restored skip previous and skip next buttons to playback center alignment.
- Adjusted brightness slider to use a native material slider for direct horizontal drag control instead of capturing vertical screen-wide swipe gestures.
- Corrected Sleep Timer Dialog injection in the compose hierarchy.
- Added visual markers for A-B repeat points on the playback progress timeline slider.
- Reduced size and length of the on-screen volume gesture slider to avoid overlapping topbar icons.
