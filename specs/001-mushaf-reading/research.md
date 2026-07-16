# Phase 0 Research: Mushaf Reading

All Technical Context unknowns and key technology choices resolved below. Each item follows:
**Decision → Rationale → Alternatives considered**. Items marked **VERIFY** carry a
concrete implementation-time check because tooling to confirm them was unavailable offline
during planning.

---

## R1. Actual layout database schema (SSD assumption is wrong)

**Decision**: Model the DB as it actually is — a **line-based** layout, not the word-based
`MushafWordEntity` the SSD assumed. The packaged `mushaf_v4_layout.db` has two tables:

- `pages(page_number INT, line_number INT, line_type TEXT, is_centered INT, first_word_id INT, last_word_id INT, surah_number INT)` — 9,046 rows, one per printed line.
- `info(name TEXT, number_of_pages INT, lines_per_page INT, font_name TEXT)` — one row: `('QPC v4 tajweed', 604, 15, 'v4-tajweed')`.

`line_type ∈ {ayah, basmallah, surah_name}` (8,820 / 112 / 114 rows). For `ayah` lines,
`first_word_id..last_word_id` is an inclusive global word-id range (1..83,668). For
`surah_name` lines, `surah_number` names the chapter header and word-id fields are empty
strings. For `basmallah` lines, both are empty. `is_centered` (0/1) drives horizontal
centering of the line. Pages have 8–15 lines.

**Rationale**: Ground-truth inspection of the shipped asset supersedes the SSD's assumed
schema. The domain layer expands each `ayah` line's id range into individual `MushafWord`s
so the UI can render and highlight one word at a time (satisfying FR-010/FR-011), while
`surah_name`/`basmallah` lines render as special single-glyph line types.

**Alternatives considered**: (a) Trust the SSD schema (`page_number, line_number, surah_id,
ayah_id, word_position, glyph_code`) — rejected: no such table exists; would not compile
against the asset. (b) Repackage the DB into a word-per-row table — rejected: unnecessary
asset surgery; the line table already yields words by range expansion.

**Implementation note (resolved during build)**: The `pages`/`info` tables have no primary
key and nullable columns, so Room's prepackaged-DB schema verification rejects the asset at
open time (runtime "invalid schema" error → the "Something went wrong" state). Resolved by
reading the read-only asset with **direct SQLite** (`MushafAssetDataSource`, opened
`OPEN_READONLY`), which also normalises the asset's empty-string numeric cells to null. See
plan.md Complexity Tracking. Extensive logcat logging (tag `Mushaf`) was added across the
data source, repository, and ViewModel to make any future data failure diagnosable.

---

## R2. Word → glyph codepoint mapping (rendering key)  **VERIFIED**

**Decision**: Render each word as a **single character** whose codepoint is looked up from
the per-page QPC v4 font. A word's glyph is addressed by a codepoint derived from the word's
position **within its page**: `codepoint = 0xFC41 + (wordId - pageFirstWordId)`, where
`pageFirstWordId` is the smallest `first_word_id` among the page's ayah lines.

**VERIFIED against the shipped fonts' `cmap`** (`fontTools.ttLib` `getBestCmap()`): the
word-glyph block begins at **U+FC41** on every page — page 1's 36 words map exactly to
U+FC41..U+FC64, and the formula yields an in-font glyph for every word across sampled pages
1, 2, 3, 10, 50, 114, 300, 450, 604 (0 misses). The initial guess (base `0xE000`) was wrong,
which rendered words via a system fallback font (garbled/CJK-looking glyphs); corrected to
`0xFC41` in `GlyphCodeResolver` (the single point of change).

**Surah-name / basmallah glyphs are NOT in the packaged page fonts** (verified: page 1's
Fatihah header and page 604's three surah headers have zero non-word glyphs; pages with no
headers still have "extra" glyphs, so the extras are ayah-marker artifacts, not headers).
Full QPC setups ship a separate surah-header/basmallah font, which is not in our assets.
Resolution: `surah_name` lines render the surah name (from a 114-entry table) and
`basmallah` lines render the Uthmani basmallah as **Unicode Arabic text in the device font**
— a faithful, non-calligraphic approximation so every page shows its header + basmallah. A
dedicated header font asset would be needed for exact calligraphic frames.

**Page layout** (matching printed Mushaf, best practice): each page renders
`LINES_PER_PAGE` (15) equal-height vertical slots (full pages fill the page; short opening
pages pack from the top with identical spacing), and every line is a single non-wrapping row
at one page-wide font size measured (via `TextMeasurer`) so a full justified line fills the
column width. This replaced `Arrangement.SpaceEvenly` + `FlowRow`, which caused uneven
vertical gaps and wrapped long ayahs onto a second row.

