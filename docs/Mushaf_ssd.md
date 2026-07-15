# Epic 3: Mushaf - Reading (MUS) — AI Agent Execution Specification

## System Context & Architectural Constraints
**Agent Directive:** You are building the core reading engine for an Islamic Android application using Jetpack Compose, Kotlin, and Clean Architecture. 
*   **Data Source:** KFGQPC V4 layout SQLite database (contains page, line, and word mapping).
*   **Rendering Mechanism:** Glyph-based vector fonts. One Unicode character = one entire drawn word.
*   **Performance Constraint:** There are 604 distinct `.ttf` files (one per page). **DO NOT** load all fonts into memory at startup. Fonts must be lazy-loaded and cached by the active page.
*   **Recomposition Constraint:** The UI must support 60fps real-time highlighting driven by an audio/STT pipeline in the future. State changes for highlighting must be isolated to the individual word or line level to prevent full-page recomposition stutters.

---

## Phase 1: Local Data Layer & Asset Pipeline
**Goal:** Establish the Room database to read the pre-packaged Tarteel SQLite file and serve the structural data of the Mushaf.

### Agent Tasks:
1.  **Room Setup:** Configure a Room database using `createFromAsset()` to read the pre-packaged SQLite database provided by QUL.
2.  **Entity Mapping:** Create the `MushafWordEntity` to map exactly to the SQLite schema (page_number, line_number, surah_id, ayah_id, word_position, glyph_code).
3.  **DAO Implementation:** Write `MushafDao` with queries to fetch words by `pageNumber`, ordered strictly by `line_number` ASC, then `word_position` ASC.
4.  **Repository:** Implement `MushafRepository` to expose data as Kotlin `Flow<List<MushafWordEntity>>`.

### Acceptance Criteria:
*   [ ] Database successfully mounts from the `assets/` directory without crashing.
*   [ ] DAO returns a correctly ordered list of words for a given page integer (1-604).
*   [ ] Repository layer abstracts the DAO completely from the ViewModel.

---

## Phase 2: State Management & MVI Architecture
**Goal:** Define the unidirectional data flow that will drive the UI. The state must represent exactly what is on the screen, including highlighting and Tajweed toggles.

### Agent Tasks:
1.  **Domain Models:** Map the flat Room entities into a nested UI-friendly structure: `MushafPage` -> `List<MushafLine>` -> `List<MushafWord>`.
2.  **UI State Definition:** 
    ```kotlin
    data class MushafUiState(
        val currentPage: Int = 1,
        val lines: List<MushafLine> = emptyList(),
        val isTajweedEnabled: Boolean = false,
        val highlightedWordId: String? = null,
        val isLoading: Boolean = true
    )
    ```
3.  **Intent Definition:** Create `MushafIntent` sealed class (e.g., `LoadPage(page: Int)`, `ToggleTajweed`, `HighlightWord(id: String)`).
4.  **ViewModel:** Implement `MushafViewModel` to process intents, interact with `MushafRepository`, and mutate the `StateFlow<MushafUiState>`.

### Acceptance Criteria:
*   [ ] ViewModel exposes a single `StateFlow`.
*   [ ] Flat database lists are correctly grouped into `MushafLine` objects based on the `line_number` property.
*   [ ] State updates (like highlighting a word) do not trigger a full database reload.

---

## Phase 3: Core Rendering Engine (Compose)
**Goal:** Render the page exactly as a physical Mushaf, handling the dynamic loading of the glyph-based fonts.

### Agent Tasks:
1.  **Dynamic Font Loading:** Write a utility or `remember` block to load `p{pageNumber}.ttf` from the assets folder. Swap to `v4_tajweed/p{pageNumber}.ttf` if `isTajweedEnabled` is true. Handle missing font files gracefully with a fallback.
2.  **Line Rendering (Performance Critical):** 
    *   Do *not* use a single `AnnotatedString` for the entire page if you plan to highlight words individually at 60fps.
    *   **Directive:** Implement a `FlowRow` (or nested `Row` components) for each line. Inside the row, render each `MushafWord` as an individual `Text` composable.
    *   Apply `Modifier.background()` to the specific `Text` composable if `word.id == state.highlightedWordId`.
3.  **Page Layout:** Stack the lines vertically using a `Column` with `Arrangement.SpaceEvenly` and `Alignment.CenterHorizontally` to match the physical print constraints.

### Acceptance Criteria:
*   [ ] UI renders 15 lines correctly spaced.
*   [ ] Fonts are applied correctly; switching the Tajweed toggle swaps the font family.
*   [ ] Highlighting a specific word changes its background color without causing the entire page's composable tree to recompose (verify with Layout Inspector).

---

## Phase 4: Pagination & UX (HorizontalPager)
**Goal:** Allow the user to swipe left and right through the 604 pages smoothly.

### Agent Tasks:
1.  **Pager Integration:** Implement `HorizontalPager` from Compose Foundation. Set `pageCount` to 604.
2.  **RTL Support:** Ensure the pager flows Right-to-Left (Page 1 is on the right, swiping left goes to Page 2). Use `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)`.
3.  **State Sync:** Use `LaunchedEffect(pagerState.currentPage)` to dispatch `MushafIntent.LoadPage` to the ViewModel so the repository fetches the correct data ahead of time.
4.  **Memory Management:** Ensure the `HorizontalPager` only keeps 1-2 pages alive off-screen to prevent font memory leaks.

### Acceptance Criteria:
*   [ ] User can swipe through pages seamlessly.
*   [ ] Pager starts at Page 1 (RTL orientation).
*   [ ] Memory usage remains stable during rapid swiping (no OOM crashes from stacking `.ttf` files).

---

## Phase 5: Highlighting API (Audio Prep)
**Goal:** Expose a clean interface for the future Audio/STT layers to drive UI highlights.

### Agent Tasks:
1.  **Simulated Audio Sync:** Create a temporary debug button that iterates through the words on the current page, dispatching `MushafIntent.HighlightWord(wordId)` every 500ms.
2.  **Auto-Scrolling (Optional but recommended):** If the highlighted word moves to a line that is obscured, ensure the UI scrolls slightly (if applicable) or visually indicates progression.
3.  **Validation:** Ensure that rapid state changes (every 500ms) do not cause UI stuttering.

### Acceptance Criteria:
*   [ ] The debug "Play" button successfully moves a highlight box word-by-word across the page.
*   [ ] The application maintains 60fps during this simulated playback.