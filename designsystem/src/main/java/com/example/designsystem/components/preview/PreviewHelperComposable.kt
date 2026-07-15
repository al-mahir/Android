package com.example.designsystem.components.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
internal fun PreviewHelperComposable(
    content:@Composable () -> Unit
){
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        content()
    }
}

@Composable
private fun PreviewHelperComposablePreview() {
    PreviewHelperComposable {
        BasicText(text = "Preview Content")
    }
}
