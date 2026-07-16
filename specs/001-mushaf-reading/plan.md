# Implementation Plan: Mushaf Reading

**Branch**: `001-mushaf-reading` | **Date**: 2026-07-16 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-mushaf-reading/spec.md`

## Summary

Deliver a print-faithful, offline Qur'an reader for the 604-page KFGQPC (QPC v4) Mushaf. A
reader swipes right-to-left through pages rendered exactly as the printed Mushaf (15 lines,
glyph-per-word vector fonts), toggles Tajweed coloring on/off (dual-font swap, no layout
shift), and follows a word-by-word highlight that a future audio/recitation driver will
control. The reader's Tajweed choice and last page are persisted locally and restored on
launch.

**Technical approach**: A layered `:mushaf` module set (`:mushaf:domain`, `:mushaf:data`,
`:mushaf:presentation`) per the constitution. `:mushaf:data` mounts the packaged
`mushaf_v4_layout.db` read-only via Room `createFromAsset()` and exposes it as domain
models; reader preferences use Jetpack DataStore. `:mushaf:presentation` renders with
Compose `HorizontalPager` (RTL) + per-line `FlowRow` of per-word `Text` glyphs, swapping
`standard/` vs `tajweed/` page fonts lazily by active page, driven by an MVI
`StateFlow<MushafUiState>`. Highlighting is exposed as a driver-agnostic interface so audio
can later drive it. The line-based DB (`pages` table with `first_word_id..last_word_id`
ranges) is expanded into `Page → Line → Word` in the domain layer, with each word rendered
as a single Private-Use-Area glyph in the per-page font.

## Technical Context

**Language/Version**: Kotlin 2.2.10 (JVM target 11); Android Gradle Plugin 9.2.1

**Primary Dependencies**: Jetpack Compose (BOM 2026.02.01) — Foundation (`HorizontalPager`,
`FlowRow`), Material3; AndroidX Lifecycle ViewModel + `viewModelScope`; Kotlin
Coroutines/Flow; **Room** (read-only, `createFromAsset`) — *to add*; **Koin** (DI) — *to
add*; **Navigation 3** — *to add*; **Jetpack DataStore (Preferences)** for reader prefs —
*to add*. Ktor is **not** used (feature is fully offline; no network surface).

**Storage**:
- Read-only Room database from `assets/databases/mushaf_v4_layout.db` (QPC v4 layout;
  tables `pages`, `info`).
- Jetpack DataStore (Preferences) for `tajweedEnabled: Boolean` and `lastPage: Int`.
- Fonts as packaged assets: `assets/fonts/{standard,tajweed}/p{1..604}.ttf` (604 each;
  1208 total).

**Testing**: JUnit4 for `:mushaf:domain` (pure Kotlin: page/line/word grouping, word
expansion, reducer/state logic) and mapper logic; Compose UI tests (`ui-test-junit4`) for
pager RTL order, Tajweed toggle no-shift, and highlight isolation; Room DAO instrumented
test (`androidTest`) reading the packaged DB.

**Target Platform**: Android, `minSdk 24`, `targetSdk 36`, `compileSdk 37`.

**Project Type**: Mobile app (Android), multi-module Gradle, Clean Architecture.

**Performance Goals**: 60 fps during word-by-word highlighting; page turn renders in < 1 s;
zero layout shift on Tajweed toggle; word-level (not page-level) recomposition on highlight
change.

**Constraints**: Fully offline. Memory-bounded — never load all 1208 fonts; lazy-load the
active page's font, cache a tiny window, release off-screen fonts (`HorizontalPager`
`beyondViewportPageCount` = 1). RTL pager (page 1 on the right). No `SpanStyle`/`TextStyle`
color override for Tajweed — coloring lives entirely in the font `COLR` table; the app
swaps font directories only.

**Scale/Scope**: 604 pages, 8,820 ayah lines + 114 surah-name + 112 basmallah lines
(9,046 rows), 83,668 words, 1,208 font files. Single local user; one reading screen.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Status |
|-----------|------|--------|
| I. Clean Architecture & Layer Boundaries | `domain` pure Kotlin (no Room/Compose/Android); `data` owns Room + DataStore and maps to domain; `presentation` depends only on `domain` via use cases; no Room entity or DB row reaches UI | **PASS** — Page/Line/Word domain models are framework-free; entities/DTOs stay in `:mushaf:data` |
| II. Modular Architecture | Mushaf split into `:mushaf:domain`, `:mushaf:data`, `:mushaf:presentation`; feature modules never depend on `:app`; no cycles | **PASS** — replaces the current single `:mushaf` module; `:app` only wires nav + DI |
| III. MVI Unidirectional Data Flow | Single immutable `MushafUiState` via `StateFlow`; `MushafIntent` events; one-off `MushafEffect`; UI pure function of state; business logic in use cases | **PASS** |
| IV. Mandated Technology Stack | Compose, Navigation 3, Koin, Room (in `data`), Coroutines/Flow; Kotlin only | **PASS** — Ktor omitted (no network); documented, not a violation |
| V. Complete Localization (i18n) | All new UI strings in `values/` (EN) + `values-ar/` (AR); RTL-aware (start/end); Arabic verified | **PASS** — pager already RTL; toggle/labels/errors localized in both locales |

**Additional standards**: immutable `data class` state/domain models; typed results for
data failures with explicit loading/empty/error UI states; strings/dimens/colors as
resources; no secrets (none needed — offline). **No violations → Complexity Tracking is
empty.**

## Project Structure

### Documentation (this feature)

```text
specs/001-mushaf-reading/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (interface contracts)
│   ├── domain-repositories.md
│   ├── domain-usecases.md
│   ├── mvi-contract.md
│   └── highlight-driver.md
└── checklists/
    └── requirements.md  # Spec quality checklist (from /speckit-specify)
