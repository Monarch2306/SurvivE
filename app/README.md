# SurvivE — Android Application Module (`:app`)

This directory contains the primary Android application module for **SurvivE // Emergence AI**.

## Quick References & Full Documentation

All system architecture, design specifications, and implementation guidelines are located in the repository root [`../docs/`](file:///C:/Users/umart/OneDrive/Desktop/SurvivE/docs/):

- 📐 **Design Document:** [`../docs/DESIGN_DOCUMENT.md`](file:///C:/Users/umart/OneDrive/Desktop/SurvivE/docs/DESIGN_DOCUMENT.md)
- 🏛️ **Architecture Document:** [`../docs/ARCHITECTURE_DOCUMENT.md`](file:///C:/Users/umart/OneDrive/Desktop/SurvivE/docs/ARCHITECTURE_DOCUMENT.md)
- ✨ **Features Specification:** [`../docs/FEATURES_DOCUMENT.md`](file:///C:/Users/umart/OneDrive/Desktop/SurvivE/docs/FEATURES_DOCUMENT.md)
- ⚙️ **Setup Guide:** [`../docs/SETUP_DOCUMENT.md`](file:///C:/Users/umart/OneDrive/Desktop/SurvivE/docs/SETUP_DOCUMENT.md)
- 🗺️ **Implementation Roadmap:** [`../docs/IMPLEMENTATION_ROADMAP.md`](file:///C:/Users/umart/OneDrive/Desktop/SurvivE/docs/IMPLEMENTATION_ROADMAP.md)
- 🔒 **Security & Threat Model:** [`../docs/SECURITY_DOCUMENT.md`](file:///C:/Users/umart/OneDrive/Desktop/SurvivE/docs/SECURITY_DOCUMENT.md)
- 🧪 **Testing Strategy:** [`../docs/TESTING_DOCUMENT.md`](file:///C:/Users/umart/OneDrive/Desktop/SurvivE/docs/TESTING_DOCUMENT.md)
- 📘 **Project Root Portal:** [`../README.md`](file:///C:/Users/umart/OneDrive/Desktop/SurvivE/README.md)

## Key Source Components

- **`src/main/java/com/example/myapplication/bitchat/`**: Decentralized multi-hop mesh networking protocols (`MeshPacket.kt`, `BitChatMeshRouter.kt`, `GeohashUtils.kt`).
- **`src/main/java/com/example/myapplication/bluetooth/`**: Bluetooth RFCOMM sockets, discovery, and 16kHz PCM Walkie-Talkie audio engine (`BluetoothService.kt`, `BluetoothViewModel.kt`).
- **`src/main/java/com/example/myapplication/ui/ai/`**: MediaPipe GenAI on-device LLM engine and chat UI (`AiScreen.kt`, `AiViewModel.kt`).
- **`src/main/java/com/example/myapplication/ui/screens/LocatorScreen.kt`**: BitChat Canvas Peer Radar HUD and Mapsforge offline vector map viewer.
- **`src/main/java/com/example/myapplication/ui/screens/ManualScreen.kt`**: Offline PDF handbook viewer and emergency dialer.
- **`src/main/assets/`**: Pre-bundled humanitarian disaster manuals from UNICEF and the World Food Programme (WFP).
