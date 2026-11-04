# SurvivE // Emergence AI — Features Specification Document

## 1. Feature Matrix Overview

SurvivE delivers an integrated suite of survival, communication, and situational awareness tools designed to function without internet, mobile towers, or power grids.

| Feature Module | Primary Function | Technical Enabler | Operational State |
|---|---|---|---|
| **Emergence AI** | Offline edge generative AI for triage & field procedures | MediaPipe GenAI / Qwen2.5-0.5B `.task` | Production Ready |
| **BitChat Mesh** | Multi-hop decentralized P2P text communication | Bluetooth RFCOMM + Custom Mesh Protocol | Production Ready |
| **PTT Walkie-Talkie** | Real-time half-duplex voice broadcasting | PCM 16kHz AudioRecord + AudioTrack | Production Ready |
| **Mesh Peer Radar** | Trigonometric radar HUD displaying nearby nodes | Custom Canvas Sweep + Haversine Bearing | Production Ready |
| **Offline Vector Maps** | Zero-network topographic & road navigation | Mapsforge 0.20.0 + OSM `.map` vector tiles | Production Ready |
| **Survival Manuals** | Pre-bundled humanitarian disaster operating guides | Native APK Assets + Secure FileProvider | Production Ready |
| **Emergency Hub** | Telephony signal gauge, speed dial & ICE contacts | Android TelephonyManager + ContactsContract | Production Ready |
| **Hardware FM Radio** | Direct analog broadcast radio invocation | Multi-vendor Intent Dispatcher | Production Ready |

---

## 2. Feature Deep-Dive

### 2.1 Emergence AI (Offline Field Intelligence)

```text
Prompt Chips:
[ Water Purification ]  [ Hypothermia Triage ]  [ Field Navigation ]  [ Shelter Construction ]
```

- **Zero-Cloud Inference:** Executes locally on device hardware without sending a single byte outside the phone.
- **Quantized LLM Engine:** Utilizes Google MediaPipe GenAI running an 8-bit quantized `Qwen2.5-0.5B-Instruct` model (~350-500MB).
- **Prompt Engineering for Survival:** Strict system prompt enforcing concise, numbered, actionable procedures with bold safety warnings and zero disclaimers or conversational fluff.
- **Dynamic Model Staging:**
  - One-tap SAF file picker to load `.task` models from internal storage, SD cards, or OTG flash drives.
  - Progress indicator displaying exact file copy percentages.
  - Browser download link to the official Hugging Face repository for pre-disaster staging.
- **Emergency Shortcut Chips:** One-touch query buttons for instant answers to critical survival questions.

---

### 2.2 BitChat Off-Grid Mesh Networking

```text
[Mesh Packet: Magic 0xBC] -> [LRU Deduplication] -> [Multi-Hop Forwarding (<=7 hops)] -> [Broadcast/Direct Delivery]
```

- **Infrastructureless Communication:** Direct device-to-device communication using Bluetooth RFCOMM Classic.
- **Multi-Hop Relay Protocol:** Devices automatically forward packets meant for other nodes up to 7 hops away, significantly expanding radio range beyond physical line-of-sight.
- **Collision & Loop Prevention:** 200-message LRU cache prevents broadcast storms and duplicated packets.
- **Store-and-Forward Offline Queue:** If a peer is temporarily unreachable, packets are stored in memory and automatically dispatched once the peer comes back into range.
- **Geohash Integration:** Every message can be tagged with the sender's 7-character Geohash for localized channel filtering.

---

### 2.3 Half-Duplex Push-to-Talk (PTT) Walkie-Talkie

```text
[HOLD TO TALK] -> [AudioRecord PCM 16kHz] -> [MeshPacket (Type: VOICE)] -> [AudioTrack Playback]
```

- **One-Touch Voice Transmission:** Tactical hold-to-talk button with dynamic pulsing visual feedback (`pulseAlpha` animation).
- **Audio Stream Optimization:** Uncompressed 16-bit 16kHz PCM audio engineered for low-latency transmission over Bluetooth serial streams.
- **Direct Remote Speaker Playback:** Incoming voice packets trigger immediate playback through `AudioTrack`, functioning identically to a standard VHF/UHF tactical radio handset.

