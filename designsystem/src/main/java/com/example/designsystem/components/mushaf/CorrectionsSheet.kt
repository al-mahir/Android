package com.example.designsystem.components.mushaf

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.example.designsystem.R
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.bottomsheet.AppBottomSheet
import com.example.designsystem.theme.Theme

/** One word of a recited passage, and whether it was the mistake. */
data class CorrectionWordUi(
    val text: String,
    val isMistake: Boolean,
)


data class CorrectionMistakeUi(
    val wordId: String,
    val word: String,
    val label: String,
    val detail: String? = null,
)


data class CorrectionTabUi(
    val label: String,
    val count: Int,
)

data class CorrectionCardUi(
    val id: String,
    val ayahLabel: String,
    val words: List<CorrectionWordUi>,
    val mistakes: List<CorrectionMistakeUi>,
)


@Composable
fun CorrectionsSheet(
    title: String,
    subtitle: String,
    corrections: List<CorrectionCardUi>,
    emptyMessage: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    practiceTitle: String? = null,
    practiceFocus: List<String> = emptyList(),
    tabs: List<CorrectionTabUi> = emptyList(),
    selectedTabIndex: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    onMistakeClick: (String) -> Unit = {},
) {
    AppBottomSheet(onDismiss = onDismiss, modifier = modifier) {
        CorrectionsList(
            title = title,
            subtitle = subtitle,
            corrections = corrections,
            emptyMessage = emptyMessage,
            practiceTitle = practiceTitle,
            practiceFocus = practiceFocus,
            tabs = tabs,
            selectedTabIndex = selectedTabIndex,
            onTabSelected = onTabSelected,
            onMistakeClick = onMistakeClick,
        )
    }
}


@Composable
internal fun CorrectionsList(
    title: String,
    subtitle: String,
    corrections: List<CorrectionCardUi>,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    practiceTitle: String? = null,
    practiceFocus: List<String> = emptyList(),
    tabs: List<CorrectionTabUi> = emptyList(),
    selectedTabIndex: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    onMistakeClick: (String) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Theme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            BasicText(
                text = title,
                style = Theme.typography.body.large.copy(
                    color = Theme.colors.primaryFont,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            BasicText(
                text = subtitle,
                style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
            )
        }

        // Only when there is more than one channel to choose between. A single chip is a label
        // pretending to be a control.
        if (tabs.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = Theme.spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
            ) {
                tabs.forEachIndexed { index, tab ->
                    CorrectionFilterChip(
                        tab = tab,
                        selected = index == selectedTabIndex,
                        onClick = { onTabSelected(index) },
                    )
                }
            }
        }

        if (corrections.isEmpty()) {
            BasicText(
                text = emptyMessage,
                style = Theme.typography.body.medium.copy(
                    color = Theme.colors.secondaryFont,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Theme.spacing.extraLarge),
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Theme.spacing.large),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
        ) {
            if (practiceTitle != null && practiceFocus.isNotEmpty()) {
                item(key = "practice-focus") {
                    PracticeFocusCard(title = practiceTitle, focus = practiceFocus)
                }
            }

            items(corrections, key = { it.id }) { correction ->
                CorrectionCard(correction = correction, onMistakeClick = onMistakeClick)
            }
        }
    }
}

/**
 * What recurred, and how often.
 *
 * Sits above the list because it is the answer to the question the reciter actually has. A list
 * of eleven corrections is a transcript; "المد الطبيعي ×4" is a lesson.
 */
@Composable
private fun PracticeFocusCard(title: String, focus: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Theme.shapes.medium)
            .background(Theme.colors.primaryContainer)
            .padding(Theme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
    ) {
        BasicText(
            text = title,
            style = Theme.typography.body.medium.copy(
                color = Theme.colors.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        focus.forEach { line ->
            BasicText(
                text = line,
                style = Theme.typography.body.small.copy(color = Theme.colors.onPrimaryContainer),
            )
        }
    }
}

/**
 * A filter chip.
 *
 * Filled when selected rather than underlined: the row scrolls, and an underline on an
 * off-screen chip leaves no indication of what is being filtered.
 */
@Composable
private fun CorrectionFilterChip(
    tab: CorrectionTabUi,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val container = if (selected) Theme.colors.primary else Theme.colors.surface
    val content = if (selected) Theme.colors.onPrimary else Theme.colors.primaryFont

    Row(
        modifier = Modifier
            .clip(Theme.shapes.circle)
            .background(container)
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.border(1.dp, Theme.colors.border, Theme.shapes.circle)
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.small),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = tab.label,
            style = Theme.typography.body.small.copy(
                color = content,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
        )
        BasicText(
            text = tab.count.toString(),
            style = Theme.typography.body.small.copy(color = content.copy(alpha = 0.7f)),
        )
    }
}

@Composable
private fun CorrectionCard(
    correction: CorrectionCardUi,
    onMistakeClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Theme.shapes.medium)
            .border(1.dp, Theme.colors.border, Theme.shapes.medium)
            .background(Theme.colors.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Theme.spacing.medium,
                    end = Theme.spacing.medium,
                    top = Theme.spacing.medium,
                ),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AyahChip(label = correction.ayahLabel)
        }

        // The whole āyah, so the correction can be read in context rather than as a bare word.
        BasicText(
            text = correction.words.toAnnotatedText(),
            style = Theme.typography.body.large.copy(color = Theme.colors.primaryFont),
            modifier = Modifier.padding(Theme.spacing.medium),
        )

        HorizontalDivider(color = Theme.colors.border)

        // Every mistake gets its own row. An āyah can go wrong in several places for different
        // reasons; one label per āyah would show only the first and quietly hide the rest.
        correction.mistakes.forEachIndexed { index, mistake ->
            if (index > 0) HorizontalDivider(color = Theme.colors.border)
            MistakeRow(mistake = mistake, onClick = { onMistakeClick(mistake.wordId) })
        }
    }
}

@Composable
private fun MistakeRow(
    mistake: CorrectionMistakeUi,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(Theme.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_info),
            // The row's text says what is wrong; a second announcement would be noise.
            contentDescription = null,
            tint = Theme.colors.error,
            modifier = Modifier.size(Theme.size.iconSemiMedium),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(
                text = mistake.word,
                style = Theme.typography.body.large.copy(
                    color = Theme.colors.error,
                    fontWeight = FontWeight.Bold,
                ),
            )
            BasicText(
                text = mistake.label,
                style = Theme.typography.body.small.copy(color = Theme.colors.primaryFont),
            )
            mistake.detail?.let { detail ->
                BasicText(
                    text = detail,
                    style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
                )
            }
        }
    }
}

/**
 * The passage with its mistakes marked.
 *
 * Coloured, bold **and** underlined: colour alone fails A11Y-01 and disappears entirely for a
 * red-green colour-blind reciter, which is precisely the person who most needs to find the word.
 */
@Composable
private fun List<CorrectionWordUi>.toAnnotatedText() = buildAnnotatedString {
    val mistakeStyle = SpanStyle(
        color = Theme.colors.error,
        fontWeight = FontWeight.Bold,
        textDecoration = TextDecoration.Underline,
    )
    forEachIndexed { index, word ->
        if (index > 0) append(' ')
        if (word.isMistake) withStyle(mistakeStyle) { append(word.text) } else append(word.text)
    }
}

@Composable
private fun AyahChip(label: String) {
    BasicText(
        text = label,
        style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
        modifier = Modifier
            .clip(Theme.shapes.small)
            .border(1.dp, Theme.colors.border, Theme.shapes.small)
            .padding(horizontal = Theme.spacing.small, vertical = 2.dp),
    )
}
