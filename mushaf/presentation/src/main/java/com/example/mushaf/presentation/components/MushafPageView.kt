package com.example.mushaf.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mushaf.domain.model.LineType
import com.example.mushaf.domain.model.MushafConstants
import com.example.mushaf.domain.model.MushafPage
import com.example.mushaf.domain.model.ReadingMode
import com.example.mushaf.domain.model.recite.RecitationWordMark
import com.example.designsystem.theme.Theme
import com.example.mushaf.presentation.font.PageFontProvider
import com.example.mushaf.presentation.font.rememberPageFontFamily
import com.example.mushaf.presentation.font.rememberSurahNameFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Composable
fun MushafPageView(
    page: MushafPage,
    mode: ReadingMode,
    highlightedWordId: String?,
    modifier: Modifier = Modifier,
    prefetchPages: List<MushafPage> = emptyList(),
    areAyahsHidden: Boolean = false,
    revealedWordIds: Set<String> = emptySet(),
     
    wordMarks: Map<String, RecitationWordMark> = emptyMap(),
) {
    val fontFamily = rememberPageFontFamily(page.pageNumber, mode)
    val surahNameFontFamily = rememberSurahNameFontFamily()
    val contentColor = Theme.colors.onSurface
    val highlightColor = Theme.colors.primary.copy(alpha = 0.20f)
    val mistakeColor = Theme.colors.error
    val hintColor = Theme.colors.amber
    val underlineStroke = with(LocalDensity.current) { 2.dp.toPx() }
    val measurer = rememberTextMeasurer()
    
    
    val prefetchMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val context = LocalContext.current
    
    
    val fontResolver = LocalFontFamilyResolver.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        val availableWidthPx = constraints.maxWidth
        val availableHeightPx = constraints.maxHeight
        val slotHeightPx = availableHeightPx.toFloat() / MushafConstants.LINES_PER_PAGE

        val slotHeightSp = slotHeightPx / density.density / density.fontScale
        val heightLimitSp = slotHeightSp / MushafLayoutMath.LINE_HEIGHT_EM
        val lineSizes: Map<Int, Float> =
            remember(page.pageNumber, mode, availableWidthPx, heightLimitSp) {
                MushafLayoutCache.lineSizes(
                    page = page,
                    mode = mode,
                    widthPx = availableWidthPx,
                    heightLimitSp = heightLimitSp,
                ) { glyphs -> measureGlyphWidth(measurer, fontFamily, glyphs) }
            }


        val prefetchKey = prefetchPages.map { it.pageNumber }
        LaunchedEffect(prefetchKey, mode, availableWidthPx, availableHeightPx, contentColor) {
            if (prefetchPages.isEmpty() || availableWidthPx <= 0) return@LaunchedEffect
            withContext(Dispatchers.Default) {
                prefetchPages.forEach { neighbor ->
                    runCatching {
                        val neighborFont = PageFontProvider.create(context, neighbor.pageNumber, mode)
                        fontResolver.preload(neighborFont)
                        val neighborSizes = MushafLayoutCache.lineSizes(
                            page = neighbor,
                            mode = mode,
                            widthPx = availableWidthPx,
                            heightLimitSp = heightLimitSp,
                        ) { glyphs -> measureGlyphWidth(prefetchMeasurer, neighborFont, glyphs) }
                        pageTokens(
                            page = neighbor,
                            mode = mode,
                            lineSizes = neighborSizes,
                            measurer = prefetchMeasurer,
                            fontFamily = neighborFont,
                            surahNameFontFamily = surahNameFontFamily,
                            availableWidthPx = availableWidthPx,
                            availableHeightPx = availableHeightPx,
                            slotHeightPx = slotHeightPx,
                            contentColor = contentColor,
                        )
                    }
                }
            }
        }

        val tokens = remember(
            page, fontFamily, surahNameFontFamily, availableWidthPx, availableHeightPx, contentColor, lineSizes,
        ) {
            pageTokens(
                page = page,
                mode = mode,
                lineSizes = lineSizes,
                measurer = measurer,
                fontFamily = fontFamily,
                surahNameFontFamily = surahNameFontFamily,
                availableWidthPx = availableWidthPx,
                availableHeightPx = availableHeightPx,
                slotHeightPx = slotHeightPx,
                contentColor = contentColor,
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val highlighted = highlightedWordId
            tokens.forEach { token ->
                val isAyahWord = token.wordId != null
                val isVisible = when {
                    !areAyahsHidden -> true
                    !isAyahWord -> true
                    token.wordId in revealedWordIds -> true
                    else -> false
                }

                if (!isVisible) return@forEach

                val mark = token.wordId?.let(wordMarks::get)

                if (token.wordId != null && token.wordId == highlighted) {
                    drawRect(
                        color = highlightColor,
                        topLeft = Offset(token.left, token.top),
                        size = Size(
                            token.layout.size.width.toFloat(),
                            token.layout.size.height.toFloat(),
                        ),
                    )
                }
                
                
                
                val glyphColor = when (mark) {
                    RecitationWordMark.MISTAKE -> mistakeColor
                    RecitationWordMark.HINT -> hintColor
                    else -> Color.Unspecified
                }
                drawText(token.layout, color = glyphColor, topLeft = Offset(token.left, token.top))
                drawMarkUnderline(token, mark, mistakeColor, hintColor, underlineStroke)
            }
        }
    }
}








 
private fun DrawScope.drawMarkUnderline(
    token: PageToken,
    mark: RecitationWordMark?,
    mistakeColor: Color,
    hintColor: Color,
    strokeWidth: Float,
) {
    val color = when (mark) {
        RecitationWordMark.MISTAKE -> mistakeColor
        RecitationWordMark.HINT -> hintColor
        else -> return
    }
    val dashed = mark == RecitationWordMark.HINT
    val y = token.top + token.layout.size.height - strokeWidth
    drawLine(
        color = color,
        start = Offset(token.left, y),
        end = Offset(token.left + token.layout.size.width, y),
        strokeWidth = strokeWidth,
        pathEffect = if (dashed) {
            PathEffect.dashPathEffect(floatArrayOf(strokeWidth * 3f, strokeWidth * 2f))
        } else {
            null
        },
    )
}

 
private data class PageToken(
    val layout: TextLayoutResult,
    val left: Float,
    val top: Float,
    val wordId: String?,
)






 
private object PageTokenCache {
    private const val CACHE_SIZE = 12

