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
| Home | `HomeViewModel` | "Your Circles" section (`myCircles`), `CircleClicked → OpenCircle` |
| Circle list | `CircleListViewModel` | public + my circles, open details |
| Sheikh details | `SheikhDetailsViewModel` | host's public circles (`CircleCard(onClick)`) |
| Circle details | `CircleDetailsViewModel` | join (password/public), pending-approval → joining, joined → session |
| Joining circle | `JoiningCircleViewModel` | live approval state via STOMP, auto-advance to session |
| In-session | `InSessionViewModel` | circle session + roster via STOMP, open Mushaf |

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
- **Tests**: presentation tests green (40/42; 2 pre-existing auth failures unrelated).
  Helpers: `FakeCircleRepository`, `FakeMeetingRepository`, `FakeAppPreferencesRepository`.

### 2.5 Authentication of meeting requests — fixed ✅

`MeetingAuthTokenProvider` now decodes the JWT `exp` claim and refreshes via
`AuthRepository.refreshTokens()` when expired/near-expiry, so the meeting client never sends a
stale token (previously → 403 on every meeting/circle endpoint). Verified in logcat:
`MeetingAuth: access token expired; refresh succeeded`.

---

## 3. Sheikh app — built & handled ⚠️ (partial)

- **Built**: sheikh home (`SheikhHomeScreen` + availability state), auth, profile, settings,
  and **1:1 meeting requests** (shared `meetingRequestEntries` → `MeetingRoute.Call`).
- **Not built**: circles admin — create circles, approve/reject join requests, start/end
  circles, manage members. The `sheikh-app` navigation has **no circle routes/screens**.
- The underlying data layer already covers the admin operations (`CircleRepositoryImpl` has
  `approveJoinRequest`, `rejectJoinRequest`, `startCircle`, `endCircle`, `removeMember`,
  `getMembers`, `getPendingRequests`), so building the sheikh UI is pure presentation + wiring.

---

## 4. Known issues / blockers

| # | Issue | Status |
|---|---|---|
| 1 | `GET /api/circles/mine` returns **500** `{"success":false,"message":"An unexpected error occurred."}` on production | **Backend bug** — auth now succeeds (403 fixed), so the server itself throws. Needs backend fix. App degrades to an empty section; `HomeViewModel` logs the failure (`Log.w("HomeViewModel", "getMyCircles failed", …)`). |
| 2 | "Your Circles" section hidden when `myCircles` empty or offline | By design (`HomeContent.kt:144`); empty only shows once the account actually has memberships. |
| 3 | 2 pre-existing presentation test failures (Login/Register auth messages) | Pre-existing, unrelated to circles. |
| 4 | 30 pre-existing lint errors in 4 untouched files (java.time NewApi) | Pre-existing, unrelated. |

---

## 5. Future work

1. **Backend fix** for the `/api/circles/mine` 500 (blocking "Your Circles" for real data).
2. ~~**User: create private circle**~~ — ✅ **Built** (§2 updated below).
3. **Sheikh: circles admin UI** — create, approve/reject, start/end, member management on the
   sheikh app (data layer ready, §3).
4. Optionally: join-request push entry points (in-app banner) and error surfacing for the
   swallowed circle failures.

---

## 2.6 User: Create Private Circle ✅

Added in the user app. Entry point: the `+` FAB on the **Circle List** screen.

### What was built

| Layer | File(s) |
|-------|---------|
| State | `state/CreateCircleContract.kt` — `CreateCircleUiState`, `CreateCircleIntent`, `CreateCircleEffect`, `CreateCirclePrivacyType` |
| ViewModel | `CreateCircleViewModel.kt` — validates inputs, calls `CircleRepository.createCircle()`, navigates to session on success |
| Screen | `CreateCircleScreen.kt` — matches the provided design: Arabic/bilingual top bar, role-info banner (Arabic text), title + goals text fields, PRIVACY pill-selector (Private active / Public disabled for non-Sheikh users), access code field (shown only for PRIVATE), **Create & Generate Link** CTA |
| Strings | `values/strings.xml` + `values-ar/strings.xml` — all 9 new strings present in both locales |
| DI | `PresentationModule.kt` — `CreateCircleViewModel` registered with `viewModel { }` |
| Navigation | `AppNavigation.kt` — `AppRoute.CreateCircle` route added; `CircleListScreen` wired with `onOpenCreateCircle` callback; on creation success, navigates to `InSession(circleId)` |

### Design fidelity

- **Role banner**: exact Arabic text from the design screenshots, styled with a light primary-tinted background and border.
- **Privacy pills**: PRIVATE pill is selected and active (dark green); PUBLIC pill is visually disabled and un-clickable for non-Sheikh users — matches the left/right toggle in the screenshot.
- **Password field**: only visible when PRIVATE is selected (animated).
- **CTA button**: uses the project's `PrimaryButton` from `:designsystem` with the exact label.