---

### 2.4 Locator & Offline Geographic Awareness

#### 2.4.1 BitChat Off-Grid Mesh Radar
- **Visual Circular Radar Screen:** Renders a 360-degree rotating radar beam sweep using Jetpack Compose Canvas.
- **Concentric Metric Range Rings:** Displays calibrated distance rings at **10m**, **50m**, **100m**, and **500m**.
- **Accurate Peer Placement:**
  - **With GPS:** Calculates exact distance using the Haversine formula and compass heading using spherical trigonometry bearing equations.
  - **Without GPS:** Automatically falls back to RSSI RF path-loss distance estimation ($d = 10^{\frac{\text{MeasuredPower} - \text{RSSI}}{20}}$).

#### 2.4.2 Offline Vector Maps (Mapsforge)
- **Zero-Network Map Rendering:** Displays full street, contour, and terrain maps using Mapsforge vector rendering engine.
- **Custom Map Ingestion:** Ingests standard OpenStreetMap `.map` files with full zoom, pan, and coordinate centering.
- **Built-in Map Manager:** Direct links to the Mapsforge world map repository with import utilities via Android Storage Access Framework.

---

### 2.5 Humanitarian Survival Manuals

Pre-installed, permanently accessible documentation embedded directly in the application bundle:

1. **UNICEF Emergency Field Handbook (`emergencyhandbook.pdf`):**
   - Full standard operating procedures for crisis management, sanitation, water purification, and temporary shelter.
2. **WFP Emergency Preparedness Manual (`WFP-0000037858.pdf`):**
   - Logistics, warehouse management, food distribution, and field camp setup.
3. **WFP Logistics & Supply Chain Guidelines (`1546247785837-Logistic Cluster_Operational Guidelines.pdf`):**
   - Resource mobilization, emergency transport fleets, and distribution pipelines.
4. **WFP Humanitarian Response Pocketbook (`1DDB64518AD4C6D4C1256C7C0039EF61-wfp-pocketbook-jul02.pdf`):**
   - Rapid emergency assessments, casualty triage, and security protocols.

All manuals open securely in any installed document viewer via Android's `FileProvider` without requiring external internet or cloud storage permissions.

---

### 2.6 Emergency Contacts & Signal Telephony Diagnostics

- **National Emergency Hotlines:** Instant one-touch dialer access for:
  - **Police Hotline (100)**
  - **Medical Ambulance (102)**
  - **Fire Brigade (101)**
  - **Women Helpline (1091)**
  - **Disaster Response (108)**
- **Personal ICE (In Case of Emergency) Contact Importer:** Pick contacts from device address book via `ContactsContract` and save them into the emergency roster.
- **Cellular Signal Strength Monitor:** Real-time cellular radio telemetry displaying current signal level (0 to 4 bars) and connection status via `TelephonyManager`.

---

### 2.7 Emergency Hardware FM Radio Integration

- **Analog Broadcast Receiver Launcher:** Launches the phone's native hardware FM radio chip for receiving FM civil defense, NOAA, or disaster radio broadcasts.
- **Multi-OEM Support:** Checks and launches native FM packages across all major chipsets and smartphone manufacturers:
  - Qualcomm Snapdragon (`com.caf.fmradio`)
  - Samsung Electronics (`com.sec.android.app.fm`)
  - Xiaomi / Redmi / POCO (`com.miui.fm`)
  - Motorola Mobility (`com.motorola.fmplayer`)
  - MediaTek Inc. (`com.mediatek.fmradio`)
  - AOSP Stock FM (`com.android.fmradio`)
  - Realme (`com.realme.fmradio`)
  - Oppo (`com.oppo.fmradio`)
  - Transsion / Tecno / Infinix (`com.transsion.fmradio`)
  - Generic Android FM Intent (`android.intent.category.APP_FM`)
