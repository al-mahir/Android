# Meeting Feature — Status & Completion Guide

Tracks progress on the Circles + Sheikh 1:1 Meeting Requests feature described in
`API-Contract.md`, `Android-Agora-Implementation.md`, and `Circle-Sheikh-Meeting.postman_collection.json`.

Everything lives in a new, fully standalone Gradle module: **`:meeting-kit`**
(package `com.iti.meetingkit`). It has **zero compile dependency** on `:domain`, `:data`,
`:presentation`, `:designsystem`, or `:sheikh:presentation` — it ships its own network client,
STOMP client, MVI plumbing, models, and string resources, so it can in principle be dropped into
a different Android project. `:app` and `:sheikh-app` are the only consumers.

---

## 📍 Current status (as of this session) — read this first if picking up in a new chat

The happy path plus a cluster of real-backend edge cases are done (see "Post-launch edge-case
fixes" and its three follow-ups below): student 409/pending-request handling, stale request-screen
state, sheikh availability self-flipping offline, and — the big one, root-caused via on-device
logcat tracing — both the sheikh's and the student's Call screen not reliably navigating to an
**already-accepted, already-successfully-joined** call (reused-ViewModel stale state on the sheikh
side, one-shot effect-Channel flakiness on the student side). All of it is `compileDebugKotlin`
clean but **only spot-checked on-device for the specific repro steps in this session** — not a
full regression pass.

**Not yet addressed — next up**: process death / app-stop-and-relaunch mid-flow. None of the
in-memory state added or relied on so far (`AvailabilityViewModel`'s `Busy`/`IncomingRequest`,
`CallViewModel`'s `engine`/`requestId`, the student's `RequestUiState.Accepted`, the Nav3
backstack itself — `remember { mutableStateListOf(startDestination) }`, not
`rememberSaveable`) survives the process being killed and the app relaunched. Concretely, still
open:
- Sheikh or student killed **mid-call**: on relaunch there's no indication a call was ever
  active, and nothing tells the backend this participant left (`POST .../end` never fires) —
  relies entirely on whatever backend-side TTL/timeout exists, which isn't confirmed.
- Sheikh killed while **Available**: heartbeat stops (process dead) so the backend's own TTL
  (~45s, per `AvailabilityHeartbeat`'s docstring) should revert them to OFFLINE — this part is
  probably fine by design, but not verified against the real backend's actual TTL value.
- Sheikh killed while **Busy** (mid-accepted-call): same as "killed mid-call" above — relaunch
  drops them back to a fresh `AvailabilityViewModel` (`Offline` initial state), no memory of the
  in-progress call, no `end` call sent.
- Student's `PendingMeetingRequest` (Bug 1's fix) is the **only** piece of state in this whole
  feature that already survives process death today, via the DataStore-backed
  `PendingMeetingRequestStore` — everything else above does not.

---

## ✅ Done — Phase 0: Module scaffold & shared infra

- Gradle module `:meeting-kit` registered in `settings.gradle.kts`, own `build.gradle.kts`
  (OkHttp-engine Ktor client family for both REST and WebSockets, Koin, Compose Material3,
  Navigation3 `NavKey`, Agora `full-sdk`).
- `core/config/MeetingKitConfig.kt` — reads `local.properties` via this module's own
  `BuildConfig` (`REST_BASE_URL`, `WS_BASE_URL`, `AGORA_APP_ID`). New `local.properties` keys:
  `meetingAgoraAppId` (**empty — you must fill this in with a real Agora App ID to test calls**),
  `meetingWsBaseUrl` (optional; derived from `baseUrl` — `https→wss`, `http→ws` — if left blank).
- `core/auth/MeetingAuthTokenProvider.kt` / `MeetingCurrentUserProvider.kt` — the seam host apps
  use to bridge their own `TokenStore` in without `:meeting-kit` depending on `:data`. Wired in
  `AlMahirApp.kt` / `SheikhApp.kt` via `GlobalContext.get().get<TokenStore>()`.
- `core/mvi/{StateHolder,EffectPublisher,ObserveEffect}.kt` — duplicated from
  `:sheikh:presentation`'s convention (package renamed only).
- `network/MeetingHttpClient.kt` + `MeetingApi.kt` — REST client + endpoint path constants.
- `realtime/{StompFrame,StompFrameParser,StompClient,SocketEventEnvelope}.kt` — **hand-rolled**
  STOMP-over-WebSocket client (no third-party STOMP/RxJava library). Reconnect with exponential
  backoff (1s→2s→4s→8s, cap 30s), automatic re-subscribe, `reconnected: SharedFlow<Unit>` signal
  for callers to run their REST reconcile-GET per the API contract's mandated rule.
- `agora/{AgoraEngineWrapper,AgoraVideoViews,AgoraPermissions}.kt` — Agora RTC join/leave +
  Compose video rendering + CAMERA/RECORD_AUDIO runtime permission helper.
- `navigation/{MeetingRoute,MeetingNavigation}.kt` — `MeetingRoute : NavKey` sealed destinations,
  `meetingEntries(...)` extension registered in both apps' `entryProvider {}` blocks.
- `core/di/MeetingKitModule.kt` — `fun meetingKitModule(config, tokenProvider, currentUserProvider): Module`,
  the single entry point both apps register in their `startKoin { modules(...) }`.

## ✅ Done — Phase 1: Sheikh 1:1 Meeting Requests

**Sheikh side** (`meetingrequest/sheikh/availability/`):
- `AvailabilityHeartbeat` — `PUT /api/sheikh/{id}/availability` every 20s while "available";
  backend TTL (~45s) is the safety net if the app dies.
- `AvailabilityViewModel` — owns the heartbeat, subscribes `/topic/sheikhs/{sheikhId}/requests`,
  handles `SHEIKH_MEETING_REQUEST_RECEIVED` / `REQUEST_CANCELLED`, local countdown against
  `expiresAt` (doesn't wait for a server push to expire), accept/decline.
- `SheikhAvailabilityPanel` — a `@Composable` **slot**, not a nav route. Embedded into
  `SheikhHomeScreen` from `SheikhAppNavigation.kt` (the one place `:sheikh-app` bridges
  `:meeting-kit` into `:sheikh:presentation`'s UI, via a lambda param — `:sheikh:presentation`
  itself still has zero dependency on `:meeting-kit`).

**Student side** (`meetingrequest/student/`):
- `browse/` — `SheikhBrowseScreen`, lists `GET /api/sheikh?availability=AVAILABLE`.
- `request/` — `MeetingRequestScreen`, optional note → `Sending → Pending → Accepted/Declined/Expired`,
  subscribes `/topic/students/{studentId}/meeting-requests/{requestId}`.
- Entry point: **"Request 1:1 Meeting" button on the existing `SheikhDetailsScreen`**
  (`presentation/src/main/java/com/iti/presentation/sheikh/SheikhDetailsScreen.kt`) — pushes
  `MeetingRoute.SendMeetingRequest(sheikhId)`. `MeetingRoute.SheikhBrowseList` (the dedicated
  "browse only available sheikhs" screen) is wired into `meetingEntries` but **has no nav trigger
  yet** — nothing currently pushes it. Add a button wherever you want that flow to start (e.g. Home).

**Shared** (`call/`): `CallScreen`/`CallViewModel` — requests permissions, joins the Agora channel,
renders local/remote video. Used by both the meeting-request "accepted" path and (once built)
the Circle "joined" path.

**Deleted** (old fake, client-only availability — no real persistence, no backend call):
`domain/repository/AlmahirSheikhRepository.kt`, `domain/usecase/sheikh/{Observe,Set}MyAvailabilityUseCase.kt`,
`data/sheikh/local/AlmahirSheikhLocalDataSource.kt`, `data/sheikh/remote/AlmahirSheikhRemoteDataSource.kt`,
`data/sheikh/repository/AlmahirSheikhRepositoryImpl.kt`, `data/sheikh/di/AlmahirSheikhDataModule.kt`,
`sheikh/presentation/home/components/{SheikhAvailabilityCard,SheikhAvailabilityIllustration}.kt`.
`domain/model/Sheikh.kt`'s `SheikhAvailability` enum (`AVAILABLE/IN_SESSION/OFFLINE`) was **kept** —
it's still used by the unrelated "browse all sheikhs" profile feature.

**Verified**: `:meeting-kit`, `:app`, `:sheikh-app` all compile clean (forced rerun, not just cache).

---

## 🧪 How to try Phase 1

1. **Add a real Agora App ID** to `local.properties`: `meetingAgoraAppId=<your-app-id>`. Without
   it, `CallScreen` will call `RtcEngine.create` with an empty string and fail to join — everything
   up to that point (availability toggle, browse, send/accept/decline) works without it.
2. **Backend must implement** the new endpoints in `API-Contract.md` §13 (`PUT/GET
   /api/sheikh/{id}/availability`, `POST /api/sheikh/{id}/meeting-requests`,
   `DELETE/accept/decline /api/meeting-requests/{id}`) and the STOMP topics in §14. If the backend
   doesn't have these yet, the UI will show error/idle states on every action — that's expected,
   not a client bug.
3. Run `:sheikh-app` → toggle availability on Home → should start the heartbeat.
4. Run `:app` (as a different logged-in user) → open a sheikh's profile → "Request 1:1 Meeting" →
   send → should see "Waiting for the sheikh to respond…".
5. Back on `:sheikh-app`, the incoming-request card should appear with a live countdown if the
   STOMP connection and topic contract match what's below (see **Assumptions to verify**).

### ⚠️ Assumptions to verify against the real backend

These weren't 100% pinned down by the docs and were filled in with the most consistent reading —
**check them against the actual backend before assuming a bug is client-side**:
- **STOMP message envelope**: every event is assumed to arrive as
  `{ "eventType": "...", "payload": {...} }` (`realtime/SocketEventEnvelope.kt`) — this matches how
  `API-Contract.md` §7 documents Circle events explicitly, and was extended to the Sheikh
  request/status topics in §14 which don't show the wrapper as explicitly. If the backend sends
  the Sheikh-topic payloads unwrapped instead, `AvailabilityViewModel.handleFrame` /
  `MeetingRequestViewModel.handleFrame` need their JSON decoding adjusted.
- **`GET /api/sheikh` response envelope**: assumed to be the pre-existing
  `{ "success": true, "data": [...] }` wrapper (confirmed for the *existing* Sheikh Management API
  via `data/datasource/sheikh/SheikhRemoteDataSource.kt`), applied to the new
  `?availability=AVAILABLE` filtered call too. See `network/dto/ApiEnvelope.kt` +
  `MeetingRequestApi.getAvailableSheikhs()`.
- **STOMP auth**: JWT sent as an `Authorization: Bearer <token>` **STOMP CONNECT header** (matches
  "All REST/WS connections require Authorization: Bearer <JWT>" in §0). Confirm the backend's
  Spring STOMP config actually reads the token from there and not from the WS handshake URL/query
  param instead.

### ⚠️ Current Testing Environment (Dummy Backend)

**Note:** The Android team has fully validated the entire MVI, REST, and STOMP architecture end-to-end using a custom lightweight Ktor dummy backend (running locally with mocked STOMP messaging). 

* **Architecture fixes applied:** Resolved the WebSocket protocol configuration (`ws://` vs `http://`), injected the missing Ktor `WebSockets` plugin into `MeetingHttpClient`, and fixed a critical race condition in `StompClient.kt` where `SUBSCRIBE` frames were dropped if called before the websocket connection finished establishing.
* **Agora Token behavior:** We successfully verified the Agora call flow. A 32-character App ID is invalid as a token (Error 110). When testing, `agoraToken` must either be a real RTC token generated from the Agora console, or an empty string `""` (only permitted if the Agora project has App Certificate disabled).
* **Transition Plan:** The frontend is completely unblocked. We will change the dummy backend connection variables to point to the actual backend just as soon as the backend team finishes their REST/STOMP implementation!

---

## ✅ Done — Sheikh/student meeting UI redesign (design system + animation pass)

Redesigned the raw-Material3 screens from Phase 1 to match the app's `:designsystem` (Verdant
theme tokens) and added state-transition animations. Note: this landed in the module layout the
feature actually ended up in — `:sheikh:presentation` (package
`com.iti.sheikh.presentation.availability`) and `:presentation` (package
`com.iti.presentation.meetingrequest.request`) — **not** the standalone `:meeting-kit` module
described at the top of this doc; that extraction never happened (see the module-layout note
under Phase 1 below/in code review history).

**Sheikh home / availability panel** (`sheikh/presentation/src/main/java/com/iti/sheikh/presentation/availability/`):
- `components/AvailabilityToggleRow.kt` — replaced the raw `Switch`/`Text` row with a
  `Theme`-tokened status card: a pulsing "live" status dot (animates only while available),
  animated container/indicator color transition (`animateColorAsState`), and the `Switch`
  recolored via `SwitchDefaults.colors` to the brand palette instead of default Material3 colors.
- `components/BusyIndicator.kt` — matching status-card treatment, amber/warning-themed with a
  breathing-dot pulse.
- `components/IncomingRequestCard.kt` — `InitialsAvatar`, an animated countdown ring
  (green → amber → red as `expiresAt` approaches) instead of a plain "Expires in Ns" text, and
  `PrimaryButton`/`SecondaryButton` (with check/cancel icons) instead of raw `Button`/`OutlinedButton`.
- `SheikhAvailabilityContent.kt` — the `Offline/Available/IncomingRequest/Busy` state switch is
  now wrapped in `AnimatedContent` (fade + scale) instead of a hard cut.

**Student request flow** (`presentation/src/main/java/com/iti/presentation/meetingrequest/request/MeetingRequestContent.kt`):
- Full rewrite: design-system `TextField`/`PrimaryButton`/`SecondaryButton`, a hero-icon treatment
  per state (chat/clock/check/cancel in a tinted circle), the same pulsing countdown-ring pattern
  as the sheikh-side card on the `Pending` ("waiting for response") state, and `AnimatedContent`
  transitions between `Idle → Sending → Pending → Accepted/Declined/Expired`.
- `presentation/src/main/java/com/iti/presentation/sheikh/SheikhDetailsScreen.kt` — the
  "Request 1:1 Meeting" entry-point button swapped from a raw `Button` to `PrimaryButton`.

**Localization fix**: `sheikh/presentation` and `presentation`'s `values-ar/strings.xml` were
missing **all** `meetingrequest_*` Arabic translations (English-only since Phase 1 shipped). Added
the missing translations plus new EN/AR copy for the redesigned states in the same change.

**Verified**: `:sheikh:presentation`, `:presentation`, `:app`, `:sheikh-app` all `compileDebugKotlin`
clean. **Not yet verified**: on-device/emulator run — no Agora App ID is configured in this
environment (see the Phase 1 assumptions above), so the new animations haven't been eyeballed live
yet. Worth a manual pass on both a phone and an Arabic/RTL locale before considering this closed.

---

## ⏳ Not started — Phase 2: Circles

Group sessions: create (public/private, participant limit, require-approval toggle), search public
circles, join by ID (private) or search result (public), host approval queue, shared `CallScreen`
on join. Full endpoint/topic list: `API-Contract.md` §1–§11.

**New `:meeting-kit` files needed** (mirrors the `meetingrequest/` package shape):
- `model/{CircleSummary,CircleVisibility,CircleTopicType,JoinRequestResult}.kt`
- `network/dto/CircleDtos.kt` (create/status/search/approve/reject request+response shapes)
- `circle/data/{CircleApi,CircleRepository(+Impl)}.kt`
- `circle/create/` — `CreateCircleViewModel`/`Screen`/`Content` (name, topicType/topicValue,
  visibility, participantLimit stepper default 10, requireApproval toggle default true)
- `circle/search/` — `CircleSearchViewModel`/`Screen`/`Content` (`GET /circles?visibility=PUBLIC&search=`)
- `circle/lobby/` — guest waiting-room. `CircleLobbyUiState` sealed
  (`Idle|Requesting|Lobby|Denied|InCall|Full|Error`), `requestToJoin()` **must branch on the
  response shape** (200 vs 202 vs 409), never on a locally-cached `requireApproval` flag (host
  could've changed it). Subscribe `/topic/circles/{id}/guest/{guestId}` on 202. On
  `StompClient.reconnected` while in `Lobby`, must call `GET /circles/{id}` to reconcile.
- `circle/host/` — `CircleHostViewModel`/`Screen` — subscribe `/topic/circles/{id}/host` for
  `GUEST_WAITING`/`GUEST_LEFT`/`PARTICIPANT_JOINED`/`PARTICIPANT_LEFT`; `approve()`/`reject()`/`end()`;
  a `409 CIRCLE_FULL` from `approve()` must surface distinctly, not as a generic error.
- Extend `MeetingRoute`/`meetingEntries` with `CircleSearch`, `CreateCircle`,
  `CircleLobby(circleId)`, `CircleHost(circleId)` (already stubbed as sealed variants in
  `MeetingRoute.kt` — just need their `entry<>{}` bodies).
- Both guest and host flows land in the **existing** `call/CallScreen.kt` — don't build a second
  call UI.

### Old fake Circle scaffolding to delete (once the real thing lands)

- `domain/model/StudyCircle.kt`, `domain/repository/CircleRepository.kt`,
  `domain/usecase/circle/{GetStudyCirclesUseCase,JoinStudyCircleUseCase,CancelJoinCircleUseCase}.kt`
- `data/datasource/circle/{CircleDataSource,FakeCircleDataSource}.kt`
- `presentation/circle/` (entire directory — `CircleListScreen`/`ViewModel`,
  `JoiningCircleScreen`/`ViewModel` (fake 4s-delay "approval"), `InSessionScreen`/`ViewModel`
  (hardcoded fake participants), plus their `state/*Contract.kt` files)

### Edits required in the same change (these must land together)

`AlmahirRepositoryImpl` currently implements `AlmahirRepository, SheikhRepository,
CircleRepository, RecitationSessionRepository` with constructor
`(dataSource, sheikhDataSource, circleDataSource, dao, appPreferencesDataStore)`. Removing
`CircleRepository` means:
- `data/repository/AlmahirRepositoryImpl.kt` — drop the `CircleRepository` interface + the
  `circleDataSource` constructor param + the `// ── CircleRepository ──` method block +
  the `StudyCircle` import.
- `data/di/AlmahirDataModule.kt` — drop the `CircleDataSource`/`FakeCircleDataSource`/
  `CircleRepository` bindings, and update the `AlmahirRepositoryImpl(get(), get(), get(), get(), get())`
  call to 4 args.
- `app/navigation/AppNavigation.kt` — remove `AppRoute.CircleList`/`JoiningCircle`/`InSession` +
  their `entry<>{}` blocks + the `com.iti.presentation.circle.*` imports; rewire `HomeScreen`'s
  `onOpenCircleList` and `SheikhDetailsScreen`'s `onNavigateToJoiningCircle` to push
  `MeetingRoute.CircleSearch`/`CircleLobby` instead.
- `presentation/di/PresentationModule.kt` — grep for and remove any
  `CircleListViewModel`/`JoiningCircleViewModel`/`InSessionViewModel` Koin bindings.
- Check `domain/src/test` and `presentation/src/test` for any test referencing the above before
  deleting (none were found as of Phase 1, but re-check — new tests may have been added since).

---

## ⏳ Not started — Phase 3: FCM Push

Deliberately out of scope through Phase 2 — the app has **no Firebase Messaging dependency, no
`FirebaseMessagingService`, no `google-services.json`** committed. When picked up:
- Add `:meeting-kit`'s own `push/MeetingFcmMessageHandler.kt` — a **pure function** mapping
  `RemoteMessage.data` → a sealed `MeetingPushEvent`. Keep only the mapping function in
  `:meeting-kit`, not the actual `FirebaseMessagingService` class, so the module keeps its
  zero-Firebase-dependency posture.
- Each host app (`:app`, `:sheikh-app`) owns its own `FirebaseMessagingService` (new, manifest-
  registered) that delegates to the mapping function and builds the deep link.
- Payload shapes: `API-Contract.md` §9 (Circles) / §15 (Sheikh requests).
  `Android-Agora-Implementation.md` §7 / §10.3 for the Android-side deep-link handling pattern
  (note `MEETING_REQUEST_ACCEPTED` deep-links straight to `/call`, skipping any lobby UI).

---

## 🚧 In progress — Phase 1.5: Real backend cutover (instant meetings)

The real backend's Swagger contract for `/api/instant-meetings/...` differs from the guessed
dummy-backend contract Phase 1 was built against. Cutting over module-by-module; this section is
appended to after each step (see commit history / this doc's edit timeline for order).

**Step 1 — config (done)**: `local.properties`' `meetingRestBaseUrl`/`meetingWsBaseUrl` are now
blank by default; `app/build.gradle.kts` and `sheikh-app/build.gradle.kts` derive them from
`baseUrl` (`https→wss`, `http→ws`) instead of defaulting to `localhost:8080`. Override either
property explicitly if the meeting feature ever needs a different host than the main API.
**Unverified**: `MeetingWsDestinations.CONNECT_PATH = "/ws"` — the real backend's STOMP endpoint
path was never confirmed; if the WS connection fails after this cutover, check this first.

**Step 7/8 — Agora `userAccount` switch + call lifecycle + nav plumbing (done)**:
`AgoraEngineWrapper.joinChannel` now calls `RtcEngine.joinChannelWithUserAccount(...)` instead of
`joinChannel(..., uid: Int, ...)`, matching the real accept/token response shape. Added
`renewToken(token)` to the wrapper. `CallViewModel` now takes a `MeetingRepository` too, keeps the
`requestId` it was joined with, subscribes the unified `/topic/meeting-requests/{requestId}` topic
for `MEETING_ENDED` (new terminal `CallUiState.Ended`, `CallScreen` pops via `onLeave` when it
fires), and actually implements `onTokenPrivilegeWillExpire`/`onRequestToken` by calling `GET
.../token` and `engine.renewToken(...)` — previously these Agora callbacks were logged and
ignored, so a token expiring mid-call would have silently dropped the connection. Added
`CallViewModel.endCall()`, wired to the in-call "leave" button, which calls `POST .../end` so the
backend resets the sheikh to AVAILABLE — previously leaving the call never told the backend
anything, so the sheikh would stay stuck BUSY server-side (until whatever TTL/timeout the backend
enforces) even though the client-side heartbeat isn't involved in that field.

`MeetingRoute.Call` dropped `circleId: String`/`uid: Int` in favor of `requestId: String`/
`userAccount: String` (there is no "circle" in this flow — that field was a Phase-1 modeling
mistake, inherited from copying the Circles shape). Updated all call sites: `MeetingNavigation.kt`,
`MeetingRequestNavigation.kt`, `MeetingRequestScreen.kt`, `SheikhAvailabilityPanel.kt`,
`AppNavigation.kt`, `SheikhAppNavigation.kt`. `MeetingModule.kt`'s `meetingCallModule` now injects
`MeetingRepository` into `CallViewModel`.

**Verified**: `:meeting:domain`, `:meeting:data`, `:meeting:presentation`, `:sheikh:presentation`,
`:presentation`, `:app`, `:sheikh-app` all `compileDebugKotlin` clean (forced, not cache-only).
**Not yet verified**: on-device run against the real backend — steps 1–5 and 7–10 close every gap
that was resolvable purely from the Swagger contract you pasted, but the sheikh's "new incoming
request" notification channel (step 6, see below) is still unresolved, so the sheikh-side accept
flow can't be exercised end-to-end yet even against the real backend. Worth a manual pass on the
student side alone (browse → request → pending countdown → decline/expire) once you have a real
student JWT to test with, since that half doesn't depend on step 6 at all.

### ✅ Done — Post-launch edge-case fixes (real-backend field testing)

Four bugs found while testing against the real production backend
(`almahir-production.up.railway.app`), fixed in one pass:

1. **Student: 409 "already have a pending request" showed as "sheikh unavailable"**.
   `MeetingRepositoryImpl.sendMeetingRequest()` collapsed every `409` into
   `SendMeetingRequestResult.SheikhUnavailable`, discarding the response body. Now decodes the
   body (`MeetingErrorResponseDto`) and returns a distinct `AlreadyPending(message)` when the
   message mentions "pending". Since the real backend has **no** `GET` for "my current pending
   request" (confirmed against the live `/v3/api-docs` Swagger — only sheikh-side `GET
   .../sheikh/pending` exists), the client now persists its own record locally
   (`meeting/data/.../local/PendingMeetingRequestStore.kt`, DataStore-backed, survives app
   restarts) whenever `sendMeetingRequest` succeeds, and clears it on any terminal event
   (accepted/declined/cancelled/expired/ended) observed via the existing STOMP subscription. Home
   now shows a `PendingMeetingRequestCard` (countdown + View/Cancel) whenever this local record is
   non-null, and the request screen's `AlreadyPending` state offers "Cancel that request" inline.
2. **Student: re-requesting the same sheikh stuck until app restart** after a call ended. Root
   cause: the `MeetingRequestScreen`'s `ViewModel` could end up parked on a terminal state
   (`Ended`/`Declined`/`Expired`) when revisited. Fixed with a defensive `LaunchedEffect(sheikhId)`
   in `MeetingRequestScreen` that resets to `Idle` on first composition if the state is terminal —
   self-heals regardless of the underlying Navigation3/Koin `ViewModelStore` lifecycle.
3. **Sheikh: availability silently flipped to unavailable** after (or even just navigating away
   from) a call. Root cause: `SheikhAvailabilityPanel.kt` had a `DisposableEffect(Unit) { onDispose
   { ToggleAvailability(false) } }` that fired on **any** navigation away from Home (not just
   explicit toggle-off), since the panel is only composed while Home is the top backstack entry.
   Removed entirely — availability now only changes via explicit user toggle or the
   accept/decline/call-end flow; the backend's own ~45s TTL remains the safety net for a
   genuinely killed app.
4. **Sheikh: stuck on "Busy", never reached the call after Accept**. Same root cause as #3 (the
   errant `onDispose` overwrote `Busy` back to `Offline` and cancelled the
   `MEETING_ENDED`-watcher job right as `Call` was pushed). Also hardened
   `AvailabilityHeartbeat` with a `pause()` (stops the loop without PUTting `OFFLINE`) called on
   `accept()`, so it stops blindly re-PUTting `AVAILABLE` every 20s while the sheikh is actually in
   a call; resumes via `start()` when `MEETING_ENDED` fires.

**Verified**: `:meeting:domain`, `:meeting:data`, `:presentation`, `:sheikh:presentation`, `:app`,
`:sheikh-app` all `compileDebugKotlin` clean. **Not yet verified**: on-device manual pass — no
emulator/device in this environment; re-run the exact repro steps (background/kill mid-request,
end a call from both sides, accept while the sheikh is on the call screen) on a real device before
considering this closed.

### ✅ Done — Follow-up: sheikh still stuck on "Busy" after a second Accept

Field-tested again after the fix above: ending a call and then accepting a *second* incoming
request still sometimes left the sheikh on Home showing "Busy" with no navigation to the call.
Root cause: navigation to `MeetingRoute.Call` was driven purely by a one-shot
`AvailabilityEffect.NavigateToCall` sent through a `Channel`-backed effect (`DefaultEffectPublisher`
— "single-consumer, no-replay" by design). If `SheikhAvailabilityPanel`'s `ObserveEffect` collector
isn't actively attached at the exact instant `accept()` emits (a recomposition gap is enough — e.g.
right as `SheikhAvailabilityPanel` remounts after returning from the previous call), the event is
gone for good; `StateFlow`-backed `state`, by contrast, always replays its latest value to any
(re)started collector. Fixed by moving the call-join payload (`requestId`/`token`/`channelName`/
`userAccount`) onto `AvailabilityUiState.Busy` itself (was a bare `data object`) and driving
navigation off that state in `SheikhAvailabilityPanel` via `LaunchedEffect(busyRequestId)` instead
of the effect Channel — removed `AvailabilityEffect.NavigateToCall` entirely to avoid a double-push
race between the two mechanisms. Also added a manual "Rejoin" button on `BusyIndicator` as a
visible fallback in case automatic navigation is ever missed for an unrelated reason (e.g. process
death mid-transition) — wired straight off the same state, no effect involved.

**Verified**: `:sheikh:presentation`, `:sheikh-app`, `:app` `compileDebugKotlin` clean. Still needs
an on-device pass — same caveat as above.

### ✅ Done — Follow-up: `POST .../end` not reliably called on every leave path

Per the confirmed Swagger contract, `POST /api/instant-meetings/{requestId}/end` ("Sheikh or
Student, only if part of the meeting") is what resets the sheikh to `AVAILABLE` and fires
`MEETING_ENDED` server-side — so every real "I'm leaving this call" path on the client must call
it exactly once, and tolerate the documented `409 Meeting is not currently active` (e.g. the other
party already ended it). Audited `CallScreen.kt`/`CallViewModel.kt` against this and found two
paths that skipped it entirely:
- `ErrorContent`'s leave button (shown on `CallUiState.Error`, e.g. a dropped Agora connection)
  called the raw `onLeave` nav-pop directly instead of `handleLeave` — leaving a dropped call
  without ever telling the backend, so the sheikh would stay stuck server-side until the other
  side left or a backend timeout kicked in.
- System/gesture back had no handler at all — `NavDisplay`'s own default back dispatcher just
  popped the `Call` entry silently, same gap as above.

Fixed: `ErrorContent`'s leave button now goes through `handleLeave` (which calls
`CallViewModel.endCall()` before popping), and added a `BackHandler(enabled = state !is
CallUiState.Ended)` in `CallScreen.kt` that routes system/gesture back through the same
`handleLeave` — disabled once `CallUiState.Ended` arrives (remote `MEETING_ENDED` already means
the other side ended it; calling `end` again would just hit the documented 409, which
`MeetingRepositoryImpl.endMeeting`'s existing `runCatching` already swallows harmlessly, but no
need to fire it at all in that case). The explicit in-call "leave" control-bar button already went
through `handleLeave` correctly — untouched.

**Verified**: `:meeting:presentation`, `:app`, `:sheikh-app` `compileDebugKotlin` clean.

### ✅ Done — Follow-up: "Rejoin" did nothing, then black screen stuck on "Connecting…"

Field-tested the "Rejoin" fallback button added above: tapping it appeared to do nothing, and
tapping repeatedly eventually led to a black screen stuck on the "Connecting…" spinner forever.
Three compounding bugs, all in `CallViewModel.kt`/`CallScreen.kt`/the nav wiring:

1. **No idempotency guard on the `Call` push** — `onMeetingAccepted` in both
   `SheikhAppNavigation.kt` and `AppNavigation.kt` unconditionally did
   `backStack.add(MeetingRoute.Call(...))`. Each Rejoin tap pushed *another* `Call` entry, and
   since each carries an Android permission request (`LaunchedEffect(Unit) {
   joinPermissionsLauncher.launch(...) }`), stacking several in quick succession raced multiple
   concurrent permission requests / Agora join attempts against each other. Fixed: both call sites
   now check `backStack.lastOrNull()` and skip the push if the top entry is already `Call` for the
   same `requestId`.
2. **Stale token on rejoin** — `AvailabilityUiState.Busy` (and thus the Rejoin button) replays the
   `agoraToken` from the *original* Accept response, which can be minutes old by the time Rejoin
   is tapped, or the meeting may have already ended server-side entirely by then. `joinChannel`
   now always calls `GET .../token` (`MeetingRepository.refreshToken`) first and joins with the
   refreshed `token`/`channelName`/`userAccount`, falling back to the passed-in ones only if that
   call itself fails (e.g. offline).
3. **`CallUiState.Connecting` could hang forever with no way out** — the old code optimistically
   flipped state to `InCall` immediately after calling `engine.joinChannel(...)`, *before* Agora's
   `onJoinChannelSuccess` actually fired, so a silently-failed/hung join never surfaced any error —
   just an infinite spinner (exactly the reported symptom). Fixed: `InCall` is now only set from
   `onJoinChannelSuccess` itself, and a 15s `joinTimeoutJob` (`CallViewModel.JOIN_TIMEOUT_MS`)
   flips to `CallUiState.Error` with a working leave button (which still correctly calls
   `endCall()`) if the join hasn't succeeded by then — cancelled on success, on any Agora error
   callback, and in `onCleared()`.

**Verified**: `:meeting:presentation`, `:app`, `:sheikh-app` `compileDebugKotlin` clean.

### 🩹 Follow-up: second accept still stuck, even with Rejoin — likely `CallViewModel` reuse

Field-tested again: closing call 1, then accepting a brand-new call 2, still got stuck ("Busy" +
Rejoin does nothing) — for a call that was never joined before, so "Rejoin" shouldn't even be the
path taken. Found a real, high-confidence bug: `CallViewModel.joinChannel()`'s guard (`if (engine
!= null ...) return`) has no way to distinguish "already joined *this* call" from "still holding a
stale `engine` from a *previous* call" — if Koin/Navigation3 ever reuses the same `CallViewModel`
instance's `ViewModelStore` across two separate accepted calls (the same category of doubt raised
earlier about `MeetingRequestViewModel` reuse for bug 2 — never conclusively resolved, since it's
a Nav3-internals question), the second `joinChannel()` call would silently no-op forever with no
error, no log, nothing — matching "why is Rejoin even showing, and why does nothing happen."

Fixed defensively regardless of the exact Nav3 cause: `joinChannel()` now compares the incoming
`requestId` against the previously-joined one; if they differ, it tears down the stale
engine/jobs first (`teardownForRejoin()`) before proceeding, and resets state to `Connecting`.
This makes a fresh join always possible no matter whether the ViewModel instance is new or reused.

**Also added verbose `Log.d`/`Log.w` tracing** (tag `MeetingLifecycle` in
`AvailabilityViewModel`/`SheikhAvailabilityPanel`/`SheikhAppNavigation`/`CallScreen`, existing tag
`MeetingCall` in `CallViewModel`) at every handoff in the chain: `accept()` REST result →
`Busy` state set → `SheikhAvailabilityPanel`'s auto-navigate `LaunchedEffect` → the backstack
push/skip decision → `CallScreen` composition → `refreshToken` result → `joinChannel`/stale-reset
decision. **If this reproduces again**, grep logcat for `MeetingLifecycle|MeetingCall` from the
moment Accept is tapped — the trace will show exactly which step didn't fire or produced an
unexpected value, instead of guessing blind again.

**Verified**: `:meeting:presentation`, `:sheikh:presentation`, `:app`, `:sheikh-app`
`compileDebugKotlin` clean. Genuinely unverified on-device — this is the third pass at this
specific symptom cluster; the added logging is there specifically because further blind fixes
without on-device evidence are no longer a responsible use of effort.

### ✅ Done — Root cause found via the logging above, on both sides

The `MeetingLifecycle`/`MeetingCall` trace from a real repro (close call 1 → accept call 2)
nailed it down precisely:

**Sheikh side — confirmed `CallViewModel` instance reuse, and the actual race.** The log showed
`CallScreen: composed requestId=<call2> state=Ended` — a *freshly pushed* `Call` entry for the
*new* call rendering `Ended`, the *previous* call's terminal state. Root cause: `joinChannel()`
only resets state after an async permission-grant callback resolves; in the gap before that,
`CallScreen`'s own `if (state is Ended) LaunchedEffect(Unit) { onLeave() }` fires off the stale
leftover value and immediately pops the screen back out — racing the real join that was about to
start underneath. (The underlying join actually succeeded per the `CallViewModel` log, but nobody
was there to see it since the screen had already been popped.) Fixed by adding
`CallViewModel.prepareForRequest(requestId)`, called via `remember(requestId) { ... }` in
`CallScreen` — **synchronously, during composition, before `state` is read for the first time** —
so a reused ViewModel's stale terminal state is reset before it can ever render. A
`LaunchedEffect` cannot do this: it still lets one stale frame through before it runs.
`joinChannel()` keeps its own stale-check as a safety net in case `prepareForRequest` is ever
skipped.

**Student side — the exact same effect-Channel flakiness as the sheikh's original bug, never
ported over.** The `CallViewModel` log showed the student's call 2 **joined successfully**, but
the student stayed stuck on `RequestUiState.Accepted`'s "being connected" screen — because
`MeetingRequestViewModel`/`MeetingRequestScreen` still navigated via the one-shot
`RequestEffect.MeetingAccepted` Channel, the exact mechanism already diagnosed and replaced with
state-driven navigation on the sheikh side earlier in this doc. Applied the identical fix here:
removed `RequestEffect.MeetingAccepted` entirely; `MeetingRequestScreen` now drives navigation off
`RequestUiState.Accepted` (already carried `requestId`/`token`/`channelName`/`userAccount`) via
`LaunchedEffect(acceptedRequestId)`.

**Verified**: `:meeting:presentation`, `:presentation`, `:sheikh:presentation`, `:app`,
`:sheikh-app` `compileDebugKotlin` clean.

### ✅ Done — Step 6: sheikh's incoming-request notification

Backend has confirmed the `messagingTemplate.convertAndSend("/topic/sheikhs/" + sheikhId + "/requests", new StompEventPayload<>("SHEIKH_MEETING_REQUEST_RECEIVED", event))` behavior. The Android implementation in `MeetingWsDestinations.sheikhRequests()` and `MeetingRepositoryImpl.observeIncomingRequests()` perfectly matches this backend contract. The UI is fully equipped to parse this event and push `AvailabilityUiState.IncomingRequest` so the sheikh can see the pending meeting. All downstream flow (`AvailabilityViewModel`, accept/decline logic, countdown ring) is already fully implemented and verified against this contract.

**Step 5 — MEETING_ENDED + reconcile-GET wiring (done)**: `MeetingRepository` gained a
`reconnected: Flow<Unit>` property (backed by `StompClient.reconnected`, previously dead code —
nothing consumed it). `AvailabilityViewModel` now reconciles via `getSheikhAvailability(sheikhId)`
on every reconnect while Available, and after accepting a request it subscribes the unified
`/topic/meeting-requests/{requestId}` topic so a `MEETING_ENDED` push (either side hanging up, or
a server-side timeout) resets `Busy → Available` automatically — previously nothing did this, so
the panel would stay stuck on "Busy" until the sheikh manually left the call screen and came back.
`AvailabilityHeartbeat.start()/stop()` dropped their `sheikhId` param (no longer needed for the
REST call). Student-side `MeetingRequestViewModel` dropped its `MeetingCurrentUserProvider`
dependency entirely (was only used to build the old per-student topic name) and its DI binding was
updated to match. Added a new terminal `RequestUiState.Ended` (+ EN/AR strings
`meetingrequest_request_ended*`) for when `MEETING_ENDED` arrives while the student is on the
Accepted screen.

**Step 4 — `MeetingRepository` domain contract + impl (done)**: added
`getSheikhAvailability(sheikhId)`, `endMeeting(requestId)`, `refreshToken(requestId)`; new domain
models `SheikhAvailability`, `TokenRefresh`; `MeetingRequestAccepted` dropped `circleId`, renamed
`sheikhAgoraToken`→`agoraToken`, `uid`→`userAccount: String`. `setMyAvailability` dropped its
`sheikhId` param (server derives it from the JWT now). `observeMeetingRequestEvents` collapsed
from `(studentId, requestId)` to `(requestId)` — single unified topic, used by both sides.
`MeetingRequestEvent` gained `Cancelled` and `MeetingEnded` variants. `observeIncomingRequests`
(the sheikh's new-request inbox) is **unchanged/still on the guessed topic** — same open gap as
step 2.

**Step 3 — DTOs + `MeetingApi.kt` (done)**: every response now unwraps `ApiEnvelope<T>.data`
(previously only `getAvailableSheikhs()` did). `MeetingRequestAcceptedDto` dropped `circleId`
entirely and renamed `sheikhAgoraToken`→`agoraToken`, `uid: Int`→`userAccount: String` to match
the real accept-response schema. Added `TokenRefreshDto` (`GET .../token`) and `endMeeting()`
(`POST .../end`, empty `data: {}`, response body ignored). `declineMeetingRequest` no longer sends
a `reason` body — the contract shows no request body for `/decline`, and the one call site
(`AvailabilityViewModel.decline()`) always passed `null` anyway, so this was dead capability, not
a feature removal. **Assumption kept from Phase 1, still unverified**: `sendMeetingRequest` still
sends `{"note": ...}` as a JSON body even though the Swagger "Try it out" for `POST .../request`
shows no request body section — kept to preserve the existing optional-note UI rather than
silently drop it; if the backend 4xxs on an unexpected body, this is the first thing to check.

**Step 2 — `MeetingEndpoints.kt` (done)**: rewritten to `api/instant-meetings/...` per the
Swagger contract. `PUT`/availability-status no longer takes `sheikhId` in the path (server derives
the sheikh from the JWT). Cancel is now `POST .../cancel` (was `DELETE`). Added `GET
.../availability`, `GET .../token`, `POST .../end`. WS destinations collapsed to a single
`/topic/meeting-requests/{requestId}` per the contract, used by both sides once a requestId
exists. **`sheikhRequests(sheikhId)` topic is UNCHANGED/still guessed** — the contract has no
documented way for a sheikh to learn about a new incoming request before a requestId exists
client-side; this is a real gap, not implemented in this pass (see "Open questions" below).

## Testing checklist (not yet written)

No automated tests exist yet for this feature. Before considering it production-ready:
- `StompFrameParserTest` — round-trip parse/serialize, header edge cases, heartbeat frames.
- `AvailabilityViewModelTest` / `MeetingRequestViewModelTest` — the state-transition matrices
  described in Phase 1 above (accept/decline/expire races, 409 handling).
- Once Phase 2 lands: `CircleLobbyViewModelTest` (200/202/409 branch matrix),
  `CircleHostViewModelTest` (409-on-approve surfaced distinctly).
- Manual checklist: `Android-Agora-Implementation.md` §9 (Circles) and §10.4 (Sheikh requests).
