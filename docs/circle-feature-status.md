# Qur'an Study Circles — Current State

Status summary of the Qur'an Study Circles feature across the **user (student) app** and the
**sheikh app**, and what remains outstanding. Contract reference:
[`docs/features/20-quran-study-circles-api.md`](features/20-quran-study-circles-api.md).

---

## 1. Scope

Two apps share the same backend (`almahir-production.up.railway.app`):

- **User app** (`:app` + `:presentation` + `meeting:domain`/`meeting:data`) — circles as a
  student: discover, join (with password / approval), track approval, join the live session.
- **Sheikh app** (`sheikh-app` + `sheikh:presentation` + shared meeting modules) — circle
  **admin** (create, approve/reject members, start/end, manage members). **Not built yet** —
  see §5.

---

## 2. User app — built & handled ✅

### 2.1 Domain (`meeting:domain`)

- Models: `Circle`, `CircleStatus`, `CircleType`, `CircleMember`, `CircleToken`,
  `PendingJoinRequest`, `CircleJoinResult`, `CreateCircleRequest`
  (`.../meeting/domain/model/circle/`).
- Repository interface `CircleRepository` — returns **stdlib `kotlin.Result`**; no use-case
  layer (presentation calls the repository directly).
- Members: `getPublicCircles()`, `getMyCircles()`, `getCircle()`, `createCircle()`,
  `joinCircle()`, `startCircle()`, `endCircle()`, `cancelCircle()`, `approveJoinRequest()`,
  `rejectJoinRequest()`, `leaveCircle()`, `removeMember()`, `getPendingRequests()`,
  `getMembers()`, `getToken()`, plus live event flows (`CircleRosterEvent`,
  `JoinRequestEvent`, `PendingJoinRequestEvent`).

### 2.2 Data (`meeting:data`)

- REST (`CircleApi`): `GET /api/circles`, `GET /api/circles/mine`, `GET/DELETE /api/circles/{id}`,
  `POST /api/circles`, `POST /api/circles/{id}/join|start|end|cancel|leave`,
  `approve|reject`, members, pending-requests, Agora token. Resilient decoding
  (`decodeBody`/`decodeList`) handles envelope, raw array, and wrapped `{content:[...]}` shapes.
- Realtime (`StompClient`, `StompFrame`, `StompFrameParser`): STOMP over `/ws` with
  `{eventType, payload}` envelopes; topics `/topic/circles/{id}/requests`,
  `/topic/circle-memberships/{membershipId}`, `/topic/circles/{id}`.
- `MeetingHttpClient`: OkHttp engine, `expectSuccess`, ContentNegotiation + WebSockets,
  30s/15s timeouts, `Authorization: Bearer <token>` attached per-request, HTTP logging tag
  `MeetingHttpClient` in debug builds.
- `CircleRepositoryImpl` wires REST + STOMP and maps DTOs → domain.

### 2.3 Presentation (`:presentation` — MVI)

| Screen | ViewModel | Flow |
|---|---|---|
| Home | `HomeViewModel` | "Your Circles" section (`myCircles`); when the user belongs to a circle, the current-circle card replaces the circles summary; `CircleClicked → OpenCircle` |
| Circle list | `CircleListViewModel` | public + joined (incl. PRIVATE) circles merged & deduped; **join-private-circle sheet**; current-circle card pinned on top |
| Sheikh details | `SheikhDetailsViewModel` | host's public circles (`CircleCard(onClick)`) |
| Circle details | `CircleDetailsViewModel` | join (password/public), pending-approval → joining, joined → session, **leave membership** (confirm dialog) |
| Joining circle | `JoiningCircleViewModel` | live approval state via STOMP, auto-advance to session |
| In-session | `InSessionViewModel` | circle session + roster via STOMP, open Mushaf, **leave the circle** (confirm dialog → `POST /api/circles/{id}/leave` → back) |

Contracts live under `presentation/.../circle/state/*Contract.kt`; delegation-based
`StateHolder`/`EffectPublisher` MVI plumbing (rule 3a). No cross-layer leakage — UI consumes
only domain models.

### 2.4 Wiring

- **DI**: `meetingDataModule` (meeting `HttpClient`, `MeetingApi`, `CircleApi`,
  `MeetingRepository`, `CircleRepository`, `StompClient`); `PresentationModule` binds each
  circle ViewModel; `AlMahirApp` wires `MeetingKitConfig`, `MeetingAuthTokenProvider`,
  `MeetingCurrentUserProvider`.
