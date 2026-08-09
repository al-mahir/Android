package com.example.mushaf.presentation.muallem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.locale.rememberLocaleLocals
import com.example.designsystem.R as DesignsystemR
import com.example.designsystem.components.bottomsheet.AppBottomSheet
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.tabs.TabSelector
import com.example.designsystem.theme.Theme
import com.example.mushaf.domain.model.SurahCatalog
import com.example.mushaf.domain.model.recite.RecitationStrictness
import com.example.mushaf.presentation.R

@Composable
fun MuallemSetupSheet(
    isOffline: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (surah: Int, startAyah: Int, endAyah: Int, repeatCount: Int, difficulty: RecitationStrictness) -> Unit,
) {
    var selectedSurah by remember { mutableIntStateOf(1) }
    var selectedStartAyah by remember { mutableIntStateOf(1) }
    var selectedEndAyah by remember { mutableIntStateOf(SurahCatalog.all.getOrNull(0)?.verseCount ?: 7) }
    var repeatCount by remember { mutableIntStateOf(3) }
    var selectedDifficulty by remember { mutableStateOf(RecitationStrictness.NORMAL) }

    val maxAyah = SurahCatalog.all.getOrNull(selectedSurah - 1)?.verseCount ?: 286

    // Ensure end ayah is always >= start ayah and <= max ayah
    if (selectedEndAyah < selectedStartAyah) {
        selectedEndAyah = selectedStartAyah
    }
    if (selectedEndAyah > maxAyah) {
        selectedEndAyah = maxAyah
    }

    AppBottomSheet(onDismiss = onDismiss) {
        Spacer(Modifier.height(Theme.spacing.small))

        // ── Title & Status ───────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = stringResource(R.string.muallem_setup_title),
                style = Theme.typography.title.copy(
                    color = Theme.colors.primaryFont,
                ),
            )
            
            // Connection Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isOffline) Theme.colors.error else Theme.colors.success)
                )
                BasicText(
                    text = stringResource(
                        if (isOffline) R.string.muallem_status_disconnected
                        else R.string.muallem_status_connected,
                    ),
                    style = Theme.typography.body.small.copy(
                        color = if (isOffline) Theme.colors.error else Theme.colors.success
                    )
                )
            }
        }

        Spacer(Modifier.height(Theme.spacing.medium))

        // ── Surah list ───────────────────────────────────────────────────────
        BasicText(
            text = stringResource(R.string.muallem_setup_surah_label),
            style = Theme.typography.body.medium.copy(color = Theme.colors.secondaryFont),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Theme.spacing.small))

        val listState = rememberLazyListState(initialFirstVisibleItemIndex = (selectedSurah - 1).coerceAtLeast(0))
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(Theme.shapes.small)
                .background(Theme.colors.surface)
                .border(1.dp, Theme.colors.border, Theme.shapes.small),
        ) {
            items(SurahCatalog.all, key = { it.number }) { surah ->
                val isSelected = surah.number == selectedSurah
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) Theme.colors.primary.copy(alpha = 0.12f) else Color.Transparent
                        )
                        .clickable {
                            selectedSurah = surah.number
                            selectedStartAyah = 1
                            selectedEndAyah = surah.verseCount
                        }
                        .padding(horizontal = Theme.spacing.medium, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
                ) {
                    BasicText(
                        text = "${surah.number}.",
                        style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
                        modifier = Modifier.width(30.dp),
                    )
                    BasicText(
                        text = surah.nameArabic,
                        style = Theme.typography.body.medium.copy(
                            color = if (isSelected) Theme.colors.primary else Theme.colors.primaryFont,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    BasicText(
                        text = surah.nameEnglish,
                        style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
                    )
                }
            }
        }

        Spacer(Modifier.height(Theme.spacing.medium))

        // ── Ayah Selection (Start & End) ─────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = stringResource(R.string.muallem_setup_ayah_label),
                    style = Theme.typography.body.medium.copy(color = Theme.colors.secondaryFont),
                )
                Spacer(Modifier.height(Theme.spacing.extraSmall))
                AyahDropdownMenu(
                    selectedValue = selectedStartAyah,
                    maxAyah = maxAyah,
                    onValueChange = { 
                        selectedStartAyah = it
                        if (selectedEndAyah < it) selectedEndAyah = it
                    }
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = stringResource(R.string.muallem_setup_end_ayah_label),
                    style = Theme.typography.body.medium.copy(color = Theme.colors.secondaryFont),
                )
                Spacer(Modifier.height(Theme.spacing.extraSmall))
                AyahDropdownMenu(
                    selectedValue = selectedEndAyah,
                    minAyah = selectedStartAyah,
                    maxAyah = maxAyah,
                    onValueChange = { selectedEndAyah = it }
                )
            }
        }

        Spacer(Modifier.height(Theme.spacing.medium))

        // ── Difficulty ───────────────────────────────────────────────────────
        BasicText(
            text = stringResource(R.string.muallem_setup_difficulty_label),
            style = Theme.typography.body.medium.copy(color = Theme.colors.secondaryFont),
        )
        Spacer(Modifier.height(Theme.spacing.extraSmall))

        val difficulties = listOf(
            RecitationStrictness.LENIENT to stringResource(R.string.muallem_difficulty_lenient),
            RecitationStrictness.NORMAL to stringResource(R.string.muallem_difficulty_normal),
            RecitationStrictness.STRICT to stringResource(R.string.muallem_difficulty_strict),
        )

        TabSelector(
            tabs = difficulties.map { it.second },
            selectedIndex = difficulties.indexOfFirst { it.first == selectedDifficulty }.coerceAtLeast(0),
            onTabSelected = { index ->
                selectedDifficulty = difficulties[index].first
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(Theme.spacing.medium))

        // ── Repeat count ─────────────────────────────────────────────────────
        BasicText(
            text = stringResource(R.string.muallem_setup_repeats_label),
            style = Theme.typography.body.medium.copy(color = Theme.colors.secondaryFont),
        )
        Spacer(Modifier.height(Theme.spacing.extraSmall))
        StepperRow(
            value = repeatCount,
            min = 1,
            max = 10,
            label = stringResource(R.string.muallem_setup_repeats_suffix),
            onDecrement = { if (repeatCount > 1) repeatCount-- },
            onIncrement = { if (repeatCount < 10) repeatCount++ },
        )

        Spacer(Modifier.height(Theme.spacing.large))

        // ── Confirm ──────────────────────────────────────────────────────────
        PrimaryButton(
            caption = stringResource(R.string.muallem_setup_start),
            onClick = { onConfirm(selectedSurah, selectedStartAyah, selectedEndAyah, repeatCount, selectedDifficulty) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Theme.spacing.medium))
    }
}

