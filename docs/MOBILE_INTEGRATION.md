# Mobile integration guide

How to wire an Android or iOS app to the Al-Mahir AI service, starting from a backend
running on your own laptop.

[API.md](API.md) is the protocol contract — every field, every value. This document is
the other half: how to *reach* that service from a phone, how to feed it audio the
platform does not hand you in the right format, and how to prove each layer works before
building the next one on top of it.

Read [SETUP.md](SETUP.md) first if the backend is not running yet.

Contents:

1. [The five-minute version](#1-the-five-minute-version)
2. [Reaching your laptop from a phone](#2-reaching-your-laptop-from-a-phone)
3. [Letting the OS talk to plain HTTP](#3-letting-the-os-talk-to-plain-http)
4. [Step 1: prove the network path](#4-step-1-prove-the-network-path)
5. [Step 2: capability discovery](#5-step-2-capability-discovery)
6. [Step 3: audio capture, the part that goes wrong](#6-step-3-audio-capture-the-part-that-goes-wrong)
7. [Step 4: the WebSocket session](#7-step-4-the-websocket-session)
8. [Step 5: rendering feedback](#8-step-5-rendering-feedback)
9. [Android reference implementation](#9-android-reference-implementation)
10. [iOS reference implementation](#10-ios-reference-implementation)
11. [Flutter and React Native](#11-flutter-and-react-native)
12. [Āyah search](#12-āyah-search)
13. [The test ladder](#13-the-test-ladder)
14. [Troubleshooting](#14-troubleshooting)
15. [Before you ship](#15-before-you-ship)

---

## 1. The five-minute version

The service is one HTTP port, `8100`, carrying both REST and a WebSocket. There is no
authentication and no SDK. Any HTTP and WebSocket client will do.

The whole live-recitation loop is four moves:

```
1. WS connect      ws://<host>:8100/ws/session
2. send JSON       {"type":"start","sura":1,"aya":1,"word_idx":0}
3. send binary     16 kHz mono PCM16-LE frames, continuously, any size
4. receive JSON    one "feedback" event per pause, unprompted
```

Then `{"type":"end"}` and you get `{"type":"done"}`.

Three things cause almost every integration failure, so they are worth stating before
anything else:

- **The audio format is not negotiable.** 16 kHz, mono, signed 16-bit little-endian, raw.
  No WAV header. Not 44.1 kHz. Not float. Section 6.
- **The JSON start message must arrive first.** A binary frame before it kills the socket.
- **`localhost` on a phone means the phone.** Section 2.

---

## 2. Reaching your laptop from a phone

The backend already binds `0.0.0.0`, so it accepts connections from anywhere on the
network. What varies is the address the client must dial.

| Client | Base URL | Notes |
|---|---|---|
| iOS **simulator** | `http://localhost:8100` | Shares the Mac's network stack. Just works. |
| Android **emulator** (AVD) | `http://10.0.2.2:8100` | `10.0.2.2` is the emulator's alias for the host loopback. `localhost` would be the emulator itself. |
| Genymotion | `http://10.0.3.2:8100` | Different alias, same idea. |
| **Physical device**, same Wi-Fi | `http://192.168.1.3:8100` | Your machine's current LAN IP. Needs a firewall rule — below. |

WebSocket URLs are the same host and port with the `ws://` scheme:
`ws://10.0.2.2:8100/ws/session`.

> Your LAN IP was `192.168.1.3` (Wi-Fi) when this was written. It is DHCP-assigned and
> will change. Re-check with `ipconfig`, or reserve it in your router. This machine also
> has several virtual adapters (`192.168.224.2`, `192.168.229.2`, `192.168.56.1`) from
> WSL/VirtualBox — those are **not** reachable from your phone. Use the Wi-Fi one.

### The firewall rule you currently need

Windows Firewall has **no inbound rule for port 8100**. The service is running and bound
correctly, but a physical phone will hang and time out. Add the rule once, from an
**administrator** PowerShell:

```powershell
New-NetFirewallRule -DisplayName "Al-Mahir AI 8100" -Direction Inbound `
  -Protocol TCP -LocalPort 8100 -Action Allow -Profile Private
```

`-Profile Private` limits it to networks you have marked private. If your Wi-Fi is
classified Public, either change the network to Private in Windows settings (preferable)
or add `-Profile Public` — understanding that this opens the port to every device on
whatever network you join next.

Remove it when you are done:

```powershell
Remove-NetFirewallRule -DisplayName "Al-Mahir AI 8100"
```

### Avoiding the firewall entirely

If you would rather not open a port, tunnel over USB. Both are more reliable than Wi-Fi
on a congested network, and neither needs a firewall change.

**Android** — reverse-forward the port over ADB, then the device can use `localhost`:

```bash
adb reverse tcp:8100 tcp:8100
```

**iOS physical device** — there is no ADB equivalent; use the LAN IP, or run a tunnel
such as `ngrok http 8100` (which also gives you HTTPS, sidestepping section 3).

---

## 3. Letting the OS talk to plain HTTP

Both platforms block cleartext HTTP by default. Your local backend has no TLS, so you
must add a **development-only** exception.

### Android

`android:usesCleartextTraffic="true"` works but applies to every host. Scope it instead
with a network security config.

`app/src/main/res/xml/network_security_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">10.0.2.2</domain>
        <domain includeSubdomains="false">localhost</domain>
        <domain includeSubdomains="false">192.168.1.3</domain>
    </domain-config>
</network-security-config>
```

Reference it from `AndroidManifest.xml`, and declare the permissions the app needs:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<application
    android:networkSecurityConfig="@xml/network_security_config"
    ... >
```

Best practice is to put this file in a `debug` source set only, so the release build
never permits cleartext at all.

### iOS

Add to `Info.plist`. `NSAllowsLocalNetworking` covers `localhost` and private LAN ranges
without the blanket `NSAllowsArbitraryLoads`:

```xml
<key>NSAppTransportSecurity</key>
<dict>
    <key>NSAllowsLocalNetworking</key>
    <true/>
</dict>

<key>NSMicrophoneUsageDescription</key>
<string>Used to listen to your recitation and give tajwīd feedback.</string>
```

On iOS 14+, a physical device on the LAN also triggers the **Local Network** privacy
prompt. Add:

```xml
<key>NSLocalNetworkUsageDescription</key>
<string>Connects to the recitation service on your local network.</string>
```

App Store review rejects `NSAllowsArbitraryLoads` without justification. Ship TLS instead
(section 15).

---

## 4. Step 1: prove the network path

Do not write app code yet. Prove reachability from the device itself, or you will spend
an afternoon debugging audio code that never had a connection.

**From your laptop** (proves the server is up):

```powershell
curl http://localhost:8100/health
```

**From the Android emulator**, using the device's own shell — this is the real test,
because it exercises the emulator's network stack:

```bash
adb shell curl -s http://10.0.2.2:8100/health
```

**From a physical device**: open `http://192.168.1.3:8100/docs` in the phone's browser.
The interactive API page should load. If it does, HTTP works and the firewall is open.

A healthy response looks like this — this is a real capture from your machine:

```json
{"status":"healthy","engine":"real","available_engines":["real"],
 "device":"cuda","dtype":"bfloat16",
 "muaalem_model":"obadx/muaalem-model-v3_2",
 "segmenter_model":"obadx/recitation-segmenter-v2",
 "muaalem_device":"cuda","segmenter_device":"cpu"}
```

Only move on once this works.

---

## 5. Step 2: capability discovery

Call `GET /health` at app start and keep the result. It answers two questions you cannot
hardcode:

- **`available_engines`** — which engines this server actually built. Offer only these in
  any engine picker. Requesting one that is absent does **not** error; the server
  silently uses its default, and you find out only from the `engine` field of the session
  ack. Compare them.
- **`engine`** — the server's default, for when you do not care.

Your server currently reports `["real"]` only. `zipformer` is absent because its
(gated) model files are not installed, and `mock` is not built when a GPU is present.
Do not assume a fixed list; a CPU deployment will report something different.

Two more discovery endpoints, both safe to cache for the app's lifetime:

- `GET /moshaf-schema` — 37 fields describing the reciter's tajwīd style, each with a
  `key`, Arabic name, `default`, and its legal `options`. **Build the settings panel from
  this response rather than hardcoding**, because the legal ranges are not uniform:
  `madd_monfasel_len` accepts 2–5 while `madd_mottasel_len` accepts only 4–6. Sending an
  out-of-range value is not an error — the server discards your **entire** moshaf and
  silently uses its default, which looks exactly like the setting being ignored.
- `GET /tajweed-rules` — the 8 tajwīd rules plus 10 ṣifāt a session can be graded on,
  each with `key`, `name_ar`, `name_en`, `kind`. Use these keys for the `rules` leniency
  array.

---

## 6. Step 3: audio capture, the part that goes wrong

The wire format, exactly:

| Property | Value |
|---|---|
| Sample rate | **16000 Hz** |
| Channels | **1 (mono)** |
| Encoding | **PCM signed 16-bit** |
| Byte order | **little-endian** |
| Container | **none** — raw samples, no WAV/RIFF header |
| Frame size | anything; ~100 ms (3200 bytes) is a good default |

Neither platform gives you this by default, and a mismatch does not raise an error — it
produces *plausible but wrong* feedback, because the model hears a valid signal at the
wrong speed. A 44.1 kHz buffer sent as 16 kHz sounds like very slow recitation, and the
model will confidently report madd errors that the reciter never made. **If your first
real session reports errors everywhere, suspect the sample rate before the model.**

Send frames continuously while the mic is open. Do not try to detect pauses yourself —
the server runs silero VAD and decides where chunks end. Sending silence is correct and
expected; it is how the server knows a waqf happened.

### Android

`AudioRecord` gives you exactly the right format natively, which makes Android the easy
side:

```kotlin
val sampleRate = 16000
val minBuf = AudioRecord.getMinBufferSize(
    sampleRate,
    AudioFormat.CHANNEL_IN_MONO,
    AudioFormat.ENCODING_PCM_16BIT,
)
val recorder = AudioRecord(
    MediaRecorder.AudioSource.VOICE_RECOGNITION,  // not MIC: less aggressive processing
    sampleRate,
    AudioFormat.CHANNEL_IN_MONO,
    AudioFormat.ENCODING_PCM_16BIT,
    maxOf(minBuf, 3200 * 4),
)
```

`ENCODING_PCM_16BIT` is little-endian on every Android device, so the bytes are already
wire-ready. Read into a `ByteArray` and send it unmodified — do **not** convert to
`ShortArray` and back, which is where byte-order bugs creep in.

Use `VOICE_RECOGNITION` rather than `MIC`. It applies less aggressive noise suppression
and AGC, both of which distort the sustained vowels that madd grading measures.

### iOS

Harder: `AVAudioEngine` hands you 32-bit float at the hardware rate (usually 44.1 or
48 kHz). You must convert. Use `AVAudioConverter` — do not resample by hand.

```swift
let hwFormat = engine.inputNode.outputFormat(forBus: 0)
let target = AVAudioFormat(
    commonFormat: .pcmFormatInt16,
    sampleRate: 16000,
    channels: 1,
    interleaved: true,
)!
let converter = AVAudioConverter(from: hwFormat, to: target)!
```

Also configure the session category, or iOS may hand you a different rate or refuse the
mic entirely:

```swift
try AVAudioSession.sharedInstance().setCategory(.record, mode: .measurement)
try AVAudioSession.sharedInstance().setActive(true)
```

`.measurement` mode disables most system-level signal processing — the same reasoning as
`VOICE_RECOGNITION` on Android.

### Verifying your capture before involving the server

Write 5 seconds of your captured bytes to a file, pull it off the device, and play it:

```bash
adb pull /sdcard/Android/data/<pkg>/files/capture.raw
ffplay -f s16le -ar 16000 -ch_layout mono capture.raw
```

If it plays at normal speed and pitch, your format is right. If it sounds chipmunked or
slowed, your sample rate is wrong — fix that before writing another line of session code.

---

## 7. Step 4: the WebSocket session

### Lifecycle

```
connect ws://<host>:8100/ws/session
   |
   v
send {"type":"start", ...}          <-- MUST be text JSON, MUST be first
   |
   v
recv {"type":"session","session_id":"...","engine":"real","sample_rate":16000}
   |
   v
send binary frames  ---------------------.
   |                                     |  server pushes, unprompted:
   |<---- recv {"type":"feedback", ...} -'  one per finalized waqf chunk
   |
   |  optionally: send {"type":"seek","sura":2,"aya":255,"word_idx":0}
   v
send {"type":"end"}
   |
   v
recv {"type":"done"}   then the server closes
```

### The start message

Every field is optional, including position:

```json
{"type": "start",
 "sura": 1, "aya": 1, "word_idx": 0,
 "strictness": "normal",
 "engine": "real",
 "moshaf": {"madd_monfasel_len": 4},
 "rules": ["aared_madd", "ghonna"]}
```

**Send `sura`/`aya`/`word_idx` whenever you know them**, which in a muṣḥaf reading view is
always. It seeds the cursor, so each chunk is matched by a cheap windowed search around
where the reciter already is — structurally immune to mutashābihāt.

Omit it only for a deliberate "just start reciting, find me" mode. Then the first chunk is
matched by a fuzzy search over the whole Qur'an, and a passage that genuinely occurs in
several places comes back `status: "ambiguous"` with a candidate list rather than a guess.
The basmalah is exactly this case — it matches both 1:1 and 27:30. Show the candidates and
let the next chunk disambiguate.

`sura` and `aya` are **1-based**; `word_idx` is **0-based** within the āyah. The reference
frontend always sends the position (`frontend/src/lib/session.ts`), and so should you.

### Sending audio

Binary WebSocket frames, contents as section 6. Any frame size. Send continuously.

### Timing expectations, measured on this machine

A Quadro T2000 with the `real` engine:

| | Observed |
|---|---|
| First model load at server start | ~12 s (once, at startup, not per session) |
| 25 s of audio → 2 feedback events | **3.7 s total** |
| First recitation after server start | slower — warm-up on the first chunk |

Feedback arrives **per pause**, not continuously. A reciter who does not pause for 19
seconds hits the hard chunk cap and gets a `forced_cut` chunk — see section 8.

### Failure modes you must handle

| Situation | What happens | What to do |
|---|---|---|
| First frame is not JSON text | Server closes with **1002** | Fix your ordering |
| Requested engine not built | Silently uses the default | Compare ack's `engine` to what you asked |
| Invalid `moshaf` value | Entire moshaf discarded, default used | Build the panel from `/moshaf-schema` |
| Invalid `strictness` (e.g. `"Normal"`) | Falls back to default | Send lowercase |
| Network drop mid-session | Socket closes | Reconnect and `start` at the last cursor |

> **Two known server bugs.** A **binary** first frame, or a first frame that is valid JSON
> but not an object (`[1,2,3]`), crashes the handler and closes with **1006** instead of a
> clean 1002. Both are unhandled exceptions in `src/tajwid/api/ws.py`. This matters for
> mobile: a client that starts streaming audio before its start message lands hits the
> first case exactly. Guarantee ordering client-side — do not rely on a clean close code.

### Reconnection

There is no session resumption. On reconnect, open a new socket and `start` again with
the **last cursor you received** as the position. You lose only the in-flight chunk.

Track the cursor from every `feedback` event; it is the `cursor` field, and it is what a
reconnect should seed from.

---

## 8. Step 5: rendering feedback

A feedback event, trimmed for readability:

```json
{"type": "feedback", "chunk_seq": 0,
 "audio_span_sec": [0.168, 6.0], "forced_cut": false,
 "phonemes": "بِسمِللَااهِررَحمَاانِررَحِۦۦۦۦم",
 "feedback": {
   "status": "ok",
   "span": {"sura": 1, "aya": 1, "word_idx": 0},
   "end":  {"sura": 1, "aya": 1, "word_idx": 3},
   "uthmani_text": "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
   "words": [
     {"sura":1,"aya":1,"word_idx":0,"uthmani":"بِسْمِ",
      "status":"correct","errors":[],"trimmed":false}
   ]
 },
 "cursor": {"sura": 1, "aya": 1, "word_idx": 3}}
```

### The chunk `status` gate

Check `feedback.status` **before** rendering anything:

- **`ok`** — `words[]` is meaningful. Render it.
- **`ambiguous`** — the passage matches several places. `candidates[]` is provided.
  **Assert nothing against the reciter.** No words, no errors.
- **`no_match`** — could not be located. Again, render nothing.

`ambiguous` and `no_match` are the service declining to guess. Scoring someone against a
verse they were not reciting is the worst thing this system can do, so it refuses. Do not
paper over these with a "no errors found" state — show nothing, or a neutral hint.

### The three word statuses

| `status` | Meaning | How to render |
|---|---|---|
| `correct` | No issue | Normal, or a subtle positive |
| `almost` | Model was not confident enough to accuse | **A hint. Never a mistake. Never counted against a score.** |
| `error` | A confident, real mistake | Correction UI; `errors[]` says what and why |

Treating `almost` as an error is the single most damaging client-side bug available here.
Falsely correcting a perfect recitation is exactly what this system is built to avoid —
the server already softened that finding on purpose. Do not undo it.

### `trimmed`

Independent of status. `trimmed: true` means the word sat on a chunk boundary and **was
not scored at all**. Render it neutrally — never as a checkmark, never as an error. It
carries no information about the recitation.

### `forced_cut`

The chunk hit the 19-second hard cap without the reciter pausing. The audio was cut
mid-flow, so words near the boundary are less reliable and are usually `trimmed`. Not an
error condition, but worth knowing when results look unusually poor.

### Adjusting sensitivity

`strictness` (`lenient` / `normal` / `strict`) does **not** change what counts as a
mistake. It sets the confidence threshold at which a finding is reported as a hard
`error` rather than softened to `almost`. **A lower threshold is a harsher teacher.**

A finding with `confidence: null` — a pure deletion, where the reciter said nothing and
there is no probability to read — grades `almost` at every level, `strict` included.

> API.md notes these thresholds are **uncalibrated placeholders** awaiting a labelled
> set. Treat the ordering as meaningful and the absolute numbers as provisional. Do not
> build a user-facing accuracy percentage on them.

`rules` narrows *which tajwīd rules* are enforced — a learner drilling madd al-aared is
not corrected on qalqalah. Hifz and tashkeel are never filtered: a wrong or missing word
is always reported. `[]` means "no tajwīd rule at all, hifz and tashkeel only" and is a
real choice, distinct from omitting the field (which grades everything).

---

## 9. Android reference implementation

OkHttp plus `AudioRecord`. Add to `build.gradle`:

```kotlin
implementation("com.squareup.okhttp3:okhttp:4.12.0")
```

```kotlin
import android.media.*
import okhttp3.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import kotlin.concurrent.thread

class RecitationSession(private val baseWs: String) {

    private var socket: WebSocket? = null
    private var recorder: AudioRecord? = null
    @Volatile private var running = false

    var onFeedback: ((JSONObject) -> Unit)? = null
    var onCursor: ((Int, Int, Int) -> Unit)? = null
    var onEngine: ((String) -> Unit)? = null
    var onClosed: ((Int, String) -> Unit)? = null

    private val client = OkHttpClient.Builder()
        // The server pushes only on a pause; a short read timeout would kill an
        // otherwise-healthy socket during a long ayah. 0 = no timeout.
        .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
        .build()

    fun start(sura: Int, aya: Int, wordIdx: Int = 0, engine: String? = null) {
        val request = Request.Builder().url("$baseWs/ws/session").build()

        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                // The start message MUST be the first frame, and MUST be text.
                val cfg = JSONObject().apply {
                    put("type", "start")
                    put("sura", sura); put("aya", aya); put("word_idx", wordIdx)
                    engine?.let { put("engine", it) }
                }
                ws.send(cfg.toString())
                // Only start the mic once the config is queued, so no binary frame
                // can ever overtake it.
                startCapture(ws)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                val msg = JSONObject(text)
                when (msg.optString("type")) {
                    "session" -> onEngine?.invoke(msg.optString("engine"))
                    "feedback" -> {
                        onFeedback?.invoke(msg)
                        msg.optJSONObject("cursor")?.let {
                            onCursor?.invoke(
                                it.optInt("sura"), it.optInt("aya"), it.optInt("word_idx"),
                            )
                        }
                    }
                    "done" -> ws.close(1000, null)
                }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                stopCapture(); onClosed?.invoke(code, reason)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, r: Response?) {
                stopCapture(); onClosed?.invoke(-1, t.message ?: "failure")
            }
        })
    }

    private fun startCapture(ws: WebSocket) {
        val rate = 16000
        val minBuf = AudioRecord.getMinBufferSize(
            rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
        )
        val rec = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuf, 3200 * 4),
        )
        recorder = rec
        running = true
        rec.startRecording()

        thread(name = "mic-pump") {
            val buf = ByteArray(3200)          // 100 ms at 16 kHz, 16-bit mono
            while (running) {
                val n = rec.read(buf, 0, buf.size)
                if (n > 0) ws.send(buf.copyOf(n).toByteString())
            }
        }
    }

    private fun stopCapture() {
        running = false
        recorder?.runCatching { stop(); release() }
        recorder = null
    }

    fun seek(sura: Int, aya: Int, wordIdx: Int = 0) {
        socket?.send(
            JSONObject().apply {
                put("type", "seek")
                put("sura", sura); put("aya", aya); put("word_idx", wordIdx)
            }.toString(),
        )
    }

    /** Flushes the in-progress utterance; a final `feedback` may still arrive. */
    fun stop() {
        stopCapture()
        socket?.send(JSONObject().put("type", "end").toString())
    }
}
```

Wire it up, remembering that `RECORD_AUDIO` is a runtime permission:

```kotlin
val session = RecitationSession("ws://10.0.2.2:8100")   // emulator
session.onEngine  = { Log.i("tajwid", "engine=$it") }
session.onFeedback = { render(it) }
session.start(sura = 1, aya = 1)
```

Note the ordering in `onOpen`: the mic thread starts only *after* `ws.send(cfg)`. OkHttp
queues frames in order, so the JSON is guaranteed to precede any audio. Getting this
backwards triggers the 1006 bug from section 7.

---

## 10. iOS reference implementation

`URLSessionWebSocketTask` plus `AVAudioEngine`, with `AVAudioConverter` doing the format
change.

```swift
import AVFoundation
import Foundation

final class RecitationSession {
    private var task: URLSessionWebSocketTask?
    private let engine = AVAudioEngine()
    private var converter: AVAudioConverter?

    var onFeedback: (([String: Any]) -> Void)?
    var onEngineName: ((String) -> Void)?

    private let target = AVAudioFormat(
        commonFormat: .pcmFormatInt16, sampleRate: 16000,
        channels: 1, interleaved: true)!

    func start(baseWS: String, sura: Int, aya: Int, wordIdx: Int = 0) throws {
        let url = URL(string: "\(baseWS)/ws/session")!
        // No timeout: the server pushes only on a pause.
        let cfg = URLSessionConfiguration.default
        cfg.timeoutIntervalForRequest = 0
        task = URLSession(configuration: cfg).webSocketTask(with: url)
        task?.resume()

        // Start message first, always.
        let start: [String: Any] = ["type": "start", "sura": sura,
                                    "aya": aya, "word_idx": wordIdx]
        let data = try JSONSerialization.data(withJSONObject: start)
        task?.send(.string(String(data: data, encoding: .utf8)!)) { _ in }

        receiveLoop()
        try startCapture()
    }

    private func receiveLoop() {
        task?.receive { [weak self] result in
            guard let self else { return }
            if case .success(.string(let text)) = result,
               let d = text.data(using: .utf8),
               let msg = try? JSONSerialization.jsonObject(with: d) as? [String: Any] {
                switch msg["type"] as? String {
                case "session": self.onEngineName?(msg["engine"] as? String ?? "")
                case "feedback": self.onFeedback?(msg)
                case "done": self.task?.cancel(with: .goingAway, reason: nil); return
                default: break
                }
            }
            if case .failure = result { return }
            self.receiveLoop()
        }
    }

    private func startCapture() throws {
        let session = AVAudioSession.sharedInstance()
        // .measurement disables system processing that would distort madd length.
        try session.setCategory(.record, mode: .measurement)
        try session.setActive(true)

        let input = engine.inputNode
        let hw = input.outputFormat(forBus: 0)
        converter = AVAudioConverter(from: hw, to: target)

        input.installTap(onBus: 0, bufferSize: 1600, format: hw) { [weak self] buf, _ in
            guard let self, let converter = self.converter else { return }

            let ratio = self.target.sampleRate / hw.sampleRate
            let capacity = AVAudioFrameCount(Double(buf.frameLength) * ratio) + 1
            guard let out = AVAudioPCMBuffer(pcmFormat: self.target,
                                             frameCapacity: capacity) else { return }

            var supplied = false
            var err: NSError?
            converter.convert(to: out, error: &err) { _, status in
                if supplied { status.pointee = .noDataNow; return nil }
                supplied = true
                status.pointee = .haveData
                return buf
            }
            guard err == nil, out.frameLength > 0,
                  let ch = out.int16ChannelData else { return }

            // Int16 is already little-endian on ARM; ship the bytes as-is.
            let bytes = Data(bytes: ch[0], count: Int(out.frameLength) * 2)
            self.task?.send(.data(bytes)) { _ in }
        }

        engine.prepare()
        try engine.start()
    }

    func stop() {
        engine.inputNode.removeTap(onBus: 0)
        engine.stop()
        let end = #"{"type":"end"}"#
        task?.send(.string(end)) { _ in }
    }
}
```

Request the mic before calling `start`:

```swift
AVAudioApplication.requestRecordPermission { granted in /* ... */ }   // iOS 17+
```

The `converter.convert` input block must return the buffer **once** and then signal
`.noDataNow`. Returning it repeatedly duplicates audio; never returning it stalls.

---

## 11. Flutter and React Native

No SDK is needed — both have adequate WebSocket support. The problem in each is the same:
getting raw 16 kHz PCM16.

**Flutter.** `dart:io`'s `WebSocket` handles the transport; `webSocket.add(uint8List)`
sends a binary frame. For capture, `flutter_sound` or `record` can be configured for
16 kHz PCM16 streaming — verify the actual stream format rather than trusting the
configuration, using the playback check from section 6.

**React Native.** The built-in `WebSocket` does **not** reliably support binary frames
across both platforms. Use a native module for audio capture and send the bytes from the
native side, or use `react-native-live-audio-stream` (which yields base64 you must decode
before sending — do not send base64 to the server, it expects raw bytes).

In both cases, the format checklist in section 6 is what to verify first.

---

## 12. Āyah search

`GET /search` — plain REST, no session:

```
GET /search?q=<query>&mode=hybrid&limit=10&hyde=false
```

| Param | Values | Notes |
|---|---|---|
| `q` | any text, Arabic or English | URL-encode it |
| `mode` | `keyword`, `vector`, `hybrid` | Server default is `hybrid` |
| `limit` | integer | |
| `hyde` | `true` / `false` | LLM query expansion |

Response:

```json
{"hits": [{"sura": 1, "aya": 3,
           "text_uthmani": "ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
           "translation": "The Entirely Merciful, the Especially Merciful,",
           "score": 5.76}],
 "matched_lang": "ar", "mode": "keyword", "hyde_used": false}
```

**`mode` in the response is what actually ran, not what you asked for.** An English query
with `mode=hybrid` comes back `"mode":"vector"`, because there is no Arabic lexical bag to
fuse on the English side. That is the response being honest, not a bug.

Measured latencies on this machine:

| Mode | Latency |
|---|---|
| `keyword` | ~20 ms |
| `vector` / `hybrid` | ~100–400 ms |
| **First** `vector`/`hybrid` call after server start | **~15 s** — the embedding model loads once |

That first-call stall is worth handling: either show a spinner, or warm the index at app
start with a throwaway query.

`hyde=true` adds an LLM round-trip (~1 s) and is off by default. `hyde_used` in the
response tells you whether it actually happened — it degrades to `false` and returns
normal results if the key is missing or the provider fails. The generated text is embedded
and discarded; it is never shown to a user, so a bad expansion can only change which real
āyāt come back, never put invented scripture on screen.

HyDE is **enabled and verified working** on this server.

---

## 13. The test ladder

Climb it in order. Each rung isolates one layer, so a failure tells you where the problem
is instead of leaving you guessing.

**Rung 1 — server up (laptop).**
```powershell
curl http://localhost:8100/health
```
Expect `"engine":"real"`. If `"mock"`, CUDA broke.

**Rung 2 — reachable from the device.** `adb shell curl -s http://10.0.2.2:8100/health`,
or open `http://192.168.1.3:8100/docs` in the phone's browser. Section 4.

**Rung 3 — protocol, no audio, no app.** Prove the handshake with
[websocat](https://github.com/vi/websocat):
```
websocat ws://localhost:8100/ws/session
> {"type":"start","sura":1,"aya":1,"word_idx":0}
< {"type":"session","session_id":"...","engine":"real","sample_rate":16000}
> {"type":"end"}
< {"type":"done"}
```

**Rung 4 — audio format, no server.** Capture 5 s in your app, write to a file, play it
back with `ffplay -f s16le -ar 16000 -ch_layout mono capture.raw`. Section 6.

**Rung 5 — the reference frontend.** Open <http://localhost:5173>, tap **ابدأ التلاوة**
and recite. This is a known-good client. If it works and your app does not, the bug is in
your app. If neither works, it is the mic or the server.

**Rung 6 — your app, seeded.** Start a session with an explicit `sura`/`aya`, recite a
familiar āyah, confirm `cursor` advances between events. **A cursor that advances is the
proof that VAD, ASR and muṣḥaf tracking all ran.**

**Rung 7 — the awkward paths.** Deliberate mistakes produce `error`. Silence produces
nothing (correct). Reciting past a page boundary advances the cursor. Backgrounding the
app, losing Wi-Fi, and reconnecting at the last cursor all behave.

### A known-good baseline

The repo ships real recitations at `backend/tests/assets/`. Streaming
`hussary_053001.mp3` (seeded at 53:1) through the live socket on this machine grades
**3/3 words correct, zero findings** — so if your own audio path produces errors
everywhere, the difference is your capture, not the model.

Note that `fatiha_long_track.wav` is a stress fixture for the forced-cut path and grades
poorly by design. Do not use it as a correctness baseline.

---

## 14. Troubleshooting

**Connection refused / timeout from the device.** In order: is the server running
(`curl` on the laptop); is the address right for your client type (section 2); is the
firewall rule present (section 2)? `localhost` from an Android emulator is the single most
common cause.

**Cleartext HTTP blocked.** Android throws `CLEARTEXT communication not permitted`; iOS
fails the connection silently. Section 3.

**Socket closes immediately, code 1002.** Your first frame was not valid JSON text.

**Socket closes immediately, code 1006.** Your first frame was binary — you started
streaming audio before the start message. Section 7's bug note.

**No `feedback` events ever arrive.** Check in order: are you sending raw 16 kHz mono
PCM16-LE (not WAV, not float, not 44.1 kHz)? Did the JSON start message go first? Is the
mic permission actually granted — a denied permission often yields silent buffers rather
than an error. Remember events arrive only **on a pause**; continuous recitation with no
waqf produces nothing until the 19 s cap.

**Errors reported everywhere on good recitation.** Almost always the sample rate. Verify
with the playback check in section 6 before suspecting the model.

**The engine is not what I asked for.** An unbuilt engine falls back silently. Compare the
session ack's `engine` to your request, and check `available_engines`.

**My moshaf setting does nothing.** You sent an out-of-range value, so the server
discarded the whole object. `madd_monfasel_len` is 2–5, `madd_mottasel_len` is 4–6. Build
the panel from `/moshaf-schema`.

**The first search takes 15 seconds.** The embedding model is loading. Once per server
start. Section 12.

**Everything worked yesterday, now it cannot connect.** Your laptop's DHCP lease changed
the LAN IP. Re-check `ipconfig`.

---

## 15. Before you ship

The local setup in this document is a **development** configuration. None of it is safe
to expose:

- **No authentication.** The service has none. Put it behind your Java/Spring backend or
  an API gateway; never expose port 8100 to the internet.
- **CORS is `allow_origins=["*"]`.** Fine inside a VPC, unacceptable publicly.
- **No TLS.** Terminate HTTPS/WSS at the ingress. Then remove the cleartext exceptions
  from section 3 — App Store review rejects a blanket `NSAllowsArbitraryLoads`, and
  shipping cleartext means shipping a microphone stream over plain HTTP.
- **Make the base URL configurable**, not compiled in. A gateway may later add a path
  prefix.
- **Remove the firewall rule** when you are done developing.

### Licensing that follows the app, not just the repo

- The Qur'an text is from the **Tanzil Project under CC BY 3.0**, not MIT. The text may
  not be modified, and **your app must credit the Tanzil Project with a link to
  <https://tanzil.net>**. The web frontend does this in its footer; mobile apps must do
  the equivalent. This binds your app, not merely the backend.
- The muṣḥaf glyphs and page layout are © **King Fahd Glorious Qur'an Printing Complex**.
- If you enable the `zipformer` engine, `Muno459/zipformer_p-quran` is licensed for
  **free-to-end-user applications only**. If the app is monetized, do not ship it.

### One product-level caution

The confidence thresholds behind `almost` versus `error` are documented as **uncalibrated
placeholders**. Do not derive a user-facing accuracy score, streak, or grade from them
without calibrating against a labelled set first. Render `almost` as a hint and you stay
on the right side of the one thing this system must never do: falsely correct a correct
recitation.