- **Nav** (`app/.../AppNavigation.kt`): `CircleList`, `CircleDetails(circleId)`,
  `JoiningCircle(circleId, membershipId)`, `InSession(circleId)`; Home/SheikhDetails → details,
  details → joining (pending) or session (joined, replaces back entry).
- **i18n**: circle strings added to **both** `values/` and `values-ar/`.
- **Tests**: presentation tests green (73/75; 2 pre-existing auth failures unrelated).
  Helpers: `FakeCircleRepository` (in-memory joins grow `myCircles`), `FakeMeetingRepository`,
  `FakeAppPreferencesRepository`.

### 2.5 Authentication of meeting requests — fixed ✅

`MeetingAuthTokenProvider` now decodes the JWT `exp` claim and refreshes via
`AuthRepository.refreshTokens()` when expired/near-expiry, so the meeting client never sends a
stale token (previously → 403 on every meeting/circle endpoint). Verified in logcat:
`MeetingAuth: access token expired; refresh succeeded`.

---

## 3. Sheikh app — built & handled ⚠️ (partial)

- **Built**: sheikh home (`SheikhHomeScreen` + availability state), auth, profile, settings,
  **1:1 meeting requests** (shared `meetingRequestEntries` → `MeetingRoute.Call`), and the
  **circles admin** (see §6).
- The underlying data layer already covers the admin operations (`CircleRepositoryImpl` has
  `approveJoinRequest`, `rejectJoinRequest`, `startCircle`, `endCircle`, `removeMember`,
  `getMembers`, `getPendingRequests`).

---

## 6. Sheikh: Circles admin UI ✅

Built on `:sheikh:presentation` (MVI, same delegation-based `StateHolder`/`EffectPublisher`
plumbing and `ObserveEffect`), consuming only `:meeting:domain` models and `:designsystem`.

### Screens / flow

| Screen | ViewModel | Flow |
|---|---|---|
| List | `SheikhCircleListViewModel` | my circles (`getMyCircles`), FAB → create, tap card → manage |
| Create | `SheikhCreateCircleViewModel` | name / type (Public+Private) / approval / capacity / password / ISO dates; validation; navigates to manage on success |
| Manage | `SheikhCircleManageViewModel` | circle summary, pending join requests (approve/reject), member roster (remove), start/end/cancel with confirmation; live updates via STOMP (`observePendingRequests`, `observeCircleEvents`) |

### Files

- Contracts: `circle/state/SheikhCircleListContract.kt`, `SheikhCreateCircleContract.kt`,
  `SheikhCircleManageContract.kt`
- ViewModels: `circle/SheikhCircleListViewModel.kt`, `SheikhCreateCircleViewModel.kt`,
  `SheikhCircleManageViewModel.kt`
- Screens: `circle/SheikhCircleListScreen.kt`, `SheikhCreateCircleScreen.kt`,
  `SheikhCircleManageScreen.kt`; card component `circle/components/SheikhCircleCard.kt`
- Nav: `circle/navigation/SheikhCircleNavigation.kt` — `SheikhCircleRoute`
  (`CircleList` / `CreateCircle` / `CircleManage(circleId)`) + `sheikhCircleEntries(...)`

### Wiring

- **DI**: `SheikhPresentationModule` binds the three ViewModels (`SheikhCircleManageViewModel`
  takes a `circleId` parameter).
- **Nav**: `SheikhAppNavigation` registers `sheikhCircleEntries(...)`; the sheikh home screen
  gained a **Manage Circles** entry (`SheikhHomeScreen`/`SheikhHomeContent` `onOpenCircles`).
- **i18n**: all circle strings in both `sheikh/.../values/strings.xml` and `values-ar/strings.xml`.
- **Tests**: `SheikhCircleListViewModelTest`, `SheikhCreateCircleViewModelTest`,
  `SheikhCircleManageViewModelTest` (16 tests, all green) with a sheikh-side
  `FakeCircleRepository` mirroring `CircleRepositoryImpl`'s `kotlin.Result` contract and
  `MutableSharedFlow` event flows.

### Note

Sheikhs list their own circles via `getMyCircles()` (`GET /api/circles/mine`), which currently
500s on production — see issue #1. The admin UI therefore falls back to the network-error
placeholder until the backend is fixed; the rest of the feature is production-wired.

---

## 4. Known issues / blockers

