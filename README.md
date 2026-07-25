# TvApp - Professional IPTV Player

**TvApp** is a high-performance Android application designed to play IPTV channels via M3U playlists. It features a user-friendly interface with advanced playback controls and channel management.

## 🚀 Features

*   **Advanced Playback:** Powered by Google's Media3 (ExoPlayer) with optimized buffering for smooth streaming even on unstable networks.
*   **Easy Navigation:**
    *   Next/Previous channel buttons.
    *   Quick skip (Jump 10 channels forward/backward).
    *   Direct channel selection by number.
*   **Channel Management:**
    *   Reorder channels (Move up/down).
    *   Delete channels with an **Undo** feature.
    *   Save your customized list locally.
*   **Auto-Update:** Fetch the latest channel list automatically from a remote GitHub source.
*   **User Experience:**
    *   Full-screen mode with gesture support (Swipe up/down for channels).
    *   Prevents screen dimming during playback.
    *   D-Pad and Remote Control support (Ideal for Android TV boxes).

## 🛠️ Technical Details

*   **Language:** Kotlin
*   **UI Framework:** Android XML with ViewBinding
*   **Video Engine:** [Media3 ExoPlayer](https://developer.android.com/guide/topics/media/exoplayer)
*   **Network:** HttpURLConnection for playlist fetching
*   **Concurrency:** Kotlin Coroutines (LifecycleScope)

## 📥 Getting Started

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/saudkisso81-alt/TvApp.git
    ```
2.  **Open in Android Studio:**
    Open the project and wait for Gradle sync.
3.  **Run:**
    Connect your device/emulator and press **Run**.

## 📄 Privacy Policy
The Privacy Policy for this application can be found [here](privacy-policy.md).

## 📩 Contact
Developed by: **Saud Kisso**
Email: [saudkisso81@gmail.com](mailto:saudkisso81@gmail.com)

---
*Disclaimer: This app is a media player only and does not provide any built-in content. Users must provide their own M3U playlists.*
