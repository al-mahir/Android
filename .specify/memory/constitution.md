<!--
SYNC IMPACT REPORT
==================
Version change: (uninitialized template) → 1.0.0
Rationale: Initial ratification of the project constitution. MINOR/PATCH not
applicable; first concrete version replacing the placeholder template.

Modified principles: (none — initial definition)
Added principles:
  - I. Clean Architecture & Layer Boundaries
  - II. Modular Architecture
  - III. MVI Unidirectional Data Flow (Presentation)
  - IV. Mandated Technology Stack
  - V. Complete Localization (i18n)
Added sections:
  - Additional Constraints & Technology Standards
  - Development Workflow & Quality Gates
Removed sections: (none)

Templates requiring updates:
  - .specify/templates/plan-template.md ✅ reviewed — Constitution Check gate is
    generic ("[Gates determined based on constitution file]"); no edit required.
  - .specify/templates/spec-template.md ✅ reviewed — no principle-driven mandatory
    section added/removed; no edit required.
  - .specify/templates/tasks-template.md ✅ reviewed — task categories remain
    principle-compatible (localization/modularization surface as feature tasks);
    no edit required.
  - .specify/templates/checklist-template.md ✅ reviewed — no edit required.

Follow-up TODOs: (none)
-->

# Al-Mahir Constitution

## Core Principles

### I. Clean Architecture & Layer Boundaries

The codebase MUST follow Clean Architecture with three concentric layers: `domain`,
`data`, and `presentation`. Dependencies MUST point inward only.

- `domain` MUST be a pure Kotlin/Android-agnostic layer containing entities, use cases,
  and repository interfaces. It MUST NOT depend on `data`, `presentation`, Android
  framework classes, Ktor, Room, or any UI type.
- `data` MUST implement the repository interfaces defined in `domain`, own all remote
  (Ktor) and local (Room) data sources, and map data/DTO models to domain models. It MUST
  NOT depend on `presentation`.
- `presentation` MUST depend only on `domain` (via use cases). It MUST NOT reference
  `data`, DTOs, Room entities, or Ktor types directly.
- Cross-layer leakage (e.g., exposing a Room `@Entity` or Ktor response to the UI) is a
  violation and MUST be rejected in review.

**Rationale**: Inward-only dependencies keep business logic testable in isolation, allow
data sources to be swapped, and prevent framework churn from rippling through the app.

### II. Modular Architecture

The project MUST be organized as separate Gradle modules, not source packages, to enforce
boundaries at compile time.

- Baseline modules: `:app`, `:domain`, `:data`, `:presentation`.
- `:app` MUST own navigation wiring, the application/entry-point code, and dependency
  graph assembly; it MUST contain minimal feature logic.
- The Mushaf feature is large and MUST be split into its own module set mirroring the
  layered structure (e.g., `:mushaf:domain`, `:mushaf:data`, `:mushaf:presentation`) rather
  than living inside the baseline modules.
- A module MUST NOT create a dependency cycle. Feature modules MUST NOT depend on `:app`.
- Any new large feature SHOULD follow the same per-feature layered-module split.

**Rationale**: Module boundaries make illegal dependencies fail to compile, enable
parallel work and faster incremental builds, and keep the large Mushaf feature isolated.

### III. MVI Unidirectional Data Flow (Presentation)

All presentation logic MUST use the MVI pattern with a strictly unidirectional data flow.

- Each screen/feature MUST expose a single immutable `State`, accept user `Intent`s
  (events), and emit one-off `Effect`s (navigation, toasts) separately from state.
- State MUST flow one direction: `Intent → ViewModel/reducer → State → UI`. The UI MUST be
  a pure function of `State` and MUST NOT hold mutable business state.
- State MUST be exposed as an observable stream (e.g., `StateFlow`); Composables MUST
  render from collected state and dispatch intents, never mutate state directly.
- Business decisions MUST live in use cases (`domain`), not in Composables.

