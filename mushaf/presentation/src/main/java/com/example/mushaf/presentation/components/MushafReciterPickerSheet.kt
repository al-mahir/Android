package com.example.mushaf.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.designsystem.theme.Theme
import com.example.mushaf.domain.model.DownloadableResource
import com.example.mushaf.domain.model.Reciter
import com.example.mushaf.presentation.R
import com.example.mushaf.presentation.download.components.DownloadableItemCard

@OptIn(ExperimentalMaterial3Api::class)
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
    modifier: Modifier = Modifier
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = Theme.colors.surface,
        contentColor = Theme.colors.onSurface
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalConfiguration provides configuration,
            androidx.compose.ui.platform.LocalContext provides context,
            androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection
        ) {
            Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
        ) {
            Text(
                text = stringResource(R.string.downloads_title_reciters),
                style = Theme.typography.title.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(Theme.spacing.medium)
            )

            HorizontalDivider(color = Theme.colors.outline)

            LazyColumn(
                contentPadding = PaddingValues(Theme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)
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
                            modifier = Modifier.clickable { onReciterSelected(reciter) }
                        )
                    }
                }
            }
        }
        }
    }
}
