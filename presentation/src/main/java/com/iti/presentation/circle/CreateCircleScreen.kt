package com.iti.presentation.circle

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.topbar.BackTitleTopBar
import com.example.designsystem.theme.Theme
import com.iti.presentation.R
import com.iti.presentation.circle.state.CreateCircleEffect
import com.iti.presentation.circle.state.CreateCircleIntent
import com.iti.presentation.circle.state.CreateCirclePrivacyType
import com.iti.presentation.circle.state.CreateCircleUiState
import com.iti.presentation.core.mvi.ObserveEffect
import org.koin.androidx.compose.koinViewModel

@Composable
fun CreateCircleScreen(
    onBack: () -> Unit,
    onCircleCreated: (circleId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateCircleViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            is CreateCircleEffect.CircleCreated -> onCircleCreated(effect.circleId)
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
        onSubmit = { viewModel.onIntent(CreateCircleIntent.Submit) },
        modifier = modifier,
    )
}

@Composable
private fun CreateCircleContent(
    state: CreateCircleUiState,
    onBack: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onGoalsChanged: (String) -> Unit,
    onPrivacySelected: (CreateCirclePrivacyType) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround),
    ) {
        BackTitleTopBar(
            title = stringResource(R.string.create_circle_title),
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

            // Role info banner — regular users can only create PRIVATE circles
            UserRoleBanner()

            // Title
            CreateCircleTextField(
                value = state.title,
                onValueChange = onTitleChanged,
                placeholder = stringResource(R.string.create_circle_title_hint),
                isError = state.titleError,
            )

            // Goals / description (multiline)
            CreateCircleTextField(
                value = state.goals,
                onValueChange = onGoalsChanged,
                placeholder = stringResource(R.string.create_circle_goals_hint),
                minLines = 3,
                maxLines = 5,
            )

            // Privacy selector (user can only choose PRIVATE; PUBLIC is disabled)
            PrivacySelector(
                selected = state.selectedType,
                onSelected = onPrivacySelected,
            )

            // Password field (only for PRIVATE)
            AnimatedVisibility(visible = state.selectedType == CreateCirclePrivacyType.PRIVATE) {
                CreateCircleTextField(
                    value = state.password,
                    onValueChange = onPasswordChanged,
                    placeholder = stringResource(R.string.create_circle_password_hint),
                    isError = state.passwordError,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // CTA
        PrimaryButton(
            caption = stringResource(R.string.create_circle_cta),
            onClick = onSubmit,
            isLoading = state.isCreating,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        )
    }
}

/** Yellow info banner: regular users may only create PRIVATE circles. */
@Composable
private fun UserRoleBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Theme.colors.primary.copy(alpha = 0.08f))
            .border(1.dp, Theme.colors.primary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = stringResource(R.string.create_circle_user_role_note),
            style = Theme.typography.body.small,
            color = Theme.colors.primary,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun CreateCircleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = placeholder,
                style = Theme.typography.body.medium,
                color = Theme.colors.secondaryFont,
            )
        },
        isError = isError,
        minLines = minLines,
        maxLines = maxLines,
        textStyle = Theme.typography.body.medium.copy(color = Theme.colors.primaryFont),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Theme.colors.primary,
            unfocusedBorderColor = Theme.colors.surface,
            errorBorderColor = Theme.colors.error,
            focusedContainerColor = Theme.colors.surface,
            unfocusedContainerColor = Theme.colors.surface,
        ),
    )
}

/** Two-pill selector for PRIVATE / PUBLIC. PUBLIC is disabled for regular users. */
@Composable
private fun PrivacySelector(
    selected: CreateCirclePrivacyType,
    onSelected: (CreateCirclePrivacyType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.create_circle_privacy_label),
            style = Theme.typography.body.small.copy(fontWeight = FontWeight.SemiBold),
            color = Theme.colors.secondaryFont,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Theme.colors.surface),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            PrivacyPill(
                label = stringResource(R.string.create_circle_privacy_private),
                icon = Icons.Outlined.Lock,
                selected = selected == CreateCirclePrivacyType.PRIVATE,
                enabled = true,
                onClick = { onSelected(CreateCirclePrivacyType.PRIVATE) },
                modifier = Modifier.weight(1f),
            )
            PrivacyPill(
                label = stringResource(R.string.create_circle_privacy_public),
                icon = Icons.Outlined.Public,
                selected = selected == CreateCirclePrivacyType.PUBLIC,
                // Public circles are Sheikh-only; disabled for regular users
                enabled = false,
                onClick = {},
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PrivacyPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = when {
        selected && enabled -> Theme.colors.primary
        !enabled -> Theme.colors.surface
        else -> Theme.colors.surface
    }
    val contentColor = when {
        selected && enabled -> Theme.colors.onPrimary
        !enabled -> Theme.colors.secondaryFont.copy(alpha = 0.4f)
        else -> Theme.colors.secondaryFont
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = label,
            style = Theme.typography.body.small.copy(fontWeight = FontWeight.Medium),
            color = contentColor,
        )
    }
}
