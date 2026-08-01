# No-Internet Connectivity — Android Native Implementation Plan

Goal: Make the Android native app behave correctly and predictably whenever the device has no internet, and recover cleanly when it comes back. This covers three layers:

1. **A connectivity source of truth** (Android `ConnectivityManager`).
2. **App-wide UI reaction** — a top banner ("No internet connection" → "Back online" → hide) and a reusable full-screen "No connection" state.
3. **Per-flow guards** — auth (login/sign-up), mutations (add/delete), and navigation must be blocked while offline with a clear message.

Guiding principles:
- **Pre-empt, don't just catch.** Short-circuit actions and show "No internet connection" *before* firing a network request.
- **Reuse existing seams.** Use standard `Result<T>`, existing toast systems, and a global `MainViewModel` collected at the app root.
- **Offline-first where cached.** Screens backed by Room database should show cached data + banner. Network-only screens show the "No connection" state **only when empty**; otherwise stale content + banner.

---

## Phase 0 — Permissions

`app/src/main/AndroidManifest.xml` — add next to the existing `INTERNET` line:

```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

---

## Phase 1 — Connectivity Contract & Exceptions (Domain Layer)

### 1.1 `domain/connectivity/ConnectivityObserver.kt`

```kotlin
import kotlinx.coroutines.flow.Flow

enum class ConnectivityStatus { Available, Unavailable }

interface ConnectivityObserver {
    /** Emits current status immediately, then on every change. Never completes. */
    val status: Flow<ConnectivityStatus>

