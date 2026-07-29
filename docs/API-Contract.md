# Circle & Sheikh Meeting Contract (MVP: Agora)

Backend: Java Spring Boot + Redis (Ktor test backend spec in `Ktor-Backend-Agent-Spec.md`)
Media: Agora RTC
Mobile: Android (Jetpack Compose) / iOS (SwiftUI)

Naming convention used throughout this app:
- A **Circle** = a group recitation/study session (what was generically called a "meeting" before). Matches the "Create New Circle" screen.
- A **Sheikh** = the teacher role. Matches the already-live `Sheikh Management` API (`/api/sheikh/...`).
- A **Meeting Request** = a student pinging one specific available Sheikh for a 1:1 session.

---

## 0. Protocol Decision Matrix

| Channel | Used for | Why |
|---|---|---|
| **REST (HTTP)** | Commands / state mutations needing a definitive success/failure response | Create circle, join, approve, Sheikh availability, meeting requests |
| **WebSocket (STOMP)** | Real-time UI sync while the screen is open | Waiting-room list, live participant count, token delivery |
| **Push (FCM / APNs)** | Waking a backgrounded/killed app | Circle invites, Sheikh meeting requests, meeting-starting reminders |

All REST/WS connections require `Authorization: Bearer <JWT>`.

---

# Feature 1 — Circles (Group Sessions)

## 1. Create Circle Screen -> API Mapping

Maps directly to the "Create New Circle" screen:

| Screen field | API field | Notes |
|---|---|---|
| Circle Name | `name` | free text |
| Topic / Surah | `topicType` + `topicValue` | `topicType`: `SURAH` \| `JUZ`; `topicValue`: e.g. `"Al-Baqarah"` or `"5"` |
| Visibility | `visibility` | `PUBLIC` \| `PRIVATE`. `PUBLIC` circles appear in the active-circles search; `PRIVATE` circles are join-by-link/invite only. |
| Participant Limit | `participantLimit` | stepper, default `10` |
| Require Approval | `requireApproval` | boolean toggle. **This is the key branch point below.** |

"Start Circle Now" implies immediate start -- there is no scheduling step in this screen, so circles go live as soon as they're created. (If you later add scheduling, add an optional `scheduledAt` field; omit it for MVP.)

## 2. Create Circle

`POST /api/v1/circles`

**Request**
```json
{
  "name": "Daily Fajr Recitation",
  "topicType": "SURAH",
  "topicValue": "Al-Baqarah",
  "visibility": "PUBLIC",
  "participantLimit": 10,
  "requireApproval": true
}
```

**Response `201 Created`**
```json
{
  "circleId": "c-8f72c4",
  "channelName": "circle_8f72c4",
  "hostAgoraToken": "007eJxTY...",
  "uid": 101,
  "status": "ACTIVE",
  "name": "Daily Fajr Recitation",
  "topicType": "SURAH",
  "topicValue": "Al-Baqarah",
  "visibility": "PUBLIC",
  "participantLimit": 10,
  "requireApproval": true,
  "createdAt": "2026-07-27T14:58:00Z"
}
```

## 3. Get Circle Status

`GET /api/v1/circles/{circleId}`

**Response `200 OK`**
```json
{
  "circleId": "c-8f72c4",
  "name": "Daily Fajr Recitation",
  "status": "ACTIVE",
  "hostId": "user-101",
  "visibility": "PUBLIC",
  "requireApproval": true,
  "participantLimit": 10,
  "waitingCount": 2,
  "activeParticipants": 4
}
```

## 4. Search Active Public Circles

`GET /api/v1/circles?visibility=PUBLIC&search=fajr`

**Response `200 OK`**
```json
{
  "circles": [
    {
      "circleId": "c-8f72c4",
      "name": "Daily Fajr Recitation",
      "topicType": "SURAH",
      "topicValue": "Al-Baqarah",
      "activeParticipants": 4,
      "participantLimit": 10,
      "requireApproval": true
    }
  ]
}
```

## 5. Request to Join -- the `requireApproval` Branch

`POST /api/v1/circles/{circleId}/join-requests`

This single endpoint behaves **differently** depending on the circle's `requireApproval` flag -- client-side, treat the response shape as the source of truth, not something pre-computed from settings fetched earlier (the host could have changed them).

### 5a. `requireApproval = false` (open circle)

Joining is immediate -- no waiting room at all.

**Response `200 OK`**
```json
{
  "status": "JOINED",
  "agoraToken": "007eJxTY...",
  "channelName": "circle_8f72c4",
  "uid": 404
}
```

**Errors**
- `409 CIRCLE_FULL` -- `activeParticipants >= participantLimit`

Client goes straight to the Agora call screen -- same as Feature 2's "accepted" path, no lobby UI needed.

### 5b. `requireApproval = true` (approval required)

**Response `202 Accepted`**
```json
{ "status": "PENDING_APPROVAL", "message": "Waiting for host to admit you." }
```

Client must subscribe to its WebSocket guest topic (SS7) immediately after receiving this. Host then approves/rejects as below.

---

