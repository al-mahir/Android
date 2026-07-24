# Ta'ahud — client-side (on-device) recitation tracking: analysis & implementation plan

**Goal:** stop the live reading-cursor from depending on the AI service's grading cadence
(~300 ms+ waqf pauses, plus network RTT). Track the reciter locally — on-device speech-to-text
+ fuzzy text matching against the known page — the way the iOS build already does
(`helper_swift_code/SpeechRecognizer.swift` + `ArabicPhoneticMatcher.swift`).

Read first: [06-taahud-live-highlight-sync-analysis.md](06-taahud-live-highlight-sync-analysis.md)
— this plan assumes that analysis and reuses its vocabulary (§0's "two highlight layers,"
`RecitationPacer`, `mergeChunk`, etc.). Also read
[06-taahud-engineering.md](06-taahud-engineering.md) for the contract and invariants this must
not violate.

---

## 1. What the iOS code actually does, and why it's the right general direction

`SpeechRecognizer.swift` runs Apple's on-device `SFSpeechRecognizer` continuously, takes the
**last word of each partial transcription**, debounces it 0.45 s until it stops changing (or the
result is marked final), and hands that settled word to a callback.
`ArabicPhoneticMatcher.swift` then decides whether that spoken word matches the *expected* word
via a normalize → exact → dictionary → affix-strip → phonetic-group-substitution →
Levenshtein → soundex cascade.

This is a genuine, established pattern, not a hack:

- **Tarteel AI** (the most-used Quran memorization app with live tracking) explicitly documents
  that its live word-matching **avoids acoustic alignment methods like Dynamic Time Warping and
  performs text-based fuzzy matching on ASR transcriptions instead, for efficiency and
  robustness** — the same shape as the iOS code (STT → text → fuzzy match), not per-frame audio
  alignment.
- Their own writeup states generic speech models perform far worse on Quranic recitation audio
  than a model tuned for it — relevant below (§3).
- Standard Arabic-search normalization (diacritic stripping, alif/hamza unification, alif-maqsura
  → yaa, tatweel removal) is exactly what `ArabicPhoneticMatcher.normalizeArabic` does — this is
  the field-standard approach, not something invented for this app.

**So: porting the *shape* of this (on-device STT → normalize → fuzzy-match → advance cursor
locally) is the right call, and directly answers the "without depending on the server" and
"start in the middle of the page" asks.** The rest of this doc is about what has to change to
make it actually work on this app's data and on Android, because a line-for-line port would
silently fail or scale badly.

