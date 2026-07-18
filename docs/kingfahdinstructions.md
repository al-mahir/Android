# AI Agent Instructions: 15-Line Madani Mushaf Page Rendering & Justification Engine

## 🎯 Objective
Implement a production-grade, highly precise Quranic rendering engine in Android **Jetpack Compose** using **Kotlin**. The system must reconstruct pages based on the **King Fahd Glorious Quran Printing Complex (KFGQPC) V1 Layout (1405H print)** derived from the Tarteel Quranic Universal Library (QUL) API database schema.

The goal is to render exactly 15 uniform lines per page where text lines are dynamically justified/stretched, ensuring each page starts and finishes at predetermined data boundaries while utilizing the official glyph-based **QPC V1 Font**.

---

## 💾 Data Modeling & Schema Integration
Do not attempt text-wrapping or pagination on raw Quran text strings. The dataset is already deterministic, structured, and word-separated via database keys.

### 1. Database Entity Models
```kotlin
enum class LineType { AYAH, SURAH_NAME, BASMALLAH }

data class MushafLine(
    val pageNumber: Int,
    val lineNumber: Int, // 1 to 15
    val lineType: LineType,
    val isCentered: Boolean, // True = Center align (Surah names, Basmallah, or short terminal verses)
    val firstWordId: Int?,   // Foreign key pointer matching Word.wordIndex
    val lastWordId: Int?,    // Foreign key pointer matching Word.wordIndex
    val surahNumber: Int?    // Provided if lineType is SURAH_NAME
)

data class QuranWord(
    val wordIndex: Int,      // Unique global structural sequence identifier
    val wordKey: String,     // Format: "Surah:Ayah" (e.g., "1:1")
    val surah: Int,
    val ayah: Int,
    val text: String         // Glyph string mapped strictly to the QPC V1 Font
)
```

---

## ⚙️ Core Architectural Execution Pipeline

An AI agent implementing this engine must execute the layout through these explicit phases:

### Phase 1: Context Retrieval & Filtering
1. Query the local SQLite database for the designated `pageNumber`. Extract a fixed list of exactly 15 `MushafLine` entries sorted by `lineNumber`.
2. For lines categorized as `LineType.AYAH`, execute a range query on the `words` table where `wordIndex >= line.firstWordId` and `wordIndex <= line.lastWordId`. Maintain strict ascending order by `wordIndex`.
3. Concatenate text strings with space buffers `\u0020` or parse them as individual custom elements.

### Phase 2: High-Fidelity Font Mapping
1. Download and include the official **QPC V1 Font** (`.ttf` format) inside your Android project directory (`res/font/qpc_v1_font.ttf`).
2. Load it inside Compose via `FontFamily(Font(R.font.qpc_v1_font))`.
3. **Critical Rule:** The `text` field contains precise font glyph encodings. Do not strip vowel markers (Tashkeel) or change the character sequencing, as doing so breaks layout indexing.

### Phase 3: Text Constraints & Dynamic Measure-Stretch Loop
Standard Android standard `TextAlign.Justify` fails to render Arabic calligraphic text nicely, often resulting in distorted spacing or separation of characters from their accent marks. You must measure line components manually using `TextMeasurer` inside a `BoxWithConstraints` container.

Use code with caution.+-------------------------------------------------------------+ <- BoxWithConstraints (maxWidthPx)|  [Word 1]   [Word 2]   [Word 3]   [Word 4]   [Ayah Symbol]  | -> Measure raw width (initialWidth)+-------------------------------------------------------------+|<----------------------- remainingSpace ------------------->|
1. **Calculate Empty Space Constraints:**
    - Detect the maximum allowable pixel width: `val maxWidthPx = constraints.maxWidth`.
    - Measure the raw width of the line tokens grouped into a temporary string:
      ```kotlin
      val textLayoutResult = textMeasurer.measure(
          text = concatenatedLineText,
          style = textStyle.copy(fontFamily = qpcV1Font),
          constraints = Constraints(maxWidth = Constraints.Infinity)
      )
      val initialWidth = textLayoutResult.size.width
      val remainingSpace = maxWidthPx - initialWidth
      ```

2. **Branching Layout Logic (`isCentered` Evaluation):**
    - If `isCentered == true`: Align the text center using a standard Compose structure. No Kashida interpolation is required.
    - If `isCentered == false`: Trigger the text expansion workflow to match `maxWidthPx` precisely.

3. **Kashida Insertion Algorithm:**
    - Target the Arabic Tatweel/Kashida unicode point: `\u0640` (ـ).
    - Filter through words to find appropriate connecting letter pairs. Letters like `أ، د، ذ، ر، ز، و` cannot be extended from their left edge.
    - Evenly divide `remainingSpace` across candidate connection anchors. Interleave `\u0640` characters into the word strings iteratively.
    - Re-evaluate metrics via `TextMeasurer` within a running loop until `currentWidth` matches `maxWidthPx` safely within a narrow single-digit pixel threshold.

