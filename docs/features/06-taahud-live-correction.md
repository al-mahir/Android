# 6. Ta'ahud - Live AI Correction — TAH

> **Product spec:** [Al-Mahir-SDD.md](../Al-Mahir-SDD.md) → "Epic 6: Ta'ahud - Live AI Correction". That file is the canonical product requirement; this doc is the agent build brief.
> **Engineering handbook (contract, invariants, current status, gaps):** [06-taahud-engineering.md](06-taahud-engineering.md) — read this before writing code.
> **Rules:** [AGENTS.md](../../AGENTS.md) — architecture, modules, MVI, stack, localization (do not restate them here).

**Priority:** MUST · **Primary modules:** :presentation, :domain, :data (flagship AI/audio feature → SHOULD get its own layered module set per AGENTS §2)

## Purpose
Real-time interactive engine that analyzes spoken recitation against a selected verse range and surfaces mistakes live as the user speaks.

## Must-know constraints
- **Mic pre-prompt modal explaining privacy MUST appear before the native OS permission dialog** (TAH-02, SEC-03) — this ordering is non-negotiable.
- On-device **VAD** detects speech start/stop, skips room noise, saves bandwidth (TAH-03); **hybrid ASR** — local VAD + light ASR on-device, heavy scoring in the cloud (AUD-04).
- **Live feedback latency < 1.0s** from speech to on-screen error (PRF-01).
- Inline mistake highlight MUST use **color + icon + text** (never color alone), plus haptics/sound for status (TAH-05, A11Y-01).
- **Decoupled scoring**: Tashkil (vowels) and Tajwid (rules) are scored **independently** (ACC-04).
- Detect the full **shared mistake taxonomy** (SDD §4 ProtoBuf, `almahir.v1`): *Memorization* — missing / extra / incorrect word, incorrect sequence; *Tashkil* — incorrect / missing vowel; *Tajwid* — Madd, Ghunnah, Idgham, Ikhfa, Iqlab, Qalqalah, Makhraj (TAH-04). Wire to `MistakeCategory` / `MistakeType`.
- Surface AI **confidence** per mistake; clearly flag `is_uncertain` evaluations (ACC-02, AI-01).
- Capture pipeline: low-latency mic + echo cancellation + a **play-then-listen state machine** (AUD-01).
- **AI offline fallback**: if the cloud/connection drops, disable live mode gracefully with a clear message and recover recognition drops without ending the session (AI-12, AVL-02, TAH-13).
- Use domain-specific **Qur'anic ASR + forced alignment** models (ACC-03); persist sessions locally then sync (`SessionLog`, TAH-12).
- Localized (ar+en), RTL-aware; audio-retention/deletion privacy honored (PRV-01/02).

## Capabilities
| ID | Capability | Priority | Must-know rule |
|----|-----------|----------|----------------|
| TAH-01 | Start Session | MUST | Range selectable from a single verse up to a full Surah. |
| TAH-02 | Request Microphone | MUST | Privacy pre-prompt before the native OS permission dialog (SEC-03). |
| TAH-03 | Voice-Activity Detection | MUST | Local VAD captures only actual recitation, skips noise. |
| TAH-04 | Live Mistake Detection | MUST | Detect full Memorization/Tashkil/Tajwid taxonomy (SDD §4). |
| TAH-05 | Real-Time Inline Highlight | MUST | Render color + icon + text on the page (A11Y-01). |
| TAH-06 | View Mistake Details | MUST | Tap a mistake → overlay explaining the rule or missing word. |
| TAH-07 | Live Session Feedback | MUST | Accuracy + error counts update dynamically each completed verse. |
| TAH-08 | Pause & Resume | SHOULD | Freeze timer + analysis pipeline; keep session state in memory. |
| TAH-09 | Finish Session | MUST | Stop mic, conclude AI parsing, generate overview. |
| TAH-10 | View Session Summary | MUST | Overall scores + categorized mistake charts. |
| TAH-11 | Suggested Revision | SHOULD | Suggest target verses / tajwid rules from recurring errors. |
| TAH-12 | Save Session to History | MUST | Append to local history; schedule remote sync when connected. |
| TAH-13 | Recover Recognition Failures | SHOULD | Auto-restore pipeline on brief drops without breaking the UI. |

