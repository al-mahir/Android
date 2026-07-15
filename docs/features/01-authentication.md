# 1. Authentication & User Management — AUTH

> **Product spec:** [Al-Mahir-SDD.md](../Al-Mahir-SDD.md) → "Epic 1: Authentication & User Management". That file is the canonical product requirement; this doc is the agent build brief.
> **Rules:** [AGENTS.md](../../AGENTS.md) — architecture, modules, MVI, stack, localization (do not restate them here).

**Priority:** MUST · **Primary modules:** :presentation, :domain, :data

## Purpose
Secure identity creation, persistent session state handling, and localized credential recovery so user progress is safely saved and synced.

## Must-know constraints
- Tokens & credentials MUST live in Android EncryptedSharedPreferences / Keystore (SEC-01) — never plaintext prefs or DB. Refresh token rotates automatically.
- Password policy: >= 8 chars with upper + lower + number + special; enforce in a `domain` validator (unit-tested), not in the Composable.
- Password-reset and email endpoints MUST be enumeration-safe: identical UX/response whether or not the account exists; reset link expires in 15 min, verification is a one-time OTP.
- Login triggers local Room ↔ backend sync on JWT issuance; cloud-sync is blocked until email is verified (AUTH-09).
- Guest mode is strictly local-only: no cloud sync, bookmarks, session/exam history, or reminders — gate these features on auth state, don't hide them silently.
- Logout MUST clear local session caches + active DB bindings and invalidate the JWT server-side (AUTH-07).
- Apple Sign-In (AUTH-03) is iOS-only — not in scope for the Android build; keep the auth abstraction provider-agnostic so it slots in later.
- All auth strings in English + Arabic; RTL-aware forms (start/end); Arabic-first recovery emails/OTP copy.

## Capabilities
| ID | Capability | Priority | Must-know rule |
|----|-----------|----------|----------------|
| AUTH-01 | Email/Password Registration | MUST | Enforce password complexity (>=8, upper/lower/number/special) in a domain validator. |
| AUTH-02 | Google Sign-In | MUST | Integrate Google Identity Services SDK; graceful UI alerts on network timeout. |
| AUTH-03 | Apple Sign-In | MUST (iOS) | iOS-only; nonce verification + secure keychain. Not built on Android — keep provider abstraction open. |
| AUTH-04 | Guest Mode | SHOULD | Local-only; no sync/bookmarks/history/reminders/exams. Gate cloud features on auth state. |
| AUTH-05 | Login with Saved Data Access | MUST | On successful JWT issuance, sync local Room DB with backend server state. |
| AUTH-06 | Persistent Sessions | SHOULD | Refresh token auto-rotated in EncryptedSharedPreferences; remembered across launches. |
| AUTH-07 | Secure Logout | MUST | Clear local caches + DB bindings and invalidate JWT server-side. |
| AUTH-08 | Password Reset | MUST | Secure 15-min expiring email link; keep public API enumeration-safe. |
| AUTH-09 | Email Verification | SHOULD | OTP dispatched right after signup; block cloud-sync until verified. |

## Design system (`:designsystem`)
- Inputs: `TextField` for email/password/name; verification code (AUTH-09) → `OtpField` *(exists in module source under `components/textfield/`; README-lagged)*.
- Buttons: `PrimaryButton` (Sign in / Register / Send reset — supports `isLoading`), `SecondaryButton` (Continue as guest / alt provider), `IconButton` (Google provider button / show-password toggle).
- Processing/result: `StatusOverlay` driven by `OverlayState` (`Loading` / `Success` / `Error`) for sign-in, reset-link send, and verification — dispatch a `DismissStatus` intent from `onDismiss`.
- Errors/offline: `NetworkErrorScreen(onRetry = …)` for provider/network timeouts (AUTH-02).
- Top bars: `BackTitleTopBar` on reset-password / verification sub-screens.
- Read all colors/dp/sp via `Theme.*`; forms must render correctly in RTL (start/end alignment).

## Data, stack & offline
- `data`: Ktor client for auth/reset/verify endpoints; Room for the local user/session cache; EncryptedSharedPreferences for tokens (Keystore-backed). Map DTOs → domain models — never leak DTO/Room types to UI.
- Offline: guest reading works offline; auth actions (login/register/reset/verify) require network — surface a clear offline state, no silent failures. On reconnect after login, run local↔cloud sync (see [[17-offline-sync]]).
- Sync is gated on email verification (AUTH-09) and only runs for authenticated (non-guest) users.

## Applicable NFRs
- NFR-SEC SEC-01 (secure token storage), SEC-02 (store compliance).
- NFR-PRV PRV-03 (AES-256 at rest, TLS 1.3 in transit).
- NFR-LOC LOC-01 (ar+en), LOC-02 (RTL mirroring).
- NFR-A11Y A11Y-02 (screen-reader labels on all controls).
- NFR-AVL AVL-02 (graceful degradation when offline).

## Related features
- [[02-profile-account]] — password change, account deletion, privacy controls.
- [[16-settings]] — Account Settings (SET-08), linked auth providers.
- [[17-offline-sync]] — local↔cloud sync triggered on login.