**Rationale**: QPC v4 ("QCF v4") is a glyph-based, one-page-per-font system where each word
is a ligature/glyph — exactly matching the SSD's "one Unicode character = one drawn word"
constraint. Deriving the codepoint from page-relative word index is the documented QUL/QPC
approach and keeps rendering O(1) per word.

**Alternatives considered**: (a) Codepoint = global `word_id` directly — rejected: exceeds
typical per-page PUA windows and duplicates across pages that reuse the same font glyph
slots. (b) Bake an explicit `glyph_code` column — rejected: not present in the asset;
derivation is deterministic. (c) Text shaping of Uthmani Unicode text — rejected: defeats
the entire glyph-font design and cannot reproduce exact print layout or COLR Tajweed.

---

## R3. Dual-font Tajweed strategy (COLR baked, no Compose color override)

**Decision**: Keep the state flag `isTajweedEnabled`; resolve the font directory per page:
`fonts/${if (tajweed) "tajweed" else "standard"}/p${page}.ttf`. Load with Compose
`Font(path, context.assets)` into a `FontFamily`, keyed by the resolved path via `remember`.
Never set text color for Tajweed — the `standard` fonts are monochromatic, the `tajweed`
fonts carry `COLR`/`CPAL` color; both are geometrically identical so swapping causes **no
layout shift** (FR-008/FR-009, SC-004).

**Rationale**: The SSD's hard constraint — COLR colors cannot be stripped via Compose
modifiers — forces a font-swap approach. Geometric identity of the two font sets guarantees
zero reflow on toggle.

**Alternatives considered**: (a) Single font + `SpanStyle` recoloring — rejected: cannot
recolor COLR glyphs and cannot synthesize Tajweed rules. (b) Two overlaid Text layers —
rejected: wasteful, risks subpixel misalignment.

---

## R4. Font memory management (1,208 files, no OOM)  **VERIFY**

**Decision**: A `PageFontProvider` owns an LRU cache of `FontFamily` keyed by
`(mode, page)` with a small capacity (e.g., 4 = current ±1 for each of two modes).
`HorizontalPager` uses `beyondViewportPageCount = 1`. When a page leaves the cache window,
drop its `FontFamily` reference so it is garbage-collected. Fonts load off the main thread
(`Dispatchers.IO`) and the page shows the loading state (FR-013) until ready.

**VERIFY**: Measure heap during a rapid 50-page swipe (SC-006) with Android Studio Profiler;
confirm stable memory and no OOM. Tune cache capacity if RSS grows.

**Rationale**: 1,208 TTFs cannot coexist in memory. Bounding live `FontFamily` instances to
a per-viewport window with prompt release is the documented Compose pattern for large
per-page font sets and directly satisfies the memory constraint.

**Alternatives considered**: (a) Preload all fonts — rejected: guaranteed OOM. (b) No cache
(reload every recomposition) — rejected: jank, fails 60 fps. (c) Global unbounded cache —
rejected: unbounded growth over a long reading session.

---

## R5. Per-word rendering vs 60 fps highlight isolation

**Decision**: Render each line as a Compose `FlowRow` (RTL) of individual `Text`
composables, one per `MushafWord`, each keyed by a stable `word.id`. Highlight by applying
`Modifier.background()` only to the `Text` whose `id == highlightedWordId`. Hold
`highlightedWordId` in a way that limits recomposition to the affected word(s) — read the
highlight via a lambda/`derivedStateOf` at the word level so a highlight change does not
recompose the whole page (SC-005, FR-011).

**Rationale**: Individual `Text` per word is required for word-level backgrounds and future
audio-driven highlighting at 60 fps; a single `AnnotatedString` per page cannot isolate
recomposition. Matches the SSD Phase 3 directive.

**Alternatives considered**: (a) One `AnnotatedString`/page with `SpanStyle` highlight —
rejected: recomposes/relayouts the whole page each tick. (b) Canvas custom draw — rejected:
loses COLR font rendering and accessibility; far more complex.

---

## R6. Page layout & RTL pagination

**Decision**: `HorizontalPager(pageCount = 604)` inside
`CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)` so page 1 is
on the right and forward swipes advance toward 604 (FR-005). Each page is a `Column`
(`Arrangement.SpaceEvenly`, `Alignment.CenterHorizontally`) of 15 line slots; centered
lines honor `is_centered`. `LaunchedEffect(pagerState.currentPage)` dispatches
`MushafIntent.LoadPage`; the current-page indicator reads `pagerState`.

**Rationale**: Directly implements SSD Phase 4 and the RTL/loading/edge-case requirements
(FR-002, FR-006, FR-014, FR-015).

