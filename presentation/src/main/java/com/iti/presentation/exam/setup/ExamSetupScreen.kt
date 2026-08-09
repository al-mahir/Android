package com.iti.presentation.exam.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.button.SecondaryButton
import com.example.designsystem.components.card.InnerContentCard
import com.example.designsystem.components.section.SectionHeader
import com.example.designsystem.components.tabs.TabSelector
import com.example.designsystem.components.topbar.BackTitleTopBar
import com.example.designsystem.theme.Theme
import com.iti.domain.model.exam.ExamScope
import com.iti.domain.model.quran.QuranIndex
import com.iti.domain.model.quran.SurahNames
import com.iti.presentation.R
import com.iti.presentation.core.mvi.ObserveEffect
import com.iti.presentation.exam.setup.state.ExamSetupEffect
import com.iti.presentation.exam.setup.state.ExamSetupIntent
import com.iti.presentation.exam.setup.state.ExamSetupUiState
import com.iti.presentation.exam.setup.state.SetupTab
import org.koin.androidx.compose.koinViewModel

@Composable
fun ExamSetupScreen(
    viewModel: ExamSetupViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToSession: (scope: ExamScope, count: Int, lines: Int) -> Unit,
    onNavigateToSummary: (summaryId: String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            ExamSetupEffect.NavigateBack -> onNavigateBack()
            is ExamSetupEffect.NavigateToSummary -> onNavigateToSummary(effect.summaryId)
            is ExamSetupEffect.NavigateToSession -> onNavigateToSession(
                effect.scope,
                effect.questionCount,
                effect.linesPerQuestion
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.backGround)
    ) {
        BackTitleTopBar(
            title = stringResource(R.string.exam_setup_title),
            onBackClick = { viewModel.onIntent(ExamSetupIntent.BackClicked) }
        )

        Crossfade(targetState = state.isCreatingNewRange, label = "SetupViewTransition") { isCreating ->
            if (isCreating) {
                ExamCreateRangeView(state = state, onIntent = viewModel::onIntent)
            } else {
                ExamDashboardView(state = state, onIntent = viewModel::onIntent)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dashboard View
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExamDashboardView(
    state: ExamSetupUiState,
    onIntent: (ExamSetupIntent) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp, top = Theme.spacing.medium)
        ) {

            // -- Your Ranges (Custom Scopes) --
            item {
                SectionHeader(
                    title = stringResource(R.string.exam_setup_my_ranges),
                    modifier = Modifier.padding(horizontal = Theme.spacing.medium)
                )
            }

            items(state.customScopes) { scope ->
                RangeSelectionItem(
                    title = scope.name,
                    isSelected = state.selectedScopeId == scope.id,
                    onClick = { onIntent(ExamSetupIntent.ScopeSelected(scope.id, scope)) },
                    onDelete = { onIntent(ExamSetupIntent.DeleteCustomScopeClicked(scope.id)) }
                )
            }

            item {
                SecondaryButton(
                    caption = stringResource(R.string.exam_setup_create_new_range),
                    onClick = { onIntent(ExamSetupIntent.ToggleCreateRangeView) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Theme.spacing.medium)
                )
            }
        }

        // -- Bottom Fixed Start Button --
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Theme.colors.surface.copy(alpha = 0.95f))
                .padding(Theme.spacing.medium)
        ) {
            PrimaryButton(
                caption = stringResource(R.string.exam_start_button),
                onClick = { onIntent(ExamSetupIntent.StartClicked) },
                isDisabled = !state.canStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
        }
    }
}

