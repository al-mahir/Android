# Ta'ahud — live reading-cursor sync analysis

**Scope:** why the on-screen "you are here" highlight during recitation (تلاوة) does not
track the word the reciter is actually saying. This is analysis only — no code changed.

Read alongside [06-taahud-engineering.md](06-taahud-engineering.md) (architecture, contract,
invariants) and [06-taahud-live-correction.md](06-taahud-live-correction.md) (product brief).

---

## 0. Two different "highlights" — scope this correctly first

`MushafPageView` renders **two independent visual layers** over a word, and they come from
two different mechanisms:

| Layer | Driven by | Accuracy |
|---|---|---|
| Colored underline (mistake = solid red, hint = dashed amber) via `wordMarks` | `RecitationWordFeedback.mark`, computed straight from the server's graded `RecitationChunk.words` (`RecitationFeedbackMapper`) | **Correct.** Index-addressed against `word_idx`, not estimated. |
| Translucent "reading cursor" box via `highlightedWordId` (`MushafPageView.kt:202-211`) | `RecitationPacer`, a **client-side time estimator** | **The subject of this doc.** This is what desyncs. |

The user's complaint — "when he reads the word, it should show highlighted" — is about the
second layer, the moving cursor box. The mistake/hint marks are not estimates and are not the
problem. Any fix should leave the mistake layer untouched.

---

## 1. What the cursor box actually is today

There is **no real-time word-level recognition signal** in this system. Per
`06-taahud-engineering.md` §3 fact #3: *"Feedback is pushed per waqf chunk, ~300 ms of silence
after speech. There is no per-word real-time signal. Anything that looks live is an estimate."*

Concretely:

- `RecitationPacer` (`mushaf/domain/.../recite/RecitationPacer.kt`) counts 100 ms audio frames
  classified as "speech" by the on-device energy VAD (`SpeechGate`) and advances a word index
  once enough frames have accumulated for a **guessed, single-number "seconds per word"**
  (`framesPerWord`). It has no idea what phoneme or word is actually being uttered.
- The only ground truth is `RecitationChunk.cursor`, pushed by the server after a waqf pause
  (300 ms+ of silence, then network + ASR latency on top). When it arrives,
  `MushafViewModel.mergeChunk` calls `pacer.confirm(cursor.wordId)`, which hard-snaps the
  estimate to the true position (`RecitationPacer.kt:62-68`).

So the visible behavior is, by construction: **glide on a guess → snap to the truth → glide on
a (slightly better) guess → snap...**, repeating every waqf pause. Between snaps, the cursor is
never actually reading the reciter — it's a metronome calibrated to *average* pace.

This is a real architectural ceiling: the backend contract does not currently expose streaming
partial word matches (see `06-taahud-engineering.md` §7.3, "not built yet"). The problems below
are about how much worse the client makes this ceiling than it needs to be — some are
fixable without backend changes, some are not.

---

## 2. Root causes

### R1 — The cursor is duration-blind at the per-word level

`RecitationPacer.onSpeechFrame()` (`RecitationPacer.kt:79-100`) advances the index whenever
`framesIntoWord >= framesPerWord * drag`. `framesPerWord` is **one scalar for the whole
recitation**, not a per-word estimate. Real word durations vary enormously:

- A short word (e.g. "من", "قد") might take 150–300 ms.
- A word carrying a madd (elongation), shadda, ghunnah hold, or tanwīn can easily take
  800 ms–1.6 s+.

The pacer cannot distinguish these. It will race ahead through a long/elongated word (jumping
2–4 words ahead while the reciter is still mid-word) and lag behind a run of short words.

**Concrete scenario:** reciting "الرَّحْمَٰنِ الرَّحِيمِ" — "الرَّحْمَٰنِ" alone commonly
takes ~800 ms–1 s (madd + shadda + tanwīn). At the default 500 ms/word guess (see R2), the
cursor has already moved to "الرَّحِيمِ" (or past it) before the reciter finishes the first word.

### R2 — Pace learning is chunk-wide, coarse, and slow to converge

`observePace(wordCount, speechFrames)` (`RecitationPacer.kt:71-76`) runs **once per confirmed
chunk**, computing `chunkSpeechFrames / wordCount` as a flat average, then nudges
`framesPerWord` toward it with `paceSmoothing = 0.3` (exponential smoothing).

Consequences:

