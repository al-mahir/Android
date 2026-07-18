# Rendering a Standard Muṣḥaf (QPC V4) — Engineering Guide

How to render a page-accurate Qur'an (Muṣḥaf) that matches the printed King Fahd
Complex (KFGQPC) Madani layout: **every line contains exactly the same words as the
printed page, and every ayah ends with its numbered marker in the right place.**

This guide is written so it can be reused in any Qur'an app (Android/iOS/web). It
explains the data, the fonts, the one non-obvious trap that breaks 90% of naïve
implementations, and the exact pipeline that produces a provably-correct result.

---

## 1. The standard we are enforcing

The reference is the **Madani Muṣḥaf**: 604 pages, 15 lines per page. Two hard rules:

1. **Line composition is fixed.** Line _N_ of a page always holds the same set of
   words as the printed Muṣḥaf — never one word more or fewer. Readers memorise by
   page position, so this is not cosmetic; it is correctness.
2. **Ayah markers are anchored.** The end of every ayah carries its number glyph
   (۝١, ۝٢, …). A page that ends mid-ayah keeps going on the next page; a page that
   ends an ayah must show that ayah's marker as the last glyph of its last line.

If your render puts a word on the wrong line, or drops the trailing marker, it is
wrong even if the text reads correctly. That was the bug this guide fixes.

---

## 2. The QPC V4 font architecture (read this first)

QPC V4 ("QCF4", the colored-tajweed King Fahd font set) is **not** a normal text
font. It is a set of **604 page-specific glyph fonts**:

- One TTF per page: `p1.ttf … p604.ttf`.
- Each page font maps a contiguous Private-Use block starting at **U+FC41**. The
  first token of the page is `U+FC41`, the second `U+FC42`, and so on.
- Each glyph is a **whole precomposed token** — an entire word with all its
  tashkīl already drawn, or an ayah-end marker, or a standalone pause/waqf mark.
  You do **not** feed Arabic letters and shape them; you feed one codepoint per
  token and the font draws the calligraphy.
- **Tajweed colouring is baked in** as COLR/CPAL colour glyphs in the tajweed font
  variant. Selecting the tajweed page font colours the text natively — no runtime
  colour logic. The "standard" (plain black) variant has the same glyph layout,
  just no colour table.

Consequences that shape everything else:

- You cannot justify with Kashida/Tatweel — there are no connectable letters, only
  opaque word glyphs. Justification = distributing space **between** glyph tokens.
- Because glyphs are per-page and dense from `FC41`, a page's entire visual content
  is just `FC41 … FC41+N-1` in order, where `N` is that page's glyph count.

> Font source (same family the reference renderers use):
> `https://quran.com/fonts/quran/hafs/v4/colrv1/woff2/p{1..604}.woff2`

---

## 3. The one trap that breaks naïve implementations

**Word-ids are NOT glyph indices.** This is the whole ballgame.

The page's glyph stream interleaves three kinds of tokens:

| token kind      | example        | has a "word id"? |
|-----------------|----------------|------------------|
| word            | ذَٰلِكَ         | yes              |
| ayah-end marker | ۝١             | yes              |
| pause/waqf mark | ۛ  ۖ  صلے       | **NO**           |

The pause/waqf marks (ۛ ۖ ۗ ۚ ۙ …) exist as **separate glyphs in the font** but are
**not counted as words** in any word-level dataset. Across the whole Muṣḥaf there
are ~4,500 of them: the layout has **83,668** word tokens but the fonts contain
**88,186** glyphs.

So the tempting formula

```
glyphCode = 0xFC41 + (wordId - firstWordIdOnPage)   //  ❌ WRONG
```

drifts. It is correct only up to the first pause mark on a page; after that every
word maps one slot too early, and the page runs out of word-ids before the font's
real end — so lines end early and the final ۝ is never drawn.

### Concrete example — page 2, line 1 (start of Al-Baqarah)

