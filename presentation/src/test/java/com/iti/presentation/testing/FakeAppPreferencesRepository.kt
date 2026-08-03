package com.iti.presentation.testing

import com.iti.domain.core.Result
import com.iti.domain.model.User
import com.iti.domain.settings.model.AppLanguage
import com.iti.domain.settings.model.AppPreferences
import com.iti.domain.settings.model.ThemeMode
import com.iti.domain.settings.repository.AppPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [AppPreferencesRepository] shared by the presentation tests — behaves like the real
 * store: every write re-emits on [preferences], and [saveUser]/[clearUser] back the
 * `GetCurrentUserUseCase` path that reads the signed-in user out of the preferences.
 */
class FakeAppPreferencesRepository(
    initial: AppPreferences = AppPreferences(),
) : AppPreferencesRepository {

    private val state = MutableStateFlow(initial)

    val current: AppPreferences get() = state.value

    override val preferences: Flow<AppPreferences> = state

    override suspend fun setThemeMode(mode: ThemeMode): Result<Unit> {
        state.value = state.value.copy(themeMode = mode)
        return Result.Success(Unit)
    }

    override suspend fun setLanguage(language: AppLanguage): Result<Unit> {
        state.value = state.value.copy(language = language)
        return Result.Success(Unit)
    }

    override suspend fun setRemindersEnabled(enabled: Boolean): Result<Unit> {
        state.value = state.value.copy(remindersEnabled = enabled)
        return Result.Success(Unit)
    }

    override suspend fun setErrorSoundsEnabled(enabled: Boolean): Result<Unit> {
        state.value = state.value.copy(errorSoundsEnabled = enabled)
        return Result.Success(Unit)
    }

    override suspend fun setDataSaverEnabled(enabled: Boolean): Result<Unit> {
        state.value = state.value.copy(dataSaverEnabled = enabled)
        return Result.Success(Unit)
    }

    override suspend fun saveUser(user: User): Result<Unit> {
        state.value = state.value.copy(user = user)
        return Result.Success(Unit)
    }

    override suspend fun clearUser(): Result<Unit> {
        state.value = state.value.copy(user = null)
        return Result.Success(Unit)
    }
}
