# Blueprint: Static Media Library & FFmpeg Editor

## Core Architecture & Philosophy
1. **Offline & Private**: Zero internet connectivity. No telemetry, no background calls.
2. **Stateless Editors**: Zero caching, zero edit history logging, and zero automatic re-imports after rendering. The application only remembers the persistent Output Folder URI.
3. **Modular & Lightweight**: Boot directly into the Library/Player. Editor modules stay suspended in memory until expressly invoked, behaving like separate on-demand utilities.
4. **Seamless External Integration**: Photo and Video editors can be launched via standard OS `Intent` (e.g. sharing a file from an external file manager).

## Feature Modules

### 1. Library (Default Boot State)
* **UI Aesthetics**: MiXplorer-style list structure and typography (without using its logo). Presents files and folders based on explicitly configured file types/extensions.
* **Media Presentation**: MX Player-style item layouts. Shows thumbnail, duration/time, file name, size, and date.
  - **New Marker**: Tiny red "New" badge on the top-left of thumbnails for unplayed/newly scanned media.
  - **Subtitle Detection**: If a subtitle file sharing the identical name exists in the folder, a blue subtitle badge appears next to the video name.
* **Navigation & Sorting**: Flat folder structure (MX Player style) – all folders containing media appear separately regardless of nesting depth. Supports sorting by date (applies globally or within specific folders).
* **Refresh Mechanic**: Manual pull-to-refresh supported. Scans trigger only on app start, after manual pull-to-refresh, and immediately following file modifications (no constant background scanning).
* **Component Selection**: Long-press activates multi-select mode. Context actions include: Play, Delete, Share, Properties, Rename, Edit (invokes built-in editors), and Add to Playlist.
* **Playback State & Logic**:
  - Remembers the last played playback position.
  - Looping modes: Loop One, Loop All, Loop None.
  - Custom Playlist management via multi-select options.
* **Floating Action Button (FAB) Runtime**:
  - *Main Directory*: FAB continues the last globally played media across the app.
  - *Inside a Folder*: FAB continues the last played media explicitly belonging to that specific folder.

### 2. Media Player (MX Player-Style)
* **Video Playback Engine**: Media3 (ExoPlayer).
* **UI Aesthetics**: Transparent black overlays for out-of-the-way controls. Status bar visibility is freely configurable via settings. Tapping the screen while playing reveals the UI.
* **Gesture Controls (Configurable Zones)**: Users can define gesture boundaries within settings. Default layout:
  - Double-tap: Toggle Play/Pause.
  - Left 25% boundary: Vertical swipe for Brightness scaling.
  - Right 25% boundary: Vertical swipe for System volume slide control.
  - Middle 50% boundary: Horizontal swipe for Forward/backward seeking in video.
* **Top Bar Structure**: Video title, playback speed, dedicated audio track selector, and subtitle track selector (automatically remembers explicit paths/choices). Ends with a classic 3-dot overflow menu for extra settings.
* **Tool Row (Folded, below top bar)**: 
  - *Screenshot*: Loaded purely on-demand to keep memory footprint lightweight.
  - *Input Lock*: Disables all screen touch polling until the floating padlock symbol is tapped.
  - *Loop Toggle*: Cycle between Loop One, Loop All, and None.
  - *Background Play*: Transitions purely to audio playback.
  - *Picture-in-Picture (PiP)*: Transitions video.
* **Bottom Bar & Transports**:
  - *Time & Progress*: Current time position, interactive Progress Bar (drag/tap slider to fast-seek), and Total Time (tap to continuously toggle to remaining time format).
  - *Media Controls*: Previous, 5-Second Rewind, Play/Pause toggle, 5-Second Forward, Next.
  - *Video Scaling*: Quick-toggle between Normal, Fit, Stretch-to-Fit, and Crop bounds.
* **Background / Notification Player**: Mirrors core controls (Pause, Next, Previous, Rev, Fwd) using standard Android MediaSession integrations.

### 3. Video Editor (Stateless FFmpeg Engine)
* **Launch Mechanism**: Invoked via Library context menu. Externally, an `ACTION_SEND` ("Share") intent routes to the Editor, while an `ACTION_VIEW` ("Open") intent routes strictly to the media Player.
* **UI Architecture**:
  - **Export Header (Top)**: Estimated file size indicator alongside a prominent "SAVE" button. Visual sliders for Resolution (up to 2k/4k), Frame Rate (24fps to 60fps), and Quality (Low, Medium, High).
  - **Preview Canvas (Center)**: Core media preview container.
  - **Tool Ribbon (Bottom)**: Horizontal tool toggles (Captions, BG, Record/Mic, Volume, Rotate, Flip). 
  - **Timeline Scrubber**: Filmstrip tracking with an "Add Clip" (+) feature, mute/unmute clip audio toggles, and total duration metrics.