@Composable
private fun AyahDropdownMenu(
    selectedValue: Int,
    minAyah: Int = 1,
    maxAyah: Int,
    onValueChange: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    // DropdownMenu renders in its own Popup window, which resets the localized context installed
    // at the app root — without this the menu items resolve in the system locale.
    val localeLocals = rememberLocaleLocals()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Theme.shapes.small)
            .background(Theme.colors.surface)
            .border(1.dp, Theme.colors.border, Theme.shapes.small)
            .clickable { expanded = true }
            .padding(Theme.spacing.medium),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = stringResource(R.string.muallem_ayah_number, selectedValue),
            style = Theme.typography.body.medium.copy(color = Theme.colors.primaryFont)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Theme.colors.surface).height(300.dp)
        ) {
            localeLocals.Provide {
                for (i in minAyah..maxAyah) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.muallem_ayah_number, i),
                                color = if (i == selectedValue) Theme.colors.primary else Theme.colors.primaryFont
                            )
                        },
                        onClick = {
                            onValueChange(i)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepperRow(
    value: Int,
    min: Int,
    max: Int,
    label: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Theme.shapes.small)
            .background(Theme.colors.surface)
            .border(1.dp, Theme.colors.border, Theme.shapes.small)
            .padding(horizontal = Theme.spacing.small, vertical = Theme.spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onDecrement, enabled = value > min) {
            BasicText(
                text = "-",
                style = Theme.typography.title.copy(
                    color = if (value > min) Theme.colors.primary else Theme.colors.disable
                )
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                text = value.toString(),
                style = Theme.typography.title.copy(
                    color = Theme.colors.primaryFont,
                    textAlign = TextAlign.Center,
                ),
            )
            BasicText(
                text = label,
                style = Theme.typography.body.small.copy(
                    color = Theme.colors.secondaryFont,
                    textAlign = TextAlign.Center,
                ),
            )
        }

        IconButton(onClick = onIncrement, enabled = value < max) {
            BasicText(
                text = "+",
                style = Theme.typography.title.copy(
                    color = if (value < max) Theme.colors.primary else Theme.colors.disable
                )
            )
        }
    }
}
