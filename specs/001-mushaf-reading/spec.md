# Feature Specification: Mushaf Reading

**Feature Branch**: `feature/Mushaf`

**Created**: 2026-07-16

**Status**: Draft

**Input**: User description: "create the specification for the mushaf feature and all the details about it exist in Mushaf_ssd.md"

## Clarifications

### Session 2026-07-16

- Q: Should the Tajweed coloring toggle be remembered across sessions? → A: Persist across app restarts (remembered as a user preference).
- Q: On launch, should the reader resume their last page or always start at page 1? → A: Resume at the last page viewed (persisted across restarts).
- Q: Is word-by-word highlighting a user-facing feature or a developer-only debug trigger? → A: User-facing — the highlight indicates the word being recited during audio/recitation-follow-along mode so the reader knows which word they are reading. The actual audio/speech driver is a future feature; this feature delivers the highlighting engine plus a stand-in trigger that emulates the audio driver until it lands.
- Q: Can the reader jump directly to a specific page, or only swipe? → A: Swipe-only navigation for this feature; direct jump-to-page is out of scope (future).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Read a page of the Mushaf (Priority: P1)

A reader opens the Mushaf and sees a single page of the Qur'an rendered exactly as it appears in the printed KFGQPC (King Fahd Glorious Qur'an Printing Complex) Mushaf: fifteen lines of text, each word positioned and spaced to match the physical page, so the reader can recite and read with a familiar, print-faithful layout.

**Why this priority**: Faithful page rendering is the core value of the feature. Without a correctly laid-out page, nothing else (navigation, coloring, highlighting) has anything to display. A single readable page is the minimum viable product.

**Independent Test**: Open the feature at any given page number (1–604) and confirm the page shows the correct words, in the correct order, on the correct lines, matching the printed Mushaf for that page.

**Acceptance Scenarios**:

1. **Given** the reader opens the Mushaf at page 1, **When** the page loads, **Then** all words for that page appear grouped into their correct lines and in reading order (top line to bottom line, and within each line in the correct sequence).
2. **Given** a page contains fifteen lines, **When** the page is displayed, **Then** the lines are stacked vertically and evenly spaced and horizontally centered to resemble the printed page.
3. **Given** the reader requests a specific page number between 1 and 604, **When** the page loads, **Then** the content shown corresponds to that exact page of the standard Mushaf.
4. **Given** the page content is still being retrieved, **When** the reader is waiting, **Then** a loading indication is shown until the page is ready.

---

### User Story 2 - Navigate through the Mushaf by swiping (Priority: P2)

A reader moves through the Qur'an by swiping horizontally, turning pages one at a time across the full 604-page Mushaf. Because Arabic reads right-to-left, page 1 sits on the right and swiping advances toward page 2, mirroring how a physical Mushaf is turned.

**Why this priority**: A reader needs to move beyond a single page to read continuously. Page turning is essential to a usable reading experience but depends on page rendering (P1) existing first.

**Independent Test**: Starting at page 1, swipe repeatedly and confirm the reader can reach any page up to 604 in the correct right-to-left order, and that the displayed page number stays in sync with the visible page.

**Acceptance Scenarios**:

1. **Given** the reader is on page 1, **When** they swipe to turn the page, **Then** the next page (page 2) is shown in right-to-left reading order.
2. **Given** the reader swipes rapidly across many pages, **When** they stop, **Then** the correct page is displayed with no crash or freeze.
3. **Given** the reader is viewing any page, **When** the visible page changes, **Then** the current-page indicator reflects the newly visible page.
4. **Given** the reader reaches the last page (604), **When** they attempt to swipe further, **Then** the reader remains on the last page without error.

---

### User Story 3 - Toggle Tajweed coloring (Priority: P3)

A reader turns Tajweed coloring on or off. When on, the text shows the standard color-coded Tajweed rules that aid correct recitation; when off, the same text is shown in a single, plain color. The page geometry and word positions stay identical either way, so toggling never shifts the layout.

**Why this priority**: Tajweed coloring is a meaningful aid for reciters but is an enhancement on top of a correctly rendered, navigable page. Readers can still read fully without it.

