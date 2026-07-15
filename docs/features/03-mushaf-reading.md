# 3. Mushaf - Reading — MUS

> **Product spec:** [Al-Mahir-SDD.md](../Al-Mahir-SDD.md) → "Epic 3: Mushaf - Reading". That file is the canonical product requirement; this doc is the agent build brief.
> **Rules:** [AGENTS.md](../../AGENTS.md) — architecture, modules, MVI, stack, localization (do not restate them here).

**Priority:** MUST · **Primary modules:** `:mushaf:presentation` / `:mushaf:data` / `:mushaf:domain`

## Purpose
High-fidelity, authentic Qur'an presentation engine with vector-perfect text scaling — the app's default landing surface for authenticated users.

## Must-know constraints
- **Data:** KFGQPC V4 layout SQLite (page/line/word/glyph mapping) mounted via Room `createFromAsset()` from `assets/`. `domain` stays pure — never leak Room entities to UI.
- **Fonts:** 604 per-page glyph `.ttf` files (`p{page}.ttf`, tajweed variant `v4_tajweed/p{page}.ttf`). One Unicode char = one drawn word. **Lazy-load and cache by active page — never load all fonts at startup.** Graceful fallback on a missing file. Qur'an text MUST use official QUL glyph fonts, never the brand font.
- **Rendering isolation:** highlighting must recompose at the individual word/line level (not the page) to hold 60fps under a future audio/STT stream. Render each `MushafWord` as its own `Text`; do not fold a whole page into one `AnnotatedString`.
- **Pager:** RTL `HorizontalPager`, 604 pages, **page 1 on the right**; keep only 1–2 off-screen pages alive to avoid font-memory OOM.
- **State shape:** `MushafUiState(currentPage, lines, isTajweedEnabled, highlightedWordId, isLoading)`; flat Room rows grouped into `MushafPage → List<MushafLine> → List<MushafWord>`.
- **Budgets:** Mushaf vector page render < 400ms (PRF-02); font scaling must not break page alignment (SET-10 / A11Y-03).

> **Deep spec:** [Mushaf_ssd.md](../Mushaf_ssd.md) — full 5-phase build (data layer, MVI, rendering engine, pager, highlighting API).

## Capabilities
| ID | Capability | Priority | Must-know rule |
|----|-----------|----------|----------------|
| MUS-01 | Launch to Mushaf | MUST | Default landing page when authenticated; render pipeline under 400ms. |
| MUS-02 | Continue Last Reading | MUST | Persist active page + surah index + ayah offset locally; sync to cloud immediately. |
| MUS-03 | Browse Surahs | MUST | Structured index navigable by Surah name, Juz', or page. |
| MUS-04 | Display Mushaf Authentically | MUST | Match printed Mushaf incl. right/left page distinction; official QUL vector fonts only — brand fonts strictly forbidden for Qur'an text. |
| MUS-05 | Resume After Interruptions | SHOULD | Auto-save reading state when backgrounded by calls/push so place is never lost. |
| MUS-06 | Tap-Ayah Bottom Sheet | MUST | Tapping an ayah opens a modal bottom sheet (with micro-interactions) for translation, bookmark, copy, share, tafsir. |

## Design system (`:designsystem`)
- Tap-ayah action menu (MUS-06) → `AppBottomSheet` *(exists in module source; README-lagged)*; action rows can reuse `SettingsActionCard` / `IconButton`.
- Surah/Juz'/page index (MUS-03) → list built from `SectionedCard` + `TabSelector`/`DayTabRow` for the Surah/Juz'/Page mode switch.
- Loading page → `Shimmer` *(exists in module source under `components/loading/`; README-lagged)* or `StatusOverlay(Loading)`.
- Empty/error → `EmptyDataScreen`; top bar → `BackTitleTopBar`.
- Read all colors/dp/sp via `Theme.*`. Qur'an glyph text uses QUL per-page fonts, NOT `arabicFontFamily` from the design system.

## Data, stack & offline
- Room (local, `createFromAsset`) is the sole source for page/line/word data — fully offline-capable (OFF-01 core requirement). No remote dependency for reading.
- Last-reading position stored in local Room state, mirrored to cloud when connected (MUS-02); offline edits queued.
- Coroutines/Flow: repository exposes `Flow<List<MushafWordEntity>>` → mapped to domain `MushafPage`.

## Applicable NFRs
- NFR-PRF PRF-02 (startup < 2s, Mushaf page < 400ms), PRF-04 (crash-free ≥ 99.9%).
- NFR-ARC ARC-05 (QUL typesetting integrity — never brand fonts for Qur'anic text).
- NFR-LOC LOC-02 (RTL mirroring as native mode).
- NFR-A11Y A11Y-03 (font scaling without breaking layout), A11Y-05 (two-page tablet spread).
- NFR-AVL AVL-01 (offline core reading).

## Related features
- [[04-search-index]] — SRCH-08 opens results directly into the Mushaf with a flash highlight.
- [[10-bookmarks-collections]] — bookmark toggle from the tap-ayah sheet.
- [[11-tafsir-translation]] — translation/tafsir surfaced in the tap-ayah sheet.