    private data class Key(
        val page: Int,
        val tajweed: Boolean,
        val widthPx: Int,
        val heightPx: Int,
        val colorArgb: Int,
    )

    private val cache = object : LinkedHashMap<Key, List<PageToken>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<Key, List<PageToken>>): Boolean =
            size > CACHE_SIZE
    }

    private fun keyOf(page: MushafPage, mode: ReadingMode, widthPx: Int, heightPx: Int, colorArgb: Int) =
        Key(page.pageNumber, mode == ReadingMode.TAJWEED, widthPx, heightPx, colorArgb)

    @Synchronized
    fun get(page: MushafPage, mode: ReadingMode, widthPx: Int, heightPx: Int, colorArgb: Int): List<PageToken>? =
        cache[keyOf(page, mode, widthPx, heightPx, colorArgb)]

    @Synchronized
    fun put(page: MushafPage, mode: ReadingMode, widthPx: Int, heightPx: Int, colorArgb: Int, value: List<PageToken>) {
        cache[keyOf(page, mode, widthPx, heightPx, colorArgb)] = value
    }
}

 
private fun pageTokens(
    page: MushafPage,
    mode: ReadingMode,
    lineSizes: Map<Int, Float>,
    measurer: TextMeasurer,
    fontFamily: FontFamily,
    surahNameFontFamily: FontFamily?,
    availableWidthPx: Int,
    availableHeightPx: Int,
    slotHeightPx: Float,
    contentColor: Color,
): List<PageToken> {
    val colorArgb = contentColor.toArgb()
    PageTokenCache.get(page, mode, availableWidthPx, availableHeightPx, colorArgb)?.let { return it }
    val built = buildPageTokens(
        page = page,
        lineSizes = lineSizes,
        measurer = measurer,
        fontFamily = fontFamily,
        surahNameFontFamily = surahNameFontFamily,
        availableWidthPx = availableWidthPx,
        availableHeightPx = availableHeightPx,
        slotHeightPx = slotHeightPx,
        contentColor = contentColor,
    )
    PageTokenCache.put(page, mode, availableWidthPx, availableHeightPx, colorArgb, built)
    return built
}





 
private fun buildPageTokens(
    page: MushafPage,
    lineSizes: Map<Int, Float>,
    measurer: TextMeasurer,
    fontFamily: FontFamily,
    surahNameFontFamily: FontFamily?,
    availableWidthPx: Int,
    availableHeightPx: Int,
    slotHeightPx: Float,
    contentColor: Color,
): List<PageToken> {
    val sorted = page.lines.sortedBy { it.lineNumber }
    val lineCount = sorted.size
    if (lineCount == 0) return emptyList()

    val startY = (availableHeightPx - lineCount * slotHeightPx) / 2f
    val tokens = ArrayList<PageToken>()

    sorted.forEachIndexed { index, line ->
        val slotCenterY = startY + index * slotHeightPx + slotHeightPx / 2f
        when (line.type) {
            LineType.AYAH -> {
                val fontSize = (lineSizes[line.lineNumber] ?: MushafLayoutMath.MIN_SP).sp
                val centered = line.isCentered || line.words.size <= 1
                val layouts = line.words.map { word ->
                    measurer.measure(
                        text = AnnotatedString(word.glyphs),
                        style = TextStyle(fontFamily = fontFamily, fontSize = fontSize, color = contentColor),
                        softWrap = false,
                        maxLines = 1,
                    )
                }
                val lefts = MushafLayoutMath.tokenLefts(
                    widths = layouts.map { it.size.width.toFloat() },
                    maxWidthPx = availableWidthPx,
                    centered = centered,
                )
                line.words.forEachIndexed { i, word ->
                    val top = slotCenterY - layouts[i].size.height / 2f
                    tokens.add(PageToken(layouts[i], lefts[i], top, word.id))
                }
            }

            LineType.SURAH_NAME -> {
                val glyph = PageFontProvider.surahNameGlyph(line.surahNumber ?: 1)
                val layout = if (surahNameFontFamily != null && glyph != null) {
                    val size = fitLineSize(glyph, surahNameFontFamily, measurer, availableWidthPx, slotHeightPx)
                    measurer.measure(
                        text = AnnotatedString(glyph),
                        style = TextStyle(fontFamily = surahNameFontFamily, fontSize = size, color = contentColor),
                        softWrap = false,
                        maxLines = 1,
                    )
                } else {
                    measurer.measure(
                        text = AnnotatedString(SurahInfo.headerFor(line.surahNumber)),
                        style = TextStyle(fontFamily = FontFamily.Default, fontSize = 22.sp, color = contentColor),
                        softWrap = false,
                        maxLines = 1,
                    )
                }
                tokens.add(centeredToken(layout, availableWidthPx, slotCenterY))
            }

            LineType.BASMALLAH -> {
                
                val size = fitLineSize(SurahInfo.BASMALLAH_LIGATURE, FontFamily.Default, measurer, availableWidthPx, slotHeightPx)
                val layout = measurer.measure(
                    text = AnnotatedString(SurahInfo.BASMALLAH_LIGATURE),
                    style = TextStyle(fontFamily = FontFamily.Default, fontSize = size, color = contentColor),
                    softWrap = false,
                    maxLines = 1,
                )
                tokens.add(centeredToken(layout, availableWidthPx, slotCenterY))
            }
        }
    }
    return tokens
}

 
private fun centeredToken(layout: TextLayoutResult, availableWidthPx: Int, slotCenterY: Float): PageToken =
    PageToken(
        layout = layout,
        left = (availableWidthPx - layout.size.width) / 2f,
        top = slotCenterY - layout.size.height / 2f,
        wordId = null,
    )

 
private fun measureGlyphWidth(
    measurer: TextMeasurer,
    fontFamily: FontFamily,
    glyphs: String,
): Int =
    measurer.measure(
        text = AnnotatedString(glyphs),
        style = TextStyle(fontFamily = fontFamily, fontSize = MushafLayoutMath.REF_SP.sp),
        softWrap = false,
        maxLines = 1,
    ).size.width

 
private fun fitLineSize(
    text: String,
    font: FontFamily,
    measurer: TextMeasurer,
    maxWidthPx: Int,
    slotHeightPx: Float,
): TextUnit {
    val measured = measurer.measure(
        text = AnnotatedString(text),
        style = TextStyle(fontFamily = font, fontSize = MushafLayoutMath.REF_SP.sp),
        softWrap = false,
        maxLines = 1,
    ).size
    return MushafLayoutMath.fitSizeSp(
        contentWidthPx = measured.width,
        contentHeightPx = measured.height,
        maxWidthPx = maxWidthPx,
        maxHeightPx = slotHeightPx,
    ).sp
}