* **FFmpeg Core & Storage Constraints**: Total offline processing, no AI features, no ads. Built via raw FFmpeg commands. \n  - *FFmpeg VS SAF Reality*: Since FFmpeg C/C++ libraries cannot natively output directly to Android's SAF `content://` URIs, the pipeline caches the encoded render temporarily into internal `Context.cacheDir`, seamlessly streams the bytes into the final SAF URI using ContentResolvers, and deletes the temp file.
* **UX & OOM Mitigation**:
  - Overlays a strict blocking "Please wait..." screen during active FFmpeg execution to safeguard user interaction.
  - Offloads rendering execution directly into a Foreground Service (pushing real-time progress to the Notification Bar) to survive Android OS kills.
* **Output Rules (SAF)**: Outputs cleanly to the preconfigured Output Folder URI. Strictly retains the original source filename (e.g., `video.mp4`), utilizing native Android Storage Access Framework (SAF) auto-rename (`video(1).mp4`) seamlessly if collisions occur.

### 4. Photo Editor (Stateless Canvas)
* **Launch Mechanism & Intent Routing**: Hidden from main UI.
  - *ACTION_VIEW ("Open with")*: Launches the full Photo Editor interface.
  - *ACTION_SEND ("Share")*: Bypasses the full editor and triggers a Batch Compression/Shrink queue.
* **Batch Compression Queue**: Processes shared images sequentially (one at a time) using a Foreground Service (Notification Bar) to prevent OOM kills, reducing file size with minimal quality loss.
* **Single Image Processing**: 
  - *Tools*: Crop, single-file Shrink, and Aspect Ratio manipulation.
  - *Canvas*: Standard desktop-style paint overlays (Drawing, Eraser, Color picker), Text insertion, and PiP composition.
  - *Format Conversion*: Built-in option to convert animated WebP and GIF files directly into standard Video files.

### 5. Audio/Music Editor (Stateless Trimmer)
* **Launch Mechanism**: Invoked via Library context menu for audio files. Externally, an `ACTION_SEND` ("Share") intent routes to the Editor, while an `ACTION_VIEW` ("Open") intent routes to the Audio Player.
* **UI Structure**: Simple central audio player bordered by Left and Right adjustment dials to facilitate precise minute-level timeline trimming.
* **Architecture & OOM Mitigation**: Shares the stateless FFmpeg engine for processing, using a Foreground Service Notification to guarantee processing completion without OS kills, exporting cleanly to the set Output Folder URI.

### 6. Settings & Interfaces
* **UI Aesthetics**: Settings adopts MiXplorer's clean visual layouts.
* **Structure**: Implements MX Player's root settings categories, but introduces distinct top-level "Editors" entries, which nest their own dedicated settings pages (e.g. Video Editor, Image Editor).
* **Configuration**:
  - Map the Output Folder URI (via Storage Access Framework).
  - Select target inclusion folders and explicit MIME/extension types to display in the Library.
  - Ensure zero interior tracking footprint (no memory of edited features/locations).

---
## Development Phases

### Phase 1: Welcome Page, Permissions & The Log Keeper (Completed)
*Target: Build the core startup flow, diagnostic nervous system, and secure basic storage permissions.*
* **Welcome Screen**: A first-launch initialization page where the Logger is started and users are onboarded.
* **Permission Requests**: Explicitly ask for required local media access (Audio and Video permissions), plus any necessary storage/SAF permissions directly from the Welcome Page.
* **The Log Keeper**:
  * Initialize the Singleton Logger and map `SharedPreferences` for persistence and crash-dumps.
  * Build the Global Diagnostic FAB (positioned in the **bottom right corner with a bug symbol**), acting as the master On/Off switch, floating over all app activities.

### Phase 2: Local Media Reading (Audio & Video) & IO Foundation (Completed)
*Target: Establish immediate local file fetching capability and output configurations.*
* **Media Reading**: Implement the MediaStore logic to read local **Audio** and **Video** files from the device.
* **SAF Configuration**: Basic Settings UI to handle `ACTION_OPEN_DOCUMENT_TREE` (SAF).
* Persistent storage of the "Output Folder URI".
* Configuration for inclusion folders and specific media extensions prioritizing Audio/Video (mp4, mkv, mp3, etc.).

### Phase 3: The Headless Library Engine (Completed)
*Target: Safe, background file traversal without UI blocking.*
* A filesystem scanner that processes the flat folder structure.
* Detection logic for companion subtitle files (same filename check).
* The logic to read timestamps, file sizes, and generate cache-friendly Video/Image thumbnails.

