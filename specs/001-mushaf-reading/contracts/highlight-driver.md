# Contract: Highlight Driver (audio-ready interface)

Decouples the *source* of "which word is being recited" from the reading surface so the
future audio/STT engine drops in without UI changes (FR-012, FR-012a; research R8).

## Interface (`:mushaf:presentation/highlight` or `:mushaf:domain`)

```kotlin
interface HighlightDriver {
    /** Stream of the word currently being recited on the active page; null clears. */
    val currentWordId: Flow<String?>
    fun start(page: MushafPage)   // begin driving highlights for this page
    fun stop()                    // halt; emits null
}
```

- The ViewModel collects `currentWordId` and folds each emission into
  `MushafIntent.HighlightWord`. The driver never touches Compose or state directly.

## Stand-in implementation — `SimulatedHighlightDriver` (this feature)

- Walks `page.lines.flatMap { it.words }` in reading order, emitting each `word.id` on a
  fixed cadence (~500 ms), then completes (or loops until `stop()`).
- Runs on a coroutine scope; cancellation on `stop()` emits `null`.
- Purpose: exercise and validate the follow-along experience at 60 fps before audio ships
  (SC-005). It is the concrete driver behind `StartFollowAlongPreview`.

## Future implementation — `AudioHighlightDriver` (out of scope here)

- Same interface; maps audio/STT timing → `wordId`. No change to `MushafScreen`,
  `MushafUiState`, or the reducer — only the bound `HighlightDriver` instance changes (DI).

## Guarantees

| Guarantee | Requirement |
|-----------|-------------|
| One highlighted word at a time; advances in reading order | FR-010 |
| Only the highlighted word recomposes; layout stable | FR-011, SC-005 |
| Driver swappable without editing the reading surface | FR-012a |
| Stand-in trigger available now | FR-012 |
