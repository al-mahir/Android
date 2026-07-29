# Android Implementation Guide — Circle Feature (Agora + Jetpack Compose)

Companion to `API-Contract.md`. This covers only the Android side of the MVP.

---

## 1. Dependencies

```kotlin
// build.gradle.kts (app)
dependencies {
    // Agora RTC
    implementation("io.agora.rtc:full-sdk:4.3.0")

    // STOMP over WebSocket
    implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")

    // FCM
    implementation("com.google.firebase:firebase-messaging-ktx")

    // DI + lifecycle
    implementation("com.google.dagger:hilt-android:2.51")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
}
```

**Permissions (`AndroidManifest.xml`):**
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```

Request `RECORD_AUDIO` and `CAMERA` at runtime before joining the channel — do this on the "Approved" screen transition, not at app launch.

---

## 2. Layered Architecture

```
UI (Compose)  →  ViewModel  →  Repository (interface)  →  { RestApi, StompRepository, AgoraEngineWrapper }
```

Never let STOMP frame objects or Agora `RtcEngine` callbacks leak into the ViewModel — wrap them in sealed classes / `StateFlow`.

```kotlin
sealed interface CircleUiState {
    data object Idle : CircleUiState
    data object Requesting : CircleUiState
    data object Lobby : CircleUiState              // Pending approval (requireApproval=true only)
    data class Denied(val reason: String) : CircleUiState
    data class InCall(val token: String, val channelName: String, val uid: Int) : CircleUiState
    data class Full(val message: String) : CircleUiState   // 409 CIRCLE_FULL
    data class Error(val message: String) : CircleUiState
}
```

`requestToJoin()` in the ViewModel must branch on the response shape from `POST /circles/{id}/join-requests`, not on a locally-cached `requireApproval` flag (the host could have changed it):

```kotlin
fun requestToJoin() = viewModelScope.launch {
    _uiState.value = CircleUiState.Requesting
    when (val result = circleApi.requestJoin(circleId)) {
        is JoinRequestResult.Joined -> {
            // requireApproval == false: no lobby at all, straight into the call
            _uiState.value = CircleUiState.InCall(result.agoraToken, result.channelName, result.uid)
        }
        is JoinRequestResult.Pending -> {
            _uiState.value = CircleUiState.Lobby
            stompRepository.connectGuestTopic(circleId, myUserId)
        }
        is JoinRequestResult.CircleFull -> _uiState.value = CircleUiState.Full("This circle is full.")
        is JoinRequestResult.Error -> _uiState.value = CircleUiState.Error(result.message)
    }
}
```

---

## 3. REST Layer (Retrofit)

```kotlin
interface CircleApi {
    @POST("circles")
    suspend fun createCircle(@Body req: CreateCircleRequest): CreateCircleResponse

    @POST("circles/{id}/join-requests")
    suspend fun requestJoinRaw(@Path("id") circleId: String): Response<ResponseBody>

    @DELETE("circles/{id}/join-requests")
    suspend fun cancelJoin(@Path("id") circleId: String): Response<Unit>

    @GET("circles/{id}")
    suspend fun getStatus(@Path("id") circleId: String): CircleStatusResponse

    @POST("circles/{id}/leave")
    suspend fun leave(@Path("id") circleId: String): Response<Unit>
}

// Wrap requestJoinRaw() in the repository to parse both possible shapes:
sealed interface JoinRequestResult {
    data class Joined(val agoraToken: String, val channelName: String, val uid: Int) : JoinRequestResult
    data object Pending : JoinRequestResult
    data object CircleFull : JoinRequestResult
    data class Error(val message: String) : JoinRequestResult
}

