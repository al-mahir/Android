# 14. AI Intelligence & Personalization — AI

> **Product spec:** [Al-Mahir-SDD.md](../Al-Mahir-SDD.md) → "Epic 14: AI Intelligence & Personalization". That file is the canonical product requirement; this doc is the agent build brief.
> **Rules:** [AGENTS.md](../../AGENTS.md) — architecture, modules, MVI, stack, localization (do not restate them here).

**Priority:** SHOULD · **Primary modules:** `:presentation`, `:domain`, `:data`

## Purpose
Algorithmic layer analyzing vocal accuracy, explaining detected mistakes, recommending revision material, and generating personalized, adaptive learning plans from the user's session history.

## Must-know constraints
- **Confidence surfaced + uncertain marked (ACC-02/AI-01):** every detected mistake must display its `confidence` (0.0–1.0 from `EvaluationMistake`) and clearly mark `is_uncertain` items in the UI — this is a MUST-honor honesty requirement, never present uncertain AI output as definitive.
- **Offline fallback is MUST (AI-12 / AVL-02 / OFF-08):** on AI service loss, disable live/cloud AI modes gracefully with a clear explanation message — never crash or freeze; reading stays available.
- **Shared schema (ARC-02):** all AI I/O uses the SDD §4 ProtoBuf (`EvaluationMistake`, `AyahFeedback`, `SessionLog`); explanations map `MistakeType`/`MistakeCategory` to targeted phonetic/tajwid rule content. Scoring is decoupled — Tashkil and Tajwid scored independently (ACC-04).
- **Recommendations read from local history:** revision-verse, tajwid-rule, mode, and study-plan recommendations (AI-03..07) are derived from the locally cached `SessionLog` mistake data — recommendation reads work offline even when live evaluation does not.
- **Human-in-the-loop reporting (AI-11):** "report incorrect AI" must package the evaluation log + associated audio and send to reviewers, respecting the recording-privacy/opt-in controls (PRV-01/PRV-02) — never upload audio the user has restricted.
- **Locale-aware output (LOC-04):** AI insight text, dates, forgetting-curve reminder times, and percentages use locale-aware formatting (Hijri options); all generated text localized Arabic + English, RTL-aware.
- **Domain-specific ASR (ACC-03):** evaluation relies on Qur'an-trained recognition/forced-alignment models; the mobile client consumes results — it does not run general-purpose ASR.

## Capabilities
| ID | Capability | Priority | Must-know rule |
|----|-----------|----------|----------------|
| AI-01 | Show AI Confidence | SHOULD | Surface confidence for every detected mistake; clearly mark "uncertain" feedback. |
| AI-02 | Explain Detected Mistakes | SHOULD | Connect error types to targeted phonetic + tajwid rule explanations. |
| AI-03 | Recommend Revision Verses | COULD | Recommend focus areas by frequency + intensity of recent mistakes. |
| AI-04 | Recommend Tajwid Rules | COULD | Suggest exercises when the user repeatedly fails the same rule (e.g. Ghunnah, Qalqalah). |
| AI-05 | Generate Study Plan | COULD | Build dynamic weekly routines matching daily goals + error history. |
| AI-06 | Recommend Learning Mode | COULD | Suggest Mu'allem for heavy memorization errors, Ta'ahud for minor vocal slips. |
| AI-07 | Adaptive Difficulty | COULD | Adjust exam/practice prompt density by historical accuracy. |
| AI-08 | Smart Mistake Prediction | LATER | Post-MVP: predict likely-struggle verses from general student data. |
| AI-09 | Personalized Insights | COULD | Text summaries of improvement trends across recitation modes. |
| AI-10 | Intelligent Reminders | COULD | Spaced-repetition/forgetting-curve models trigger smart review notifications. |
| AI-11 | Report Incorrect AI | SHOULD | Flag questionable evaluations; send logs + audio to human reviewers. |
| AI-12 | AI Offline Fallback | MUST | On AI service loss, disable live modes with a clear message — no crash. |

## Design system (`:designsystem`)
- Confidence indicator (AI-01) → needs new :designsystem component: ConfidenceBadge / UncertaintyChip (icon + text + value, never color alone per A11Y-01).
- Mistake explanation (AI-02) → `AppBottomSheet` (needs new :designsystem component) or `ExpandableAccentCard`; rule points via `AccentBullet`.
- Recommendations / study plan (AI-03..07) → `SectionedCard` + `ExpandableSection`; mode/day switch → `TabSelector` / `DayTabRow`; insight cards (AI-09) → needs new :designsystem component: InsightCard.
- Report-incorrect flow (AI-11) → `AppBottomSheet` with `PrimaryButton` / `SecondaryButton`; success/error via `AnimatedStatusOverlay` (`StatusOverlay`).
- Offline/AI-down fallback (AI-12) → `NetworkErrorScreen` with explanatory copy (disabled-mode banner, not a hard error); loading → `Shimmer` (needs new :designsystem component); no-data recommendations → `EmptyDataScreen`.
- Top bar → `BackTitleTopBar`. Read all colors/dp/sp via `Theme.*` — never hardcode.

## Data, stack & offline
- Ktor/gRPC calls the AI/scoring backend; requests/responses use the shared `EvaluationMistake`/`AyahFeedback`/`SessionLog` ProtoBuf (ARC-02). Recommendation/insight generation reads locally cached history.
- Room caches evaluation results, recommendations, and reminder schedules for offline read (OFF-03); AI-11 report payloads (log + audio ref) are queued offline and uploaded on reconnect (OFF-05/OFF-06).
- `domain` use cases own recommendation logic (frequency/intensity ranking, mode selection, difficulty adaptation) so they are pure-Kotlin unit-testable; connectivity/availability state drives the AI-12 graceful-degradation path.

## Applicable NFRs
- NFR-ACC ACC-02 (confidence surfaced, uncertain marked), ACC-03 (domain-specific ASR), ACC-04 (decoupled Tashkil/Tajwid scoring).
- NFR-AVL AVL-02 (graceful degradation offline), AVL-03 (offline queueing).
- NFR-PRV PRV-01 (retention policy), PRV-02 (audio erasure / opt-in respected in AI-11 uploads).
- NFR-LOC LOC-02 (RTL), LOC-04 (locale-aware numbers/dates/reminders, Hijri).
- NFR-A11Y A11Y-01 (confidence/uncertainty never color-alone).
- NFR-ARC ARC-02 (shared ProtoBuf), ARC-03 (unified Tajwid taxonomy), ARC-04 (vector embeddings for semantic recommendations).

## Related features
- [[06-taahud-live-correction]] — live mistake detection produces the evaluations AI explains and scores.
- [[08-ikhtibar-exam]] — adaptive difficulty (AI-07) tunes exam generation.
- [[07-muallem-repeat]] — AI-06 recommends Mu'allem mode for memorization-heavy errors.
- [[12-session-history]] — recommendation/insight inputs come from HIS `SessionLog` history.
- [[13-progress-gamification]] — AI-09 insights and PRG trends inform each other.
- [[15-notifications]] — AI-10 spaced-repetition reminders dispatched via notifications.
- [[17-offline-sync]] — AI-12 fallback and queued AI-11 report uploads.
