package com.iti.presentation.profile

import com.iti.domain.model.Subscription
import com.iti.domain.model.User


internal data class ProfileAccountSnapshot(
    val user: User,
    val subscription: Subscription,
    val isOffline: Boolean = false,
)
