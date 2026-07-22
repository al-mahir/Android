# Ta'ahud — Live AI Correction: implementation plan

> Build plan for [06-taahud-live-correction.md](06-taahud-live-correction.md) against the AI
> team's shipped contract in [../API.md](../API.md). Rules from [AGENTS.md](../../AGENTS.md)
> apply throughout — this file adds only the *sequence* and the *non-obvious constraints*.

## Context

The Mushaf reader already has a `RECITATION` mode with a mic button that is a stub: it flips
`isRecordingActive` and starts `SimulatedHighlightDriver` (a fake timer). The AI team has
delivered a live-recitation service — WebSocket `/ws/session`, raw PCM16 in, per-word
`correct` / `almost` / `error` feedback out.

The goal of this phase: replace the stub with a real capture → VAD → WebSocket → per-word
highlight pipeline, built one verifiable step at a time. Each step below has a **Gate**; do
not start the next step until the current gate passes.

---

## What already exists (reuse, don't rebuild)

| Thing | Where |
|---|---|
| `RECITATION` mode, mic button, `isRecordingActive` | `MushafMode.kt`, `components/MushafBottomBar.kt`, `MushafViewModel.toggleRecording()` |
| Per-word highlight rendering on the page | `components/MushafPageView.kt` — `PageToken.wordId`, `highlightedWordId` |
| Word identity | `MushafWord.id`, produced by `MushafMapper` from the DB `word_key` |
| Ktor client + Koin wiring for `:mushaf:data` | `data/di/MushafDataModule.kt` |
| MVI plumbing | `presentation/core/mvi/` (`StateHolder`, `EffectPublisher`, `ObserveEffect`) |

### The single most useful fact: word IDs already match the API

