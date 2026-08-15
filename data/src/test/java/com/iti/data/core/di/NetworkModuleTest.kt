package com.iti.data.core.di

import android.content.Context
import com.iti.data.core.network.AlmahirApi
import com.iti.data.core.network.AuthRoutes
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

/**
 * The two apps differ in exactly one thing — which account family's auth routes they speak — and
 * getting that wrong is invisible until a token expires in the wild, at which point the app
 * refreshes against the other family's endpoint, is refused, and signs the user out.
 *
 * [verify] walks each module's definitions and fails on anything it cannot construct, which is
 * what catches a dependency going missing when the network wiring is rearranged; [Context] is
 * listed as externally supplied because `androidContext()` provides it at startup.
 */
@OptIn(KoinExperimentalAPI::class)
class NetworkModuleTest {

    @Test
    fun `student networking resolves and points at the student refresh endpoint`() {
        networkModule.verify(extraTypes = EXTERNALLY_PROVIDED)

        assertEquals(AlmahirApi.Auth.REFRESH, AuthRoutes.Student.refreshEndpoint)
    }

    @Test
    fun `sheikh networking resolves and points at the sheikh refresh endpoint`() {
        sheikhNetworkModule.verify(extraTypes = EXTERNALLY_PROVIDED + HttpClient::class)

        assertEquals(AlmahirApi.Auth.Sheikh.REFRESH, AuthRoutes.Sheikh.refreshEndpoint)
    }

    @Test
    fun `the two families never share a refresh endpoint`() {
        assertNotEquals(AuthRoutes.Student.refreshEndpoint, AuthRoutes.Sheikh.refreshEndpoint)
    }

    @Test
    fun `each family treats its own refresh route as public so refreshing cannot recurse`() {
        // A refresh call that itself went through the Auth plugin would refresh on its own 401.
        assertTrue(AuthRoutes.Student.isPublicEndpoint(AuthRoutes.Student.refreshEndpoint))
        assertTrue(AuthRoutes.Sheikh.isPublicEndpoint(AuthRoutes.Sheikh.refreshEndpoint))
    }

    private companion object {
        /**
         * `verify` reads factory signatures reflectively and cannot see which parameters have
         * defaults, so anything not injected has to be declared here: [Context] arrives via
         * `androidContext()` at startup, and the rest are defaulted arguments of
         * [com.iti.data.core.network.createAlmahirHttpClient].
         */
        val EXTERNALLY_PROVIDED = listOf(
            Context::class,
            HttpClientEngine::class,
            String::class,
            Boolean::class,
            Function1::class,
        )
    }
}
