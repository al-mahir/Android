# Phase 1 Data Model: Mushaf Reading

Two data surfaces: (1) the **read-only layout DB** (`mushaf_v4_layout.db`) mapped by Room in
`:mushaf:data`, and (2) **reader preferences** in DataStore. Both are mapped to
framework-free **domain models** consumed by `:mushaf:presentation`. No Room entity or
DataStore type crosses into the UI (Constitution I).

---

## 1. Persistence layer (`:mushaf:data`)

### 1.1 Room entity — `MushafLineEntity` (table `pages`)

Maps 1:1 to the shipped schema (see research R1). Read-only.

| Column | Type | Notes |
|--------|------|-------|
| `page_number` | INTEGER | 1..604 |
| `line_number` | INTEGER | 1..15 within a page |
| `line_type` | TEXT | `ayah` \| `basmallah` \| `surah_name` |
| `is_centered` | INTEGER | 0/1 → Boolean |
| `first_word_id` | INTEGER (nullable) | inclusive global word id for `ayah` lines; empty/NULL otherwise |
| `last_word_id` | INTEGER (nullable) | inclusive global word id for `ayah` lines; empty/NULL otherwise |
| `surah_number` | INTEGER (nullable) | chapter for `surah_name` lines; empty/NULL otherwise |

> Empty-string cells in the asset (`''`) are read as NULL/absent by the mapper. No primary
> key is declared in the asset; the DAO relies on natural ordering, and a `@DatabaseView`
> or `@Query` supplies deterministic ordering. Room's `@Entity` may declare a composite
> key `(page_number, line_number)` as a logical identity even though the asset has none.

### 1.2 Room entity — `MushafInfoEntity` (table `info`)

| Column | Type | Value |
|--------|------|-------|
| `name` | TEXT | "QPC v4 tajweed" |
| `number_of_pages` | INTEGER | 604 |
| `lines_per_page` | INTEGER | 15 |
| `font_name` | TEXT | "v4-tajweed" |

Used for validation/metadata (e.g., assert 604 pages); not required for rendering.

### 1.3 DataStore — `ReaderPreferences` keys

| Key | Type | Default | Source req |
|-----|------|---------|-----------|
| `tajweed_enabled` | Boolean | `true` | FR-007, FR-007a |
| `last_page` | Int | `1` | FR-006a |

---

## 2. Domain models (`:mushaf:domain`, pure Kotlin, immutable)

```text
MushafPage
├─ pageNumber: Int              # 1..604
└─ lines: List<MushafLine>      # ordered by lineNumber ASC

MushafLine
├─ lineNumber: Int              # 1..15
├─ type: LineType              # AYAH | BASMALLAH | SURAH_NAME
├─ isCentered: Boolean
├─ surahNumber: Int?           # present when type == SURAH_NAME
└─ words: List<MushafWord>     # AYAH lines: expanded from id range; else empty

MushafWord
├─ id: String                  # stable unique key, e.g. "p{page}:l{line}:w{wordId}"
├─ wordId: Int                 # global id (first_word_id..last_word_id)
├─ pageNumber: Int
├─ lineNumber: Int
├─ positionInLine: Int         # 0-based order within the line (RTL reading order)
└─ glyphCode: Int              # per-page PUA codepoint for the font (research R2)

enum LineType { AYAH, BASMALLAH, SURAH_NAME }

ReadingMode { TAJWEED, PLAIN }      # derived from tajweedEnabled

ReaderPreferences
├─ tajweedEnabled: Boolean
└─ lastPage: Int
```

### 2.1 Derivation rules (mapper, `:mushaf:data`)

- **Page assembly**: group `MushafLineEntity` rows by `page_number`, order lines by
  `line_number` ASC → `MushafPage.lines`.
- **Word expansion** (`AYAH` lines): for `wordId in first_word_id..last_word_id`, emit one
  `MushafWord` with `positionInLine = wordId - first_word_id`, and
  `glyphCode = GlyphCodeResolver.resolve(page, wordId)` (research R2). `BASMALLAH` and
  `SURAH_NAME` lines carry a single line-glyph and an **empty** `words` list (rendered as a
  whole-line glyph, not individually highlightable).
- **`id`**: `"p${pageNumber}:l${lineNumber}:w${wordId}"` — globally unique, stable across
  reloads; used as the Compose key and `highlightedWordId` (FR-010/FR-011).
- **`surahNumber`**: populated only for `SURAH_NAME` lines.

### 2.2 Validation rules

| Rule | Enforced where | Requirement |
|------|----------------|-------------|
| `pageNumber ∈ 1..604`; navigation clamped to bounds | use case / pager | FR-015, SC-003 |
| Lines ordered by `lineNumber` ASC; words by `positionInLine` ASC | mapper/DAO | FR-001, SC-001 |
| A page has 8–15 lines; renders even when < 15 | UI layout | Edge case (short pages) |
| `AYAH` line has non-null `first/last_word_id`; `SURAH_NAME` has `surahNumber` | mapper | R1 correctness |
| `tajweedEnabled` defaults `true`; `lastPage` defaults `1`, clamped to 1..604 | prefs repo | FR-006a, FR-007a |

### 2.3 State transitions (reading session)

```text
App launch
  → load ReaderPreferences (tajweedEnabled, lastPage)
  → open pager at lastPage (resume; first-time = page 1)                 [FR-006a]

Swipe settles on page P
  → LoadPage(P) → fetch MushafPage(P) → render                          [FR-004, FR-006]
  → persist lastPage = P                                                 [FR-006a]

Toggle Tajweed
  → flip tajweedEnabled → swap font dir, re-render same layout           [FR-008, FR-009]
  → persist tajweedEnabled                                              [FR-007a]

Highlight tick (from HighlightDriver)
  → set highlightedWordId = wordId → recompose only that word           [FR-010, FR-011]
```

### 2.4 Relationships

```text
info (1) ──describes──> pages (9,046 rows)
MushafPage 1 ──has──> 8..15 MushafLine
MushafLine  1 ──(AYAH) expands to──> 0..N MushafWord
MushafWord  N ──rendered by──> per-page font p{page}.ttf glyph[glyphCode]
ReaderPreferences (1 per device) ──seeds──> MushafUiState
```
