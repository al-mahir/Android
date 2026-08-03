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
fun EntryProviderScope<NavKey>.sheikhCircleEntries(
    onNavigate: (SheikhCircleRoute) -> Unit,
    onBack: () -> Unit,
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
        )
    }
}
