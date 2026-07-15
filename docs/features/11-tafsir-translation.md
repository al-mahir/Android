# 11. Tafsir & Translation — TFS

> **Product spec:** [Al-Mahir-SDD.md](../Al-Mahir-SDD.md) → "Epic 11: Tafsir & Translation". That file is the canonical product requirement; this doc is the agent build brief.
> **Rules:** [AGENTS.md](../../AGENTS.md) — architecture, modules, MVI, stack, localization (do not restate them here).

**Priority:** SHOULD · **Primary modules:** `:presentation` / `:data` / `:domain`

## Purpose
Multi-source verified translations and scholarly interpretations (Tafsir), viewable inline per verse and available offline.

## Must-know constraints
- **Licensed datasets only:** use legally licensed, verified translation/tafsir sources (LOC-03); multiple sources per language must be selectable.
- **Offline packages:** translation/tafsir datasets ship as downloadable packages to local storage (TFS-06); once downloaded they read fully offline. Prompt the user to download rather than bundling everything.
- **Inline surfacing:** short translation/tafsir renders inline under the selected verse inside the tap-ayah bottom sheet (drives Epic 3 MUS-06); detailed tafsir loads on demand in a scrollable view.
- **Independent selection:** translation language (TFS-02) and tafsir source (TFS-05) are separate user selections; keep them decoupled in state/settings.
- **Translation text is not Qur'an glyph text:** translation/tafsir bodies use the design-system typography (`Theme.typography`), never the QUL glyph fonts — those are reserved for Arabic Qur'an text only.

## Capabilities
| ID | Capability | Priority | Must-know rule |
|----|-----------|----------|----------------|
| TFS-01 | View Translation | MUST | Translation rendered inline under the verse in bottom sheet / split view. |
| TFS-02 | Switch Translation Language | SHOULD | Multiple verified sets (e.g. Arabic, English, Urdu), user-switchable. |
| TFS-03 | Read Short Tafsir | SHOULD | Summary interpretation (e.g. Tafsir al-Jalalayn) in the detail sheet. |
| TFS-04 | Read Detailed Tafsir | SHOULD | Full exegesis (e.g. Ibn Kathir) loaded on demand, scrollable. |
| TFS-05 | Select Tafsir Source | COULD | Settings menu to swap between available scholarly sources. |
| TFS-06 | Offline Tafsir & Translation | SHOULD | Download localized translation/tafsir packages to local storage. |

## Design system (`:designsystem`)
- Inline translation / short tafsir (TFS-01/03) → `AppBottomSheet` *(in module source; README-lagged)* hosting `SectionedCard`; short vs detailed toggle → `ExpandableSection`.
- Detailed tafsir (TFS-04) → scrollable body in `SectionedCard` / `ExpandableAccentCard`.
- Source & language pickers (TFS-02/05) → `SettingsActionCard` rows or `TabSelector`.
- Download packages (TFS-06) → `PrimaryButton` + `StatusOverlay(Loading/Success/Error)` for download progress; per-source list via `SectionedCard`.
- Failed download → `NetworkErrorScreen(onRetry)`; nothing downloaded yet → `EmptyDataScreen`; top bar → `BackTitleTopBar`.
- Body text uses `Theme.typography` (design-system font), RTL-aware for Arabic; en + ar UI strings. Qur'an verse itself stays on QUL glyph fonts.

## Data, stack & offline
- Ktor remote (`data`) lists and downloads translation/tafsir packages; Room (local) stores downloaded datasets and the active source/language selections.
- Once a package is downloaded, all reads are offline (TFS-06 / AVL-01); detailed tafsir (TFS-04) loads from local store on demand.
- Domain models hold verse-keyed translation/tafsir entries mapped from DTO/Room, kept pure of framework types.

## Applicable NFRs
- NFR-LOC LOC-01/LOC-02 (Arabic + English UI, RTL), LOC-03 (licensed/verified translation datasets).
- NFR-AVL AVL-01 (offline reading of downloaded datasets).
- NFR-PRF PRF-04 (stable on-demand loading of large tafsir text).
- NFR-A11Y A11Y-03 (translation/tafsir text scales with system font).

## Related features
- [[03-mushaf-reading]] — translation/tafsir open from the MUS-06 tap-ayah sheet.
- [[04-search-index]] — search filtering by Translation/Tafsir source (SRCH-07).
- [[10-bookmarks-collections]] — bookmark notes sit alongside translations on a saved verse.
