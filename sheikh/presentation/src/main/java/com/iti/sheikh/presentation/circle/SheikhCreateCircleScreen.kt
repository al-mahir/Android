package com.iti.sheikh.presentation.circle

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.button.ButtonHeightCompact
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.button.SecondaryButton
import com.example.designsystem.components.textfield.TextField
import com.example.designsystem.components.topbar.BackTitleTopBar
import com.example.designsystem.theme.Theme
import com.iti.meeting.domain.model.circle.CircleType
import com.iti.sheikh.presentation.R
import com.iti.sheikh.presentation.circle.state.SheikhCreateCircleEffect
import com.iti.sheikh.presentation.circle.state.SheikhCreateCircleIntent
import com.iti.sheikh.presentation.circle.state.SheikhCreateCircleUiState
import com.iti.sheikh.presentation.core.mvi.ObserveEffect
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SheikhCreateCircleScreen(
    onBack: () -> Unit,
    onCircleCreated: (circleId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SheikhCreateCircleViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingInviteToken by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            is SheikhCreateCircleEffect.CircleCreated -> onCircleCreated(effect.circleId)
            is SheikhCreateCircleEffect.ShowMessage ->
                Toast.makeText(context, effect.messageRes, Toast.LENGTH_SHORT).show()
            is SheikhCreateCircleEffect.ShowInviteToken ->
                pendingInviteToken = Triple(effect.token, effect.circleName, effect.circleId)
        }
    }

    SheikhCreateCircleContent(
        state = state,
        onBack = onBack,
        onNameChanged = { viewModel.onIntent(SheikhCreateCircleIntent.NameChanged(it)) },
        onTypeSelected = { viewModel.onIntent(SheikhCreateCircleIntent.TypeSelected(it)) },
        onRequiresApprovalChanged = { viewModel.onIntent(SheikhCreateCircleIntent.RequiresApprovalChanged(it)) },
        onMaxParticipantsChanged = { viewModel.onIntent(SheikhCreateCircleIntent.MaxParticipantsChanged(it)) },
        onPasswordChanged = { viewModel.onIntent(SheikhCreateCircleIntent.PasswordChanged(it)) },
        onStartDateChanged = { viewModel.onIntent(SheikhCreateCircleIntent.StartDateChanged(it)) },
        onEndDateChanged = { viewModel.onIntent(SheikhCreateCircleIntent.EndDateChanged(it)) },
        onSubmit = { viewModel.onIntent(SheikhCreateCircleIntent.Submit) },
        modifier = modifier,
    )

    pendingInviteToken?.let { (token, name, circleId) ->
        CreateInviteTokenDialog(
            circleName = name,
            token = token,
            onDismiss = {
                pendingInviteToken = null
                onCircleCreated(circleId)
            },
        )
    }
}

