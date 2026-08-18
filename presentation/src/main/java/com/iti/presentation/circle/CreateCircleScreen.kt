package com.iti.presentation.circle

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.button.ButtonHeightCompact
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.section.SectionHeader
import com.example.designsystem.components.textfield.TextField
import com.example.designsystem.components.topbar.BackTitleTopBar
import com.example.designsystem.theme.Theme
import com.iti.presentation.R
import com.iti.presentation.circle.state.CreateCircleEffect
import com.iti.presentation.circle.state.CreateCircleIntent
import com.iti.presentation.circle.state.CreateCirclePrivacyType
import com.iti.presentation.circle.state.CreateCircleUiState
import com.iti.presentation.core.mvi.ObserveEffect
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Compact height of the app bar content (below the status bar) on this screen. */
private val CreateCircleTopBarHeight: Dp = 64.dp

@Composable
fun CreateCircleScreen(
    onBack: () -> Unit,
    onCircleCreated: (circleId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateCircleViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingCreated by remember { mutableStateOf<Triple<String, String, String?>?>(null) }

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            is CreateCircleEffect.CircleCreated -> onCircleCreated(effect.circleId)
            is CreateCircleEffect.ShowCreatedCircle ->
                pendingCreated = Triple(effect.circleId, effect.password, effect.token)
            is CreateCircleEffect.ShowMessage ->
                Toast.makeText(context, effect.messageRes, Toast.LENGTH_SHORT).show()
        }
    }

    CreateCircleContent(
        state = state,
        onBack = onBack,
        onTitleChanged = { viewModel.onIntent(CreateCircleIntent.TitleChanged(it)) },
        onGoalsChanged = { viewModel.onIntent(CreateCircleIntent.GoalsChanged(it)) },
        onPrivacySelected = { viewModel.onIntent(CreateCircleIntent.PrivacySelected(it)) },
        onPasswordChanged = { viewModel.onIntent(CreateCircleIntent.PasswordChanged(it)) },
        onStartDateChanged = { viewModel.onIntent(CreateCircleIntent.StartDateChanged(it)) },
        onEndDateChanged = { viewModel.onIntent(CreateCircleIntent.EndDateChanged(it)) },
        onSubmit = { viewModel.onIntent(CreateCircleIntent.Submit) },
        modifier = modifier,
    )

    pendingCreated?.let { (circleId, password, token) ->
        CreatedCircleDialog(
            circleId = circleId,
            password = password,
            token = token,
            onDone = {
                pendingCreated = null
                onCircleCreated(circleId)
            },
        )
    }
}

@Composable
private fun CreateCircleContent(
    state: CreateCircleUiState,
    onBack: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onGoalsChanged: (String) -> Unit,
    onPrivacySelected: (CreateCirclePrivacyType) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onStartDateChanged: (String) -> Unit,
    onEndDateChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround),
    ) {
        BackTitleTopBar(
            title = stringResource(R.string.create_circle_title),
            onBackClick = onBack,
            height = CreateCircleTopBarHeight,
            extendsUnderStatusBar = true,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = CreateCircleTopBarHeight)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Hero header
            CreateCircleHeader()

            // Role info banner — regular users can only create PRIVATE circles
            UserRoleBanner()

            // Circle details
            SectionHeader(title = stringResource(R.string.create_circle_details_section))
            TextField(
                text = state.title,
                onTextChange = onTitleChanged,
                title = stringResource(R.string.create_circle_title_label),
                hint = stringResource(R.string.create_circle_title_hint),
                isError = state.titleError,
                errorMessage = if (state.titleError) {
                    stringResource(R.string.create_circle_error_title_required)
                } else {
                    null
                },
                singleLine = true,
            )
            TextField(
                text = state.goals,
                onTextChange = onGoalsChanged,
                title = stringResource(R.string.create_circle_goals_label),
                hint = stringResource(R.string.create_circle_goals_hint),
                minLines = 3,
                maxLines = 6,
                fieldHeight = 120.dp,
                fieldVerticalAlignment = Alignment.Top,
            )
            ScheduleDateField(
                title = stringResource(R.string.create_circle_start_date_label),
                value = state.startDate,
                hint = stringResource(R.string.create_circle_start_date_hint),
                isError = state.startDateError,
                errorMessage = if (state.startDateError) {
                    stringResource(R.string.create_circle_error_start_date)
                } else {
                    null
                },
                onValueChange = onStartDateChanged,
            )
            ScheduleDateField(
                title = stringResource(R.string.create_circle_end_date_label),
                value = state.endDate,
                hint = stringResource(R.string.create_circle_end_date_hint),
                isError = state.endDateError,
                errorMessage = if (state.endDateError) {
                    stringResource(R.string.create_circle_error_end_date)
                } else {
                    null
                },
                onValueChange = onEndDateChanged,
            )

            // Privacy selector (user can only choose PRIVATE; PUBLIC is disabled)
            SectionHeader(title = stringResource(R.string.create_circle_privacy_label))
            PrivacySelector(
                selected = state.selectedType,
                onSelected = onPrivacySelected,
            )

            // Password field (only for PRIVATE)
            AnimatedVisibility(visible = state.selectedType == CreateCirclePrivacyType.PRIVATE) {
                TextField(
                    text = state.password,
                    onTextChange = onPasswordChanged,
                    title = stringResource(R.string.create_circle_password_label),
                    hint = stringResource(R.string.create_circle_password_hint),
                    isError = state.passwordError,
                    errorMessage = if (state.passwordError) {
                        stringResource(R.string.create_circle_error_password_required)
                    } else {
                        null
                    },
                    singleLine = true,
                )
            }

            Spacer(modifier = Modifier.height(96.dp))
        }

        // CTA
        PrimaryButton(
            caption = stringResource(R.string.create_circle_cta),
            onClick = onSubmit,
            isLoading = state.isCreating,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        )
    }
}

