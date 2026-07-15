# 12. Session History — HIS

> **Product spec:** [Al-Mahir-SDD.md](../Al-Mahir-SDD.md) → "Epic 12: Session History". That file is the canonical product requirement; this doc is the agent build brief.
> **Rules:** [AGENTS.md](../../AGENTS.md) — architecture, modules, MVI, stack, localization (do not restate them here).

**Priority:** SHOULD · **Primary modules:** `:presentation`, `:domain`, `:data`

## Purpose
Detailed session reporting showing chronological progress, per-session mistake breakdowns, aggregated statistics, and improvement/regression trends across Ta'ahud, Exam, and Mu'allem sessions.

## Must-know constraints
- **Offline-first read (AVL-01/OFF-03):** the history list and details render from the local Room cache with no network dependency; background sync (HIS-10) pushes local edits and pulls cloud updates quietly, resolving conflicts. Never block the UI on the network.
- **Shared schema (ARC-02):** sessions are stored/synced as the `SessionLog` ProtoBuf from SDD §4 (`session_id`, `user_id`, `timestamp`, `target_range`, `final_accuracy`, `duration_seconds`, `feedback_records`). Mistake breakdowns come from `AyahFeedback.mistakes` grouped by `MistakeCategory` (memorization / tashkil / tajwid). Do not invent new fields.
- **AI confidence (ACC-02/AI-01):** the mistake bank (HIS-03) and detail views must surface each mistake's `confidence` and clearly mark `is_uncertain` entries — never present uncertain AI output as fact.
- **Locale-aware formatting (LOC-04):** all dates, durations, counts, scores, and streaks use locale-aware number/date formatting including Hijri options — never manual string concatenation; Arabic-first, RTL-aware.
- **GDPR delete (PRV-02/HIS-08):** deleting a session must hard-delete the record from both the local database and the remote server, with a confirmation prompt.
- **Export (HIS-09):** progress report export produces structured PDF **and** CSV (study hours, average accuracy, completed goals); generated from domain models, not UI state.
- **Guest mode:** session history is unavailable to guests (per AUTH-04) — gate the feature behind an authenticated account.

## Capabilities
| ID | Capability | Priority | Must-know rule |
|----|-----------|----------|----------------|
| HIS-01 | View Session History | MUST | Chronological list of all practice + exam sessions with quick score/type summaries. |
| HIS-02 | View Session Details | MUST | Detail view shows verse range, total mistakes, categories, accuracy, and duration. |
| HIS-03 | View Mistake History | SHOULD | Dedicated "mistake bank" compiling verses where the user has historically struggled. |
| HIS-04 | Compare Progress | SHOULD | Side-by-side comparison of current vs historical baseline; flag improvement or regression. |
| HIS-05 | View Learning Statistics | SHOULD | Aggregate metrics: total sessions, reading time, average score, average mistakes, best score, streak. |
| HIS-06 | Personalized Recommendations | COULD | Parse error frequency to highlight sections/rules needing revision. |
| HIS-07 | Filter Session History | COULD | Filter by session type (Ta'ahud / Exam / Mu'allem) or date range. |
| HIS-08 | Delete Session History | SHOULD | Prompt, then delete selected records from local DB and remote server. |
| HIS-09 | Export Progress Report | COULD | Generate structured PDF/CSV: total study hours, average accuracy, completed goals. |
| HIS-10 | Sync History Across Devices | SHOULD | Quiet background sync pushes local edits to the primary user profile. |

## Design system (`:designsystem`)
- Session list rows (HIS-01) → `SectionedCard` per entry; filter chips (HIS-07) → `DayTabRow` / `TabSelector` for type, plus a date-range control (needs new :designsystem component: DateRangePicker).
- Session detail (HIS-02) → `SectionedCard` + `ExpandableSection` for per-category mistake breakdowns; per-mistake rows via `AccentBullet`.
- Statistics dashboard (HIS-05) → needs new :designsystem component: StatTile (metric tiles for sessions/time/avg score/streak).
- Comparison (HIS-04) → needs new :designsystem component: ProgressComparisonCard / AccuracyTrendChart.
- Loading → `Shimmer` (needs new :designsystem component); empty history → `EmptyDataScreen`; sync/network failure → `NetworkErrorScreen`.
- Top bar → `BackTitleTopBar`; delete/export confirmations → `AppBottomSheet` (needs new :designsystem component) with `PrimaryButton` / `SecondaryButton`; export action row → `SettingsActionCard`.
- Read all colors/dp/sp via `Theme.*` — never hardcode.

## Data, stack & offline
- Room stores session logs mirroring the `SessionLog` schema (session + ayah-feedback + mistake tables); repository exposes `Flow<List<Session>>` mapped to pure domain models.
- Ktor/gRPC syncs `SessionLog` records with the unified user-data service (ARC-01); offline reads served from cache, writes (deletes) queued and reconciled on reconnect (OFF-05/OFF-06).
- Aggregation (stats, streaks, trends) computed in `domain` use cases over local data so it works fully offline.

## Applicable NFRs
- NFR-AVL AVL-01 (offline core), AVL-03 (offline queueing).
- NFR-ACC ACC-02 (surface confidence, mark uncertain in mistake bank).
- NFR-LOC LOC-02 (RTL), LOC-04 (locale-aware numbers/dates, Hijri).
- NFR-PRV PRV-02 (GDPR data erasure on delete).
- NFR-ARC ARC-01 (unified user-data service), ARC-02 (shared ProtoBuf schema).

## Related features
- [[06-taahud-live-correction]] — TAH-12 appends completed sessions to this history log.
- [[08-ikhtibar-exam]] — exam sessions and results feed this history.
- [[13-progress-gamification]] — statistics/streaks/dashboards build on HIS aggregates.
- [[14-ai-personalization]] — recommendations (HIS-06) driven by the AI mistake analysis.
- [[17-offline-sync]] — HIS-10 background sync and offline queueing.
