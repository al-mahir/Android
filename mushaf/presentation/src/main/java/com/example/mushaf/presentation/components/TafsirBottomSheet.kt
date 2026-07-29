package com.example.mushaf.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import com.example.designsystem.theme.Theme
import com.example.mushaf.presentation.state.TafsirState

/** Map a raw tafsirKey to a user-friendly Arabic display name. */
private fun tafsirDisplayName(key: String): String = when (key) {
    "ibn-kathir" -> "ابن كثير"
    "mukhtasar" -> "المختصر"
    "jalalayn" -> "الجلالين"
    "tabari" -> "الطبري"
    else -> key
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafsirBottomSheet(
    tafsirState: TafsirState,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Theme.colors.surface,
        contentColor = Theme.colors.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Theme.colors.onSurface.copy(alpha = 0.4f)) },
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp, top = 8.dp),
        ) {
            when (tafsirState) {
                is TafsirState.Idle -> {}

                is TafsirState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Theme.colors.primary)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "جاري تحميل التفسير…",
                                style = Theme.typography.body.medium,
                                color = Theme.colors.secondaryFont,
                            )
                        }
                    }
                }

                is TafsirState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.CloudOff,
                                contentDescription = null,
                                tint = Theme.colors.error,
                                modifier = Modifier.size(40.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = tafsirState.message,
                                color = Theme.colors.error,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                            )
                            if (onRetry != null) {
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = onRetry,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Theme.colors.primary,
                                    ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("إعادة المحاولة")
                                }
                            }
                        }
                    }
                }

                is TafsirState.Success -> {
                    val tafsir = tafsirState.tafsir
                    val isOnline = tafsir.tafsirKey != "mukhtasar"

                    // ── Header row: Ayah label + source badge ───────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${tafsir.surahNameArabic} - الآية ${tafsir.ayahNumber}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Theme.colors.primary,
                                fontSize = 20.sp,
                            ),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start,
                        )
                        Spacer(Modifier.width(8.dp))
                        SourceBadge(
                            label = tafsirDisplayName(tafsir.tafsirKey),
                            isOnline = isOnline,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Tafsir text ─────────────────────────────────────────────
                    val parsedHtml = HtmlCompat.fromHtml(
                        tafsir.tafsirText,
                        HtmlCompat.FROM_HTML_MODE_COMPACT,
                    ).toString()
                    Text(
                        text = parsedHtml,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 28.sp,
                            fontSize = 18.sp,
                            color = Theme.colors.primaryFont,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        textAlign = TextAlign.Right,
                    )
                }
            }
        }
    }
}

// ── Small source badge shown in the sheet header ──────────────────────────────
@Composable
private fun SourceBadge(label: String, isOnline: Boolean) {
    val icon: ImageVector = if (isOnline) Icons.Filled.CloudDone else Icons.Filled.CloudOff
    val tint = if (isOnline) Theme.colors.primary else Theme.colors.secondaryFont
    val bg = if (isOnline) Theme.colors.primary.copy(alpha = 0.10f) else Theme.colors.surface

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            style = Theme.typography.body.small.copy(fontWeight = FontWeight.Medium),
            color = tint,
        )
    }
}
