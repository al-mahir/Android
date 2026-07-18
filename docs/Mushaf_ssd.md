# Epic 3: Mushaf - Reading (MUS) — AI Agent Execution Specification

## System Context & Architectural Constraints
**Agent Directive:** You are building the core reading engine for an Islamic Android application using Jetpack Compose, Kotlin, and Clean Architecture.
*   **Data Source:** KFGQPC V4 layout SQLite database (contains page, line, and word mapping).
*   **Rendering Mechanism:** Glyph-based vector fonts. One Unicode character = one entire drawn word.
*   **Tajweed Toggle Constraint:** Tajweed colors are baked into the font's `COLR` tables and **cannot** be stripped via Compose modifiers. The app utilizes a dual-font strategy. The agent must swap between `tajweed` fonts and `standard` (monochromatic) fonts based on UI state. Both font sets are geometrically identical.
*   **Performance Constraint:** There are 1,208 distinct `.ttf` files (604 colored, 604 standard). **DO NOT** load all fonts into memory at startup. Fonts must be lazy-loaded, cached by the active page, and immediately garbage-collected when off-screen.
*   **Recomposition Constraint:** The UI must support 60fps real-time highlighting driven by an audio/STT pipeline in the future. State changes for highlighting must be isolated to the individual word or line level to prevent full-page recomposition stutters.
*   **direction Constraint:** This feature under mushaf module .

---

## Phase 1: Local Data Layer & Asset Pipeline
**Goal:** Establish the Room database to read the pre-packaged SQLite file and enforce the strict dual-font asset directory structure.

### Agent Tasks:
1.  **Asset Structure Validation:** Assume the following asset directory structure exists and construct file paths accordingly:
    ```text
    src/main/assets/
    ├── databases/
    │   └── mushaf_v4_layout.sqlite
    ├── fonts/
    │   ├── tajweed/
    │   │   └── p{1..604}.ttf
    │   └── standard/
    │       └── p{1..604}.ttf
    ```
2.  **Room Setup:** Configure a Room database using `createFromAsset()` targeting `databases/mushaf_v4_layout.sqlite`.
3.  **Entity Mapping:** Create the `MushafWordEntity` to map exactly to the SQLite schema (page_number, line_number, surah_id, ayah_id, word_position, glyph_code).
4.  **DAO Implementation:** Write `MushafDao` with queries to fetch words by `pageNumber`, ordered strictly by `line_number` ASC, then `word_position` ASC.
5.  **Repository:** Implement `MushafRepository` to expose data as Kotlin `Flow<List<MushafWordEntity>>`.

### Acceptance Criteria:
*   [ ] Database successfully mounts from the `assets/` directory without crashing.
*   [ ] DAO returns a correctly ordered list of words for a given page integer (1-604).
*   [ ] Repository layer abstracts the DAO completely from the ViewModel.

---

## Phase 2: State Management & MVI Architecture
**Goal:** Define the unidirectional data flow that will drive the UI. The state must represent exactly what is on the screen, including the current font rendering mode.

### Agent Tasks:
1.  **Domain Models:** Map the flat Room entities into a nested UI-friendly structure: `MushafPage` -> `List<MushafLine>` -> `List<MushafWord>`.
2.  **UI State Definition:**
    ```kotlin
    data class MushafUiState(
        val currentPage: Int = 1,
        val lines: List<MushafLine> = emptyList(),
        val isTajweedEnabled: Boolean = true, // Default to Tajweed ON
        val highlightedWordId: String? = null,
        val isLoading: Boolean = true
    )
    ```
3.  **Intent Definition:** Create `MushafIntent` sealed class (e.g., `LoadPage(page: Int)`, `ToggleTajweed(enabled: Boolean)`, `HighlightWord(id: String)`).
4.  **ViewModel:** Implement `MushafViewModel` to process intents, interact with `MushafRepository`, and mutate the `StateFlow<MushafUiState>`.

