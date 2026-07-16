---
description: "Task list for Mushaf Reading feature implementation"
---

# Tasks: Mushaf Reading

**Input**: Design documents from `/specs/001-mushaf-reading/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/)

**Tests**: Included selectively — the constitution requires `domain` use-case and reducer/state
logic to be unit-tested and mandates verifying RTL/layout behavior. Test tasks are marked and
may be written alongside (or before) their implementation.

**Organization**: Grouped by user story (US1–US4) so each is independently implementable and
testable. Priority order from spec.md: US1 (P1) → US2 (P2) → US3 (P3) → US4 (P4).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on incomplete tasks)
- **[Story]**: US1/US2/US3/US4 (user-story phases only)

## Path Conventions (from plan.md — multi-module Android)

- Domain: `mushaf/domain/src/main/java/com/example/mushaf/domain/`
- Data: `mushaf/data/src/main/java/com/example/mushaf/data/` (+ `src/main/assets/`)
- Presentation: `mushaf/presentation/src/main/java/com/example/mushaf/presentation/` (+ `src/main/res/`)
- App: `app/src/main/java/com/iti/al_mahir/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Restructure `:mushaf` into the constitution-mandated layered module set and add
the missing stack dependencies.

- [X] T001 Add stack dependencies to `gradle/libs.versions.toml`: versions + `[libraries]` + `[plugins]` for Room + KSP, Koin (`koin-android`, `koin-androidx-compose`), Navigation 3, Jetpack DataStore (Preferences), Kotlin Coroutines, and the `kotlin-android` plugin alias.
- [X] T002 Update `settings.gradle.kts`: remove flat `include(":mushaf")`; add `include(":mushaf:domain")`, `include(":mushaf:data")`, `include(":mushaf:presentation")`. Create the three module directories with skeleton `build.gradle.kts` and `AndroidManifest.xml` where needed; delete the obsolete flat `mushaf/src` sources.
- [X] T003 [P] Configure `mushaf/domain/build.gradle.kts` as a framework-free module (Kotlin + Coroutines core only; no Compose/Room/Android UI deps) — namespace `com.example.mushaf.domain`.
- [X] T004 [P] Configure `mushaf/data/build.gradle.kts`: Room + KSP, DataStore, Koin, Coroutines; `implementation(project(":mushaf:domain"))`; namespace `com.example.mushaf.data`.
- [X] T005 [P] Configure `mushaf/presentation/build.gradle.kts`: Compose BOM + Foundation + Material3, `kotlin.compose` plugin, Koin-compose, `implementation(project(":mushaf:domain"))`, `implementation(project(":designsystem"))`; namespace `com.example.mushaf.presentation`.
- [X] T006 Move packaged assets into `mushaf/data/src/main/assets/` — `databases/mushaf_v4_layout.db` and `fonts/{standard,tajweed}/p{1..604}.ttf` — and remove them from the old flat module location.
- [X] T007 [P] Add baseline localized string resources in `mushaf/presentation/src/main/res/values/strings.xml` (EN) and `values-ar/strings.xml` (AR) with feature keys placeholder (page indicator, loading, error, retry, tajweed toggle, follow-along).

