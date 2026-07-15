# 10. Bookmarks & Collections — BMK

> **Product spec:** [Al-Mahir-SDD.md](../Al-Mahir-SDD.md) → "Epic 10: Bookmarks & Collections". That file is the canonical product requirement; this doc is the agent build brief.
> **Rules:** [AGENTS.md](../../AGENTS.md) — architecture, modules, MVI, stack, localization (do not restate them here).

**Priority:** SHOULD · **Primary modules:** `:presentation` / `:data` / `:domain`

## Purpose
Saved-passage management with custom collections, notes, and full offline accessibility.

## Must-know constraints
- **Offline-first:** bookmarks, notes, and collection metadata are cached in Room and fully readable/editable offline (BMK-07); changes queue locally and sync when connected.
- **Identifier, not text:** a bookmark stores the target verse identifier (surah + ayah), not a copy of the glyph text — resolve display text from the Mushaf data layer.
- **Toggle semantics:** BMK-01 add and BMK-02 remove are the same control toggled; removing prompts confirmation before deletion.
- **Guest restriction:** guest mode has **no bookmarks** (per AUTH-04) — gate the feature behind an authenticated session.
- **Notes bound:** custom notes capped at 500 characters (BMK-06).
- **Sync:** local edits push to the unified user-data service (ARC-01) on reconnect; resolve conflicts on sync.

## Capabilities
| ID | Capability | Priority | Must-know rule |
|----|-----------|----------|----------------|
| BMK-01 | Bookmark Ayah | MUST | Toggle saves the target verse identifier to the local DB. |
| BMK-02 | Remove Bookmark | MUST | Tapping active bookmark icon prompts deletion, removes from lists. |
| BMK-03 | View All Bookmarks | MUST | Centralized index, chronological or grouped by Surah. |
| BMK-04 | Organize Collections | SHOULD | Create / rename / delete named folders holding selected bookmarks. |
| BMK-05 | Search Bookmarks | COULD | Text search over bookmarked verses index and user notes. |
| BMK-06 | Add Notes to Bookmarks | COULD | Free-text note per bookmark, max 500 characters. |
| BMK-07 | Offline Bookmark Access | SHOULD | Bookmark text, notes, and collection metadata cached for offline use. |

## Design system (`:designsystem`)
- Bookmark toggle (from Mushaf tap-ayah sheet) → `IconButton`; delete confirmation → `AppBottomSheet` *(in module source; README-lagged)* or `StatusOverlay`.
- Bookmarks index / collection cards → `SectionedCard`; collapsible per-Surah grouping → `ExpandableSection` / `ExpandableAccentCard`.
- Collection mode switch (chronological vs by Surah) → `TabSelector`/`DayTabRow`.
- Note entry (BMK-06) → `TextField` (multi-line, 500-char). Search (BMK-05) → `SearchBar` *(in module source; README-lagged)*.
- Empty state → `EmptyDataScreen` ("no bookmarks yet" + CTA); search-empty → `EmptySearchScreen`; top bar → `BackTitleTopBar`.
- All colors/dp/sp via `Theme.*`; RTL-aware layout; strings in en + ar.

## Data, stack & offline
- Room (local) is primary store for bookmarks, collections, and notes — fully offline (OFF-02 / AVL-01).
- Offline queueing (OFF-05) of add/remove/edit; automatic sync to user-data service via Ktor when online (OFF-06, ARC-01).
- Coroutines/Flow expose bookmark lists as `Flow` to the ViewModel; domain holds pure bookmark/collection models.

## Applicable NFRs
- NFR-AVL AVL-01 (offline access), AVL-03 (offline queueing + auto-sync).
- NFR-ARC ARC-01 (unified user-data microservice owns bookmarks).
- NFR-LOC LOC-01/LOC-02 (Arabic + English, RTL).
- NFR-PRV PRV-03 (data encrypted at rest / in transit for synced bookmarks).

## Related features
- [[03-mushaf-reading]] — bookmark toggle originates in the MUS-06 tap-ayah sheet; view opens the verse in the Mushaf.
- [[04-search-index]] — bookmark search reuses local text-search patterns.
- [[11-tafsir-translation]] — notes complement translations/tafsir on a saved verse.
