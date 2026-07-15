# 9. In-Session Feedback Bar — FBK

> **Product spec:** [Al-Mahir-SDD.md](../Al-Mahir-SDD.md) → "Epic 9: In-Session Feedback Bar". That file is the canonical product requirement; this doc is the agent build brief.
> **Rules:** [AGENTS.md](../../AGENTS.md) — architecture, modules, MVI, stack, localization (do not restate them here).

**Priority:** MUST · **Primary modules:** :presentation, :domain (+ :designsystem for the shared bar component)

## Purpose
Context-aware overlay layer that shows live stats and memorization hints during practice and assessment sessions.

## Must-know constraints
- This bar is a **single shared, reusable component** consumed by Ta'ahud, Mu'allem, and Ikhtibar — build it once (driven by a shared feedback state: accuracy %, mistake count, toggle flags), not per-mode.
- Live accuracy % and running mistake count update **immediately from the evaluation stream** as errors are detected (FBK-01/02), within **< 1.0s** (PRF-01).
- Accuracy/status indicators MUST use **icon + text + color**, never color alone (A11Y-01).
- **Hint actions carry consequences**: Show-Next-Word applies a **score penalty** (FBK-05); Reveal-Remaining-Ayah **marks the verse as a prompt error** (FBK-06). Compute these penalties in a `domain` use case, not the Composable.
- Hint/reveal actions MUST be **disabled in Exam mode** (IKH-01 disables inline hints) — the bar takes a "hints allowed" capability flag.
- Toggle Ayah Visibility **blurs/hides** the Mushaf text to force recall (FBK-03); Toggle Mistake Highlight instantly shows/hides inline highlights (FBK-04).
- Past-mistakes list slides up from local history for the active verse/page (FBK-07).
- Must be **clean, non-intrusive, WCAG-compliant** with subtle transitions and strong contrast (FBK-08, A11Y-04).
- Localized (ar+en), RTL-aware, locale-aware percentage/number formatting (LOC-04).

## Capabilities
| ID | Capability | Priority | Must-know rule |
|----|-----------|----------|----------------|
| FBK-01 | Live Session Accuracy | MUST | Real-time % updated by the evaluation stream. |
| FBK-02 | Running Mistake Count | MUST | Total mistakes on the current page; increments as errors detected. |
| FBK-03 | Toggle Ayah Visibility | MUST | Blur/hide Mushaf text; show only target verse bounds. |
| FBK-04 | Toggle Mistake Highlight | SHOULD | Instantly show/hide inline highlights across the page. |
| FBK-05 | Show Next Word (Hint) | SHOULD | Reveal only the next word; apply a score penalty. |
| FBK-06 | Reveal Remaining Ayah | SHOULD | Uncover full verse; mark it as containing a prompt error. |
| FBK-07 | Open Past Mistakes | SHOULD | Slide-up list of historically flagged errors on the passage. |
| FBK-08 | Minimize Visual Distraction | SHOULD | Clean typography, subtle transitions, WCAG-compliant. |

## Design system (`:designsystem`)
- **needs new :designsystem component: FeedbackBar** — the core deliverable of this epic: live accuracy chip, running mistake-count chip, and toggle actions in one compact, non-intrusive bar (icon + text + color, never color-alone per A11Y-01).
- Toggles: `IconButton` for hide-ayah, toggle-highlight, next-word, and reveal-remaining (each with a descriptive TalkBack label).
- **needs new :designsystem component: BlurredAyahOverlay** — the blur/hide treatment for FBK-03 (the Mushaf text itself is owned by `:mushaf:presentation`).
- Past mistakes: `AppBottomSheet` (slide-up) with `SectionedCard` / `ExpandableSection` rows.
- Transitions via `AnimatedVisibility`; all colors/dp/sp from `Theme.*`; RTL-aware layout (start/end).

## Data, stack & offline
- No dedicated remote data source — the bar is a **pure function of the active session's feedback state** (accuracy %, mistake count, toggle flags) supplied by the host mode's `domain` layer (Ta'ahud/Mu'allem/Ikhtibar).
- Hint penalties (FBK-05/06) are applied to the session score inside a `domain` use case (unit-tested), then reflected back into state — the Composable only dispatches intents.
- Past-mistakes (FBK-07) read from the local Room session-history cache ([[12-session-history]]); works offline.

## Applicable NFRs
- NFR-PRF PRF-01 (<1.0s live update), PRF-04 (stability).
- NFR-A11Y A11Y-01 (icon+text, never color-alone), A11Y-02 (control labels), A11Y-04 (contrast for Tashkil legibility).
- NFR-LOC LOC-01/02 (ar+en, RTL), LOC-04 (locale-aware % / number formatting).

## Related features
- [[06-taahud-live-correction]] · [[07-muallem-repeat]] · [[08-ikhtibar-exam]] — all host and drive this bar (hints suppressed in exams).
- [[03-mushaf-reading]] — blur/hide and highlight toggles act on Mushaf page text.
- [[12-session-history]] — source of the past-mistakes list (FBK-07).
