# Sheikh Availability Notifications + Auth Refresh — Status

> Companion to [`Meeting-Feature-Status.md`](Meeting-Feature-Status.md) and
> [`Meeting-Call-Lifecycle-Plan.md`](Meeting-Call-Lifecycle-Plan.md)/[`-Status.md`](Meeting-Call-Lifecycle-Status.md).
> Those docs cover the 1:1 meeting-request feature and the active-call foreground-service
> lifecycle. This doc covers three separate but related pieces of work done in the same session on
> top of that: (1) Uber-style always-on availability + ringing incoming-request notifications for
> the sheikh, (2) a silent-401/403-refresh fix for the meeting feature's HTTP client, and (3) a bug
> hunt into two regressions reported after (1) and (2) shipped — **the second of which is not yet
> resolved as of this writing.**

---

## ⚠️ Current status — two reported bugs, one confirmed as a backend issue, one still open

1. **Ringing notification firing more than once for a single incoming request.** Root cause found:
   not a duplicate event, but two overlapping audio sources (notification-channel sound +
   `IncomingRequestRinger`'s own player) for the same single ring — see "Bug A" below. Fixed in
   code; **not yet verified on-device.**
2. **After ending a call and sending a new request to the same sheikh, neither app can navigate
   into the new call — the sheikh app appears to show/return to the first (already-ended)
   meeting.** Root-caused as far as client-side logs allow: **`POST /api/instant-meetings/{id}/end`
   returns `500 "An unexpected error occurred"` from the production backend**, confirmed via
   on-device logcat (see "Root-cause findings" below). This is very likely a **backend bug**, not a
   client bug — no amount of client-side retry can fix a server 500. **Still open**, pending either
   a backend fix or further evidence (see "What's still needed" below).

Both bugs were reported as still happening after the fixes described in this document were
already applied and verified via on-device logcat — **do not assume the fixes below fully resolved
either issue.** Treat this doc as the up-to-date record of what's been tried, what's been ruled
in/out, and what's still unknown.

---

## ✅ Done — Part 1: Uber-style always-on availability + ringing notifications (sheikh-only)

**Problem it solves**: before this work, a sheikh's "Available" toggle and its STOMP subscription
to `/topic/sheikhs/{sheikhId}/requests` only ran while `AvailabilityViewModel` (Koin
`viewModel`-scoped, tied to `SheikhAvailabilityPanel`'s composition inside Home) was alive.
Navigating away from Home, backgrounding the app, or locking the screen killed the listener —
`StompClient.subscribe()`'s underlying `SharedFlow` has `replay = 0`, so any incoming-request event
arriving with no active collector was silently and permanently dropped.

**Architecture** (mirrors the already-shipped `CallSessionController`/`CallForegroundService`
pattern from `Meeting-Call-Lifecycle-Status.md`):

- **New**: `SheikhAvailabilityController` (`sheikh/presentation/.../availability/`) — Koin
  `single`, owns the heartbeat + STOMP subscription + accept/decline logic on its own
  process-lifetime `CoroutineScope` instead of `viewModelScope`. `accept()`/`decline()`
  reparameterized to take `requestId: String` (validated against current state) instead of reading
  `currentState` internally, since a notification action can fire with no ViewModel alive to read
  from.
- **Changed**: `AvailabilityViewModel` shrunk to a ~30-line adapter forwarding intents to the
  controller and exposing `controller.state`/`controller.errors` — mirrors `CallViewModel`'s
  relationship to `CallSessionController`. Zero changes to any UI composable
  (`SheikhAvailabilityPanel`, `SheikhAvailabilityContent`, `IncomingRequestCard`, `BusyIndicator`).
- **New**: `SheikhAvailabilityForegroundService` (`sheikh/presentation/.../availability/service/`)
  — persistent low-priority "You're Online" notification (silent channel, "Go Offline" action) while
  Available/Busy; a separate high-importance ringing notification
  (`NotificationCompat.CallStyle.forIncomingCall`, full-screen intent, Accept/Decline actions, real
  sound + vibration) whenever `controller.state` is `IncomingRequest`.
- **New**: `IncomingRequestRinger` — loops the default ringtone + a vibration pattern, DND/ringer-
  mode aware, 45s hard timeout as a safety net.
- **Accept from notification**: the notification's Accept action is a `PendingIntent.getActivity`
  trampoline into `MainActivity` (same `packageManager.getLaunchIntentForPackage` +
  `FLAG_ACTIVITY_SINGLE_TOP/CLEAR_TOP` trick `CallForegroundService` already uses), carrying only a
  new action string `ACTION_ACCEPT_INCOMING_REQUEST` — no extras, same "intent is just a trigger,
  real data comes from live singleton state" idiom as the existing `ACTION_OPEN_ACTIVE_CALL`.
  `SheikhAppNavigation.kt`'s `LaunchedEffect(pendingAction)` handles it by resetting the backstack
  to a clean `[Home]` (guards against back-stack corruption regardless of which screen the
  notification was tapped from) and calling `availabilityController.accept(requestId)` — navigation
  into `Call` then happens via the **existing, unchanged**, already-proven `Busy`-state-driven
  `LaunchedEffect(busyRequestId)` in `SheikhAvailabilityPanel`. Decline is a `PendingIntent
  .getService` action handled entirely in the background, no app-open needed.
- Manifest: `SheikhAvailabilityForegroundService` declared in `sheikh-app` only (students don't
  have this feature), `foregroundServiceType="dataSync"`, `stopWithTask="false"`. New permissions:
  `VIBRATE`, `FOREGROUND_SERVICE_DATA_SYNC`, `USE_FULL_SCREEN_INTENT`.
- EN/AR strings + drawables added for everything new.

**Known open risks, flagged at the time and still unverified**:
- `foregroundServiceType="dataSync"` is the closest existing Android FGS category for "keep a
  socket-listening connection alive while idle," but Android 15+ imposes runtime caps on `dataSync`
  intended for bounded sync jobs, not indefinite presence — not confirmed safe for a sheikh staying
  "available" for many hours.
- `setFullScreenIntent` needs a user-granted `USE_FULL_SCREEN_INTENT` permission on Android 14+ to
  actually wake the screen; degrades to a heads-up notification otherwise.

---

## ✅ Done — Part 2: silent 401/403 refresh for the meeting feature's HTTP client

**Problem it solves**: `MeetingHttpClient` (`:meeting:data`, shared by both apps for every meeting-
request/availability/call call) had **no auth-retry logic at all** — a stale access token just
returned a raw 401/403 to the caller with no retry. This was the original bug reported at the start
of this thread (`POST /api/instant-meetings/sheikh/{id}/request` → 403, no silent recovery).

- `MeetingAuthTokenProvider` (`:domain`) extended from a read-only `currentToken()`-only `fun
  interface` to a full `interface` adding `refreshToken(): String?` and `onAuthenticationExpired()`.
- New `TokenRefresher` (`:data`) — a standalone refresh caller on its own bare `HttpClient` (no
  `Auth` plugin of its own, so no recursion risk), POSTs to a given refresh endpoint, saves the new
  tokens to `TokenStore`, clears the session on a 4xx refresh failure.
- `MeetingHttpClient` now installs Ktor's `Auth`/`bearer` plugin (mirroring the already-working
  pattern in `AlmahirHttpClient`) instead of a static "attach whatever token we have" hook: on
  401/403 it calls `refreshToken()` up to **2 times**, retries the request once with the fresh
  token, and on final failure calls `onAuthenticationExpired()`.
- Wired per-app in `AlMahirApp.kt`/`SheikhApp.kt`, each using its **own** correct refresh endpoint
  (student `/api/auth/user/refresh` vs sheikh `/api/auth/sheikh/refresh` — these were already
  different endpoints elsewhere in the codebase, just never threaded through to the meeting client
  before this).
- `onAuthenticationExpired()` just calls `TokenStore.clear()` — both apps' `SessionViewModel`
  already reactively observes that (`AuthRepository.authState` ← `TokenStore.isLoggedIn`) and
  redirects to the Login screen, so no new navigation code was needed.

**Verified**: `:domain`, `:data`, `:meeting:data`, `:app`, `:sheikh-app` all `compileDebugKotlin`
clean. Confirmed working end-to-end via on-device logcat (`MeetingHttpClient`/`MeetingCall` tags
show successful `refreshToken`/retry cycles in later sessions).

---

## 🩹 Bug hunt — two regressions reported after Parts 1 & 2 shipped

### Bug A: ringing notification firing more than once per incoming request

**Update — root cause found and fixed**: the reported symptom ("2 rings a few milliseconds apart,
sounds like an echo") was never a duplicate STOMP event or a duplicate `notify()` call — it was
**two independent audio sources firing for the same single ring**.
`SheikhAvailabilityForegroundService.ensureChannels()` created the `RINGING_CHANNEL_ID` channel
with its own `setSound(ringtoneUri, ...)`, so Android's system played that channel's alert sound
the instant `notifySafely()` posted the heads-up notification. At the same time,
`showRingingNotification()` also calls `ringer.start()` (`IncomingRequestRinger`), which starts its
**own** looping `MediaPlayer` on the same default ringtone. Two players, same ringtone, started a
few ms apart (notification post vs. `MediaPlayer.prepare()`/`start()`) — exactly the echo effect
reported, and it happened on every single ring, unconditionally (unrelated to the "possible
duplicate STOMP delivery" theory below, which remains unconfirmed and may still be worth checking
if echoing persists).

**Fix applied**: removed `setSound(...)` from the ringing channel — `IncomingRequestRinger` is now
the sole audio source (it already loops, respects ringer mode/DND, and has its own 45s timeout).
Notification channels are immutable once created, so devices that already got the old
sound-bearing channel needed it recreated under a new ID: `RINGING_CHANNEL_ID` changed from
`"sheikh_availability_ringing"` to `"sheikh_availability_ringing_v2"`, and `ensureChannels()` now
calls `manager.deleteNotificationChannel(LEGACY_RINGING_CHANNEL_ID)` to clean up the old one instead
of leaving an orphaned channel in system settings. `:sheikh:presentation` `compileDebugKotlin`
clean. **Not yet verified on-device** — no emulator/device available in this environment.

**Previously-applied fixes** (still in place, real hardening, but not the actual cause of the
echo):
1. `goAvailable()` in `SheikhAvailabilityController` had no idempotency guard. Since it's now a
   process-lifetime singleton (unlike the old ViewModel it replaced, which was recreated — and thus
   implicitly re-guarded — every time Home was recomposed), a second call anywhere in the process's
   life would stack a second, independent `observeIncomingRequests` collector on top of the first,
   double-processing every event. Added an `isSubscribed` guard.
2. `accept()` had a narrow race window where a rapid double-tap (in-app + notification, or two fast
   taps) could fire two concurrent `acceptMeetingRequest` REST calls. Added an `isAccepting` guard.
3. `MeetingRequestViewModel.send()` (student side) had no guard against a rapid double-tap firing
   two concurrent `sendMeetingRequest` calls. This mattered more after Part 2 shipped: previously a
   race against an expiring token meant at most one of two racing taps would ever succeed; now both
   silently refresh-and-retry, so **both can succeed**, creating two distinct pending requests
   server-side — which would explain a sheikh's ringing notification firing for two genuinely
   separate requests. Added an in-flight (`currentState is Sending`) guard.
4. The ringing notification (`SheikhAvailabilityForegroundService.buildRingingNotification`) was
   missing `NotificationCompat.Builder.setOnlyAlertOnce(true)` — a well-known Android gotcha where
   re-posting/updating a notification on a sound-enabled channel re-triggers the alert (sound +
   heads-up) on **every** `notify()` call, not just the first. Added.

**Status**: user reports the bug **still happens** after all four fixes above. Not yet re-diagnosed
with fresh evidence — the leading remaining suspect is a genuine duplicate STOMP delivery of
`SHEIKH_MEETING_REQUEST_RECEIVED` from the backend (two distinct `requestId`s for what the user
perceives as one request), which none of the above fixes would catch since they all assume a single
canonical event. **Not confirmed** — see "What's still needed" below.

### Bug B: after ending a call, a new request to the same sheikh can't be navigated into; sheikh app appears stuck on the old meeting

**Root-cause finding, confirmed via on-device logcat**: `CallSessionController.endCall()` called
`repository.endMeeting(id)` fire-and-forget with its `Result` discarded — if the REST call failed,
the client-side UI still showed the call as `Ended` normally, but the backend was never reliably
told. Hardened: `endCall()` now checks the result and retries once, with logging either way
(`endMeetingReliably`, tag `MeetingCall`).

**This hardening surfaced the real underlying problem, which is server-side**: on-device logcat
shows `POST https://almahir-production.up.railway.app/api/instant-meetings/{requestId}/end`
returning **`500 {"success":false,"message":"An unexpected error occurred."}`** — consistently, on
both the original attempt and the retry. This is not fixable from the client.

**Suggestive detail**: despite the 500, the sheikh's `AvailabilityHeartbeat` was observed resuming
on its own shortly after (`PUT .../sheikh/availability {"status":"AVAILABLE"}` succeeding, then
repeating on its normal ~20s cadence) — this code path only runs when `SheikhAvailabilityController`
receives a `MEETING_ENDED` STOMP event. That implies the backend **does** process the actual
"end meeting" business logic and broadcast `MEETING_ENDED` correctly, and the 500 is specifically in
building/returning the HTTP response afterward (a common bug shape: work commits, then response
serialization throws). If that read is correct, the sheikh side of the flow may partially recover on
its own, but something else server-side — possibly tied to the same broken code path — appears to
prevent the **student's** second request from going anywhere.

**Status**: **not resolved.** User reports the bug still happens. This needs either a backend-side
fix to the `/end` endpoint's 500, or further client-side evidence to determine whether there's
*also* a client bug compounding it (see below).

---

## What's still needed to make further progress

1. **Backend**: fix the 500 on `POST /api/instant-meetings/{requestId}/end` — check server logs for
   that route around the failing timestamps; "An unexpected error occurred" is a generic message
   masking whatever's actually throwing, most likely after the real work (ending the meeting,
   broadcasting `MEETING_ENDED`) has already happened.
2. **Client evidence still missing**: the **student app's** (`com.iti.al_mahir`, not `.sheikh`)
   logcat for `MeetingLifecycle|MeetingCall|MeetingHttpClient|MeetingKit`, specifically the
   `REQUEST`/`RESPONSE` block for the student's **second** `POST /api/instant-meetings/sheikh/{id}
   /request` call (the second "Send Request" tap, after the first call ended). Every log excerpt
   provided so far has been sheikh-app-only. If that second request comes back `409`
   ("already pending" or similar), it confirms a backend-side cleanup gap tied to the same `/end`
   bug. If it comes back a clean `200`, the failure is somewhere else in the student's own
   navigation/state handling and needs a fresh look with that evidence in hand.
3. **For Bug A**: a logcat capture bracketing exactly one ringing episode, filtered to `MeetingKit`
   (which logs every raw STOMP frame via `observeIncomingRequests RAW FRAME: ...`) — this would show
   directly whether the backend is sending one `SHEIKH_MEETING_REQUEST_RECEIVED` event or two
   distinct ones (different `requestId`s) for what looks like a single incoming request.

Do not attempt further blind fixes on either bug without one of the above — the client-side changes
already made are real, defensible hardening regardless, but neither has been confirmed to be *the*
root cause of what the user is still seeing.

---

## Files touched in this body of work

- `domain/src/main/java/com/iti/domain/auth/MeetingAuthTokenProvider.kt`
- `data/src/main/java/com/iti/data/core/token/TokenRefresher.kt` (new)
- `meeting/data/src/main/java/com/iti/meeting/data/remote/MeetingHttpClient.kt`
- `meeting/presentation/src/main/java/com/iti/meeting/presentation/call/session/CallSessionController.kt`
- `mushaf/presentation/src/main/java/com/example/mushaf/presentation/audio/AudioPlaybackService.kt`
  (unrelated `RemoteServiceException` fix bundled into the same session — `onStartCommand` now
  `stopSelf()`s when there's nothing loaded to play, instead of leaving the service started-but-not-
  foregrounded past the OS's 5s timeout)
- `sheikh/presentation/src/main/java/com/iti/sheikh/presentation/availability/SheikhAvailabilityController.kt` (new)
- `sheikh/presentation/src/main/java/com/iti/sheikh/presentation/availability/AvailabilityViewModel.kt`
- `sheikh/presentation/src/main/java/com/iti/sheikh/presentation/availability/di/SheikhMeetingPresentationModule.kt`
- `sheikh/presentation/src/main/java/com/iti/sheikh/presentation/availability/service/SheikhAvailabilityForegroundService.kt` (new)
- `sheikh/presentation/src/main/java/com/iti/sheikh/presentation/availability/service/IncomingRequestRinger.kt` (new)
- `sheikh/presentation/src/main/res/values{,-ar}/strings.xml`, `res/drawable/*.xml` (new)
- `sheikh-app/src/main/AndroidManifest.xml`
- `sheikh-app/src/main/java/com/iti/al_mahir/sheikh/SheikhApp.kt`
- `sheikh-app/src/main/java/com/iti/al_mahir/sheikh/navigation/SheikhAppNavigation.kt`
- `app/src/main/java/com/iti/al_mahir/AlMahirApp.kt`
- `presentation/src/main/java/com/iti/presentation/meetingrequest/request/MeetingRequestViewModel.kt`

All of the above `compileDebugKotlin` clean as of this writing (`:domain`, `:data`, `:meeting:data`,
`:meeting:presentation`, `:mushaf:presentation`, `:sheikh:presentation`, `:presentation`, `:app`,
`:sheikh-app`). No device/emulator is available in this environment — every fix above has been
verified by compilation and, where the user provided logcat, by reading real on-device log output,
but **not** independently reproduced end-to-end by the assistant.
