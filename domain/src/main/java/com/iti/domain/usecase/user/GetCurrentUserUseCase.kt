package com.iti.domain.usecase.user

import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import com.iti.domain.model.User
import com.iti.domain.settings.repository.AppPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetCurrentUserUseCase(
    private val repository: AppPreferencesRepository,
) {
    operator fun invoke(): Flow<Result<User>> = repository.preferences.map { prefs ->
        val user = prefs.user
        if (user != null) {
            Result.Success(user)
        } else {
            Result.Error(DomainError.ServerError("User not found in preferences"))
        }
    }
}