The bundled layout DB (`mushaf_v4_layout.db`, QUL layout #19) keys every word as
`"sura:aya:wordIndex1Based"` — verified: page 1 starts `1:1:1, 1:1:2, …`. The API returns
`sura`, `aya`, `word_idx` where **`word_idx` is 0-based**. So:

```kotlin
val wordId = "${w.sura}:${w.aya}:${w.wordIdx + 1}"   // == MushafWord.id
```

No lookup table, no fuzzy matching. Note the DB also stores the **ayah-end marker** as a word
row (`char_type = "end"`, e.g. `2:1:2` for 2:1 which has one real word) — the API never
returns that index, so markers simply stay unhighlighted. Correct behaviour, no special case.

---

## Decisions taken

1. **Code lives inside `:mushaf:domain` / `:mushaf:data` / `:mushaf:presentation`**, in new
   `recite/` packages — not a separate `:taahud:*` module set. AGENTS.md §2 forbids one
   presentation module depending on another, and the highlights must be drawn by
   `MushafPageView`. Recorded as a deliberate deviation from the brief's "SHOULD get its own
   module set".
2. **Transport is Ktor WebSocket, not gRPC.** The brief (§Data) predates the API and specifies
   gRPC + `almahir.v1` protobuf; the AI team shipped JSON-over-WS + raw PCM. Follow API.md.
   The mistake taxonomy in the brief still applies — map API `error_type` onto it in `domain`.
3. **⚠️ Ktor's `Android` engine does not support WebSockets.** `MushafDataModule` currently
   builds `HttpClient(Android)`. The AI-service client must use the **OkHttp engine** plus
   `ktor-client-websockets`. Add to `gradle/libs.versions.toml`:
   `ktor-client-okhttp`, `ktor-client-websockets`. Keep the existing quran.com client as-is.
   (`MOBILE_INTEGRATION.md` §9 reaches for raw OkHttp; Ktor-over-OkHttp gives the same
   transport while keeping the constitution's mandated client.)
3b. **Two server bugs to code around** (`MOBILE_INTEGRATION.md` §7): a binary first frame, or a
   first frame that is valid JSON but not an object, crashes the handler and closes **1006**
   instead of a clean 1002. Guarantee ordering client-side — send `start`, await the ack, *then*
   start streaming. Never rely on the close code to diagnose it.
3c. **The live server reports `available_engines: ["real"]` only** — no `mock`, no `zipformer`.
   The mock-engine fallback described in API.md §4 is not available on this deployment, so the
   local Ktor stub (step 3) is the only offline test target.
4. **Energy/RMS VAD**, pure Kotlin, no new dependency (see step 2 for why this is enough).
5. **Base URL is configurable at runtime**, never compiled in (API.md §1 + AGENTS.md
   "no secrets/endpoints in VCS"): a `debugBaseUrl` field in `ReaderPreferencesDataStore`
   with a `BuildConfig` default, exposed in the settings screen for the dev build.

---

## Step 1 — Voice capture, proven locally ✅ *built, awaiting on-device gate*

No network, no VAD, no UI feedback. Only: does the mic produce correct bytes?

**Landed:** `RecitationAudioFormat` / `AudioFrame` (`:mushaf:domain`), `AudioRecordPcmRecorder`,
`PcmCodec`, `WavDebugSink` (`:mushaf:data`), `MicPermissionPreprompt` (`:designsystem`),
mic permission flow + level meter (`:mushaf:presentation`). 55 unit tests green, lint clean.

**On-device gate — run this before starting step 2:**

```bash
adb logcat -c && adb logcat -s Mushaf        # then tap the mic in Recitation mode and recite
# expect: "Capture started: 16kHz mono PCM16 …" then a "Capture 1000ms: … rms=… peak=…" line/sec
adb exec-out run-as com.iti.al_mahir sh -c 'cat cache/recitation-debug/capture-*' > capture.wav
```

Play `capture.wav`: correct pitch and clean speech. A chipmunk pitch means the sample rate,
static means the byte order — the two failures this dump exists to tell apart.

**Build**
- `app/src/main/AndroidManifest.xml`: add `<uses-permission android:name="android.permission.RECORD_AUDIO" />`.
- `:mushaf:data` → `recite/audio/PcmRecorder.kt` — an interface plus
  `AudioRecordPcmRecorder`, exposing `fun record(): Flow<ShortArray>`.
  Exact config from API.md §10:
  `AudioSource.VOICE_RECOGNITION` (**not** `MIC` — it disables the AGC/noise suppression that
  distorts the sustained vowels madd grading measures), `16000`, `CHANNEL_IN_MONO`,
  `ENCODING_PCM_16BIT`, read on `Dispatchers.IO`, 100 ms frames (1600 samples / 3200 bytes).
- `recite/audio/PcmCodec.kt` — `ShortArray.toLittleEndianBytes()` using
  `ByteBuffer.allocate(n * 2).order(ByteOrder.LITTLE_ENDIAN)`. Never trust the platform default.
- **Debug sink**: `recite/audio/WavDebugSink.kt` — writes captured frames to a 16k/mono/16-bit
  WAV in `context.cacheDir`, behind a debug-only flag. This is the whole debugging story for
  audio; without it every later bug looks identical.
- `:mushaf:presentation` → mic pre-prompt (TAH-02/SEC-03): a **privacy explainer dialog must
  appear before the OS permission dialog**. New `:designsystem` component
  `MicPermissionPreprompt`. Strings in `values/` **and** `values-ar/`.

**Gate**
- Tap mic in `RECITATION` mode → pre-prompt → OS dialog → recording starts.
- `adb pull` the WAV; it plays back as clean, correctly-pitched speech. Wrong sample rate or
  byte order is instantly audible (chipmunk / static) — this is why the WAV dump comes first.
- Log peak RMS per second; confirm it moves with speech and rests near the noise floor.
- Recording stops on mic-off, on `onStop`, and releases `AudioRecord` (no leak on rotation).

---

## Step 2 — VAD gating: drop non-speech, **but keep the pauses** ✅ *built, awaiting on-device gate*

**The trap, now confirmed by the backend team.** `MOBILE_INTEGRATION.md` §6 says outright:
*"Do not try to detect pauses yourself — the server runs silero VAD and decides where chunks
end. Sending silence is correct and expected; it is how the server knows a waqf happened."*

The server finalizes a chunk only after hearing **≥300 ms of silence** (API.md §5.3). Strip all
silence and no chunk ever finalizes: no feedback until the 19 s forced cut. So this is a *gate*,
not a *stripper* — it drops the long dead air **between** phrases and keeps the pauses **around**
them.

`SpeechGate` lives in **`:mushaf:domain`** (`model/recite/`), not `:mushaf:data` as first
planned — it is pure Kotlin with no Android types, "what counts as the reciter speaking" is a
business rule, and domain placement lets it be tested without the framework.

| Parameter | Value | Why |
|---|---|---|
| Frame | 100 ms (the capture frame) | pre-roll makes finer resolution unnecessary |
| Speech test | RMS > max(noiseFloor × 2.5, 0.004) | floor adapts down fast, up slowly, **never on speech** |
| Onset confirm | 2 frames (200 ms) | rejects clicks; the server discards <200 ms anyway |
| **Pre-roll** | 300 ms, replayed on onset | never clip the start of a word |
| **Hangover tail** | **600 ms — 2× the server's threshold** | enforced by `require()` in `SpeechGateConfig` |
| Warm-up | 500 ms unconditional pass-through | a reciter who starts instantly is never cut |
| Long silence | dropped beyond the tail | the actual bandwidth saving |

Two design points worth keeping:

- **`SpeechGateConfig.Disabled`** streams continuously — the A/B control for proving the gate
  is not the cause of a scoring regression once the backend is live.
- **`captureSpeech()` is a second repository capability**, not a flag on `capture()`. "Hear the
  microphone" and "hear the reciter" are different questions, and only the second may discard
  audio. It emits `SpeechEvent.Audio` / `SpeechEvent.SpeechEnded`, because the gate opens
  *retroactively* (replaying pre-roll) so a frame's fate is not known when it is captured.

**Consequence to remember:** because dead air is dropped, the server's `audio_span_sec` measures
*streamed* audio, not wall-clock time. Do not place events on a real-time timeline with it.

**On-device gate — run before starting step 3:**

```bash
adb logcat -c && adb logcat -s Mushaf     # recite a few ayat with pauses, then stop
# expect on stop: "Speech gate (enabled=true): sent N/M frames … dropped XX% …"
adb exec-out run-as com.iti.al_mahir sh -c 'cat cache/recitation-debug/raw-*'   > raw.wav
adb exec-out run-as com.iti.al_mahir sh -c 'cat cache/recitation-debug/gated-*' > gated.wav
```

Compare the two: `gated.wav` is materially smaller, **every word intact**, and ≥0.5 s of silence
still present after each phrase. If a word is clipped, raise `preRollFrames` or lower
`speechFactor` — do not lower `hangoverFrames`.

### Also landed here — network prep for your local backend

`app/src/debug/` now carries a `network_security_config.xml` permitting cleartext to
`10.0.2.2`, `10.0.3.2`, `localhost`, `127.0.0.1`, `192.168.1.3` only. It is in the **debug
source set**, so the release manifest has no `networkSecurityConfig` at all (verified). Update
the `192.168.1.3` entry when your DHCP lease changes, or use `adb reverse tcp:8100 tcp:8100`
and dial `localhost` to avoid both the firewall and the IP churn.

---

## Step 3 — WebSocket protocol client ✅ *built and verified against the live GPU server*

**Verified against the real backend**, not just the stub:

```
[live] health: status=healthy, engine=real, availableEngines=[real], device=cuda,
       muaalem=obadx/muaalem-model-v3_2, segmenter=obadx/recitation-segmenter-v2
[live] 18 tajweed rules, 37 moshaf fields
[live] session 392cdaaa-…-75c7ffa3b241 on engine 'real'  → done
```

Run it yourself — it skips cleanly when the server is down, so it is safe on CI:

```bash
./gradlew :mushaf:data:testDebugUnitTest --tests "*LiveServerSmokeTest*" -i
# elsewhere:      -DalmahirServer=192.168.1.3:8100
# real recitation: -DalmahirWav=C:/path/to/gated.wav   (a WavDebugSink dump)
```

Landed in `:mushaf:data/recite/remote/`: `AiServiceConfig` (one authority, both schemes),
`AiServiceApi` (health / tajweed-rules / moshaf-schema), `LiveRecitationSocket`, DTOs, plus
`WavPcmRecorder` for replaying a capture instead of speaking, and `FakeAiService` — an
in-process Ktor stub replaying the API.md payloads verbatim.

**The ack is awaited before any audio is sent.** Ordering is enforced rather than assumed: a
binary first frame crashes the real server's handler and closes **1006**, so the symptom would
be a generic network error pointing at the wrong layer. One round trip removes the whole class
of bug.

**Known flake, unresolved:** the live session test failed once in ~15 runs with the socket
closing before `done`, and has not reproduced since (5/5 isolated, 2/2 full-suite, 1/1 parallel
build). `LiveRecitationSocket` now throws `LiveSessionException` carrying the close code instead
of completing silently, so a recurrence will name 1002 vs 1006 rather than "never reached done".

---

### Original step-3 notes

`:mushaf:data` → `recite/remote/`:
- `AiServiceApi.kt` — `GET /health`, `GET /tajweed-rules`, `GET /moshaf-schema` (Ktor, plain REST).
- `LiveRecitationSocket.kt` — the session. Contract, in order:
  1. Connect `ws://<host>:8100/ws/session`.
  2. Send `start` as a **text** frame, first, always valid JSON — a non-JSON first frame is a
     hard close 1002. Include `sura`/`aya`/`word_idx` from the current page: with a cursor the
     server does a cheap windowed match and is immune to mutashābihāt; without one the basmalah
     is genuinely ambiguous.
  3. Read the `session` ack. **Compare `ack.engine` to what was requested** and surface a
     mismatch — an unbuilt engine silently falls back and the ack is the only evidence.
  4. Stream gated audio as **binary** frames (`Frame.Binary`). A `String` send becomes a text
     frame and is silently ignored as a control message.
  5. Collect inbound `feedback` / `done`, expose as `Flow<LiveFeedbackDto>`.
  6. `seek` on page turn / ayah tap; `end` on stop, then **wait for `done`** before closing or
     the last chunk is lost.
- DTOs in `recite/remote/dto/` — `@Serializable`, per-resource (`FeedbackDto`, `WordDto`,
  `WordErrorDto`, `TajweedRuleDto`, …), `ignoreUnknownKeys = true`. No screen-shaped envelope.

**Local stub for testing before the server exists** (server not yet reachable): a
~40-line Ktor CIO server in `mushaf/data/src/test` that accepts the socket, asserts the first
frame is JSON, counts binary bytes, and replays a canned `feedback` payload copied verbatim
from API.md §5.4. Plus a **WAV-replay source** implementing `PcmRecorder` that reads the step-1
dump instead of the mic — makes every later step reproducible without speaking.

**Gate**
- `GET /health` parses; `available_engines` cached.
- Against the stub: start frame is valid JSON, audio arrives as binary, `end` → `done` → clean
  1000 close. Kill the stub mid-session → 1006 surfaced, not a crash.
- Against a real server when available: `feedback` events arrive as you pause, `chunk_seq`
  increments, and the logged `words[]` matches what you recited.

---

## Step 4 — Domain models and mapping ✅ *built, 27 new tests*

The theme: **the service's safety rules are enforced by the type system, not by remembering them.**

- `RecitationMatch` is a **sealed hierarchy**, not a status enum beside a `words` list. On
  `Ambiguous`/`NoMatch` the words do not exist, so scoring someone against a verse they were not
  reciting is a compile error rather than a code review.
- `RecitationWordFeedback.mark` folds `trimmed` in, so the *obvious* property to reach for is
  already the safe one: a trimmed word reads `UNVERIFIED` however the service labelled it.
  `countsAsMistake` and `scorableMistakes` exclude hints and unverified words by construction.
- **Unknowns resolve towards silence.** An unrecognised word status softens to a hint; an
  unrecognised chunk status becomes `NoMatch`. A server that grows a value costs this client
  precision, never a false correction.

`LiveRecitationRepository` is **one capability**, not three: microphone, gate and socket start and
stop together, and a caller able to start one without the others could only build a broken
session. `LiveRecitationRepositoryImpl` assembles them and reuses `captureSpeech()`, so the
raw/gated WAV dumps keep working inside a live session.

Two ordering bugs the pipeline tests caught, both fixed:

1. **The microphone opened before the handshake.** `Level` events raced ahead of `Started`, and a
   session that never connected still opened the mic. Capture now starts on `Started`.
2. **`Finish` before the handshake crashed the session.** The control closed the command channel,
   then the late handshake started capture and wrote to it —
   `ClosedSendChannelException`. A reciter double-tapping the mic on a slow first connection
   produces exactly this. Guarded, and pinned by
   `stopping before the handshake lands ends cleanly without opening the microphone`.

Deviation from the plan: no `StopLiveRecitationUseCase` / `SeekLiveRecitationUseCase`. Stop and
seek are `RecitationControl` values emitted into the running session, so separate use cases would
be empty pass-throughs. `StartLiveRecitationUseCase` is the single entry point.

### Original step-4 notes

`:mushaf:domain` → `model/recite/` (pure Kotlin, one file per concept):
- `RecitationWordFeedback(sura, aya, wordIdx, status, errors, isTrimmed)` with
  `val wordId: String get() = "$sura:$aya:${wordIdx + 1}"`.
- `RecitationWordStatus { CORRECT, ALMOST, ERROR }`, `RecitationMistake` (maps API
  `error_type` → the brief's `MistakeCategory`: `normal`→Memorization, `tashkeel`→Tashkil,
  `tajweed`/`sifa`→Tajwid), `RecitationChunk`, `RecitationCursor`, `LiveSessionStatus`
  (`Ok` / `Ambiguous(candidates)` / `NoMatch`).
- `repository/LiveRecitationRepository.kt` — capability-oriented:
  `fun session(config): Flow<RecitationChunk>`, `suspend fun seek(...)`, `suspend fun stop()`.
- `usecase/` — `StartLiveRecitationUseCase`, `StopLiveRecitationUseCase`,
  `SeekLiveRecitationUseCase`. Single-purpose; no `GetEverythingForScreenX`.

`:mushaf:data` → `recite/RecitationFeedbackMapper.kt` (DTO → domain) and
`repository/LiveRecitationRepositoryImpl.kt` wiring recorder → gate → socket → mapper.

**Gate**: unit tests on the mapper using the three real captures pasted from API.md
(§5.4 ok, §5.6 error-with-madd, §5.9 ambiguous) — assert `wordId` strings, statuses,
`trimmed`, and that `errors[]` is never non-empty on a `correct` word.

---

## Step 5 — Wire into the ViewModel ✅ *built, 21 reducer tests*

`MushafViewModel` now drives a real session in `RECITATION` mode. `MUALLEM` still runs the
simulated highlight until it is migrated onto the same pipeline.

`MushafUiState.liveCorrection` (a `LiveCorrectionUiState`) holds the session: connecting/active,
the engine actually running, accumulated `wordFeedback`, candidates, `lastOutcome`, cursor and
the selected mistake. `mistakeCount` is **derived** from `countsAsMistake`, never tallied, so
hints and unverified words cannot drift into it.

Decisions worth keeping:

- **Stop flushes, it does not cancel.** `RecitationControl.Finish` lets the server grade the
  utterance still in flight — the last seconds of what was just recited. Cancelling drops them.
  Mode change and backgrounding *do* cancel, because there the recitation is genuinely abandoned.
- **Reconnect is bounded (3 attempts, 1 s apart), seeded from the last cursor.** There is no
  server-side resumption, so a reconnect opens a new session at the last cursor and loses only
  the in-flight chunk. Bounded because retrying forever against an unreachable service looks
  exactly like a reciter making no mistakes — hence `CaptureError.SERVICE_UNREACHABLE` and a
  visible message instead.
- **A permission failure never retries**; only transport failures do.
- **A page turn seeks**, deferred until the page's words are loaded. If no cursor can be derived
  the seek is *skipped, not invented* — seeking to a fabricated position causes exactly the false
  mismatches the seek exists to prevent.

The merge rule lives in `:mushaf:domain` (`RecitationLedger.mergedWith`): **an unscored report
never overwrites a scored one.** API.md §5.4/§5.5 show word 1:1:3 arriving scored `correct` and
then `correct, trimmed` — a naive later-wins merge would downgrade a verdict the reciter had
already earned, and they would watch it disappear.

### Original step-5 notes

`MushafViewModel.toggleRecording()` currently starts `SimulatedHighlightDriver`. In
`RECITATION` mode it now drives the live session instead (`MUALLEM` keeps the simulator for now).

- State additions to `MushafUiState`: `wordFeedback: Map<String, RecitationWordFeedback>`,
  `liveSessionStatus`, `isConnecting`, `engineNotice: Int?`.
- Intents: `StartLiveRecitation`, `StopLiveRecitation`, `SelectMistake(wordId)`.
- Page turn / ayah tap while recording → `SeekLiveRecitationUseCase`.
- Keep the **last `cursor`** from every feedback event; on a 1006 close, reconnect and `start`
  from it (API.md §10 Reconnection).
- **Do not** add a response timeout — silence legitimately produces no events.

**Gate**: reducer unit tests (`MushafReducerTest` pattern) — feedback merges into the map,
mode switch clears it, stop clears it. On-device: mic on → words light up as you recite.

---

## Step 6 — Rendering the feedback ✅ *built, 10 mapper tests*

Two surfaces, matching `helper_ui/correctionbutton.jpeg` and `helper_ui/corrections.jpeg`:

**On the page** (`MushafPageView`, new `wordMarks` param) — mistake words are tinted *and*
underlined solid; hints are tinted amber *and* underlined dashed; correct and unverified words
are left completely unmarked. **The underline, not the tint, is the signal**: in tajweed mode the
page font supplies its own COLR colours and the `drawText` colour override cannot reach them, so
a tint-only design would vanish exactly where tajweed matters most. It also satisfies A11Y-01 —
shape distinguishes a hint from an accusation for a colour-blind reciter.

**In the bottom bar** — the corrections badge, accuracy pill and status pill sit on their own
line *inside* `MushafBottomBar`, passed as a `statusRow` slot. Not floating over the page: the
muṣḥaf stays fully readable, and the pills hide with the rest of the chrome on a tap. The slot
keeps the bar unaware of what a correction session is.

**In a sheet** — `CorrectionsSheet` lists **one row per mistaken word**, not one per āyah. Each
row names the word, what was wrong with it, and the explanation where one exists
("المد الطبيعي: المتوقع ٢، قرأت ٣"); the full āyah sits above it as context, since a flagged word
alone is unreadable. Per-word matters because an āyah can go wrong in several places for
different reasons — one label per āyah showed the first and silently hid the rest. Tapping a row
highlights that exact word on the page.

Rules enforced in `CorrectionsUiMapper`, each with a test:

- **Hints and unverified words never produce a card.** They still appear *inside* a card as
  context — a lone flagged word is unreadable — but never marked, never counted.
- **No invented explanations.** A finding without a rule and both lengths gets no detail line.
  Telling a reciter something specific and wrong about their tajwīd is worse than saying nothing.
- The badge appears only once there is something to review; a permanent "0" reads as a score.

### Connecting a physical device (the "no output" bug)

The default address was `10.0.2.2:8100` — the Android **emulator's** alias for the host loopback,
which a physical phone cannot reach. Compounding it, Windows Firewall has **no inbound rule for
8100**, so the LAN IP would have failed too. Both fail *silently*: no connection, no feedback, no
badge, and with OkHttp's 10 s default connect timeout × 3 reconnects, half a minute before any
error appeared.

Fixed three ways:

1. **The address is configurable.** `almahir.aiService` in `local.properties` (git-ignored, so no
   endpoint in VCS) feeds a `BuildConfig` field in `:mushaf:data`. `-Palmahir.aiService=…` also
   works. Default remains the emulator alias.
2. **Connect timeout is 4 s**, so a wrong address surfaces in seconds rather than looking like a
   reciter being ignored.
3. **`LiveSessionStatusRow` makes silence legible** — connecting / listening / didn't catch that.
   A session with no mistakes and a session that never connected both produce no marks; without
   this they are indistinguishable, and a reciter reads the second as a flawless recitation.

Recommended setup, works for a device *and* the emulator and needs no firewall rule:

```bash
adb reverse tcp:8100 tcp:8100      # re-run after replug or reboot
# local.properties: almahir.aiService=localhost:8100
```

> **Pulling debug WAVs on Windows:** `adb exec-out … > file.wav` in **PowerShell corrupts the
> file** — it re-encodes as UTF-16 *and* inserts a CR before every LF, which is lossy. Use
> `adb shell "run-as com.iti.al_mahir cat cache/recitation-debug/gated-*.wav > /sdcard/g.wav"`
> then `adb pull /sdcard/g.wav`, or run the redirect from Git Bash.

### The accuracy percentage

Built at the product owner's request, matching the reference design's `٩٨.٤٪` pill —
**with the caveat recorded rather than forgotten.** `API.md` §5.2 and `MOBILE_INTEGRATION.md` §15
both say the thresholds separating `almost` from `error` are *uncalibrated placeholders*, so this
figure can move when the model is retuned without the reciter reciting any differently.

It is therefore constrained to be as honest as the data allows:

- **Hints do not lower it.** The service softened those findings on purpose; re-hardening them
  into a penalty here would undo the safety property.
- **Unverified words are excluded from both halves of the fraction** — counting them as successes
  would inflate it, as failures would penalise a recitation nobody graded.
- **It does not appear until something has been scored**, and it shows its denominator, so
  "100%" after three words reads as three words rather than perfection.

Do not build a grade, streak or ranking on it until the thresholds are calibrated against a
labelled set.

### Original step-6 notes

New `:designsystem` component `MistakeHighlight`; `MushafPageView` takes
`wordFeedback: Map<String, …>` alongside the existing `highlightedWordId`.

These are **hard requirements** from API.md §5.5, not preferences:

| Case | Render |
|---|---|
| `error` | color **+ icon + text** (A11Y-01 — never colour alone), tappable → detail sheet |
| `almost` | a **hint**. Never in the mistake list, never counted in a score. It means the model wasn't confident enough to accuse — falsely correcting a perfect recitation is the one failure this system must not produce |
| `trimmed: true` | **neutral, whatever `status` says.** The word was cut by the chunker and *not scored*. No tick, no green. Read this flag before trusting `status` |
| `ambiguous` | show candidate verses (they carry their text) — mark **no** word |
| `no_match` | neutral "didn't catch that" — mark nothing |
| `non_verse` (istiaatha/basmalah/sadaka) | acknowledge, don't score |
| engine ≠ requested | visible notice |

Mistake detail via `AppBottomSheet`, using `uthmani_pos` for the character span and
`tajweed_rules[].name_ar` + `expected_len`/`predicted_len` for the explanation
("المد الطبيعي: expected 2, you held 3"). The two phoneme spans are diagnostic only — different
coordinate systems, they diverge on every real error.

**Gate**: the API.md §10 rendering checklist, run against a live session. Verify in Arabic RTL.

---

## Step 7 — Robustness

Lifecycle (release mic on background, AUD-03 interruption handling), offline → disable live mode
with a clear message rather than failing silently (AI-12/AVL-02), backpressure (keep streaming
while feedback lags — the server never blocks sends), thermal/battery over a 1 h session (PRF-03).

## Step 8 — Settings, session summary, history

`/moshaf-schema` (37 fields, build the panel from the response — and send `madd_monfasel_len`
explicitly, the schema default `2` differs from the server's grading default `4`),
`/tajweed-rules` picker, strictness, engine picker greyed by `available_engines` — with the
honest warning that `zipformer` gives word tracking but **no** tajwīd grading. Then TAH-07/10/12:
live accuracy bar, summary, `SessionLog` to Room.

---

## Files touched, by step

| Step | Primary files |
|---|---|
| 1 | `app/src/main/AndroidManifest.xml`; new `mushaf/data/.../recite/audio/`; new `:designsystem` `MicPermissionPreprompt`; `MushafBottomBar.kt` |
| 2 | `mushaf/data/.../recite/audio/SpeechGate.kt` + tests |
| 3 | `gradle/libs.versions.toml`; `data/di/MushafDataModule.kt`; new `recite/remote/` |
| 4 | new `mushaf/domain/.../model/recite/`, `repository/`, `usecase/`; `recite/RecitationFeedbackMapper.kt` |
| 5 | `MushafViewModel.kt`, `state/MushafUiState.kt`, `state/MushafIntent.kt` |
| 6 | `components/MushafPageView.kt`; new `:designsystem/MistakeHighlight`; `values/` + `values-ar/strings.xml` |

## Verification summary

- `./gradlew :mushaf:data:test :mushaf:domain:test :mushaf:presentation:test` — gate, mapper,
  reducer.
- `./gradlew assembleDebug` + lint clean; no `data` type reaching `presentation`.
- Manual: WAV dump (step 1) → gated WAV (step 2) → stub session (step 3) → live session
  (steps 5–6) → API.md §10 checklist in both locales.
