package com.example.mushaf.presentation.recite

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.designsystem.components.session.SessionBreakdownUi
import com.example.designsystem.components.session.SessionStatUi
import com.example.designsystem.theme.Theme
import com.example.mushaf.presentation.R
import com.example.mushaf.presentation.SurahNameResolver
import com.iti.domain.model.recitation.RecitationSessionSummary
import com.iti.domain.model.recitation.SessionMistakeCategory
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit







 
@Composable
fun RecitationSessionSummary.headline(): String = stringResource(
    when {
        gradedNothing -> R.string.session_headline_nothing_checked
        mistakeCount == 0 -> R.string.session_headline_clean
        else -> R.string.session_headline_reviewed
    },
)

@Composable
fun RecitationSessionSummary.subtitle(): String {
    val from = "${SurahNameResolver.nameFor(start.sura)} ${start.aya}"
    val to = "${SurahNameResolver.nameFor(end.sura)} ${end.aya}"
    return stringResource(R.string.session_range, from, to)
}






 
@Composable
fun RecitationSessionSummary.stats(): List<SessionStatUi> {
    val locale = currentLocale()
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

private fun RecitationSessionSummary.durationLabel(locale: Locale): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    return String.format(locale, "%d:%02d", minutes, seconds)
}

@Composable
fun RecitationSessionSummary.breakdown(): List<SessionBreakdownUi> =
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
fun RecitationSessionSummary.practiceLines(): List<String> = practiceFocus.map { focus ->
    val name = focus.ruleName ?: stringResource(focus.category.labelRes())
    stringResource(R.string.mushaf_practice_focus_item, name, focus.occurrences)
}




 
@Composable
fun RecitationSessionSummary.emptyMessageOrNull(): String? =
    if (gradedNothing) stringResource(R.string.session_nothing_checked_message) else null

private fun SessionMistakeCategory.labelRes(): Int = when (this) {
    SessionMistakeCategory.MEMORIZATION -> R.string.session_category_memorization
    SessionMistakeCategory.TASHKIL -> R.string.mushaf_correction_tashkeel
    SessionMistakeCategory.TAJWID -> R.string.mushaf_correction_tajweed
    SessionMistakeCategory.OTHER -> R.string.mushaf_correction_generic
}
