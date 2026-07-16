# Contract: Domain Use Cases (`:mushaf:domain`)

Business decisions live here, not in Composables or the ViewModel (Constitution I & III).
Each use case is a single-responsibility `operator fun invoke`.

## `GetPageUseCase`

```kotlin
class GetPageUseCase(private val repo: MushafRepository) {
    /** Clamps page to 1..604, then returns the page stream. */
    operator fun invoke(pageNumber: Int): Flow<MushafPage>
}
```
- Clamps out-of-range requests to the valid bound (FR-015).
- Delegates ordering/expansion to the repository/mapper.

## `ObserveReaderPreferencesUseCase`

```kotlin
class ObserveReaderPreferencesUseCase(private val repo: ReaderPreferencesRepository) {
    operator fun invoke(): Flow<ReaderPreferences>   // tajweedEnabled + lastPage
}
```
- Seeds initial UI state (resume page + Tajweed mode) on launch (FR-006a, FR-007a).

## `SetTajweedEnabledUseCase`

```kotlin
class SetTajweedEnabledUseCase(private val repo: ReaderPreferencesRepository) {
    suspend operator fun invoke(enabled: Boolean)
}
```
- Persists the toggle (FR-007a). UI reflects the change by swapping font dir (no reload).

## `SaveLastPageUseCase`

```kotlin
class SaveLastPageUseCase(private val repo: ReaderPreferencesRepository) {
    suspend operator fun invoke(page: Int)           // page clamped 1..604
}
```
- Called when a swipe settles on a page (FR-006a).

## Requirement traceability

| Use case | Requirements |
|----------|--------------|
| `GetPageUseCase` | FR-001, FR-003, FR-015, SC-001 |
| `ObserveReaderPreferencesUseCase` | FR-006a, FR-007, FR-007a |
| `SetTajweedEnabledUseCase` | FR-007a, FR-009 |
| `SaveLastPageUseCase` | FR-006a |
