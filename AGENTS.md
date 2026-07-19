# Al-Mahir — Agent & Contributor Guide

This file is the **single source of truth** for *how* code is written in this repository and
the **map** to everything else. It stays deliberately lean: the binding engineering rules live
here; the per-feature detail lives in [`docs/features/`](docs/features/) and the design-system
catalog lives in [`designsystem/README.md`](designsystem/README.md). All agent guides
(including `CLAUDE.md`) point here. When any other doc conflicts with this file, the
**[Constitution](.specify/memory/constitution.md)** prevails, and this file MUST be kept
consistent with it.

- **Project**: Al-Mahir (Quran / Mushaf Android app)
- **Constitution**: [.specify/memory/constitution.md](.specify/memory/constitution.md) (v1.0.0)
- **Product spec (canonical requirements)**: [docs/Al-Mahir-SDD.md](docs/Al-Mahir-SDD.md)
- **Locales**: Arabic (`ar`) + English (`en`) — Arabic-first, RTL-aware

> **How to use this repo's docs:** read this file for the rules → open the relevant
> [feature brief](#feature-documentation-index) for what to build and the must-know
> constraints → consume [`:designsystem`](#design-system-designsystem) for the UI. The SDD is
> the product source of truth; feature briefs are the agent build sheets.

---

## Architecture Rules

### 1. Clean Architecture & Layer Boundaries

Three concentric layers; dependencies point **inward only** (`presentation → domain ← data`).

- `domain` — pure Kotlin, framework-agnostic. Holds entities, use cases, and repository
  **interfaces**. MUST NOT depend on `data`, `presentation`, Android framework, Ktor, or Room.
- `data` — implements `domain` repository interfaces; owns Ktor (remote) and Room (local)
  data sources; maps DTO/Room models → domain models. MUST NOT depend on `presentation`.
- `presentation` — depends only on `domain` via use cases, and on `:designsystem` for UI.
  MUST NOT reference `data`, DTOs, Room entities, or Ktor types.
- ❌ Never expose a Room `@Entity` or Ktor response type to the UI. Cross-layer leakage is a
  hard violation.

### 2. Modular Architecture (Gradle modules, not packages)

- Baseline modules: `:app`, `:domain`, `:data`, `:presentation`.
- Shared UI module: **`:designsystem`** — every visual primitive (tokens + components).
  Consumed by presentation layers; it MUST NOT depend on any feature/`domain`/`data` module.
- `:app` owns navigation wiring, the entry point, and DI graph assembly — **minimal** feature
  logic.
- The **Mushaf** feature is large → its own layered module set:
  `:mushaf:domain`, `:mushaf:data`, `:mushaf:presentation` (not inside the baseline modules).
- ❌ No dependency cycles. ❌ Feature modules MUST NOT depend on `:app`.
- Any new large feature SHOULD follow the same per-feature layered split.

### 3. MVI Unidirectional Data Flow (presentation)

- Each screen exposes a single immutable **State**, accepts **Intent**s, emits one-off
  **Effect**s (navigation/toasts) separately from state.
- Flow is one direction: `Intent → ViewModel/reducer → State → UI`.
- UI is a **pure function of State**. Composables render collected state (e.g. `StateFlow`)
  and dispatch intents — they never mutate business state directly.
- Business decisions live in **use cases** (`domain`), not Composables. A ViewModel depends on
  **use cases only** — never on a repository or a data source directly.

#### 3a. MVI plumbing via delegation (required)

State and effect plumbing is **not** hand-rolled per ViewModel. Every screen composes the two
shared contracts with Kotlin delegation, so each ViewModel contains only its own reducer:

```kotlin
class HomeViewModel(
    private val getHomeSummary: GetHomeSummaryUseCase,
) : ViewModel(),
    StateHolder<HomeUiState> by DefaultStateHolder(HomeUiState()),
    EffectPublisher<HomeEffect> by DefaultEffectPublisher() {

    fun onIntent(intent: HomeIntent) = when (intent) {
        HomeIntent.Retry -> load()
        HomeIntent.SearchClicked -> sendEffect(HomeEffect.OpenSearch)   // from EffectPublisher
    }

    private fun load() = updateState { copy(isLoading = true) }         // from StateHolder
}
```

- `StateHolder<S>` — exposes `state: StateFlow<S>`, `currentState`, and `updateState { }`.
  ❌ No bare `MutableStateFlow` / `_state` field in a ViewModel.
- `EffectPublisher<E>` — exposes `effect: Flow<E>` and `sendEffect(e)`, backed by a
  `Channel(BUFFERED)`. ❌ No bare `Channel` / `SharedFlow` effect field in a ViewModel.
- **Effects are collected only via `ObserveEffect(viewModel.effect) { … }`** — it is
  lifecycle-aware (`repeatOnLifecycle(STARTED)`) and runs on `Dispatchers.Main.immediate`.
  ❌ Never collect an effect flow in a bare `LaunchedEffect`; a navigation effect delivered to
  a stopped screen loses back-stack entries or crashes.
- Effects carry `@StringRes` ids, never resolved user-facing strings (see
  [Localization](#localization-i18n--required-for-beta)).
- Screens are split **stateful → stateless**: `FooScreen(viewModel)` collects state with
  `collectAsStateWithLifecycle()`, handles effects, and delegates to a `FooContent(state, …)`
  that takes the state plus lambdas and holds no state of its own.

**Where this code lives.** The three types are duplicated, intentionally, in each presentation
module's `core/mvi` package:

| Module | Package |
|--------|---------|
| `:presentation` | `com.iti.presentation.core.mvi` |
| `:mushaf:presentation` | `com.example.mushaf.presentation.core.mvi` |

They are sibling modules with no dependency between them, and a presentation module MUST NOT
depend on another presentation module. The copies are ~30 lines each and **MUST stay
identical** — change one, change the other in the same commit. A new layered feature module
gets its own `core/mvi` copy the same way.

### 3b. Model the business, never the screen (`domain` / `data`)

`domain` and `data` describe **what the app is**, not **what a screen shows**. A screen is one
consumer of the business layer; it must never dictate its shape.

- ❌ **No screen names below `presentation`.** No `model/home/`, `dto/home/`, `HomeDataSource`,
  `HomeSummaryDto`, `GetHomeSummaryUseCase`. If deleting a screen would force a rename in
  `domain` or `data`, the layering is wrong.
- **One file per entity**, named for the business concept: `User`, `Sheikh`, `StudyCircle`,
  `ReadingProgress` — not one file bundling "everything screen X needs".
- **DTOs are per resource** (`UserDto`, `SheikhDto`, …), so any endpoint that embeds a user
  reuses `UserDto`. ❌ No screen-shaped envelope DTO.
- **Data sources expose one member per endpoint.** A screen showing four sections reads four
  streams; it does not get a bespoke `observeXScreen()` that bundles them.
- **Repositories are capability-oriented** — one member per resource, plus writes. Adding a
  section to a screen must not change a repository contract.
- **Use cases are single-purpose** and own product rules (ordering, filtering, validation), so
  every surface inherits the same behaviour. ❌ No `GetEverythingForScreenXUseCase`.
- **The screen aggregate lives in `presentation`.** Combine the streams in the ViewModel into
  that screen's UI state; if a typed carrier is needed for `combine`, keep it `internal` to the
  feature package — it is not a domain model.

### 4. Design System First (UI)

- Presentation modules build UI **only** from `:designsystem` components and `Theme.*` tokens
  — see [Design System](#design-system-designsystem).
- ❌ No raw `Color(...)`, `dp`, `sp`, or ad-hoc Material components with hardcoded styling in
  feature code. If a token or component is missing, add it to `:designsystem` first, then use
  it — do not bypass the theme.

---

## Mandated Technology Stack

Use the standard tool for each concern; alternatives require a constitution amendment.

| Concern              | Standard                                       |
|----------------------|------------------------------------------------|
| UI                   | Jetpack Compose (no XML for new screens)       |
| Design system        | `:designsystem` module (tokens + components)   |
| Navigation           | Navigation 3                                   |
| Dependency Injection | Koin (provide via Koin modules)                |
| Networking           | Ktor client (confined to `data`)               |
| Local persistence    | Room (confined to `data`)                      |
| Async / reactive     | Kotlin Coroutines + Flow                       |
| Animations (loaders) | Lottie (exposed via `:designsystem`)           |
| Language             | Kotlin only; latest stable AndroidX/Compose BOM |

---

## Design System (`:designsystem`)

A standalone `com.android.library` + Compose module (`com.example.designsystem`) that owns
every visual primitive: color/typography/dimension/shape tokens, locale + RTL handling, and
the reusable components built on them. Feature UI inherits light/dark and LTR/RTL behavior for
free by consuming it.

**Full catalog & usage guide:** [designsystem/README.md](designsystem/README.md). The rules
below are the non-negotiables; the README is the deep reference (it may lag the module source —
when in doubt, check `designsystem/src`).

**Golden rules**

1. Wrap UI trees once with `SpTheme(isDarkTheme, locale) { … }` (installs locale, layout
   direction, colors, typography, spacing, shapes, fonts for that subtree).
2. Read every color/size/spacing/shape/text style through the `Theme` accessor
   (`Theme.colors.*`, `Theme.typography.*`, `Theme.spacing.*`, `Theme.shapes.*`, `Theme.size.*`).
   Never hardcode design values in feature code.
3. Prefer an existing component. Missing one? Add it under `designsystem/components/<name>/`
   with a `*Preview.kt` (Light/Dark × LTR/RTL) — never fork a styled component into a feature
   module.
4. ⚠️ **Qur'an text is exempt from the brand font.** Mushaf glyph rendering MUST use the
   official QUL per-page fonts, never `:designsystem`'s `arabicFontFamily`. Tokens/spacing/
   colors still apply.

**Component catalog (from module source — the authoritative list):**

| Group | Components |
|-------|-----------|
| Buttons | `PrimaryButton`, `SecondaryButton`, `IconButton` (loaders via Lottie; `height`/`shape`/`captionStyle` params for compact inline variants) |
| Inputs | `TextField`, `OtpField`, `SearchBar`, `ClickableSearchBar`, `SearchOverlapHeader` |
| Identity & meta | `InitialsAvatar`, `SectionHeader`, `StatusDot`, `StatusLabel`, `RatingLabel` |
| Cards | `SectionedCard`, `ExpandableSection`, `ExpandableAccentCard`, `InnerContentCard`, `SettingsActionCard` |
| Nav & bars | `AppBottomNavBar`, `BackTitleTopBar`, `BackTitleNotificationTopBar`, `AppBottomSheet` |
| Tabs | `DayTabRow`, `TabSelector` |
| Feedback | `StatusOverlay` (`Loading`/`Success`/`Error`), `Shimmer` (loading), `AccentBullet` |
| Placeholders | `NetworkErrorScreen`, `EmptySearchScreen`, `EmptyDataScreen`, `StandardEmptyState` |

> Note: individual feature briefs may say "needs new `:designsystem` component" for a name that
> already exists in source but is undocumented in the README — treat this table as the source
> of truth for what already exists, and genuinely-absent items (charts, audio player bar,
> inline mistake highlight, mic pre-prompt, etc.) as real gaps to build.

---

## Localization (i18n) — required for beta

- ❌ **No hardcoded user-facing strings** in Composables or ViewModels. Every UI string comes
  from a string resource.
- Keep `values/` (English) and `values-ar/` (Arabic) **complete**. A new user-facing string
  MUST be added to **all** locales in the same change.
- Layouts MUST be **RTL-aware**: use start/end (never left/right); verify Arabic RTL rendering.
  `SpTheme` sets layout direction from the locale — rely on it, don't hardcode `Ltr`.
- Locale-sensitive data (numbers, dates, plurals, **Hijri**) uses **locale-aware formatting**,
  not manual string concatenation.

---

## Additional Standards

- **Immutability**: domain and state models are immutable (`data class` with `val`).
- **Best practices**: follow official Android/Compose guidance — stable Composable params,
  `remember` / `derivedStateOf` where appropriate, lifecycle-aware collection, avoid
  unnecessary recomposition.
- **Data flow shape**: features follow `data source → repository → use case → ViewModel → UI`.
  Every data source is declared as an **interface** in `:data` and bound in a Koin module, so a
  stand-in can be swapped for the real one without touching any layer above. While a backend is
  unfinished, bind a `*FakeDataSource` that returns realistic data (and realistic per-resource
  delays) — the screen then exercises the production path, not a special case, and going live is
  a one-line change to the binding. The app-wide non-Mushaf source/repository pair is
  **`AlmahirDataSource` / `AlmahirFakeDataSource` / `AlmahirRepository`**.
- **Error handling**: data sources surface failures as typed results/domain errors; the UI
  renders explicit **loading / empty / error** states (use `Shimmer`, placeholder screens,
  and `StatusOverlay`).
- **Resources**: strings, dimensions, and colors are defined as resources/tokens, not inline
  literals.
- **No secrets in VCS**: API keys/endpoints MUST NOT be committed in plaintext source.
- **Cross-team contracts**: session/scoring data uses the shared `almahir.v1` ProtoBuf schema
  (SDD §4) — keep mobile models mapped to it, don't diverge.

---

## Feature Documentation Index

Each epic has a build brief under [`docs/features/`](docs/features/): must-know constraints,
capability table, `:designsystem` mapping, data/offline notes, and applicable NFRs. The
canonical product requirements remain in [docs/Al-Mahir-SDD.md](docs/Al-Mahir-SDD.md); the
cross-cutting NFRs live in [SDD §3](docs/Al-Mahir-SDD.md).

| # | Epic (code) | Priority | Brief |
|---|-------------|----------|-------|
| 1 | Authentication & User Management (AUTH) | MUST | [01-authentication.md](docs/features/01-authentication.md) |
| 2 | Profile & Account Management (PROF) | SHOULD | [02-profile-account.md](docs/features/02-profile-account.md) |
| 3 | Mushaf – Reading (MUS) | MUST | [03-mushaf-reading.md](docs/features/03-mushaf-reading.md) → deep: [Mushaf_ssd.md](docs/Mushaf_ssd.md) |
| 4 | Search & Index (SRCH) | SHOULD | [04-search-index.md](docs/features/04-search-index.md) |
| 5 | Listen Mode (LSN) | MUST | [05-listen-mode.md](docs/features/05-listen-mode.md) |
| 6 | Ta'ahud – Live AI Correction (TAH) | MUST | [06-taahud-live-correction.md](docs/features/06-taahud-live-correction.md) |
| 7 | Mu'allem – Repeat After the Sheikh (MLM) | SHOULD | [07-muallem-repeat.md](docs/features/07-muallem-repeat.md) |
| 8 | Ikhtibar – Exam (IKH) | SHOULD | [08-ikhtibar-exam.md](docs/features/08-ikhtibar-exam.md) |
| 9 | In-Session Feedback Bar (FBK) | MUST | [09-feedback-bar.md](docs/features/09-feedback-bar.md) |
| 10 | Bookmarks & Collections (BMK) | SHOULD | [10-bookmarks-collections.md](docs/features/10-bookmarks-collections.md) |
| 11 | Tafsir & Translation (TFS) | SHOULD | [11-tafsir-translation.md](docs/features/11-tafsir-translation.md) |
| 12 | Session History (HIS) | SHOULD | [12-session-history.md](docs/features/12-session-history.md) |
| 13 | Progress, Statistics & Gamification (PRG) | SHOULD | [13-progress-gamification.md](docs/features/13-progress-gamification.md) |
| 14 | AI Intelligence & Personalization (AI) | SHOULD | [14-ai-personalization.md](docs/features/14-ai-personalization.md) |
| 15 | Notifications (NTF) | SHOULD | [15-notifications.md](docs/features/15-notifications.md) |
| 16 | Settings (SET) | SHOULD | [16-settings.md](docs/features/16-settings.md) |
| 17 | Offline Support & Sync (OFF) | MUST | [17-offline-sync.md](docs/features/17-offline-sync.md) |
| 18 | Platform Integrations (INT) | COULD | [18-platform-integrations.md](docs/features/18-platform-integrations.md) |
| 19 | Community & Sharing (P2) | LATER | [19-community-sharing.md](docs/features/19-community-sharing.md) |

---

## Quality Gates (before merge)

1. ✅ Compiles and passes lint.
2. ✅ Layer boundaries and dependency direction respected (no illegal module deps).
3. ✅ Presentation follows MVI shape.
4. ✅ Correct stack usage (Compose / Nav 3 / Koin / Ktor / Room).
5. ✅ UI built from `:designsystem` components + `Theme.*` tokens — no hardcoded colors/dp/sp.
6. ✅ New user-facing text present in **both** English and Arabic resources; layout RTL-verified.
7. ✅ `domain` use cases and reducer/state logic covered by unit tests where practical
   (pure `domain` tests MUST NOT require the Android framework).
8. ✅ Added complexity (extra modules/patterns) is justified — prefer the simplest solution
   consistent with these rules.

---

## Spec Kit workflow

This repo uses Spec Kit. Typical flow: `/speckit-specify` → `/speckit-plan` →
`/speckit-tasks` → `/speckit-implement`. The plan's **Constitution Check** gate is evaluated
against [.specify/memory/constitution.md](.specify/memory/constitution.md).