@Composable
private fun RangeSelectionItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val borderColor = if (isSelected) Theme.colors.primary else androidx.compose.ui.graphics.Color.Transparent
    val borderWidth = if (isSelected) 2.dp else 0.dp
    val bgColor = if (isSelected) Theme.colors.primary.copy(alpha = 0.05f) else Theme.colors.surface

    InnerContentCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.extraSmall)
            .clip(Theme.shapes.medium)
            .border(borderWidth, borderColor, Theme.shapes.medium)
            .background(bgColor)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Selected",
                        tint = Theme.colors.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 8.dp)
                    )
                }
                Text(
                    text = title,
                    style = Theme.typography.body.large.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = Theme.colors.onSurface
                )
            }

            if (onDelete != null) {
                Text(
                    text = stringResource(R.string.exam_setup_delete),
                    style = Theme.typography.body.small,
                    color = Theme.colors.error,
                    modifier = Modifier
                        .clickable(onClick = onDelete)
                        .padding(4.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Range Creation Builder
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExamCreateRangeView(
    state: ExamSetupUiState,
    onIntent: (ExamSetupIntent) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // 1. The Tabs & Content Selection
            item {
                ScopeSelectionSection(
                    currentTab = state.currentTab,
                    selectedSurah = state.selectedSurah,
                    selectedJuz = state.selectedJuz,
                    selectedRub = state.selectedRub,
                    selectedAyahRange = state.selectedAyahRange,
                    state = state,
                    onIntent = onIntent
                )
            }

            // 2. Add Component Button
            item {
                OutlinedButton(
                    onClick = { onIntent(ExamSetupIntent.AddCurrentScopeToCustomClicked) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Theme.spacing.medium),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Theme.colors.primary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Theme.colors.primary)
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.exam_setup_add_to_selection))
                }
                Spacer(modifier = Modifier.height(Theme.spacing.medium))
            }

            // 3. Accumulated List of selected scopes
            item {
                AnimatedVisibility(
                    visible = state.customScopeComponents.isNotEmpty(),
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(horizontal = Theme.spacing.medium)) {
                        SectionHeader(title = stringResource(R.string.exam_setup_added_to_range), modifier = Modifier.padding(bottom = Theme.spacing.small))

                        state.customScopeComponents.forEachIndexed { index, scope ->
                            SelectedComponentItem(scope = scope, onRemove = { onIntent(ExamSetupIntent.RemoveScopeFromCustomClicked(index)) })
                        }
                        Spacer(modifier = Modifier.height(Theme.spacing.medium))
                    }
                }
            }

            // 4. Range Name Input (Required)
            item {
                OutlinedTextField(
                    value = state.customScopeName,
                    onValueChange = { onIntent(ExamSetupIntent.CustomScopeNameChanged(it)) },
                    label = { Text(stringResource(R.string.exam_setup_range_name_required)) },
                    isError = state.customScopeName.isBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Theme.spacing.medium)
                )
            }

            // 5. Expose the settings sliders directly here so the user can tweak it before saving
            item {
                Spacer(modifier = Modifier.height(Theme.spacing.medium))
                QuestionCountSection(
                    state = state,
                    onCountChanged = { onIntent(ExamSetupIntent.QuestionCountChanged(it)) }
                )
                LinesCountSection(
                    state = state,
                    onCountChanged = { onIntent(ExamSetupIntent.LinesCountChanged(it)) }
                )
            }
        }

        // -- Bottom Fixed Add Button --
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Theme.colors.surface.copy(alpha = 0.95f))
                .padding(Theme.spacing.medium)
        ) {
            PrimaryButton(
                caption = stringResource(R.string.exam_setup_add_range),
                onClick = { onIntent(ExamSetupIntent.SaveCustomScopeClicked) },
                isDisabled = state.customScopeName.isBlank() || state.isInvalidRange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
        }
    }
}

@Composable
private fun SelectedComponentItem(scope: ExamScope, onRemove: () -> Unit) {
    val label = when (scope) {
        is ExamScope.SingleSurah -> SurahNames.nameOf(scope.surahNumber) ?: "سورة ${scope.surahNumber}"
        is ExamScope.SingleJuz -> "جزء ${scope.juzNumber}"
        is ExamScope.SingleRub -> "الربع ${scope.rubNumber}"
        is ExamScope.AyahRange -> "سورة ${SurahNames.nameOf(scope.startSurah)} آية ${scope.startAyah} - سورة ${SurahNames.nameOf(scope.endSurah)} آية ${scope.endAyah}"
        else -> "نطاق"
    }

    InnerContentCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Theme.spacing.extraSmall)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = Theme.typography.body.large, color = Theme.colors.onSurface)
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Remove",
                    tint = Theme.colors.error
                )
            }
        }
    }
}

