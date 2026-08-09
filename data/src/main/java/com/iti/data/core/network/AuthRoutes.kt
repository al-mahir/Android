package com.iti.data.core.network

/**
 * Which account family's auth routes a host app speaks. The student and sheikh families are on
 * separate backend routes ([AlmahirApi.Auth] vs [AlmahirApi.Auth.Sheikh]), and a client that
 * refreshes against the wrong one presents a valid refresh token to an endpoint that will not
 * accept it — which reads as "this session is dead" and signs the user out.
 *
 * Bundling the endpoint and its public-path set together means an app picks its family once,
 * rather than every client construction site getting the pairing right on its own.
 */
class AuthRoutes(
    val refreshEndpoint: String,
    val isPublicEndpoint: (String) -> Boolean,
) {
    companion object {
        val Student = AuthRoutes(AlmahirApi.Auth.REFRESH, AlmahirApi.Auth::isPublic)
        val Sheikh = AuthRoutes(AlmahirApi.Auth.Sheikh.REFRESH, AlmahirApi.Auth.Sheikh::isPublic)
    }
}
