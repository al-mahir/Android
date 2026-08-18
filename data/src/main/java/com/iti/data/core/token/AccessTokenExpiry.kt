package com.iti.data.core.token

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val JWT_EXPIRY_LEEWAY_MS = 60_000L
private val EXP_CLAIM = Regex(""""exp"\s*:\s*(\d+)""")

/**
 * Reads the JWT `exp` claim without verifying the signature — sufficient for deciding when to
 * proactively refresh before sending a stale bearer token to meeting/circle endpoints.
 */
internal fun String.isAccessTokenExpiredOrNearExpiry(
    leewayMs: Long = JWT_EXPIRY_LEEWAY_MS,
): Boolean {
    val payload = split('.').getOrNull(1) ?: return false
    return runCatching {
        val exp = payload.decodeBase64UrlPayload().parseExpClaim()
        exp > 0L && exp * 1_000L <= System.currentTimeMillis() + leewayMs
    }.getOrDefault(false)
}

private fun String.parseExpClaim(): Long =
    EXP_CLAIM.find(this)?.groupValues?.get(1)?.toLongOrNull() ?: 0L

@OptIn(ExperimentalEncodingApi::class)
private fun String.decodeBase64UrlPayload(): String {
    val padded = this + when (length % 4) {
        2 -> "=="
        3 -> "="
        else -> ""
    }
    return String(Base64.UrlSafe.decode(padded))
}
