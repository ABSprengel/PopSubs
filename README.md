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
* **Open from other apps:** tap a `.vtt` or `.srt` file in any file manager to open it directly in PopSubs.
* **Skip prev/next cue** buttons to jump between subtitle blocks (also available in fullscreen).
* **Seek buttons** (±5s, ±0.7s, ±0.1s) for quick fine-grained position adjustment.
* **Configurable time format:** choose between `MM:SS.mmm`, `MM:SS`, `H:MM:SS.mmm`, or `H:MM:SS` in Settings.

## Motivation

This project was inspired by the lack of a simple, dedicated **standalone subtitle player for Android** similar to popular desktop applications like **Penguin Subtitle Player**. The goal was to create a lightweight **Android app** focused solely on loading a `.vtt` or `.srt` file and displaying the timed text in a configurable overlay, providing a functional equivalent for mobile users needing such a tool.

## Features

* **Load subtitle files:** Select `.vtt` and `.srt` files via the in-app file picker, or open them directly from any file manager.
* **VTT/SRT parsing:** Supports both WebVTT and SubRip formats with robust timing and text extraction.
* **Playback controls:** Play/Pause, Reset, seek slider, skip prev/next cue buttons, and fine-grained seek buttons (±5s, ±0.7s, ±0.1s).
* **Adjustable speed:** 0.5×–2.0× playback speed.
* **Configurable time format:** `MM:SS.mmm`, `MM:SS`, `H:MM:SS.mmm`, or `H:MM:SS` — with a live example in Settings.
* **Configurable captions:** Text size, text color, caption box color, and pre-roll/post-roll timing padding.
* **Keep screen awake:** Optional setting that prevents the screen from sleeping during active playback.
* **Fullscreen mode:** A distraction-free fullscreen/landscape view showing captions over a solid background color.
* **Floating overlay window:** Displays the current subtitle text in a system overlay that floats above other applications (similar in concept to the **Penguin Subtitle Player** overlay).
* **Overlay toggle:** Easily hide or show the floating overlay using a button in the main app.

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

1.  Launch PopSubs, or tap a `.vtt` / `.srt` file in your file manager to open it directly.
2.  Use **Play/Pause**, **Reset**, the seek **slider**, **⏮/⏭** cue buttons, and the **±** seek buttons to control playback.
3.  Tap the **gear** icon to open Settings (caption size/colors, playback speed, time format, screen-awake, timing padding, fullscreen background).
4.  Tap the **fullscreen** icon for a distraction-free caption view; tap the screen for controls, press Back to exit.
5.  Tap **Toggle Overlay** (grant permission if needed) to show/hide the floating subtitles over other apps.

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
