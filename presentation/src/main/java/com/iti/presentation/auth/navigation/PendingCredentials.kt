package com.iti.presentation.auth.navigation

/**
 * Ephemeral, in-memory holder for the user's password between the Register and
 * OTP screens. The password is stashed when the Register effect fires and
 * consumed (cleared) when the OTP screen reads it.
 *
 * This avoids putting the password into a serialized navigation route where it
 * would be persisted in the saved-state bundle. The holder is process-scoped,
 * so it is automatically cleared on process death.
 */
internal object PendingCredentials {
    private var password: String? = null

    fun stash(pw: String) {
        password = pw
    }

    fun consume(): String? = password.also { password = null }
}