Sources:
[Real-Time-Quran-recitation-tracker-System](https://github.com/yayaiu6/Real-Time-Quran-recitation-tracker-System) ·
[Tarteel: automatic recitation & memorization](https://medium.com/tarteel/introducing-automatic-recitation-and-memorization-on-tarteel-41f0e6927ea1) ·
[Tarteel Whisper Quran model](https://koshurai.medium.com/transcribing-quran-recitation-using-ai-step-by-step-guide-using-tarteel-whisper-model-f1b5fb216ce2) ·
[QCRI Arabic Normalizer](https://alt.qcri.org/tools/arabic-normalizer/) ·
[Quran Search Engine normalization guide](https://adelpro-quran-search-engine.mintlify.app/guides/normalization)

---

## 2. Four gaps that stop this from being a direct port

### Gap A — There is no plain Arabic text to match against in this app yet

This is the biggest, most concrete blocker, found by reading the actual data layer:

`MushafWordEntity.glyphText` (`mushaf/data/.../db/MushafLineEntity.kt:18`) stores **font glyph
codepoints**, not letter-by-letter Unicode Arabic. A real row from the test fixtures
(`MushafMapperTest.kt:54`) is `glyphText = "ﱂ"` — a single Arabic Presentation Forms ligature
codepoint from the QCF-style Mushaf font used for rendering. You cannot normalize or
Levenshtein-compare that against ASR output; it isn't decomposable into ح/م/د the way a phonetic
matcher needs.

The app's only source of plain, searchable Uthmani text today is the **server's** `/search`
endpoint (`SearchApi.kt` → `text_uthmani` field) — a network call, which defeats the entire point
of going local.

**What's needed:** a local, plain-text-per-word corpus, offline, bundled with the app, keyed by
the *same* `wordKey` format already used everywhere (`"sura:aya:word"` — confirmed at
`MushafAssetDataSource.kt:157`, and it already matches `RecitationCursor.wordId`
(`RecitationCursor.kt:16`) exactly). This is a data task, not a matching-algorithm task, and it
has to happen before anything else here can work.

**Which text variant, specifically:** Tanzil (already this app's text source, per
`06-taahud-engineering.md` §7.1's attribution requirement) publishes several encodings of the
same corpus — Uthmani (full Quranic Rasm + tashkeel), Uthmani-minimal, Simple, and
Simple-clean (modern spelling, no diacritics). **Use simple-clean, not Uthmani, as the matching
corpus.** Reasoning in Gap B.

### Gap B — Quranic Rasm (Uthmani spelling) and ASR output don't speak the same spelling convention

This is the specific concern raised — "أحمد vs احمد must be the same" — and it goes deeper than
hamza normalization. Quranic Uthmani orthography (Rasm) differs from modern Arabic spelling in
systematic ways beyond diacritics: alifs that are silent/omitted in the Rasm but present in
modern spelling (and vice versa), dagger alifs standing in for a full alif, etc. An on-device (or
server) ASR model is trained on **modern spelling conventions** — it will never emit Uthmani Rasm.
So even a perfect normalizer that only strips diacritics and unifies hamza forms will still
mismatch on Rasm-vs-modern spelling differences that have nothing to do with pronunciation.

**Implication:** the expected-word side of every comparison must come from the **simple-clean**
text variant (modern spelling, already diacritic-free), never from the Uthmani text used for
rendering/grading. Normalize *that* the same way the STT output is normalized (Gap C), and the
"أحمد = احمد" class of problem mostly disappears at the source instead of needing to be patched
by the phonetic-group cascade.

### Gap C — The hand-curated misrecognition dictionary doesn't scale to 6,236 verses

`commonMisrecognitions` in the Swift code is a manually written dictionary, and it only covers
Al-Fātiḥa (~30 entries). The Qur'an has ~77,000 words. Hand-curating this for the whole Mushaf
isn't a realistic task, and a dictionary keyed by *exact expected word* doesn't generalize to
words it wasn't written for.

**Recommendation:** don't port the dictionary as load-bearing. Keep the parts of the cascade that
*are* general — normalization, prefix/suffix stripping, phonetic-group substitution, Levenshtein,
soundex — since those apply to any word without per-word authoring. If accuracy data later shows
specific, frequent ASR confusions worth hard-coding, mine them from real session logs (data-driven,
after the fact), not hand-guessed up front.

### Gap D — Android has no equivalent of iOS's on-device Arabic `SFSpeechRecognizer`, confirmed

This is the gap most likely to be underestimated. Options, as they actually stand:

| Engine | Offline? | Arabic? | Quran-tuned? | Integration cost |
|---|---|---|---|---|
| `android.speech.SpeechRecognizer` (built-in, cloud) | No — needs network | Yes, generically | No | Lowest — but re-adds the network dependency this whole effort exists to remove |
| `SpeechRecognizer.createOnDeviceSpeechRecognizer()` (API 33+) | Yes | **Unconfirmed** — Android's on-device model language coverage is narrow and not documented per-locale; must be spiked on real devices before relying on it | No | Low, if it works |
| ML Kit GenAI Speech Recognition (Gemini Nano / AICore) | Yes | Unclear, currently alpha | No | Not production-ready yet |
| VOSK | Yes, fully | Yes — ships an Arabic model | No — generic MSA/dialect model | Medium |
| **sherpa-onnx** (Kotlin bindings, runs Whisper/Zipformer/etc. on-device) | Yes, fully | Yes | **Yes — can load a Quran-tuned model**, e.g. community wav2vec2/Whisper models trained specifically on Quranic recitation | Medium-high (bundle a model asset, larger APK, own the inference loop) |

The generic-accuracy risk is not hypothetical — it's the exact problem Tarteel names explicitly:
generic ASR is measurably worse on Quranic recitation audio than a model trained for it (elongated
madd, tajwīd-specific pronunciation, Classical Arabic vs. everyday spoken Arabic). Using Android's
built-in recognizer (if it even supports on-device Arabic) risks building the whole pipeline on a
foundation that mistranscribes constantly, which would make matching *worse*, not better, than
today's server-only approach.

**Recommendation:** spike Android's built-in on-device recognizer for `ar` first (cheapest, ~1
day to know if it's viable at all on target devices), but budget for **sherpa-onnx + a
Quran/Arabic-tuned model** as the realistic fallback, because it is the only option in the table
that is simultaneously offline and domain-appropriate.

Sources:
[Android SpeechRecognizer reference](https://developer.android.com/reference/android/speech/SpeechRecognizer) ·
[Android Speech Recognition in 2026 guide](https://picovoice.ai/blog/android-speech-recognition/) ·
[VOSK](https://alphacephei.com/vosk/) ·
[sherpa-onnx Android docs](https://k2-fsa.github.io/sherpa/onnx/android/index.html) ·
[ML Kit GenAI Speech Recognition](https://developers.google.com/ml-kit/genai/speech-recognition/android)

---

## 3. A constraint the plan must respect: this augments the pacer, it does not replace grading

Per `06-taahud-engineering.md` §4 (the invariants), correctness (mistakes, tajwīd grading,
confidence) is the server's job and must stay that way — the local matcher is not a Quranic ASR
grader, it's a fuzzy text matcher with no acoustic-confidence model behind it. Treating a local
phonetic-match hit as "the reciter said this correctly" would be a serious regression against the
"almost is a hint, never a mistake" and "an unscored report never overwrites a scored one"
invariants.

**The local matcher's job is exactly what `RecitationPacer` already owns: position, never
correctness** (mirroring the existing invariant "the pacer's estimate is position, never
correctness," `06-taahud-engineering.md` §4). Concretely: it becomes a better **estimator inside
the same role**, replacing the blind 100 ms-frame timer with a text-anchored estimate, while
`RecitationChunk.cursor` from the server remains the authoritative resync point exactly as it is
today (`MushafViewModel.mergeChunk`, `pacer.confirm(...)`).

This also directly answers the "without depending on the server / connection problems" framing:
the *live-follow highlight* becomes usable even when offline or during a reconnect (§7.3 of the
engineering doc lists offline handling as a known gap) — but grading/mistakes still require the
server, and the UI must not blur that distinction (see §6 below).

---

## 4. Architecture — where this lives, per AGENTS.md boundaries

```
:mushaf:domain   (pure Kotlin, unit-tested)
  model/recite/local/ArabicTextNormalizer.kt      — port of normalizeArabic()
  model/recite/local/ArabicPhoneticMatcher.kt     — port of isPhoneticMatch(), minus the dictionary
  model/recite/local/LocalRecitationCursor.kt      — windowed matcher: given a settled spoken
                                                       word + the current window of expected
                                                       words, returns the best matching index
                                                       (nearest-to-current-position wins ties)
  repository/LocalWordCorpusRepository.kt          — contract: plain text lookup by wordId,
                                                       windowed lookahead from a cursor
  repository/LocalSpeechRecognizer.kt              — contract: settled-word stream from raw audio

:mushaf:data     (platform/IO)
  recite/local/corpus/LocalWordCorpusRepositoryImpl.kt   — reads the bundled simple-clean asset
  recite/local/asr/<engine>SpeechRecognizerImpl.kt        — wraps whichever engine wins the spike

:mushaf:presentation
  MushafViewModel — wires LocalRecitationCursor in alongside RecitationPacer (see §5)
```

Nothing here crosses into `:presentation` (Profile) or exposes a platform/engine type outside
`:mushaf:data`, consistent with AGENTS.md's module rules and the existing `LiveRecitationRepository`
pattern.

---

## 5. Sequenced implementation plan

### Phase 0 — Local plain-text corpus (data only, no behavior change, fully offline-testable)

1. Source a **simple-clean** Arabic text export (Tanzil-licensed, matching the corpus this app
   already attributes) keyed by `sura:aya:word`.
2. Bundle it as a local asset (mirrors how `MushafAssetDataSource` already ships the glyph/layout
   DB) — a lightweight table `word_key -> plain_text`, one row per word, same cardinality as
   `MushafWordEntity`.
3. `LocalWordCorpusRepositoryImpl` exposes: `plainTextFor(wordId): String?` and
   `windowFrom(cursor: RecitationCursor, size: Int): List<Pair<wordId, plainText>>` (walks
   forward across ayah/page/sūrah boundaries the same way `wordsForCurrentPage()` +
   `advancePageIfRecitationMovedOn` already do — reuse, don't re-derive, the page-boundary
   handling that exists today).
4. Verify 1:1 coverage: every `wordKey` in the existing glyph table has a corresponding
   simple-clean entry. This is a pure data-integrity check, cheap to automate, and it will catch
   corpus mismatches before they ever reach the matcher.

### Phase 1 — Port the normalizer + matcher (pure Kotlin, no ASR yet)

1. `ArabicTextNormalizer.normalize(text: String): String` — same rule set as the Swift map
   (hamza/alif unification, tā-marbūṭa, alif-maqṣūra, tatweel, diacritics, waṣla), extended per
   the QCRI/Quran-search-engine references in §1 to also fold Unicode NFC/NFKC composed vs.
   decomposed hamza forms (a real-world Android input-method variance the iOS version doesn't
   need to worry about, since iOS text tends to arrive pre-composed).
2. `ArabicPhoneticMatcher.isMatch(spoken: String, expected: String): MatchConfidence` — port the
   exact-match → affix-strip → phonetic-group-substitution → Levenshtein → soundex cascade.
   **Drop the hardcoded dictionary** (Gap C) — start without it, add data-driven entries only if
   evaluation (Phase 7) shows a specific recurring confusion worth encoding.
3. Unit tests: mirror every case class the Swift file demonstrates (hamza forms, tā-marbūṭa,
   prefix/suffix stripping, phonetic substitution, partial-word, Levenshtein threshold) plus the
   Quran-specific case that motivated this doc: `"احمد"` vs `"أحمد"` must match after
   normalization alone, with no fallback to fuzzy matching required.
4. This phase is fully testable without any device, microphone, or ASR engine — pure text-pair
   unit tests. Good place to stop and validate before spending budget on Phase 2's spike.

### Phase 2 — On-device ASR spike (time-boxed, decision point)

1. Spike Android's built-in `SpeechRecognizer` / `createOnDeviceSpeechRecognizer()` with an `ar`
   locale against a handful of recorded Quranic recitation WAVs — reuse the existing debug WAV
   infrastructure already in the repo (`WavDebugSink`, `WavPcmRecorder`,
   `06-taahud-engineering.md` §8.2's WAV-pull recipe) so this is reproducible and doesn't require
   live mic testing every iteration.
2. Score transcription accuracy against the Phase 0 simple-clean corpus directly (this is now
   possible precisely because Phase 0 exists).
3. **Decision gate:** if built-in accuracy is unusable (the expected outcome, per Tarteel's own
   findings on generic ASR vs. Quranic audio), move to sherpa-onnx with a Quran/Arabic-tuned
   model; budget for the larger APK asset and the inference-loop integration work up front rather
   than discovering it mid-implementation.
4. This decision should be made with the user/product owner before committing engineering time —
   it trades APK size and a new native dependency against transcription accuracy, and that's a
   product call, not just a technical one.

### Phase 3 — Feed the existing audio stream into the local recognizer, don't open a second mic session

`AudioRecordPcmRecorder` already owns the single `AudioRecord` session
(`AudioRecordPcmRecorder.kt:32-38`, `VOICE_RECOGNITION` source). Android does not reliably support
two concurrent exclusive recognition-mode `AudioRecord` sessions across OEMs. **Tap the same
`AudioFrame` flow** that already passes through `SpeechGate`/`RecitationCaptureRepositoryImpl`
(`mushaf/data/.../RecitationCaptureRepositoryImpl.kt:51-64`) and mirror it into the chosen local
ASR engine's input buffer, rather than standing up an independent capture path. This keeps mic
ownership, permission handling, and the `finally`-block release guarantee
(`06-taahud-engineering.md` file index note on `AudioRecordPcmRecorder`) exactly as they are today.

### Phase 4 — Windowed local matching, anchored to the current cursor

This is what directly answers "what if the user starts in the middle of the page":

1. On session/page seed (`seedPacerForCurrentPage`, `MushafViewModel.kt:752-763`), also seed
   `LocalRecitationCursor` with a bounded lookahead window (e.g. next 10–15 words) starting from
   whatever `lastCursor`/`startCursorForCurrentPage()` resolves to — **never** assume "start of
   page." This reuses the exact anchor the pacer already uses today, so mid-page starts,
   mid-ayah resumes, and seek/candidate-selection (`selectCandidate`,
   `MushafViewModel.kt:765-768`) all carry over for free.
2. On each settled STT word (debounce pattern from the Swift code — last word unchanged for
   ~400-500 ms, or marked final), run `ArabicPhoneticMatcher.isMatch` against the window,
   **biased toward the position nearest the current predicted cursor** — this is what correctly
   resolves common words that repeat within a window (e.g. "من", "في") instead of matching the
   first occurrence anywhere in the Qur'an. Advance the local cursor to the matched index; slide
   the window forward.
3. No match found in the window within a small tolerance → don't move the cursor and don't
   error; just wait for the next settled word or the next server chunk to resync. A wrong local
   guess is worse than a stale one, since resync is happening on that cadence anyway (Phase 5).

### Phase 5 — Reconcile with `RecitationPacer` / `MushafViewModel`

1. `LocalRecitationCursor`'s output becomes the primary driver of `highlightedWordId` (replacing
   `pacer.onSpeechFrame()`'s blind timing advance in `updateMicLevel`,
   `MushafViewModel.kt:675-698`).
2. Keep `mergeChunk`'s `pacer.confirm(cursor.wordId)` exactly as today — it remains the
   authoritative resync from the server. With a working local matcher, disagreements between the
   local estimate and the server's confirmed cursor should become rare; **log/count them**
   (cheaply, locally) as the accuracy signal for whether Phase 2's engine choice is good enough,
   without sending anything extra to the server.
3. Persistent disagreement (the local cursor and the confirmed cursor keep landing on different
   words) is a signal to widen the local search window or fall back to the timing estimate for
   that stretch, not to keep trusting a matcher that's clearly lost the reciter.

### Phase 6 — UX: say when a position is estimated vs. confirmed

Directly extends R6/R9 from the sync-analysis doc, and becomes more important here because a
phonetic fuzzy-matcher *will* sometimes be confidently wrong (homophone collision within the
window, a misheard word). Render the moving cursor with a visibly different treatment
(e.g. dashed outline, or held at lower opacity) while it's local-estimate-only, switching to the
current solid highlight only once the server's chunk has actually confirmed that word. This keeps
faith with the product's stated A11Y-01 rule (status must be felt as well as seen, never color
alone) — the confirmed/estimated distinction should get the same treatment, not just color.

### Phase 7 — Fallback, resilience, and evaluation

1. If the chosen local ASR engine fails to initialize (missing on-device model, unsupported
   device, permission issue), fall back silently to today's timing-based `RecitationPacer`
   behavior — this feature must never be a hard dependency for recitation mode to work at all.
2. Automated evaluation harness: run the Phase 0 corpus + Phase 1 matcher + Phase 2's chosen
   engine against a small library of known recorded recitations (reuse the debug-WAV
   infrastructure again) to track word-match accuracy over time as a real regression-testable
   metric, the same way `RecitationFeedbackMapperTest` pins the server-side contract today.
3. On-device manual verification across a few Android versions/OEMs is required regardless of
   engine choice — on-device ASR availability and quality is known to be fragmented across
   manufacturers in a way iOS's single-vendor Speech framework is not.

---

## 6. Open decisions that need a product/eng call before implementation starts

1. **Which ASR engine** (Phase 2's decision gate) — built-in Android recognizer vs. sherpa-onnx +
   a bundled Quran-tuned model. Materially different cost (APK size, native dependency,
   engineering time) for materially different accuracy. Recommend running the Phase 2 spike
   before committing.
2. **Whether local, offline live-follow is worth shipping *before* offline grading exists** — this
   plan makes the moving highlight resilient to connectivity, but mistake grading still requires
   the server per the existing architecture (§3). Worth confirming that's an acceptable half-step
   for the product, since a reciter offline will see themselves tracked but get no
   mistake/tajwīd feedback until reconnected.
3. **Corpus licensing** — confirm the simple-clean Tanzil variant can be bundled locally under the
   same terms already covering the Uthmani text and glyphs (per the existing Tanzil/KFGQPC
   attribution obligations, `06-taahud-engineering.md` §7.1 item 4).