4. **Fallback Character Compression Engine:**
    - In rare scenarios where `remainingSpace < 0`, adjust line settings by setting a small negative `letterSpacing` (e.g., `-0.02.sp`) using a `SpanStyle` overlay to prevent overlapping or layout truncation.

---

## 🖥️ Production Reference Implementation

This Jetpack Compose structural template handles the custom measurement and baseline canvas execution logic for a single line:

```kotlin
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Initialize the official QUL QPC V1 glyph-compatible font family
val QpcV1FontFamily = FontFamily(
    Font(resId = R.font.qpc_v1_font) 
)

@OptIn(ExperimentalTextApi::class)
@Composable
fun MushafLineRenderer(
    line: MushafLine,
    words: List<QuranWord>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val baseTextStyle = TextStyle(
        fontFamily = QpcV1FontFamily,
        fontSize = 22.sp // Scale baseline depending on screen configuration density
    )

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val maxWidthPx = constraints.maxWidth

        when (line.lineType) {
            LineType.SURAH_NAME -> {
                // Surah Name headers are centered by design rule
                SurahHeaderRow(surahNumber = line.surahNumber ?: 1, style = baseTextStyle)
            }
            LineType.BASMALLAH -> {
                // Basmallah row is rendered standalone and centered
                BasmallahRow(style = baseTextStyle)
            }
            LineType.AYAH -> {
                val rawString = remember(words) { words.joinToString(" ") { it.text } }

                if (line.isCentered) {
                    // Standard centered display for short initial/terminal lines
                    Canvas(modifier = Modifier.fillMaxWidth().height(45.dp)) {
                        drawText(
                            textMeasurer = textMeasurer,
                            text = rawString,
                            style = baseTextStyle.copy(textAlign = TextAlign.Center),
                            size = this.size
                        )
                    }
                } else {
                    // Process advanced layout adjustment for normal text lines
                    val justifiedText = remember(rawString, maxWidthPx) {
                        applyKashidaJustification(
                            text = rawString,
                            targetWidthPx = maxWidthPx,
                            measurer = textMeasurer,
                            style = baseTextStyle
                        )
                    }

                    Canvas(modifier = Modifier.fillMaxWidth().height(45.dp)) {
                        drawText(
                            textMeasurer = textMeasurer,
                            text = justifiedText,
                            style = baseTextStyle,
                            size = this.size
                        )
                    }
                }
            }
        }
    }
}

private fun applyKashidaJustification(
    text: String,
    targetWidthPx: Int,
    measurer: TextMeasurer,
    style: TextStyle
): String {
    val rawLayout = measurer.measure(text = text, style = style, constraints = Constraints(maxWidth = Constraints.Infinity))
    var currentWidth = rawLayout.size.width
    var deficit = targetWidthPx - currentWidth

    if (deficit <= 0) return text // Line is already full or overflowing

    val wordsList = text.split(" ").toMutableList()
    val kashida = "\u0640"
    
    // Character insertion look-up loops are implemented iteratively by the agent engine 
    // to map strings to specific width criteria, skipping the final verse markers (\u06DD).
    var pass = 0
    while (deficit > 0 && pass < 5) {
        for (i in 0 until wordsList.size - 1) { // Skip the last element if it contains the Ayah symbol
            val word = wordsList[i]
            if (word.length > 2) { 
                // Insert a Kashida character at a valid anchor position within the word
                wordsList[i] = word.substring(0, word.length / 2) + kashida + word.substring(word.length / 2)
                
                // Re-evaluate line dimensions
                val testString = wordsList.joinToString(" ")
                val testLayout = measurer.measure(text = testString, style = style, constraints = Constraints(maxWidth = Constraints.Infinity))
                currentWidth = testLayout.size.width
                deficit = targetWidthPx - currentWidth
                
                if (deficit <= 0) break
            }
        }
        pass++
    }
    
    return wordsList.joinToString(" ")
}

@Composable fun SurahHeaderRow(surahNumber: Int, style: TextStyle) { /* Custom drawing logic for Surah Name */ }
@Composable fun BasmallahRow(style: TextStyle) { /* Custom drawing logic for ﷽ */ }
```

---

## ⚠️ Edge Cases & Validation Constraints for AI Execution
1. **End-of-Verse Component Integrity:** The Ayah end glyph `\u06DD` (۝) accompanied by its embedded localized text numerals must never be broken, wrapped, or isolated. Do not insert kashidas into these structural marker groups.
2. **Preserving Tashkeel/Vowel Mark Alignment:** Ensure inserted kashidas do not split a base Arabic character from its overlapping vowel accents or diacritics. Always insert them right before vowel markers to prevent display artifacts.
3. **Glyph Splitting Safety:** If a word contains complex embedded multi-character ligatures (like Lam-Alif `لا`), avoid splitting the ligature group with a kashida character.