### Phase 4: Library UI & Context Menus (Completed)
*Target: The MiXplorer/MX Player hybrid interface.*
* Flat-folder list layouts (e.g., LazyColumn/RecyclerView).
* Manual pull-to-refresh gesture mapping.
* Long-press multi-selection logic and the context menu (Play, Delete, Rename, Options, Add to Playlist).

### Phase 5: Media3 (ExoPlayer) Core Lifecycle (Completed)
*Target: Stable video and background audio rendering.*
* Invoking the Player natively.
* Foreground Service mapping for Background Playback (audio-only).
* Picture-in-Picture (PiP) lifecycle hooks (handling window resizing seamlessly).

### Phase 6: Player UI & Invisible Gesture Zones
*Target: The transparent overlay and touch handling.*
* The configurable gesture zones (Brightness Left, Volume Right, Seek Middle, Double-Tap Play/Pause).
* The folded UI tool row (Screenshot, Lock, Loop, PiP).
* Audio/Subtitle track selection hooks mapping into ExoPlayer.

### Phase 7: App Intents & Universal Routing
*Target: Tying the OS to the App.*
* AndroidManifest configuration for `ACTION_VIEW` (Open) and `ACTION_SEND` (Share).
* An invisible routing activity that checks MIME types to dispatch the payload to the Library, Player, Video Editor, or Photo Editor.

### Phase 8: Photo Editor (Canvas & Compression)
*Target: Single & Batch image handling.*
* Batch Compression Queue via a Foreground Service Notification (for "Share" actions).
* The single-image Canvas UI for cropping, drawing, text, and aspect ratio manipulation.

### Phase 9: Audio Trimmer
*Target: Lightweight audio manipulation.*
* Audio playback UI with left/right timeline trimming dials.

### Phase 10: Video Editor (The FFmpeg Bridge)
*Target: Stateless, extremely lightweight video manipulation and rendering.*
* **UI Structure & Layout**: 
  - *Main UI*: ExoPlayer preview window (top), scrollable timeline/progress bar (bottom), and a scrollable bottom row of tool icons (Trim, Speed, Crop, Audio/Volume, Aspect Ratio, Rotate, Captions) matching an audio-editor aesthetic. Top bar includes Undo, Redo, Save, and Play/Preview buttons.
  - *Partial Tool UIs*: Tapping any tool opens a dedicated partial UI panel at the bottom, substituting the main tools row. Edits are non-destructive and saved to a lightweight in-memory parameter object (e.g., `trim_start: 12.0`, `mute: true`) when accepted.
  - *Live Preview*: The ExoPlayer in the Main UI dynamically simulates parameters in the lightweight data object (clamping playback to trims, applying volume changes) prior to the hard rendering step.
* **Included Tools**:
  - *Trimmer*: Double-dial UI similar to the Audio Editor. Supports "Normal" (keep inside) and "Cut" (remove middle part). No split required.
  - *Adjustments*: Speed, Crop, Aspect Ratio, Volume/Audio controls, Rotate, and Captions.
* **Export & Quality Control Panel**: 
  - Accessed via the "SAVE" button. Opens a localized panel for rendering options and compression.
  - Sliders for Resolution, Frame Rate, and Quality (Low/Medium/High). Continually displays an "Estimated file size".
  - Incorporates a **Converter Dropdown** menu (Default: `.mp4`, with `.mp3` for extracting audio-only, or converting `.webp` / `.gif` into video).
  - *Batch Mode/Direct Send*: When multiple video files are shared externally to the app, the Editor UI is bypassed. The app *only* presents this Quality/Converter panel alongside a progress bar to process them one-by-one sequentially.
* **FFmpeg Logic & Output**:
  - Output files preserve original formats by appending the execution setup date (e.g., `filename_YYYYMMDD_HHMM.ext`), storing safely directly to the SAF chosen Output Folder.
  - Generates the FFmpeg command based exclusively on the in-memory parameter object.
  - Runs in a Foreground Service to survive OS background kills with a blocking "Please wait..." UI when active.

### Phase 11: Polish & Deactivation
*Target: Releasing the app.*
* Hiding the floating Log Keeper FAB via the master switch.
* Final memory leak checks and performance profiling.

---
## Progress Ledger

* **Phase 1 Complete**: 
  - Scaffolded the base application with Navigation Compose.
  - Implemented the singleton `LogKeeper` for diagnostic dumping and SharedPreferences persistence.
  - Implemented a `WelcomeScreen` to onboard users and request explicit `READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO`, and `READ_EXTERNAL_STORAGE` permissions based on Android SDK levels.
  - Deployed the Global Diagnostic FAB (bug icon, bottom right) operating across the entire application interface.
