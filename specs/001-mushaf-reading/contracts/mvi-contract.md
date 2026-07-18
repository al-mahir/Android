# Contract: MVI Presentation (`:mushaf:presentation`)

Single immutable `State` via `StateFlow`, `Intent` events in, one-off `Effect`s out; UI is a
pure function of state (Constitution III). `MushafViewModel` reduces intents using the domain
use cases.

## State

```kotlin
data class MushafUiState(
    val currentPage: Int = 1,
    val page: MushafPage? = null,          // lines + words for currentPage
    val isTajweedEnabled: Boolean = true,  // default ON (FR-007)
    val highlightedWordId: String? = null, // FR-010/FR-011
    val isLoading: Boolean = true,         // FR-013
    val error: MushafError? = null,        // explicit error state
    val pageCount: Int = 604,
)
```

- UI renders solely from this. `isTajweedEnabled` selects the font directory; it never
  changes layout (FR-008/FR-009).
- `highlightedWordId` is read at the word level so a change recomposes only the affected
  word (SC-005).

## Intents

```kotlin
sealed interface MushafIntent {
    data class LoadPage(val page: Int) : MushafIntent          // FR-004, FR-006
    data class ToggleTajweed(val enabled: Boolean) : MushafIntent // FR-007, FR-009
    data class HighlightWord(val wordId: String?) : MushafIntent  // FR-010 (null clears)
    data object StartFollowAlongPreview : MushafIntent         // FR-012 stand-in trigger
    data object StopFollowAlongPreview : MushafIntent
    data object Retry : MushafIntent                            // recover from error
}
```

## Effects (one-off)

```kotlin
sealed interface MushafEffect {
    data class ShowMessage(val resId: Int) : MushafEffect      // localized (Constitution V)
}
```

Navigation is handled by `:app` (Navigation 3); this screen emits no nav effects for v1.

## Reducer behavior

| Intent | Effect on state | Side effect |
|--------|-----------------|-------------|
| `LoadPage(p)` | `currentPage=p`, `isLoading=true`, clear `highlightedWordId` | `GetPageUseCase(p)` → on success set `page`, `isLoading=false`; on failure set `error`; `SaveLastPageUseCase(p)` |
| `ToggleTajweed(e)` | `isTajweedEnabled=e` (no `page` reload) | `SetTajweedEnabledUseCase(e)` |
| `HighlightWord(id)` | `highlightedWordId=id` | none (word-level recompose only) |
| `StartFollowAlongPreview` | — | subscribe `SimulatedHighlightDriver`, forwards `HighlightWord` ticks |
| `StopFollowAlongPreview` | `highlightedWordId=null` | cancel driver subscription |
| `Retry` | `error=null`, `isLoading=true` | re-dispatch `LoadPage(currentPage)` |

## Initialization

On `init`, collect `ObserveReaderPreferencesUseCase()`; seed `isTajweedEnabled` and open at
`lastPage` (resume — FR-006a). Pager `currentPage` is synced via
`LaunchedEffect(pagerState.currentPage)` → `LoadPage`.

## UI contract (Compose)

- `HorizontalPager(pageCount = state.pageCount)` inside `LayoutDirection.Rtl` (FR-005).
- Per page: `Column(SpaceEvenly, CenterHorizontally)` of line slots; each `AYAH` line is a
  RTL `FlowRow` of per-word `Text(glyph, fontFamily = pageFont)`; `SURAH_NAME`/`BASMALLAH`
  render as a single centered line glyph.
- Highlighted word: `Modifier.background(highlightColor)` when `word.id ==
  highlightedWordId` — no `SpanStyle`/text-color change (FR-011).
- Explicit `isLoading` and `error` composables; all strings from resources (EN + AR).
