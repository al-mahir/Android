# Quickstart & Validation Guide: Mushaf Reading

How to build, run, and validate the feature end-to-end. Details live in
[data-model.md](./data-model.md) and [contracts/](./contracts/); this is the run/verify
guide, not implementation.

## Prerequisites

- Android Studio (matching AGP 9.2.1 / Kotlin 2.2.10), a device/emulator on API 24+.
- Packaged assets already present in `:mushaf:data`:
  - `assets/databases/mushaf_v4_layout.db`
  - `assets/fonts/standard/p{1..604}.ttf` and `assets/fonts/tajweed/p{1..604}.ttf`
- Version catalog updated with Room (+KSP), Koin, Navigation 3, DataStore, Coroutines
  (see plan Structure Decision + research R9).

## Build & run

```powershell
# From repo root
./gradlew :app:assembleDebug            # build
./gradlew :app:installDebug             # install on connected device/emulator
# Launch the app; navigate to the Mushaf reading screen.
```

Fast module checks while developing:

```powershell
./gradlew :mushaf:domain:test           # pure-Kotlin unit tests (grouping, expansion, reducer)
./gradlew :mushaf:data:connectedAndroidTest   # Room DAO reads packaged DB
./gradlew :mushaf:presentation:connectedAndroidTest  # Compose UI tests
```

## One-time verification checks (from research VERIFY items)

1. **Glyph mapping (R2)**: On first render of page 1, confirm all 36 words show correct
   glyphs. If mis-mapped, inspect a font `cmap` (`fontTools` `getBestCmap()` on
   `standard/p1.ttf`) and correct `GlyphCodeResolver` (single point of change).
2. **Heap stability (R4)**: With Android Studio Profiler, rapid-swipe 50 pages and confirm
   memory stays bounded (SC-006).

## Validation scenarios (map to spec)

| # | Scenario | Steps | Expected | Spec |
|---|----------|-------|----------|------|
| 1 | Print-faithful page | Open page 1 | Al-Fatihah renders: surah-name line + ayah lines, correct order, ≤15 lines centered | US1 / FR-001, FR-002, SC-001 |
| 2 | Specific page | Open page 604 | Correct final page content shown | FR-003 |
| 3 | Loading state | Open a fresh page | Loading indicator shows, then page appears | FR-013 |
| 4 | RTL swipe | Swipe from page 1 | Advances to page 2; page 1 was on the right | US2 / FR-004, FR-005 |
| 5 | Page indicator sync | Swipe several pages | Indicator matches visible page | FR-006 |
| 6 | Rapid swipe stability | Fast-swipe 50+ pages | No crash/freeze/OOM; memory stable | FR-014, SC-006 |
| 7 | Bounds | Swipe back from p1 / forward from p604 | Stays on p1 / p604, no error | FR-015 |
| 8 | Tajweed toggle no-shift | Toggle off, then on | Color changes; zero word/line position change | US3 / FR-008, FR-009, SC-004 |
| 9 | Tajweed persistence | Toggle off, kill app, relaunch | Reopens with Tajweed still off | FR-007a |
| 10 | Resume last page | Go to page 50, kill app, relaunch | Reopens at page 50 | FR-006a |
| 11 | Word highlight isolation | Start follow-along preview | One word highlighted at a time, advances in reading order; only that word changes | US4 / FR-010, FR-011 |
| 12 | 60 fps follow-along | Run preview across a page | Smooth, no stutter/flicker/font reload | FR-012, SC-005 |
| 13 | Localization + RTL | Switch device locale EN↔AR | All labels/messages localized; layout RTL-correct | FR-016, SC-007 |

## Definition of done (feature-level)

- All 13 scenarios pass on a physical device.
- Both `VERIFY` checks (glyph mapping, heap) confirmed.
- `:mushaf:domain` unit tests green; DAO + Compose tests green.
- Constitution gates (plan) still pass post-implementation; EN + AR strings complete.