**Independent Test**: On any page, toggle the setting and confirm the text switches between color-coded and single-color presentations instantly, with no change to word positions or line layout.

**Acceptance Scenarios**:

1. **Given** Tajweed coloring is on (the default), **When** the page is displayed, **Then** words show the color-coded Tajweed presentation.
2. **Given** the reader turns Tajweed coloring off, **When** the toggle is applied, **Then** the same page is redrawn in a single plain color with no shift in the position or spacing of any word.
3. **Given** the reader toggles the setting back and forth, **When** each toggle is applied, **Then** the change is reflected immediately without a visible reload or layout jump.

---

### User Story 4 - Word-by-word highlighting (Priority: P4)

The reader can see a single word highlighted at a time, moving word-by-word across the page, indicating the word currently being recited so the reader can visually follow along as they read the Qur'an. In the finished product this highlight is driven by an audio/recitation-follow-along mode (which listens as the reader recites and marks the word being read). That audio/speech driver is a future feature; this feature delivers the highlighting surface and engine plus a stand-in trigger that emulates the audio driver so the follow-along experience can be built and validated now.

**Why this priority**: Highlighting is the reading surface for the follow-along experience that audio/recitation-tracking will drive. It adds real follow-along value but is not required for basic reading, navigation, or Tajweed, and its ultimate driver (audio) lands later.

**Independent Test**: Trigger the word-by-word progression on a page (via the stand-in trigger) and confirm that a highlight advances from word to word in reading order, smoothly, without disturbing the rest of the page.

**Acceptance Scenarios**:

1. **Given** the reader starts the word-by-word progression, **When** it runs, **Then** exactly one word is highlighted at a time and the highlight advances through the page in reading order.
2. **Given** a word becomes highlighted, **When** the highlight is applied, **Then** only that word's appearance changes and the surrounding words and layout stay stable.
3. **Given** the highlight is progressing steadily, **When** the reader observes the page, **Then** the motion remains smooth with no stutter or flicker.
4. **Given** a highlighted word is near an obscured or off-view part of the page, **When** the highlight reaches it, **Then** the reader is able to keep track of the current word (e.g., the view keeps the highlighted word discernible).

---

### Edge Cases

- **First and last page bounds**: Swiping backward from page 1 or forward from page 604 keeps the reader on a valid page without error.
- **Rapid page turning**: Fast, repeated swipes must not cause the app to run out of memory, crash, or leave the page indicator out of sync.
- **Toggle during navigation**: Changing the Tajweed setting while mid-swipe or on a partially loaded page must resolve to a consistent, correctly colored page.
- **Highlighting during a page turn**: If the reader turns the page while highlighting is active, the highlight state resolves cleanly for the newly visible page.
- **Page with fewer visual lines**: Pages that do not fill all fifteen lines (e.g., the opening pages or a surah ending) still render with correct spacing and no empty-line artifacts.
- **Slow content retrieval**: If a page's content is slow to load, the reader sees a loading state rather than a blank or broken page.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST display a single Mushaf page composed of its words grouped into lines, ordered by line and by word position within each line, matching the standard printed Mushaf layout.
- **FR-002**: The system MUST render each page's lines stacked vertically, evenly spaced, and horizontally centered to resemble the physical printed page.
- **FR-003**: The system MUST support all 604 pages of the standard Mushaf and display the correct content for any requested page number in that range.
- **FR-004**: The system MUST allow the reader to move between pages by horizontal swiping, one page per turn. Swiping is the only page-navigation method in this feature; direct jump-to-page (entering/selecting a page number) is out of scope.
- **FR-005**: Page navigation MUST follow right-to-left reading order, with page 1 positioned on the right and forward swipes advancing toward page 604.
- **FR-006**: The system MUST keep a visible current-page indicator synchronized with the page currently shown.
- **FR-006a**: The system MUST remember the reader's last-viewed page across app restarts and reopen the Mushaf at that page; a first-time reader opens at page 1.
- **FR-007**: The system MUST provide a control to toggle Tajweed coloring on or off, defaulting to on for a first-time reader.
- **FR-007a**: The system MUST persist the reader's Tajweed coloring choice across app restarts, so a returning reader sees their last-used setting rather than the default.
- **FR-008**: When Tajweed coloring is off, the system MUST display the same page content in a single plain color while preserving identical word positions, spacing, and line layout as when coloring is on.
- **FR-009**: Toggling Tajweed coloring MUST update the current page immediately without a visible reload and without any layout shift.
- **FR-010**: The system MUST be able to highlight one word at a time and advance the highlight from word to word across a page in reading order.
- **FR-011**: Applying or moving a word highlight MUST change only the highlighted word's appearance and MUST NOT visibly disturb other words or the page layout.
- **FR-012**: The system MUST provide a stand-in trigger that advances the word-by-word highlight across the current page, emulating the future audio/recitation-follow-along driver so the follow-along experience can be exercised before audio ships.
- **FR-012a**: The system MUST expose the highlighting as a driver-agnostic interface (one word designated as "currently recited" at a time) so the future audio/speech-recognition mode can drive the same highlight without changing the reading surface.
- **FR-013**: The system MUST show a loading indication while a page's content is being retrieved and MUST replace it with the page once ready.
- **FR-014**: The system MUST remain stable (no crash, freeze, or out-of-memory failure) during rapid, repeated page turning across the full page range.
- **FR-015**: The system MUST prevent navigation beyond the valid page range, keeping the reader on page 1 or page 604 at the respective bounds.
- **FR-016**: All reader-facing text and controls introduced by this feature MUST be available in both English and Arabic, with right-to-left-aware layout.

