package com.example.mushaf.presentation.guide

/**
 * Describes which part of the screen the guide step's target element lives in.
 * The [GuideTooltip] uses this to position the card on the *opposite* side
 * and draw an arrow pointing toward the target.
 */
enum class TooltipAnchor {
    /** Target is near the top (e.g. the Surah-name pill in the TopBar). */
    TOP,
    /** Target is in the middle of the screen (e.g. Ayah text area). */
    CENTER,
    /** Target is near the bottom (e.g. mode tabs in the BottomBar). */
    BOTTOM,
}
