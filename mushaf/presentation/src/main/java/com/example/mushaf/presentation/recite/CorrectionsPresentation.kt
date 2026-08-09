package com.example.mushaf.presentation.recite

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.core.designsystem.locale.isArabicLocale
import com.example.designsystem.components.mushaf.CorrectionCardUi
import com.example.designsystem.components.mushaf.CorrectionFindingUi
import com.example.designsystem.components.mushaf.CorrectionMistakeUi
import com.example.designsystem.components.mushaf.CorrectionTabUi
import com.example.mushaf.domain.model.recite.MistakeCategory
import com.example.mushaf.domain.model.recite.PracticeFocus
import com.example.mushaf.domain.model.recite.SifaComparison
import com.example.mushaf.domain.model.recite.TajweedRuleReference
import com.example.mushaf.presentation.R
import com.example.mushaf.presentation.SurahNameResolver
import com.example.mushaf.presentation.state.LiveCorrectionUiState
import com.example.designsystem.components.mushaf.CorrectionWordUi as DesignCorrectionWordUi







 
@Composable
fun AyahCorrectionUi.toCard(): CorrectionCardUi = CorrectionCardUi(
    id = id,
    ayahLabel = stringResource(R.string.mushaf_ayah_label, sura, aya),
    words = words.map { DesignCorrectionWordUi(text = it.text, isMistake = it.isMistake) },
    mistakes = mistakes.map { mistake ->
        CorrectionMistakeUi(
            wordId = mistake.wordId,
            word = mistake.word,
            findings = mistake.findings.map { finding ->
                CorrectionFindingUi(
                    label = stringResource(finding.labelRes),
                    details = finding.detailLines(),
                )
            },
        )
    },
)

/**
 * Every detail the engine reported for one finding, as display lines.
 *
 * Each line is emitted only when its data is present, so a memorization slip shows nothing extra
 * while a madd error shows its rule, its lengths, the phonemes and the confidence. Rule name and
 * lengths collapse into one line when both exist — "المد الطبيعي: المتوقع ٢، قرأت ٣" reads better
 * than the same facts split across two rows.
 */
@Composable
private fun MistakeFindingUi.detailLines(): List<String> = buildList {
    val ruleName = rules.firstOrNull()?.displayName()

    when {
        ruleName != null && expectedLength != null && actualLength != null ->
            add(stringResource(R.string.mushaf_correction_detail, ruleName, expectedLength, actualLength))

        ruleName != null ->
            add(stringResource(R.string.mushaf_correction_rule, ruleName))

        expectedLength != null && actualLength != null ->
            add(stringResource(R.string.mushaf_correction_length, expectedLength, actualLength))
    }

    // A word can cite more than one rule; the first is folded into the line above.
    rules.drop(1).forEach { add(stringResource(R.string.mushaf_correction_rule, it.displayName())) }

    sifa?.let { add(it.describe()) }

    when {
        expectedPhonemes != null && predictedPhonemes != null ->
            add(stringResource(R.string.mushaf_correction_phonemes, expectedPhonemes, predictedPhonemes))

        expectedPhonemes != null ->
            add(stringResource(R.string.mushaf_correction_phonemes_expected, expectedPhonemes))

        predictedPhonemes != null ->
            add(stringResource(R.string.mushaf_correction_phonemes_heard, predictedPhonemes))
    }

    confidencePercent?.let { add(stringResource(R.string.mushaf_correction_confidence, it)) }
}

/** "الشدة والرخاوة: المتوقع شديدة، ونطقتها رخوة" — never `shidda_or_rakhawa=rikhw`. */
@Composable
private fun SifaComparison.describe(): String {
    val attribute = sifaText("mushaf_sifa_attr_", attributeKey)
    val expected = expectedValue?.let { sifaText("mushaf_sifa_value_", it) }
    val actual = actualValue?.let { sifaText("mushaf_sifa_value_", it) }

    return when {
        expected != null && actual != null ->
            stringResource(R.string.mushaf_correction_sifa, attribute, expected, actual)

        expected != null ->
            stringResource(R.string.mushaf_correction_sifa_expected, attribute, expected)

        actual != null ->
            stringResource(R.string.mushaf_correction_sifa_heard, attribute, actual)

        else -> attribute
    }
}

/**
 * Looks a ṣifā token up by resource name, so the vocabulary lives in strings.xml and a token the
 * engine adds later needs no code change. An unmapped token degrades to the token itself, which
 * is still far more readable than the `attribute=value` pair it came from.
 */
@Composable
private fun sifaText(prefix: String, token: String): String {
    val context = LocalContext.current
    val resName = prefix + token.lowercase().replace(NON_RESOURCE_CHARS, "_")
    val resId = remember(resName) {
        context.resources.getIdentifier(resName, "string", context.packageName)
    }
    return if (resId != 0) stringResource(resId) else token
}

private val NON_RESOURCE_CHARS = Regex("[^a-z0-9_]")

/** Tajwīd rules are named in Arabic; fall back to it when there is no English name. */
@Composable
private fun TajweedRuleReference.displayName(): String =
    if (isArabicLocale()) nameArabic else nameEnglish ?: nameArabic







 
@Composable
fun LiveCorrectionUiState.correctionsSubtitle(): String {
    val count = pluralStringResource(R.plurals.mushaf_corrections_count, mistakeCount, mistakeCount)
    val first = wordFeedback.values.minByOrNull { it.position.sura * 1000 + it.position.aya }
    val last = wordFeedback.values.maxByOrNull { it.position.sura * 1000 + it.position.aya }
    if (first == null || last == null) return count

    fun label(sura: Int, aya: Int) = "${SurahNameResolver.nameFor(sura)} $aya"
    val range = stringResource(
        R.string.mushaf_corrections_range,
        label(first.position.sura, first.position.aya),
        label(last.position.sura, last.position.aya),
    )
    return "$count · $range"
}


@Composable
fun PracticeFocus.label(): String {
    val name = ruleName ?: stringResource(
        when (category) {
            MistakeCategory.MEMORIZATION -> R.string.mushaf_correction_generic
            MistakeCategory.TASHKIL -> R.string.mushaf_correction_tashkeel
            MistakeCategory.TAJWID -> R.string.mushaf_correction_tajweed
            MistakeCategory.OTHER -> R.string.mushaf_correction_generic
        },
    )
    return stringResource(R.string.mushaf_practice_focus_item, name, occurrences)
}


@Composable
fun CorrectionTab.toChip(): CorrectionTabUi = CorrectionTabUi(
    label = stringResource(labelRes),
    count = count,
)
