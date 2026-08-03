package com.iti.presentation.home

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iti.presentation.core.mvi.ObserveEffect
import com.iti.presentation.home.state.HomeEffect
import com.iti.presentation.home.state.HomeIntent
import org.koin.androidx.compose.koinViewModel


@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenMushafAtPage: (Int) -> Unit,
    onOpenSheikh: (String) -> Unit,
    onOpenSheikhList: () -> Unit,
    onOpenCircleList: () -> Unit,
    onOpenCircle: (String) -> Unit,
    onOpenMeetingRequest: (String, String?) -> Unit,
    onOpenActiveCall: (String, String, String, String, String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            HomeEffect.OpenSearch -> onOpenSearch()
            HomeEffect.OpenProfile -> onOpenProfile()
            HomeEffect.OpenSheikhList -> onOpenSheikhList()
            HomeEffect.OpenCircleList -> onOpenCircleList()
            is HomeEffect.OpenMushafAtPage -> onOpenMushafAtPage(effect.page)
            is HomeEffect.OpenSheikh -> onOpenSheikh(effect.sheikhId)
            is HomeEffect.OpenCircle -> onOpenCircle(effect.circleId)
            is HomeEffect.OpenMeetingRequest -> onOpenMeetingRequest(effect.sheikhId, effect.sheikhName)
            is HomeEffect.OpenActiveCall ->
                onOpenActiveCall(effect.requestId, effect.token, effect.channelName, effect.userAccount, effect.remoteDisplayName)
            is HomeEffect.ShowMessage ->
                Toast.makeText(context, effect.messageRes, Toast.LENGTH_SHORT).show()
        }
    }

    HomeContent(
        state = state,
        onSearchClick = { viewModel.onIntent(HomeIntent.SearchClicked) },
        onProfileClick = { viewModel.onIntent(HomeIntent.ProfileClicked) },
        onContinueReadingClick = { viewModel.onIntent(HomeIntent.ContinueReadingClicked) },
        onSeeAllSheikhsClick = { viewModel.onIntent(HomeIntent.SeeAllSheikhsClicked) },
        onSeeAllCirclesClick = { viewModel.onIntent(HomeIntent.SeeAllCirclesClicked) },
        onSheikhClick = { sheikhId -> viewModel.onIntent(HomeIntent.SheikhClicked(sheikhId)) },
        onCircleClick = { circleId -> viewModel.onIntent(HomeIntent.CircleClicked(circleId)) },
        onRetryClick = { viewModel.onIntent(HomeIntent.Retry) },
        onViewPendingMeetingClick = { viewModel.onIntent(HomeIntent.ViewPendingMeetingClicked) },
        onCancelPendingMeetingClick = { viewModel.onIntent(HomeIntent.CancelPendingMeetingClicked) },
        onRejoinActiveCallClick = { viewModel.onIntent(HomeIntent.RejoinActiveCallClicked) },
        onDismissActiveCallClick = { viewModel.onIntent(HomeIntent.DismissActiveCallClicked) },
        modifier = modifier,
    )
}