**Rationale**: A single source of truth with unidirectional flow makes UI behavior
predictable, reproducible, and testable, and eliminates a whole class of state bugs.

### IV. Mandated Technology Stack

The following stack is standard and MUST be used for its concern; alternatives require a
documented amendment.

- **UI**: Jetpack Compose (no XML layouts for new screens).
- **Navigation**: Navigation 3.
- **Dependency Injection**: Koin. Dependencies MUST be provided via Koin modules, not
  constructed ad hoc.
- **Networking**: Ktor client (confined to `data`).
- **Local persistence**: Room (confined to `data`).
- **Async**: Kotlin Coroutines and Flow for all asynchronous and reactive work.

**Rationale**: A fixed stack keeps modules interoperable, reduces onboarding cost, and
concentrates framework knowledge instead of fragmenting it across ad hoc choices.

### V. Complete Localization (i18n)

The app MUST fully support Arabic and English for the beta release, and remain
localization-ready thereafter.

- Every user-facing UI string MUST come from a localized string resource. Hardcoded
  display strings in Composables or ViewModels are a violation and MUST be rejected.
- Both `values/` (English) and `values-ar/` (Arabic) resources MUST be kept complete; a
  new user-facing string MUST be added to all supported locales in the same change.
- Layouts MUST be RTL-aware: use start/end (not left/right), and verify Arabic RTL
  rendering.
- Locale-sensitive data (numbers, dates, plurals) MUST use locale-aware formatting, not
  manual string concatenation.

**Rationale**: Al-Mahir serves an Arabic-first audience; treating localization and RTL as
first-class from beta prevents costly retrofits and broken layouts.

## Additional Constraints & Technology Standards

- **Language**: Kotlin only for application code; latest stable AndroidX/Compose BOM.
- **Best practices**: Follow official Android and Jetpack Compose guidance — immutable
  state, stable Composable parameters, `remember`/`derivedStateOf` where appropriate,
  lifecycle-aware collection, and avoidance of unnecessary recomposition.
- **Immutability**: Domain and state models MUST be immutable (`data class` with `val`).
- **Error handling**: Data sources MUST surface failures as typed results/domain errors;
  the UI MUST render explicit loading, empty, and error states.
- **No secrets in VCS**: API keys and endpoints MUST NOT be committed in plaintext source.
- **Resource discipline**: Strings, dimensions, and colors MUST be defined as resources,
  not inline literals in UI code.

## Development Workflow & Quality Gates

- **Build gate**: The project MUST compile and pass lint before merge. Module boundary
  and dependency-direction violations MUST fail review.
- **Review gate**: Every change MUST be reviewed against this constitution. Reviewers MUST
  confirm layer boundaries (Principle I), module placement (II), MVI shape (III), correct
  stack usage (IV), and localization completeness (V).
- **Testing**: `domain` use cases and reducer/state logic SHOULD be covered by unit tests;
  tests MUST NOT require Android framework for pure `domain` code.
- **Localization gate**: Any change adding user-facing text MUST include the string in both
  English and Arabic resources before merge.
- **Complexity**: Added architectural complexity (extra modules, new patterns) MUST be
  justified; the simplest solution consistent with the principles is preferred.

## Governance

This constitution supersedes ad hoc conventions and other practices where they conflict.

- **Amendments**: Changes to principles or mandated stack MUST be proposed as a documented
  amendment (what changes, why, and migration impact) and approved before adoption.
- **Versioning**: This document follows semantic versioning — MAJOR for
  backward-incompatible principle removals/redefinitions, MINOR for new principles or
  materially expanded guidance, PATCH for clarifications and non-semantic refinements.
- **Compliance**: All PRs and reviews MUST verify compliance with the principles above.
  Justified, temporary deviations MUST be recorded in the plan's Complexity Tracking.
- **Runtime guidance**: Agent- and contributor-facing guidance files MUST stay consistent
  with this constitution; on conflict, this constitution prevails.

**Version**: 1.0.0 | **Ratified**: 2026-07-15 | **Last Amended**: 2026-07-15
