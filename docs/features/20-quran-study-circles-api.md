# Qur'an Study Circles API

Qur'an study circles (group meetings) — PUBLIC or PRIVATE, hosted by a Sheikh (PUBLIC) or by any user (PRIVATE). Supports optional approval flow, participant capacity limits, and password-gated private access.

Connect to STOMP at `/ws` BEFORE calling join/start/approve/etc.

## WebSockets (STOMP)

### TOPICS:

* **`/topic/circles/{circleId}/requests`** — Circle owner subscribes to keep their pending join-requests list live.
    * `CIRCLE_JOIN_REQUEST_RECEIVED`: new join request came in. Payload: `PendingJoinRequestResponse` object.
    * `CIRCLE_JOIN_REQUEST_REMOVED`: a request left the pending list (approved/rejected). Payload: raw `membershipId` (UUID).
* **`/topic/circle-memberships/{membershipId}`** — The requesting user subscribes to track their own join request (`membershipId` is returned in the `joinCircle` response).
    * `REQUEST_APPROVED`: payload is `CircleMemberResponse`.
    * `REQUEST_REJECTED`: payload is a free-text reason string.
* **`/topic/circles/{circleId}`** — All active members (and the owner) subscribe to stay in sync with the circle's lifecycle and roster.
    * `MEMBER_JOINED`: someone joined (auto-approved or just got approved). Payload: `CircleMemberResponse`.
    * `MEMBER_LEFT`: a member left voluntarily. Payload: raw `userId` (UUID).
    * `MEMBER_REMOVED`: the owner removed a member. Payload: raw `userId` (UUID).
    * `CIRCLE_STARTED`: owner started the circle (SCHEDULED -> ONGOING). Payload: raw `circleId` (UUID). Members can now request an Agora token.
    * `CIRCLE_ENDED`: owner ended the circle (ONGOING -> COMPLETED). Payload: raw `circleId` (UUID).
    * `CIRCLE_CANCELLED`: owner cancelled the circle (SCHEDULED -> CANCELLED). Payload: raw `circleId` (UUID).

**Envelope for every message:** `{ "eventType": "...", "payload": {...} }`

### LIFECYCLE
* `SCHEDULED -> ONGOING -> COMPLETED` (via start/end)
* `SCHEDULED -> CANCELLED` (via cancel)

A circle can only be cancelled while `SCHEDULED`, and only ended while `ONGOING`. Agora tokens are only issued while the circle is `ONGOING`.

* **PRIVATE circles:** not returned by the list endpoint, require a password to join, and require active membership (or ownership) to view the member list.

---

## REST API

### GET `/api/circles`
**List all Circles**

Retrieve a paginated list of PUBLIC circles, optionally filtered by status.

**Parameters**
* `status` (query, string): Filter circles by status (e.g., SCHEDULED, ONGOING, COMPLETED, CANCELLED)
* `page` (query, integer): Zero-based page index (0..N). Default: 0
* `size` (query, integer): The size of the page to be returned. Default: 20
* `sort` (query, array[string]): Sorting criteria in the format: property,(asc|desc). Default: startDate,ASC

**Responses**
* `200 OK`: Returns paginated circle objects.

---

### POST `/api/circles`
**Create a new Circle**

PUBLIC circles: Sheikh only. PRIVATE circles: any authenticated user.

**Request body**
```json
{
  "name": "string",
  "startDate": "2026-08-03T17:17:20.942Z",
  "endDate": "2026-08-03T17:17:20.942Z",
  "type": "PUBLIC",
  "requiresApproval": true,
  "maxParticipants": 1073741824,
  "password": "string"
}
```

**Responses**
* `200 OK`: Returns the created circle object.

---

### POST `/api/circles/{circleId}/start`
**Start Circle**

Transitions a SCHEDULED circle to ONGOING. Restricted to the owning user. Members can only get an Agora token once the circle is ONGOING.

**Parameters**
* `circleId` (path, UUID): UUID of the circle

**Responses**
* `200 OK`: Returns updated circle object.

---

### POST `/api/circles/{circleId}/reject/{userId}`
**Reject Join Request**

Allows the owning user to reject a pending join request.

**Parameters**
* `circleId` (path, UUID): UUID of the circle
* `userId` (path, UUID): UUID of the requesting user

**Responses**
* `200 OK`

