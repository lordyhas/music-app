# Muzik

**Muzik** is a modern, lightweight local audio player for Android built with Google's recommended stack: **Jetpack Compose**, **Material 3**, and **AndroidX Media3 (ExoPlayer)**.

It provides a seamless, fast, and elegant offline listening experience with zero ads and no internet connection required.

---

## Features

- **High-Performance Audio Playback**:
  - Audio engine powered by **AndroidX Media3 / ExoPlayer**.
  - Background playback via a dedicated foreground service (`MediaSessionService`).
  - Lockscreen controls and system notification media controls.
  - Full playback queue support with shuffle and repeat modes (*Repeat All*, *Repeat One*, *Off*).

- **Modern & Fluid UI (Jetpack Compose & Material 3)**:
  - Polished dark theme with subtle gradients and fluid navigation animations.
  - **Persistent Mini-Player**: seamless browsing while keeping playback controls accessible.
  - **Animated Fullscreen Now Playing screen**: high-res album artwork, thin seek bar, and instant queue management.

- **Local Music Organization**:
  - **Home**: smart greetings based on the time of day, recently played tracks, albums, and artists.
  - **Tracks**: complete list of all local songs with real-time search, sorting, and fast indexing via `MediaStore`.
  - **Albums**: browse by album with cover artwork, disc numbers, and track numbers.
  - **Artists**: explore your music library by artist and quickly access their discography.
  - **Custom Playlists**: create, edit, and manage custom playlists backed by **Room Database**.

- **Track Details & Metadata**:
  - Inspect detailed audio information (title, artist, album, duration, file format, bitrate, sample rate, and file path).

-  **Privacy & Permission Handling**:
  - 100% offline — your data stays strictly on your device.
  - Full compatibility with modern Android permission models (`READ_MEDIA_AUDIO`, `POST_NOTIFICATIONS` on Android 13+, legacy storage on older versions).

---

## 🛠️ Tech Stack & Architecture

| Component | Technology |
|---|---|
| **Language** | [Kotlin](https://kotlinlang.org/) |
| **User Interface** | [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3) |
| **Architecture** | MVVM (Model-View-ViewModel) + StateFlow |
| **Audio Engine** | [AndroidX Media3 ExoPlayer](https://developer.android.com/guide/topics/media/media3) & MediaSession |
| **Local Database** | [Room](https://developer.android.com/training/data-storage/room) with KSP |
| **Image Loading** | [Coil Compose](https://coil-kt.github.io/coil/compose/) |
| **Navigation** | Navigation Compose |

---

## Requirements

- **Android Studio**: Ladybug / Jellyfish or newer
- **JDK**: Version 11 (Java 11)
- **Min SDK**: Android 7.0 (API 24)
- **Target / Compile SDK**: Android 15 (API 36)

---

## Getting Started & Building

1. **Clone the repository**:
   ```bash
   git clone https://github.com/lordyhas/music-app.git
   cd music-app
   ```

2. **Open the project** in Android Studio.

3. **Sync Gradle**:
   Allow Gradle to download the dependencies and index the project.

4. **Build and Run**:
   - Via Android Studio: click **Run 'app'** (`Shift + F10`).
   - Via Command Line (Windows PowerShell):
     ```powershell
     .\gradlew.bat assembleDebug
     ```
   - The generated debug APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

---

## Project Structure

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/lordyhas/muzik/
│   ├── MainActivity.kt               # Main entry point activity
│   ├── data/
│   │   ├── local/                    # Room database (Playlists, DAOs, Entities)
│   │   ├── model/                    # Data models (Song, Album, Artist, Playlist)
│   │   └── repository/               # Repositories (MediaStore & Playlist data layers)
│   ├── service/
│   │   ├── MusicPlaybackService.kt   # Media3 background playback service
│   │   └── MusicController.kt        # Media controller bridge
│   └── ui/
│       ├── components/               # Reusable Compose components (MiniPlayer, NowPlaying, etc.)
│       ├── screens/                  # Main screens (Home, Tracks, Albums, Artists, Playlists...)
│       ├── theme/                    # Color palette, Typography, and Material3 theme
│       └── viewmodel/                # MusicViewModel (UI state management)
└── res/                              # Drawables, mipmaps, launcher icons, XML resources
```

---

## License

This project is licensed under the terms of the repository's open-source license.
