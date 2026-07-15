# CLAUDE.md

**Single source of truth: [AGENTS.md](AGENTS.md).** Read it first and follow it for every
change in this repository — architecture, module boundaries, MVI, the mandated stack, and
localization rules all live there.

The binding rules are governed by the
**[Constitution](.specify/memory/constitution.md)** (v1.0.0). If anything here or in
`AGENTS.md` conflicts with the constitution, the constitution prevails.

## Quick reference (see AGENTS.md for the full rules)

- **Clean Architecture**: deps point inward — `presentation → domain ← data`. `domain` is
  pure Kotlin; never leak Room/Ktor/DTO types into the UI.
- **Modules** (Gradle, not packages): `:app`, `:domain`, `:data`, `:presentation`; the large
  Mushaf feature is split into `:mushaf:domain`, `:mushaf:data`, `:mushaf:presentation`.
  `:app` = navigation + entry point + DI assembly only. Feature modules never depend on `:app`.
- **MVI** (presentation): immutable State, Intents, one-off Effects; `Intent → reducer →
  State → UI`; UI is a pure function of State.
- **Stack**: Jetpack Compose · Navigation 3 · Koin (DI) · Ktor (in `data`) · Room (in `data`)
  · Coroutines/Flow · Kotlin only.
- **Localization**: no hardcoded UI strings; keep English + Arabic resources complete in the
  same change; RTL-aware layouts (start/end, not left/right).
- **Before merge**: compiles + lint clean, boundaries respected, MVI shape, correct stack,
  new text in both locales, `domain`/reducer logic unit-tested where practical.
