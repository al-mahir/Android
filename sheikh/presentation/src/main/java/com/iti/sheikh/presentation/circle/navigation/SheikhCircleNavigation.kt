package com.iti.sheikh.presentation.circle.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.iti.sheikh.presentation.circle.SheikhCircleListScreen
import com.iti.sheikh.presentation.circle.SheikhCircleManageScreen
import com.iti.sheikh.presentation.circle.SheikhCreateCircleScreen

/**
 * Sheikh-app circle destinations. [NavKey] is a plain library interface, so implementing it here
 * does not couple `:sheikh:presentation` to any host app's nav graph.
 */
sealed interface SheikhCircleRoute : NavKey {
    data object CircleList : SheikhCircleRoute
    data object CreateCircle : SheikhCircleRoute
    data class CircleManage(val circleId: String) : SheikhCircleRoute
}

/** Registers the sheikh circle destinations on the host app's Navigation 3 back stack. */
/**
 * @param onOpenSession opens the circle's live session screen. Deliberately takes only a circleId:
 *   the Agora credentials are fetched by the session controller itself, from
 *   `GET /api/circles/{id}/token`. Passing them through navigation (as an earlier version did) meant
 *   a token that was already minutes old by the time the screen mounted, and the host app simply
 *   dropped them on the floor.
 */
fun EntryProviderScope<NavKey>.sheikhCircleEntries(
    onNavigate: (SheikhCircleRoute) -> Unit,
    onBack: () -> Unit,
    onOpenSession: (circleId: String) -> Unit,
) {
    entry<SheikhCircleRoute.CircleList> {
        SheikhCircleListScreen(
            onBack = onBack,
            onOpenCircle = { circleId -> onNavigate(SheikhCircleRoute.CircleManage(circleId)) },
            onOpenCreateCircle = { onNavigate(SheikhCircleRoute.CreateCircle) },
        )
    }

    entry<SheikhCircleRoute.CreateCircle> {
        SheikhCreateCircleScreen(
            onBack = onBack,
            onCircleCreated = { circleId -> onNavigate(SheikhCircleRoute.CircleManage(circleId)) },
        )
    }

    entry<SheikhCircleRoute.CircleManage> { route ->
        SheikhCircleManageScreen(
            circleId = route.circleId,
            onBack = onBack,
            onOpenSession = onOpenSession,
        )
    }
}