**Checkpoint**: Project compiles with three empty Mushaf modules wired via version catalog.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared domain models, read-only Room base, preferences store, DI, MVI scaffolding,
and navigation host that ALL user stories depend on.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T008 [P] Create domain models in `mushaf/domain/.../model/`: `MushafPage.kt`, `MushafLine.kt`, `MushafWord.kt`, `LineType.kt` (AYAH/BASMALLAH/SURAH_NAME), `ReadingMode.kt`, `ReaderPreferences.kt` — immutable `data class`/`enum` per [data-model.md](./data-model.md) §2.
- [X] T009 [P] Define repository interfaces in `mushaf/domain/.../repository/`: `MushafRepository.kt` and `ReaderPreferencesRepository.kt` per [contracts/domain-repositories.md](./contracts/domain-repositories.md).
- [X] T010 Create Room layer in `mushaf/data/.../db/`: `MushafLineEntity.kt` (maps `pages`), `MushafInfoEntity.kt` (maps `info`), `MushafDao.kt` (fetch lines by `page_number` ordered by `line_number` ASC; `getPageCount()` from `info`), and `MushafDatabase.kt` configured with `Room.databaseBuilder(...).createFromAsset("databases/mushaf_v4_layout.db")` (read-only) per [data-model.md](./data-model.md) §1.
- [X] T011 [P] Implement preferences store in `mushaf/data/.../prefs/ReaderPreferencesDataStore.kt` + `mushaf/data/.../repository/ReaderPreferencesRepositoryImpl.kt` exposing `Flow<ReaderPreferences>` (defaults `tajweedEnabled=true`, `lastPage=1`) and suspend setters (page clamped 1..604) per [contracts/domain-repositories.md](./contracts/domain-repositories.md).
- [X] T012 [P] Create MVI contracts in `mushaf/presentation/.../state/`: `MushafUiState.kt`, `MushafIntent.kt`, `MushafEffect.kt`, and `MushafError.kt` per [contracts/mvi-contract.md](./contracts/mvi-contract.md).
- [X] T013 Create `mushaf/presentation/.../MushafViewModel.kt` skeleton: single `StateFlow<MushafUiState>`, `onIntent(MushafIntent)` dispatch stub, and `init{}` that collects `ObserveReaderPreferencesUseCase` to seed `isTajweedEnabled` + resume page (wired further per story).
- [X] T014 [P] Create `ObserveReaderPreferencesUseCase.kt` in `mushaf/domain/.../usecase/` per [contracts/domain-usecases.md](./contracts/domain-usecases.md).
- [X] T015 Create Koin modules: `mushaf/data/.../di/DataModule.kt` (Room, DAO, DataStore, repo impls), `mushaf/presentation/.../di/PresentationModule.kt` (ViewModel, use cases). Add `app/src/main/java/com/iti/al_mahir/AlMahirApp.kt` (`Application`) calling `startKoin { modules(...) }` and register it in `app/src/main/AndroidManifest.xml`.
- [X] T016 Wire Navigation 3 in `app/src/main/java/com/iti/al_mahir/navigation/` + `MainActivity.kt`: add a Mushaf destination that hosts an (initially empty) `MushafScreen` composable.

**Checkpoint**: App launches, Koin resolves the Mushaf graph, DB mounts, empty screen renders.

---

## Phase 3: User Story 1 - Read a page of the Mushaf (Priority: P1) 🎯 MVP

**Goal**: Render a single Mushaf page (15 lines, glyph-per-word) exactly as the printed page,
with loading/error states.

**Independent Test**: Open the feature at page 1 (or any 1–604) and confirm words are grouped
into correct lines in reading order, centered/spaced like print (quickstart scenarios 1–3).

### Tests for User Story 1

- [X] T017 [P] [US1] Unit test for page/line/word mapping + word-id range expansion in `mushaf/domain/src/test/java/.../MushafMapperTest.kt` (assert page 1 = 36 words, correct line grouping, `SURAH_NAME`/`BASMALLAH` yield empty word lists).
- [ ] T018 [P] [US1] Instrumented DAO test in `mushaf/data/src/androidTest/java/.../MushafDaoTest.kt` reading the packaged DB (page 1 returns ordered lines; `getPageCount()==604`).

### Implementation for User Story 1

