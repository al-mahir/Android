package com.iti.data.auth.remote

import com.iti.data.auth.remote.dto.AuthDataDto
import com.iti.data.auth.remote.dto.ForgotPasswordRequest
import com.iti.data.auth.remote.dto.GoogleAuthRequest
import com.iti.data.auth.remote.dto.LoginRequest
import com.iti.data.auth.remote.dto.LogoutRequest
import com.iti.data.auth.remote.dto.RegisterRequest
import com.iti.data.auth.remote.dto.ResetPasswordRequest
import com.iti.data.auth.remote.dto.UserDto
import com.iti.data.core.network.AlmahirApi
import com.iti.data.core.network.dto.ApiResponse
import com.iti.data.core.network.dto.RefreshTokenRequest
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

/** A binary part uploaded alongside a registration (e.g. the profile picture). */
data class FilePart(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is FilePart &&
            fileName == other.fileName &&
            mimeType == other.mimeType &&
            bytes.contentEquals(other.bytes))

    override fun hashCode(): Int =
        (fileName.hashCode() * 31 + mimeType.hashCode()) * 31 + bytes.contentHashCode()
}


class AuthRemoteDataSource(
    private val client: HttpClient,
    private val json: Json,
) {

    suspend fun register(
        request: RegisterRequest,
        profilePicture: FilePart? = null,
    ): ApiResponse<UserDto> = client.submitFormWithBinaryData(
        url = AlmahirApi.Auth.REGISTER,
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
        postJson(AlmahirApi.Auth.LOGIN, request)

    suspend fun loginWithGoogle(request: GoogleAuthRequest): ApiResponse<AuthDataDto> =
        postJson(AlmahirApi.Auth.GOOGLE, request)

    suspend fun refresh(request: RefreshTokenRequest): ApiResponse<AuthDataDto> =
        postJson(AlmahirApi.Auth.REFRESH, request)

    suspend fun logout(request: LogoutRequest): ApiResponse<Unit> =
        postJson(AlmahirApi.Auth.LOGOUT, request)

    suspend fun forgotPassword(request: ForgotPasswordRequest): ApiResponse<Unit> =
        postJson(AlmahirApi.Auth.FORGOT_PASSWORD, request)

    suspend fun resetPassword(request: ResetPasswordRequest): ApiResponse<Unit> =
        postJson(AlmahirApi.Auth.RESET_PASSWORD, request)

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