Font glyph stream (what the page really contains):

```
slot: 0     1    2      3        4    5     6     7     8     9
glyph:الٓمٓ  ۝١  ذَٰلِكَ ٱلْكِتَٰبُ لَا  رَيْبَ  ۛ    فِيهِ  ۛ    هُدًى
                                              ↑pause      ↑pause
```

The printed line 1 is `الٓمٓ ۝١ ذَٰلِكَ ٱلْكِتَٰبُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًى` — **10 glyphs, ending
at هُدًى.** The naïve formula only consumes 8 word-ids and stops at فِيهِ, pushing
هُدًى onto line 2 and cascading the error down the page until the closing ۝٥ falls
off the bottom. That is exactly the "words end early / no ayah marker" symptom.

**Fix:** never derive glyphs arithmetically. Store, per word, its **exact glyph
string** (word glyph + any trailing mark glyphs), taken from an authoritative
word→glyph dataset.

---

## 4. The data you need (and where it comes from)

Three assets, from [QUL](https://qul.tarteel.ai) (Tarteel's open Qur'an Utility
Library) and the quran.com font CDN:

| # | asset | QUL resource | what it gives |
|---|-------|--------------|---------------|
| A | **Layout** | mushaf-layout **#19** "KFGQPC V4 layout — 1441H" | per page, per line: which words are on it, `is_centered`, surah headers, basmala rows |
| B | **Glyph script** | quran-script **#47** "V4 Glyphs (With Tajweed) — Word by Word" | per word: its exact V4 glyph string (`text` column), incl. trailing marks |
| C | **Fonts** | 604 page TTFs (quran.com `hafs/v4/colrv1`) | the actual glyphs; ship `standard` + `tajweed` variants |

Key facts that make A+B compose cleanly:

- Both #19 and #47 enumerate **83,668** word tokens in the same Qur'an reading
  order, so **#47 `id` == #19 `word_id`** (1:1). No fuzzy matching needed.
- #47's `text` is 1 char for a plain word, 2+ chars for a word that carries
  trailing marks (e.g. `ريب` → `[FC46, FC47]` where FC47 is the ۛ). Summed over a
  page, `text` lengths equal that page's font glyph count — exactly.
- An ayah-end marker is the **last word position of each ayah** in #47 (e.g. `2:1:2`
  = ۝١), so you can flag markers without extra data.

> ⚠️ **Do not substitute the quran.com public API or QPC V2 data for V4.** The API
> exposes `code_v1`/`code_v2` + `line_v1`/`line_v2` but **no `code_v4`/`line_v4`**.
> QCF V2 ≈ V4 but differs (V2 has 88,384 glyphs vs V4's 88,186; token counts 83,665
> vs 83,668; line-packing diverges on ~260 pages, mostly the last juz). V2 data
> renders V2 fonts perfectly and V4 fonts **wrongly**. Also, the only colored-tajweed
> font family that exists is v4/colrv1 — there is no v2 colored font — so if you want
> tajweed colour you must stay on V4.

---

## 5. The rebuilt layout database (schema)

Combine A (line breaks) + B (glyph strings) into one SQLite file the app ships in
`assets/databases/mushaf_v4_layout.db`:

```sql
CREATE TABLE info  (name TEXT, number_of_pages INT, lines_per_page INT, font_name TEXT);

-- one row per LINE (metadata only)
CREATE TABLE pages (page_number INT, line_number INT, line_type TEXT,
                    is_centered INT, surah_number INT);

-- one row per TOKEN (word or ayah marker) with its exact glyph string
CREATE TABLE words (page_number INT, line_number INT, position INT,
                    word_key TEXT,     -- "surah:ayah:word" (stable id for highlight/audio)
                    char_type TEXT,    -- 'word' | 'end'
                    glyph_text TEXT);  -- the FC glyph char(s) to draw

CREATE INDEX idx_pages_page ON pages(page_number);
CREATE INDEX idx_words_page ON words(page_number);
```