| # | Issue | Status |
|---|---|---|
| 1 | `GET /api/circles/mine` returns **500** `{"success":false,"message":"An unexpected error occurred."}` on production | **Backend bug** — auth now succeeds (403 fixed), so the server itself throws. Needs backend fix. App degrades to an empty section; `HomeViewModel` logs the failure (`Log.w("HomeViewModel", "getMyCircles failed", …)`). Affects both user "Your Circles" and the sheikh circles list. |
| 2 | "Your Circles" section hidden when `myCircles` empty or offline | By design (`HomeContent.kt:144`); empty only shows once the account actually has memberships. |
| 3 | 2 pre-existing presentation test failures (Login/Register auth messages) | Pre-existing, unrelated to circles. |
| 4 | 30 pre-existing lint errors in 4 untouched files (java.time NewApi) | Pre-existing, unrelated. |

---

## 5. Future work

1. **Backend fix** for the `/api/circles/mine` 500 (blocking "Your Circles" / sheikh circle list
   for real data).
2. ~~**User: create private circle**~~ — ✅ **Built** (§2.6 below).
3. ~~**Sheikh: circles admin UI**~~ — ✅ **Built** (§6 above).
4. Optionally: join-request push entry points (in-app banner) and error surfacing for the
   swallowed circle failures.

---

## 2.6 User: Create Private Circle ✅

Added in the user app. Entry point: the `+` FAB on the **Circle List** screen.

### What was built

| Layer | File(s) |
|-------|---------|
| State | `state/CreateCircleContract.kt` — `CreateCircleUiState`, `CreateCircleIntent`, `CreateCircleEffect`, `CreateCirclePrivacyType` |
| ViewModel | `CreateCircleViewModel.kt` — validates inputs, calls `CircleRepository.createCircle()`, shows the created-circle popup (copyable Session ID + password), then navigates on Done |
| Screen | `CreateCircleScreen.kt` — matches the provided design: Arabic/bilingual top bar, role-info banner (Arabic text), title + goals text fields, PRIVACY pill-selector (Private active / Public disabled for non-Sheikh users), access code field (shown only for PRIVATE), **Create & Generate Link** CTA |
| Strings | `values/strings.xml` + `values-ar/strings.xml` — all 9 new strings present in both locales |
| DI | `PresentationModule.kt` — `CreateCircleViewModel` registered with `viewModel { }` |
| Navigation | `AppNavigation.kt` — `AppRoute.CreateCircle` route added; `CircleListScreen` wired with `onOpenCreateCircle` callback; the created-circle popup's **Done** lands the creator on `CircleDetails(circleId)` (not the live session, so they can enter/leave/back out freely) |

### Design fidelity

- **Role banner**: exact Arabic text from the design screenshots, styled with a light primary-tinted background and border.
- **Privacy pills**: PRIVATE pill is selected and active (dark green); PUBLIC pill is visually disabled and un-clickable for non-Sheikh users — matches the left/right toggle in the screenshot.
- **Password field**: only visible when PRIVATE is selected (animated).
- **CTA button**: uses the project's `PrimaryButton` from `:designsystem` with the exact label.

---

## 2.7 User: Join private circles + Current Circle widget ✅

### Join a private circle

- **Entry point**: a **Join Private Circle** button (outlined, lock icon) on the Circle List
  screen under the search bar.
- **Sheet**: `AppBottomSheet` with **circle ID** + **password** fields, inline error text, and a
  loading submit button. The backend lists only PUBLIC circles, so joining a private circle is by
  ID + password (`POST /api/circles/{id}/join`); pending-approval circles confirm the request and
  wait for the host.
- **After joining**: the sheet closes, a toast confirms, and `loadMyCircles()` refreshes so the
  new circle appears in the list. The list is now **public circles + joined circles, deduplicated
  by id** (`buildFilteredList`), so private circles the user joined stay visible (with the
  "Joined" badge) and filter/search still apply.
- **Errors**: `INVALID_PASSWORD` / `CIRCLE_FULL` / `TIME_CONFLICT` / `ALREADY_MEMBER` map to the
  existing circle error strings; `ALREADY_MEMBER` closes the sheet and just toasts.

### Current Circle widget

- New `CurrentCircleCard` (`presentation/.../circle/CurrentCircleCard.kt`) — brand-primary card
  showing the circle name, host, status, date, and member count. Pinned as the **first item on the
  Circle List screen** (above search/filters) whenever the user has a membership, and shown on
  **Home in place of the circles summary** when joined (with a "Qur'an Circles" **See All**
  section header above it). Featured circle = the ONGOING membership, else the first joined circle
  (`CircleListUiState.currentCircle`).
