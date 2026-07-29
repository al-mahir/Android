package com.iti.presentation.meetingrequest.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iti.presentation.R
import com.iti.presentation.meetingrequest.browse.SheikhSummaryRow
import com.iti.presentation.meetingrequest.browse.SheikhBrowseIntent
import com.iti.presentation.meetingrequest.browse.SheikhBrowseUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheikhBrowseContent(
    state: SheikhBrowseUiState,
    onIntent: (SheikhBrowseIntent) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.meetingrequest_browse_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.meetingrequest_request_back))
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading && state.sheikhs.isEmpty() -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )

                state.sheikhs.isEmpty() -> Text(
                    text = state.errorMessage ?: stringResource(R.string.meetingrequest_browse_empty),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.sheikhs, key = { it.sheikhId }) { sheikh ->
                        SheikhSummaryRow(
                            sheikh = sheikh,
                            onClick = { onIntent(SheikhBrowseIntent.SheikhSelected(sheikh.sheikhId)) },
                        )
                    }
                }
            }
        }
    }
}





