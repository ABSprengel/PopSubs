# PopSubs - Standalone Android Subtitle Player (VTT/SRT)

> **This is a fork** of the original [PopSubs by RyuReyhan](https://github.com/RyuReyhan/PopSubs).
>
> **Heads up:** this fork is **completely AI-assisted** — I honestly don't really know what I'm
> doing here 😅. It builds and runs for me, but treat it accordingly. See the [License](#license)
> below (short version: do whatever you want, no promises).

## Description

PopSubs is an Android application for displaying WebVTT (`.vtt`) and SubRip (`.srt`) subtitle files in a **floating overlay window**, independent of any specific video player. It acts as a **standalone subtitle player**, making it useful when watching videos from sources lacking built-in subtitle support, or when you simply have a separate subtitle file you want to display on your screen.

## What this fork adds

On top of the original, this fork adds:

* **Fixed / tidied main-screen button layout.**
* **Settings bottom sheet** (gear icon) to configure captions and playback, with everything saved between launches.
* **Caption styling:** adjustable text size, text color, and caption box color — applied to the in-app view, the floating overlay, and fullscreen.
* **Playback speed** control (0.5×–2.0×).
* **Timing padding:** show captions earlier (pre-roll) and keep them on screen longer (post-roll).
* **Keep-screen-awake** is now an on/off setting.
* **Fullscreen presentation mode** (works in landscape) that shows only the caption over a solid, user-chosen background color.

## Motivation

This project was inspired by the lack of a simple, dedicated **standalone subtitle player for Android** similar to popular desktop applications like **Penguin Subtitle Player**. The goal was to create a lightweight **Android app** focused solely on loading a `.vtt` or `.srt` file and displaying the timed text in a configurable overlay, providing a functional equivalent for mobile users needing such a tool.

## Features

* **Load Local Subtitle Files:** Select `.vtt` and `.srt` files using the Android system file picker. Includes robust filename retrieval.
* **VTT/SRT Parsing:** Parses both WebVTT (`.vtt`) and SubRip (`.srt`) file formats to extract timings and text content. Handles basic format variations.
* **Simulated Playback:** Controls (`Play/Pause`, `Reset`) allow simulating subtitle progression without needing video, at an adjustable speed.
* **Seeking:** A `Material Slider` allows seeking to specific times within the subtitle file.
* **Accurate Timing:** Displays elapsed playback time in `MM:SS.ms` format.
* **Configurable Captions:** Text size, text color, and caption box color, plus pre-roll/post-roll timing padding.
* **Keep Screen Awake:** Optional setting that prevents the screen from sleeping during active playback.
* **Fullscreen Mode:** A distraction-free fullscreen/landscape view showing captions over a solid background color.
* **Floating Overlay Window:** Displays the current subtitle text in a system overlay window that floats above other applications (similar in concept to the **Penguin Subtitle Player** overlay).
* **Overlay Toggle:** Easily hide or show the floating overlay window using a button in the main app.

## Setup & Build

There are two ways to get the app:

1.  **Build from Source (Recommended for Developers):**
    * Clone this repository.
    * Open the project in a recent version of Android Studio.
    * Ensure the Material Components dependency (`implementation 'com.google.android.material:material:...'`) is present and synced.
    * Build the project (`Build` > `Make Project`).
    * Run on an Android device or emulator (API 21+).

2.  **Download Pre-built APK (easiest):**
    * Go to the **[Releases page](../../releases/latest)** and download `app-release.apk`.
    * On your Android device, open the downloaded file to install it.
    * If prompted, enable **"Install from unknown sources"** (or *"Install unknown apps"*) for your browser/file manager in Settings → Security.

## Usage

1.  Launch PopSubs.
2.  Click "Select VTT / SRT File" and choose a valid `.vtt` or `.srt` file.
3.  Use the Play/Pause/Reset buttons and the Slider to control playback.
4.  Tap the **gear** icon to open Settings (caption size/colors, playback speed, screen-awake, timing padding, fullscreen background).
5.  Tap the **fullscreen** icon for a full-screen/landscape caption view; tap the screen for controls, back to exit.
6.  Click "Toggle Overlay" (grant permission if needed) to show/hide the floating subtitles over other apps.

## Known Issues / Limitations

* Subtitle parsers are relatively basic; may not support all advanced VTT/SRT features (e.g., complex styling, positioning tags).
* Playback is simulated; **not** synchronized with external video/audio players.
* Basic error handling for malformed files.
* Overlay cannot be repositioned, and fullscreen backgrounds are solid colors only.

## License

Do whatever you want with it — use it, change it, ship it, sell it, no need to ask.
It comes with **no warranty and no promises**; if it breaks, you get to keep both pieces.

This fork is released into the public domain under [The Unlicense](LICENSE). The original project
belongs to [RyuReyhan](https://github.com/RyuReyhan/PopSubs) — please respect the original author's
terms for anything upstream.

## Keywords

Android, Subtitle Player, VTT Player, SRT Player, Standalone Subtitle Player, Floating Subtitles, Overlay Subtitles, Subtitle Overlay App, Penguin Subtitle Player Alternative, PopSubs
