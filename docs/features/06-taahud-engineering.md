# Ta'ahud (Live AI Correction) — engineering handbook

**The one document to read before touching this feature.** It carries the contract, the
invariants, the architecture, what is built, and what is not.

- **Product brief:** [06-taahud-live-correction.md](06-taahud-live-correction.md) — *what* to build
- **Service contract:** [../API.md](../API.md) — every field and value
- **Connecting a device:** [../MOBILE_INTEGRATION.md](../MOBILE_INTEGRATION.md)
- **Engineering rules:** [../../AGENTS.md](../../AGENTS.md) — architecture, MVI, stack, localization

**Status:** live correction works end to end against the real GPU service. ~175 unit tests,
lint clean. Sessions are recorded locally and reviewable, and every API.md §10 contract
obligation is now rendered. The recitation-settings surface (engine / strictness / tajwīd rules /
moshaf) is **not built** — see [§7.2](#72-recitation-settings-the-whole-surface).

---

## 1. What it is

The reciter opens the muṣḥaf, switches to **تلاوة** mode, taps the mic and recites. Audio is
captured, gated, and streamed to the Al-Mahir AI service over a WebSocket. The service replies
with per-word verdicts after each pause; the page marks mistakes inline, a sheet lists them by
category, and finishing the session files a summary the reciter can review later.

---

## 2. Quick start

The single most common failure is a device that cannot reach the service, which fails **silently**
in every layer above it.

```bash
# 1. Backend running on the laptop (see SETUP.md in the AI repo)
curl http://localhost:8100/health          # expect {"status":"healthy","engine":"real",...}

# 2. Tunnel the port to the device — works for a phone AND the emulator,
#    and sidesteps Windows Firewall, which has no inbound rule for 8100.
adb reverse tcp:8100 tcp:8100              # ⚠ lost on every replug/reboot — re-run it

# 3. Point the app at it (git-ignored, so no endpoint in VCS)
#    local.properties:
#      almahir.aiService=localhost:8100

./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

| Client | `almahir.aiService` |
|---|---|
| Emulator, no tunnel | `10.0.2.2:8100` (the build default) |
| Phone or emulator **with** `adb reverse` | `localhost:8100` ← recommended |
| Phone over Wi-Fi | the laptop's LAN IP, e.g. `192.168.1.3:8100`, **plus** a firewall rule |

Cleartext is permitted only in `app/src/debug/res/xml/network_security_config.xml`. The release
manifest carries no `networkSecurityConfig` at all.

**Verify without the app** (skips cleanly when the server is down, so it is CI-safe):

```bash
./gradlew :mushaf:data:testDebugUnitTest --tests "*LiveServerSmokeTest*" -i
# point elsewhere:   -DalmahirServer=192.168.1.3:8100
# real recitation:   -DalmahirWav=C:/path/to/gated.wav
```

---

## 3. The contract — facts that are not negotiable

From `API.md` and `MOBILE_INTEGRATION.md`. Each of these has cost time or would have.

| # | Fact | Consequence |
|---|---|---|
| 1 | Audio is **16 kHz mono PCM16 little-endian, raw** — no WAV header, no float, no 44.1 kHz | A mismatch does not error. It produces *plausible but wrong* feedback. If a good recitation reports errors everywhere, **suspect the sample rate before the model** |
| 2 | The `start` message must be the **first frame** and must be **text JSON** | A binary first frame crashes the server handler and closes **1006**, not the documented 1002 — so the symptom points at the network, not at ordering |
| 3 | Feedback is **pushed per waqf chunk**, ~300 ms of silence after speech | There is **no per-word real-time signal**. Anything that looks live is an estimate |
| 4 | **Silence is meaningful.** The server's VAD needs to hear the pause | Stripping all silence means chunks never finalize — no feedback at all until the 19 s forced cut |
| 5 | `word_idx` is **0-based**; the layout DB keys words **1-based** | `wordId = "$sura:$aya:${word_idx + 1}"`. Off by one and every mistake lands on the neighbouring word |
| 6 | An unbuilt engine **falls back silently**; the ack is the only evidence | Compare `ack.engine` to the request and tell the user |
| 7 | An out-of-range `moshaf` value makes the server discard the **entire** object | Indistinguishable from the setting being ignored. Build the panel from `/moshaf-schema` |
| 8 | This deployment reports `available_engines: ["real"]` only | No `mock`, no `zipformer`. `FakeAiService` is the only offline test target |
| 9 | Ktor's **Android engine does not implement WebSockets** | The AI client runs on the **OkHttp** engine with `readTimeout(0)` |
| 10 | Confidence thresholds are **uncalibrated placeholders** | Do not build a grade, streak or ranking on accuracy without calibrating first |

---

## 4. The invariants

The service deliberately declines to assert things it cannot support. A client that renders those
refusals as assertions destroys the safety property. **These are enforced in code and pinned by
tests — do not weaken one without reading its test first.**

| Invariant | Where it lives |
|---|---|
| **`almost` is a hint, never a mistake.** Never listed, never counted, never scored against | `RecitationWordFeedback.mark`, `countsAsMistake` |
| **`trimmed` means unverified, not correct.** No tick, no green, whatever `status` says | `RecitationWordMark.UNVERIFIED` — trimming wins over status |
| **`ambiguous`/`no_match` assert nothing.** Words are unreachable by construction | `RecitationMatch` sealed hierarchy |
| **An unscored report never overwrites a scored one** | `RecitationLedger.mergedWith` |
| **Unknown values resolve toward silence** — unknown word status → hint, unknown chunk status → no match | `RecitationFeedbackMapper` |
| **Unverified words are excluded from both halves of accuracy** | `LiveCorrectionUiState.accuracy`, `RecitationSessionSummary.accuracy` |
| **Hints and unverified words can never become a diagnosis** | `practiceFocus` (min. 2 occurrences, confident only) |
| **A session that graded nothing is saved and says so** | `gradedNothing` → explicit message, never 0 % |
| **The pacer's estimate is position, never correctness** | Neutral reading cursor only; capped at 6 words past ground truth |
| **Status is felt as well as seen** (A11Y-01) | Haptic on new mistake; colour **+** shape (solid vs dashed underline) |

---

## 5. Architecture

### 5.1 Why things live where they do

Profile (`:presentation`) and Mushaf (`:mushaf:presentation`) are **sibling modules that must not
depend on each other** (AGENTS.md §2). That single constraint explains most of the layout.

```
:domain            session records, SurahNames        ← shared by both presentation modules
:data              Room (AlmahirDatabase), session repo
:designsystem      all shared UI (sheets, pills, summary)   ← may not depend on domain/feature
:mushaf:domain     live protocol models, gate, pacer, recorder  (depends on :domain)
:mushaf:data       audio capture, WebSocket, mappers
:mushaf:presentation  reader UI, MVI
:presentation      profile → session history
```

`RecitationSessionRecorder` sits in `:mushaf:domain` because it is the **only** place that sees
both the live taxonomy and the recorded one.

### 5.2 Runtime flow

```
AudioRecord (VOICE_RECOGNITION, 16k mono PCM16)
   └─ SpeechGate            drop long silence, keep pre-roll + 600 ms tail
        └─ LiveRecitationSocket   start → ack → binary frames → end → done
             └─ RecitationFeedbackMapper   DTO → domain
                  └─ MushafViewModel
                       ├─ RecitationLedger   merge chunks (unscored never overwrites scored)
                       ├─ RecitationPacer    live cursor between chunks
                       └─ RecitationSessionRecorder → :domain repo → Room
```

`LiveRecitationRepositoryImpl` owns the whole pipeline as **one capability** — mic, gate and
socket start and stop together, because a session with any one missing is not partially working,
it is broken.

### 5.3 File index

<details>
<summary><b>:mushaf:domain</b> — pure logic, all unit-tested</summary>

| File | Role |
|---|---|
| `model/recite/RecitationAudioFormat.kt` | The wire format, in one place |
| `model/recite/AudioFrame.kt` | Samples + `rms()`/`peak()`. Not a data class — array equality would be wrong |
| `model/recite/SpeechGate.kt` | Drops idle silence, **keeps the waqf** |
| `model/recite/RecitationPacer.kt` | Live cursor estimate between chunks |
| `model/recite/RecitationLedger.kt` | `mergedWith` — the unscored-never-overwrites rule |
| `model/recite/RecitationChunk.kt` | `RecitationMatch` sealed type |
| `model/recite/RecitationWordFeedback.kt` | `mark`, `countsAsMistake`, `scorableMistakes` |
| `model/recite/RecitationCursor.kt` | `wordId` ⇄ `fromWordId`, the 0/1-based bridge |
| `model/recite/PracticeFocus.kt` | Recurring findings |
| `model/recite/RecitationSessionRecorder.kt` | Live → recorded, the only crossing point |
| `repository/LiveRecitationRepository.kt`, `RecitationCaptureRepository.kt` | Contracts |
| `usecase/StartLiveRecitationUseCase.kt` | Entry point |

</details>

<details>
<summary><b>:mushaf:data</b> — capture and transport</summary>

| File | Role |
|---|---|
| `recite/audio/AudioRecordPcmRecorder.kt` | `VOICE_RECOGNITION`, releases mic in `finally` |
| `recite/audio/PcmCodec.kt` | Explicit little-endian; `count` param so a short read can't leak stale buffer |
| `recite/audio/WavDebugSink.kt` | Playable WAV in `cacheDir`, debug builds only |
| `recite/audio/WavPcmRecorder.kt` | Replays a WAV instead of the mic — reproducible sessions |
| `recite/remote/AiServiceConfig.kt` | One authority → both schemes; from `BuildConfig` |
| `recite/remote/LiveRecitationSocket.kt` | The session. **Awaits the ack before any audio** |
| `recite/remote/AiServiceApi.kt` | health / tajweed-rules / moshaf-schema — **built, not yet used by the app** |
| `recite/RecitationFeedbackMapper.kt` | DTO ⇄ domain |
| `repository/LiveRecitationRepositoryImpl.kt` | Assembles mic + gate + socket |

</details>

<details>
<summary><b>:mushaf:presentation</b>, <b>:designsystem</b>, <b>:domain</b>/<b>:data</b>, <b>:presentation</b></summary>

| File | Role |
|---|---|
| `MushafViewModel.kt` | Session lifecycle, reconnect, pacer, page-follow, recording |
| `state/LiveCorrectionUiState.kt` | Live session aggregate; `accuracy`, `practiceFocus` |
| `recite/CorrectionsUiMapper.kt` | Feedback → per-āyah cards, one row per mistaken word |
| `recite/CorrectionFilter.kt` | Category chips + filtering |
| `recite/MicPermission.kt` | Pre-prompt → OS dialog ordering (SEC-03) |
| `components/MushafPageView.kt` | `wordMarks` → tint **+** underline |
| `designsystem/components/mushaf/*` | Badge, pills, corrections sheet |
| `designsystem/components/session/*` | Summary content + sheet, shared with Profile |
| `domain/model/recitation/*`, `domain/repository`, `domain/usecase` | Session records |
| `data/local/recitation/*`, `data/repository/RecitationSessionRepositoryImpl.kt` | Room |
| `presentation/sessions/*` | Profile → session history |

</details>

### 5.4 Key tuning values

| Knob | Value | Why |
|---|---|---|
| `SpeechGateConfig.hangoverFrames` | 6 (600 ms) | 2× the server's 300 ms waqf threshold. **`require()`d — do not lower** |
| `SpeechGateConfig.preRollFrames` | 3 (300 ms) | Never clip a word's onset |
| `RecitationPacerConfig.maxLookaheadWords` | 6 | The cursor stops when the service stops confirming |
| `MAX_RECONNECT_ATTEMPTS` | 3, 1 s apart | Retrying forever looks exactly like a flawless recitation |
| OkHttp `connectTimeout` | 4 s | A wrong host must fail fast, not look like being ignored |
| OkHttp `readTimeout` | 0 | The server pushes only on a pause |

If a word gets clipped: raise `preRollFrames` or lower `speechFactor`. **Never** lower
`hangoverFrames`.

---

## 6. What works today

| Capability | Status | Notes |
|---|---|---|
| TAH-02 Mic pre-prompt before OS dialog | ✅ | `MicPermissionPreprompt` |
| TAH-03 On-device VAD | ✅ | Energy gate; gates, does not strip |
| Live session (connect → stream → grade → end) | ✅ | Verified against the real `real`/CUDA server |
| TAH-04 Mistake taxonomy | ✅ | Memorization / Tashkil / Tajwid / Other |
| TAH-05 Inline highlight, colour **+** shape | ✅ | Tint + solid/dashed underline |
| Live follow-along cursor | ✅ | Pacer, learns the reciter's pace |
| Auto page-turn while reciting | ✅ | Confirmed cursor only, never the estimate |
| Corrections sheet, per-word rows, category chips | ✅ | |
| "يستحق المراجعة" practice focus | ✅ | ≥2 occurrences, confident only |
| Accuracy + mistake-count pills (in the bottom bar) | ✅ | Cleared when the session ends |
| Haptics on a new mistake | ✅ | A11Y-01 |
| Session boundary ("finish & start new") | ✅ | Flag icon beside the mic |
| TAH-09/10 Finish + summary | ✅ | Sheet over the muṣḥaf |
| TAH-12 Save to history (**local**) | ✅ | Room; Profile → جلسات التلاوة |
| Reconnect from last cursor | ✅ | Bounded, then `SERVICE_UNREACHABLE` |
| Engine-substitution notice | ✅ | Dismissible banner naming the engine |
| Ambiguous candidate picker | ✅ | Non-modal; tapping seeks the service |
| `non_verse` acknowledgement | ✅ | Pill, explicitly "not scored" |
| Content attributions (Tanzil, KFGQPC) | ✅ | Profile → التنويهات; local, unconditional |
| Localization ar + en, RTL | ✅ | Incl. Arabic plurals (six forms) |

---

## 7. Not built yet

Ordered by how much it matters.

### 7.1 Contract obligations — ✅ closed

All four are built. Kept here because they are the API.md §10 checklist items and the reason
each exists is not obvious from the code alone.

1. **Engine-substitution notice** — a dismissible amber banner above the muṣḥaf controls, naming
   the engine that actually ran. On `zipformer` a session gives word tracking and **no** tajwīd
   grading, so an engine the reciter did not choose changes what the app can teach them.
2. **Ambiguous candidates** — a non-modal card listing each candidate with **its verse text**,
   because "(27, 30)" is a lookup the reciter would otherwise have to perform before they could
   answer. Tapping one seeks the service, which resolves the ambiguity for every later chunk.
   Non-modal on purpose: the contract allows simply waiting for the next chunk to resolve it.
3. **`non_verse` acknowledgement** — istiʿādha / basmalah / ṣadaqa appear as a neutral pill
   reading "غير مُقيَّمة". Acknowledged, never scored: the reciter knows they said it, and
   swallowing it reads as the system not listening.
4. **Tanzil credit** — Profile → **التنويهات** (`AttributionsScreen`), crediting the Tanzil
   Project with a link to <https://tanzil.net>, plus KFGQPC for the glyphs and the upstream ASR
   models. Deliberately **local and hardcoded**, not fetched like the terms and privacy pages: an
   attribution that depends on a server returning the right markdown is one that can silently
   disappear, and the CC BY 3.0 obligation binds the app whether or not a backend answers.

**Known limit:** selecting a candidate seeks the *service* but does not navigate the reader to a
distant sūrah — mapping an arbitrary `wordId` to a page needs a layout-DB lookup that does not
exist yet. Auto page-turn only follows onto already-loaded neighbours.

### 7.2 Recitation settings (the whole surface)

`AiServiceApi` exposes `/health`, `/tajweed-rules` and `/moshaf-schema`, is registered in Koin,
and **is never called by the app**. Nothing sets `strictness`, `engine`, `rules` or `moshaf`, so
every session runs on server defaults.

Build the panel **from the response**, never hardcoded — legal ranges are not uniform
(`madd_monfasel_len` 2–5, `madd_mottasel_len` 4–6), and an out-of-range value discards the whole
moshaf object. Note also that `/moshaf-schema` reports `madd_monfasel_len` default `2` while the
server grades with `4`; send the field explicitly if the displayed value must match the graded one.

### 7.3 Other gaps

- **TAH-06 mistake detail overlay.** `selectedMistakeWordId` is set by an intent and nothing opens
  a detail sheet; explanations are inline in the corrections list instead.
- **MUALLEM mode** still runs `SimulatedHighlightDriver`. It should move onto this pipeline.
- **Offline handling (AI-12/AVL-02).** No connectivity check — a session simply fails and reports
  `SERVICE_UNREACHABLE` after ~12 s. Live correction should be disabled up front when offline.
- **AUD-03 audio interruptions** (a call arriving) are not handled beyond `ON_STOP`.
- **Backend sync (TAH-12).** History is local. The repository is written so sync is additive.
- **TLS.** Dev is cleartext over `adb reverse`. See MOBILE_INTEGRATION §15 before shipping.
- **`CaptureRecitationAudioUseCase`** is registered but unused — superseded by the speech variant.
  Remove it or wire it into a diagnostics screen.

---

## 8. Verifying

### 8.1 Automated

```bash
./gradlew :app:assembleDebug \
  :domain:test :data:testDebugUnitTest :presentation:testDebugUnitTest \
  :mushaf:domain:testDebugUnitTest :mushaf:data:testDebugUnitTest \
  :mushaf:presentation:testDebugUnitTest \
  :mushaf:presentation:lintDebug :designsystem:lintDebug
```

~170 feature tests. The ones that encode the invariants, and are the first place to look when
changing behaviour:

| Suite | Guards |
|---|---|
| `SpeechGateTest` (14) | The waqf tail, onset, noise adaptation |
| `RecitationPacerTest` (16) | Silence holds, lookahead cap, pace learning |
| `RecitationFeedbackMapperTest` (20) | The three contract rules, against real API.md captures |
| `RecitationLedgerTest` (6) | Unscored never overwrites scored |
| `RecitationSessionRecorderTest` (10) | Only confident mistakes are written down |
| `LiveRecitationSocketTest` (11) | Ordering, binary frames, engine substitution |
| `MushafReducerTest` (25) | Session lifecycle, reconnect, boundaries, pill clearing |
| `RecitationSessionRepositoryTest` (8) | Round trip, unknown category, corrupt row |

### 8.2 On device

```bash
adb logcat -c && adb logcat -s Mushaf
```

Expect: `Capture started: 16kHz mono PCM16 …` → `Speech gate … dropped XX%` →
`Session <id> on engine 'real'` → `Chunk N: … cursor=…`.

Pull the debug WAVs — **not with PowerShell `>`**, which corrupts binary (UTF-16 *and* a CR before
every LF):

```bash
adb shell "run-as com.iti.al_mahir cat cache/recitation-debug/gated-*.wav > /sdcard/g.wav"
adb pull /sdcard/g.wav
```

**Checklist (API.md §10), in Arabic RTL:**

- [ ] `almost` renders as a hint, absent from the sheet, excluded from the count
- [ ] `trimmed` renders neutrally — no tick, no green
- [ ] `ambiguous` marks no word, and its candidates are listed with their text
- [ ] `no_match` shows a neutral state
- [ ] `non_verse` acknowledged as "غير مُقيَّمة", not scored
- [ ] Engine mismatch surfaced as a dismissible banner naming the engine
- [ ] Tanzil credited — Profile → التنويهات, link opens tanzil.net

---

## 9. Gotchas that have already cost time

| Symptom | Cause |
|---|---|
| No feedback, no badge, nothing in the UI | The device cannot reach the service. `10.0.2.2` is the **emulator's** alias; a phone needs `adb reverse` or the LAN IP **and** a firewall rule |
| Pulled WAV is static / unplayable | PowerShell `>` re-encodes binary as UTF-16 and inserts CR before every LF. Use `adb pull` |
| Sessions never appear in history | Guarding the save on `isRecordingActive` — the mic clears it *before* the server flushes. Guard on the session start timestamp |
| Session ends the instant it restarts | `startLiveCorrection()` cancels `sessionJob`, which is the coroutine the `Finished` handler runs in. Post the restart to `viewModelScope` |
| A whole module fails to compile after a doc edit | Kotlin block comments **nest** — a `/*` inside KDoc (e.g. a `cache/*.wav` glob) opens an unclosed comment |
| Follow-along cursor blank after a page turn | Neighbouring pages are **prefetched**, so a swipe hits a cached page and `requestPage` returns early. Seeding the pacer only in its load callback left it walking the *previous* page's words. Seed on every page change, and place the cursor immediately rather than waiting for a speech frame |
| Integration tests time out instantly | `runTest` uses **virtual** time; `withTimeout` fires immediately against real I/O. Use `runBlocking` |
| `Invalid unicode escape sequence` at `mergeDebugResources` | An unescaped `'` in a string resource. Android needs `'` |
| Ktor test stub reports a nonsense engine | Inside `embeddedServer { }`, a property named `engine` is shadowed by Ktor's own |
| Tajweed-mode mistakes show no tint | COLR page fonts supply their own colour and ignore the `drawText` override. **The underline is the signal**, not the tint |

---

## 10. Deviations from the brief, and why

| Brief says | Built | Reason |
|---|---|---|
| gRPC + `almahir.v1` protobuf | JSON over WebSocket | The AI team shipped that. Taxonomy still maps to the shared one |
| Own `:taahud:*` module set | Inside `:mushaf:*` | Highlights are drawn by `MushafPageView`, and a presentation module may not depend on another |
| — | Accuracy pill built despite the uncalibrated caveat | Product decision, taken explicitly. Constrained: hints don't lower it, unverified excluded, denominator shown |
| Separate Stop/Seek use cases | `RecitationControl` values | They would be empty pass-throughs into a running session |
