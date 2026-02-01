# Autocrypt

A lightweight Android auto-clicker app. Record tap sequences and replay them automatically.

## Features

- **Record Mode**: Tap anywhere on screen to record click positions
- **Playback Mode**: Replay recorded clicks automatically
- **Transparent Recording**: Clicks are recorded AND forwarded to the underlying app
- **Persistent Storage**: Recorded sequences are saved and restored on app restart
- **Countdown Timer**: Shows time remaining before next cycle
- **Configurable**: Set repetition count and delay between cycles

## Download

Download the latest APK from the [releases](releases/) folder:

- [autocrypt-v1.2.apk](releases/autocrypt-v1.2.apk) - Latest version

---

# Installation Guide (Step by Step)

## Step 1: Download the APK

1. On your Android phone, open this page in Chrome (or any browser)
2. Tap on **[autocrypt-v1.2.apk](releases/autocrypt-v1.2.apk)**
3. Wait for the download to complete
4. You'll see a notification "Download complete"

---

## Step 2: Allow Installation from Unknown Sources

Since this app is not from Google Play, you need to allow your browser to install apps.

1. Tap the downloaded file notification (or go to Downloads)
2. Android will show: **"For your security, your phone is not allowed to install unknown apps from this source"**
3. Tap **Settings**
4. You'll see **"Allow from this source"** - Turn it **ON**
5. Tap the **back arrow** to return to the installation

> **Note**: This only allows Chrome (or your browser) to install this one app. It's safe.

---

## Step 3: Install the APK

1. After allowing unknown sources, you'll see the install screen
2. Tap **Install**
3. Wait for installation (takes a few seconds)
4. Tap **Open** to launch Autocrypt

---

## Step 4: Grant Overlay Permission

Autocrypt needs to display its control panel on top of other apps.

1. In the Autocrypt app, tap **"Overlay Permission"** button
2. Android opens the **"Display over other apps"** settings
3. Find **"Autocrypt"** in the list
4. Turn **ON** the switch for "Allow display over other apps"
5. Tap **back** to return to Autocrypt
6. The status should now show **"Granted"** in green

---

## Step 5: Enable Accessibility Service

Autocrypt needs the Accessibility Service to perform taps on screen.

1. In the Autocrypt app, tap **"Accessibility Service"** button
2. Android opens Accessibility Settings
3. Scroll down and find **"Autocrypt"** under "Downloaded apps" or "Installed services"
4. Tap on **"Autocrypt"**
5. Turn **ON** the switch
6. A warning popup appears - tap **"Allow"** or **"OK"**
7. Tap **back** twice to return to Autocrypt
8. The status should now show **"Enabled"** in green

> **Why this permission?** Android only allows apps with Accessibility Service to perform screen taps. This is the same system used by legitimate auto-clickers and assistive apps.

---

## Step 6: Start Using Autocrypt

Now both permissions are granted:

1. Set your **Repetitions** (how many times to repeat the sequence)
2. Set your **Delay** (seconds to wait between each cycle)
3. Tap **"Start Overlay"**
4. Switch to your game or app
5. The Autocrypt control panel appears on the right side of your screen

---

# How to Use

## Recording Clicks

1. Open the app you want to automate
2. The Autocrypt panel is on the **right edge** of the screen
3. Tap the **red REC button** to start recording
4. **Tap anywhere** on screen - each tap is:
   - Recorded by Autocrypt
   - AND executed in the app (so you see immediate feedback)
5. A counter shows how many clicks recorded
6. Tap **REC again** to stop recording

## Playing Back

1. Tap the **green PLAY button**
2. Autocrypt replays all your recorded taps
3. Status shows: "1/10" (current cycle / total)
4. Between cycles, a **countdown timer** shows seconds until next run
5. Tap **PLAY again** to stop

## Controls Reference

| Button | Action |
|--------|--------|
| **REC** (red) | Start/Stop recording |
| **REC** (long press) | Clear all recorded clicks |
| **PLAY** (green) | Start/Stop playback |
| **Gear icon** | Open settings |

---

## Configuration

In the main app settings:

- **Repetitions**: Number of times to replay (default: 10)
- **Cycle Delay**: Pause between cycles in seconds (default: 50s)

## Requirements

- Android 8.0 (API 26) or higher

## License

MIT License
