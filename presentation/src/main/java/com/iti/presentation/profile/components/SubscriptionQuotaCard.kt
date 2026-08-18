package com.iti.presentation.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.card.InnerContentCard
import com.example.designsystem.theme.Theme
import com.iti.domain.model.SubscriptionMinutes
import com.iti.presentation.R

/**
 * Live subscription status for the profile: package name, remaining-minute balance and expiry.
 *
 * Renders three distinct states rather than one, because the student's next action differs:
 * healthy (nothing to do), running low (top up soon), and expired / exhausted (renew now).
 */
@Composable
internal fun SubscriptionQuotaCard(
    minutes: SubscriptionMinutes,
    nowEpochMillis: Long,
    onRenewClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Bound to a local: `expiresAtEpochMillis` lives in :domain, so it cannot be smart-cast here.
    val expiresAt = minutes.expiresAtEpochMillis
    val isExpired = minutes.isExpiredAt(nowEpochMillis)
    val isDepleted = minutes.remainingMinutes <= 0
    val needsAction = isExpired || isDepleted
    val isRunningLow = !needsAction && minutes.remainingMinutes <= LOW_BALANCE_MINUTES

    InnerContentCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.padding(end = Theme.spacing.small)) {
                BasicText(
                    text = stringResource(R.string.profile_subscription_status),
                    style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
                )
                BasicText(
                    text = minutes.packageName ?: stringResource(R.string.profile_plan_premium),
                    style = Theme.typography.body.medium.copy(
                        color = Theme.colors.primaryFont,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            BasicText(
                text = pluralStringResource(
                    R.plurals.profile_minutes_remaining,
                    minutes.remainingMinutes,
                    minutes.remainingMinutes,
                ),
                style = Theme.typography.body.medium.copy(
                    color = if (needsAction || isRunningLow) Theme.colors.error else Theme.colors.primary,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }

        if (minutes.totalMinutes > 0) {
            LinearProgressIndicator(
                progress = { 1f - minutes.usedFraction },
                color = if (needsAction || isRunningLow) Theme.colors.error else Theme.colors.primary,
                trackColor = Theme.colors.disable,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Theme.spacing.small)
                    .height(PROGRESS_HEIGHT),
            )
            BasicText(
                text = stringResource(
                    R.string.profile_minutes_used_pattern,
                    minutes.usedMinutes,
                    minutes.totalMinutes,
                ),
                style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
                modifier = Modifier.padding(top = Theme.spacing.extraSmall),
            )
        }

        val statusText = when {
            isExpired -> stringResource(R.string.profile_subscription_expired)
            isDepleted -> stringResource(R.string.profile_subscription_depleted)
            isRunningLow -> stringResource(R.string.profile_subscription_running_low)
            expiresAt != null -> stringResource(
                R.string.profile_subscription_renews_pattern,
                rememberFormattedDate(expiresAt),
            )
            else -> null
        }

        if (statusText != null) {
            BasicText(
                text = statusText,
                style = Theme.typography.body.small.copy(
                    color = if (needsAction) Theme.colors.error else Theme.colors.secondaryFont,
                ),
                modifier = Modifier.padding(top = Theme.spacing.small),
            )
        }

        if (needsAction) {
            PrimaryButton(
                caption = stringResource(R.string.profile_subscription_renew_cta),
                onClick = onRenewClick,
                shape = Theme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Theme.spacing.medium),
            )
        }
    }
}

/** Roughly one short session left — early enough that the student can top up before being stuck. */
private const val LOW_BALANCE_MINUTES = 30
private val PROGRESS_HEIGHT = 6.dp
