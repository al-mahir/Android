# Meeting Call Lifecycle — Architecture Plan

> Companion to [`Meeting-Feature-Status.md`](Meeting-Feature-Status.md). That doc tracks the 1:1
> meeting-request feature overall; this one is scoped to one problem inside it — what happens to
> an **in-progress call** when the app is backgrounded, minimized, killed, or relaunched — and is
> the design reference for [`Meeting-Call-Lifecycle-Status.md`](Meeting-Call-Lifecycle-Status.md),
> which tracks day-to-day implementation progress against this plan.
>
> Read this file for **why** each piece exists. Read the status doc for **what's actually landed
> so far**.

## Context

The 1:1 meeting feature has a working happy path but, per `Meeting-Feature-Status.md`'s own "Not
yet addressed" section, no answer for what happens to an in-progress call when the app is
backgrounded, killed, or relaunched. Today `CallViewModel`
(`meeting/presentation/.../call/CallViewModel.kt`) owns the `AgoraEngineWrapper` directly and is
just a Koin `viewModel {}` — its lifetime is tied to whatever `ViewModelStoreOwner` hosts it
(effectively the `Activity`, per the reused-instance bugs already chased down in the status doc).
There is no foreground service, no call notification, and no persisted "a call is active" record,
so: backgrounding risks the OS throttling/killing the process mid-call, swiping the app from
Recents definitely kills it, and reopening the app after either has zero awareness a call ever
happened.

Target behavior, matching Google Meet/Zoom-grade professional apps: audio keeps flowing when the
app is backgrounded, a notification exposes mute/unmute/end controls, and reopening the app does
the smart thing — straight back into the call when we know it's still live, a "rejoin" prompt when
we had to reconstruct state after the process actually died. Applies to both `:app` (student) and
`:sheikh-app` (sheikh), sharing the logic that already lives in `:meeting:presentation`/
`:meeting:data`.

