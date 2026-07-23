package com.example.mushaf.presentation.settings.recite

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.placeholderscreens.NetworkErrorScreen
import com.example.designsystem.components.settings.SettingsItemRow
import com.example.designsystem.components.settings.SettingsSectionHeader
import com.example.designsystem.components.settings.SettingsSwitch
import com.example.designsystem.components.topbar.PlainTitleTopBar
import com.example.designsystem.theme.Theme
import com.example.mushaf.domain.model.recite.MoshafConstraints
import com.example.mushaf.domain.model.recite.MoshafFieldSpec
import com.example.mushaf.domain.model.recite.RecitationStrictness
import com.example.mushaf.domain.model.recite.TajweedRuleSpec
import com.example.mushaf.presentation.R
import org.koin.androidx.compose.koinViewModel
import com.example.designsystem.R as DesignSystemR


@Composable
fun ReciteSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ReciteSettingsViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    ReciteSettingsContent(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun ReciteSettingsContent(
    state: ReciteSettingsUiState,
    onIntent: (ReciteSettingsIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround),
    ) {
        PlainTitleTopBar(
            title = stringResource(R.string.recite_settings_title),
            onBackClick = onBack,
        )

        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = Theme.colors.primary) }

            state.isUnavailable -> NetworkErrorScreen(
                modifier = Modifier.fillMaxSize(),
                onRetry = { onIntent(ReciteSettingsIntent.Retry) },
            )

            else -> ReciteSettingsList(state = state, onIntent = onIntent)
        }
    }
}

@Composable
private fun ReciteSettingsList(
    state: ReciteSettingsUiState,
    onIntent: (ReciteSettingsIntent) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { SettingsSectionHeader(title = stringResource(R.string.recite_settings_engine)) }
        item { EngineSection(state = state, onIntent = onIntent) }

        item { SettingsSectionHeader(title = stringResource(R.string.recite_settings_sensitivity)) }
        item { StrictnessSection(state = state, onIntent = onIntent) }

        item { SettingsSectionHeader(title = stringResource(R.string.recite_settings_rules)) }
        item { RulesSection(state = state, onIntent = onIntent) }

        item { SettingsSectionHeader(title = stringResource(R.string.recite_settings_moshaf)) }
        items(state.commonMoshafFields, key = { it.key }) { field ->
            MoshafFieldRow(field = field, state = state, onIntent = onIntent)
        }

        item {
            ExpanderRow(
                title = stringResource(R.string.recite_settings_moshaf_advanced),
                expanded = state.isAdvancedMoshafExpanded,
                onClick = { onIntent(ReciteSettingsIntent.ToggleAdvancedMoshaf) },
            )
        }

        if (state.isAdvancedMoshafExpanded) {
            items(state.advancedMoshafFields, key = { it.key }) { field ->
                MoshafFieldRow(field = field, state = state, onIntent = onIntent)
            }
        }

        item {
            SectionCard {
                Text(
                    text = stringResource(R.string.recite_settings_moshaf_reset),
                    style = Theme.typography.body.large,
                    color = if (state.moshafSelection.isEmpty()) {
                        Theme.colors.hint
                    } else {
                        Theme.colors.error
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = state.moshafSelection.isNotEmpty()) {
                            onIntent(ReciteSettingsIntent.ResetMoshaf)
                        },
                )
                Hint(stringResource(R.string.recite_settings_moshaf_reset_hint))
            }
        }
    }
}

@Composable
private fun EngineSection(
    state: ReciteSettingsUiState,
    onIntent: (ReciteSettingsIntent) -> Unit,
) {
    SectionCard {
        state.engines.forEach { engine ->
            val corrects = engine.correctsRecitation
            OptionRow(
                title = stringResource(
                    if (corrects) R.string.recite_engine_teacher else R.string.recite_engine_follow,
                ),
                subtitle = stringResource(
                    if (corrects) {
                        R.string.recite_engine_teacher_hint
                    } else {
                        R.string.recite_engine_follow_hint
                    },
                ),
                selected = engine.isSelected,
                onClick = { onIntent(ReciteSettingsIntent.SelectEngine(engine.key)) },
            )
        }

        
        
        
        if (!state.engineCorrects) {
            Warning(stringResource(R.string.recite_engine_follow_warning))
        }
    }
}