**Alternatives considered**: (a) `LazyRow` — rejected: `HorizontalPager` gives snapping +
`beyondViewportPageCount` memory control for free. (b) Manual RTL by reversing indices —
rejected: error-prone vs `LayoutDirection.Rtl`.

---

## R7. Reader preference persistence (Tajweed toggle + last page)

**Decision**: Jetpack **DataStore (Preferences)** in `:mushaf:data`, behind a domain
`ReaderPreferencesRepository` exposing `Flow<ReaderPreferences>` (`tajweedEnabled`,
`lastPage`) plus suspend setters. ViewModel seeds initial state from the first emission
(default `tajweedEnabled = true`, `lastPage = 1` for a first-time reader) and persists on
toggle / page settle (FR-006a, FR-007a; Clarifications Q1, Q2).

**Rationale**: DataStore is the current AndroidX standard for small async key-value prefs,
Flow-native (fits Coroutines/Flow mandate), and avoids the deprecated `SharedPreferences`.
Overkill to model this in Room.

**Alternatives considered**: (a) `SharedPreferences` — rejected: deprecated pattern, not
Flow-native. (b) Store prefs in the Room DB — rejected: the layout DB is read-only from an
asset; mixing writable prefs muddies it.

---

## R8. Highlight driver interface (audio-ready)

**Decision**: Define a domain-agnostic `HighlightDriver` abstraction that emits a stream of
"currently recited" `wordId`s (or a play/pause/seek surface) which the ViewModel folds into
`highlightedWordId`. Ship a `SimulatedHighlightDriver` that walks the current page's words
on a fixed cadence (~500 ms) as the stand-in trigger (FR-012). The future audio/STT engine
implements the same interface without touching the reading surface (FR-012a).

**Rationale**: Decoupling the highlight source from the UI lets audio land later as a drop-in
implementation, and lets tests drive highlighting deterministically.

**Alternatives considered**: (a) Hard-code a debug button in the UI — rejected: not
swappable for audio, leaks a driver concern into Compose. (b) Timer inside the ViewModel —
rejected: couples playback policy to the reducer; harder to replace with real audio.

---

## R9. Module split & build wiring

**Decision**: Replace the flat `:mushaf` library with `:mushaf:domain` (Kotlin/JVM or
minimal Android lib, no framework deps), `:mushaf:data` (Android lib: Room + DataStore +
assets), `:mushaf:presentation` (Android lib: Compose + `:designsystem`). Update
`settings.gradle.kts` and add version-catalog aliases for Room (+ KSP), Koin, Navigation 3,
DataStore, and Coroutines. `:app` depends on the three modules for nav + Koin `startKoin`.

**Rationale**: Constitution Principle II mandates the layered feature-module split;
compile-time boundaries enforce Principle I. The catalog currently lacks Room/Koin/Nav3/
DataStore, so adding them is prerequisite work.

**Alternatives considered**: (a) Keep one `:mushaf` module with packages — rejected: violates
Principle II; boundaries not compiler-enforced. (b) Put `domain` in pure Kotlin JVM module —
acceptable, but a minimal Android library eases sharing with Android-typed test utilities;
either is constitution-compliant since `domain` stays framework-free.

---

## R10. Localization & RTL

**Decision**: All new user-facing strings (Tajweed toggle label/state, loading/error/empty
messages, page indicator format, follow-along control label) live in
`:mushaf:presentation` `res/values/strings.xml` (EN) and `res/values-ar/strings.xml` (AR),
added in the same change. Layouts use start/end; the pager is RTL; page numbers use
locale-aware formatting. Arabic rendering verified on-device.

**Rationale**: Constitution Principle V and FR-016 require complete EN+AR and RTL-aware UI at
all times.

**Alternatives considered**: none — mandated.

---

## Resolved unknowns summary

| Unknown (Technical Context) | Resolution |
|-----------------------------|------------|
| Real DB schema | R1 — line-based `pages` + `info`; expand word-id ranges |
| Word→glyph mapping | R2 — page-relative PUA codepoint (VERIFY vs font cmap) |
| Font memory strategy | R4 — LRU `FontFamily` cache + `beyondViewportPageCount=1` (VERIFY heap) |
| Persistence mechanism | R7 — Jetpack DataStore (Preferences) |
| Highlight source | R8 — `HighlightDriver` interface + simulated stand-in |
| Module boundaries / missing deps | R9 — 3-way split; add Room/Koin/Nav3/DataStore to catalog |

No unresolved `NEEDS CLARIFICATION` remain. Two **VERIFY** items (R2 glyph mapping, R4 heap)
are implementation-time confirmations, not design blockers.