* **Phase 1 Update**:
  - Re-mapped the `LogKeeper` state to store typed `LogEntry` objects instead of plain strings for more robust querying.
  - Implemented the full-screen `LoggerScreen` overlay with top app bar controls.
  - Built time-based filter pills (1h, 6h, 12h, 24h, All) to query the diagnostic local cache.
  - Added export options to immediately copy the structured log output to the clipboard or download the dump locally via MediaStore.
  - Wired the global diagnostic FAB to trigger the new `LoggerScreen` view conditionally over the application navigation host.
* **Phase 2 Complete**:
  - Established `SettingsManager` to persistently store the SAF Output Directory URI and media extension preferences.
  - Built out the `SettingsScreen` leveraging `ACTION_OPEN_DOCUMENT_TREE` to allow selection and URI granting capabilities for the media output folder.
  - Implemented `MediaRepository` tapping into device `MediaStore` safely to asynchronously gather local Video and Audio entities.
  - Wired a new `MediaViewModel` into the `MainScreen` to actively fetch, load, and present indexed media in a scaffolded, responsive list layout.
* **Phase 3 Complete**:
  - Headless Library Engine logic added to `MediaRepository` to traverse flat folder structures directly via `DocumentsContract`.
  - Folder grouping data model `MediaFolder` designed and pushed to the ViewModel.
  - Companion subtitle files matching (`.srt`, `.vtt`, `.ass`, `.sub`) cross-referenced by filename during directory scans.
  - Asynchronous MediaMetadataRetriever mechanism put in place to safely extract timestamps/duration for each video.
* **Phase 4 Complete**:
  - Deployed dynamic folder layouts on `MainScreen` closely mimicking the requested MX Player / Next Player list layout style.
  - Implemented back-navigation state via Compose to transition gracefully between the main library Folder List and the Media Listing inside the respective folders.
  - Added Long-press Multi-selection mode (`isMultiSelectMode`) on Media Items leveraging `combinedClickable`.
  - Constructed the Contextual `TopAppBar` appearing dynamically during multi-selection showing total files selected (e.g. `2/10 Selected`) with Play and Add to Playlist actions.
  - Constructed a `BottomAppBar` containing file properties, renaming and deleting functions during multi-selection mode.
  - Integrated `coil` and `coil-video` with `VideoFrameDecoder` to display real video thumbnail frame extracts inside media item cards.
  - Forced default Light Theme for clean MX Player aesthetic.
  - Added programmatic tagging framework (`PlaybackTag`: `NEW`, `UNSEEN`, `SEEN`) where media < 15 days old receives a `NEW` badge overlaid on thumbnails, dynamically formatting list item typography to represent its state.
  - Hooked a "Mark as..." context action locally in the `BottomAppBar` to allow batch changing of tags in multi-select mode.
* **Phase 4 Update**:
  - Implemented single-load optimization in MediaStore indexing to prevent unnecessary re-scanning during normal navigation.
  - Enhanced `MediaFolder` data model to track and cache `totalSize` and specific location `path`.
  - Updated Library UI to systematically reflect aggregate folder sizes and exact path locations under folder titles.
  - Stabilized and heavily optimized Video Thumbnail generation in Coil by forcing deep disk caching (100MB), Memory caching, and aggressive first-frame extraction (`videoFrameMillis(0)`) utilizing asynchronous crossfading to eliminate lazy-loading list stuttering.
* **Phase 5 Complete**:
  - Wired `androidx.media3` dependencies into Gradle properties and catalog.
  - Designed `PlaybackService` leveraging `MediaSessionService` to anchor native ExoPlayer background capabilities.
  - Implemented background hardware optimization for ExoPlayer, utilizing an inactivity timeout handler to stop the player and release UI surfaces if it remains paused for 5 minutes.
  - Created the ExoPlayer integration `PlayerScreen` view, routing heavily off `MediaController` callbacks to parse the explicit SAF file URI (using Base64 safe-encoding for Navigation routes).
  - Integrated `setPictureInPictureParams()` native hooks for Android 12+ (S) ensuring the application automatically enters PiP layout transitions on home swipe or task backgrounding.
  - Hooked playback state tracking (`currentPosition`, `duration`) into `SettingsManager` to remember positions and mark videos as 'Seen' upon 99% completion.

* **Phase 6 (Current / Pending)**:
  - Implement full Player UI (transparent black overlays for out-of-the-way controls). (Partially complete)
  - Implement gesture controls for brightness, volume, and playback seeking. (Partially complete)
  - **Picture-in-Picture (PiP)**: Keep as a placeholder (permission loop removed, PiP currently non-functional).
  - **Screen Orientation**: Add sensor-based landscape/portrait rotation switching in the player based on the video constraints and user rotation.
  - Future refinement of non-functional Context Menu actions from Phase 4 (Rename, Delete, Properties, Add to Playlist).
