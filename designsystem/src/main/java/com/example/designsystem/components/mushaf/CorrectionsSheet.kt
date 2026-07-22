package com.example.designsystem.components.mushaf

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/**
 * A single mistake, named.
 *
 * One entry per mistaken **word**, not per āyah: an āyah can go wrong in several places and for
 * different reasons, and collapsing them to one label per āyah hides all but the first.
 *
 * @param detail the concrete explanation when there is one — "المد الطبيعي: المتوقع ٢، قرأت ٣".
 *   Null when the finding carries no rule or lengths, rather than inventing prose.
 */
data class CorrectionMistakeUi(
    val wordId: String,
    val word: String,
    val label: String,
    val detail: String? = null,
)

/** One āyah that contained at least one mistake. */
data class CorrectionCardUi(
    val id: String,
    val ayahLabel: String,
    /** The whole āyah, for context — a flagged word alone is unreadable. */
    val words: List<CorrectionWordUi>,
    val mistakes: List<CorrectionMistakeUi>,
)

/**
 * The session's mistakes, listed.
 *
 * Only confident mistakes appear here. Hints (`almost`) and unscored words are deliberately
 * absent from the *list* — the service softened those findings because it was not sure enough to
 * accuse, and a list is an accusation — though they still appear inside an āyah as context.
 *
 * Qur'an text comes from the service's Uthmani strings, which are ordinary Unicode Arabic rather
 * than the muṣḥaf's per-page glyph fonts, so it renders in the theme's Arabic face.
 */
@Composable
fun CorrectionsSheet(
    title: String,
    subtitle: String,
    corrections: List<CorrectionCardUi>,
    emptyMessage: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onMistakeClick: (String) -> Unit = {},
) {
    AppBottomSheet(onDismiss = onDismiss, modifier = modifier) {
        CorrectionsList(
            title = title,
            subtitle = subtitle,
            corrections = corrections,
            emptyMessage = emptyMessage,
            onMistakeClick = onMistakeClick,
        )
    }
}

/**
 * The sheet's body without the modal around it.
 *
 * Internal so previews can render the layout directly — `ModalBottomSheet` does not appear on the
 * preview surface, and the list is the thing worth reviewing.
 */
@Composable
internal fun CorrectionsList(
    title: String,
    subtitle: String,
    corrections: List<CorrectionCardUi>,
    emptyMessage: String,
    modifier: Modifier = Modifier,
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
            items(corrections, key = { it.id }) { correction ->
                CorrectionCard(correction = correction, onMistakeClick = onMistakeClick)
            }
        }
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
            imageVector = Icons.Outlined.ErrorOutline,
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