- **Nav**: Home now routes `HomeIntent.CircleClicked → HomeEffect.OpenCircle(circleId)` through
  `onOpenCircle` in `AppNavigation.kt` to open circle details.
- **i18n**: 9 new strings in both `values/` and `values-ar/`.
- **Tests**: 8 new (7 in `CircleListViewModelTest`, 1 in `HomeViewModelTest`); the shared test
  `FakeCircleRepository` gained `privateCircles` and appends to `myCircles` on a successful join.

---

## 2.8 User: Leave Circle ✅

Member can leave a circle from either surface; both confirm first via the shared
`ConfirmationDialog` (`:designsystem`, error-colored confirm) and call the same endpoint.

- **In-session**: the session's **Leave** button now opens a confirmation dialog instead of
  navigating straight back. On confirm, `InSessionViewModel` calls
  `CircleRepository.leaveCircle(circleId)` (`POST /api/circles/{id}/leave`, empty-data envelope);
  success → `NavigateBack`, failure → error toast (`InSessionEffect.ShowMessage`) and the session
  stays open.
- **Circle details**: members (joined, non-pending) get a **Leave Circle** secondary button below
  **Enter Circle**. On success `isMember` flips false (the UI returns to Join) and a toast
  confirms; on failure a toast shows and membership is kept.
- **Domain/data**: `leaveCircle(circleId): Result<Unit>` added to `CircleRepository` and
  implemented in `CircleRepositoryImpl` via the existing `CircleApi.leaveCircle` endpoint.
- **i18n**: 7 new strings (`circle_leave*`) in both `values/` and `values-ar/`.
- **Tests**: 8 new — 4 in `CircleDetailsViewModelTest` (dialog open/dismiss, success flips
  membership, failure keeps membership) and 4 in the new `InSessionViewModelTest` (dialog
  open/dismiss, success navigates back, failure stays in session). Both fakes implement
  `leaveCircle` (`FakeCircleRepository` also drops the circle from `joinedCircles`, so Home/List
  revert to the summary automatically after leaving).

---

## 2.9 Invite Tokens & Circle Editing ✅

The remaining pieces of the Swagger specification were fully integrated into both apps.

### Sheikh app: Editing & Token generation
- **Domain/Data**: Added `UpdateCircleRequest` (PATCH) for modifying `name`, `startDate`, and `endDate` of an existing circle. Added `channelName`, `ownerId`, and `inviteToken` to the domain `Circle` model. Fixed a legacy discrepancy where the backend sends `username` but the DTO expected `displayName` (which caused blank names in the Sheikh UI).
- **Edit Circle**: `SheikhCircleManageScreen` gained an Edit button in the top bar (only shown for `SCHEDULED` circles) that opens a dialog to update the circle. Updates patch the backend and refresh the current view.
- **Token Generation**: Private circles generated by the Sheikh now surface an `inviteToken`. `SheikhCreateCircleScreen` displays this token immediately after creation in a copyable dialog. `SheikhCircleManageScreen` also captures this token upon load and displays it if the circle is private and scheduled.

### User app: Join via Invite Token
- **Domain/Data**: Implemented `CircleApi.joinCircleViaToken` (`POST /api/circles/join/{token}`).
- **Join Sheet**: The `JoinPrivateCircleSheet` on `CircleListScreen` now features a mode-toggle (FilterChips) that allows switching between joining via **ID + Password** and joining **By Invite Link** (Token).
- **Wiring**: Added intent/state fields in `CircleListContract`, handled the API call in `CircleListViewModel` mapping errors correctly (e.g., `ALREADY_MEMBER`), and added the new translations in both English and Arabic locales.

### User app: Create popup & leave round-trip ✅
- **Create popup**: The created-circle dialog shows a copyable **Invite code** row (when the backend returns `inviteToken`) alongside Session ID + password, and can only be dismissed via its **Done** button (no accidental outside-tap dismissal). Done lands the creator on `CircleDetails(circleId)`.
- **Membership detection**: `CircleDetailsViewModel.checkMembership()` now also checks `getMyPrivateCircles()`, so a creator's own private circle shows Enter/Leave (not Join) and the post-leave success popup's Done exits back to the list.
- **List refresh**: `CircleListScreen` accepts a `refreshKey` from the nav host and re-fetches joined circles whenever it becomes the top entry again, so a circle left on the details/session screens disappears from the list (and the Current Circle card) immediately on return.
