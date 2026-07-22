# Android: model configuration and recitation settings

Everything a user can change about how the model listens and grades, and how to build
that settings UI in an Android app.

[MOBILE_INTEGRATION.md](MOBILE_INTEGRATION.md) covers connecting and streaming audio.
[API.md](API.md) is the raw protocol. This document is about the **settings layer**: the
four independent things a reciter can tune, what each actually does to the output, and
the traps that make a setting silently do nothing.

Every value in this document was read from a live server, not from source.

Contents:

1. [The four layers](#1-the-four-layers)
2. [The one rule: never hardcode the schema](#2-the-one-rule-never-hardcode-the-schema)
3. [Layer 1 — engine (which model listens)](#3-layer-1--engine-which-model-listens)
4. [Layer 2 — moshaf (how the reciter recites)](#4-layer-2--moshaf-how-the-reciter-recites)
5. [Layer 3 — strictness (how sure before accusing)](#5-layer-3--strictness-how-sure-before-accusing)
6. [Layer 4 — rules (what gets graded at all)](#6-layer-4--rules-what-gets-graded-at-all)
7. [How settings reach the model](#7-how-settings-reach-the-model)
8. [Android implementation](#8-android-implementation)
9. [The four ways a setting silently does nothing](#9-the-four-ways-a-setting-silently-does-nothing)
10. [Testing that a setting actually changed the output](#10-testing-that-a-setting-actually-changed-the-output)
11. [Full reference: the 37 moshaf fields](#11-full-reference-the-37-moshaf-fields)
12. [Full reference: the 18 gradeable rules](#12-full-reference-the-18-gradeable-rules)

---

## 1. The four layers

They are independent and compose. Each answers a different question:

| Layer | Question | Field | Values |
|---|---|---|---|
| **Engine** | Which acoustic model listens? | `engine` | from `/health` |
| **Moshaf** | How does this reciter recite? | `moshaf` | 37 fields, from `/moshaf-schema` |
| **Strictness** | How sure must the model be before calling it a mistake? | `strictness` | `lenient` / `normal` / `strict` |
| **Rules** | Which rules are enforced at all? | `rules` | 18 keys, from `/tajweed-rules` |

A useful way to hold them apart:

- **Moshaf changes what is *correct*.** Holding a madd for 4 counts is right or wrong
  depending on the reciter's style. This is the only layer that changes the *reference*.
- **Rules change what is *checked*.** A filtered rule leaves no red word behind it.
- **Strictness changes how *confidently* a finding is reported** — `error` versus the
  softer `almost`. It never changes what counts as a mistake.
- **Engine changes what is *detectable*.** Zipformer cannot report ṣifāt at all.

> The reference web frontend exposes **engine, moshaf and rules** but **not strictness**.
> If you add a strictness control, your Android app is offering something the web app
> does not — which is fine, but means you cannot copy its UI wholesale.

---

## 2. The one rule: never hardcode the schema

Fetch `/moshaf-schema` and `/tajweed-rules` at runtime and build the UI from the
response. Do not paste the tables in sections 11 and 12 into your app as constants.

They are in this document so you can *design* against them — see how many fields there
are, how wide the options get, what the panel has to accommodate. They are a snapshot,
not a contract.

The reference frontend does exactly this, and says why:

> *"The schema is the backend's — introspected from `MoshafAttributes` — so this file
> never hard-codes the fields; it only fetches them and remembers the reciter's
> choices."* — `frontend/src/lib/moshaf.ts`

The rules catalogue matters even more, because it is derived from the rule classes the
grader actually recognises, so *a chip in your UI can never name a rule nothing
enforces*. Hardcode it and the first backend update gives your users switches that do
nothing.

Cache both for the app session. They do not change while the server runs.

---

## 3. Layer 1 — engine (which model listens)

### What exists

The backend can build three engines. Only two are ever a user-facing choice:

| Engine | Show to users? | Hardware | What it gives |
|---|---|---|---|
| `real` (Muaalem) | **Yes** | NVIDIA GPU | Phonemes, per-character confidence, all 10 ṣifāt, full tajwīd grading, the `almost` softening |
| `zipformer` | **Yes** | CPU only | Phonemes, word-level correct/error. **No ṣifāt, no confidence, no `almost`** |
| `mock` | **No** | Anything | Fabricated output for GPU-less development. Never a user choice |

Never show the raw wire name. The reference frontend labels `real` as **المُعلِّم** with
the hint *"تحليل تجويد كامل، مع درجة ثقة لكل حرف"*, and `zipformer` as **Zipformer** with
*"أخفّ وأسرع، بدون تقييم تجويد"*.

### Consequences of picking zipformer

This is not merely "faster". Three things disappear:

1. **All ṣifāt grading.** Every `sifa` finding is gone — 10 of the 18 gradeable rules.
2. **Per-character confidence.** With no confidence there is no `almost`, so
   **strictness becomes meaningless**. Grey out that control when zipformer is selected.
3. **Tajwīd grading** in the full sense.

If your UI lets someone pick zipformer *and* set strictness *and* tick ṣifāt rules, you
are offering three controls where two do nothing. Disable them, with a reason.

### Discovering what is available

`GET /health` returns `available_engines`. **Offer only what is in that list.**

```json
{"status":"healthy","engine":"real","available_engines":["real"],
 "device":"cuda","dtype":"bfloat16", "...": "..."}
```

This server reports `["real"]` only — zipformer's gated model is not installed, and
`mock` is not built when a GPU is present. A CPU deployment reports something different.
Never assume a fixed list.

**Requesting an engine that does not exist is not an error.** The server silently uses
its default. The only way to know is to compare the session ack's `engine` to what you
asked for:

```kotlin
// in onMessage, on the "session" event
val actual = msg.optString("engine")
if (requested != null && actual != requested) {
    // Tell the user, or at least don't keep claiming they're on `requested`.
}
```

---

## 4. Layer 2 — moshaf (how the reciter recites)

37 fields describing the reciter's tajwīd style — madd lengths, sakt positions, specific
disputed words. This is the **only layer that changes what counts as correct**, because
the reference phonemes are generated from it.

Most fields are single-word rulings on individual āyāt (`raa_misr` — whether the rāʾ in
{مصر} is heavy or light when pausing). A handful drive most day-to-day grading:

| Field | Why it matters | Options | Schema default |
|---|---|---|---|
| `madd_monfasel_len` | Separated madd length | 2, 3, 4, 5 | `2` |
| `madd_mottasel_len` | Connected madd length | 4, 5, 6 | `4` |
| `madd_mottasel_waqf` | Connected madd when pausing | 4, 5, 6 | `4` |
| `madd_aared_len` | Madd before a pause-sukūn | 2, 4, 6 | `2` |
| `madd_alleen_len` | Madd al-leen | 2, 4, 6 | `2` |
| `recitation_speed` | Murattal / mujawwad / ḥadr | 4 options | `murattal` |

Note the ranges are **not uniform**: `madd_monfasel_len` accepts 2–5, `madd_mottasel_len`
accepts 4–6. There is no single "madd length" slider you can build. This is exactly why
section 2 says to render from the schema.

### ⚠ The defaults trap

**The schema's `default` is not the server's effective default.** Verified on this
server — three fields disagree:

| Field | `/moshaf-schema` says | Server actually uses when `moshaf` is omitted |
|---|---|---|
| `madd_monfasel_len` | **2** | **4** |
| `madd_aared_len` | **2** | **4** |
| `madd_alleen_len` | **2** | **4** |

The schema reports the `MoshafAttributes` class defaults; the session uses
`Settings.madd_*`, which the deployment overrides via `TAJWID_MADD_*`.

Two real consequences:

1. **A settings panel seeded from `field.default` misreports the current state.** It
   shows "2" for monfasel while a session that sends no moshaf is grading against 4.
2. **Merely opening the settings sheet and saving changes grading**, without the user
   knowingly changing anything — because saving sends the full config, now including
   `madd_monfasel_len: 2`.

The reference frontend's `defaultConfig()` builds from `field.default` and has this
behaviour.

**Recommended handling**: treat "the user has never opened settings" as *do not send a
`moshaf` object at all*, so the server's own defaults apply. Only send a moshaf once the
user has explicitly changed something. If you must show values before then, be aware the
three fields above will read low. Ask the backend team to reconcile them — it is a
one-line settings change on their side and worth requesting rather than working around
forever.

### Partial configs are allowed and preferred

You do **not** have to send all 37 fields. The server layers your dict over its resolved
default:

```json
{"moshaf": {"madd_monfasel_len": 4}}
```

Everything else stays at the server default. Sending only what the user changed is
smaller, more robust, and sidesteps the defaults trap for every field they never touched.

---

## 5. Layer 3 — strictness (how sure before accusing)

Three levels. **It does not change what counts as a mistake** — the diff finds exactly
the same errors at every level. It sets the model-confidence threshold at which a finding
is reported as a hard `error` rather than softened to `almost`.

**A lower threshold is a harsher teacher.**

| Level | Phoneme threshold | Ṣifa threshold |
|---|---|---|
| `lenient` | 0.90 | 0.95 |
| `normal` *(default)* | 0.70 | 0.85 |
| `strict` | 0.50 | 0.65 |

Two thresholds because the probabilities come from different heads of the model and are
not assumed to share a calibration.

Worked example: a finding the model is **0.75** sure of grades `almost` on `lenient` and
`error` on both `normal` and `strict`. At **0.55** it is `error` only on `strict`.

A finding with **unknown** confidence (`confidence: null` — a pure deletion, where the
reciter said nothing and there is no probability to read) grades `almost` at **every**
level, `strict` included. No setting turns a guess into an accusation.

### Two hard constraints on your UI

**Send lowercase.** `"Normal"` with a capital N is not recognised and silently falls back
to the default. If your UI stores a display label, map it — do not send it.

**Do not present these as accuracy levels.** API.md marks these thresholds as
*uncalibrated placeholders* — hand-picked guesses awaiting calibration against a labelled
set. Treat the **ordering** as meaningful and the absolute numbers as provisional. Label
the control something like "حساسية التصحيح" (correction sensitivity), never "دقة 90%".

**Disable it entirely on zipformer**, which produces no confidence for it to threshold.

---

## 6. Layer 4 — rules (what gets graded at all)

18 gradeable items: 8 tajwīd rules and 10 ṣifāt. A learner drilling madd al-aared does
not want to be corrected on qalqalah.

Findings for unselected rules are dropped **before** they can mark a word, so `status`
and `errors[]` stay consistent — a filtered rule leaves no red word behind it.

### `null` and `[]` are different, and both are real

This is the single most important thing about this layer:

| Value | Meaning |
|---|---|
| omitted / `null` | **Grade everything.** The default. |
| `["aared_madd", "ghonna"]` | Grade these; stay silent on every other tajwīd rule. |
| `[]` | **A real choice**: no tajwīd rule at all — hifz and tashkeel only. |

A truthiness test (`if (rules)`) collapses `[]` into `null` and silently grades
everything for a user who explicitly asked for nothing. In Kotlin, model this as
`List<String>?` and check `!= null` explicitly, never `isNullOrEmpty()`.

The reference frontend guards this at the storage layer too — a corrupted entry that
deserialises to a non-array is treated as `null` rather than sent as-is, because sending
a malformed value would match nothing and silently mute every rule the user picked.

### What is never filtered

**Hifz and tashkeel are always reported**, whatever the selection:

- a wrong or missing word (`error_type: "normal"`)
- a wrong haraka (`error_type: "tashkeel"`)

Leniency narrows which *tajwīd rules* are enforced, not whether the recitation is checked
at all. A `tajweed` finding carrying an empty `tajweed_rules[]` — the reciter skipped a
rule-bearing letter outright — is also never filtered, since there is no rule to match it
against and hiding it would bury a real miss.

Unknown keys are not an error; they simply match nothing. Sending only unknown keys
therefore grades no tajwīd rule at all, exactly as `[]` does — which is a good reason to
build the chips from `/tajweed-rules` rather than a hardcoded list that may drift.

### Grouping

Each rule has `kind`: `tajweed` or `sifa`. Group by it. And remember: **all 10 `sifa`
rules are inert on zipformer.**

Note ghunnah is `ghonna`, a **ṣifa** key, not a tajwīd rule — a common mistake when
building the panel by hand.

---

## 7. How settings reach the model

All four layers travel in the **start message**, and nowhere else:

```json
{"type": "start",
 "sura": 1, "aya": 1, "word_idx": 0,
 "engine": "real",
 "strictness": "normal",
 "moshaf": {"madd_monfasel_len": 4},
 "rules": ["aared_madd", "ghonna"]}
```

### Settings are per-session and immutable mid-session

The protocol has exactly three client messages: `start`, `seek`, and `end`. **There is no
message that updates settings on a live session.** `seek` moves the cursor only.

So changing any setting requires: `end` the session → apply the change → open a new
socket → `start` again with the new config, seeded at the last cursor.

Design for this. Either:

- **Close the settings sheet, then restart the session automatically** at the last known
  cursor. Smoothest, and what most users expect.
- Or disable the settings entry point while a session is live.

Silently accepting a change that will not apply until the next session is the option to
avoid — the user hears no difference and concludes the setting is broken.

### Persistence

Persist all four choices locally and reapply on the next session. The reference frontend
uses three `localStorage` keys (`tajwid.engine`, `tajwid.moshaf`, `tajwid.rules`); on
Android use `DataStore`.

Persist the **user's selection**, not the fetched schema. Schema and catalogue are server
truth and should be re-fetched; only the choices are yours to keep. Validate a restored
selection against the freshly fetched schema before using it, in case the server changed.

---

## 8. Android implementation

### Models

```kotlin
// --- fetched from the server, never hardcoded -------------------------------
data class MoshafOption(val value: Any, val label: String)

data class MoshafField(
    val key: String,
    val nameAr: String,
    val description: String?,
    val default: Any,
    val options: List<MoshafOption>,
)

data class TajweedRuleDef(
    val key: String,
    val nameAr: String,
    val nameEn: String,
    val kind: String,          // "tajweed" | "sifa"
)

data class Health(
    val engine: String,
    val availableEngines: List<String>,
    val device: String,
)

// --- the user's choices -----------------------------------------------------
enum class Strictness(val wire: String) {   // wire value is lowercase, always
    LENIENT("lenient"), NORMAL("normal"), STRICT("strict")
}

data class RecitationSettings(
    val engine: String? = null,                    // null = server default
    val strictness: Strictness = Strictness.NORMAL,
    /** Only what the user actually changed. Empty = send no `moshaf` at all. */
    val moshaf: Map<String, Any> = emptyMap(),
    /**
     * null = grade everything; emptyList() = hifz + tashkeel only.
     * These are DIFFERENT. Never collapse them with isNullOrEmpty().
     */
    val rules: List<String>? = null,
)
```

### Building the start message

```kotlin
fun RecitationSettings.toStartMessage(sura: Int, aya: Int, wordIdx: Int): JSONObject =
    JSONObject().apply {
        put("type", "start")
        put("sura", sura); put("aya", aya); put("word_idx", wordIdx)

        engine?.let { put("engine", it) }
        put("strictness", strictness.wire)

        // Omit entirely when untouched, so the SERVER's defaults apply rather than the
        // schema's — see section 4's defaults trap.
        if (moshaf.isNotEmpty()) {
            put("moshaf", JSONObject().apply { moshaf.forEach { (k, v) -> put(k, v) } })
        }

        // `null` and `[]` mean different things, so this tests for null explicitly.
        if (rules != null) put("rules", JSONArray(rules))
    }
```

### Persistence with DataStore

```kotlin
private val Context.settingsStore by preferencesDataStore("recitation_settings")

object SettingsKeys {
    val ENGINE     = stringPreferencesKey("engine")
    val STRICTNESS = stringPreferencesKey("strictness")
    val MOSHAF     = stringPreferencesKey("moshaf_json")
    val RULES      = stringPreferencesKey("rules_json")   // absent = null = grade all
}

suspend fun Context.saveSettings(s: RecitationSettings) {
    settingsStore.edit { p ->
        if (s.engine != null) p[SettingsKeys.ENGINE] = s.engine else p.remove(SettingsKeys.ENGINE)
        p[SettingsKeys.STRICTNESS] = s.strictness.wire
        p[SettingsKeys.MOSHAF] = JSONObject(s.moshaf).toString()
        // Absent key = null = grade everything. "[]" is a stored, meaningful value.
        if (s.rules != null) p[SettingsKeys.RULES] = JSONArray(s.rules).toString()
        else p.remove(SettingsKeys.RULES)
    }
}
```

Storing `rules` as an **absent key** for `null` and `"[]"` for the empty selection is
what keeps the distinction alive across process death. A nullable `Set<String>` in
`SharedPreferences` cannot express it.

### A schema-driven settings screen

Because every moshaf field is "a label plus 2–4 mutually exclusive options", one
composable renders all 37:

```kotlin
@Composable
fun MoshafSettings(
    fields: List<MoshafField>,
    selected: Map<String, Any>,
    serverDefaults: Map<String, Any>,      // what a session uses when you send nothing
    onChange: (String, Any) -> Unit,
) {
    LazyColumn {
        items(fields, key = { it.key }) { field ->
            // Prefer the user's pick; else what the SERVER would really use; else the
            // schema default as a last resort (see section 4 — these disagree on 3 fields).
            val current = selected[field.key]
                ?: serverDefaults[field.key]
                ?: field.default

            Column(Modifier.padding(vertical = 12.dp)) {
                Text(field.nameAr, style = MaterialTheme.typography.titleSmall)
                field.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                SingleChoiceSegmentedButtonRow(Modifier.padding(top = 8.dp)) {
                    field.options.forEachIndexed { i, opt ->
                        SegmentedButton(
                            selected = opt.value == current,
                            onClick = { onChange(field.key, opt.value) },
                            shape = SegmentedButtonDefaults.itemShape(i, field.options.size),
                        ) { Text(opt.label) }
                    }
                }
            }
        }
    }
}
```

Three Android-specific notes:

- **Set `layoutDirection = Rtl`.** Every label is Arabic.
- **Preserve option value types.** `madd_monfasel_len` is an `Int` on the wire, not
  `"4"`. `JSONObject.put("k", 4)` and `put("k", "4")` produce different JSON, and the
  string form is rejected — which discards the whole moshaf (section 9).
- The 37 fields are a long flat list. Consider a "common" section (the six in section 4)
  with the rest behind "خيارات متقدمة". The reference frontend renders them flat.

### Rule chips

```kotlin
@Composable
fun RuleSelector(
    catalogue: List<TajweedRuleDef>,
    selection: List<String>?,          // null = everything
    engine: String?,
    onSelectionChange: (List<String>?) -> Unit,
) {
    val sifaInert = engine == "zipformer"     // zipformer reports no sifat at all

    // "Grade everything" must be reachable, and is NOT the same as selecting all chips.
    FilterChip(
        selected = selection == null,
        onClick = { onSelectionChange(null) },
        label = { Text("كل الأحكام") },
    )

    catalogue.groupBy { it.kind }.forEach { (kind, rules) ->
        Text(if (kind == "tajweed") "أحكام التجويد" else "الصفات")
        FlowRow {
            rules.forEach { rule ->
                FilterChip(
                    selected = selection?.contains(rule.key) == true,
                    enabled = !(sifaInert && kind == "sifa"),
                    onClick = {
                        val cur = selection ?: emptyList()
                        onSelectionChange(
                            if (rule.key in cur) cur - rule.key else cur + rule.key
                        )
                    },
                    label = { Text(rule.nameAr) },
                )
            }
        }
    }
}
```

Note that deselecting the last chip yields `emptyList()`, **not** `null` — "grade
nothing" rather than "grade everything". That is correct and intentional. Give the user a
separate, explicit way back to `null`, as the "كل الأحكام" chip does above.

---

## 9. The four ways a setting silently does nothing

None of these produce an error. All of them look like your UI is broken.

**1. An out-of-range moshaf value discards the *entire* moshaf.**
Not just the bad field — the whole object, falling back to the server default. Setting
`madd_mottasel_len: 2` (legal range 4–6) also throws away the `madd_monfasel_len` the
user carefully set. Send values only from `field.options`, and preserve their types.

**2. A genuinely invalid *combination* does the same.**
Every value individually legal, still rejected. The constraint: madd al-leen may not
exceed madd al-aared. Verified on this server:

| Sent | Result |
|---|---|
| `{"madd_aared_len": 2}` | **discarded** → aared stays 4 (default leen of 4 would exceed 2) |
| `{"madd_aared_len": 2, "madd_alleen_len": 2}` | **applied** → both become 2 |
| `{"madd_monfasel_len": 2}` | applied |

**A field can therefore refuse to change for reasons the user cannot see in that field**,
and the fix is to send the *dependent* field alongside it. If a setting will not stick,
this is usually why.

Practical handling: when the user lowers `madd_aared_len`, clamp `madd_alleen_len` to it
in the same update rather than sending the one field alone. Then read back the resolved
values — see section 10 — instead of assuming the write landed.

**3. A mis-cased `strictness` falls back to the default.**
`"Normal"` is not `"normal"`.

**4. An unavailable engine falls back to the default.**
Always compare the session ack's `engine` to what you requested.

The unifying lesson: **the server never fails a session over a bad setting.** A bad
setting costs you the setting, not the session. That is deliberate and good for
reliability — and it means your client is the only thing that can tell the user their
choice did not take. Read back what you got and show it.

---

## 10. Testing that a setting actually changed the output

Settings bugs hide well, because a wrong setting still produces plausible feedback. Prove
each layer end to end.

**Engine.** Start a session with `"engine": "zipformer"` on this server, where it is not
built. Ack should say `real`. Your UI must not keep claiming zipformer.

**Moshaf.** The cleanest proof, because madd length directly changes the reference:
recite an āyah with a clear monfasel madd once at `madd_monfasel_len: 2` and once at `5`.
The findings must differ. If they are identical, your moshaf is being discarded — check
value types first (`4` vs `"4"`).

**Strictness.** Find a finding with confidence between the thresholds. At `lenient`
(0.90) a 0.75-confidence finding is `almost`; at `normal` (0.70) the same finding is
`error`. Same audio, different `status`.

**Rules.** Send `"rules": []` and recite with a deliberate madd mistake. You should get
**no** tajwīd finding — but a wrong *word* must still be reported, because hifz is never
filtered. That single test proves both halves of the layer.

**The `null` vs `[]` distinction.** Kill and relaunch the app with `[]` saved. If it
comes back grading everything, your persistence collapsed the two.

### A curl-level sanity check

Confirm the two schema endpoints before blaming your client:

```bash
curl http://localhost:8100/moshaf-schema      # expect 37 fields
curl http://localhost:8100/tajweed-rules      # expect 18 rules: 8 tajweed + 10 sifa
curl http://localhost:8100/health             # expect available_engines
```

Android emulator: replace `localhost` with `10.0.2.2`.

---

## 11. Full reference: the 37 moshaf fields

Read live from this server. **A snapshot for design purposes — fetch at runtime.**
`Default` is the *schema's* default; see section 4 for where three of these disagree with
what the server actually uses.

| Key | الاسم | Default | Options |
|---|---|---|---|
| `recitation_speed` | سرعة التلاوة | `murattal` | mujawad/above_murattal/murattal/hadr |
| `takbeer` | التكبير | `no_takbeer` | no_takbeer/beginning_of_sharh/end_of_doha/general_takbeer |
| `madd_monfasel_len` | مد المنفصل | `2` ⚠ | 2/3/4/5 |
| `madd_mottasel_len` | مقدار المد المتصل | `4` | 4/5/6 |
| `madd_mottasel_waqf` | مقدار المد المتصل وقفا | `4` | 4/5/6 |
| `madd_aared_len` | مقدار المد العارض | `2` ⚠ | 2/4/6 |
| `madd_alleen_len` | مقدار مد اللين | `2` ⚠ | 2/4/6 |
| `ghonna_lam_and_raa` | غنة اللام و الراء | `no_ghonna` | ghonna/no_ghonna |
| `meem_aal_imran` | ميم آل عمران {الم الله} وصلا | `waqf` | waqf/wasl_2/wasl_6 |
| `madd_yaa_alayn_alharfy` | مقدار المد اللازم الحرفي للعين | `6` | 2/4/6 |
| `saken_before_hamz` | الساكن قبل الهمز | `tahqeek` | tahqeek/general_sakt/local_sakt |
| `sakt_iwaja` | السكت عند عوجا في الكهف | `waqf` | sakt/waqf/idraj |
| `sakt_marqdena` | السكت عند مرقدنا في يس | `waqf` | sakt/waqf/idraj |
| `sakt_man_raq` | السكت عند من راق في القيامة | `sakt` | sakt/waqf/idraj |
| `sakt_bal_ran` | السكت عند بل ران في المطففين | `sakt` | sakt/waqf/idraj |
| `sakt_maleeyah` | {ماليه هلك} بالحاقة | `waqf` | sakt/waqf/idgham |
| `between_anfal_and_tawba` | وجه بين الأنفال والتوبة | `waqf` | waqf/sakt/wasl |
| `noon_and_yaseen` | النون عند الواو {يس والقرآن} و{ن والقلم} | `izhar` | izhar/idgham |
| `yaa_ataan` | إثبات الياء وحذفها وقفا {آتان} بالنمل | `wasl` | wasl/hadhf/ithbat |
| `start_with_ism` | البدء بكلمة {الاسم} في الحجرات | `wasl` | wasl/lism/alism |
| `yabsut` | السين والصاد {ويبسط} بالبقرة | `seen` | seen/saad |
| `bastah` | السين والصاد {بسطة} بالأعراف | `seen` | seen/saad |
| `almusaytirun` | السين والصاد {المصيطرون} بالطور | `saad` | seen/saad |
| `bimusaytir` | السين والصاد {بمصيطر} بالغاشية | `saad` | seen/saad |
| `tasheel_or_madd` | همزة الوصل {آلذكرين}/{آلآن}/{آلله} | `madd` | tasheel/madd |
| `yalhath_dhalik` | الإدغام وعدمه {يلهث ذلك} بالأعراف | `idgham` | izhar/idgham/waqf |
| `irkab_maana` | الإدغام والإظهار {اركب معنا} بهود | `idgham` | izhar/idgham/waqf |
| `noon_tamnna` | الإشمام والروم {لا تأمنا على يوسف} | `ishmam` | ishmam/rawm |
| `harakat_daaf` | حركة الضاد {ضعف} بالروم | `fath` | fath/dam |
| `alif_salasila` | إثبات الألف وحذفها وقفا {سلاسلا} | `wasl` | hadhf/ithbat/wasl |
| `idgham_nakhluqkum` | إدغام القاف في الكاف {نخلقكم} | `idgham_kamil` | idgham_naqis/idgham_kamil |
| `raa_firq` | التفخيم والترقيق {فرق} بالشعراء وصلا | `tafkheem` | waqf/tafkheem/tarqeeq |
| `raa_alqitr` | {القطر} في سبأ وقفا | `wasl` | wasl/tafkheem/tarqeeq |
| `raa_misr` | {مصر} في يونس ويوسف والزخرف وقفا | `wasl` | wasl/tafkheem/tarqeeq |
| `raa_nudhur` | {نذر} بالقمر وقفا | `tafkheem` | wasl/tafkheem/tarqeeq |
| `raa_yasr` | {يسر}/{أن أسر}/{فأسر} وقفا | `tarqeeq` | wasl/tafkheem/tarqeeq |
| `meem_mokhfah` | هل الميم مخفاة أو مدغمة | `ikhfaa` | meem/ikhfaa |

⚠ = the schema default disagrees with what the server actually uses. Section 4.

Each field also carries a `description` — often a long explanation of the ruling. Show it
as helper text; several are far too long for a label.

---

## 12. Full reference: the 18 gradeable rules

For the `rules` array. **All 10 `sifa` entries are inert on the zipformer engine.**

### Tajwīd (8)

| Key | English | العربية |
|---|---|---|
| `normal_madd` | Normal Madd | المد الطبيعي |
| `monfasel_madd` | Monfasel Madd | المد المنفصل |
| `mottasel_madd` | Mottasel Madd | المد المتصل |
| `mottasel_madd_at_pause` | Mottasel Madd at Pause | المد المتصل وقفا |
| `lazem_madd` | Lazem Madd | المد اللازم |
| `aared_madd` | Aared Madd | المد العارض للسكون |
| `leen_madd` | Leen Madd | مد اللين |
| `qalqalah` | Qalqalah | قلقة |

### Ṣifāt (10)

| Key | English | العربية |
|---|---|---|
| `hams_or_jahr` | Hams Or Jahr | الهمس والجهر |
| `shidda_or_rakhawa` | Shidda Or Rakhawa | الشدة والرخاوة |
| `tafkheem_or_taqeeq` | Tafkheem Or Taqeeq | التفخيم والترقيق |
| `itbaq` | Itbaq | الإطباق |
| `safeer` | Safeer | الصفير |
| `qalqla` | Qalqla | القلقلة (صفة) |
| `tikraar` | Tikraar | التكرار |
| `tafashie` | Tafashie | التفشي |
| `istitala` | Istitala | الاستطالة |
| `ghonna` | Ghonna | الغنة |

Note both `qalqalah` (tajwīd rule) and `qalqla` (ṣifa) exist and are distinct. And
ghunnah is `ghonna` — a **ṣifa**, not a tajwīd rule.
