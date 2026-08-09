package com.example.mushaf.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import com.example.core.designsystem.locale.rememberLocaleLocals
import com.example.designsystem.text.asString
import com.example.designsystem.theme.Theme
import com.example.mushaf.domain.model.DownloadFailure
import com.example.mushaf.domain.model.DownloadState
import com.example.mushaf.domain.model.TafsirBook
import com.example.mushaf.presentation.state.TafsirState

// ── Internal helpers ──────────────────────────────────────────────────────────

/** Maps a raw tafsirKey to its user-facing display name via string resources. */
@Composable
private fun tafsirDisplayName(key: String): String {
    val context = LocalContext.current
    val resName = "tafsir_${key.replace("-", "_")}"
    val resId = remember(resName) {
        context.resources.getIdentifier(resName, "string", context.packageName)
    }
    return if (resId != 0) stringResource(resId) else key
}

// ── Main composable ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafsirBottomSheet(
    tafsirState: TafsirState,
    availableBooks: List<TafsirBook>,
    selectedKey: String,
    isAyahBookmarked: Boolean,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null,
    onChangeTafsir: (String) -> Unit,
    onDownloadTafsir: (String, String) -> Unit,
    onDeleteTafsir: (String) -> Unit,
    onBookmarkAyah: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showSelection by remember { mutableStateOf(false) }
    // ModalBottomSheet renders in its own window, which resets the localized context.
    val localeLocals = rememberLocaleLocals()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Theme.colors.surface,
        contentColor = Theme.colors.onSurface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Theme.colors.onSurface.copy(alpha = 0.4f))
        },
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        localeLocals.Provide {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp, top = 8.dp),
            ) {
                AnimatedVisibility(visible = showSelection, enter = fadeIn(), exit = fadeOut()) {
                    TafsirSelectionList(
                        books = availableBooks,
                        selectedKey = selectedKey,
                        onChangeTafsir = {
                            onChangeTafsir(it)
                            showSelection = false
                        },
                        onDownloadTafsir = onDownloadTafsir,
                        onDeleteTafsir = onDeleteTafsir,
                        onBack = { showSelection = false },
                    )
                }

                AnimatedVisibility(visible = !showSelection, enter = fadeIn(), exit = fadeOut()) {
                    TafsirContent(
                        tafsirState = tafsirState,
                        selectedKey = selectedKey,
                        isAyahBookmarked = isAyahBookmarked,
                        onRetry = onRetry,
                        onBadgeClick = { showSelection = true },
                        onBookmarkAyah = onBookmarkAyah,
                    )
                }
            }
        }
    }
}

// ── Tafsir content (main view) ────────────────────────────────────────────────

@Composable
private fun TafsirContent(
    tafsirState: TafsirState,
    selectedKey: String,
    isAyahBookmarked: Boolean,
    onRetry: (() -> Unit)?,
    onBadgeClick: () -> Unit,
    onBookmarkAyah: () -> Unit,
) {
    when (tafsirState) {
        is TafsirState.Idle -> {}

        is TafsirState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
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
                        text = tafsirState.message.asString(),
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
            val isBundled = tafsir.tafsirKey == "mukhtasar"

            Column(modifier = Modifier.fillMaxWidth()) {
                // Header row: Ayah label + source badge
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
                    IconButton(onClick = onBookmarkAyah) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(
                                if (isAyahBookmarked) {
                                    com.example.designsystem.R.drawable.ic_bookmark_filled
                                } else {
                                    com.example.designsystem.R.drawable.ic_bookmark
                                },
                            ),
                            contentDescription = stringResource(com.example.mushaf.presentation.R.string.mushaf_cd_bookmark_ayah),
                            tint = Theme.colors.primary,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.clickable { onBadgeClick() }) {
                        SourceBadge(
                            label = tafsirDisplayName(tafsir.tafsirKey),
                            isBundled = isBundled,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tafsir text
                val parsedText = remember(tafsir.tafsirText) {
                    HtmlCompat.fromHtml(tafsir.tafsirText, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
                }
                Text(
                    text = parsedText,
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

// ── Source badge ──────────────────────────────────────────────────────────────

@Composable
private fun SourceBadge(label: String, isBundled: Boolean) {
    val icon: ImageVector = if (isBundled) Icons.Filled.CloudOff else Icons.Filled.CloudDone
    val tint = if (isBundled) Theme.colors.secondaryFont else Theme.colors.primary
    val bg = if (isBundled) Theme.colors.surface else Theme.colors.primary.copy(alpha = 0.10f)

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

// ── Source selection list ─────────────────────────────────────────────────────

@Composable
fun TafsirSelectionList(
    books: List<TafsirBook>,
    selectedKey: String,
    onChangeTafsir: (String) -> Unit,
    onDownloadTafsir: (String, String) -> Unit,
    onDeleteTafsir: (String) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
    ) {
        // Header row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = Theme.colors.onSurface,
                )
            }
            Text(
                text = "المصادر المتاحة",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = Theme.colors.onSurface,
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Books list
        androidx.compose.foundation.lazy.LazyColumn {
            items(books.size) { index ->
                TafsirBookRow(
                    book = books[index],
                    isSelected = books[index].tafsirKey == selectedKey,
                    onSelect = { onChangeTafsir(books[index].tafsirKey) },
                    onDownload = { onDownloadTafsir(books[index].tafsirKey, books[index].downloadUrl) },
                    onDelete = { onDeleteTafsir(books[index].tafsirKey) },
                )
                HorizontalDivider(color = Theme.colors.surfaceVariant)
            }
        }
    }
}

// ── Single book row ───────────────────────────────────────────────────────────

@Composable
private fun TafsirBookRow(
    book: TafsirBook,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    val isBundled = book.tafsirKey == "mukhtasar"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = book.state == DownloadState.Downloaded || isBundled) { onSelect() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Text info (left side in RTL = right)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.displayName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Theme.colors.primary else Theme.colors.onSurface,
                ),
            )
            if (isBundled) {
                Text(
                    text = "مدمج مع التطبيق",
                    style = MaterialTheme.typography.bodySmall,
                    color = Theme.colors.secondaryFont,
                )
            } else if (book.fileSizeBytes > 0) {
                Text(
                    text = "${book.fileSizeBytes / 1024 / 1024} MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = Theme.colors.secondaryFont,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Action control (right side) — hidden for bundled mukhtasar
        if (!isBundled) {
            TafsirBookAction(
                state = book.state,
                onDownload = onDownload,
                onDelete = onDelete,
            )
        }
    }
}

// ── Per-book action icon ──────────────────────────────────────────────────────

@Composable
private fun TafsirBookAction(
    state: DownloadState,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    when (state) {
        is DownloadState.Downloaded -> {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "حذف",
                    tint = Theme.colors.error,
                )
            }
        }

        is DownloadState.Downloading -> {
            val progress by animateFloatAsState(
                targetValue = state.progress,
                label = "download_progress",
            )
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
                if (progress > 0f) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp,
                        color = Theme.colors.primary,
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Theme.colors.primary,
                        fontSize = 8.sp,
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp,
                        color = Theme.colors.primary,
                    )
                }
            }
        }

        is DownloadState.Failed -> {
            // Show a retry icon so the user can try again.
            IconButton(onClick = onDownload) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "إعادة التحميل",
                    tint = Theme.colors.error,
                )
            }
        }

        else -> {
            // NotDownloaded
            IconButton(onClick = onDownload) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "تحميل",
                    tint = Theme.colors.primary,
                )
            }
        }
    }
}
