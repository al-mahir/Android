# Meeting Call Lifecycle — Implementation Status

> Design reference: [`Meeting-Call-Lifecycle-Plan.md`](Meeting-Call-Lifecycle-Plan.md) — read that
> first for **why**; this file tracks **what's actually landed**, step by step, in implementation
> order, so progress can be followed incrementally. Parent feature doc:
> [`Meeting-Feature-Status.md`](Meeting-Feature-Status.md).

## Progress checklist

| # | Step | Status |
|---|------|--------|
| 1 | Fix missing `CAMERA` manifest permission (both apps) | ✅ Done |
| 2 | `CallSessionController` singleton (move engine ownership out of `CallViewModel`) | ✅ Done |
| 3 | `CallForegroundService` + `CallStyle` notification + channel + `POST_NOTIFICATIONS` | ✅ Done |
| 4 | Manifest wiring both apps (service declaration, permissions, `stopWithTask=false`) | ✅ Done |
| 5 | `ActiveCallRecord` store + `MeetingRepository` additions | ✅ Done |
| 6 | Reopen routing (both `MainActivity`s + nav hosts, notification tap / icon relaunch) | ✅ Done |
| 7 | `OngoingCallCard` on both Home screens (process-death rejoin path) | ✅ Done |
| 8 | Thread `remoteDisplayName` end-to-end (both sides) | ✅ Done |
| 9 | EN/AR strings for everything new | ✅ Done |

Legend: ⏳ not started · 🚧 in progress · ✅ done (compiles clean) · 🔍 done, needs on-device
verification.

---

## Step-by-step log

_Entries are appended below as each step lands — what changed, which files, and anything worth
flagging for the next step or for on-device testing._

### Step 1 — `CAMERA` permission

Added `android.permission.CAMERA` + `<uses-feature android:name="android.hardware.camera"
required="false">` to both `app/src/main/AndroidManifest.xml` and
`sheikh-app/src/main/AndroidManifest.xml`. Also caught and fixed a second, worse latent bug while
here: **`sheikh-app`'s manifest had no `RECORD_AUDIO` permission declared at all** — sheikh-side
calls have never been able to get microphone permission granted either. Added
`POST_NOTIFICATIONS`, `FOREGROUND_SERVICE_MICROPHONE`, `FOREGROUND_SERVICE_CAMERA` at the same
time (needed by Step 3/4, no reason to touch these files twice).

### Step 2 — `CallSessionController`

Moved the entire `AgoraEngineWrapper`-owning body of the old `CallViewModel` into a new Koin
`single`, `CallSessionController`
(`meeting/presentation/.../call/session/CallSessionController.kt`), holding a new
`CallSessionState` (`requestId`/`channelName`/`userAccount`/`remoteDisplayName`/`callState:
CallUiState`). Added `CallUiState.Idle` as the initial/no-call state. `CallViewModel` is now a
~50-line adapter: forwards intents to the controller, mirrors its `StateFlow` into the
`CallUiState` shape `CallScreen` already expects. `CallScreen.kt` treats `Idle` the same as
`Connecting`. DI: `meetingCallModule` now registers `CallSessionController` as `single` and injects
it into `CallViewModel`.

One surprise mid-port: `CallUiState`'s file lives at `call/state/CallUiState.kt` on disk but
actually declares `package com.iti.meeting.presentation.call` (not `...call.state`) — cost one
failed compile to notice; fixed the new files' imports to match reality, left the existing
(slightly misleading) file location alone since renaming it is out of scope here.

