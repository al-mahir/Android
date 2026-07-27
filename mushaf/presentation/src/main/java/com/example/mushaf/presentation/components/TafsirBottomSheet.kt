package com.example.mushaf.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import com.example.designsystem.text.asString
import com.example.designsystem.theme.Theme
import com.example.mushaf.presentation.state.TafsirState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafsirBottomSheet(
    tafsirState: TafsirState,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Theme.colors.surface,
        contentColor = Theme.colors.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Theme.colors.onSurface.copy(alpha = 0.4f)) },
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp, top = 8.dp)
        ) {
            when (tafsirState) {
                is TafsirState.Idle -> {}
                is TafsirState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Theme.colors.primary)
                    }
                }
                is TafsirState.Error -> {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = tafsirState.message.asString(),
                            color = Theme.colors.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                is TafsirState.Success -> {
                    val tafsir = tafsirState.tafsir
                    Text(
                        text = "${tafsir.surahNameArabic} - الآية ${tafsir.ayahNumber}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Theme.colors.primary,
                            fontSize = 20.sp
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val parsedHtml = HtmlCompat.fromHtml(tafsir.tafsirText, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
                    Text(
                        text = parsedHtml,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 28.sp,
                            fontSize = 18.sp,
                            color = Theme.colors.primaryFont
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        textAlign = TextAlign.Right
                    )
                }
            }
        }
    }
}
