package com.example.mushaf.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.designsystem.components.bottomsheet.AppBottomSheet
import com.example.designsystem.theme.Theme
import com.example.mushaf.domain.model.DownloadableResource
import com.example.mushaf.domain.model.Reciter
import com.example.mushaf.presentation.R
import com.example.mushaf.presentation.download.components.DownloadableItemCard

@Composable
fun MushafReciterPickerSheet(
    reciters: List<Reciter>,
    downloadableResources: List<DownloadableResource>,
    selectedId: Int?,
    onReciterSelected: (Reciter) -> Unit,
    onDownloadClick: (String) -> Unit,
    onCancelClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppBottomSheet(
        onDismiss = onDismiss,
        skipPartiallyExpanded = false,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        ) {
            Text(
                text = stringResource(R.string.downloads_title_reciters),
                style = Theme.typography.title.copy(fontWeight = FontWeight.Bold),
                color = Theme.colors.primaryFont,
                modifier = Modifier.padding(vertical = Theme.spacing.medium),
            )

            HorizontalDivider(color = Theme.colors.border)

            LazyColumn(
                contentPadding = PaddingValues(vertical = Theme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
            ) {
                items(reciters, key = { it.id }) { reciter ->
                    val resource = downloadableResources.find {
                        it.id == reciter.id.toString()
                    }

                    if (resource != null) {
                        DownloadableItemCard(
                            resource = resource,
                            onDownload = { onDownloadClick(resource.id) },
                            onCancel = { onCancelClick(resource.id) },
                            onDelete = { onDeleteClick(resource.id) },
                            onClick = { onReciterSelected(reciter) },
                        )
                    }
                }
            }
        }
    }
}
