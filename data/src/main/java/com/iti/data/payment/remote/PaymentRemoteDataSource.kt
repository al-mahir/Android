package com.iti.data.payment.remote

import com.iti.data.core.network.AlmahirApi
import com.iti.data.core.network.dto.ApiResponse
import com.iti.data.payment.PaymentDataSource
import com.iti.data.payment.dto.CreateIntentionRequest
import com.iti.data.payment.dto.PaymentIntentionDto
import com.iti.data.payment.dto.PaymentOutcomeDto
import com.iti.data.payment.dto.SubscriptionMinutesDto
import com.iti.data.payment.dto.SubscriptionPackageDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class PaymentRemoteDataSource(
    private val client: HttpClient,
) : PaymentDataSource {

    override suspend fun getPackages(): List<SubscriptionPackageDto> =
        client.get(AlmahirApi.Payment.PACKAGES).body()

    /**
     * A student with no subscription comes back as HTTP 404 or a success envelope with a null
     * `data`; both mean "no subscription", not an error, so both map to `null`. Any other
     * failure (401/403/5xx) still throws and surfaces as a [com.iti.domain.core.DomainError].
     */
    override suspend fun getSubscriptionMinutes(): SubscriptionMinutesDto? = try {
        client.get(AlmahirApi.Students.SUBSCRIPTION_MINUTES)
            .body<ApiResponse<SubscriptionMinutesDto>>()
            .data
    } catch (notFound: ClientRequestException) {
        if (notFound.response.status == HttpStatusCode.NotFound) null else throw notFound
    }

    override suspend fun createIntention(
        packageId: String,
        method: String,
        idempotencyKey: String,
    ): PaymentIntentionDto = client.post(AlmahirApi.Payment.CREATE_INTENTION) {
        contentType(ContentType.Application.Json)
        setBody(CreateIntentionRequest(packageId, method, idempotencyKey))
    }.body()

    override suspend fun getPaymentStatus(intentionId: String): PaymentOutcomeDto =
        client.get(AlmahirApi.Payment.statusUrl(intentionId)).body()
}