## 6. Approval Flow (only relevant when `requireApproval = true`)

### 6.1 Cancel Own Join Request (Guest)

`DELETE /api/v1/circles/{circleId}/join-requests` -> `204 No Content`

### 6.2 Approve a Guest (Host)

`POST /api/v1/circles/{circleId}/approve`
```json
{ "guestId": "user-404" }
```
**Response `200 OK`**: `{ "status": "APPROVED", "guestId": "user-404" }`

**Errors**: `409 CIRCLE_FULL` if `activeParticipants` has hit `participantLimit` since the guest started waiting -- the host UI should surface this instead of silently failing.

The Agora token is **not** in this response -- it's pushed over the guest's WS topic (SS7), same as before.

### 6.3 Reject a Guest (Host)

`POST /api/v1/circles/{circleId}/reject`
```json
{ "guestId": "user-404", "reason": "Circle is full" }
```
**Response `200 OK`**: `{ "status": "REJECTED", "guestId": "user-404" }`

---

## 7. WebSocket Contract (Circles)

### Host Topic -- `/topic/circles/{circleId}/host`

- **`GUEST_WAITING`**: `{ "eventType": "GUEST_WAITING", "payload": { "guestId", "name", "avatarUrl" } }` (only fires when `requireApproval=true`)
- **`GUEST_LEFT`**: `{ "eventType": "GUEST_LEFT", "payload": { "guestId" } }`
- **`PARTICIPANT_JOINED`**: `{ "eventType": "PARTICIPANT_JOINED", "payload": { "guestId", "uid" } }` (fires for *both* open and approval-required circles, once media actually connects)
- **`PARTICIPANT_LEFT`**: `{ "eventType": "PARTICIPANT_LEFT", "payload": { "guestId" } }`

### Guest Topic -- `/topic/circles/{circleId}/guest/{guestId}`

- **`APPROVAL_GRANTED`**: `{ "eventType": "APPROVAL_GRANTED", "payload": { "agoraToken", "channelName", "uid" } }`
- **`APPROVAL_DENIED`**: `{ "eventType": "APPROVAL_DENIED", "payload": { "reason" } }`
- **`CIRCLE_ENDED`**: `{ "eventType": "CIRCLE_ENDED", "payload": { "endedAt" } }`

### Reconnection Rule

On every reconnect: re-subscribe, then call `GET /api/v1/circles/{circleId}` to reconcile -- the socket may have missed events while suspended.

---

## 8. Other Circle Endpoints

| Method | Path | Notes |
|---|---|---|
| GET | `/api/v1/circles/{circleId}/participants` | list active participants |
| POST | `/api/v1/circles/{circleId}/leave` | any participant leaves |
| POST | `/api/v1/circles/{circleId}/end` | host-only, fires `CIRCLE_ENDED` |

---

## 9. Push Notifications (Circles)

**`CIRCLE_INVITE`**
```json
{
  "data": { "type": "CIRCLE_INVITE", "circleId": "c-8f72c4", "hostName": "Yassen", "actionUrl": "myapp://circles/c-8f72c4/join" },
  "notification": { "title": "Yassen invited you to a Circle", "body": "Daily Fajr Recitation" }
}
```

**`CIRCLE_ENDED`**
```json
{ "data": { "type": "CIRCLE_ENDED", "circleId": "c-8f72c4" } }
```

---

## 10. Redis Schema (Circles)

| Key | Type | Notes |
|---|---|---|
| `circle:{id}:status` | String | `ACTIVE` / `ENDED` |
| `circle:{id}:meta` | Hash | `name, topicType, topicValue, visibility, participantLimit, requireApproval, hostId` |
| `circle:{id}:waiting` | Set | only used when `requireApproval=true` |
| `circle:{id}:participants` | Set | enforce `participantLimit` here for **both** approval modes |
| `circle:{id}:token:{uid}` | String, TTL | cached Agora token |

**Invariant**: `participantLimit` must be checked against `SCARD circle:{id}:participants` at three points -- direct join (5a), approval (6.2), and reconnection -- never trust a client-side count.

## 11. State Diagrams

`circle-architecture.mermaid`, `meeting-approval-sequence.mermaid`, `guest-state-machine.mermaid`.

---

# Feature 2 -- Sheikh Availability & On-Demand Meeting Requests

A student pings **one specific available Sheikh**; the Sheikh accepts or declines. Acceptance *is* the approval -- both sides join the Agora call immediately, no lobby.

## 12. Existing Sheikh Management Endpoints (already implemented -- reuse, don't duplicate)