suspend fun CircleApi.requestJoin(circleId: String): JoinRequestResult {
    val response = requestJoinRaw(circleId)
    return when (response.code()) {
        200 -> moshi.adapter(JoinedBody::class.java).fromJson(response.body()!!.string())!!.let {
            JoinRequestResult.Joined(it.agoraToken, it.channelName, it.uid)
        }
        202 -> JoinRequestResult.Pending
        409 -> JoinRequestResult.CircleFull
        else -> JoinRequestResult.Error("Unexpected response: ${response.code()}")
    }
}
```

`CreateCircleRequest` maps directly to the "Create New Circle" screen fields:

```kotlin
data class CreateCircleRequest(
    val name: String,
    val topicType: String,      // "SURAH" | "JUZ"
    val topicValue: String,     // e.g. "Al-Baqarah" or "5"
    val visibility: String,     // "PUBLIC" | "PRIVATE"
    val participantLimit: Int,  // stepper default: 10
    val requireApproval: Boolean // toggle default: true
)
```

---

## 4. STOMP WebSocket Repository

```kotlin
class StompCircleRepository(private val baseWsUrl: String, private val jwt: String) {

    private var stompClient: StompClient? = null
    private val _events = MutableSharedFlow<CircleSocketEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<CircleSocketEvent> = _events

    fun connectGuestTopic(circleId: String, guestId: String) {
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, "$baseWsUrl/ws")
        stompClient?.withClientHeartbeat(10_000)?.withServerHeartbeat(10_000)

        stompClient?.connect(listOf(StompHeader("Authorization", "Bearer $jwt")))

        stompClient?.topic("/topic/circles/$circleId/guest/$guestId")
            ?.subscribeOn(Schedulers.io())
            ?.subscribe(
                { frame -> _events.tryEmit(parseEvent(frame.payload)) },
                { error -> scheduleReconnect(circleId, guestId) }
            )
    }

    private fun scheduleReconnect(circleId: String, guestId: String) {
        // Exponential backoff: 1s, 2s, 4s, 8s, capped at 30s
        retryWithBackoff { connectGuestTopic(circleId, guestId) }
    }

    fun disconnect() {
        stompClient?.disconnect()
    }
}
```

**Reconnection rule:** after any reconnect, immediately call `GET /circles/{id}` to reconcile — the socket may have missed an `APPROVAL_GRANTED` while the app was backgrounded (iOS/Android both suspend sockets aggressively).

---

## 5. Compose UI

```kotlin
@Composable
fun CircleScreen(viewModel: CircleViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(uiState) {
        if (uiState is CircleUiState.InCall && (!cameraPermission.status.isGranted || !micPermission.status.isGranted)) {
            cameraPermission.launchPermissionRequest()
            micPermission.launchPermissionRequest()
        }
    }

    when (val state = uiState) {
        CircleUiState.Idle -> JoinButton(onClick = viewModel::requestToJoin)
        CircleUiState.Requesting -> LoadingSpinner("Requesting to join...")
        CircleUiState.Lobby -> WaitingRoomUI(onCancel = viewModel::cancelJoin)
        is CircleUiState.Denied -> ErrorView(state.reason)
        is CircleUiState.InCall -> AgoraCallScreen(
            token = state.token,
            channelName = state.channelName,
            uid = state.uid
        )
        is CircleUiState.Full -> ErrorView(state.message)
        is CircleUiState.Error -> ErrorView(state.message)
    }
}
```

---

## 6. Agora Engine Wrapper + Compose Video Rendering

```kotlin
class AgoraEngineWrapper(context: Context, appId: String, private val listener: RtcEngineEventListener) {

    val engine: RtcEngine = RtcEngine.create(context, appId, listener).apply {
        enableVideo()
        setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
    }

    fun joinChannel(token: String, channelName: String, uid: Int) {
        val options = ChannelMediaOptions().apply {
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        }
        engine.joinChannel(token, channelName, uid, options)
    }

    fun leaveChannel() = engine.leaveChannel()
    fun destroy() = RtcEngine.destroy()
}

@Composable
fun AgoraLocalVideo(engine: RtcEngine, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            SurfaceView(context).apply {
                engine.setupLocalVideo(VideoCanvas(this, VideoCanvas.RENDER_MODE_HIDDEN, 0))
                engine.startPreview()
            }
        }
    )
}

