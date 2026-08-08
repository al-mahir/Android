package com.iti.presentation.profile.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.iti.presentation.attributions.AttributionsScreen
import com.iti.presentation.payment.checkout.CheckoutScreen
import com.iti.presentation.sessions.SessionHistoryScreen
import com.iti.presentation.staticcontent.StaticContentScreen
import com.iti.presentation.subscription.PackagesScreen
import com.iti.presentation.subscription.SubscriptionDetailsScreen


fun EntryProviderScope<NavKey>.profileEntries(
    onBack: () -> Unit,
    onNavigateToCheckout: (String) -> Unit,
) {
    entry<ProfileRoute.StaticContent> { route ->
        StaticContentScreen(
            documentType = route.documentType,
            onBack = onBack,
        )
    }

    entry<ProfileRoute.Sessions> {
        SessionHistoryScreen(onBack = onBack)
    }

    entry<ProfileRoute.Attributions> {
        AttributionsScreen(onBack = onBack)
    }

    entry<ProfileRoute.Premium> {
        PackagesScreen(onBack = onBack, onNavigateToCheckout = onNavigateToCheckout)
    }

    entry<ProfileRoute.MySubscription> {
        SubscriptionDetailsScreen(onBack = onBack)
    }

    entry<ProfileRoute.Checkout> { route ->
        CheckoutScreen(packageId = route.packageId, onBack = onBack)
    }
}