    /** Best-effort synchronous read for pre-flight guards (e.g., before a login call). */
    fun currentStatus(): ConnectivityStatus
}
```

### 1.2 `domain/exceptions/NoConnectionException.kt`

```kotlin
class NoConnectionException(
    message: String = "No internet connection"
) : Exception(message)
```

### 1.3 Error Mapping Helper

Normalises raw network exceptions (Retrofit/Ktor/OkHttp) into `NoConnectionException`.

```kotlin
fun Throwable.toAppException(): Throwable {
    val name = this::class.simpleName ?: ""
    val msg = message ?: ""
    val looksOffline = name.contains("UnknownHost", true) ||
        name.contains("ConnectException", true) ||
        name.contains("SocketTimeout", true) ||
        msg.contains("Unable to resolve host", true) ||
        msg.contains("failed to connect", true)
    return if (looksOffline) NoConnectionException() else this
}
```

---

## Phase 2 — Implementation (Data/Framework Layer)

### 2.1 `data/connectivity/AndroidConnectivityObserver.kt`

Uses `ConnectivityManager` + `NetworkCallback`, wrapped in `callbackFlow`. 

```kotlin
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class AndroidConnectivityObserver(context: Context) : ConnectivityObserver {

    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private fun isOnline(): Boolean {
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    override val status: Flow<ConnectivityStatus> = callbackFlow {
        trySend(if (isOnline()) ConnectivityStatus.Available else ConnectivityStatus.Unavailable)

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val ok = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                trySend(if (ok) ConnectivityStatus.Available else ConnectivityStatus.Unavailable)
            }
            override fun onLost(network: Network) {
                trySend(ConnectivityStatus.Unavailable)
            }
            override fun onUnavailable() {
                trySend(ConnectivityStatus.Unavailable)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, callback)

        awaitClose { cm.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    override fun currentStatus(): ConnectivityStatus =
        if (isOnline()) ConnectivityStatus.Available else ConnectivityStatus.Unavailable
}
```

### 2.2 Dependency Injection Setup (Koin)

*Example with Koin:*
```kotlin
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val connectivityModule = module {
    single<ConnectivityObserver> { AndroidConnectivityObserver(androidContext()) }
}
```

---

## Phase 3 — App-wide State (`MainViewModel`)

Drive the banner state and reconnect signal globally.

```kotlin
enum class ConnectivityBanner { Hidden, Offline, BackOnline }

class MainViewModel(
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = connectivityObserver.status
        .map { it == ConnectivityStatus.Available }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val banner: StateFlow<ConnectivityBanner> = flow {
        var wasOffline = false
        connectivityObserver.status.collect { status ->
            when (status) {
                ConnectivityStatus.Unavailable -> { 
                    wasOffline = true; emit(ConnectivityBanner.Offline) 
                }
                ConnectivityStatus.Available -> {
                    if (wasOffline) {
                        emit(ConnectivityBanner.BackOnline)
                        delay(3000)
                        emit(ConnectivityBanner.Hidden)
                        wasOffline = false
                    } else {
                        emit(ConnectivityBanner.Hidden)
                    }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectivityBanner.Hidden)

    val reconnectSignal: SharedFlow<Unit> = connectivityObserver.status
        .map { it == ConnectivityStatus.Available }
        .distinctUntilChanged()
        .drop(1)
        .filter { it }
        .map { }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000))
}
```

---

## Phase 4 — Reusable UI Components (Jetpack Compose)

### 4.1 `OfflineBanner.kt`

```kotlin
@Composable
fun OfflineBanner(
    banner: ConnectivityBanner,
    modifier: Modifier = Modifier,
) {
    val visible = banner != ConnectivityBanner.Hidden
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        val (bg, text, icon) = when (banner) {
            ConnectivityBanner.Offline -> Triple(MaterialTheme.colorScheme.error, "No internet connection", Icons.Default.WifiOff)
            ConnectivityBanner.BackOnline -> Triple(Color(0xFF4CAF50), "Back online", Icons.Default.Wifi)
            ConnectivityBanner.Hidden -> Triple(MaterialTheme.colorScheme.error, "", Icons.Default.WifiOff)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}
```

### 4.2 `NoConnectionState.kt`

```kotlin
@Composable
fun NoConnectionState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.WifiOff, contentDescription = null, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("You're offline", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Check your connection and try again.", color = Color.Gray)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text("Try Again")
            }
        }
    }
}
```

---

## Phase 5 — Global Banner Mount

In your root `MainActivity.kt` or `App.kt` composition:

```kotlin
import org.koin.androidx.compose.koinViewModel

@Composable
fun App(mainViewModel: MainViewModel = koinViewModel()) {
    val banner by mainViewModel.banner.collectAsState()

    Column(Modifier.fillMaxSize()) {
        OfflineBanner(banner)
        Box(Modifier.weight(1f)) { 
            // Your App Navigation / NavHost goes here
            AppNavigation() 
        }
    }
}
```

---

## Phase 6 — Pre-flight Guards (Auth & Mutations)

In your `ViewModels` for specific actions (like Auth, checkout, making posts):

```kotlin
class AuthViewModel(
    private val connectivityObserver: ConnectivityObserver,
    // ...
) : ViewModel() {

    fun login() {
        if (connectivityObserver.currentStatus() == ConnectivityStatus.Unavailable) {
            // Show local state error or emit toast side-effect
            _uiState.update { it.copy(error = "No internet connection") }
            return
        }
        
        // Proceed with network call
    }
}
```

---

## Phase 7 — Offline Data Display (Cached vs Empty)

For network-backed screens (e.g., Home, Feed, Products):

1. **Observe `reconnectSignal`** to automatically fetch fresh data when back online.
2. **Handle empty state fallback**: If the error is network-related AND the screen has no cached data to show, display `NoConnectionState`.

```kotlin
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    mainViewModel: MainViewModel // Pass globally or retrieve via DI
) {
    val state by viewModel.uiState.collectAsState()
    
    // Automatically refresh when internet returns
    LaunchedEffect(Unit) {
        mainViewModel.reconnectSignal.collect {
            viewModel.refresh()
        }
    }

    if (state.isOfflineError && state.items.isEmpty()) {
        NoConnectionState(onRetry = { viewModel.refresh() })
    } else {
        // Show cached content (stale data). The global banner will inform them they are offline.
        HomeContent(items = state.items)
    }
}
```
