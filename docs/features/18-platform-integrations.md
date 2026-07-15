# 18. Platform Integrations — INT

> **Product spec:** [Al-Mahir-SDD.md](../Al-Mahir-SDD.md) → "Epic 18: Platform Integrations". That file is the canonical product requirement; this doc is the agent build brief.
> **Rules:** [AGENTS.md](../../AGENTS.md) — architecture, modules, MVI, stack, localization (do not restate them here).

**Priority:** COULD · **Primary modules:** :app, :presentation, :domain, :data

## Purpose
System-level OS surfaces that keep users engaged and simplify sharing: home-screen widgets, deep/universal links, voice shortcuts, and the native share sheet.

## Must-know constraints
- **Deep links (INT-02):** scheme `almahir://surah/{id}/ayah/{num}` must jump to the exact verse in the Mushaf and **gracefully fall back to a browser page if the app is absent** (App Links + web fallback). Wire routes through Navigation 3 in `:app`; validate `{id}`/`{num}` against structural bounds before navigating.
- **Widgets (INT-01):** Android `AppWidgetProvider` (prefer Glance/Compose) showing daily verse, streak, or quick-resume; **updated via local schedules** (WorkManager), must render with cached/offline data — no network on the widget path.
- **Native share sheet (INT-04):** use Android `ACTION_SEND` / `Intent.createChooser`; share clean localized text and link image exports (exam results, summaries) directly — do not build a custom share UI. Coordinate export image generation with the exam/history features.
- **Voice shortcuts (INT-03):** Android App Actions / shortcuts to trigger daily recitation hands-free; register shortcuts, keep labels localized.
- Widget/shortcut/share strings are resources in **ar + en**, RTL-aware; verse numbers/dates use locale-aware formatting (LOC-04, incl. Hijri where shown). Never leak `data`/DTO types into widget or share code — go through domain use cases.
- **INT-05 (Smartwatch) is LATER / post-MVP** — do not build now; leave a clean seam.

## Capabilities
| ID | Capability | Priority | Must-know rule |
|----|-----------|----------|----------------|
| INT-01 | Home-Screen Widgets | COULD | `AppWidgetProvider`/Glance; daily verse, streak, resume; updated daily via local schedule, offline-safe. |
| INT-02 | Deep / Universal Linking | SHOULD | `almahir://surah/{id}/ayah/{num}`; validate bounds; browser fallback if app missing. |
| INT-03 | Voice Shortcuts | COULD | Android App Actions to trigger daily recitation hands-free. |
| INT-04 | Native Share Sheet | SHOULD | System `ACTION_SEND`; share clean localized text + linked image exports. |
| INT-05 | Smartwatch Companions | LATER | Post-MVP Wear OS extension — do not build in MVP. |

## Design system (`:designsystem`)
- Widgets render **outside** the Compose theme tree (RemoteViews/Glance) — they cannot consume `Theme.*` directly; mirror token values (colors, spacing) into the widget layout and keep them in sync manually. **needs new :designsystem component: WidgetTokens export** (or a documented token snapshot for Glance).
- In-app share affordances: `IconButton` / `SecondaryButton` ("Share") on exam/summary screens invoke the system chooser; `PrimaryButton` for primary export actions.
- Deep-link landing highlight reuses the Mushaf flash-highlight interaction (see [[03-mushaf-reading]] / SRCH-08) — no new component.
- Share/export progress via `StatusOverlay` (`Loading`/`Success`/`Error`). Read all in-app tokens via `Theme.*`, RTL-aware.

## Data, stack & offline
- **:app** owns Navigation 3 deep-link route declarations and the `intent-filter` (App Links, autoVerify) in the manifest; a domain use case resolves surah/ayah → page and validates bounds.
- **Widgets:** `AppWidgetProvider` + Glance; periodic `WorkManager` update pulls the daily verse / streak / last-read position from Room (offline-first, [[17-offline-sync]]). No Ktor on the widget refresh path.
- **Share/export:** image/PDF export generated in the feature module; share via `FileProvider` + `Intent.createChooser` — respect scoped storage.
- **Voice shortcuts:** register static/dynamic shortcuts and App Actions mapping to in-app intents/routes.
- Keep all OS-integration glue in `:app` / `:presentation`; business decisions stay in `domain` use cases (no `data` leakage).

## Applicable NFRs
- NFR-LOC LOC-01 (ar+en), LOC-02 (RTL for widgets/share text), LOC-04 (locale-aware verse/date formatting).
- NFR-A11Y A11Y-02 (screen-reader labels on widgets and share controls).
- NFR-AVL AVL-01 (widgets/quick-resume work offline from cache).
- NFR-SEC SEC-02 (store compliance for links, share, and background updates).
- NFR-PRF PRF-02 (deep-link launch respects startup budget; land on the verse fast).

## Related features
- [[03-mushaf-reading]] — deep-link target, resume position, flash-highlight.
- [[08-ikhtibar]] / exam results — image/PDF export feeding INT-04 share.
- [[13-progress-gamification]] — streak/daily-goal data shown in widgets.
- [[17-offline-sync]] — cached data powering offline widget updates.
- [[15-notifications]] — notification taps share deep-link routing.
