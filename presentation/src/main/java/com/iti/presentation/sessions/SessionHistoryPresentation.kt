package com.iti.presentation.sessions

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.example.designsystem.components.session.SessionBreakdownUi
import com.example.designsystem.components.session.SessionStatUi
import com.example.designsystem.theme.Theme
import com.iti.domain.model.quran.SurahNames
import com.iti.domain.model.recitation.RecitationSessionSummary
import com.iti.domain.model.recitation.SessionMistakeCategory
import com.iti.presentation.R
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Formatting for a stored session.
 *
 * A sibling of the muṣḥaf's copy. The two presentation modules must not depend on each other
 * (AGENTS.md §2) and the shared part — the summary layout — already lives in `:designsystem`;
 * what is repeated is only each module's own localized wording.
 */
/**
 * The composition's locale.
 *
 * Read from [LocalConfiguration] rather than `Locale.getDefault()`: the app installs its own
 * locale for the subtree, and the platform default would ignore it and format dates and numbers
 * in the system language instead of the one the reciter chose.
 */
@Composable
internal fun rememberLocale(): Locale {
    val locales = LocalConfiguration.current.locales
    return if (locales.isEmpty) Locale.ROOT else locales[0]
}

/** "الفاتحة ١ — الفاتحة ٧". */
internal fun RecitationSessionSummary.rangeLabel(): String {
    val from = "${SurahNames.nameOf(start.sura).orEmpty()} ${start.aya}".trim()
    val to = "${SurahNames.nameOf(end.sura).orEmpty()} ${end.aya}".trim()
    return if (from == to) from else "$from — $to"
}

/** Date and time the session was recited, in the reader's locale. */
internal fun RecitationSessionSummary.dateLabel(locale: Locale): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
        .format(Date(startedAtEpochMs))

internal fun RecitationSessionSummary.durationLabel(locale: Locale): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    return String.format(locale, "%d:%02d", minutes, seconds)
}

@Composable
internal fun RecitationSessionSummary.headline(): String = stringResource(
    when {
        gradedNothing -> R.string.session_headline_nothing_checked
        mistakeCount == 0 -> R.string.session_headline_clean
        else -> R.string.session_headline_reviewed
    },
)

/**
 * Accuracy is absent, not zero, when nothing was graded — a session the service never checked
 * has no accuracy, and 0% would read as a catastrophic recitation.
 */
@Composable
internal fun RecitationSessionSummary.stats(locale: Locale): List<SessionStatUi> {
    val integers = NumberFormat.getIntegerInstance(locale)
    val percent = NumberFormat.getPercentInstance(locale).apply { maximumFractionDigits = 1 }

    return buildList {
        accuracy?.let {
            add(SessionStatUi(percent.format(it), stringResource(R.string.session_stat_accuracy)))
        }
        add(SessionStatUi(integers.format(mistakeCount), stringResource(R.string.session_stat_mistakes)))
        add(SessionStatUi(integers.format(scoredWordCount), stringResource(R.string.session_stat_words)))
        add(SessionStatUi(durationLabel(locale), stringResource(R.string.session_stat_duration)))
    }
}

@Composable
internal fun RecitationSessionSummary.breakdown(): List<SessionBreakdownUi> =
    mistakesByCategory.entries
        .sortedByDescending { it.value }
        .map { (category, count) ->
            SessionBreakdownUi(
                label = stringResource(category.labelRes()),
                count = count,
                color = when (category) {
                    SessionMistakeCategory.MEMORIZATION -> Theme.colors.error
                    SessionMistakeCategory.TASHKIL -> Theme.colors.amber
                    SessionMistakeCategory.TAJWID -> Theme.colors.primary
                    SessionMistakeCategory.OTHER -> Theme.colors.secondary
                },
            )
        }

@Composable
internal fun RecitationSessionSummary.practiceLines(): List<String> = practiceFocus.map { focus ->
    val name = focus.ruleName ?: stringResource(focus.category.labelRes())
    stringResource(R.string.session_practice_item, name, focus.occurrences)
}

@Composable
internal fun RecitationSessionSummary.emptyMessageOrNull(): String? =
    if (gradedNothing) stringResource(R.string.session_nothing_checked_message) else null

private fun SessionMistakeCategory.labelRes(): Int = when (this) {
    SessionMistakeCategory.MEMORIZATION -> R.string.session_category_memorization
    SessionMistakeCategory.TASHKIL -> R.string.session_category_tashkil
    SessionMistakeCategory.TAJWID -> R.string.session_category_tajwid
    SessionMistakeCategory.OTHER -> R.string.session_category_other
}
