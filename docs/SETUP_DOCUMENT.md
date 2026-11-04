# SurvivE // Emergence AI — Environment Setup & Developer Guide

## 1. System Requirements & Prerequisites

### 1.1 Development Environment
- **Operating System:** Windows 10/11, macOS (Apple Silicon or Intel), or Linux (Ubuntu 20.04+).
- **IDE:** Android Studio Ladybug (2024.2.1+) or newer.
- **Java Development Kit (JDK):** JDK 11 or JDK 17 (configured in Android Studio Embedded JDK).
- **Android SDK:**
  - `compileSdk`: 36 (Release 36.1)
  - `minSdk`: 29 (Android 10)
  - `targetSdk`: 36
  - Android SDK Build-Tools 34.0.0 or 35.0.0+
- **Gradle Version:** Gradle 8.9+ (managed via Gradle Wrapper `gradlew`).

### 1.2 Hardware Recommendations for Testing
- **Physical Device Strongly Recommended:** Testing Bluetooth RFCOMM mesh networking, Walkie-Talkie audio streaming, GPS providers, and hardware FM radio requires a physical Android device running Android 10 (API 29) to Android 15/16 (API 35/36).
- **Two Devices Recommended:** To fully test multi-hop BitChat mesh routing and real-time voice transmission, two or more physical Android phones are recommended.

---

## 2. Project Setup & Build Workflow

### 2.1 Repository Ingestion
Open terminal or PowerShell in your desired workspace and inspect the project root:
```powershell
cd C:\Users\umart\OneDrive\Desktop\SurvivE
```

### 2.2 Gradle Synchronization
Open the project folder in Android Studio. Android Studio will automatically invoke Gradle Sync. Alternatively, run via terminal:
```bash
./gradlew --refresh-dependencies
```

### 2.3 Compiling & Assembling Debug Build
To build the debug APK:
```bash
# On Windows PowerShell
.\gradlew.bat assembleDebug

# On macOS / Linux
./gradlew assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### 2.4 Deploying to a Connected Android Device
Ensure Developer Options and USB Debugging are enabled on your device:
```bash
# Verify ADB connection
adb devices

# Install APK directly
.\gradlew.bat installDebug

# Or using ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 3. Staging Offline Assets (Pre-Disaster Setup)

Because SurvivE is designed for 100% offline environments, external assets like AI models and vector maps should be staged during initial setup.

### 3.1 Pre-Staging Emergence AI Model (`.task`)
The on-device LLM engine requires the pre-quantized Qwen2.5-0.5B-Instruct LiteRT `.task` file (~380 MB).

#### Method A: In-App Staging (Recommended for Users)
1. Launch **SurvivE** and open the **AI** tab.
2. Tap **"Download from Repository"** to open the official Hugging Face URL:
   `https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct/resolve/main/Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task`
3. Once downloaded, tap **"Select Model File (.task)"** in SurvivE.
4. Select the downloaded file via the Android File Picker. The app will copy and stage it automatically.

#### Method B: Direct ADB Push (Fastest for Developers)
You can directly push the model file into the app's internal sandbox using ADB:
```bash
# Push model directly to app files directory
adb push Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task /data/local/tmp/qwen2_5_0_5b.task
adb shell run-as com.example.myapplication cp /data/local/tmp/qwen2_5_0_5b.task /data/user/0/com.example.myapplication/files/qwen2_5_0_5b.task
```

### 3.2 Pre-Staging Offline Mapsforge Vector Maps (`.map`)
1. Download regional vector maps (OpenStreetMap format) from the Mapsforge repository:
   `https://download.mapsforge.org/maps/v5/`
   *(Example: `asia/india.map`, `europe/germany.map`, `north-america/us-california.map`)*
2. In SurvivE, navigate to **Locator** -> **Offline Map** tab.
3. Tap **"Import .map File"** and select the `.map` file. The app registers the map and activates offline vector rendering immediately.

### 3.3 Humanitarian PDF Manuals Verification
The survival manuals are pre-bundled in `app/src/main/assets/`:
- `emergencyhandbook.pdf`
- `WFP-0000037858.pdf`
- `1546247785837-Logistic Cluster_Operational Guidelines.pdf`
- `1DDB64518AD4C6D4C1256C7C0039EF61-wfp-pocketbook-jul02.pdf`

These are automatically packaged into the APK binary during the Gradle build with zero additional developer action required.

---

## 4. Required Runtime Permissions Checklist

When launching the app for the first time, grant the following permissions when prompted:

| Permission | Android Version | Purpose in SurvivE |
|---|---|---|
| `ACCESS_FINE_LOCATION` | All Versions | High-precision GPS coordinates for Geohash encoding and radar bearing |
| `ACCESS_COARSE_LOCATION` | All Versions | Fallback location estimation |
| `BLUETOOTH_SCAN` | Android 12+ (API 31+) | Discovering nearby BitChat mesh nodes |
| `BLUETOOTH_CONNECT` | Android 12+ (API 31+) | Establishing RFCOMM communication channels and peer audio sockets |
| `BLUETOOTH_ADVERTISE` | Android 12+ (API 31+) | Broadcasting presence to other mesh nodes |
| `RECORD_AUDIO` | All Versions | Capturing live voice frames for the PTT Walkie-Talkie |
| `READ_CONTACTS` | All Versions | Selecting personal ICE emergency contacts from device phonebook |

---

## 5. Troubleshooting Common Issues

### Issue 1: Bluetooth Connection Fails on Android 12+
- **Symptom:** Discovered devices list remains empty or socket connection throws `SecurityException`.
- **Solution:** Verify that `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` runtime permissions are explicitly granted in device Settings -> Apps -> SurvivE -> Permissions.

### Issue 2: AI Model Ingestion Hangs or Crashes
- **Symptom:** Loading bar freezes or model fails initialization.
- **Solution:** Ensure the imported file is an authentic MediaPipe `.task` bundle (not a raw GGUF or PyTorch checkpoint). Verify device has at least 1 GB of free internal RAM before initializing the engine.

### Issue 3: Mapsforge Vector Map Renders Blank White Screen
- **Symptom:** Map loads but shows no roads or terrain.
- **Solution:** Tap the **"My Location"** Floating Action Button to center the viewport on your current coordinates. If your GPS coordinates are outside the bounding box of the downloaded country map, pan to the region covered by the `.map` file.