```

### Source Code (repository root)

The current single `:mushaf` Android library is replaced by a layered module set. Packaged
assets (DB + fonts) move under `:mushaf:data`. `:app` gains only navigation + DI assembly.

```text
settings.gradle.kts            # add :mushaf:domain, :mushaf:data, :mushaf:presentation; remove flat :mushaf
gradle/libs.versions.toml      # add room, koin, navigation3, datastore, coroutines, ksp aliases

mushaf/
├── domain/                    # :mushaf:domain — pure Kotlin (JVM), no Android
│   └── src/main/java/com/example/mushaf/domain/
│       ├── model/             # MushafPage, MushafLine, MushafWord, LineType, ReadingMode
│       ├── repository/        # MushafRepository, ReaderPreferencesRepository (interfaces)
│       └── usecase/           # GetPageUseCase, ObserveReaderPreferencesUseCase, SetTajweedEnabledUseCase, SaveLastPageUseCase
├── data/                      # :mushaf:data — Room + DataStore + assets
│   └── src/main/
│       ├── assets/            # (moved) databases/mushaf_v4_layout.db, fonts/{standard,tajweed}/p*.ttf
│       └── java/com/example/mushaf/data/
│           ├── db/            # MushafDatabase, MushafDao, MushafLineEntity (maps `pages`), MushafInfoEntity
│           ├── prefs/         # ReaderPreferencesDataStore
│           ├── mapper/        # line rows → domain Page/Line/Word (word-id expansion)
│           └── repository/    # MushafRepositoryImpl, ReaderPreferencesRepositoryImpl
└── presentation/              # :mushaf:presentation — Compose + MVI
    └── src/main/
        ├── java/com/example/mushaf/presentation/
        │   ├── state/         # MushafUiState, MushafIntent, MushafEffect
        │   ├── MushafViewModel.kt
        │   ├── MushafScreen.kt
        │   ├── components/    # MushafPageView, MushafLineRow, MushafWordGlyph, TajweedToggle
        │   ├── font/          # PageFontProvider (lazy load/cache/release), font path resolver
        │   └── highlight/     # HighlightDriver interface + SimulatedHighlightDriver (stand-in trigger)
        └── res/values{,-ar}/  # strings.xml (EN + AR)

app/
└── src/main/java/com/iti/al_mahir/
    ├── MainActivity.kt        # hosts Nav 3 graph
    ├── navigation/            # Mushaf destination wiring (Navigation 3)
    └── di/                    # Koin modules assembly (app-level startKoin)
```

**Structure Decision**: Adopt the constitution-mandated per-feature layered split for
Mushaf (`:mushaf:domain`, `:mushaf:data`, `:mushaf:presentation`), replacing today's flat
`:mushaf` library. Dependencies point inward: `presentation → domain ← data`, with `:app`
depending on the three feature modules only for navigation + Koin assembly. Packaged assets
live in `:mushaf:data` (Android merges module assets into the APK, so any module can read
them via `context.assets`). `:mushaf:presentation` reuses `:designsystem` for theming.

## Complexity Tracking

| Deviation | Why Needed | Simpler / Mandated Alternative Rejected Because |
|-----------|------------|-------------------------------------------------|
| Read-only prebuilt DB opened via **direct SQLite** (`MushafAssetDataSource`) instead of Room (Principle IV mandates Room) | The packaged `pages`/`info` tables have **no primary key and nullable columns**. Room's prepackaged-DB schema verification (`onValidateSchema`) rejects the foreign asset at open time, throwing at runtime ("Pre-packaged database has an invalid schema") — this caused the "Something went wrong while opening the Mushaf" error. | Room `@Entity` requires a primary key and matching non-null/PK schema; the foreign asset cannot satisfy this without regenerating the DB. Direct `SQLiteDatabase.OPEN_READONLY` reads the asset losslessly and stays confined to `:mushaf:data` behind `MushafRepository`. Room remains a module dependency for future app-owned writable data (bookmarks, etc.). |
