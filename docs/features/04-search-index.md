# 4. Search & Index — SRCH

> **Product spec:** [Al-Mahir-SDD.md](../Al-Mahir-SDD.md) → "Epic 4: Search & Index". That file is the canonical product requirement; this doc is the agent build brief.
> **Rules:** [AGENTS.md](../../AGENTS.md) — architecture, modules, MVI, stack, localization (do not restate them here).

**Priority:** SHOULD · **Primary modules:** `:presentation` / `:data` / `:domain`

## Purpose
Multi-modal lookup engine: exact textual search, structural (Surah/Ayah) navigation, metadata filtering, and backend semantic query matching.

## Must-know constraints
- **Local FTS5:** keyword text search runs on a local SQLite FTS5 index (Room) for instant, offline queries; matching is **diacritic-insensitive** (normalize tashkil on both index and query).
- **Structural parsing:** accept `Surah:Ayah` (e.g. `2:255`); validate against real structural bounds before navigating — reject out-of-range verses.
- **Fuzzy + bilingual:** Surah-name search must match Arabic and English transliterations, exact and fuzzy.
- **Debounce:** typeahead debounced 150ms before hitting the local index to avoid render stutter.
- **Semantic is online-only:** SRCH-04 goes through backend vector embeddings (Ktor) — degrade gracefully offline (disable, don't crash).
- **Result open:** SRCH-08 jumps to the target Mushaf page, triggers a flash highlight on the ayah, and closes the search sheet — this is the contract with Epic 3.
- **Recent searches:** keep last 10 locally; clear on request and at guest-session close.

## Capabilities
| ID | Capability | Priority | Must-know rule |
|----|-----------|----------|----------------|
| SRCH-01 | Search by Surah | MUST | Exact + fuzzy matching across Arabic and English transliterations. |
| SRCH-02 | Search by Surah & Ayah | MUST | Accept `Surah:Ayah` format; validate against structural bounds. |
| SRCH-03 | Keyword Text Search | MUST | Local SQLite FTS5, diacritic-insensitive, instantaneous. |
| SRCH-04 | Semantic Search | SHOULD | Backend vector-embedding lookup for abstract concepts (online-only). |
| SRCH-05 | Recent Searches | COULD | Store last 10 successful queries locally; clear on request / guest close. |
| SRCH-06 | Typeahead Suggestions | COULD | Debounce input 150ms before querying local index. |
| SRCH-07 | Search Filtering | SHOULD | Filter by Surah, Juz', Meccan/Medinan, Translation, Tafsir source. |
| SRCH-08 | Open Search Result | MUST | Jump to target page, flash-highlight the ayah, close search sheet. |
| SRCH-09 | Empty-State / Zero Results | SHOULD | "No results" feedback + suggested keywords, recents, or browse CTA. |
| SRCH-10 | Voice Search | LATER | Post-MVP speech-to-text semantic pipeline — out of scope for MVP. |

## Design system (`:designsystem`)
- Search input → `SearchBar` / `SearchOverlapHeader` *(exist in module source under `components/search/`; README-lagged)*.
- Result list / recents → `SectionedCard`; filter chips (SRCH-07) → `TabSelector`/`DayTabRow` pill row.
- Zero-results (SRCH-09) → `EmptySearchScreen` (has optional action CTA, e.g. "Clear search" / "Browse").
- Network failure on semantic search → `NetworkErrorScreen(onRetry)`; in-flight → `Shimmer` *(in module source; README-lagged)* or `StatusOverlay(Loading)`.
- Top bar → `BackTitleTopBar`. All tokens via `Theme.*`; RTL-aware start/end layout for Arabic query rendering.

## Data, stack & offline
- Local Room FTS5 index (offline-capable) powers SRCH-01/02/03/05/06/07; recents persisted in Room.
- Ktor remote (`data`) powers semantic search SRCH-04 only — gated on connectivity, degrades gracefully offline.
- Debounce/typeahead via Coroutines/Flow `debounce(150)` in the ViewModel-driven use case.

## Applicable NFRs
- NFR-PRF PRF-02 (instantaneous local queries).
- NFR-LOC LOC-01/LOC-02 (Arabic + English search, RTL), LOC-04 (locale-aware numerals in `Surah:Ayah`).
- NFR-AVL AVL-01 (local search offline), AVL-02 (semantic search degrades gracefully).
- NFR-ARC ARC-04 (vector-embedding service backs semantic search).

## Related features
- [[03-mushaf-reading]] — SRCH-08 opens directly into the Mushaf with a flash highlight (page + ayah contract).
- [[11-tafsir-translation]] — filtering by Translation/Tafsir source (SRCH-07).
