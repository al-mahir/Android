package com.iti.domain.usecase.user

import com.iti.domain.model.User
import com.iti.domain.repository.AlmahirRepository
import kotlinx.coroutines.flow.Flow

class GetCurrentUserUseCase(
    private val repository: AlmahirRepository,
) {
    operator fun invoke(): Flow<User> = repository.observeCurrentUser()
}
