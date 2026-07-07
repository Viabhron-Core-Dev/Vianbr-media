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
- Implemented video player gestures (volume, brightness, seek).
- Updated launcher and app icons to use the NextPlayer-inspired "play" icon.
- Grouped "Sort" and "Settings" actions into a 3-dots overflow menu in the MainScreen top app bar.
- Added a "Batch Convert" launcher alias with a layers/stack icon.
- Implemented A-B Repeat and Sleep Timer functionality in the player.
