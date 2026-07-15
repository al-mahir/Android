# 5. Listen Mode — LSN

> **Product spec:** [Al-Mahir-SDD.md](../Al-Mahir-SDD.md) → "Epic 5: Listen Mode". That file is the canonical product requirement; this doc is the agent build brief.
> **Rules:** [AGENTS.md](../../AGENTS.md) — architecture, modules, MVI, stack, localization (do not restate them here).

**Priority:** MUST · **Primary modules:** :presentation, :domain, :data (large audio feature → SHOULD follow a per-feature layered split per AGENTS §2)

## Purpose
High-fidelity audio player with word-level highlight synchronization so users can listen to any reciter while following along on the Mushaf.

## Must-know constraints
- Word highlight is driven by **precise sub-second word-timestamp schemas** shipped with the audio feed (LSN-05) — advance the highlight from real timed offsets, never estimate from average word duration.
- Scrubbing MUST **snap to the nearest Ayah boundary** (LSN-03); Next/Prev jump to neighboring verse starts and re-layout the page immediately (LSN-04).
- Background + lockscreen playback via Android **MediaSession** / media-notification controls (LSN-07, AUD-02); keep playing when backgrounded unless interrupted.
- Interruption handling: auto-pause and gracefully recover on incoming calls / other audio focus loss (AUD-03).
- Speed control 0.75x–1.5x MUST use **pitch-preserving** time-stretch — no chipmunk distortion (LSN-06).
- Offline: cache downloaded audio as `.m4a`/`.mp3` in the app sandbox; the engine auto-falls back to the local path when the remote stream is unreachable (LSN-08/09, AVL-01).
- Audio errors show explanatory **retry** prompts and auto-resume on reconnect — never hard-crash (LSN-10, AVL-02).
- This mode captures **no microphone** and runs **no AI scoring** — mic pre-prompt / VAD / decoupled scoring do not apply here; it is playback-only.
- All controls localized (ar+en) and RTL-aware — Next/Prev and scrub direction mirror in RTL.

## Capabilities
| ID | Capability | Priority | Must-know rule |
|----|-----------|----------|----------------|
| LSN-01 | Start Listen Session | MUST | Highlight auto-updates from timed offsets in the audio feed. |
| LSN-02 | Select Reciter | MUST | Query remote CDN catalog of multiple qaris; stream the selected voice. |
| LSN-03 | Playback Controls | MUST | Seamless buffering; scrub snaps to nearest Ayah boundary. |
| LSN-04 | Ayah Navigation | MUST | Next/Prev jump to neighboring verse start; re-layout immediately. |
| LSN-05 | Word-by-Word Highlight | MUST | Requires precise sub-second word-timestamp schema synced to the audio file. |
| LSN-06 | Playback Speed Control | SHOULD | 0.75x–1.5x with linear pitch correction across all speeds. |
| LSN-07 | Background Playback | SHOULD | Integrate Android MediaSession + lockscreen media controls. |
| LSN-08 | Download Audio | SHOULD | Cache `.m4a`/`.mp3` in secure sandbox directories. |
| LSN-09 | Offline Listening | SHOULD | Switch automatically to local storage paths when streams unreachable. |
| LSN-10 | Audio Error Handling | SHOULD | Explanatory retry prompts; auto-resume playback on reconnect. |

## Design system (`:designsystem`)
- **needs new :designsystem component: AudioPlayerBar** — play/pause/resume, Ayah-snapping scrub bar, Next/Prev, and 0.75x–1.5x speed chip (no player exists in the catalog).
- Reciter picker: `AppBottomSheet` (reciter list) with `Shimmer` placeholders while the CDN catalog loads.
- **needs new :designsystem component: WordHighlight** — inline word-level highlight painted over Mushaf text (the page text itself is owned by `:mushaf:presentation`).
- Download management: `SectionedCard` rows per reciter/surah plus **needs new :designsystem component: DownloadProgress** indicator.
- Errors/offline: `NetworkErrorScreen(onRetry = …)` for stream failures (LSN-10); `StatusOverlay` `Error` for transient hiccups.
- Top bar: `BackTitleTopBar`. Read all colors/dp/sp via `Theme.*`; controls mirror correctly in RTL.

## Data, stack & offline
- `data`: Ktor client to the remote CDN for the reciter catalog, audio streams, and per-word timestamp JSON; Room stores downloaded-audio metadata + last playback position (feeds MUS-02 continue-reading). DTO/Room → domain mapping only — no leaks to UI.
- Playback engine: Media3/ExoPlayer with MediaSession for background + lockscreen (AUD-02) and pitch-preserving speed.
- Offline: downloaded audio + timestamps are fully playable offline; engine prefers local paths and reports a clear state if a stream drops. Sync last position via [[17-offline-sync]].

## Applicable NFRs
- NFR-AUD AUD-02 (background audio), AUD-03 (interruption handling).
- NFR-PRF PRF-02 (Mushaf page load < 400ms while following along), PRF-04 (stability).
- NFR-AVL AVL-01 (offline core), AVL-02 (graceful degradation).
- NFR-A11Y A11Y-02 (control labels), A11Y-03 (font scaling without breaking layout).
- NFR-LOC LOC-01 (ar+en), LOC-02 (RTL mirroring), LOC-04 (locale-aware duration/number formatting).

## Related features
- [[03-mushaf-reading]] — word highlight is painted over Mushaf pages; shares reading position.
- [[06-taahud-live-correction]] · [[07-muallem-repeat]] — reuse the reciter catalog and audio pipeline.
- [[16-settings]] — Audio Settings (SET-05): streaming, cache, default volume.
- [[17-offline-sync]] — downloaded-audio and position sync.