@Composable
fun AgoraRemoteVideo(engine: RtcEngine, uid: Int, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            SurfaceView(context).apply {
                setZOrderMediaOverlay(true)
                engine.setupRemoteVideo(VideoCanvas(this, VideoCanvas.RENDER_MODE_HIDDEN, uid))
            }
        }
    )
}
```

---

## 7. Firebase Cloud Messaging (Circle Invites)

```kotlin
class CircleFcmService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"] ?: return
        if (type == "CIRCLE_INVITE") {
            val circleId = message.data["circleId"]!!
            showNotification(
                title = message.notification?.title ?: "Circle invite",
                body = message.notification?.body ?: "",
                deepLink = "myapp://circles/$circleId/join"
            )
        }
    }
}
```

Handle the deep link in your `NavHost` so tapping the notification routes straight to `CircleScreen(circleId)`.

---

## 8. Lifecycle & Background Handling

| Scenario | Handling |
|---|---|
| App backgrounded while in lobby | STOMP socket will be suspended by the OS. On `onResume`, reconnect + `GET` status to reconcile. |
| App backgrounded while `InCall` | Keep audio alive with a foreground service (`FOREGROUND_SERVICE_MICROPHONE` type on API 34+); video naturally pauses. |
| Process death | Persist `circleId` + `role` in `SavedStateHandle` / DataStore so the app can rejoin the lobby flow on relaunch. |
| Network switch (Wi-Fi ↔ 5G) | Agora SDK handles ICE renegotiation internally; your STOMP client needs its own exponential backoff reconnect (see §4). |

---

## 9. Testing Checklist

- [ ] Mock `StompCircleRepository` with a fake 3-second-delay emitter to build/test UI without a live backend.
- [ ] Join a `requireApproval=false` circle — verify it skips `Lobby` entirely and goes straight to `InCall` on the `200 JOINED` response.
- [ ] Join a `requireApproval=true` circle — verify it enters `Lobby` on `202` and waits for the WS `APPROVAL_GRANTED` event.
- [ ] Join a circle already at `participantLimit` — verify the `409` maps to `CircleUiState.Full`, not a generic error.
- [ ] Kill the app while in `Lobby` state, relaunch, verify it reconciles via `GET /circles/{id}`.
- [ ] Double-tap "Join" — verify idempotent 200 vs 202 handling doesn't create duplicate UI states.
- [ ] Revoke camera/mic permission mid-call — verify graceful `Error` state instead of a crash.
- [ ] Airplane-mode toggle during `InCall` — verify Agora reconnect + STOMP reconnect both recover independently.

---

## 10. Feature 2 — Sheikh Availability & On-Demand Circle Requests

This is a separate screen/flow from Feature 1: no scheduled circle, no multi-guest lobby. A student pings **one specific available sheikh**, and the sheikh accepts/declines. Acceptance *is* the approval — both sides drop straight into the Agora call.

### 10.1 Sheikh Side: Availability Toggle

```kotlin
sealed interface SheikhAvailabilityUiState {
    data object Offline : SheikhAvailabilityUiState
    data object Available : SheikhAvailabilityUiState
    data class IncomingRequest(
        val requestId: String,
        val studentName: String,
        val note: String,
        val expiresAt: Instant
    ) : SheikhAvailabilityUiState
    data object Busy : SheikhAvailabilityUiState
}
```

The toggle drives a **heartbeat**, not a single API call:

```kotlin
class AvailabilityHeartbeat(
    private val api: SheikhApi,
    private val scope: CoroutineScope
) {
    private var job: Job? = null

    fun start(sheikhId: String) {
        job = scope.launch {
            while (isActive) {
                api.setAvailability(sheikhId, "AVAILABLE")
                delay(20_000) // must be well under the backend's TTL (e.g. 45s)
            }
        }
    }

    fun stop(sheikhId: String) {
        job?.cancel()
        scope.launch { api.setAvailability(sheikhId, "OFFLINE") }
    }
}
```

Stop the heartbeat in `onPause`/`onStop` of the availability screen — don't rely on the app staying foregrounded; the backend's TTL expiry is your safety net if the process dies without calling `OFFLINE` explicitly.

Subscribe to `/topic/sheikhs/{sheikhId}/requests` whenever `Available`, using the same `StompCircleRepository` pattern from §4, parsing `SHEIKH_MEETING_REQUEST_RECEIVED` / `REQUEST_CANCELLED` events into `SheikhAvailabilityUiState.IncomingRequest` / back to `Available`.

```kotlin
@Composable
fun SheikhAvailabilityScreen(viewModel: SheikhAvailabilityViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    when (val s = state) {
        SheikhAvailabilityUiState.Offline -> AvailabilityToggle(checked = false, onToggle = viewModel::goAvailable)
        SheikhAvailabilityUiState.Available -> AvailabilityToggle(checked = true, onToggle = viewModel::goOffline)
        is SheikhAvailabilityUiState.IncomingRequest -> IncomingRequestCard(
            studentName = s.studentName,
            note = s.note,
            expiresAt = s.expiresAt,
            onAccept = { viewModel.accept(s.requestId) },
            onDecline = { viewModel.decline(s.requestId) }
        )
        SheikhAvailabilityUiState.Busy -> InCallIndicator()
    }
}
```

`IncomingRequestCard` should show a live countdown against `expiresAt` so the sheikh sees the request expiring in real time — don't wait for a server `REQUEST_EXPIRED` push to update the UI; compute it locally and just reconcile if the server event arrives first.

### 10.2 Student Side: Browse & Request

```kotlin
sealed interface RequestUiState {
    data object Idle : RequestUiState
    data object Sending : RequestUiState
    data class Pending(val requestId: String, val expiresAt: Instant) : RequestUiState
    data class Accepted(val circleId: String, val token: String, val channelName: String, val uid: Int) : RequestUiState
    data class Declined(val reason: String?) : RequestUiState
    data object Expired : RequestUiState
}
```

- The sheikh list screen (`GET /api/sheikh?availability=AVAILABLE` — reusing the existing "Get all Sheikhs" endpoint with a new filter param, not a separate endpoint) can optionally subscribe per-sheikh to `/topic/sheikhs/{sheikhId}/status` for live badge updates, but a simple pull-to-refresh is also acceptable for MVP.
- On `Accepted`, navigate directly into the same `AgoraCallScreen` composable used in Feature 1 (§5/§6) — the call UI is shared between both features; only the *entry path* differs.

### 10.3 Push Notifications (Feature 2 additions)

Extend the `CircleFcmService` from §7 with two more `type` values:

```kotlin
when (message.data["type"]) {
    "SHEIKH_MEETING_REQUEST" -> showNotification(deepLink = "myapp://requests/${message.data["requestId"]}")
    "MEETING_REQUEST_ACCEPTED" -> showNotification(deepLink = "myapp://circles/${message.data["circleId"]}/call") // skips lobby UI entirely
    "MEETING_REQUEST_DECLINED" -> showNotification(deepLink = "myapp://requests/${message.data["requestId"]}/result")
}
```

Note `MEETING_REQUEST_ACCEPTED` deep-links to `/call`, not `/join` — there is no waiting room to show; the token is already attached.

### 10.4 Testing Checklist (Feature 2)

- [ ] Kill the sheikh's app while `Available` — verify the student sees the sheikh become unavailable within ~1 TTL window (no manual toggle-off needed).
- [ ] Fire two requests at the same available sheikh back-to-back — verify the second gets `409` and a clear "sheikh is busy" state.
- [ ] Let a request sit until `expiresAt` — verify both sides reconcile to `Expired`/`Available` without any user action.
- [ ] Accept while the student app is backgrounded — verify the push deep-link drops them straight into the call, not into a lobby screen.
