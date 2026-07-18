package com.example.mushaf.presentation.components


object MushafLayoutMath {

    const val REF_SP = 40f


    const val MAX_SP = 96f


    const val MIN_SP = 10f

    const val WIDTH_SAFETY = 0.98f

    const val HEIGHT_SAFETY = 0.9f


    const val LINE_HEIGHT_EM = 1.9f


    fun uniformAyahSizeSp(
        lineWidthsPx: List<Int>,
        maxWidthPx: Int,
        heightLimitSp: Float,
    ): Float {
        if (lineWidthsPx.isEmpty() || maxWidthPx <= 0 || heightLimitSp <= 0f) return MAX_SP
        val widest = (lineWidthsPx.maxOrNull() ?: return MAX_SP).coerceAtLeast(1)
        return fillLineSizeSp(widest, maxWidthPx, heightLimitSp)
    }

    fun fillLineSizeSp(lineWidthPx: Int, maxWidthPx: Int, heightLimitSp: Float): Float {
        if (lineWidthPx <= 0 || maxWidthPx <= 0 || heightLimitSp <= 0f) {
            return heightLimitSp.coerceIn(MIN_SP, MAX_SP)
        }
        val byWidth = REF_SP * (maxWidthPx.toFloat() / lineWidthPx) * WIDTH_SAFETY
        return minOf(byWidth, heightLimitSp).coerceIn(MIN_SP, MAX_SP)
    }

    fun fitSizeSp(
        contentWidthPx: Int,
        contentHeightPx: Int,
        maxWidthPx: Int,
        maxHeightPx: Float,
    ): Float {
        if (contentWidthPx <= 0 || contentHeightPx <= 0 || maxWidthPx <= 0 || maxHeightPx <= 0f) {
            return MAX_SP
        }
        val byWidth = REF_SP * (maxWidthPx.toFloat() / contentWidthPx) * WIDTH_SAFETY
        val byHeight = REF_SP * (maxHeightPx / contentHeightPx) * HEIGHT_SAFETY
        return minOf(byWidth, byHeight).coerceIn(MIN_SP, MAX_SP)
    }

    /**
     * Left x (px, top-left origin) of each token when a line is laid out in reading order
     * **right-to-left** across [maxWidthPx].
     *
     * - Justified (`centered == false`, >1 token): the first token is pinned to the right edge and
     *   the last to the left edge, with the leftover width spread evenly between tokens — the
     *   pixel-for-pixel equivalent of `Arrangement.SpaceBetween`.
     * - Centered / single token: tokens are packed adjacent and the group is centred, like
     *   `Arrangement.Center`.
     *
     * Returned lefts are parallel to [widths] (token order = reading order).
     */
    fun tokenLefts(widths: List<Float>, maxWidthPx: Int, centered: Boolean): List<Float> {
        if (widths.isEmpty()) return emptyList()
        val total = widths.sum()
        val n = widths.size

        var right: Float
        val gap: Float
        if (centered || n == 1) {
            right = maxWidthPx / 2f + total / 2f
            gap = 0f
        } else {
            right = maxWidthPx.toFloat()
            gap = (maxWidthPx - total) / (n - 1)
        }

        val lefts = ArrayList<Float>(n)
        for (w in widths) {
            val left = right - w
            lefts.add(left)
            right = left - gap
        }
        return lefts
    }
}