@Composable
private fun SheikhCreateCircleContent(
    state: SheikhCreateCircleUiState,
    onBack: () -> Unit,
    onNameChanged: (String) -> Unit,
    onTypeSelected: (CircleType) -> Unit,
    onRequiresApprovalChanged: (Boolean) -> Unit,
    onMaxParticipantsChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onStartDateChanged: (String) -> Unit,
    onEndDateChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround),
    ) {
        BackTitleTopBar(
            title = stringResource(R.string.sheikh_create_circle_title),
            onBackClick = onBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            CircleTextField(
                value = state.name,
                onValueChange = onNameChanged,
                title = stringResource(R.string.sheikh_create_circle_name_label),
                hint = stringResource(R.string.sheikh_create_circle_name_hint),
                isError = state.errorMessageRes == R.string.sheikh_create_circle_error_name,
            )

            TypeSelector(selected = state.type, onSelected = onTypeSelected)

            ApprovalSelector(
                requiresApproval = state.requiresApproval,
                onRequiresApprovalChanged = onRequiresApprovalChanged,
            )

            CircleTextField(
                value = state.maxParticipants,
                onValueChange = onMaxParticipantsChanged,
                title = stringResource(R.string.sheikh_create_circle_capacity_label),
                hint = stringResource(R.string.sheikh_create_circle_capacity_hint),
                keyboardType = KeyboardType.Number,
                singleLine = true,
                isError = state.errorMessageRes == R.string.sheikh_create_circle_error_capacity,
            )

            AnimatedVisibility(visible = state.type == CircleType.PRIVATE) {
                CircleTextField(
                    value = state.password,
                    onValueChange = onPasswordChanged,
                    title = stringResource(R.string.sheikh_create_circle_password_label),
                    hint = stringResource(R.string.sheikh_create_circle_password_hint),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardType = KeyboardType.Password,
                    singleLine = true,
                    isError = state.errorMessageRes == R.string.sheikh_create_circle_error_password,
                )
            }

            SheikhScheduleDateField(
                title = stringResource(R.string.sheikh_create_circle_start_date_label),
                value = state.startDate,
                hint = stringResource(R.string.sheikh_create_circle_date_hint),
                isError = state.errorMessageRes == R.string.sheikh_create_circle_error_start_date,
                onValueChange = onStartDateChanged,
            )

            SheikhScheduleDateField(
                title = stringResource(R.string.sheikh_create_circle_end_date_label),
                value = state.endDate,
                hint = stringResource(R.string.sheikh_create_circle_date_hint),
                isError = false,
                onValueChange = onEndDateChanged,
            )

            state.errorMessageRes?.let { messageRes ->
                BasicText(
                    text = stringResource(messageRes),
                    style = Theme.typography.body.small.copy(color = Theme.colors.error),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        PrimaryButton(
            caption = stringResource(R.string.sheikh_create_circle_cta),
            onClick = onSubmit,
            isLoading = state.isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        )
    }
}

@Composable
private fun CircleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    singleLine: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
) {
    TextField(
        text = value,
        onTextChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        title = title,
        hint = hint,
        isError = isError,
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
    )
}

@Composable
private fun TypeSelector(
    selected: CircleType,
    onSelected: (CircleType) -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectorGroup(
        label = stringResource(R.string.sheikh_create_circle_type_label),
        modifier = modifier,
    ) {
        SelectorPill(
            label = stringResource(R.string.sheikh_circle_type_public),
            icon = Icons.Outlined.Public,
            selected = selected == CircleType.PUBLIC,
            onClick = { onSelected(CircleType.PUBLIC) },
            modifier = Modifier.weight(1f),
        )
        SelectorPill(
            label = stringResource(R.string.sheikh_circle_type_private),
            icon = Icons.Outlined.Lock,
            selected = selected == CircleType.PRIVATE,
            onClick = { onSelected(CircleType.PRIVATE) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ApprovalSelector(
    requiresApproval: Boolean,
    onRequiresApprovalChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectorGroup(
        label = stringResource(R.string.sheikh_create_circle_approval_label),
        modifier = modifier,
    ) {
        SelectorPill(
            label = stringResource(R.string.sheikh_create_circle_approval_no),
            selected = !requiresApproval,
            onClick = { onRequiresApprovalChanged(false) },
            modifier = Modifier.weight(1f),
        )
        SelectorPill(
            label = stringResource(R.string.sheikh_create_circle_approval_yes),
            selected = requiresApproval,
            onClick = { onRequiresApprovalChanged(true) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SelectorGroup(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText(
            text = label,
            style = Theme.typography.body.small.copy(
                fontWeight = FontWeight.SemiBold,
                color = Theme.colors.secondaryFont,
            ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Theme.colors.surface),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            content = content,
        )
    }
}

@Composable
private fun SelectorPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Theme.colors.primary else Theme.colors.surface)
            .then(if (selected) Modifier.border(1.dp, Theme.colors.primary, RoundedCornerShape(10.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = if (selected) Theme.colors.onPrimary else Theme.colors.secondaryFont,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.size(6.dp))
        }
        BasicText(
            text = label,
            style = Theme.typography.body.small.copy(
                fontWeight = FontWeight.Medium,
                color = if (selected) Theme.colors.onPrimary else Theme.colors.secondaryFont,
            ),
        )
    }
}

/** Success popup after creating a circle — shows the invite token to share. */
@Composable
private fun CreateInviteTokenDialog(
    circleName: String,
    token: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val tokenLabel = stringResource(R.string.sheikh_circle_invite_token_title)

    fun copyToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(tokenLabel, token))
        Toast.makeText(context, R.string.sheikh_circle_invite_token_copied, Toast.LENGTH_SHORT).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                        .clip(androidx.compose.foundation.shape.CircleShape)
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
                    text = stringResource(R.string.sheikh_circle_invite_token_title),
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
                    text = circleName,
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
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BasicText(
                        text = stringResource(R.string.sheikh_circle_invite_token_copy),
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
                            text = token,
                            style = Theme.typography.body.medium.copy(
                                color = Theme.colors.primaryFont,
                                fontWeight = FontWeight.Medium,
                            ),
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        IconButton(onClick = ::copyToClipboard) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(R.string.sheikh_circle_invite_token_copy),
                                tint = Theme.colors.primary,
                                modifier = Modifier.size(Theme.size.iconSemiMedium),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                caption = stringResource(R.string.sheikh_circle_confirm_dismiss),
                onClick = onDismiss,
                height = ButtonHeightCompact,
                shape = Theme.shapes.medium,
            )
        },
    )
}

/**
 * Read-only date &amp; time field. Tapping it opens a Material 3 date picker followed by a
 * time picker; the chosen local value is stored back as an ISO-8601 UTC string.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SheikhScheduleDateField(
    title: String,
    value: String,
    hint: String,
    isError: Boolean,
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
        modifier = modifier.fillMaxWidth(),
        title = title,
        hint = hint,
        isError = isError,
        singleLine = true,
        readOnly = true,
        trailingIcon = rememberVectorPainter(Icons.Outlined.CalendarMonth),
        onClickTrailingIcon = { showDatePicker = true },
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
                            text = stringResource(R.string.sheikh_create_circle_date_next),
                            color = Theme.colors.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(
                            text = stringResource(R.string.sheikh_create_circle_cancel),
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
                            text = stringResource(R.string.sheikh_create_circle_time_done),
                            color = Theme.colors.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                title = {
                    BasicText(
                        text = stringResource(R.string.sheikh_create_circle_time_label),
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
                            text = stringResource(R.string.sheikh_create_circle_cancel),
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