## Design system (`:designsystem`)
- **needs new :designsystem component: MicPermissionPreprompt** — privacy explainer shown before the OS dialog (SEC-03).
- **needs new :designsystem component: RecordingIndicator** — active-mic/VAD state with icon + text (not color alone).
- **needs new :designsystem component: MistakeHighlight** — inline color + icon + text mark over Mushaf words; tap-target for detail.
- Mistake detail + suggested revision: `AppBottomSheet` (mistake-detail overlay); `ExpandableSection` / `SectionedCard` for revision lists.
- Live stats: the shared **FeedbackBar** (see [[09-feedback-bar]]) renders live accuracy + running mistake count.
- **needs new :designsystem component: MistakeBreakdownChart** — categorized memorization/tajwid/tashkil summary (TAH-10).
- Range select: **needs new :designsystem component: VerseRangePicker**; strategy/scope chips via `DayTabRow`/`TabSelector`.
- Buttons: `PrimaryButton` (Start / Finish, `isLoading` while parsing), `SecondaryButton` (Pause/Resume), `IconButton` (mic toggle).
- Processing/offline: `StatusOverlay` (`Loading`/`Success`/`Error`) while finishing; `NetworkErrorScreen` / `EmptyDataScreen` with guidance when AI is unreachable (AI-12).
- `BackTitleTopBar`; all tokens via `Theme.*`; RTL-aware highlights and controls.

## Data, stack & offline
- `data`: low-latency mic capture (AudioRecord/Oboe) + on-device VAD + light ASR; **gRPC streaming to the cloud scoring service using the shared ProtoBuf contract** (`almahir.v1`: `EvaluationMistake`, `AyahFeedback`, `SessionLog`, ARC-02/03). Ktor for non-streaming calls; Room queues session history.
- Streaming eval: push audio frames, receive `AyahFeedback` (mistakes + `confidence` + `is_uncertain`), render inline within 1s. Keep Tashkil and Tajwid scores separate (ACC-04).
- Offline: no live scoring offline — disable and show guidance (AI-12); history reads from Room and queues sync ([[17-offline-sync]], OFF-05/06). Never expose gRPC/DTO/Room types to the UI.

## Applicable NFRs
- NFR-ACC ACC-01..04 (metrics, confidence, domain ASR, decoupled scoring).
- NFR-PRF PRF-01 (<1.0s feedback), PRF-03 (thermal/battery over 1h), PRF-04 (>=99.9% crash-free, structured audio logging).
- NFR-AUD AUD-01 (capture pipeline), AUD-03 (interruption), AUD-04 (hybrid ASR).
- NFR-A11Y A11Y-01 (icon+text+haptics, never color-alone), A11Y-02 (labels), A11Y-04 (Tashkil contrast).
- NFR-SEC SEC-03 (pre-prompt). NFR-PRV PRV-01/02/03 (audio retention, deletion, encryption).
- NFR-AVL AVL-02 (graceful AI degradation). NFR-LOC LOC-01/02. NFR-ARC ARC-02/03 (shared ProtoBuf + taxonomy).

## Related features
- [[09-feedback-bar]] — the live accuracy/mistake bar this mode drives.
- [[07-muallem-repeat]] · [[08-ikhtibar-exam]] — reuse the same capture + scoring pipeline and taxonomy.
- [[12-session-history]] — `SessionLog` persistence and review.
- [[14-ai-personalization]] — confidence surfacing (AI-01), explanations (AI-02), revision recommendations.
- [[03-mushaf-reading]] — highlights are rendered over Mushaf pages.