- [X] T019 [P] [US1] Implement `GlyphCodeResolver.kt` in `mushaf/data/.../mapper/` — resolve per-page PUA codepoint from page-relative word index per [research.md](./research.md) R2 (single point of change; VERIFY vs font cmap).
- [X] T020 [US1] Implement `MushafMapper.kt` in `mushaf/data/.../mapper/` — group line rows into `MushafPage → MushafLine → MushafWord`, expanding `first_word_id..last_word_id` and assigning stable `id` `"p{page}:l{line}:w{wordId}"` (depends on T008, T019).
- [X] T021 [US1] Implement `MushafRepositoryImpl.kt` in `mushaf/data/.../repository/` — `getPage()` (Flow) + `getPageCount()`, mapping DAO rows via `MushafMapper`; surface typed failure (depends on T010, T020).
- [X] T022 [P] [US1] Implement `GetPageUseCase.kt` in `mushaf/domain/.../usecase/` — clamp page to 1..604, delegate to repo (depends on T009).
- [X] T023 [US1] Implement `PageFontProvider.kt` in `mushaf/presentation/.../font/` — resolve `fonts/{standard|tajweed}/p{page}.ttf`, load `FontFamily` off main thread (`Dispatchers.IO`), LRU-cache by `(mode,page)`, release off-window entries per [research.md](./research.md) R4.
- [X] T024 [US1] Wire `MushafViewModel` `LoadPage` reducer: set loading, call `GetPageUseCase`, update `page`/`isLoading`/`error` (depends on T013, T021, T022).
- [X] T025 [P] [US1] Implement `MushafWordGlyph.kt` in `mushaf/presentation/.../components/` — single `Text(glyph, fontFamily = pageFont)`, stable key by `word.id`.
- [X] T026 [US1] Implement `MushafLineRow.kt` in `mushaf/presentation/.../components/` — RTL `FlowRow` of `MushafWordGlyph` for AYAH lines; single centered glyph for `SURAH_NAME`/`BASMALLAH` (honors `isCentered`) (depends on T025).
- [X] T027 [US1] Implement `MushafPageView.kt` in `mushaf/presentation/.../components/` — `Column(SpaceEvenly, CenterHorizontally)` of line slots; consumes `PageFontProvider` (depends on T023, T026).
- [X] T028 [P] [US1] Implement loading + error/retry composables in `mushaf/presentation/.../components/` using `:designsystem` (Shimmer/overlay); strings from resources.
- [X] T029 [US1] Assemble `MushafScreen.kt` for a single page: collect state, render `MushafPageView` / loading / error; dispatch initial `LoadPage(currentPage)` (depends on T024, T027, T028).
- [X] T030 [US1] Populate US1 strings (loading, error, retry) in `values/strings.xml` + `values-ar/strings.xml`.

**Checkpoint**: A correct, print-faithful page renders with loading/error states — MVP deliverable.

---

## Phase 4: User Story 2 - Navigate through the Mushaf by swiping (Priority: P2)

**Goal**: RTL `HorizontalPager` across all 604 pages, synced page indicator, bounds handling,
bounded memory, and resume-at-last-page.

**Independent Test**: From page 1 swipe to any page up to 604 in RTL order; indicator stays in
sync; relaunch resumes last page (quickstart scenarios 4–7, 10).

### Tests for User Story 2

- [ ] T031 [P] [US2] Compose UI test in `mushaf/presentation/src/androidTest/java/.../MushafPagerTest.kt` — RTL order (page 1 on right, swipe advances to 2), indicator sync, bounds at 1/604.

### Implementation for User Story 2

- [X] T032 [US2] Add `HorizontalPager` to `MushafScreen.kt` inside `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)`, `pageCount = state.pageCount`, `beyondViewportPageCount = 1`; render `MushafPageView` per page (depends on T029).
- [X] T033 [US2] Add `LaunchedEffect(pagerState.currentPage)` dispatching `MushafIntent.LoadPage(currentPage)`; clamp at bounds (FR-015) (depends on T032).
- [X] T034 [P] [US2] Implement page-indicator composable in `mushaf/presentation/.../components/PageIndicator.kt` bound to `pagerState`, locale-aware number formatting; strings in both locales.
- [X] T035 [P] [US2] Implement `SaveLastPageUseCase.kt` in `mushaf/domain/.../usecase/` per [contracts/domain-usecases.md](./contracts/domain-usecases.md).
- [X] T036 [US2] Persist last page: on `LoadPage`/page settle, call `SaveLastPageUseCase`; seed `pagerState` initial page from `ReaderPreferences.lastPage` on launch (resume) (depends on T013, T032, T035).

**Checkpoint**: US1 + US2 work — reader swipes RTL through 604 pages and resumes on relaunch.

---

## Phase 5: User Story 3 - Toggle Tajweed coloring (Priority: P3)

**Goal**: Toggle Tajweed on/off with instant font-directory swap, zero layout shift, persisted
across restarts.

**Independent Test**: Toggle on any page — color changes with no word/line movement; relaunch
keeps last setting (quickstart scenarios 8–9).

### Tests for User Story 3

- [ ] T037 [P] [US3] Compose UI test in `.../androidTest/.../TajweedToggleTest.kt` — assert word/line positions unchanged across toggle (no layout shift); assert font path switches.

### Implementation for User Story 3

