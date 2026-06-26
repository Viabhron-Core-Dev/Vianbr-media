# Vianbr Media

A video player, audio player, photo editor, video editor, and batch media compressor for Android.

Built entirely on a low-end Android phone (3GB RAM) with no laptop or desktop.

## How this was built

- **Google AI Studio** — code generation and iteration
- **GitHub Actions** — APK compilation pipeline (no Android Studio needed)
- **Claude (Anthropic)** — architecture advice, code auditing, debugging direction
- **GitHub** — version control and source of truth

No Android Studio. No laptop. No USB debugging. Every build was compiled in the cloud and installed directly on device.

## Building the APK

The APK is built automatically via GitHub Actions on every push to `main`.

Go to the **Actions** tab → select the latest workflow run → download the APK from the Artifacts section.

## Requirements

- Android 8.0 (API 26) or higher
- Storage permission granted on first launch
