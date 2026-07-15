# 17. Offline Support & Sync — OFF

> **Product spec:** [Al-Mahir-SDD.md](../Al-Mahir-SDD.md) → "Epic 17: Offline Support & Sync". That file is the canonical product requirement; this doc is the agent build brief.
> **Rules:** [AGENTS.md](../../AGENTS.md) — architecture, modules, MVI, stack, localization (do not restate them here).

**Priority:** MUST · **Primary modules:** :data, :domain, :presentation

## Purpose
Offline-first architecture: core reading, bookmarks, history, and settings work with no connection; local changes are queued and auto-synced with conflict resolution when connectivity returns.

## Must-know constraints
- **Offline core (OFF-01 / AVL-01):** Mushaf rendering, reading-position tracking, bookmarks, history reads, and settings MUST be fully functional without internet. Room is the source of truth; the UI reads local data first, never blocks on the network.
- **Local queueing (OFF-05 / AVL-03):** persist mutating actions (bookmarks, streak/reading updates, notes, session logs) to a local **transaction/outbox log** in Room while offline — never lose a write. Each queued op carries a stable client id + timestamp for idempotent replay.
- **Automatic sync (OFF-06):** on connectivity regained, a background worker uploads queued ops, downloads cloud updates, and **resolves conflicts** deterministically (define the policy — e.g. last-write-wins by server timestamp, or field-level merge for streaks/bookmarks). Sync must be idempotent and safe to retry.
- **Graceful degradation (OFF-08 / AVL-02):** cloud-dependent features (live AI Ta'ahud/Mu'allem/Exam, semantic search, cloud sync) disable cleanly with a clear help message — never freeze or crash. Reading stays available (AI-12).
- Sync runs quietly in the background (WorkManager with a network constraint); status indicators are non-intrusive (OFF-07). Guest mode has no cloud sync (AUTH-04) — queue/sync is a no-op.
- Downloaded audio/tafsir/translation packages live in secure app-private storage; encrypt sensitive data at rest (PRV-03/SEC-01). Any user-facing sync/offline strings are ar + en.

## Capabilities
| ID | Capability | Priority | Must-know rule |
|----|-----------|----------|----------------|
| OFF-01 | Read Qur'an Offline | MUST | Mushaf engine + reading tracking run fully offline. |
| OFF-02 | Access Bookmarks Offline | SHOULD | Read/update local bookmarks; queue changes when offline. |
| OFF-03 | Offline History Access | SHOULD | Read session stats from local cache when no network. |
| OFF-04 | Download Audio Offline | SHOULD | Download recitation segments to secure offline directories. |
| OFF-05 | Local Queueing | SHOULD | Queue DB mutations via transaction logs for later sync. |
| OFF-06 | Automatic Synchronization | SHOULD | On reconnect: upload queued, download cloud, resolve conflicts. |
| OFF-07 | Sync Status Alerts | COULD | Clean, non-intrusive indicator in profile/settings. |
| OFF-08 | Graceful Offline UI | MUST | Disable AI/exam gracefully with guidance; keep reading available. |

## Design system (`:designsystem`)
- Sync status (OFF-07): `StatusOverlay` for active sync (`Loading`/`Success`/`Error`) on explicit sync; a small inline indicator in profile/settings — **needs new :designsystem component: SyncStatusChip** (compact idle/syncing/synced/error state pill).
- Offline / degraded features (OFF-08): `NetworkErrorScreen(onRetry = …)` for failed online fetches; `EmptyDataScreen` when a dataset isn't cached; an inline **needs new :designsystem component: OfflineBanner** to explain a disabled AI/exam surface with a help CTA.
- Download management (OFF-04): `SectionedCard` / `ExpandableSection` list rows with progress; `PrimaryButton` (Download) / `SecondaryButton` (Cancel/Remove), `IconButton` for per-item actions. Loading placeholders via `Shimmer`.
- Read all tokens via `Theme.*`; RTL-aware; disabled-feature affordances use `Theme.colors.disable` (never hardcode gray).

## Data, stack & offline
- **Room:** primary store for verses metadata/reading position, bookmarks, history, settings, plus an **outbox/transaction-log table** for queued mutations and a sync-state table (last-synced cursor, pending count).
- **Ktor:** sync client — batched upload of queued ops and delta download; conflict resolution mapped in `data`, exposing only domain models upward (no DTO/Room leakage to UI).
- **WorkManager:** connectivity-constrained sync worker with exponential backoff; observe `ConnectivityManager` to trigger sync on reconnect. Downloads use a foreground/expedited worker with progress.
- **domain:** repository interfaces expose offline-first reads (local Flow) and enqueue-on-write; a `SyncUseCase` and conflict-resolution policy are pure-Kotlin and unit-tested.
- Degradation gates (OFF-08/AVL-02): a domain connectivity/capability check drives which features presentation disables; reading path never gated.

## Applicable NFRs
- NFR-AVL AVL-01 (offline core), AVL-02 (graceful degradation), AVL-03 (offline queueing + auto-sync).
- NFR-ARC ARC-01 (unified user-data service for bookmarks/sessions/exams/streaks), ARC-02 (shared ProtoBuf scoring schemas on sync).
- NFR-SEC SEC-01 (secure storage for tokens used by sync), NFR-PRV PRV-03 (AES-256 at rest / TLS 1.3 in transit).
- NFR-PRF PRF-04 (stability; sync must not crash or corrupt on retry).
- NFR-LOC LOC-01/LOC-02 (ar+en, RTL for status/offline messaging).

## Related features
- [[03-mushaf-reading]] — offline reading + resume position.
- [[10-bookmarks-collections]] — offline bookmark read/write and queueing.
- [[12-session-history]] — offline history reads and background history sync (HIS-10).
- [[16-settings]] — offline settings and sync preferences.
- [[15-notifications]] — local alerts fire regardless of connectivity.
