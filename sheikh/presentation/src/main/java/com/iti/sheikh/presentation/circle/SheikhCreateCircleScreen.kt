package com.iti.sheikh.presentation.circle

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.button.PrimaryButton
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

@Composable
fun SheikhCreateCircleScreen(
    onBack: () -> Unit,
    onCircleCreated: (circleId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SheikhCreateCircleViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            is SheikhCreateCircleEffect.CircleCreated -> onCircleCreated(effect.circleId)
            is SheikhCreateCircleEffect.ShowMessage ->
                Toast.makeText(context, effect.messageRes, Toast.LENGTH_SHORT).show()
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

            CircleTextField(
                value = state.startDate,
                onValueChange = onStartDateChanged,
                title = stringResource(R.string.sheikh_create_circle_start_date_label),
                hint = stringResource(R.string.sheikh_create_circle_date_hint),
                singleLine = true,
                isError = state.errorMessageRes == R.string.sheikh_create_circle_error_start_date,
            )

            CircleTextField(
                value = state.endDate,
                onValueChange = onEndDateChanged,
                title = stringResource(R.string.sheikh_create_circle_end_date_label),
                hint = stringResource(R.string.sheikh_create_circle_date_hint),
                singleLine = true,
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
            label = stringResource(R.string.sheikh_create_circle_type_public),
            icon = Icons.Outlined.Public,
            selected = selected == CircleType.PUBLIC,
            onClick = { onSelected(CircleType.PUBLIC) },
            modifier = Modifier.weight(1f),
        )
        SelectorPill(
            label = stringResource(R.string.sheikh_create_circle_type_private),
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
            style = Theme.typography.body.small.copy(fontWeight = FontWeight.SemiBold),
            color = Theme.colors.secondaryFont,
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
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Theme.colors.onPrimary else Theme.colors.secondaryFont,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.size(6.dp))
        BasicText(
            text = label,
            style = Theme.typography.body.small.copy(
                fontWeight = FontWeight.Medium,
                color = if (selected) Theme.colors.onPrimary else Theme.colors.secondaryFont,
            ),
        )
    }
}