- [X] T038 [P] [US3] Implement `SetTajweedEnabledUseCase.kt` in `mushaf/domain/.../usecase/`.
- [X] T039 [US3] Wire `MushafViewModel` `ToggleTajweed` reducer — flip `isTajweedEnabled` without reloading `page`; call `SetTajweedEnabledUseCase` (depends on T013, T038).
- [X] T040 [US3] Ensure `PageFontProvider`/font-path resolver selects directory from `isTajweedEnabled` and that `MushafPageView` re-renders on mode change only (no relayout) (depends on T023, T027, T039).
- [X] T041 [P] [US3] Implement `TajweedToggle.kt` composable in `mushaf/presentation/.../components/` using `:designsystem`; dispatch `ToggleTajweed`; labels from resources (EN + AR).
- [X] T042 [US3] Seed toggle initial state from `ReaderPreferences.tajweedEnabled` (default ON) via the `init` collector; add toggle to `MushafScreen` chrome (depends on T036 seeding path, T041).

**Checkpoint**: US1–US3 work — reader toggles Tajweed with no shift, persisted across restarts.

---

## Phase 6: User Story 4 - Word-by-word highlighting (Priority: P4)

**Goal**: One-word-at-a-time highlight advancing in reading order, driven by a swappable
interface, with a stand-in trigger; word-level recomposition at 60 fps.

**Independent Test**: Start the follow-along preview — a highlight advances word-by-word,
smoothly, only the highlighted word changes (quickstart scenarios 11–12).

### Tests for User Story 4

- [ ] T043 [P] [US4] Compose UI test in `.../androidTest/.../HighlightTest.kt` — single word highlighted at a time, advances in reading order; surrounding words/layout stable.

### Implementation for User Story 4

- [X] T044 [P] [US4] Define `HighlightDriver.kt` interface in `mushaf/presentation/.../highlight/` (or `:mushaf:domain`) per [contracts/highlight-driver.md](./contracts/highlight-driver.md).
- [X] T045 [US4] Implement `SimulatedHighlightDriver.kt` in `mushaf/presentation/.../highlight/` — walk current page words in reading order emitting `wordId` every ~500 ms; cancel on stop (emits null) (depends on T044).
- [X] T046 [US4] Wire `MushafViewModel` `HighlightWord`, `StartFollowAlongPreview`, `StopFollowAlongPreview` reducers — fold driver emissions into `highlightedWordId` (depends on T013, T045).
- [X] T047 [US4] Apply `Modifier.background()` in `MushafWordGlyph` only when `word.id == highlightedWordId`, reading the highlight via a lambda/`derivedStateOf` so only that word recomposes (no `SpanStyle`) (depends on T025, T046).
- [X] T048 [P] [US4] Add follow-along trigger control to `MushafScreen` chrome; dispatch Start/Stop; label from resources (EN + AR).

**Checkpoint**: All four user stories independently functional.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Verification, tests, and constitution/quality gates across stories.

- [X] T049 [P] Reducer/state unit tests in `mushaf/presentation/src/test/java/.../MushafReducerTest.kt` (LoadPage/ToggleTajweed/HighlightWord/Retry transitions).
- [X] T050 [P] **VERIFY R2**: confirm word→glyph mapping against a font `cmap` (`fontTools getBestCmap()` on `standard/p1.ttf`); correct `GlyphCodeResolver` if needed.
- [ ] T051 **VERIFY R4/SC-006**: rapid-swipe 50+ pages under Android Studio Profiler; confirm bounded heap, no OOM; tune `PageFontProvider` cache capacity.
- [X] T052 [P] Localization audit — every US1–US4 string present and correct in `values/` and `values-ar/`; RTL layout verified on-device (FR-016, SC-007).
- [X] T053 Run `:app:assembleDebug` + lint; verify module-boundary/dependency-direction cleanliness (no Room/DataStore type leaks into presentation) per Constitution I & II.
- [ ] T054 Execute all 13 [quickstart.md](./quickstart.md) validation scenarios on a physical device; record pass/fail.

### Remediation (from /speckit-analyze)

