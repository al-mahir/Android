# 7. Mu'allem - Repeat After the Sheikh — MLM

> **Product spec:** [Al-Mahir-SDD.md](../Al-Mahir-SDD.md) → "Epic 7: Mu'allem - Repeat After the Sheikh". That file is the canonical product requirement; this doc is the agent build brief.
> **Rules:** [AGENTS.md](../../AGENTS.md) — architecture, modules, MVI, stack, localization (do not restate them here).

**Priority:** SHOULD · **Primary modules:** :presentation, :domain, :data (audio/AI feature → MAY get its own layered module set per AGENTS §2)

## Purpose
Call-and-response learning loop: the user hears the Sheikh recite a verse, then repeats it, and each repetition is AI-evaluated until the target repeat count passes.

## Must-know constraints
- **Play-then-listen state machine** (AUD-01): the Sheikh's audio plays fully, *then* mic recording + UI prompt trigger (MLM-04) — never capture while playback is active; use echo cancellation.
- **Mic pre-prompt modal before the native OS permission dialog** (SEC-03).
- On-device **VAD** + **hybrid ASR** (local VAD/light ASR + cloud scoring) (AUD-04); every repetition is evaluated (MLM-05) against the **shared mistake taxonomy** (SDD §4 ProtoBuf, `almahir.v1`).
- **Decoupled scoring**: Tashkil and Tajwid scored independently (ACC-04).
- Error highlight after each recording MUST use **color + icon + text**, never color alone (MLM-06, A11Y-01).
- Repetition count is configurable **1–10 per verse**; **auto-advance only when the target count is met with passing scores** (MLM-03, MLM-07).
- Recitation style (Tahqiq / Tadwir / Hadr) **scales ASR temporal-alignment parameters** — pick the reciter/tempo and adjust alignment accordingly (MLM-08).
- Feedback rendering after each attempt still targets **< 1.0s** (PRF-01); **AI offline fallback** disables evaluation gracefully with a clear message (AI-12, AVL-02).
- Session restores after interruption (MLM-09); final stats saved to history as `SessionLog` (MLM-10). Localized (ar+en), RTL-aware.

## Capabilities
| ID | Capability | Priority | Must-know rule |
|----|-----------|----------|----------------|
| MLM-01 | Start Guided Session | SHOULD | Define target range; initialize the call-and-response loop. |
| MLM-02 | Select Sheikh | SHOULD | Stream high-quality target-verse audio from the chosen reciter. |
| MLM-03 | Set Repetition Count | SHOULD | Loop 1–10 repetitions per verse before advancing. |
| MLM-04 | Repeat After Sheikh | SHOULD | Trigger mic recording immediately after playback finishes. |
| MLM-05 | Evaluate Every Repetition | SHOULD | Local/cloud eval on each try; log every attempt to history. |
| MLM-06 | Highlight Practice Errors | SHOULD | Highlight mistakes (color+icon+text) after each recording. |
| MLM-07 | Auto-Advance Verses | SHOULD | Advance when target count is met with passing scores. |
| MLM-08 | Recitation Style Selection | COULD | Tahqiq/Tadwir/Hadr scales ASR temporal-alignment params. |
| MLM-09 | Session Restoration | SHOULD | Restore active parameters + progress after interruptions. |
| MLM-10 | Practice Session Complete | SHOULD | Save final stats; show progress dashboard for all runs. |

## Design system (`:designsystem`)
- Sheikh playback: reuse the **AudioPlayerBar** (see [[05-listen-mode]]) for the call phase.
- Recording/response: **RecordingIndicator** (icon+text mic state) and **MistakeHighlight** (see [[06-taahud-live-correction]]) after each attempt.
- Live/attempt stats: shared **FeedbackBar** (see [[09-feedback-bar]]).
- **needs new :designsystem component: RepetitionStepper** — 1–10 count selector with current-rep progress.
- Sheikh + style pickers: `AppBottomSheet`; recitation style (Tahqiq/Tadwir/Hadr) via `DayTabRow`/`TabSelector`.
- Summary/dashboard: `SectionedCard` + **MistakeBreakdownChart** (see [[06-taahud-live-correction]]).
- Buttons: `PrimaryButton` (Start / repeat now), `SecondaryButton` (skip/retry), `IconButton`.
- Processing/offline: `StatusOverlay` (`Loading`/`Success`/`Error`); `NetworkErrorScreen` when the reciter stream or AI is unreachable.
- `BackTitleTopBar`; all tokens via `Theme.*`; RTL-aware.

## Data, stack & offline
- `data`: Ktor to the reciter CDN for target-verse audio (shared with [[05-listen-mode]]); mic capture + VAD + light ASR; **gRPC cloud scoring per repetition using shared ProtoBuf** (`AyahFeedback`/`EvaluationMistake`, ARC-02/03); Room stores per-attempt logs + session state for restoration (MLM-09).
- Offline: downloaded Sheikh audio can play offline, but evaluation needs the cloud → disable eval gracefully (AI-12) while keeping the listen/repeat flow. Queue history sync ([[17-offline-sync]]). No DTO/Room/gRPC leakage to UI.

## Applicable NFRs
- NFR-AUD AUD-01 (play-then-listen capture), AUD-03 (interruption), AUD-04 (hybrid ASR).
- NFR-ACC ACC-03 (domain ASR), ACC-04 (decoupled scoring).
- NFR-PRF PRF-01 (<1.0s feedback), PRF-03 (thermal/battery).
- NFR-A11Y A11Y-01 (icon+text, never color-alone), A11Y-02 (labels).
- NFR-SEC SEC-03 (pre-prompt). NFR-AVL AVL-02 (graceful AI fallback). NFR-LOC LOC-01/02.
- NFR-ARC ARC-02/03 (shared ProtoBuf + taxonomy).

## Related features
- [[05-listen-mode]] — reuses reciter catalog + audio player for the call phase.
- [[06-taahud-live-correction]] — shares mic capture, scoring pipeline, taxonomy, and highlight/chart components.
- [[09-feedback-bar]] — attempt-level accuracy/mistake feedback.
- [[12-session-history]] — per-attempt logs and `SessionLog` persistence.
- [[14-ai-personalization]] — recommends Mu'allem for heavy memorization errors (AI-06).
