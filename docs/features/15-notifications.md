# 15. Notifications — NTF

> **Product spec:** [Al-Mahir-SDD.md](../Al-Mahir-SDD.md) → "Epic 15: Notifications". That file is the canonical product requirement; this doc is the agent build brief.
> **Rules:** [AGENTS.md](../../AGENTS.md) — architecture, modules, MVI, stack, localization (do not restate them here).

**Priority:** SHOULD · **Primary modules:** :presentation, :domain, :data

## Purpose
Engagement reminders and completion alerts (daily recitation, streak-at-risk, session/exam results, Khatmah) governed by per-category preferences and strict quiet-hour schedules.

## Must-know constraints
- Notifications are **local** (WorkManager / AlarmManager), scheduled on-device from user prefs — not server push. Fire even when offline; no network dependency for scheduling or delivery.
- **Quiet Hours (NTF-07)** blocks all non-critical alerts inside the user-defined window. Enforce centrally in the domain scheduler so every category respects it — never per-notification ad hoc. Window may cross midnight; use locale-aware time and honor the device timezone.
- **Permission guidance (NTF-08):** Android 13+ requires the `POST_NOTIFICATIONS` runtime permission. On revoke/deny, render an in-app banner that deep-links to system notification settings — never nag with repeated system dialogs.
- Tap actions must deep-link to the correct destination: NTF-04 → session summary, NTF-05 → exam summary, NTF-06 → Khatmah celebration. Coordinate with [[18-platform-integrations]] deep-linking.
- All notification titles/bodies come from string resources in **ar + en**; times/dates use locale-aware formatting (LOC-04, incl. Hijri where shown).
- Preference toggles are the single source of truth (Room-backed); guest mode has no reminders (see AUTH-04) — scheduler must no-op for guests.

## Capabilities
| ID | Capability | Priority | Must-know rule |
|----|-----------|----------|----------------|
| NTF-01 | Configure Preferences | SHOULD | Explicit per-category toggle switches; persisted locally and drive all scheduling. |
| NTF-02 | Daily Recitation Reminder | SHOULD | Local notification fired at user-configured daily target time. |
| NTF-03 | Streak Reminders | COULD | Local check ~4h before daily cycle end; alert only if streak is at risk. |
| NTF-04 | Session Summary Alert | SHOULD | Local alert when background AI processing for a session finishes; taps open the summary. |
| NTF-05 | Exam Scored Notification | SHOULD | Tapping opens the target exam summary screen directly. |
| NTF-06 | Khatmah Achievement Alert | COULD | Gold-accented celebration card + native congratulations alert on Khatmah completion. |
| NTF-07 | Quiet Hours | SHOULD | Block non-critical alerts during the user-specified window (may cross midnight). |
| NTF-08 | Permissions Guidance | SHOULD | In-app banner directing to OS settings when notification permission is revoked. |

## Design system (`:designsystem`)
- Preference screen: `SettingsActionCard` / `SectionedCard` rows per category; each row needs a toggle — **needs new :designsystem component: SettingToggleRow** (switch control not in catalog).
- Daily time + quiet-hours window pickers: **needs new :designsystem component: TimePickerField / QuietHoursRangePicker** (no time picker in catalog).
- Permission banner (NTF-08): reuse `NetworkErrorScreen`-style slotted messaging is not a fit — **needs new :designsystem component: PermissionGuidanceBanner** (inline dismissible banner with CTA to OS settings).
- Buttons: `PrimaryButton` (Save / Open settings), `SecondaryButton` (Dismiss), `IconButton` for row affordances.
- Khatmah celebration (NTF-06): gold-accented card via `SectionedCard` with an accent; use `Theme.colors` accent tokens — never hardcode gold. `StatusOverlay` `Success` can confirm a saved preference change.
- Top bar `BackTitleTopBar` for the notifications settings sub-screen; read all tokens via `Theme.*`, RTL-aware.

## Data, stack & offline
- **Scheduling:** WorkManager (`PeriodicWorkRequest` for daily; one-shot for streak/summary triggers) or `AlarmManager` for exact daily times; a domain-level scheduler applies quiet-hours filtering and reschedules on boot (`BOOT_COMPLETED` receiver) and on preference change.
- **Room:** notification preferences, quiet-hours window, and scheduled-trigger bookkeeping persist locally; all evaluation runs offline.
- **Ktor:** none required for delivery. NTF-04/NTF-05 fire off the completion of local/queued AI/exam results ([[06-taahud]] / [[08-ikhtibar]]); when those depend on cloud scoring, degrade gracefully offline (AVL-02).
- Channels: define per-category `NotificationChannel`s so users can also tune them at the OS level.

## Applicable NFRs
- NFR-LOC LOC-01 (ar+en), LOC-02 (RTL), LOC-04 (locale-aware time/date incl. Hijri).
- NFR-A11Y A11Y-01 (non-color indicators — Khatmah card not gold-only), A11Y-02 (screen-reader labels on toggles/banners).
- NFR-AVL AVL-01 (works offline), AVL-02 (graceful degradation of alerts that depend on cloud results).
- NFR-SEC SEC-02 (Play Store notification/permission compliance).

## Related features
- [[16-settings]] — Notification Settings (SET-06) links here.
- [[18-platform-integrations]] — deep-link targets for notification taps.
- [[12-session-history]] / exam & Khatmah surfaces that generate NTF-04/05/06.
- [[13-progress-gamification]] — streak data driving NTF-03.
