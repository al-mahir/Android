# Al-Mahir — Agent & Contributor Guide

This file is the **single source of truth** for how code is written in this repository.
All agent guides (including `CLAUDE.md`) point here. When any other doc conflicts with
this file, the **[Constitution](.specify/memory/constitution.md)** prevails, and this file
MUST be kept consistent with it.

- **Project**: Al-Mahir (Quran / Mushaf Android app)
- **Constitution**: [.specify/memory/constitution.md](.specify/memory/constitution.md) (v1.0.0)
- **Locales**: Arabic (`ar`) + English (`en`) — Arabic-first, RTL-aware

---

## Architecture Rules

### 1. Clean Architecture & Layer Boundaries

Three concentric layers; dependencies point **inward only** (`presentation → domain ← data`).

- `domain` — pure Kotlin, framework-agnostic. Holds entities, use cases, and repository
  **interfaces**. MUST NOT depend on `data`, `presentation`, Android framework, Ktor, or Room.
- `data` — implements `domain` repository interfaces; owns Ktor (remote) and Room (local)
  data sources; maps DTO/Room models → domain models. MUST NOT depend on `presentation`.
- `presentation` — depends only on `domain` via use cases. MUST NOT reference `data`, DTOs,
  Room entities, or Ktor types.
- ❌ Never expose a Room `@Entity` or Ktor response type to the UI. Cross-layer leakage is a
  hard violation.

### 2. Modular Architecture (Gradle modules, not packages)

- Baseline modules: `:app`, `:domain`, `:data`, `:presentation`.
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
- Business decisions live in **use cases** (`domain`), not Composables.

---

## Mandated Technology Stack

Use the standard tool for each concern; alternatives require a constitution amendment.

| Concern            | Standard                                  |
|--------------------|-------------------------------------------|
| UI                 | Jetpack Compose (no XML for new screens)  |
| Navigation         | Navigation 3                              |
| Dependency Injection | Koin (provide via Koin modules)         |
| Networking         | Ktor client (confined to `data`)          |
| Local persistence  | Room (confined to `data`)                 |
| Async / reactive   | Kotlin Coroutines + Flow                  |
| Language           | Kotlin only; latest stable AndroidX/Compose BOM |

---

## Localization (i18n) — required for beta

- ❌ **No hardcoded user-facing strings** in Composables or ViewModels. Every UI string comes
  from a string resource.
- Keep `values/` (English) and `values-ar/` (Arabic) **complete**. A new user-facing string
  MUST be added to **all** locales in the same change.
- Layouts MUST be **RTL-aware**: use start/end (never left/right); verify Arabic RTL rendering.
- Locale-sensitive data (numbers, dates, plurals) uses **locale-aware formatting**, not manual
  string concatenation.

---

## Additional Standards

- **Immutability**: domain and state models are immutable (`data class` with `val`).
- **Best practices**: follow official Android/Compose guidance — stable Composable params,
  `remember` / `derivedStateOf` where appropriate, lifecycle-aware collection, avoid
  unnecessary recomposition.
- **Error handling**: data sources surface failures as typed results/domain errors; the UI
  renders explicit **loading / empty / error** states.
- **Resources**: strings, dimensions, and colors are defined as resources, not inline literals.
- **No secrets in VCS**: API keys/endpoints MUST NOT be committed in plaintext source.

---

## Quality Gates (before merge)

1. ✅ Compiles and passes lint.
2. ✅ Layer boundaries and dependency direction respected (no illegal module deps).
3. ✅ Presentation follows MVI shape.
4. ✅ Correct stack usage (Compose / Nav 3 / Koin / Ktor / Room).
5. ✅ New user-facing text present in **both** English and Arabic resources.
6. ✅ `domain` use cases and reducer/state logic covered by unit tests where practical
   (pure `domain` tests MUST NOT require the Android framework).
7. ✅ Added complexity (extra modules/patterns) is justified — prefer the simplest solution
   consistent with these rules.

---

## Spec Kit workflow

This repo uses Spec Kit. Typical flow: `/speckit-specify` → `/speckit-plan` →
`/speckit-tasks` → `/speckit-implement`. The plan's **Constitution Check** gate is evaluated
against [.specify/memory/constitution.md](.specify/memory/constitution.md).
