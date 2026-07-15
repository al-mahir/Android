# 13. Progress, Statistics & Gamification — PRG

> **Product spec:** [Al-Mahir-SDD.md](../Al-Mahir-SDD.md) → "Epic 13: Progress, Statistics & Gamification". That file is the canonical product requirement; this doc is the agent build brief.
> **Rules:** [AGENTS.md](../../AGENTS.md) — architecture, modules, MVI, stack, localization (do not restate them here).

**Priority:** SHOULD · **Primary modules:** `:presentation`, `:domain`, `:data`

## Purpose
Engagement systems tracking milestones, daily goals, reading consistency (streaks), Khatmah completion, and structural achievements to drive user retention.

## Must-know constraints
- **Offline-first read (AVL-01/OFF-03):** the dashboard, goals, streaks, and Khatmah progress render from the local Room cache and recompute locally; background sync (OFF-06) reconciles with cloud. Never block on network.
- **Derived from HIS `SessionLog` (ARC-02):** all metrics (accuracy averages, mistake trends, sessions completed, reading time) are aggregated in `domain` use cases from the session-history data (SDD §4 `SessionLog` / `AyahFeedback`) — do not create a parallel data model. Goal/streak/Khatmah state are the only PRG-owned persisted entities.
- **AI confidence (ACC-02/AI-01):** mistake-trend surfaces (PRG-07) reflect the same confidence/`is_uncertain` flags from AI evaluations — do not aggregate uncertain mistakes as if certain.
- **Locale-aware formatting (LOC-04):** streaks, percentages, durations, estimated completion dates, and all chart axes use locale-aware number/date formatting including Hijri; Arabic-first, RTL-aware (mirror charts and trend directions correctly).
- **Streak integrity (PRG-05):** consecutive-day logic must be timezone-correct and derived from local activity records; streak-at-risk checks tie into Epic 15 notifications (NTF-03), not this epic.
- **Khatmah celebration (PRG-08):** completion generates a gold-accented achievement card; the same gold-accent celebration convention is shared with NTF-06.
- **Export (PRG-11):** exports PDF summaries including rendered charts, reading metrics, average accuracy, streaks, and Khatmah completion; generate from domain models.
- **Guest mode:** progress/streaks/goals require an authenticated account (AUTH-04 restrictions).

## Capabilities
| ID | Capability | Priority | Must-know rule |
|----|-----------|----------|----------------|
| PRG-01 | Learning Dashboard | SHOULD | Unified view: current streak, total time, completed sessions, avg accuracy, total mistakes, last activity, Khatmah progress. |
| PRG-02 | Track Daily Reading Goal | SHOULD | Goal types: pages, ayahs, minutes, or sessions; progress auto-updates and completion is shown. |
| PRG-03 | View Daily Progress | SHOULD | Show today's goal, completed %, remaining target, and active time spent. |
| PRG-04 | Weekly & Monthly Reports | COULD | Reading days, sessions completed, accuracy average, total mistakes, reading duration. |
| PRG-05 | Track Reading Streak | SHOULD | Current streak, longest streak, last missed day, streak status. |
| PRG-06 | Accuracy Trend Charts | COULD | Interactive graphs of avg accuracy, improvement %, and regression alerts. |
| PRG-07 | Analyze Mistake Trends | COULD | Top Tajwid/Tashkeel mistakes, difficult verses, frequently mispronounced words. |
| PRG-08 | Track Khatmah Progress | SHOULD | Completion %, current Juz', remaining pages, estimated completion date; gold achievement on completion. |
| PRG-09 | Unlock Achievements | COULD | Milestones: first session, first Surah, 7/30-day streak, first Khatmah, perfect exam, 100 consecutive correct verses. |
| PRG-10 | Compare Historical Periods | COULD | This week vs last, this month vs last, current vs previous accuracy, mistake-reduction %. |
| PRG-11 | Export Progress Stats | COULD | PDF summary with charts, reading metrics, avg accuracy, streaks, Khatmah completion. |

## Design system (`:designsystem`)
- Dashboard tiles (PRG-01/PRG-03) → needs new :designsystem component: StatTile (metric tiles); grouped in `SectionedCard`.
- Streak widget (PRG-05) → needs new :designsystem component: StreakRing (with numeric + icon label, not color alone per A11Y-01).
- Daily goal (PRG-02/PRG-03) → needs new :designsystem component: GoalProgressBar / RingProgress; goal-type switch via `TabSelector` / `DayTabRow`.
- Trend graphs (PRG-06/PRG-10) → needs new :designsystem component: AccuracyTrendChart; mistake trends (PRG-07) → needs new :designsystem component: MistakeTrendChart.
- Achievements (PRG-09) → needs new :designsystem component: AchievementCard; Khatmah completion (PRG-08) → gold-accented card, reuse `ExpandableAccentCard` pattern for the accent-bar treatment.
- Weekly/monthly report sections (PRG-04) → `ExpandableSection`; loading → `Shimmer` (needs new :designsystem component); empty (no activity yet) → `EmptyDataScreen`; sync failure → `NetworkErrorScreen`.
- Top bar → `BackTitleTopBar`; export action → `SettingsActionCard` + `PrimaryButton`.
- Read all colors/dp/sp via `Theme.*` — Khatmah gold accent must be a `Theme.*` token, never a hardcoded hex.

## Data, stack & offline
- Room stores PRG-owned state: daily-goal config, streak counters, Khatmah progress, unlocked achievements; session-derived metrics are computed on read, not duplicated.
- Repository exposes `Flow` of goal/streak/Khatmah state → mapped to immutable domain models; aggregation use cases combine PRG state with HIS `SessionLog` data.
- Ktor/gRPC syncs goal/streak/achievement state via the unified user-data service (ARC-01); offline reads from cache, writes queued (OFF-05/OFF-06).

## Applicable NFRs
- NFR-AVL AVL-01 (offline core), AVL-03 (offline queueing).
- NFR-ACC ACC-02 (confidence honored in mistake-trend aggregation).
- NFR-LOC LOC-02 (RTL, mirrored charts), LOC-04 (locale-aware numbers/dates, Hijri).
- NFR-A11Y A11Y-01 (streak/status never color-alone — icon + text), A11Y-02 (chart controls labeled for screen readers).
- NFR-ARC ARC-01 (unified user-data service), ARC-02 (shared ProtoBuf schema).

## Related features
- [[12-session-history]] — PRG metrics are aggregated from HIS `SessionLog` records.
- [[03-mushaf-reading]] — reading activity (pages/time) feeds goals, streaks, and Khatmah progress.
- [[14-ai-personalization]] — AI insights (AI-09) and study plans build on PRG trends.
- [[15-notifications]] — streak-at-risk (NTF-03) and Khatmah (NTF-06) alerts tie into PRG state.
- [[17-offline-sync]] — background sync of goal/streak/achievement state.
