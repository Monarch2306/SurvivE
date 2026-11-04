# SurvivE // Emergence AI — Implementation Roadmap

## 1. Roadmap Overview & Strategic Milestones

The SurvivE platform architecture is designed to evolve from an ad-hoc local emergency tool into a military-grade, disaster-hardened, multi-bearer survival communications operating system.

```mermaid
gantt
    title SurvivE Engineering Evolution Roadmap
    dateFormat  YYYY-Q#
    section Phase 1 (Current)
    Core Mesh & On-Device AI Baseline :done, p1, 2026-Q1, 2026-Q2
    section Phase 2 (Hardening)
    BLE Extended Adv & Wi-Fi Direct Mesh :active, p2, 2026-Q3, 2026-Q4
    End-to-End Cryptography (Noise Protocol) :p3, 2026-Q3, 2026-Q4
    section Phase 3 (Intelligence)
    On-Device Multimodal Vision (Triage/Flora) :p4, 2027-Q1, 2027-Q2
    Offline POI Vector Search & GPX Tracking :p5, 2027-Q1, 2027-Q2
    section Phase 4 (Long-Range Radio)
    Hardware LoRa / Meshtastic USB-OTG Interop :p6, 2027-Q3, 2027-Q4
    Satellite SOS API Interoperability (Android 15+) :p7, 2027-Q3, 2027-Q4
```

---

## 2. Phase-by-Phase Execution Plan

### Phase 1: Foundation & Baseline (Completed — v1.0)
- [x] **BitChat Mesh Protocol v1:** Binary framing (`MeshPacket`, Magic `0xBC`), 7-hop relaying, and 200-item LRU loop prevention.
- [x] **Half-Duplex PTT Audio:** 16kHz PCM voice streaming via RFCOMM serial channels.
- [x] **Emergence AI On-Device LLM:** MediaPipe GenAI engine execution of 8-bit quantized `Qwen2.5-0.5B-Instruct` model.
- [x] **Dynamic Offline Radar:** Canvas-rendered real-time 360-degree sweep HUD with spherical trigonometry peer positioning.
- [x] **Mapsforge Offline Vector Maps:** Local `.map` tile rendering without internet connectivity.
- [x] **Bundled Humanitarian Knowledge Base:** UNICEF and WFP survival manuals served via secure `FileProvider`.
- [x] **Emergency Hub & FM Launcher:** Multi-OEM hardware FM radio dispatch and cellular signal diagnostics.

---

### Phase 2: Mesh Hardening & Hybrid Transports (Next Priority — v1.5)
- [ ] **Dual-Radio Mesh Transport (BLE + Classic RFCOMM):**
  - Implement BLE Extended Advertising (Bluetooth 5.0+) for continuous, ultra-low-power peer presence beaconing without establishing battery-draining RFCOMM sockets.
  - Automatically promote connection to RFCOMM or Wi-Fi Direct only when large data payloads (audio or images) are transmitted.
- [ ] **Wi-Fi Aware (NAN) & Wi-Fi Direct Fallback:**
  - Introduce Wi-Fi Aware / Neighbor Awareness Networking (`WifiAwareManager`) to achieve high-throughput (5-10 Mbps) off-grid file transfers across distances up to 100 meters.
- [ ] **Dynamic AODV (Ad-hoc On-Demand Distance Vector) Routing:**
  - Transition from naive flood routing to structured route discovery utilizing `ROUTE_REQ` and `ROUTE_REP` packets, minimizing RF spectrum congestion.

---

### Phase 3: Cryptographic Zero-Trust & Privacy (v2.0)
- [ ] **Noise Protocol Framework Integration (Noise_XX):**
  - Implement mutual authentication and forward secrecy for all private node-to-node communications using Curve25519, ChaCha20, and Poly1305.
- [ ] **Decentralized Ephemeral Identity:**
  - Generate cryptographic public-key node identities that rotate periodically to prevent RF tracking by hostile or surveillance entities.
- [ ] **Tamper-Proof Message Hashing:**
  - Sign emergency broadcast packets with Ed25519 signatures to prevent malicious spoofing or panic manipulation in crowded disaster shelters.

---

### Phase 4: Edge Multimodal Vision & Enhanced Navigation (v2.5)
- [ ] **Computer Vision for Emergency Triage & Flora Identification:**
  - Integrate LiteRT / MediaPipe Vision models allowing users to photograph wounds, burns, or unfamiliar wild plants for instant offline toxicity and first-aid analysis.
- [ ] **Topographic Elevation Contours & Hillshading:**
  - Enhance Mapsforge integration with offline Digital Elevation Model (DEM) data (SRTM / HGT tiles) to render terrain contours and elevation relief for wilderness survival.
- [ ] **Offline POI & Rally Point Database:**
  - Bundle indexed SQLite / Spatialite database of natural water sources, high-ground shelters, ranger stations, and medical clinics searchable completely offline.

---

### Phase 5: Long-Range Hardware Radio Interoperability (v3.0)
- [ ] **LoRa / Meshtastic USB-OTG & Bluetooth Serial Bridge:**
  - Enable direct plug-and-play integration with hardware LoRa transceivers (SX1262 / SX1268 via USB-OTG or BLE) to extend BitChat mesh communications from 30 meters up to 15+ kilometers.
- [ ] **Android 15+ Satellite Messaging Integration:**
  - Interface with the Android Satellite Messaging API (`TelephonyManager.isSatelliteEnabled()`) to route SOS emergency beacons when terrestrial mesh relays are unavailable.
- [ ] **Audio Compression (Opus / Codec2):**
  - Replace uncompressed PCM with the ultra-low-bitrate **Codec2** (700 bps - 2400 bps) or **Opus** codec, allowing voice transmission over ultra-narrowband channels including LoRa and packet radio.

---

## 3. Engineering Risk & Mitigation Matrix

| Risk Factor | Severity | Probability | Mitigation Strategy |
|---|---|---|---|
| **Android Background Execution Limits (Doze Mode)** | High | High | Implement a Foreground Emergency Service (`android.app.Service`) with persistent low-power notification to prevent OS from killing Bluetooth listening sockets. |
| **RF Spectrum Congestion in Dense Crowds** | Medium | High | Enforce adaptive backoff algorithms, strict packet deduplication, and max hop reduction dynamically based on detected peer density. |
| **LLM Memory Pressure & Out-Of-Memory (OOM)** | High | Medium | Strict validation of device available RAM prior to model loading; support fallback to CPU-only inference or smaller 0.3B parameter models on low-end devices. |
| **OEM Bluetooth Stack Fragmentation** | Medium | High | Maintain dedicated compatibility layers and graceful fallback mechanisms between Bluetooth Classic SPP and BLE GATT. |
