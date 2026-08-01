package com.iti.data.sheikh.auth.remote

import com.iti.data.core.network.AlmahirApi
import com.iti.data.core.network.dto.ApiResponse
import com.iti.data.core.network.dto.RefreshTokenRequest
import com.iti.data.user.auth.remote.FilePart
import com.iti.data.user.auth.remote.dto.AuthDataDto
import com.iti.data.user.auth.remote.dto.GoogleAuthRequest
import com.iti.data.user.auth.remote.dto.LoginRequest
import com.iti.data.user.auth.remote.dto.LogoutRequest
import com.iti.data.user.auth.remote.dto.RegisterRequest
import com.iti.data.user.auth.remote.dto.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

/**
 * Same wire shapes as [com.iti.data.user.auth.remote.AuthRemoteDataSource] (identical request/
 * response DTOs, reused as-is) — only the sheikh-namespaced paths differ.
 */
class SheikhAuthRemoteDataSource(
    private val client: HttpClient,
    private val json: Json,
) {

    suspend fun register(
        request: RegisterRequest,
        profilePicture: FilePart? = null,
    ): ApiResponse<UserDto> = client.submitFormWithBinaryData(
        url = AlmahirApi.Auth.Sheikh.REGISTER,
        formData = formData {
            append(
                PART_DATA,
                json.encodeToString(request),
                Headers.build {
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                },
            )
            profilePicture?.let { picture ->
                append(
                    PART_FILE,
                    picture.bytes,
                    Headers.build {
                        append(HttpHeaders.ContentType, picture.mimeType)
                        append(HttpHeaders.ContentDisposition, "filename=\"${picture.fileName}\"")
                    },
                )
            }
        },
    ).body()

    suspend fun login(request: LoginRequest): ApiResponse<AuthDataDto> =
        postJson(AlmahirApi.Auth.Sheikh.LOGIN, request)

    suspend fun loginWithGoogle(request: GoogleAuthRequest): ApiResponse<AuthDataDto> =
        postJson(AlmahirApi.Auth.Sheikh.GOOGLE, request)

    suspend fun refresh(request: RefreshTokenRequest): ApiResponse<AuthDataDto> =
        postJson(AlmahirApi.Auth.Sheikh.REFRESH, request)

    suspend fun logout(request: LogoutRequest): ApiResponse<Unit> =
        postJson(AlmahirApi.Auth.LOGOUT, request)

    private suspend inline fun <reified B : Any, reified R> postJson(
        path: String,
        body: B,
    ): ApiResponse<R> = client.post(path) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }.body()

    private companion object {
        const val PART_DATA = "data"
        const val PART_FILE = "file"
    }
}
