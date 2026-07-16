package com.example.mushaf.domain.model


data class ReaderPreferences(
    val tajweedEnabled: Boolean = true,
    val lastPage: Int = MushafConstants.FIRST_PAGE,
)

object MushafConstants {
    const val FIRST_PAGE = 1
    const val LAST_PAGE = 604
    const val LINES_PER_PAGE = 15

    fun clampPage(page: Int): Int = page.coerceIn(FIRST_PAGE, LAST_PAGE)
}
