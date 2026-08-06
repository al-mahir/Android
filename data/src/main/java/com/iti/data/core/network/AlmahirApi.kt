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

        object ForgotPassword {
            fun verifyEmail(email: String) = "forgot-password/verify-email/$email"
            fun verifyOtp(otp: String, email: String) = "forgot-password/verify-otp/$otp/$email"
            fun changePassword(email: String) = "forgot-password/change-password/$email"
        }

        private val PUBLIC_EXACT = setOf(REGISTER, LOGIN, GOOGLE, REFRESH)

        fun isPublic(encodedPath: String): Boolean {
            val clean = encodedPath.trim('/')
            return clean in PUBLIC_EXACT ||
                clean.contains("forgot-password/") ||
                clean.startsWith("forgot-password/") ||
                clean.startsWith("api/auth/forgot-password/")
        }

        object Sheikh {
            const val REGISTER = "api/auth/sheikh/register"
            const val LOGIN = "api/auth/sheikh/login"
            const val GOOGLE = "api/auth/sheikh/google"
            const val REFRESH = "api/auth/sheikh/refresh"

            private val PUBLIC_EXACT = setOf(REGISTER, LOGIN, GOOGLE, REFRESH)

            fun isPublic(encodedPath: String): Boolean {
                val clean = encodedPath.trim('/')
                return clean in PUBLIC_EXACT ||
                    clean.contains("forgot-password/") ||
                    clean.startsWith("forgot-password/") ||
                    clean.startsWith("api/auth/forgot-password/")
            }
        }
    }

    object Sheikh {
        const val ALL = "api/sheikh"
        const val BY_ID = "api/sheikh/{id}"
        const val SEARCH = "api/sheikh/search"
        const val UPDATE = "api/sheikh/{id}"
    }
}