- **Cold start every time:** the first chunk of every session runs on the arbitrary default
  `initialFramesPerWord = 5` (500 ms/word) — see R3 for how bad that default is for the actual
  target users.
- **Converges to "this reciter's average," never to "this word's expected length."** Even after
  many chunks, within-chunk drift from R1 is not corrected, only the running average is nudged.
- **Learned pace is thrown away on every session (re)start** — `pacer.reset()` is called in
  `startLiveCorrection` (`MushafViewModel.kt:557`) and `clearLiveSession`
  (`MushafViewModel.kt:870`). This fires on:
  - automatic reconnect after a dropped connection (`runSession`, `MushafViewModel.kt:582-615`);
  - a settings-triggered restart, e.g. flipping the حفظ/تجويد toggle
    (`setTajweedGrading`, `MushafViewModel.kt:790-814`, which sets
    `pendingRestart = SessionRestart.SETTINGS` and calls `finishLiveCorrection()`).

  A reciter who has been going for 10 minutes with a well-calibrated pace loses that
  calibration instantly on a transient network blip or a mid-session settings change, and restarts
  from the 500 ms default.

### R3 — The default pace guess is tuned for the wrong kind of reading

`initialFramesPerWord = 5` → 500 ms/word. This is a brisk, fluent pace. The primary users of a
*correction* tool are often reciting deliberately/slowly (tarteel, memorization practice,
tajwīd-conscious pacing), which commonly runs 800 ms–1.5 s+/word. For that (arguably the
**common case for this feature**), the cursor races ahead for the entire first chunk of every
session, which is exactly the failure mode the user is describing.

### R4 — Frame-counting isn't a reliable proxy for wall-clock time

The 100 ms "frame" the pacer counts is defined by `RecitationAudioFormat.FRAME_DURATION_MS`
and produced by a **blocking** `AudioRecord.read(buffer, 0, buffer.size)` sized to exactly
`FRAME_SAMPLES` (`AudioRecordPcmRecorder.kt:53-63`). If the OS ever returns a short read (buffer
pressure, thermal throttling, another app briefly stealing the audio session — all plausible on
a phone with the screen on, mic open, WebSocket streaming, and Compose recomposing at once),
the emitted `AudioFrame` covers less than 100 ms of real audio. `RecitationCaptureRepositoryImpl`
still emits it as one `SpeechEvent.Audio` (`RecitationCaptureRepositoryImpl.kt:51-63`), and
`MushafViewModel.updateMicLevel` still counts it as one full frame tick toward `framesPerWord`
(`MushafViewModel.kt:684-689`). A run of short reads makes the cursor advance faster than real
elapsed time, for reasons that have nothing to do with what was said.

### R5 — Confirmation causes a visible jump, every waqf pause, by design of the current merge

`mergeChunk` (`MushafViewModel.kt:700-731`) calls `pacer.confirm(cursor.wordId)` on every
graded chunk, which hard-sets both `predictedIndex` and `confirmedIndex` to the server's
position (`RecitationPacer.kt:62-68`), discarding whatever the running estimate had drifted to.
If the estimate had raced ahead (R1/R3/R4) or lagged, the highlighted word **visibly jumps**
backward or forward at that instant. Because this happens on every waqf pause — i.e.
continuously through a session — the net feel is glide → jump → glide → jump rather than a
steady word-by-word progression. This is very likely the dominant complaint: it's not just
"slightly off," it's periodically and visibly *wrong* before snapping.

### R6 — No ceiling tied to actual confirmation, and no visual honesty about confidence

- `maxLookaheadWords = 20` (`RecitationPacerConfig`, `RecitationPacer.kt:8`) lets the estimate
  run up to 20 words ahead of the last confirmed word — roughly 2–4 full verses on a typical
  page — before `isAtLookaheadLimit` freezes it (`RecitationPacer.kt:36-37`), and drag only
  starts easing it in after 10 words of lead (`easeAfterWords`). If the pace guess is even
  moderately fast, the cursor can sit many words ahead of the true position, unmoving-but-wrong,
  for a long stretch before the cap engages.
- `MushafPageView` draws the cursor box identically (`MushafPageView.kt:202-211`) whether the
  position is confirmed or guessed — same translucent rect, no dashed/dimmed/pulsing treatment
  for "this is an estimate." Every bit of drift reads to the reciter as a flat bug rather than
  expected estimator uncertainty, because the UI never admits uncertainty exists.

### R7 — Page-turn boundary can blank the highlight entirely