Also changed `endCall()`'s behavior slightly from the original: it now optimistically transitions
to `CallUiState.Ended` immediately (previously it only fired the `POST .../end` REST call and
waited for the `MEETING_ENDED` websocket echo to update state). Needed so the foreground service
(Step 3) can reliably know to stop even when ended via a path with no `CallScreen` around to
observe the round trip (e.g. the notification's End action).

**Verified**: `:meeting:presentation`, `:meeting:data`, `:presentation`, `:sheikh:presentation`,
`:app`, `:sheikh-app` all `compileDebugKotlin` clean (forced, not cache-only, on the actual Windows
Gradle build in this environment). **Not yet verified** on-device — this is a behavior-preserving
refactor in principle, but the `endCall()` change above is new behavior worth eyeballing once a
device is available.

### Step 3 — `CallForegroundService` + notification

New `CallForegroundService` (`meeting/presentation/.../call/session/CallForegroundService.kt`), a
plain `Service` (no `lifecycle-service` dependency needed) injected with `CallSessionController`
via Koin's `by inject()`. `onCreate` calls `ServiceCompat.startForeground(...)` immediately with
`FOREGROUND_SERVICE_TYPE_MICROPHONE or FOREGROUND_SERVICE_TYPE_CAMERA`, then collects
`controller.state` for the rest of its life — updates the notification on every change, calls
`stopSelfGracefully()` once `session.isLive` goes false. Notification actions
(`ACTION_TOGGLE_MIC`/`ACTION_END_CALL`) come back in as `Intent.action` on `onStartCommand` and
delegate straight to the controller. `CallSessionController.joinChannel` now calls
`CallForegroundService.start(appContext)` as its first step (before the async token
refresh/Agora join even begins) — the notification appears the instant a call starts connecting,
not once it's live, matching how Meet/WhatsApp behave. `appContext: Context` added to the
controller's constructor (via Koin `androidContext()`), used only to start the service — no
Activity context ever touches the singleton.

Notification uses `NotificationCompat.CallStyle.forOngoingCall(...)` (big-avatar "ongoing call"
treatment, native hang-up button) plus one bonus mute/unmute action. Added three vector drawables
(`ic_call_notification`, `ic_mic_notification`, `ic_mic_off_notification` — the same mic/mic-off
glyph shapes `CallScreen.kt` already uses via Compose Material Icons, just as static XML for the
notification) and `:meeting:presentation`'s **first-ever `res/values` + `res/values-ar`**
(`meeting_call_notification_*` strings) — correcting course from what the plan doc originally
said ("notification strings live in each host app's strings.xml"): that's not actually buildable,
since `CallForegroundService.kt` lives in `:meeting:presentation` and can only resolve string
resources it has compile-time `R` access to, and this module deliberately has zero dependency on
either host app. Plan doc updated to match. The content-intent deep link reuses the exact
`packageManager.getLaunchIntentForPackage(packageName)` trick `AudioPlaybackService` already uses
(`ACTION_OPEN_ACTIVE_CALL`), so the shared service can reopen whichever host app it's running
inside without depending on either app's `MainActivity` class.

`CallScreen.kt`'s existing `joinPermissionsLauncher` (already requesting RECORD_AUDIO + CAMERA)
now also requests `POST_NOTIFICATIONS` on API 33+ (`Build.VERSION.SDK_INT >=
Build.VERSION_CODES.TIRAMISU`), in the same multi-permission prompt — no separate dialog.

### Step 4 — Manifest wiring

`CallForegroundService` declared in both `app/src/main/AndroidManifest.xml` and
`sheikh-app/src/main/AndroidManifest.xml`:
`android:foregroundServiceType="microphone|camera"`, `android:stopWithTask="false"` (survives a
Recents swipe — matches WhatsApp/Zoom), `android:exported="false"` (nothing outside the app should
be able to start/bind it).

**Verified**: `:meeting:presentation:compileDebugKotlin` (including the new `res/` — first time
this module has shipped any resources), `:app:compileDebugKotlin`,
`:sheikh-app:compileDebugKotlin`, **and** `:app:processDebugMainManifest` /
`:sheikh-app:processDebugMainManifest` (manifest merge, not just Kotlin — catches things
`compileDebugKotlin` alone wouldn't, like a malformed `<service>` tag or a missing permission the
merger flags) — all clean on the actual Windows Gradle build in this environment. **Not yet
verified** on-device: whether `CallStyle` actually renders the way it's expected to (OEM
skinning varies), whether the notification icons look right, whether `stopWithTask="false"`
behaves as documented on a real device/Android version.

### Step 5 — `ActiveCallRecord` persistence

New `meeting/domain/.../model/ActiveCallRecord.kt` (`requestId`/`channelName`/`userAccount`/
`remoteDisplayName`) and `meeting/data/.../local/ActiveCallStore.kt` — a byte-for-byte structural
mirror of `PendingMeetingRequestStore` (own DataStore file `meeting_active_call_prefs`, same
`get`/`save`/`clearIfMatches`/`clear` shape). `MeetingRepository` gained
`observeActiveCall()/getActiveCall()/saveActiveCall()/clearActiveCall()`, wired through
`MeetingRepositoryImpl` and `MeetingDataModule`.