- [X] T055 [P] Unit test `GetPageUseCase` page-clamp logic in `mushaf/domain/src/test/java/.../GetPageUseCaseTest.kt` — assert requests < 1 clamp to 1 and > 604 clamp to 604 (FR-015; constitution III use-case testing). *(Finding G1)*
- [ ] T056 Measure page-turn latency (SC-002) on a reference mid-range API 24 device: swipe to an uncached page and confirm the rendered page appears in < 1 s; record the device tier used. Extend the T051 profiling session. *(Findings C1, U1)*
- [ ] T057 Resolve US4 Acceptance #4 (highlighted word stays discernible): confirm a full page fits on-screen without scrolling for standard pages and, if any page can clip a highlighted word, scroll/nudge so it stays visible; otherwise record in `spec.md`/`quickstart.md` that pages fit on-screen (scenario N/A). Add an assertion to the T043 highlight UI test. *(Finding C2)*

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately.
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories.
- **User Stories (Phase 3–6)**: All depend on Foundational. US1 is the MVP. US2/US3/US4 layer
  onto US1's screen/rendering but each is independently testable at its checkpoint.
- **Polish (Phase 7)**: Depends on the targeted user stories being complete.

### User Story Dependencies

- **US1 (P1)**: Foundational only. Delivers the rendering surface (MVP).
- **US2 (P2)**: Foundational; integrates with US1's `MushafPageView` inside a pager.
- **US3 (P3)**: Foundational; reuses US1's `PageFontProvider`/`MushafPageView` for font swap.
- **US4 (P4)**: Foundational; reuses US1's `MushafWordGlyph` for the highlight modifier.

> US2–US4 build on the US1 rendering surface by design (single reading screen). Each remains
> independently *testable* via its own checkpoint and UI test, per spec independent-test notes.

### Within Each User Story

- Tests may be authored first (recommended for domain/reducer per constitution).
- Data mappers/repos before use cases; use cases before ViewModel wiring; ViewModel before UI.

### Parallel Opportunities

- Setup: T003, T004, T005, T007 in parallel after T002; T001 first.
- Foundational: T008, T009, T011, T012, T014 in parallel (distinct files); T010 then repos.
- US1 tests T017/T018 in parallel; T019, T022, T025, T028 parallel where marked.
- Different stories can be staffed in parallel after Foundational, coordinating on shared
  `MushafScreen.kt`/`MushafViewModel.kt` edits (US2/US3/US4 touch these — sequence those edits).

---

## Parallel Example: User Story 1

```bash
# Tests together:
Task: "Unit test MushafMapper word expansion in mushaf/domain/src/test/.../MushafMapperTest.kt"
Task: "Instrumented DAO test in mushaf/data/src/androidTest/.../MushafDaoTest.kt"

# Parallel implementation (distinct files):
Task: "Implement GlyphCodeResolver in mushaf/data/.../mapper/GlyphCodeResolver.kt"
Task: "Implement GetPageUseCase in mushaf/domain/.../usecase/GetPageUseCase.kt"
Task: "Implement MushafWordGlyph in mushaf/presentation/.../components/MushafWordGlyph.kt"
Task: "Implement loading/error composables in mushaf/presentation/.../components/"
```

---

## Implementation Strategy

### MVP First (User Story 1 only)

1. Phase 1 Setup → 2. Phase 2 Foundational → 3. Phase 3 US1 → **STOP & VALIDATE** (quickstart
   1–3) → demo a readable, print-faithful page.

### Incremental Delivery

Foundation → US1 (MVP: read a page) → US2 (swipe + resume) → US3 (Tajweed toggle) → US4
(highlighting). Each story validated at its checkpoint before the next.

### Parallel Team Strategy

After Foundational: Dev A drives US1 (rendering core); once US1's `MushafScreen`/`MushafPageView`
land, Dev B takes US2 (pager) and Dev C takes US3 (toggle) with coordinated edits to the shared
screen/ViewModel; US4 follows on the same surface.

---

## Notes

- [P] = different files, no incomplete-task dependency.
- US2–US4 intentionally share the single reading screen; serialize edits to `MushafScreen.kt`
  and `MushafViewModel.kt` across those stories to avoid conflicts.
- Keep `domain` framework-free; never leak Room entities/DataStore types into presentation.
- Every new UI string lands in both `values/` and `values-ar/` in the same change.
- Two VERIFY tasks (T050 glyph mapping, T051 heap) confirm the research assumptions on-device.
- Commit after each task or logical group; stop at any checkpoint to validate independently.
