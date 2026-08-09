package com.example.mushaf.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.res.vectorResource
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
import com.example.designsystem.R as DesignSystemR
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun MushafPageView(
    page: MushafPage,
    mode: ReadingMode,
    highlightedWordId: String?,
    modifier: Modifier = Modifier,
    prefetchPages: List<MushafPage> = emptyList(),
    areAyahsHidden: Boolean = false,
    revealedWordIds: Set<String> = emptySet(),
    onWordClick: (String) -> Unit = {},
    onWordLongClick: (String) -> Unit = {},
    onBlankClick: () -> Unit = {},
    wordMarks: Map<String, RecitationWordMark> = emptyMap(),
) {
    val fontFamily = rememberPageFontFamily(page.pageNumber, mode)
    val surahNameFontFamily = rememberSurahNameFontFamily()
    val surahBannerPainter = rememberVectorPainter(
        image = ImageVector.vectorResource(id = DesignSystemR.drawable.surah_name_design),
    )
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
            .padding(start = 12.dp, end = 12.dp, top = 40.dp, bottom = 12.dp),
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
        LaunchedEffect(prefetchPages, availableWidthPx) {
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

        // A word still under the memorisation veil gets no highlight: the box alone would give
        // away a position the veil exists to withhold, and the on-device cursor deliberately does
        // not reveal anything on its own.
        val highlightTarget = remember(tokens, highlightedWordId, areAyahsHidden, revealedWordIds) {
            tokens.firstOrNull { it.wordId != null && it.wordId == highlightedWordId }
                ?.takeIf { !areAyahsHidden || it.wordId in revealedWordIds }
                ?.let { token ->
                    Rect(
                        offset = Offset(token.left, token.top),
                        size = Size(token.layout.size.width.toFloat(), token.layout.size.height.toFloat()),
                    )
                }
        }
        val highlight = rememberSlidingHighlight(highlightTarget, slotHeightPx)

        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(tokens) {
                detectTapGestures(
                    onTap = { offset ->
                        var clickedWord: String? = null
                        for (token in tokens) {
                            if (token.wordId != null) {
                                val right = token.left + token.layout.size.width
                                val bottom = token.top + token.layout.size.height
                                if (offset.x >= token.left && offset.x <= right && offset.y >= token.top && offset.y <= bottom) {
                                    clickedWord = token.wordId
                                    break
                                }
                            }
                        }
                        if (clickedWord != null) {
                            onWordClick(clickedWord)
                        } else {
                            onBlankClick()
                        }
                    },
                    onLongPress = { offset ->
                        var clickedWord: String? = null
                        for (token in tokens) {
                            if (token.wordId != null) {
                                val right = token.left + token.layout.size.width
                                val bottom = token.top + token.layout.size.height
                                if (offset.x >= token.left && offset.x <= right && offset.y >= token.top && offset.y <= bottom) {
                                    clickedWord = token.wordId
                                    break
                                }
                            }
                        }
                        if (clickedWord != null) {
                            onWordLongClick(clickedWord)
                        }
                    }
                )
            }
        ) {
            // Drawn before the glyphs, and once for the whole page rather than per token: the
            // highlight is now a single animated rectangle that travels, not a property of
            // whichever token happens to be current.
            if (highlight.isPlaced && highlight.alpha.value > 0f) {
                drawRect(
                    color = highlightColor.copy(alpha = highlightColor.alpha * highlight.alpha.value),
                    topLeft = Offset(highlight.left.value, highlight.top.value),
                    size = Size(highlight.width.value, highlight.height.value),
                )
            }
            tokens.forEach { token ->
                val isAyahWord = token.wordId != null
                val isVisible = when {
                    !areAyahsHidden -> true
                    !isAyahWord -> true
                    token.isEndOfAyah -> true
                    token.wordId in revealedWordIds -> true
                    else -> false
                }

                if (!isVisible) return@forEach

                if (token.isSurahNameBanner) {
                    drawSurahNameBanner(token, surahBannerPainter, availableWidthPx, availableHeightPx)
                }

                val mark = token.wordId?.let(wordMarks::get)

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








 
/**
 * The animated position of the reading highlight.
 *
 * Held as [Animatable]s rather than derived per frame so the values can be read inside the
 * [Canvas] draw lambda: that keeps a moving highlight in the draw phase, instead of recomposing
 * the whole page — including its text measurement — sixty times a second.
 */
@Stable
private class SlidingHighlight {
    val left = Animatable(0f)
    val top = Animatable(0f)
    val width = Animatable(0f)
    val height = Animatable(0f)
    val alpha = Animatable(0f)

    /** False until the highlight has a real position, so it never animates in from the origin. */
    var isPlaced by mutableStateOf(false)
}

/**
 * Slides the highlight from word to word.
 *
 * Only *along a line*. A move to another line has no path worth travelling — sliding diagonally
 * would drag the highlight across words the reciter never said, and across the whole page on a
 * line wrap — so those snap, as do first appearances and page turns. [lineHeightPx] is what
 * separates the two cases: anything landing more than half a line away is a different line.
 */
@Composable
private fun rememberSlidingHighlight(target: Rect?, lineHeightPx: Float): SlidingHighlight {
    val highlight = remember { SlidingHighlight() }

    LaunchedEffect(target, lineHeightPx) {
        if (target == null) {
            highlight.alpha.animateTo(0f, tween(HIGHLIGHT_FADE_MS))
            highlight.isPlaced = false
            return@LaunchedEffect
        }

        val slides = highlight.isPlaced &&
            kotlin.math.abs(target.top - highlight.top.value) < lineHeightPx / 2f
        if (slides) {
            coroutineScope {
                launch { highlight.left.animateTo(target.left, HIGHLIGHT_MOVE_SPEC) }
                launch { highlight.top.animateTo(target.top, HIGHLIGHT_MOVE_SPEC) }
                launch { highlight.width.animateTo(target.width, HIGHLIGHT_MOVE_SPEC) }
                launch { highlight.height.animateTo(target.height, HIGHLIGHT_MOVE_SPEC) }
            }
        } else {
            highlight.left.snapTo(target.left)
            highlight.top.snapTo(target.top)
            highlight.width.snapTo(target.width)
            highlight.height.snapTo(target.height)
            highlight.isPlaced = true
            highlight.alpha.animateTo(1f, tween(HIGHLIGHT_FADE_MS))
        }
    }

    return highlight
}

/** Fast enough to stay under the reciter rather than trail them, damped enough not to overshoot
 * into the neighbouring word — an overshoot on a word-sized target reads as the wrong word being
 * highlighted for a frame or two. */
private val HIGHLIGHT_MOVE_SPEC = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
)
private const val HIGHLIGHT_FADE_MS = 140

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

private fun DrawScope.drawSurahNameBanner(
    token: PageToken,
    painter: VectorPainter,
    availableWidthPx: Int,
    availableHeightPx: Int,
) {
    val paddingY = SURAH_BANNER_PADDING_VERTICAL.toPx()
    val frameTop = (token.top - paddingY).coerceAtLeast(0f)
    val frameBottom = (token.top + token.layout.size.height + paddingY).coerceAtMost(availableHeightPx.toFloat())
    val frameHeight = frameBottom - frameTop
    translate(left = 0f, top = frameTop) {
        with(painter) { draw(size = Size(availableWidthPx.toFloat(), frameHeight)) }
    }
}

private val SURAH_BANNER_PADDING_VERTICAL = 14.dp
private const val SURAH_BANNER_WIDTH_FRACTION = 0.55f
private const val SURAH_BANNER_HEIGHT_FRACTION = 0.55f


private data class PageToken(
    val layout: TextLayoutResult,
    val left: Float,
    val top: Float,
    val wordId: String?,
    val isEndOfAyah: Boolean = false,
    val isSurahNameBanner: Boolean = false,
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
                    tokens.add(PageToken(layouts[i], lefts[i], top, word.id, word.isEndOfAyah))
                }
            }

            LineType.SURAH_NAME -> {
                val glyph = PageFontProvider.surahNameGlyph(line.surahNumber ?: 1)
                val layout = if (surahNameFontFamily != null && glyph != null) {
                    val size = fitLineSize(
                        text = glyph,
                        font = surahNameFontFamily,
                        measurer = measurer,
                        maxWidthPx = (availableWidthPx * SURAH_BANNER_WIDTH_FRACTION).toInt(),
                        slotHeightPx = slotHeightPx * SURAH_BANNER_HEIGHT_FRACTION,
                    )
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
                tokens.add(centeredToken(layout, availableWidthPx, slotCenterY, isSurahNameBanner = true))
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


private fun centeredToken(
    layout: TextLayoutResult,
    availableWidthPx: Int,
    slotCenterY: Float,
    isSurahNameBanner: Boolean = false,
): PageToken =
    PageToken(
        layout = layout,
        left = (availableWidthPx - layout.size.width) / 2f,
        top = slotCenterY - layout.size.height / 2f,
        wordId = null,
        isSurahNameBanner = isSurahNameBanner,
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
