package com.iti.data.payment.remote

import com.iti.data.core.network.AlmahirApi
import com.iti.data.payment.PaymentDataSource
import com.iti.data.payment.dto.CreateIntentionRequest
import com.iti.data.payment.dto.PaymentIntentionDto
import com.iti.data.payment.dto.PaymentOutcomeDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class PaymentRemoteDataSource(
    private val client: HttpClient,
) : PaymentDataSource {

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