@Composable
private fun ScopeSelectionSection(
    currentTab: SetupTab,
    selectedSurah: Int,
    selectedJuz: Int,
    selectedRub: Int,
    selectedAyahRange: Pair<Pair<Int, Int>, Pair<Int, Int>>,
    state: ExamSetupUiState,
    onIntent: (ExamSetupIntent) -> Unit,
) {
    Column(modifier = Modifier.padding(Theme.spacing.medium)) {
        TabSelector(
            tabs = listOf(
                stringResource(R.string.exam_tab_surah),
                stringResource(R.string.exam_tab_juz),
                stringResource(R.string.exam_tab_rub),
                stringResource(R.string.exam_tab_ayah)
            ),
            selectedIndex = currentTab.ordinal,
            onTabSelected = { onIntent(ExamSetupIntent.TabSelected(SetupTab.values()[it])) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(Theme.spacing.medium))
        InnerContentCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(Theme.spacing.medium)) {
                when (currentTab) {
                    SetupTab.SURAH -> {
                        SelectionDropdown(
                            label = stringResource(R.string.exam_setup_select_surah),
                            items = (1..114).toList(),
                            selectedItem = selectedSurah,
                            itemLabel = { SurahNames.nameOf(it) ?: "سورة $it" },
                            onItemSelected = { onIntent(ExamSetupIntent.SurahSelected(it)) }
                        )
                    }
                    SetupTab.JUZ -> {
                        SelectionDropdown(
                            label = stringResource(R.string.exam_setup_select_juz),
                            items = (1..30).toList(),
                            selectedItem = selectedJuz,
                            itemLabel = { "الجزء $it" },
                            onItemSelected = { onIntent(ExamSetupIntent.JuzSelected(it)) }
                        )
                    }
                    SetupTab.RUB -> {
                        SelectionDropdown(
                            label = stringResource(R.string.exam_setup_select_rub),
                            items = (1..240).toList(),
                            selectedItem = selectedRub,
                            itemLabel = { "الربع $it" },
                            onItemSelected = { onIntent(ExamSetupIntent.RubSelected(it)) }
                        )
                    }
                    SetupTab.AYAH -> {
                        val startSurah = selectedAyahRange.first.first
                        val startAyah = selectedAyahRange.first.second
                        val endSurah = selectedAyahRange.second.first
                        val endAyah = selectedAyahRange.second.second

                        Text(stringResource(R.string.exam_setup_from), style = Theme.typography.body.large, color = Theme.colors.onSurface)
                        Spacer(modifier = Modifier.height(Theme.spacing.small))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)
                        ) {
                            SelectionDropdown(
                                label = stringResource(R.string.exam_setup_surah),
                                items = (1..114).toList(),
                                selectedItem = startSurah,
                                itemLabel = { SurahNames.nameOf(it) ?: "سورة $it" },
                                onItemSelected = {
                                    val count = QuranIndex.ayahCountOf(it)
                                    val newAyah = startAyah.coerceAtMost(count)
                                    onIntent(ExamSetupIntent.AyahRangeSelected(it to newAyah, selectedAyahRange.second))
                                },
                                modifier = Modifier.weight(1f)
                            )
                            SelectionDropdown(
                                label = stringResource(R.string.exam_setup_ayah),
                                items = (1..QuranIndex.ayahCountOf(startSurah)).toList(),
                                selectedItem = startAyah,
                                itemLabel = { "$it" },
                                onItemSelected = {
                                    onIntent(ExamSetupIntent.AyahRangeSelected(startSurah to it, selectedAyahRange.second))
                                },
                                modifier = Modifier.weight(0.5f)
                            )
                        }

                        Spacer(modifier = Modifier.height(Theme.spacing.medium))
                        Text(stringResource(R.string.exam_setup_to), style = Theme.typography.body.large, color = Theme.colors.onSurface)
                        Spacer(modifier = Modifier.height(Theme.spacing.small))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)
                        ) {
                            SelectionDropdown(
                                label = stringResource(R.string.exam_setup_surah),
                                items = (1..114).toList(),
                                selectedItem = endSurah,
                                itemLabel = { SurahNames.nameOf(it) ?: "سورة $it" },
                                onItemSelected = {
                                    val count = QuranIndex.ayahCountOf(it)
                                    val newAyah = endAyah.coerceAtMost(count)
                                    onIntent(ExamSetupIntent.AyahRangeSelected(selectedAyahRange.first, it to newAyah))
                                },
                                modifier = Modifier.weight(1f)
                            )
                            SelectionDropdown(
                                label = stringResource(R.string.exam_setup_ayah),
                                items = (1..QuranIndex.ayahCountOf(endSurah)).toList(),
                                selectedItem = endAyah,
                                itemLabel = { "$it" },
                                onItemSelected = {
                                    onIntent(ExamSetupIntent.AyahRangeSelected(selectedAyahRange.first, endSurah to it))
                                },
                                modifier = Modifier.weight(0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SelectionDropdown(
    label: String,
    items: List<T>,
    selectedItem: T,
    itemLabel: (T) -> String,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = itemLabel(selectedItem),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                unfocusedBorderColor = Theme.colors.border,
                focusedBorderColor = Theme.colors.primary,
                unfocusedTextColor = Theme.colors.onSurface,
                focusedTextColor = Theme.colors.onSurface
            ),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Theme.colors.surface)
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemLabel(item), color = Theme.colors.onSurface) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun QuestionCountSection(
    state: ExamSetupUiState,
    onCountChanged: (Int) -> Unit
) {
    val range = state.questionCountRange
    val currentCount = state.selectedQuestionCount
    Column(modifier = Modifier.padding(horizontal = Theme.spacing.medium)) {
        SectionHeader(title = stringResource(R.string.exam_question_count_label))

        if (state.isInvalidRange) {
            Text(
                text = stringResource(R.string.exam_scope_invalid_range),
                style = Theme.typography.body.medium,
                color = Theme.colors.error,
                modifier = Modifier.padding(vertical = Theme.spacing.small)
            )
        } else if (range.isEmpty()) {
            Text(
                text = stringResource(R.string.exam_scope_too_small),
                style = Theme.typography.body.medium,
                color = Theme.colors.error,
                modifier = Modifier.padding(vertical = Theme.spacing.small)
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${range.first}",
                    style = Theme.typography.body.medium,
                    color = Theme.colors.hint
                )
                Slider(
                    value = currentCount.toFloat(),
                    onValueChange = { onCountChanged(it.toInt()) },
                    valueRange = range.first.toFloat()..range.last.toFloat(),
                    steps = if (range.last - range.first > 0) range.last - range.first - 1 else 0,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Theme.spacing.medium),
                    colors = SliderDefaults.colors(
                        thumbColor = Theme.colors.primary,
                        activeTrackColor = Theme.colors.primary,
                        inactiveTrackColor = Theme.colors.surfaceVariant
                    )
                )
                Text(
                    text = "${range.last}",
                    style = Theme.typography.body.medium,
                    color = Theme.colors.hint
                )
            }
            Text(
                text = "$currentCount",
                style = Theme.typography.title,
                color = Theme.colors.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LinesCountSection(
    state: ExamSetupUiState,
    onCountChanged: (Int) -> Unit
) {
    val currentCount = state.selectedLinesPerQuestion
    Column(modifier = Modifier.padding(horizontal = Theme.spacing.medium)) {
        SectionHeader(title = stringResource(R.string.exam_setup_lines_per_question))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "1",
                style = Theme.typography.body.medium,
                color = Theme.colors.hint
            )
            Slider(
                value = currentCount.toFloat(),
                onValueChange = { onCountChanged(it.toInt()) },
                valueRange = 1f..12f,
                steps = 10,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Theme.spacing.medium),
                colors = SliderDefaults.colors(
                    thumbColor = Theme.colors.primary,
                    activeTrackColor = Theme.colors.primary,
                    inactiveTrackColor = Theme.colors.surfaceVariant
                )
            )
            Text(
                text = "12",
                style = Theme.typography.body.medium,
                color = Theme.colors.hint
            )
        }
        Text(
            text = "$currentCount",
            style = Theme.typography.title,
            color = Theme.colors.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}