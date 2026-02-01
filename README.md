# Autocrypt

A lightweight Android auto-clicker app. Record tap sequences and replay them automatically.

## Features

- **Record Mode**: Tap anywhere on screen to record click positions
- **Playback Mode**: Replay recorded clicks automatically
- **Transparent Recording**: Clicks are recorded AND forwarded to the underlying app
- **Persistent Storage**: Recorded sequences are saved and restored on app restart
- **Configurable**: Set repetition count and delay between cycles

## Download

Download the latest APK from the [releases](releases/) folder:

- [autocrypt-v1.2.apk](releases/autocrypt-v1.2.apk) - Latest version (countdown timer + credits)
- [autocrypt-v1.1.apk](releases/autocrypt-v1.1.apk) - Previous version

## Installation

1. Download the APK to your Android device
2. Open the APK file and install (you may need to allow installation from unknown sources)
3. Open **Autocrypt** and grant the required permissions:
   - **Overlay Permission**: Tap the button and enable "Display over other apps"
   - **Accessibility Service**: Tap the button, find "Autocrypt" and enable it

## Usage

### Recording Clicks

1. Open the app you want to automate (e.g., a game)
2. The Autocrypt control panel appears on the right side of the screen
3. Tap the **red REC button** to start recording
4. Tap anywhere on screen - each tap is recorded AND executed in the app
5. The counter shows how many clicks have been recorded
6. Tap **REC again** to stop recording

### Playing Back

1. Tap the **green PLAY button** to replay recorded clicks
2. The status shows current repetition (e.g., "1/10")
3. Tap **PLAY again** to stop playback

### Controls

| Button | Action |
|--------|--------|
| **REC** (red) | Start/Stop recording |
| **REC** (long press) | Clear all recorded clicks |
| **PLAY** (green) | Start/Stop playback |
| **SETTINGS** (gear) | Open main app settings |

## Configuration

In the main app, you can configure:

- **Repetitions**: Number of times to replay the sequence (default: 10)
- **Cycle Delay**: Pause between repetitions in seconds (default: 50s)

## Requirements

- Android 8.0 (API 26) or higher

## License

MIT License
