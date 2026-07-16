# Contract: Domain Repository Interfaces (`:mushaf:domain`)

Framework-free interfaces owned by `domain`, implemented in `:mushaf:data`. No Room/DataStore
types appear in signatures (Constitution I). All async work is Coroutines/Flow.

## `MushafRepository`

```kotlin
interface MushafRepository {
    /** Emits the fully-assembled page (lines + expanded words). Cold flow; one page. */
    fun getPage(pageNumber: Int): Flow<MushafPage>

    /** Total pages in the mounted Mushaf (expected 604, from `info`). */
    suspend fun getPageCount(): Int
}
```

**Behavior**
- `pageNumber` outside `1..getPageCount()` → the use case clamps before calling; repo may
  additionally reject with a typed failure (see Error handling).
- Ordering guaranteed: lines by `lineNumber` ASC, words by `positionInLine` ASC (SC-001).
- Read-only. No write path to the layout DB.

## `ReaderPreferencesRepository`

```kotlin
interface ReaderPreferencesRepository {
    val preferences: Flow<ReaderPreferences>          // emits on every change
    suspend fun setTajweedEnabled(enabled: Boolean)   // FR-007a
    suspend fun setLastPage(page: Int)                // FR-006a (page clamped 1..604)
}
```

**Behavior**
- First emission provides defaults if unset: `tajweedEnabled = true`, `lastPage = 1`.
- Writes are last-write-wins; concurrent toggles resolve to the latest value.

## Error handling contract

Data failures (corrupt/missing asset DB, read error) surface as a **typed domain result**,
not raw exceptions, so the UI can render an explicit error state (Constitution: typed
results). Recommended shape:

```kotlin
sealed interface PageResult {
    data class Success(val page: MushafPage) : PageResult
    data class Failure(val reason: PageError) : PageResult   // NotFound, DataUnavailable
}
```

`getPage` MAY return `Flow<PageResult>` instead of `Flow<MushafPage>` if the team prefers
explicit failure in the stream; either is acceptable provided the UI can distinguish
loading / success / error (FR-013).