### Acceptance Criteria:
*   [ ] ViewModel exposes a single `StateFlow`.
*   [ ] Flat database lists are correctly grouped into `MushafLine` objects based on the `line_number` property.
*   [ ] State updates do not trigger a full database reload.

---

## Phase 3: Core Rendering Engine & Font Swapping (Compose)
**Goal:** Render the page exactly as a physical Mushaf, handling the dynamic swapping of font directories based on the Tajweed toggle.

### Agent Tasks:
1.  **Dynamic Font Swapping:** Implement logic to resolve the font path dynamically inside the composable.
    ```kotlin
    val fontSubFolder = if (uiState.isTajweedEnabled) "tajweed" else "standard"
    val fontAssetPath = "fonts/$fontSubFolder/p${uiState.currentPage}.ttf"
    
    val pageFontFamily = remember(fontAssetPath) {
        FontFamily(Font(resPaths = fontAssetPath, assets = context.assets))
    }
    ```
2.  **Line Rendering (Performance Critical):**
    *   Do *not* use a single `AnnotatedString` for the entire page if you plan to highlight words individually at 60fps.
    *   **Directive:** Implement a `FlowRow` (or nested `Row` components) for each line. Inside the row, render each `MushafWord` as an individual `Text` composable.
    *   Apply `Modifier.background()` to the specific `Text` composable if `word.id == state.highlightedWordId`.
    *   **Do not** attempt to override the text color using `SpanStyle` or `TextStyle` for Tajweed. Rely entirely on the loaded `FontFamily`.
3.  **Page Layout:** Stack the lines vertically using a `Column` with `Arrangement.SpaceEvenly` and `Alignment.CenterHorizontally` to match physical print constraints.

### Acceptance Criteria:
*   [ ] UI renders 15 lines correctly spaced.
*   [ ] Toggling Tajweed successfully switches the font directory and re-renders the text instantly without layout shifting.
*   [ ] Highlighting a specific word changes its background color without causing the entire page's composable tree to recompose.

---

## Phase 4: Pagination & UX (HorizontalPager)
**Goal:** Allow the user to swipe left and right through the 604 pages smoothly while rigorously managing font memory.

### Agent Tasks:
1.  **Pager Integration:** Implement `HorizontalPager` from Compose Foundation. Set `pageCount` to 604.
2.  **RTL Support:** Ensure the pager flows Right-to-Left (Page 1 is on the right, swiping left goes to Page 2). Use `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)`.
3.  **State Sync:** Use `LaunchedEffect(pagerState.currentPage)` to dispatch `MushafIntent.LoadPage` to the ViewModel.
4.  **Memory Management:** Configure the `HorizontalPager` to keep a strict `beyondBoundsPageCount` of `1` (or default `0`) to prevent caching too many `.ttf` files in memory simultaneously.

### Acceptance Criteria:
*   [ ] User can swipe through pages seamlessly.
*   [ ] Pager starts at Page 1 (RTL orientation).
*   [ ] Memory usage remains stable during rapid swiping (no OOM crashes from stacking `.ttf` files).

---

## Phase 5: Highlighting API (Audio Prep)
**Goal:** Expose a clean interface for the future Audio/STT layers to drive UI highlights.

### Agent Tasks:
1.  **Simulated Audio Sync:** Create a temporary debug button that iterates through the words on the current page, dispatching `MushafIntent.HighlightWord(wordId)` every 500ms.
2.  **Auto-Scrolling (Optional):** If the highlighted word moves to a line that is obscured, ensure the UI scrolls slightly or visually indicates progression.
3.  **Validation:** Ensure that rapid state changes (every 500ms) do not cause UI stuttering or trigger font reloads.

### Acceptance Criteria:
*   [ ] The debug "Play" button successfully moves a highlight box word-by-word across the page.
*   [ ] The application maintains 60fps during this simulated playback.