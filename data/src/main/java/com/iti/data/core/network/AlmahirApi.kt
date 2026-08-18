package com.iti.data.core.network

import com.iti.data.BuildConfig

object AlmahirApi {

    val BASE_URL: String get() = BuildConfig.BASE_URL

    object Auth {
        const val REGISTER = "api/auth/user/register"
        const val LOGIN = "api/auth/user/login"
        const val GOOGLE = "api/auth/user/google"
        const val REFRESH = "api/auth/user/refresh"
        const val LOGOUT = "api/auth/logout"
        const val FORGOT_PASSWORD = "api/auth/user/forgot-password"
        const val RESET_PASSWORD = "api/auth/user/reset-password"

        private val PUBLIC = setOf(REGISTER, LOGIN, GOOGLE, REFRESH, FORGOT_PASSWORD, RESET_PASSWORD)

        fun isPublic(encodedPath: String): Boolean =
            encodedPath.trim('/') in PUBLIC

        object Sheikh {
            const val REGISTER = "api/auth/sheikh/register"
            const val LOGIN = "api/auth/sheikh/login"
            const val GOOGLE = "api/auth/sheikh/google"
            const val REFRESH = "api/auth/sheikh/refresh"

            private val PUBLIC = setOf(REGISTER, LOGIN, GOOGLE, REFRESH)

            fun isPublic(encodedPath: String): Boolean =
                encodedPath.trim('/') in PUBLIC
        }
    }

    object Sheikh {
        const val ALL = "api/sheikh"
        const val BY_ID = "api/sheikh/{id}"
        const val SEARCH = "api/sheikh/search"
        const val UPDATE = "api/sheikh/{id}"
    }

    object Students {
        const val SUBSCRIPTION_MINUTES = "api/students/me/subscription-minutes"
    }

    object Payment {
        const val PACKAGES = "api/payment/packages"
        const val CREATE_INTENTION = "api/payment/intentions"
        fun statusUrl(intentionId: String) = "api/payment/intentions/$intentionId/status"
    }
}