These already exist in production and should be used as-is by mobile for profile data:

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/sheikh/{id}` | Get Sheikh by ID |
| PUT | `/api/sheikh/{id}` | Update Sheikh profile |
| GET | `/api/sheikh` | Get all Sheikhs |
| GET | `/api/sheikh/username/{username}` | Get Sheikh by username |
| GET | `/api/sheikh/search` | Search Sheikhs |
| GET | `/api/sheikh/email/{email}` | Get Sheikh by email |

> **Action item for the backend team**: extend `GET /api/sheikh` and `GET /api/sheikh/search` with an optional `?availability=AVAILABLE` query filter, rather than standing up a separate "list available sheikhs" endpoint. This keeps one canonical Sheikh-listing surface instead of two.

## 13. New Endpoints Needed (not yet built)

### 13.1 Set Availability (Sheikh) -- NEW

`PUT /api/sheikh/{id}/availability`
```json
{ "status": "AVAILABLE" }
```
(`AVAILABLE` | `BUSY` | `OFFLINE`)

**Response `200 OK`**
```json
{ "sheikhId": "sh-77", "status": "AVAILABLE", "updatedAt": "2026-07-27T15:00:00Z" }
```

Treat as a **heartbeat** (resend every ~20s while the toggle is on); backend stores it in Redis with a TTL (~45s) so a killed app auto-reverts to `OFFLINE`.

### 13.2 Get Sheikh Availability -- NEW

`GET /api/sheikh/{id}/availability` -> same shape as above.

### 13.3 Send Meeting Request (Student) -- NEW

`POST /api/sheikh/{id}/meeting-requests`
```json
{ "note": "I'd like guidance on Surah Al-Mulk tajweed" }
```

**Response `202 Accepted`**
```json
{ "requestId": "req-9f21", "status": "PENDING", "expiresAt": "2026-07-27T15:10:30Z" }
```

**Errors**: `404` Sheikh not found; `409 SHEIKH_UNAVAILABLE` -- not `AVAILABLE`, or already has a pending/active request.

### 13.4 Cancel Meeting Request (Student) -- NEW

`DELETE /api/meeting-requests/{requestId}` -> `204 No Content`

### 13.5 Accept Meeting Request (Sheikh) -- NEW

`POST /api/meeting-requests/{requestId}/accept`

**Response `200 OK`**
```json
{
  "status": "ACCEPTED",
  "circleId": "c-3c91",
  "channelName": "circle_3c91",
  "sheikhAgoraToken": "007eJxTY...",
  "uid": 501
}
```

> An accepted 1:1 meeting request is represented internally as a two-participant **Circle** with `requireApproval=false` and `participantLimit=2` -- one underlying media/session model instead of two. The `circleId` here can be treated by mobile exactly like any other circle once inside the call screen.

Backend simultaneously: sets `sheikh:{id}:status = BUSY`, generates the student's token, pushes `REQUEST_ACCEPTED` over the student's WS topic.

### 13.6 Decline Meeting Request (Sheikh) -- NEW

`POST /api/meeting-requests/{requestId}/decline`
```json
{ "reason": "In another session" }
```
**Response `200 OK`**: `{ "status": "DECLINED" }`

---

## 14. WebSocket Contract (Sheikh Requests)

- **Sheikh topic** `/topic/sheikhs/{sheikhId}/requests`
  - `SHEIKH_MEETING_REQUEST_RECEIVED`: `{ requestId, studentId, studentName, note, expiresAt }`
  - `REQUEST_CANCELLED`: `{ requestId }`
- **Student topic** `/topic/students/{studentId}/meeting-requests/{requestId}`
  - `REQUEST_ACCEPTED`: `{ circleId, channelName, agoraToken, uid }`
  - `REQUEST_DECLINED`: `{ reason }`
  - `REQUEST_EXPIRED`: `{}`
- **Public status topic** `/topic/sheikhs/{sheikhId}/status`
  - `SHEIKH_STATUS_CHANGED`: `{ status, updatedAt }`

## 15. Push Notifications (Sheikh Requests)

**`SHEIKH_MEETING_REQUEST`** (to Sheikh, backgrounded)
```json
{ "data": { "type": "SHEIKH_MEETING_REQUEST", "requestId": "req-9f21", "studentName": "Nour", "actionUrl": "myapp://requests/req-9f21" } }
```

**`MEETING_REQUEST_ACCEPTED`** (to student) -- deep-links straight into the call, not a lobby:
```json
{ "data": { "type": "MEETING_REQUEST_ACCEPTED", "circleId": "c-3c91", "actionUrl": "myapp://circles/c-3c91/call" } }
```

**`MEETING_REQUEST_DECLINED`**
```json
{ "data": { "type": "MEETING_REQUEST_DECLINED", "requestId": "req-9f21" } }
```

## 16. Redis Schema (Sheikh Requests)

| Key | Type | Notes |
|---|---|---|
| `sheikh:{id}:status` | String, TTL ~ 45s | heartbeat-driven |
| `sheikh:{id}:activeRequest` | String = requestId | cleared on resolve |
| `meetingRequest:{requestId}` | Hash | `studentId, sheikhId, status, note, createdAt, expiresAt`, TTL = time to expiry |

Expiry enforced via Redis keyspace notifications or a scheduled sweep -> sets `EXPIRED`, clears `activeRequest`, fires `REQUEST_EXPIRED`.

## 17. State Diagrams

`sheikh-status-state-machine.mermaid`, `meeting-request-lifecycle.mermaid`, `sheikh-availability-request-sequence.mermaid`.