@Composable
private fun StrictnessSection(
    state: ReciteSettingsUiState,
    onIntent: (ReciteSettingsIntent) -> Unit,
) {
    SectionCard {
        
        
        RecitationStrictness.entries.forEach { level ->
            OptionRow(
                title = stringResource(level.labelRes()),
                subtitle = stringResource(level.hintRes()),
                selected = state.strictness == level,
                enabled = state.engineCorrects,
                onClick = { onIntent(ReciteSettingsIntent.SelectStrictness(level)) },
            )
        }
        if (!state.engineCorrects) {
            Hint(stringResource(R.string.recite_sensitivity_disabled_hint))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RulesSection(
    state: ReciteSettingsUiState,
    onIntent: (ReciteSettingsIntent) -> Unit,
) {
    Column {
        SettingsItemRow(
            title = stringResource(R.string.recite_rules_grade_tajweed),
            subtitle = stringResource(R.string.recite_rules_grade_tajweed_hint),
            icon = painterResource(DesignSystemR.drawable.ic_tajweed),
            trailingContent = {
                SettingsSwitch(
                    checked = state.tajweedGradingEnabled && state.engineCorrects,
                    onCheckedChange = { onIntent(ReciteSettingsIntent.SetTajweedGrading(it)) },
                )
            },
        )

        val chipsEnabled = state.tajweedGradingEnabled && state.engineCorrects

        SectionCard {
            
            
            
            FilterPill(
                label = stringResource(R.string.recite_rules_all),
                selected = state.selectedRules == null,
                enabled = chipsEnabled,
                onClick = { onIntent(ReciteSettingsIntent.GradeAllRules) },
            )

            RuleGroup(
                title = stringResource(R.string.recite_rules_tajweed_group),
                rules = state.tajweedRules,
                state = state,
                enabled = chipsEnabled,
                onIntent = onIntent,
            )

            RuleGroup(
                title = stringResource(R.string.recite_rules_sifa_group),
                rules = state.sifaRules,
                
                
                enabled = chipsEnabled && state.engineCorrects,
                state = state,
                onIntent = onIntent,
            )

            if (state.selectedRules?.isEmpty() == true && chipsEnabled) {
                Hint(stringResource(R.string.recite_rules_none_hint))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RuleGroup(
    title: String,
    rules: List<TajweedRuleSpec>,
    state: ReciteSettingsUiState,
    enabled: Boolean,
    onIntent: (ReciteSettingsIntent) -> Unit,
) {
    if (rules.isEmpty()) return

    Text(
        text = title,
        style = Theme.typography.body.small,
        color = Theme.colors.secondaryFont,
        modifier = Modifier.padding(top = Theme.spacing.small),
    )

    FlowRow(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
        rules.forEach { rule ->
            FilterPill(
                label = rule.label,
                
                selected = state.selectedRules?.contains(rule.key) ?: true,
                enabled = enabled,
                onClick = { onIntent(ReciteSettingsIntent.ToggleRule(rule.key)) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoshafFieldRow(
    field: MoshafFieldSpec,
    state: ReciteSettingsUiState,
    onIntent: (ReciteSettingsIntent) -> Unit,
) {
    val current = state.moshafSelection[field.key]
        ?: MoshafConstraints.EFFECTIVE_SERVER_DEFAULTS[field.key]
        ?: field.default

    SectionCard {
        Text(
            text = field.label,
            style = Theme.typography.body.large,
            color = Theme.colors.primaryFont,
        )
        field.description?.let { Hint(it) }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
            modifier = Modifier.padding(top = Theme.spacing.small),
        ) {
            field.options.forEach { option ->
                FilterPill(
                    label = option.label,
                    selected = option.value == current,
                    enabled = true,
                    onClick = {
                        onIntent(ReciteSettingsIntent.SetMoshafValue(field.key, option.value))
                    },
                )
            }
        }
    }
}

@Composable
private fun ExpanderRow(
    title: String,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    SettingsItemRow(
        title = title,
        icon = painterResource(DesignSystemR.drawable.ic_book_open),
        onClick = onClick,
        trailingContent = {
            Icon(
                painter = painterResource(DesignSystemR.drawable.ic_chevron_end),
                contentDescription = null,
                tint = Theme.colors.hint,
                modifier = Modifier
                    .size(Theme.size.iconSemiMedium)
                    .rotate(if (expanded) -90f else 0f),
            )
        },
    )
}

@Composable
private fun OptionRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = Theme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = Theme.typography.body.large,
                color = if (enabled) Theme.colors.primaryFont else Theme.colors.hint,
            )
            subtitle?.let { Hint(it) }
        }

        if (selected) {
            Icon(
                painter = painterResource(DesignSystemR.drawable.ic_check),
                contentDescription = null,
                tint = if (enabled) Theme.colors.primary else Theme.colors.hint,
                modifier = Modifier.size(Theme.size.iconSemiMedium),
            )
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val background = when {
        !enabled -> Theme.colors.disable
        selected -> Theme.colors.primary
        else -> Theme.colors.field
    }
    val content = when {
        !enabled -> Theme.colors.hint
        selected -> Theme.colors.onPrimary
        else -> Theme.colors.secondaryFont
    }

    Text(
        text = label,
        style = Theme.typography.body.small,
        color = content,
        modifier = Modifier
            .padding(vertical = 2.dp)
            .background(background, RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.colors.surface)
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.small),
        content = content,
    )
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = Theme.typography.body.small,
        color = Theme.colors.hint,
        modifier = Modifier.padding(top = 2.dp),
    )
}

@Composable
private fun Warning(text: String) {
    Text(
        text = text,
        style = Theme.typography.body.small,
        color = Theme.colors.error,
        modifier = Modifier.padding(top = Theme.spacing.small),
    )
}

private fun RecitationStrictness.labelRes(): Int = when (this) {
    RecitationStrictness.LENIENT -> R.string.recite_sensitivity_lenient
    RecitationStrictness.NORMAL -> R.string.recite_sensitivity_normal
    RecitationStrictness.STRICT -> R.string.recite_sensitivity_strict
}

private fun RecitationStrictness.hintRes(): Int = when (this) {
    RecitationStrictness.LENIENT -> R.string.recite_sensitivity_lenient_hint
    RecitationStrictness.NORMAL -> R.string.recite_sensitivity_normal_hint
    RecitationStrictness.STRICT -> R.string.recite_sensitivity_strict_hint
}