### Key Entities *(include if feature involves data)*

- **Mushaf Page**: One of the 604 pages of the standard Mushaf. Identified by its page number (1–604). Contains the ordered set of lines that make up the page.
- **Mushaf Line**: A single printed line on a page. Has a position within the page (line number) and contains the ordered words that appear on that line.
- **Mushaf Word**: A single displayable word unit on a line. Has a position within its line, belongs to a specific surah (chapter) and ayah (verse), and can be individually highlighted. It is the smallest unit the reader interacts with (for highlighting/follow-along).
- **Reading Presentation Mode**: The reader's choice of how text is colored — Tajweed (color-coded) or plain (single color) — applied to the whole page without altering layout.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For any page in 1–604, the words shown, their line grouping, and their reading order exactly match the standard printed Mushaf for that page (100% correspondence on spot-checked pages).
- **SC-002**: A reader can turn from one page to the next in under 1 second on a typical device, with the correct next page shown.
- **SC-003**: A reader can navigate from page 1 to page 604 (and back) entirely by swiping, in the correct right-to-left order, with the page indicator always matching the visible page.
- **SC-004**: Toggling Tajweed coloring updates the visible page within a fraction of a second and produces zero measurable change in word positions or line layout.
- **SC-005**: During word-by-word highlighting, the page maintains smooth motion (target 60 frames per second) with no perceptible stutter or flicker.
- **SC-006**: Rapid swiping across at least 50 consecutive pages completes without a crash, freeze, or out-of-memory failure, and with stable memory usage.
- **SC-007**: All feature text and controls render correctly and completely in both English and Arabic, with correct right-to-left layout.

## Assumptions

- The complete, pre-built page/line/word content for all 604 pages of the standard KFGQPC Mushaf is packaged with the app and available offline; no network access is required to read.
- The visual identity of each word (its shape and, in Tajweed mode, its coloring) is supplied by the packaged Mushaf assets; the app selects and displays them rather than computing colors itself.
- Two visually identical presentations of every page are available — one color-coded (Tajweed) and one plain — differing only in color, so switching between them never changes layout.
- The standard Mushaf targeted here is the 604-page KFGQPC edition; other Mushaf editions or page counts are out of scope for this feature.
- Audio recitation and speech-follow-along are future features; this specification delivers the reading surface and highlighting engine (with a driver-agnostic interface) plus a stand-in trigger that emulates the audio driver — not real audio playback or speech recognition.
- The reader's Tajweed setting and last-viewed page are persisted locally on the device; multi-user, accounts, and cross-device syncing are out of scope here.
- Direct jump-to-page navigation is out of scope; readers move only by swiping in this feature.
- Bookmarks, search, and navigation-by-surah/juz are out of scope for this feature and may be addressed separately.