`seedPacerForCurrentPage()` (`MushafViewModel.kt:752-763`) only reseeds the pacer's word list if
the new page's words are already in state; if the page hasn't loaded yet, it returns without
touching the pacer at all — it does **not** clear it. Trace what happens when recitation crosses
a page boundary:

1. A chunk arrives whose `cursor` lands on the next page.
2. `mergeChunk` calls `pacer.confirm(cursor.wordId)` (`MushafViewModel.kt:715`) against the
   pacer's **current (old-page) word list** — `words.indexOf(wordId)` returns `-1`, so `confirm`
   silently no-ops (`RecitationPacer.kt:62-68`). The pacer index is left wherever it was.
3. `advancePageIfRecitationMovedOn` (`MushafViewModel.kt:734-749`) detects the cross-page cursor
   and calls `loadPage(nextPage)`.
4. `loadPage` (`MushafViewModel.kt:365-399`) updates `currentPage` and — because a live session
   is active — calls `seedPacerForCurrentPage()` immediately. **If the next page's data hasn't
   finished loading yet** (Room fetch + font shaping still in flight via `requestPage`), that
   reseed is a no-op, per the note above.
5. Until `requestPage`'s async callback lands and reseeds
   (`MushafViewModel.kt:421-423`), the pacer still holds the **old page's** word IDs, while the
   UI is already showing the new page. `highlightedWordId` therefore names a word that doesn't
   exist among the new page's tokens, so `MushafPageView`'s `token.wordId == highlighted` check
   never matches — **the cursor disappears entirely** for however long that page load takes.

Separately, `loadPage`/`requestPage` also fire a `RecitationControl.Seek` back to the server on
every automatic page-follow (`seekLiveCorrection`, called at `MushafViewModel.kt:392` and
`:427`). That's an extra network round trip layered on top of an already-async page load, worth
double-checking it can't race the next chunk that's already correctly in flight.

### R8 — Onset latency stacks with the cold-start guess

`SpeechGate` requires `onsetFrames = 2` consecutive above-threshold frames (~200 ms) before it
opens and starts passing audio at all (`SpeechGate.kt:118-134`); the pacer only advances on
frames marked `isSpeaking`. So there's an inherent ~200 ms+ floor between the reciter starting a
word and the earliest frame the pacer could react to. Small on its own, but it stacks with R3's
500 ms cold-start guess right at the point where a session (or a page) begins — exactly where a
first impression of "is this in sync?" gets formed.

### R9 — `highlightedWordId` is a single field written by four unrelated call sites

`MushafUiState.highlightedWordId` is written by: the pacer's live estimate and the
chunk-confirm snap (`MushafViewModel.mergeChunk`/`updateMicLevel`), page-load resets
(`loadPage`), and the MUALLEM/LISTEN highlight drivers (`updateWordHighlight`) — the last of
these gated by mode but sharing the same field. Not a bug in itself (mode checks currently keep
these from clobbering each other), but it means "the highlight" carries no signal about *which*
mechanism produced the current value — no confidence flag exists in state today. That absence is
what makes R6's "no visual distinction" hard to fix without a small state shape change first.

---

## 3. What is *not* the problem (verified, to rule out common suspects)

