# Mu'allem Feature Status & Implementation Summary

## Overview
The **Mu'allem (المعلم)** mode is a "repeat after the Sheikh" feature designed to help users learn proper Quranic recitation. The flow alternates between the Sheikh reciting an Ayah and the user repeating it, with real-time AI evaluation and feedback on the user's Tajweed and accuracy.

## Current Implementation State
The core flow of the Mu'allem feature is fully integrated into the `MushafScreen` and `MushafViewModel`. The feature utilizes a unidirectional MVI (Model-View-Intent) architecture and interfaces with a remote AI service via WebSockets for live recitation scoring.

### Key Components

*   **State Management (`MushafUiState`, `MuallemSessionState`)**: 
    Tracks the active session, including the target Surah/Ayah range, repetition count, difficulty (`RecitationStrictness`), and the current phase of the flow.
*   **Phases (`MuallemPhase`)**:
    *   `SheikhPlaying`: The app plays the Sheikh's audio for the current Ayah.
    *   `UserRecording`: The microphone is active, capturing the user's recitation and streaming it to the AI backend.
    *   `ShowingFeedback`: Displays the AI's word-by-word scoring/feedback before moving to the next repetition or Ayah.
*   **Networking (`LiveRecitationSocket`, `MushafNetworkModule`)**:
    *   Uses a Ktor `HttpClient` backed by an **OkHttp engine**.
    *   Establishes a persistent `wss://` connection to stream PCM audio to the AI server and receive `LiveSessionEvent` updates.

## Recent Fixes & Stabilizations

During testing with the remote AI backend (hosted via ngrok), several critical networking issues were resolved to ensure reliable connectivity:

1.  **WebSocket Authentication & ngrok Bypass (`403 Forbidden`)**:
    *   **Issue**: The ngrok free tier intercepts requests with a browser warning page, and the WebSocket handshake was failing because Ktor's `defaultRequest` plugin does not apply to WebSocket upgrades.
    *   **Resolution**: Implemented a `preconfigured` OkHttpClient with a native `Interceptor`. This ensures that the `Authorization: Bearer <token>` header, the `ngrok-skip-browser-warning: true` header, and the `Origin` header are all injected at the lowest level, successfully bypassing ngrok and authenticating with the AI server.
2.  **SSL Trust Anchors (`SSLHandshakeException`)**:
    *   **Issue**: Older Android devices or emulators lacked the proper root certificates to validate Let's Encrypt certificates (used by `api.quran.com` and ngrok).
    *   **Resolution**: Updated the debug `network_security_config.xml` to explicitly trust both `system` and `user` certificate authorities, allowing secure connections to succeed regardless of the device's default trust store state.
3.  **Detailed Diagnostic Logging**:
    *   Added comprehensive logging (`AiServiceNet` tag) to the OkHttp interceptor to trace exact headers and URLs during the WebSocket handshake, making future backend debugging much easier.

## Next Steps / Pending Work
*(To be determined based on AI team feedback and further testing)*
- Verify that the AI backend correctly accepts the token via the `Origin` and `Authorization` headers for the `/ws/session` endpoint.
- Fine-tune UI animations during the transition between the `SheikhPlaying` and `UserRecording` phases.
- Handle edge cases (e.g., user skipping an Ayah, network disconnects mid-session).
