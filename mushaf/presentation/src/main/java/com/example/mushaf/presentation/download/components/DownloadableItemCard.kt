package com.example.mushaf.presentation.download.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme
import com.example.mushaf.domain.model.DownloadState
import com.example.mushaf.domain.model.DownloadableResource
import com.example.mushaf.domain.model.LocalizedText
import com.example.mushaf.domain.model.ResourceKind
import com.example.designsystem.R as DesignSystemR
import com.example.mushaf.presentation.R
import java.util.Locale


@Composable
fun DownloadableItemCard(
    resource: DownloadableResource,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val subtitle = resource.subtitle?.resolve()
    val size = rememberFormattedSize(resource.sizeBytes)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Theme.shapes.large)
            .background(Theme.colors.surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = resource.name.resolve(),
                style = Theme.typography.body.large.copy(fontWeight = FontWeight.Medium),
                color = Theme.colors.primaryFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = Theme.typography.body.small,
                    color = Theme.colors.secondaryFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = size,
                style = Theme.typography.hint.small,
                color = Theme.colors.hint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        DownloadTrailing(
            state = resource.state,
            onDownload = onDownload,
            onCancel = onCancel,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun DownloadTrailing(
    state: DownloadState,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
    ) {
        when (state) {
            DownloadState.NotDownloaded -> ActionIcon(
                iconRes = DesignSystemR.drawable.ic_download,
                contentDescription = stringResource(R.string.downloads_action_download),
                tint = Theme.colors.primary,
                onClick = onDownload,
            )

            is DownloadState.Downloading -> {
                
                
                Box(
                    modifier = Modifier
                        .size(Theme.size.iconMedium)
                        .clip(CircleShape)
                        .clickable(onClick = onCancel)
                        .semantics { role = Role.Button },
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        progress = { state.progress },
                        color = Theme.colors.primary,
                        trackColor = Theme.colors.border,
                        strokeWidth = PROGRESS_STROKE,
                        modifier = Modifier.size(Theme.size.iconMedium),
                    )
                    Icon(
                        painter = painterResource(DesignSystemR.drawable.ic_cancel),
                        contentDescription = stringResource(R.string.downloads_action_cancel),
                        tint = Theme.colors.hint,
                        modifier = Modifier.size(Theme.size.iconSmall),
                    )
                }
            }

            DownloadState.Downloaded -> {
                ActionIcon(
                    iconRes = DesignSystemR.drawable.ic_check,
                    contentDescription = stringResource(R.string.downloads_state_downloaded),
                    tint = Theme.colors.success,
                    onClick = null,
                )
                ActionIcon(
                    iconRes = DesignSystemR.drawable.ic_trash,
                    contentDescription = stringResource(R.string.downloads_action_delete),
                    tint = Theme.colors.error,
                    onClick = onDelete,
                )
            }

            is DownloadState.Failed -> ActionIcon(
                iconRes = DesignSystemR.drawable.ic_download,
                contentDescription = stringResource(R.string.downloads_action_retry),
                tint = Theme.colors.error,
                onClick = onDownload,
            )
        }
    }
}

@Composable
private fun ActionIcon(
    iconRes: Int,
    contentDescription: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: (() -> Unit)?,
) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier
            .size(Theme.size.iconMedium)
            .clip(CircleShape)
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable(onClick = onClick)
                        .semantics { role = Role.Button }
                } else {
                    Modifier
                }
            )
            .padding(DOWNLOAD_ICON_PADDING),
    )
}

private val PROGRESS_STROKE = 2.dp
private val DOWNLOAD_ICON_PADDING = 2.dp

@Preview(name = "Downloadable states (RTL)", locale = "ar", showBackground = true)
@Composable
private fun DownloadableItemCardRtlPreview() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("ar")) {
        Column(
            modifier = Modifier
                .background(Theme.colors.backGround)
                .padding(Theme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        ) {
            PreviewStates.forEach { state ->
                DownloadableItemCard(
                    resource = DownloadableResource(
                        id = state.toString(),
                        kind = ResourceKind.RECITER,
                        name = LocalizedText("محمود خليل الحصري", "Mahmoud Khalil Al-Husary"),
                        subtitle = LocalizedText("حفص عن عاصم", "Hafs 'an 'Asim"),
                        sizeBytes = 820L * 1024 * 1024,
                        state = state,
                    ),
                    onDownload = {},
                    onCancel = {},
                    onDelete = {},
                )
            }
        }
    }
}

@Preview(name = "Downloadable states (LTR)", locale = "en", showBackground = true)
@Composable
private fun DownloadableItemCardLtrPreview() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("en")) {
        Column(
            modifier = Modifier
                .background(Theme.colors.backGround)
                .padding(Theme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        ) {
            PreviewStates.forEach { state ->
                DownloadableItemCard(
                    resource = DownloadableResource(
                        id = state.toString(),
                        kind = ResourceKind.TAFSEER,
                        name = LocalizedText("تفسير ابن كثير", "Tafsir Ibn Kathir"),
                        sizeBytes = 48L * 1024 * 1024,
                        state = state,
                    ),
                    onDownload = {},
                    onCancel = {},
                    onDelete = {},
                )
            }
        }
    }
}

private val PreviewStates = listOf(
    DownloadState.NotDownloaded,
    DownloadState.Downloading(progress = 0.45f),
    DownloadState.Downloaded,
)