- **0-based/1-based word-index off-by-one** (`06-taahud-engineering.md` contract fact #5): correctly
  handled — `RecitationCursor.wordId` emits `wordIndex + 1`, `fromWordId` parses back with
  `oneBasedWord - 1` (`RecitationCursor.kt:16`, `:33`). Not a source of this bug.
- **Mistake/hint marks (colored underline):** index-addressed from graded server data, not
  pacer-derived. Accurate by construction (see §0).
- **`almost`/`trimmed` rendering invariants:** enforced elsewhere (`RecitationWordFeedback.mark`)
  and unrelated to the live cursor.

---

## 4. Edge-case catalogue

| # | Scenario | Expected | Actual | Root cause |
|---|---|---|---|---|
| 1 | Long madd/elongated word (e.g. "الرَّحْمَٰنِ") | Cursor stays on that word for its full duration | Cursor jumps 1–3 words ahead mid-word | R1, R3 |
| 2 | Run of short words spoken quickly | Cursor keeps pace | Cursor lags, visibly behind by the time the next chunk confirms | R1 |
| 3 | First words of a fresh session or a fresh chunk | Cursor tracks from the first word | Cursor runs on the 500 ms default guess, likely too fast for a deliberate/slow reciter | R2, R3, R8 |
| 4 | Network drop mid-session → auto-reconnect | Learned pace persists across the reconnect | `pacer.reset()` wipes it; resumes at the 500 ms default | R2 |
| 5 | Reciter toggles حفظ/تجويد mid-session | Cursor continues smoothly | Session restarts (`SessionRestart.SETTINGS`), pace resets | R2 |
| 6 | A long, pause-free verse (few/no waqf breaks) | Cursor stays close to true position, capped early if wrong | Can run up to 20 words ahead before the lookahead cap engages | R6 |
| 7 | Any waqf pause / chunk confirmation | Cursor eases toward the confirmed word | Cursor **snaps** — a visible jump, forward or backward | R5 |
| 8 | Recitation crosses a page boundary while the next page is still loading | Cursor continues on the new page's first word (or freezes gracefully) | Cursor **disappears** (points at an old-page word ID absent from the new page) until the load completes | R7 |
| 9 | Recitation jumps further than the ±2-page prefetch window (e.g. picking a distant ambiguous candidate) | Cursor relocates once the destination loads | Likely a longer/more visible version of #8 — destination page isn't even being prefetched | R7 (extended) |
| 10 | A natural mid-ayah breath pause shorter than the 600 ms hangover | Cursor freezes during the pause, resumes correctly after | **Works correctly** — no frames are marked speaking, so `onSpeechFrame()` isn't called; this is one of the few paths that behaves as intended | — |
| 11 | Sustained background noise (TV, traffic) crosses the adaptive noise floor | Cursor stays put; nothing was recited | Gate can open on false "speech," and the cursor advances with no actual recitation happening | R4 (VAD false positive, not just short-read) |
| 12 | Whispered/quiet recitation near the absolute floor | Cursor advances smoothly | `isSpeechFrame` can flicker true/false frame-to-frame, so `framesIntoWord` accumulates erratically (stop-start advancing) | R4 |
| 13 | Deliberately slow/tarteel recitation (memorization practice — plausibly the majority use case) | Cursor matches the slow pace after brief learning | Every chunk before pace convergence has the cursor racing ahead; converges slowly, resets on any restart (see #4, #5) | R2, R3 |
| 14 | Chunk confirms a cursor *behind* a word the estimate already (wrongly) marked current | No visual confusion | The translucent "current" box can render on a word that also carries a mistake/hint underline from a prior chunk, since the two layers don't coordinate | R5, R9 |

---

## 5. Severity ranking (for prioritizing a fix)

1. **R5 (visible snap on every chunk)** — happens on *every* waqf pause, i.e. constantly,
   for every user, every session. Highest frequency, highest visibility.
2. **R1/R3 (duration-blind, wrong default pace for slow reciters)** — the target audience for a
   correction tool skews toward slow/deliberate recitation, which is the worst case for the
   current model. Affects most of every session, not just edge moments.
3. **R7 (highlight vanishes on page turn)** — lower frequency (once per page) but a total,
   confusing failure (no highlight at all) rather than an imprecise one.
4. **R2 (pace reset on reconnect/settings change)** — lower frequency, but actively destroys
   learning the app had already done, making sessions worse the longer they run whenever a
   restart occurs.
5. **R6/R9 (no confidence signal, generous lookahead cap)** — amplifies how *bad* the other
   issues look, without being a bug per se; likely the highest-leverage, lowest-risk fix
   (rendering change + tightening a config constant) if a quick perceptual improvement is wanted
   before deeper estimator work.
6. **R4 (short-read frame drift), R8 (onset latency)** — real, but smaller in magnitude than the
   above; worth fixing but not where the main complaint is coming from.

---

## 6. The hard ceiling

None of the above can produce a genuinely word-accurate live highlight on its own, because the
backend contract (`06-taahud-engineering.md` §3, fact #3) does not currently emit any per-word
real-time signal — only a coarse grade after each waqf pause. A materially better fix (true
forced-alignment-driven highlighting) would need either a backend change (streaming partials) or
an on-device lightweight ASR pass purely for cursor-following, which `AUD-04` (hybrid ASR) in the
product brief gestures at but which is not built (`06-taahud-live-correction.md` — "local VAD +
light ASR on-device" is the stated design, but only the VAD half exists today). Everything in
§2 is about tightening the estimate and being honest about its uncertainty within that ceiling —
not about eliminating the ceiling itself.