---

### POST `/api/circles/{circleId}/leave`
**Leave Circle**

Allows a user to leave a circle they are currently active in.

**Parameters**
* `circleId` (path, UUID): UUID of the circle

**Responses**
* `200 OK`

---

### POST `/api/circles/{circleId}/join`
**Request to Join a Circle**

Submits a join request. For PRIVATE circles, a valid password must be provided. Fails if the user has a time overlap with an existing active circle.

**Parameters**
* `circleId` (path, UUID): UUID of the circle

**Request body**
```json
{
  "password": "string"
}
```

**Responses**
* `200 OK`: Returns the membership object containing `membershipId`.

---

### POST `/api/circles/{circleId}/end`
**End Circle**

Ends an ONGOING circle and updates its status to COMPLETED. Restricted to the owning user.

**Parameters**
* `circleId` (path, UUID): UUID of the circle

**Responses**
* `200 OK`: Returns status and endedAt.

---

### POST `/api/circles/{circleId}/approve/{userId}`
**Approve Join Request**

Allows the owning user to approve a pending join request.

**Parameters**
* `circleId` (path, UUID): UUID of the circle
* `userId` (path, UUID): UUID of the requesting user

**Responses**
* `200 OK`: Returns the approved member object.

---

### GET `/api/circles/{circleId}`
**Get Circle details by ID**

Retrieve detailed information for a specific circle (public or private, if you have the ID).

**Parameters**
* `circleId` (path, UUID): UUID of the circle

**Responses**
* `200 OK`: Returns circle details.

---

### DELETE `/api/circles/{circleId}`
**Cancel Circle**

Cancels a SCHEDULED circle (not yet started). Restricted to the owning user.

**Parameters**
* `circleId` (path, UUID): UUID of the circle

**Responses**
* `200 OK`

---

### PATCH `/api/circles/{circleId}`
**Update Circle**

Allows the owning user to update circle details (name, dates).

**Parameters**
* `circleId` (path, UUID): UUID of the circle

**Request body**
```json
{
  "name": "string",
  "startDate": "2026-08-03T17:18:37.737Z",
  "endDate": "2026-08-03T17:18:37.737Z"
}
```

**Responses**
* `200 OK`: Returns updated circle details.

---

### GET `/api/circles/{circleId}/token`
**Get an Agora token to join a Circle's audio/video channel**

Owner or an ACTIVE member only. Circle must be ONGOING.

**Parameters**
* `circleId` (path, UUID): UUID of the circle

**Responses**
* `200 OK`: Returns the Agora token, channel name, and user account.

---

### GET `/api/circles/{circleId}/pending-requests`
**Get Pending Join Requests**

Allows the owning user to view pending join requests for their circle.

**Parameters**
* `circleId` (path, UUID): UUID of the circle
* `page` (query, integer): Zero-based page index (0..N). Default: 0
* `size` (query, integer): The size of the page to be returned. Default: 20
* `sort` (query, array[string]): Sorting criteria. Default: joinedAt,ASC

**Responses**
* `200 OK`: Returns paginated pending join requests.

---

### GET `/api/circles/{circleId}/members`
**Get Circle Active Members**

Lists all active members in a circle. Private circles require active membership.

**Parameters**
* `circleId` (path, UUID): UUID of the circle
* `page` (query, integer): Zero-based page index (0..N). Default: 0
* `size` (query, integer): The size of the page to be returned. Default: 20
* `sort` (query, array[string]): Sorting criteria. Default: joinedAt,ASC

**Responses**
* `200 OK`: Returns paginated active members.

---

### GET `/api/circles/mine`
**Get My Active Circles**

Retrieve a paginated list of active circles the logged-in user is a member of.

**Parameters**
* `page` (query, integer): Zero-based page index (0..N). Default: 0
* `size` (query, integer): The size of the page to be returned. Default: 20
* `sort` (query, array[string]): Sorting criteria. Default: startDate,ASC

**Responses**
* `200 OK`: Returns paginated list of circles.

---

### DELETE `/api/circles/{circleId}/members/{userId}`
**Remove Member**

Allows the owning user to remove a member from the circle.

**Parameters**
* `circleId` (path, UUID): UUID of the circle
* `userId` (path, UUID): UUID of the user to remove

**Responses**
* `200 OK`
