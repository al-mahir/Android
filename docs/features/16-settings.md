# 16. Settings — SET

> **Product spec:** [Al-Mahir-SDD.md](../Al-Mahir-SDD.md) → "Epic 16: Settings". That file is the canonical product requirement; this doc is the agent build brief.
> **Rules:** [AGENTS.md](../../AGENTS.md) — architecture, modules, MVI, stack, localization (do not restate them here).

**Priority:** SHOULD · **Primary modules:** :presentation, :domain, :data

## Purpose
App localization, appearance (light/dark), font scaling, accessibility, audio, notification, privacy, and account configuration — the central preferences hub.

## Must-know constraints
- **Language switch (SET-01 / LOC-01 / LOC-02):** switching Arabic ⇄ English must reload core UI **instantly** and flip layout direction (Arabic RTL, English LTR) as a native mode. Drive it through `SpTheme(locale = …)` per-tree locale override — do not mutate global app config. RTL is not an afterthought.
- **Dark mode (SET-03 / A11Y-04):** the "Emerald Calm" dark palette must keep **Tashkil markings legible** with contrast ≥ 4.5:1. Use `darkColors` tokens via `Theme.*`; never hardcode colors. Verify Tashkil contrast, not just body text.
- **Font size scaling (SET-10 / A11Y-03):** UI text scales with the chosen size **without breaking Mushaf page alignment**. Mushaf uses fixed QUL vector typesetting — font scaling applies to chrome/translation text, and must not reflow or misalign the Mushaf spread (see [[03-mushaf-reading]] / ARC-05).
- Settings are **offline-first** (AVL-01): all preferences read/write to local Room and take effect immediately; account/privacy actions needing the network degrade gracefully and never fake success.
- **Reset (SET-09)** clears local config only — it MUST NOT delete user progress, history, or saved searches.
- Sub-screens delegate rather than duplicate: SET-06 → [[15-notifications]], SET-07 privacy tools → [[02-profile-account]] (PROF-04), SET-02 translation → [[11-tafsir-translation]].
- Every setting label/value is a string resource in **ar + en**; numbers/dates locale-aware (LOC-04).

## Capabilities
| ID | Capability | Priority | Must-know rule |
|----|-----------|----------|----------------|
| SET-01 | Change App Language | MUST | Arabic (RTL) + English (LTR); reload core UI instantly via per-tree locale. |
| SET-02 | Select Translation Language | SHOULD | Sets translation across Mushaf, Search, and Session views. |
| SET-03 | Enable Dark Mode | SHOULD | "Emerald Calm" dark palette; keep Tashkil highly legible (≥4.5:1). |
| SET-04 | Accessibility Settings | MUST | Screen-reader support, audio descriptions, high-contrast styling. |
| SET-05 | Audio Settings | SHOULD | Background-streaming permission, cache settings, default volume. |
| SET-06 | Notification Settings | SHOULD | Links to Epic 15 configuration; no duplicate logic. |
| SET-07 | Privacy Settings | MUST | Links to data download, recording deletion, audio-training opt-in. |
| SET-08 | Account Settings | SHOULD | Email, sync preferences, linked auth providers. |
| SET-09 | Reset Settings | COULD | Clears config only — never deletes progress or search history. |
| SET-10 | Font Size Scaling | SHOULD | Scales text dynamically without breaking Mushaf alignment. |

## Design system (`:designsystem`)
- Settings list: `SectionedCard` for grouped sections; `SettingsActionCard` for tappable rows (navigate to sub-screens); `ExpandableSection` / `ExpandableAccentCard` for inline-expanding option groups.
- Toggles (dark mode, audio, accessibility switches): **needs new :designsystem component: SettingToggleRow** (switch not in catalog).
- One-of-N selectors (language, translation language, font-size step): `DayTabRow` works as a small one-of-N pill selector; larger lists via `AppBottomSheet` *(in module source; README-lagged)* selection sheet.
- Font-size control: a discrete stepper/slider — **needs new :designsystem component: FontSizeSlider** (no slider in catalog); preview text must be a live sample.
- Inputs: `TextField` for account email; `PrimaryButton` (Save), `SecondaryButton` (Cancel), `IconButton` for row chevrons.
- Reset confirmation (SET-09): **needs new :designsystem component: ConfirmationDialog**; result via `StatusOverlay` (`Loading`/`Success`/`Error`).
- Top bar `BackTitleTopBar`; error/retry on network-backed account actions via `NetworkErrorScreen(onRetry = …)`. Read all tokens through `Theme.*`.

## Data, stack & offline
- **Room** is the source of truth for all preferences (language, theme, font size, translation source, audio, accessibility, notification toggles). Reads/writes are fully offline (AVL-01).
- **Ktor** only for account settings (SET-08) and privacy server actions (SET-07) — surface offline messaging (AVL-02); queue non-critical preference changes for sync ([[17-offline-sync]]).
- Theme/locale/font-scale preferences are read at the app root and passed into `SpTheme(isDarkTheme, locale, fontFamily)`; changes recompose the tree without an activity restart.
- Do not persist secrets/tokens here; auth provider links use secure storage (SEC-01).

## Applicable NFRs
- NFR-LOC LOC-01 (ar+en), LOC-02 (RTL mirroring), LOC-03 (licensed translations for SET-02), LOC-04 (locale formatting).
- NFR-A11Y A11Y-02 (screen-reader labels), A11Y-03 (font scaling without breaking Mushaf), A11Y-04 (Emerald Calm Tashkil contrast).
- NFR-AVL AVL-01 (offline settings), AVL-02 (graceful degradation of account/privacy actions).
- NFR-SEC SEC-01 (secure storage for linked auth), SEC-02 (store compliance).
- NFR-PRV PRV-01/PRV-02 (privacy settings link to retention + erasure tools).

## Related features
- [[15-notifications]] — SET-06 target.
- [[02-profile-account]] — SET-07 privacy tools, SET-08 account overlap.
- [[11-tafsir-translation]] — SET-02 translation-source selection.
- [[03-mushaf-reading]] — font scaling & QUL typesetting integrity for SET-10.
- [[17-offline-sync]] — preference queueing and sync.