`line_type` is `ayah`, `surah_name`, or `basmallah`. Header/basmala lines carry no
words (handled specially at render — see §7).

---

## 6. Build pipeline

Pseudo-code (the real script is `scratchpad/build_db.py` in this repo's history):

```python
script = open_sqlite("qpc-v4.db")          # QUL #47  -> words(id, location, surah, ayah, word, text)
layout = open_sqlite("qpc-v4-layout.db")   # QUL #19  -> pages(page, line, line_type, is_centered, first_word_id, last_word_id, surah_number)

wtext = { id: text for id, text in script.words }          # word_id -> glyph string
maxpos = last word position per (surah, ayah)              # to flag ayah-end markers

for page in 1..604:
  for line in layout.pages[page] (ordered by line_number):
    emit pages_row(page, line.number, line.type, line.is_centered, line.surah_number)
    if line.type == 'ayah':
      pos = 0
      for wid in line.first_word_id .. line.last_word_id:
        s,a,w = location_of(wid)
        emit words_row(page, line.number, pos,
                       word_key = f"{s}:{a}:{w}",
                       char_type = 'end' if w == maxpos[(s,a)] else 'word',
                       glyph_text = wtext[wid])
        pos += 1
```

### Validation gate (must pass before shipping)

For **all 604 pages**, concatenating `glyph_text` in `(line_number, position)` order
must reproduce the page font's glyph set exactly:

```
codes(page) == [0xFC41, 0xFC42, … , 0xFC41 + N-1]      # contiguous, no gaps
len(codes(page)) == (# cmap codepoints ≥ 0xFC41 in p{page}.ttf)   # for BOTH standard & tajweed fonts
```

If a page is not a contiguous `FC41` block, the mapping is wrong — fail the build.
This repo ships that check as a JVM unit test:
`mushaf/data/src/test/.../MushafLayoutDbIntegrityTest.kt` (asserts contiguity +
83,668 words + 88,186 glyphs). It fails the build if a misaligned DB is ever
introduced.

---

## 7. Rendering

### 7.1 A word = one text run
Render each `words` row as a **single** text element containing its `glyph_text`.
Because a word's trailing marks live in the same string, marks stay glued to their
word. Never split a word's glyphs across elements.

### 7.2 Justification
Per line, lay the word runs out with space distributed **between** them
(`Arrangement.SpaceBetween` on Android / `justify-content: space-between` on web).
Centre only the lines flagged `is_centered` (short surah-ends, Al-Fātiḥah). This is
the KFGQPC justification standard for glyph fonts — there is no kashida.

### 7.3 Per-line font sizing (the subtle part)
The printed Muṣḥaf uses one visual size per page, but because each line's natural
width differs, a single page-wide size leaves short lines with huge `SpaceBetween`
gaps. Size **each justified line** so its own glyphs fill the page width, capped by a
per-line vertical budget:

```
sizeForLine = min( REF_SP * (pageWidth / sumOfWordWidthsAtRef) , heightLimitPerSlot )
```

Two gotchas baked into these fonts:
- They declare a very tall (~2.5 em) line box (empty tashkīl padding). Budget the
  vertical slot at ~1.9 em of font size, or the font is throttled and everything
  gaps.
- Measure a line's width as the **sum of per-word run widths**, not the words joined
  into one string — cross-glyph positioning can under-measure a joined string and
  clip edge words.

### 7.4 Surah headers and basmala (not in the word data)
- **Surah name**: a decorative ornament font (`surah_names.ttf`, an icomoon-style
  font). Its glyphs are in import order, **not** surah order — keep an explicit
  114-entry surah→codepoint table.
- **Basmala**: render the single ligature **U+FDFD (﷽)** from the system Naskh font;
  reliable and calligraphic without shipping another font.

### 7.5 Tajweed toggle
Swap the page font family between `fonts/standard/p{n}.ttf` and
`fonts/tajweed/p{n}.ttf`. Same glyph codes, same layout DB — the tajweed font simply
carries a COLR/CPAL colour table. No other code path changes.

---

## 8. Word-level features (highlight, audio sync, tap)
`word_key` = `"surah:ayah:word"` is a stable id independent of layout. Use it to:
- highlight the currently-recited word (follow-along),
- map taps to a word for translation/tafsir,
- sync ayah audio.

Ayah-end markers (`char_type = 'end'`) should be excluded from word highlighting.

---

## 9. Reuse checklist for another Qur'an app

1. Pick the mushaf edition and get its **matched** layout + glyph-script + fonts from
   QUL. They are versioned together (V1/V2/V4); never mix versions.
2. Build the DB with the §6 pipeline and **run the §6 validation gate**. If it fails,
   your layout and glyph-script are not the same version/edition.
3. Ship per-page fonts (both plain and tajweed if you want the toggle) +
   ornament + the DB.
4. Render per §7. Store `word_key` for interactivity.
5. Add a DB-version guard so app updates re-copy the bundled DB over the cached copy
   on existing installs.

Platform notes:
- **Web**: same idea; declare each `p{n}` font via `@font-face` and print the glyph
  string per word. COLRv1 tajweed renders natively in modern browsers.
- **iOS**: Core Text renders COLR/PUA glyphs; the per-line sizing math is identical.

---

## 10. What NOT to do (hard-won)
- ❌ `glyph = FC41 + wordId` arithmetic — drifts on every pause mark (see §3).
- ❌ Detecting marks heuristically from the font (advance width, glyph shape) —
  combining marks are zero-advance but inline lettered-waqf (صلے/قلے) carry real
  advance and are indistinguishable from short words. Tops out ~65% of pages.
- ❌ Using quran.com's API `code_v2`/`line_v2` to drive V4 fonts — V2 ≠ V4 (§4).
- ❌ Rendering a whole line as one joined string — breaks per-word justification,
  highlighting, and width measurement.
- ❌ Mixing a V1 layout with V4 fonts (or any cross-version pair) — different word
  counts and line breaks.

---

## 11. Cost / footprint
- 604 × 2 font sets ≈ **320 MB** of TTFs (plain + tajweed). This dominates app size.
  Options: ship one set and download the other on demand; or use WOFF2 (much
  smaller) where the platform supports it.
- Layout DB ≈ 4 MB.

---

## 12. References
- QUL (data): https://qul.tarteel.ai
  - Layout #19 — `resources/mushaf-layout/19` (KFGQPC V4, 1441H)
  - Glyph script #47 — `resources/quran-script/47` (V4 Glyphs, word-by-word)
  - (V1/V2 equivalents: layouts #15/#10, scripts #57/#61 — for those font families)
- Fonts: `https://quran.com/fonts/quran/hafs/v4/colrv1/woff2/p{n}.woff2`
- quran.com API (V1/V2 codes + lines, **not** V4): `https://api.quran.com/api/v4/verses/by_page/{n}?words=true&word_fields=code_v2,line_v2,char_type_name`

---

### This repo's implementation map
- DB asset: `mushaf/data/src/main/assets/databases/mushaf_v4_layout.db`
- Fonts: `mushaf/data/src/main/assets/fonts/{standard,tajweed,ornament}/`
- Data layer: `MushafAssetDataSource` (queries + DB-version guard), `MushafMapper`,
  `MushafWordEntity`/`MushafLineEntity`
- Domain: `MushafWord(glyphs: String, isEndOfAyah, …)`, `MushafLine`, `MushafPage`
- UI: `MushafPageView` (per-line sizing), `MushafLineRow` (justification),
  `MushafWordGlyph` (one text run per word), `PageFontProvider` (font selection,
  surah-name table)
- Correctness gate: `mushaf/data/src/test/.../MushafLayoutDbIntegrityTest.kt`