/** Hero header: icon badge + short intro line under the app bar. */
@Composable
private fun CreateCircleHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(Theme.size.iconContainer)
                .clip(CircleShape)
                .background(Theme.colors.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Groups,
                contentDescription = null,
                tint = Theme.colors.primary,
                modifier = Modifier.size(Theme.size.iconLarge),
            )
        }
        BasicText(
            text = stringResource(R.string.create_circle_subtitle),
            style = Theme.typography.body.large.copy(color = Theme.colors.secondaryFont),
        )
    }
}

/** Tinted info banner: regular users may only create PRIVATE circles. */
@Composable
private fun UserRoleBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Theme.shapes.large)
            .background(Theme.colors.primary.copy(alpha = 0.08f))
            .border(1.dp, Theme.colors.primary.copy(alpha = 0.25f), Theme.shapes.large)
            .padding(horizontal = Theme.spacing.medium, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = Theme.colors.primary,
            modifier = Modifier.size(Theme.size.iconSemiMedium),
        )
        BasicText(
            text = stringResource(R.string.create_circle_user_role_note),
            style = Theme.typography.body.small.copy(color = Theme.colors.primary),
            modifier = Modifier.weight(1f),
        )
    }
}

/** Stacked selectable cards for PRIVATE / PUBLIC. PUBLIC is disabled for regular users. */
@Composable
private fun PrivacySelector(
    selected: CreateCirclePrivacyType,
    onSelected: (CreateCirclePrivacyType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PrivacyOption(
            title = stringResource(R.string.create_circle_privacy_private),
            description = stringResource(R.string.create_circle_privacy_private_desc),
            icon = Icons.Outlined.Lock,
            selected = selected == CreateCirclePrivacyType.PRIVATE,
            enabled = true,
            onClick = { onSelected(CreateCirclePrivacyType.PRIVATE) },
        )
        PrivacyOption(
            title = stringResource(R.string.create_circle_privacy_public),
            description = stringResource(R.string.create_circle_privacy_public_desc),
            icon = Icons.Outlined.Public,
            selected = selected == CreateCirclePrivacyType.PUBLIC,
            // Public circles are Sheikh-only; disabled for regular users
            enabled = false,
            badge = stringResource(R.string.create_circle_privacy_public_badge),
            onClick = {},
        )
    }
}