Two real, unrelated bugs surfaced during this investigation and are fixed as part of this work
since they block video calls / notifications outright:
- `CAMERA` is requested at runtime (`CallScreen.kt`'s `joinPermissionsLauncher`) but **never
  declared** in either app's `AndroidManifest.xml` — an undeclared runtime permission is
  auto-denied by the OS, so video has silently never been grantable.
- Neither manifest declares `POST_NOTIFICATIONS`, needed for any of this to be visible on API 33+.

## Architecture change: move call ownership out of the ViewModel

The root fix everything else builds on: the live call (Agora engine + its state) must be owned by
something that outlives the UI, not a ViewModel. Introduce a Koin **`single`** —
`CallSessionController` (new file, `meeting/presentation/.../call/session/CallSessionController.kt`)
— that:
- Owns the `AgoraEngineWrapper`, the `IRtcEngineEventHandler`, and a `StateFlow<CallSessionState>`
  (today's `CallUiState` plus `requestId`/`channelName`/`userAccount`/`remoteDisplayName`).
- Gets essentially the entire body of today's `CallViewModel` moved into it verbatim
  (`joinChannel`, `teardownForRejoin`/`prepareForRequest` → simplified since there's now exactly
  one instance app-wide so "stale reused ViewModel" can't happen, `toggleMic/Camera/Speaker`,
  `switchCamera`, `endCall`, `renewToken`, the join-timeout job, the duration timer).
- Starts/stops `CallForegroundService` itself (`ContextCompat.startForegroundService` on a
  successful join / `stopService` on terminal state) — the one place that decides the service's
  lifetime.

`CallViewModel` shrinks to a thin per-screen adapter: forwards user intents to
`CallSessionController` and maps its `StateFlow` to `CallUiState` for `CallScreen`. Multiple
`CallScreen` compositions (Activity recreated, Nav entry churn) all observe the same controller,
so state never resets — this also organically subsumes the whole
"reused-`CallViewModel`/stale-state" bug chase documented across the last several
`Meeting-Feature-Status.md` entries, since ownership is now unambiguous.

## `CallForegroundService`

New file, `meeting/presentation/.../call/session/CallForegroundService.kt`, a plain `Service`
(no need for `LifecycleService` — avoids a new dependency):
- `onStartCommand` calls `ServiceCompat.startForeground(this, NOTIFICATION_ID, notification,
  ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or FOREGROUND_SERVICE_TYPE_CAMERA)` immediately —
  required within the same call turn on API 34+.
- Collects `CallSessionController.state` on a service-owned `CoroutineScope` and calls
  `NotificationManager.notify(...)` on every change (mic muted icon, connecting→in-call, duration).
- Handles `ACTION_TOGGLE_MIC` / `ACTION_END_CALL` intents fired from the notification's actions,
  delegating straight to `CallSessionController`.
- Stops itself (`stopForeground(STOP_FOREGROUND_REMOVE)` + `stopSelf()`) once state goes terminal
  (`Ended`/cleared).
- One shared class, declared in **both** `app/src/main/AndroidManifest.xml` and
  `sheikh-app/src/main/AndroidManifest.xml` by fully-qualified name — same pattern already used for
  `com.example.mushaf.presentation.audio.AudioPlaybackService` in `app`'s manifest today.
- `android:stopWithTask="false"` — swiping the app off Recents does **not** end the call (matches
  WhatsApp/Zoom), since the service is independent of any Activity task.

### Notification — `NotificationCompat.CallStyle` (modern, not deprecated)

- One `NotificationChannel` (`"active_call"`, `IMPORTANCE_HIGH`, `CATEGORY_CALL`), created once,
  idempotently, on first use.
- `NotificationCompat.Builder(...).setStyle(NotificationCompat.CallStyle.forOngoingCall(person,
  endCallPendingIntent))` — the actual native "ongoing call" notification treatment (big avatar,
  red hang-up button), not a hand-rolled layout. `.addAction(...)` for the mute/unmute toggle
  (CallStyle renders a bonus action alongside hang-up). `.setOngoing(true)`,
  `.setContentIntent(...)` opens the app straight back into `CallScreen`.
- All `PendingIntent`s use `FLAG_IMMUTABLE` (mandatory, no deprecated mutable-by-default).
- `POST_NOTIFICATIONS` requested once, at the same moment `CallScreen` already requests
  mic/camera permission (its existing `joinPermissionsLauncher`) — add it to that same
  multi-permission launcher. If denied, the service still runs as a legitimate foreground service
  (confirmed OS behavior: `startForeground` succeeds even if the notification itself can't post) —
  no special-casing needed beyond not crashing on the missing permission.
- `Person`/title needs a display name, which nothing threads through today. Add
  `remoteDisplayName: String?` to `MeetingRoute.Call`, `AvailabilityUiState.Busy` (sheikh side —
  source: `IncomingRequestCard`'s already-known `studentName`), and `RequestUiState.Accepted`
  (student side — source: the sheikh name already known at `SheikhDetailsScreen`'s
  "Request 1:1 Meeting" entry point). Falls back to a generic "Ongoing call" string if null.

## Process-death survival & the three-way reopen behavior

`stopWithTask="false"` covers "swiped from Recents." It does **not** cover a real OS low-memory
kill or the user force-stopping the app — nothing can keep a process alive through that, so the
reopen logic has to branch on what's actually still true:

1. **Notification tapped** → the service (and thus `CallSessionController`) is by definition alive.
   `contentIntent` launches the host `MainActivity` with a well-known action string (reusing the
   existing pattern in `app/.../MainActivity.kt` — `pendingAction` `StateFlow` + `onNewIntent`,
   currently used for `"ACTION_OPEN_MUSHAF_LISTEN"`; add `"ACTION_OPEN_ACTIVE_CALL"` the same way).
   The nav host, on seeing that action, pushes `MeetingRoute.Call` with the controller's current
   session params — no card, no confirmation.
2. **App icon relaunch while the process is still alive** (Activity was destroyed but the process/
   `CallSessionController` singleton was not): at nav-host startup, inject `CallSessionController`
   and check its state synchronously — if non-idle/non-`Ended`, push straight to `MeetingRoute.Call`
   the same as case 1. This is the common "just reopened after minimizing" case.
3. **Cold start after real process death** (`CallSessionController` is a fresh empty instance):
   nothing in memory to trust. Read a new persisted `ActiveCallRecord` (DataStore-backed store,
   mirroring the existing `PendingMeetingRequestStore` pattern in
   `meeting/data/.../local/PendingMeetingRequestStore.kt` almost exactly — same shape, same
   save-on-join/clear-on-terminal lifecycle) and, if present, show an `OngoingCallCard` on Home
   (mirrors `presentation/.../home/components/PendingMeetingRequestCard.kt`, added the same way to
   `HomeUiState`/the student Home screen, and the sheikh-side equivalent for
   `SheikhHomeUiState`/`SheikhHomeScreen`). Tapping it calls the existing
   `MeetingRepository.refreshToken(requestId)` as a liveness probe — success → push `Call` with the
   refreshed token; failure → clear the record and toast that the call already ended. This is
   deliberately **not** an automatic silent rejoin: we can't trust a token that might be
   minutes-to-hours stale, or that the meeting is even still active server-side.

`MeetingRepository` gains `observeActiveCall()/getActiveCall()/clearActiveCall()` alongside the
existing pending-request accessors, backed by the new store. `CallSessionController` writes the
record on a successful join and clears it in the same places `PendingMeetingRequestStore` is
already cleared (terminal states / explicit `endCall`).

`sheikh-app`'s `MainActivity`/`SheikhAppNavHost` currently has **no** `pendingAction`/`onNewIntent`
plumbing at all (unlike `:app`) — add it, mirroring `:app`'s implementation exactly.

## Manifest & Gradle changes (both `app` and `sheikh-app`)

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" /> <!-- sheikh-app only, app already has it -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />

<service
    android:name="com.iti.meeting.presentation.call.session.CallForegroundService"
    android:foregroundServiceType="microphone|camera"
    android:stopWithTask="false"
    android:exported="false" />
```

`meeting/presentation/build.gradle.kts`: no new dependency needed for `CallStyle`
(`androidx.core:core-ktx` is already pinned to `1.19.0`, well above the `1.12.0` floor). Plain
`Service`, so no `lifecycle-service` addition either.

## Localization

Every new user-facing string (notification title/body/action labels, `OngoingCallCard`'s copy,
any new toast) gets EN + AR entries in the same change. Notification strings live in
**`:meeting:presentation`'s own `res/values`/`res/values-ar`** (new — this module had no
resources before this work): `CallForegroundService.kt` lives there and can only resolve string
resources it has compile-time `R` access to, and the module deliberately has zero dependency on
either host app, so the strings can't live in `:app`/`:sheikh-app` the way an earlier draft of
this plan assumed. `OngoingCallCard` strings live in `presentation`/`sheikh:presentation` next to
the existing `meetingrequest_*` EN/AR pairs, since that's where the card itself lives.

## Implementation order

1. Fix the `CAMERA` manifest gap (isolated, immediately valuable regardless of the rest).
2. `CallSessionController` singleton — move engine ownership out of `CallViewModel`; verify
   `compileDebugKotlin` clean and behavior unchanged (pure refactor first, no service/notification
   yet).
3. `CallForegroundService` + `CallStyle` notification + channel + `POST_NOTIFICATIONS` request,
   wired to start/stop via the controller.
4. Manifest wiring in both apps (service declaration, new permissions, `stopWithTask=false`).
5. `ActiveCallRecord` store + `MeetingRepository` additions.
6. Reopen routing: extend both `MainActivity`s + nav hosts for cases 1–2 above (sheikh-app needs
   the `pendingAction` plumbing added from scratch).
7. `OngoingCallCard` on both Home screens for case 3.
8. Thread `remoteDisplayName` end-to-end (`IncomingRequestCard`→`Busy`→`Call` on sheikh side;
   `SheikhDetailsScreen`→`Accepted`→`Call` on student side).
9. EN/AR strings for everything new.

## Explicitly out of scope

- Telecom `ConnectionService` self-managed integration (system phone UI, Bluetooth/car
  integration, survives even a Force Stop) — a large separate undertaking; `stopWithTask=false` +
  the persisted-record rejoin path covers the requested behavior without it.
- Picture-in-picture floating video bubble — a reasonable follow-up, not part of "keep the audio
  alive with notification controls."
- FCM-based silent call restore — Phase 3 in `Meeting-Feature-Status.md` is still not started at
  all; the rejoin-card path is the interim answer to the same gap.

## Verification

No device/emulator is available in this environment (per `Meeting-Feature-Status.md`, every prior
pass in this feature has been `compileDebugKotlin`-verified only) — same here:
`:meeting:presentation`, `:meeting:data`, `:presentation`, `:sheikh:presentation`, `:app`,
`:sheikh-app` must all compile clean, but genuine verification needs an on-device manual pass:
- Start a call, press Home → audio continues, notification appears with correct name/mute state.
- Mute/unmute and End from the notification actions.
- Swipe the app from Recents mid-call → call survives (service alive), notification still there.
- Reopen via app icon → lands straight back in `CallScreen` (case 2).
- Reopen via notification tap → same (case 1).
- Force-stop the app mid-call, relaunch → `OngoingCallCard` on Home, tap → rejoin or graceful
  "already ended" depending on real backend state (case 3).
- Repeat the above on `:sheikh-app`.
- Arabic/RTL pass on the new card and notification text.