Two write paths, deliberately not one: `CallSessionController.persistActiveCall()` saves the
record right after `onJoinChannelSuccess` (so it's on disk as early as realistically possible —
not at `joinChannel()`'s start, since we don't have a confirmed channel/userAccount until the
refreshed token comes back). Clearing happens in two independent places rather than relying on
just one: `CallSessionController.endCall()` clears it immediately (the local "I hung up" case),
and `MeetingRepositoryImpl.observeMeetingRequestEvents`'s existing terminal-event handler now also
calls `activeCallStore.clearIfMatches(requestId)` specifically on `MeetingRequestEvent.MeetingEnded`
(the "other party hung up, or a server timeout fired, and I never got a chance to call `endCall()`
myself" case — e.g. the process died before it could). Deliberately *not* cleared on
`Accepted`/`Declined`/`Cancelled`/`Expired` the way the sibling `pendingRequestStore` is in that
same handler — those are pre-call request states, irrelevant to a call that's already in progress.

**Verified**: `:meeting:domain`, `:meeting:data`, `:meeting:presentation`, `:presentation`,
`:sheikh:presentation`, `:app`, `:sheikh-app` all `compileDebugKotlin` clean.

### Step 6 — Reopen routing

Both `MainActivity`s now share the same shape: a `pendingAction: MutableStateFlow<String?>` set
from `intent.action` in both `onCreate` and `onNewIntent`, threaded into the nav host as
`pendingAction`/`onActionHandled`. `:app` already had this (used for
`"ACTION_OPEN_MUSHAF_LISTEN"`); `sheikh-app`'s `MainActivity.kt` had **none of it** before this
step — added from scratch, mirroring `:app`'s implementation line-for-line.

Both nav hosts (`AppNavigation.kt`'s private `AppNavHost`, `SheikhAppNavigation.kt`'s private
`SheikhAppNavHost`) gained an identical `openActiveCallIfLive()` local function: reads
`CallSessionController.state.value` (injected via `koinInject()`) and, if `isLive` with a
requestId not already at the top of the backstack, pushes `MeetingRoute.Call`. Called from two
places: a `LaunchedEffect(Unit)` at nav-host startup (case 2 — app icon relaunch while the process
survived) and a `LaunchedEffect(pendingAction) { if (pendingAction ==
CallForegroundService.ACTION_OPEN_ACTIVE_CALL) ... }` (case 1 — notification tapped).

One deliberate shortcut: the pushed `MeetingRoute.Call.token` is always `""`. This is safe (not a
placeholder-that-might-break) because `CallSessionController.joinChannel()`'s very first guard is
`if (engine != null || joinTimeoutJob != null) return` — in both reopen cases the engine is by
definition already live, so the empty token is constructed but never actually used by
`CallScreen`'s re-triggered permission→join flow. This avoids adding a raw Agora token to
`CallSessionState` just to satisfy a route shape, keeping the token's lifetime as short as
possible.

Case 3 (cold start after real process death) is deliberately **not** handled here — that's Step 7,
since it needs the persisted `ActiveCallRecord` from Step 5, not the live controller.

**Verified**: `:app`, `:sheikh-app` `compileDebugKotlin` clean.

### Step 7 — `OngoingCallCard`

Two structurally-identical `OngoingCallCard` composables — `presentation/.../home/components/`
and `sheikh/presentation/.../home/components/` — not shared, since `:sheikh:presentation`
deliberately has zero dependency on `:presentation` (module boundary from AGENTS.md); each mirrors
its side's existing `PendingMeetingRequestCard`/equivalent styling exactly. Both wired through the
same MVI shape: `HomeUiState`/`SheikhHomeUiState` gained `activeCall: ActiveCallRecord?`
(populated via a new `observeActiveCall()` collecting `MeetingRepository.observeActiveCall()`,
same pattern as the existing `observePendingMeetingRequest()`), `HomeIntent`/`SheikhHomeIntent`
gained `RejoinActiveCallClicked`/`DismissActiveCallClicked`, `HomeEffect`/`SheikhHomeEffect`
gained `OpenActiveCall(requestId, token, channelName, userAccount)`.

`rejoinActiveCall()` (both ViewModels) is the actual case-3 liveness probe described in the plan:
calls `MeetingRepository.refreshToken(requestId)` — success means the call is still active
server-side and emits `OpenActiveCall` with the **freshly refreshed** token/channelName/userAccount
(not the persisted record's, which could be stale); failure clears the record and shows a "that
call has already ended" toast. `dismissActiveCall()` best-effort calls `endMeeting` then always
clears the local record regardless of that call's result — dismissing must never leave a stale
card behind even if the network call fails.

`SheikhHomeViewModel` didn't have `MeetingRepository` injected before this (only
`GetCurrentUserUseCase`/`ConnectivityObserver`) — added as a third constructor param, DI updated
in `SheikhPresentationModule.kt`. Both nav hosts wire `onOpenActiveCall` to push
`MeetingRoute.Call` with the real (non-empty, freshly-refreshed) token — unlike Step 6's reopen
path, this one **does** need a real token, since the controller is *not* live in this scenario
(that's the whole reason this path exists).

**Verified**: `:presentation`, `:sheikh:presentation`, `:app`, `:sheikh-app` all
`compileDebugKotlin` clean. EN + AR strings added in the same change
(`home_active_call_*`/`sheikh_home_active_call_*`) — this happened to cover most of what Step 9
would otherwise need to do for the Home-card surface; Step 9 is left to sweep anything still
missing plus double-check RTL layout.

### Step 8 — `remoteDisplayName` threading

`MeetingRoute.Call` gained `remoteDisplayName: String? = null`. Threaded from two distinct
sources depending on the flow, both of which already knew the name — this was pure plumbing, not
new lookups:
- **Sheikh side**: `AvailabilityUiState.IncomingRequest.studentName` (already known when a request
  arrives) → copied onto `AvailabilityUiState.Busy.remoteDisplayName` in
  `AvailabilityViewModel.accept()` → `SheikhAvailabilityPanel`'s two `onMeetingAccepted(...)` call
  sites (auto-navigate effect + manual "Rejoin" button) → `SheikhAppNavigation.kt`'s
  `MeetingRoute.Call(...)` construction.
- **Student side**: `MeetingRequestScreen`'s already-known `sheikhName` composable parameter
  (sourced from `SheikhDetailsScreen`'s "Request 1:1 Meeting" entry point) — **not** stored in
  `MeetingRequestViewModel`/`RequestUiState.Accepted` at all; `MeetingRequestScreen`'s existing
  `LaunchedEffect(acceptedRequestId) { onMeetingAccepted(...) }` just closes over the composable
  parameter directly when calling the now-5-arg `onMeetingAccepted`. Simpler than adding a field to
  the reducer state for something the UI layer already had in scope.

`CallSessionController.state.remoteDisplayName` and `ActiveCallRecord.remoteDisplayName` (Steps
2/5) were already carrying the field — Step 8 was about getting a real value *into* them instead
of always `null`, plus wiring `CallScreen`'s new `remoteDisplayName` param through to
`CallViewModel.joinChannel(...)`, and updating every `MeetingRoute.Call(...)` construction site
across both apps (Step 6's reopen paths now also forward `session.remoteDisplayName`/
`active.remoteDisplayName` instead of leaving it implicitly null).

**Verified**: `:meeting:presentation`, `:meeting:data`, `:meeting:domain`, `:presentation`,
`:sheikh:presentation`, `:app`, `:sheikh-app` all `compileDebugKotlin` clean, plus a final
`processDebugMainManifest` pass on both apps.

### Step 9 — Localization

Swept for anything still English-only after Steps 3–8 (which had already added their own EN/AR
pairs alongside the UI that needed them, rather than deferring — see those steps' entries above).
Found two **pre-existing** hardcoded English strings while sweeping, carried over verbatim from
the original `CallViewModel.kt` during Step 2's refactor, not introduced by this work:
`CallSessionController.kt`'s `CallUiState.Error("Couldn't connect to the call...")` /
`Error("Connection lost...")` / `Error("Call error ($err)")`. Left as-is — genuinely out of this
change's scope, and fixing them properly isn't a string-resource swap: `CallUiState.Error` carries
a raw `message: String` rather than a string-resource ID or error code, so localizing it means
restructuring that state shape, a separate refactor. Flagging here for whoever picks up this gap
next, rather than scope-creeping it into this pass.

Everything actually introduced by this work is fully EN+AR: `meeting:presentation`'s
`meeting_call_notification_*` (Step 3), `presentation`'s `home_active_call_*` and
`sheikh:presentation`'s `sheikh_home_active_call_*` (Step 7).

**Verified**: full `:app`/`:sheikh-app` `compileDebugKotlin` + `processDebugMainManifest` pass,
clean.

---

## All 9 steps done — what's left before this is actually shippable

Every step above compiles clean, but **none of it has been run on a real device or emulator** —
this environment has neither (same constraint the parent feature has lived with throughout, per
`Meeting-Feature-Status.md`). The Verification checklist in `Meeting-Call-Lifecycle-Plan.md` is
the concrete manual pass still needed before calling this production-ready: background/minimize
during a call, notification mute/end actions, Recents-swipe survival, all three reopen paths on
both apps, and an Arabic/RTL check on the new card and notification.

---

### 🩹 Follow-up: hang up (in-call or from the notification), then a fresh request → stuck on the rejoin card, can't actually rejoin

Field-reported regression of a bug class already chased down twice before this feature (see
`Meeting-Feature-Status.md`'s "reused-`CallViewModel`/stale-state" entries) — reintroduced by the
Step 2 `CallSessionController` port, which dropped two things the pre-port `CallViewModel` used to
do:

1. **`endCall()` never released the Agora engine or reset session identity.** It only flipped
   `callState` to `Ended` — never called `engine?.leaveChannel()/destroy()`, never cleared
   `requestId`/`channelName`/`userAccount`. The pre-port code did this teardown in `onCleared()`.
   Net effect: after hanging up (either the in-call button or the notification's "End Call" —
   both route through `CallSessionController.endCall()`), the engine stayed alive, still attached
   to the now-dead channel, and the controller's `state.requestId` stayed pinned to the just-ended
   call.
2. **`prepareForRequest()`'s `teardown()` released the engine but never reset the exposed
   `state`.** The pre-port `CallViewModel.prepareForRequest` explicitly did
   `teardownForRejoin(); updateState { CallUiState.Connecting }` — the port kept the teardown call
   but dropped the state reset. Combined with #1 always leaving a stale `requestId` behind,
   *every* subsequent call composes `CallScreen` on top of a controller still reporting the
   previous call's `Ended` state. `CallScreen` reads that state synchronously
   (`remember(requestId) { viewModel.prepareForRequest(requestId) }` then `collectAsStateWithLifecycle()`
   in the same composition) and its `if (state is CallUiState.Ended) LaunchedEffect(Unit) {
   onLeave() }` fires immediately — popping the new call's screen before `joinChannel()` (which
   needs an async permission-launcher round trip first) ever runs. User-visible symptom: hang up →
   send a new request → sheikh accepts → the call screen flashes and bounces straight back to the
   rejoin card, and tapping the card does nothing new because the same race repeats.

Fixed in `CallSessionController.kt`: split the old single `teardown()` into `releaseEngine()`
(engine + duration/join-timeout jobs only, leaves `state` untouched) and `teardown()`
(`releaseEngine()` + cancels `eventsJob` + `updateState { CallSessionState() }`, i.e. back to a
clean `Idle`). `endCall()` now calls `teardown()` instead of just flipping `callState`. The remote
`MEETING_ENDED` handler (`observeMeetingEnded`) now calls `releaseEngine()` before setting
`Ended` — releases the engine promptly on a remote hangup too (previously leaked until the next
call's stale-check happened to run), while deliberately still leaving `state` at `Ended` there
(not reset to `Idle`) since that path is the only way a `CallScreen` watching a remotely-ended call
knows to navigate itself away.

**Verified**: `:meeting:presentation`, `:meeting:data`, `:app`, `:sheikh-app`
`compileDebugKotlin` clean (forced, not cache-only). Still needs an on-device pass — same
caveat as every other entry in this doc; the specific repro to re-run: hang up a call (try both
the in-call Leave button and the notification's End Call action while backgrounded), send/accept a
brand-new request right after, confirm the new call actually connects instead of bouncing back.

---

### 🩹 Follow-up: student stuck on infinite "Connecting…" after leaving a call

Immediate fallout from the fix above, student-app-only (`:app`). Root cause is a *pre-existing*,
separate bug that the previous fix's `Ended → Idle` state change merely changed the visible
symptom of (from an infinite pop/push flicker to a genuine hung join attempt):

On the student side, `MeetingRoute.Call` is pushed from `MeetingRequestScreen`
(`MeetingRequestRoute.SendMeetingRequest`) once `RequestUiState.Accepted` fires — it sits directly
underneath `Call` on the backstack. Leaving the call only popped the single `Call` entry
(`meetingEntries(onBack = { backStack.removeLastOrNull() })` in `AppNavigation.kt`), which
re-reveals `MeetingRequestScreen`. Since Nav3's `NavDisplay` only composes the top backstack entry,
this is a **fresh composition** — and `MeetingRequestScreen`'s state is still parked on
`RequestUiState.Accepted` for the call that just ended (its self-heal `LaunchedEffect(sheikhId)`
only resets on `Ended`/`Declined`/`Expired`, never `Accepted` — see
`presentation/.../meetingrequest/request/MeetingRequestScreen.kt`). Its
`LaunchedEffect(acceptedRequestId) { onMeetingAccepted(...) }` fires again on that fresh
composition (a repeated key doesn't suppress it — there's no prior composition to compare against)
and immediately re-pushes `MeetingRoute.Call` for the *same, already-ended* `requestId`. Before the
fix above, this landed on a controller state still coincidentally equal to the old `requestId`, so
`prepareForRequest` treated it as *not* stale, state stayed `Ended`, and `CallScreen`'s own
`state is Ended → onLeave()` immediately popped it again — a fast, silent pop/push flicker loop.
After the fix above, `endCall()` resets the controller's `requestId` to `null`, so this same phantom
re-push now looks like a legitimate fresh join: `CallScreen` proceeds through the permission flow
and `joinChannel()`, which calls `refreshToken` on a `requestId` the backend has already closed —
that fails, falls back to the stale original token, and Agora either errors or the whole thing just
sits on the `Connecting…` spinner. Same underlying navigation bug either way; only the visible
failure mode changed.

Fixed in `AppNavigation.kt`'s `meetingEntries(onBack = ...)`: popping `Call` now also pops any
`MeetingRequestRoute` (`SendMeetingRequest`/`SheikhBrowseList`) it reveals, so leaving a call can
never land back on a screen that auto-navigates into it. `sheikh-app` was checked and does **not**
have this bug — the sheikh's `Call` is pushed straight from the `Home`-embedded
`SheikhAvailabilityPanel`, so leaving it always reveals `Home`, never an intermediate
auto-navigating screen.

**Verified**: `:app:compileDebugKotlin` clean. Still needs an on-device pass: send a request,
get accepted, join, leave (both the in-call button and backgrounding + notification End Call),
confirm landing back on `SheikhDetails`/wherever and *not* an immediate silent rejoin attempt.

---

### 🩹 Follow-up: same "infinite Connecting…" bug, sheikh side — `AvailabilityUiState.Busy` never resets locally

Same failure mode as the `:app` fix above, reproduced on `:sheikh-app` right after that fix landed
— same underlying bug *class*, different trigger, because the sheikh side's "resume into an
accepted call" mechanism is independent state, not nav-structure:

`SheikhAvailabilityPanel` (embedded directly in `SheikhHomeScreen`, not a separate nav route) drives
navigation into `Call` off `AvailabilityUiState.Busy` via
`LaunchedEffect(busyRequestId) { onMeetingAccepted(...) }` — same "state-driven nav so it's always
reachable" pattern as the student's `RequestUiState.Accepted`. `AvailabilityViewModel.Busy` only
resets to `Available` when the *remote* `MEETING_ENDED` echo arrives via its own
`observeActiveCall(requestId)` subscription (`AvailabilityViewModel.kt`) — there is no local/
synchronous reset when the sheikh explicitly taps Leave. Leaving `Call` pops back to `Home`, which
is a fresh composition of `SheikhAvailabilityPanel` (Nav3 only composes the top backstack entry) —
its `LaunchedEffect` re-fires against the still-`Busy(oldRequestId)` state (the async echo hasn't
arrived yet) and immediately re-pushes `Call` for the same already-ended `requestId`, which then
hangs trying to rejoin a meeting the backend already closed. Unlike the student side, `Home` is the
*correct* destination to land on after leaving, so the "skip the triggering intermediate screen"
fix used for `:app` doesn't apply here — `AvailabilityViewModel`'s `Busy` state itself is stale, not
the nav structure.

Can't fix this by having `AvailabilityViewModel` ask `CallSessionController` directly —
`sheikh:presentation` only depends on `:meeting:domain`, not `:meeting:presentation`, and adding
that dependency is a bigger module-boundary change than this bug warrants. Fixed instead at the
same host-app-nav layer as the `:app` fix (which already depends on both): added
`isPhantomReplayOfEndedCall(requestId)` in both `SheikhAppNavigation.kt` and (belt-and-suspenders,
symmetric) `AppNavigation.kt` — true when `CallSessionController.state.value.requestId == requestId
&& !isLive`. Guarded the `onMeetingAccepted` push site (covers both the auto-navigate
`LaunchedEffect` and the manual "Rejoin" button on `BusyIndicator`, since both call the same
lambda) and, on the student side, the `meetingRequestEntries` `onNavigateToCall` site.

This guard depends on `CallSessionController.endCall()` still exposing `requestId`/`Ended` after a
local hangup (not wiping to a blank `Idle`) — so `endCall()` was changed back to mirror
`observeMeetingEnded`'s remote-hangup handling exactly: release the engine, but keep `requestId`/
`channelName`/`userAccount` in `state` and surface `Ended` rather than resetting to `Idle`. This
does **not** reopen the earlier "rejoin card, can't rejoin" bug (`Meeting-Call-Lifecycle-Status.md`,
the first 🩹 entry above): that fix lives in `teardown()` (used by `prepareForRequest`/
`joinChannel` whenever a *genuinely different* next call starts) resetting fully to `Idle` — this
`Ended`-with-identity state only lingers until either a different call starts (fully reset) or the
async echo arrives (also resets it via the same `observeMeetingEnded` path), whichever's first.

One accepted cosmetic gap: `AvailabilityViewModel`'s own `Busy`/`BusyIndicator` UI still visually
lags for that same short window — it doesn't flip to `Available` until the echo arrives, so a
tapped "Rejoin" in that window is now silently swallowed by the nav guard rather than doing
anything, instead of still showing "Busy" indefinitely with a broken control. Full fix would give
`AvailabilityViewModel` its own local signal too, deferred — no way to do it cleanly without either
the cross-module dependency above or plumbing a new callback through `SheikhAvailabilityPanel`'s
already-wide parameter surface for what's now a narrow timing window, not a hang.

**Verified**: `:meeting:presentation`, `:app`, `:sheikh-app` `compileDebugKotlin` clean. Still
needs an on-device pass on both apps: leave a call, watch for a moment whether the `Busy`/Rejoin
indicator lingers (expected, cosmetic), confirm no phantom re-navigation/hang either way.

---

### ✅ Done — Audio routed to the earpiece, plus full in-call device controls

Field-reported: opening a meeting sometimes played the remote audio out of the **phone's earpiece**
instead of the speaker — intermittently, which is why it read as "no sound" rather than "wrong
output". Fixed, and the surrounding controls were rebuilt so the user can change any of it mid-call.

**Root cause.** `AgoraEngineWrapper`'s init block called `setEnableSpeakerphone(true)` at *engine
creation* time, before `joinChannel`. In `CHANNEL_PROFILE_COMMUNICATION` the SDK re-decides the
audio route as part of joining, so anything set beforehand is discarded — the sibling call,
`setDefaultAudioRoutetoSpeakerphone(true)`, only sets the fallback and isn't a "switch now" command.
Whether the pre-join call happened to survive depended on timing, hence the intermittency. The
missing `MODIFY_AUDIO_SETTINGS` permission was ruled out — `:meeting:presentation`'s own manifest
already declares it and it merges into both apps (verified in the merged manifests).

**The fix**: the route is now asserted from `onJoinChannelSuccess` — the first moment it sticks —
via `RtcEngine.setRouteInCommunicationMode()`, the API actually intended for in-call routing, and
re-asserted once ~1.2s later (`ROUTE_REASSERT_DELAY_MS`), because several OEM audio HALs finish
setting up the voice-call stream a beat after join and reset the route while doing so.

**New: user-controlled audio output.** `call/audio/AudioRouteController.kt` enumerates connected
outputs via `AudioManager.getDevices` and tracks connect/disconnect with an `AudioDeviceCallback`;
`AudioOutputDevice` maps them to `Constants.AUDIO_ROUTE_*`. Policy: an explicit user pick is sticky
until that device physically disconnects, otherwise wired headset > Bluetooth > speaker — never the
earpiece by default. The controller owns policy only and never touches Agora; `CallSessionController`
applies `selected` to the engine, and Agora's `onAudioRouteChanged` is folded back into the state so
the picker shows where audio actually is rather than where we asked for it. Bluetooth on API 31+
needs runtime `BLUETOOTH_CONNECT`, now declared in `:meeting:presentation`'s manifest and requested
**lazily** — only when the user picks Bluetooth in the sheet, not at call start.

**New: camera control.** `setCameraDirection(front)` (via `CameraCapturerConfiguration`) replaces
blind `switchCamera()` flipping, so front/back are explicit, idempotent, and survive a rejoin; the
state carries `isFrontCamera`. Torch support is exposed too, re-probed whenever the camera is turned
on or switched (the SDK reports "no torch" for a camera it isn't currently holding, so the value
taken at join time is not trustworthy).

**New: call UI.** `CallScreen` rewritten — top bar with the remote party's name and a duration /
reconnecting / waiting status pill; auto-hiding chrome (5s, suppressed while a sheet is open or
before the remote joins) with tap-to-toggle; tap-to-swap between the main stage and the PiP tile,
with the mic badge following whoever is in the tile; `InitialsAvatar` placeholders instead of a
generic person glyph; long-press on the camera controls opens the camera sheet. New
`call/components/` holds `CallControls.kt` (control bar, buttons, status pill) and
`CallOptionSheets.kt` (audio-output and camera sheets, built on the design system's
`AppBottomSheet`). Control-button convention: a filled white button means the capability is **off**.

**Localization gap closed.** `CallScreen` previously hardcoded every English string, and
`CallUiState.Error` carried a raw `message: String` — flagged as an open gap in Step 9 above and now
fixed: `Error` carries a `CallErrorReason` enum plus an optional diagnostic Agora code, and all call
UI text is EN + AR resources.

`AgoraVideoViews` also gained `onRelease` canvas detach on both local and remote views — required
now that swapping recreates those `SurfaceView`s, since the engine otherwise keeps rendering into a
destroyed surface.

**Verified**: `:meeting:presentation` `lintDebug` **clean** (the pre-existing `RememberReturnType`
error on `prepareForRequest` is resolved — kept synchronous-during-composition, which is
load-bearing, behind a documented `@Suppress` on a small helper), `:app`/`:sheikh-app`
`compileDebugKotlin` + `processDebugMainManifest` clean. All changes are confined to
`:meeting:presentation` — neither host app needed touching. **Not verified on-device** (same
constraint as every entry in this doc). The specific pass to run: join from both apps and confirm
audio comes out of the speaker by default; open the output sheet and switch between earpiece and
speaker; plug in wired headphones mid-call and confirm it auto-switches and reverts on unplug;
connect a Bluetooth headset and confirm the permission prompt appears only on selecting it; switch
front/back camera; check the whole screen in Arabic/RTL.

---

## Open questions / risks carried into this work

- No on-device/emulator in this environment (same constraint noted throughout
  `Meeting-Feature-Status.md`) — every step below is `compileDebugKotlin`-verified only until a
  manual on-device pass happens; see the Verification checklist in the plan doc.
- `NotificationCompat.CallStyle`'s exact rendering (which actions show as icons vs. text, avatar
  fallback) can vary by OEM/Android version — worth eyeballing on at least one real device before
  calling the notification UX "done."
- `refreshToken(requestId)` is being reused as a liveness probe for the Step 7 rejoin card; it was
  designed for token renewal, not as a status check — if the backend ever returns success with a
  token for an already-ended meeting, the rejoin card would need a dedicated status endpoint
  instead (none exists today, per `Meeting-Feature-Status.md`'s Phase 1.5 notes).