@Composable
private fun PrivacyOption(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    val background = when {
        !enabled -> Theme.colors.disable
        selected -> Theme.colors.primary.copy(alpha = 0.10f)
        else -> Theme.colors.surface
    }
    val borderColor = when {
        selected && enabled -> Theme.colors.primary
        else -> Theme.colors.border
    }
    val iconTint = when {
        !enabled -> Theme.colors.onDisable
        selected -> Theme.colors.primary
        else -> Theme.colors.secondaryFont
    }
    val titleColor = when {
        !enabled -> Theme.colors.onDisable
        selected -> Theme.colors.primary
        else -> Theme.colors.primaryFont
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Theme.shapes.large)
            .background(background)
            .border(1.dp, borderColor, Theme.shapes.large)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = Theme.spacing.medium, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(Theme.size.avatarSmall)
                .clip(CircleShape)
                .background(Theme.colors.primary.copy(alpha = if (enabled) 0.12f else 0.05f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(Theme.size.iconMedium),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BasicText(
                    text = title,
                    style = Theme.typography.body.medium.copy(
                        color = titleColor,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                badge?.let {
                    BasicText(
                        text = it,
                        style = Theme.typography.body.small.copy(color = Theme.colors.primary),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Theme.colors.primary.copy(alpha = 0.10f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            BasicText(
                text = description,
                style = Theme.typography.body.small.copy(
                    color = if (enabled) Theme.colors.secondaryFont else Theme.colors.onDisable,
                ),
            )
        }
        PrivacyRadioDot(selected = selected && enabled, enabled = enabled)
    }
}

/** Trailing selection dot for [PrivacyOption]. */
@Composable
private fun PrivacyRadioDot(selected: Boolean, enabled: Boolean) {
    val borderColor = when {
        !enabled -> Theme.colors.onDisable.copy(alpha = 0.4f)
        selected -> Theme.colors.primary
        else -> Theme.colors.hint
    }
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .border(2.dp, borderColor, CircleShape)
            .background(if (selected && enabled) Theme.colors.primary else Theme.colors.backGround),
        contentAlignment = Alignment.Center,
    ) {
        if (selected && enabled) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Theme.colors.onPrimary),
            )
        }
    }
}

/** Success popup after creating a PRIVATE circle — shows the session id + access code to share. */
@Composable
private fun CreatedCircleDialog(
    circleId: String,
    password: String,
    token: String?,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val copySessionIdLabel = stringResource(R.string.create_circle_success_session_label)
    val copyPasswordLabel = stringResource(R.string.create_circle_success_password_label)
    val copyTokenLabel = stringResource(R.string.create_circle_success_token_label)

    fun copyToClipboard(label: String, value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(context, R.string.create_circle_copied, Toast.LENGTH_SHORT).show()
    }

    AlertDialog(
        onDismissRequest = { /* Success popup — dismiss only via the Done button. */ },
        containerColor = Theme.colors.surface,
        shape = Theme.shapes.extraLarge,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(Theme.size.iconContainer)
                        .clip(CircleShape)
                        .background(Theme.colors.success.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Theme.colors.success,
                        modifier = Modifier.size(Theme.size.iconLarge),
                    )
                }
                BasicText(
                    text = stringResource(R.string.create_circle_success_title),
                    style = Theme.typography.title.copy(
                        color = Theme.colors.primaryFont,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BasicText(
                    text = stringResource(R.string.create_circle_success_message),
                    style = Theme.typography.body.medium.copy(
                        color = Theme.colors.secondaryFont,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Theme.colors.border),
                )
                CreatedValueRow(
                    label = stringResource(R.string.create_circle_success_session_label),
                    value = circleId,
                    onCopy = { copyToClipboard(copySessionIdLabel, circleId) },
                )
                CreatedValueRow(
                    label = stringResource(R.string.create_circle_success_password_label),
                    value = password,
                    onCopy = { copyToClipboard(copyPasswordLabel, password) },
                )
                if (!token.isNullOrBlank()) {
                    CreatedValueRow(
                        label = stringResource(R.string.create_circle_success_token_label),
                        value = token,
                        onCopy = { copyToClipboard(copyTokenLabel, token) },
                    )
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                caption = stringResource(R.string.create_circle_success_done),
                onClick = onDone,
                height = ButtonHeightCompact,
                shape = Theme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

/** A labelled value row with a trailing copy button, shown inside [CreatedCircleDialog]. */
@Composable
private fun CreatedValueRow(
    label: String,
    value: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BasicText(
            text = label,
            style = Theme.typography.body.small.copy(
                color = Theme.colors.secondaryFont,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Theme.shapes.medium)
                .background(Theme.colors.backGround)
                .border(1.dp, Theme.colors.border, Theme.shapes.medium)
                .padding(start = Theme.spacing.medium, top = 6.dp, bottom = 6.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = value,
                style = Theme.typography.body.medium.copy(
                    color = Theme.colors.primaryFont,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.create_circle_copy),
                    tint = Theme.colors.primary,
                    modifier = Modifier.size(Theme.size.iconSemiMedium),
                )
            }
        }
    }
}

/**
 * Read-only date &amp; time field. Tapping it opens a Material 3 date picker followed by a
 * time picker; the chosen local value is stored back as an ISO-8601 UTC string.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ScheduleDateField(
    title: String,
    value: String,
    hint: String,
    isError: Boolean,
    errorMessage: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }

    val current = remember(value) {
        runCatching { Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDateTime() }
            .getOrNull()
    }

    val locale = LocalConfiguration.current.locales[0]
    val displayFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(locale)
    }
    val displayText = current?.let(displayFormatter::format) ?: value

    TextField(
        text = displayText,
        onTextChange = {},
        title = title,
        hint = hint,
        isError = isError,
        errorMessage = errorMessage,
        singleLine = true,
        readOnly = true,
        trailingIcon = rememberVectorPainter(Icons.Outlined.CalendarMonth),
        onClickTrailingIcon = { showDatePicker = true },
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ) { showDatePicker = true },
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (current?.toLocalDate() ?: LocalDate.now().plusDays(1))
                .toEpochMillis(),
        )
        MaterialTheme(colorScheme = brandMaterialColorScheme()) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val millis = datePickerState.selectedDateMillis
                            if (millis != null) {
                                pendingDate = millis.toLocalDate()
                                showDatePicker = false
                                showTimePicker = true
                            }
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.create_circle_date_next),
                            color = Theme.colors.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(
                            text = stringResource(R.string.create_circle_cancel),
                            color = Theme.colors.secondaryFont,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = current?.hour ?: 9,
            initialMinute = current?.minute ?: 0,
            is24Hour = true,
        )
        MaterialTheme(colorScheme = brandMaterialColorScheme()) {
            TimePickerDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val date = pendingDate ?: current?.toLocalDate() ?: LocalDate.now().plusDays(1)
                            val local = LocalDateTime.of(
                                date,
                                LocalTime.of(timePickerState.hour, timePickerState.minute),
                            )
                            onValueChange(local.atZone(ZoneId.systemDefault()).toInstant().toString())
                            showTimePicker = false
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.create_circle_time_done),
                            color = Theme.colors.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                title = {
                    BasicText(
                        text = stringResource(R.string.create_circle_time_label),
                        style = Theme.typography.body.medium.copy(
                            color = Theme.colors.primaryFont,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, top = 16.dp, end = 24.dp),
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text(
                            text = stringResource(R.string.create_circle_cancel),
                            color = Theme.colors.secondaryFont,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
            ) {
                TimePicker(state = timePickerState)
            }
        }
    }
}

/** Material 3 color scheme mapped from the app's brand tokens (light or dark as active). */
@Composable
private fun brandMaterialColorScheme(): androidx.compose.material3.ColorScheme {
    val c = Theme.colors
    val dark = c.backGround.luminance() < 0.5f
    return if (dark) {
        darkColorScheme(
            primary = c.primary,
            onPrimary = c.onPrimary,
            primaryContainer = c.primaryContainer,
            onPrimaryContainer = c.onPrimaryContainer,
            secondary = c.secondary,
            onSecondary = c.onSecondary,
            background = c.backGround,
            surface = c.surface,
            onSurface = c.primaryFont,
            surfaceVariant = c.surfaceVariant,
            onSurfaceVariant = c.secondaryFont,
            error = c.error,
            onError = c.onError,
            outline = c.outline,
        )
    } else {
        lightColorScheme(
            primary = c.primary,
            onPrimary = c.onPrimary,
            primaryContainer = c.primaryContainer,
            onPrimaryContainer = c.onPrimaryContainer,
            secondary = c.secondary,
            onSecondary = c.onSecondary,
            background = c.backGround,
            surface = c.surface,
            onSurface = c.primaryFont,
            surfaceVariant = c.surfaceVariant,
            onSurfaceVariant = c.secondaryFont,
            error = c.error,
            onError = c.onError,
            outline = c.outline,
        )
    }
}

private fun LocalDate.toEpochMillis(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
