# Mika Music

Mika Music is a modern Android music player built with Kotlin and Jetpack Compose. It focuses on local, offline playback with a polished interface, customizable themes, and tools for organizing and enjoying your music library.

## Features

- Offline music playback with background playback support
- Browse songs, albums, artists, genres, folders, and playlists
- Search your music library and manage the playback queue
- Track artwork and lyrics support
- Equalizer and audio effects
- Custom themes, gradients, and playback UI
- Library scanning, statistics, and trash management
- Backup and restore support

## Screenshots

Sample screenshots are available in [`public/assets/aistudio/`](public/assets/aistudio/).

## Technology

- Kotlin
- Jetpack Compose and Material 3
- AndroidX Media3 / ExoPlayer
- Room and DataStore
- Coroutines
- Gradle Kotlin DSL

## Requirements

- Android Studio with Android SDK 36 support
- JDK 11 or newer
- Android device or emulator running Android 7.0 (API 24) or newer

## Build and Run

Clone the repository and open it in Android Studio:

```bash
git clone https://github.com/sagorxzx/Mika.git
cd Mika
```

Allow Gradle sync to finish, then run the `app` configuration on a device or emulator.

To build a debug APK from the command line:

```bash
./gradlew assembleDebug
```

The APK is generated under `app/build/outputs/apk/debug/`.

## Optional API Configuration

The project includes `.env.example` for optional Gemini API configuration. If you use Gemini-related functionality, copy the example and provide a key locally:

```bash
cp .env.example .env
```

Keep secrets out of version control. The example key is a placeholder, and the API configuration is not required for local music playback.

## Project Layout

```text
app/src/main/java/com/example/
├── data/       # Database, preferences, backup, and repository
├── media/      # Media scanning, artwork, and lyrics
├── model/      # Music and playback models
├── playback/   # Playback service and audio effects
└── ui/         # Compose screens, components, navigation, and theme
```

## Contributing

Bug reports and contributions are welcome. For substantial changes, please open an issue or pull request with a brief description of the proposed change.

## License

No license file is currently included. Please check with the repository owner before redistributing or reusing this project.
