# 2. Profile & Account Management — PROF

> **Product spec:** [Al-Mahir-SDD.md](../Al-Mahir-SDD.md) → "Epic 2: Profile & Account Management". That file is the canonical product requirement; this doc is the agent build brief.
> **Rules:** [AGENTS.md](../../AGENTS.md) — architecture, modules, MVI, stack, localization (do not restate them here).

**Priority:** SHOULD · **Primary modules:** :presentation, :domain, :data

## Purpose
Personalization settings, user profile customization, and self-service privacy controls (data export, recording deletion, permission management).

## Must-know constraints
- Password change (PROF-02) MUST re-verify the current password before accepting any new input; enforce the same complexity policy as AUTH-01 via the shared domain validator.
- Account deletion (PROF-03) MUST trigger hard server-side deletion of all recordings, historical stats, and credentials under GDPR — not a soft-disable. Require explicit confirmation and clear local state afterward (mirrors logout).
- Privacy self-service (PROF-04): single-tap "delete all voice recordings" that preserves progress stats; provide data-export (download personal data). Deletion of voice data must be decoupled from progress data.
- "Number of modod" = tajwid madd elongation preference (2 / 4 / 6 harakat) — a numeric enum choice, not free text.
- Profile edits (language, reciter, daily goal) write through to the relevant settings surfaces and sync to cloud; local Room is source of truth offline, queued for sync.
- App-language change from the profile must reload UI instantly and stay RTL-aware; keep all profile strings in ar + en.

## Capabilities
| ID | Capability | Priority | Must-know rule |
|----|-----------|----------|----------------|
| PROF-01 | Edit Profile Details | SHOULD | Edit name, photo, app language, preferred reciter, daily goal, number of modod (2/4/6 harakat madd enum). |
| PROF-02 | Secure Password Change | MUST | Verify current password before accepting new input; reuse AUTH complexity policy. |
| PROF-03 | Permanent Account Deletion | MUST | GDPR hard deletion of all recordings, stats, and credentials server-side; explicit confirmation. |
| PROF-04 | Privacy Self-Service | MUST | Single-tap delete of all voice data while preserving progress stats; provide personal-data download. |

## Design system (`:designsystem`)
- Profile layout: `SectionedCard` for grouped profile info; `SettingsActionCard` / `ExpandableSection` for account & privacy rows (Change password, Delete account, Download my data, Delete recordings).
- Inputs: `TextField` for name / password fields; reciter, language, and modod pickers via `AppBottomSheet` selection (exists in module source; README-lagged); modod (2/4/6) or day/goal selectors can use `DayTabRow` / `TabSelector` as a one-of-N selector.
- Buttons: `PrimaryButton` (Save changes / Confirm), `SecondaryButton` (Cancel), destructive delete uses `PrimaryButton` styled via `Theme.colors.error` (do not hardcode red).
- Confirmation for PROF-03/PROF-04 destructive actions: `AppBottomSheet` (needs new component) or an overlay dialog — **needs new :designsystem component: ConfirmationDialog** for irreversible-action confirmation.
- Processing/result: `StatusOverlay` (`Loading`/`Success`/`Error`) for password change, deletion, and data-export/erasure.
- Empty/error: `NetworkErrorScreen(onRetry = …)` when a server-side privacy/delete request fails.
- Top bar: `BackTitleTopBar` for profile sub-screens; read all tokens via `Theme.*`, RTL-aware.

## Data, stack & offline
- `data`: Ktor for profile update, password change, account-delete, data-export, and recording-deletion endpoints; Room for the local profile/preferences cache. Preferences (language, reciter, daily goal, modod) persist locally and queue for sync when offline.
- Offline: reading profile prefs works offline; password change, account deletion, and data export/erasure require network — surface clear offline messaging (AVL-02) and do not fake success.
- Deletion flows must clear local caches to stay consistent with the server hard-delete.

## Applicable NFRs
- NFR-PRV PRV-01 (retention policy), PRV-02 (data erasure + download), PRV-03 (encryption at rest/in transit), PRV-04 (privacy label).
- NFR-SEC SEC-01 (secure credential handling on password change).
- NFR-LOC LOC-01 (ar+en), LOC-02 (RTL), LOC-04 (locale-aware number/goal formatting).
- NFR-A11Y A11Y-01 (non-color indicators), A11Y-02 (screen-reader labels).

## Related features
- [[01-authentication]] — shared password policy, logout parallels for account deletion.
- [[16-settings]] — Privacy Settings (SET-07), Account Settings (SET-08), language & appearance.
- [[17-offline-sync]] — preference edits queued and synced.
