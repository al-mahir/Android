# Feature Briefs

Per-feature **agent build briefs** for Al-Mahir. Each file distils one epic into the must-know
engineering constraints, a capability table, the `:designsystem` component mapping, data/offline
notes, and the NFRs that apply — so [`AGENTS.md`](../../AGENTS.md) can stay lean.

**Doc hierarchy**

- [`AGENTS.md`](../../AGENTS.md) — the rules + the index (start here).
- [`docs/Al-Mahir-SDD.md`](../Al-Mahir-SDD.md) — canonical product requirements (all epics + NFRs + §4 ProtoBuf).
- `docs/features/NN-*.md` — this folder: one build brief per epic.
- [`docs/Mushaf_ssd.md`](../Mushaf_ssd.md) — deep 5-phase execution spec for the Mushaf engine.
- [`designsystem/README.md`](../../designsystem/README.md) — UI component catalog + tokens.

**A brief holds:** must-know constraints · capability table (IDs verbatim from the SDD) ·
`:designsystem` mapping · data/stack/offline · applicable NFRs · related features. It does **not**
restate architecture rules — those live in `AGENTS.md`.

| # | Epic | Priority | Brief |
|---|------|----------|-------|
| 1 | Authentication (AUTH) | MUST | [01-authentication.md](01-authentication.md) |
| 2 | Profile & Account (PROF) | SHOULD | [02-profile-account.md](02-profile-account.md) |
| 3 | Mushaf – Reading (MUS) | MUST | [03-mushaf-reading.md](03-mushaf-reading.md) |
| 4 | Search & Index (SRCH) | SHOULD | [04-search-index.md](04-search-index.md) |
| 5 | Listen Mode (LSN) | MUST | [05-listen-mode.md](05-listen-mode.md) |
| 6 | Ta'ahud – Live AI Correction (TAH) | MUST | [06-taahud-live-correction.md](06-taahud-live-correction.md) |
| 7 | Mu'allem – Repeat After Sheikh (MLM) | SHOULD | [07-muallem-repeat.md](07-muallem-repeat.md) |
| 8 | Ikhtibar – Exam (IKH) | SHOULD | [08-ikhtibar-exam.md](08-ikhtibar-exam.md) |
| 9 | In-Session Feedback Bar (FBK) | MUST | [09-feedback-bar.md](09-feedback-bar.md) |
| 10 | Bookmarks & Collections (BMK) | SHOULD | [10-bookmarks-collections.md](10-bookmarks-collections.md) |
| 11 | Tafsir & Translation (TFS) | SHOULD | [11-tafsir-translation.md](11-tafsir-translation.md) |
| 12 | Session History (HIS) | SHOULD | [12-session-history.md](12-session-history.md) |
| 13 | Progress & Gamification (PRG) | SHOULD | [13-progress-gamification.md](13-progress-gamification.md) |
| 14 | AI & Personalization (AI) | SHOULD | [14-ai-personalization.md](14-ai-personalization.md) |
| 15 | Notifications (NTF) | SHOULD | [15-notifications.md](15-notifications.md) |
| 16 | Settings (SET) | SHOULD | [16-settings.md](16-settings.md) |
| 17 | Offline Support & Sync (OFF) | MUST | [17-offline-sync.md](17-offline-sync.md) |
| 18 | Platform Integrations (INT) | COULD | [18-platform-integrations.md](18-platform-integrations.md) |
| 19 | Community & Sharing (P2) | LATER | [19-community-sharing.md](19-community-sharing.md) |
