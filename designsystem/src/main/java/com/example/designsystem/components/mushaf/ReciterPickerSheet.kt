package com.example.designsystem.components.mushaf

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import com.example.designsystem.theme.Theme

data class ReciterItem(
    val id: Int,
    val name: String,
    val nameArabic: String,
    val style: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReciterPickerSheet(
    reciters: List<ReciterItem>,
    selectedId: Int?,
    onReciterSelected: (ReciterItem) -> Unit,
    onDownloadClick: ((ReciterItem) -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = Theme.colors.surface,
        contentColor = Theme.colors.onSurface
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
        ) {
            Text(
                text = "Select Reciter",
                style = Theme.typography.title.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(Theme.spacing.medium)
            )
            
            Divider(color = Theme.colors.outline)
            
            LazyColumn {
                items(reciters, key = { it.id }) { reciter ->
                    val isSelected = reciter.id == selectedId
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onReciterSelected(reciter) }
                            .background(if (isSelected) Theme.colors.primaryContainer.copy(alpha = 0.3f) else Theme.colors.surface)
                            .padding(Theme.spacing.medium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = reciter.nameArabic,
                                style = Theme.typography.body.large.copy(fontWeight = FontWeight.Medium),
                                color = if (isSelected) Theme.colors.primary else Theme.colors.onSurface
                            )
                            Text(
                                text = reciter.style,
                                style = Theme.typography.body.medium,
                                color = Theme.colors.secondaryFont
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Theme.colors.primary,
                                    modifier = Modifier.padding(end = Theme.spacing.small)
                                )
                            }
                            
                            IconButton(onClick = { onDownloadClick?.invoke(reciter) }) {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = android.R.drawable.stat_sys_download),
                                    contentDescription = "Download options",
                                    tint = Theme.colors.primary
                                )
                            }
                        }
                    }
                    
                    Divider(color = Theme.colors.outline, modifier = Modifier.padding(start = Theme.spacing.medium))
                }
            }
        }
    }
}
