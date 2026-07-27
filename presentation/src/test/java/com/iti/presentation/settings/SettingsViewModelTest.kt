package com.iti.presentation.settings

import com.iti.domain.core.Result
import com.iti.domain.settings.model.AppLanguage
import com.iti.domain.settings.model.AppPreferences
import com.iti.domain.settings.model.ThemeMode
import com.iti.domain.settings.repository.AppPreferencesRepository
import com.iti.domain.settings.repository.RecordingsRepository
import com.iti.domain.usecase.settings.DeleteAllRecordingsUseCase
import com.iti.domain.usecase.settings.ObserveAppPreferencesUseCase
import com.iti.domain.usecase.settings.SetAppLanguageUseCase
import com.iti.domain.usecase.settings.SetDataSaverEnabledUseCase
import com.iti.domain.usecase.settings.SetErrorSoundsEnabledUseCase
import com.iti.domain.usecase.settings.SetRemindersEnabledUseCase
import com.iti.domain.usecase.settings.SetThemeModeUseCase
import com.iti.presentation.settings.state.SettingsIntent
import com.iti.presentation.settings.state.SettingsSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `stored preferences replace the defaults once loaded`() = runTest(dispatcher) {
        val repository = FakeAppPreferencesRepository(
            AppPreferences(themeMode = ThemeMode.DARK, language = AppLanguage.ENGLISH),
        )
        val viewModel = viewModel(repository)

        advanceUntilIdle()

        assertFalse(viewModel.currentState.isLoading)
        assertEquals(ThemeMode.DARK, viewModel.currentState.themeMode)
        assertEquals(AppLanguage.ENGLISH, viewModel.currentState.language)
    }

    @Test
    fun `selecting a theme persists it and closes the sheet`() = runTest(dispatcher) {
        val repository = FakeAppPreferencesRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.onIntent(SettingsIntent.ThemeClicked)
        assertEquals(SettingsSheet.THEME, viewModel.currentState.visibleSheet)

        viewModel.onIntent(SettingsIntent.ThemeSelected(ThemeMode.LIGHT))
        advanceUntilIdle()

        assertNull(viewModel.currentState.visibleSheet)
        assertEquals(ThemeMode.LIGHT, repository.current.themeMode)
        // The new value must arrive back through the flow, not from a local copy.
        assertEquals(ThemeMode.LIGHT, viewModel.currentState.themeMode)
    }

    @Test
    fun `selecting a language persists it and closes the sheet`() = runTest(dispatcher) {
        val repository = FakeAppPreferencesRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.onIntent(SettingsIntent.LanguageClicked)
        viewModel.onIntent(SettingsIntent.LanguageSelected(AppLanguage.ENGLISH))
        advanceUntilIdle()

        assertNull(viewModel.currentState.visibleSheet)
        assertEquals(AppLanguage.ENGLISH, repository.current.language)
        assertEquals(AppLanguage.ENGLISH, viewModel.currentState.language)
    }

    @Test
    fun `toggles are written straight through`() = runTest(dispatcher) {
        val repository = FakeAppPreferencesRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.onIntent(SettingsIntent.RemindersToggled(false))
        viewModel.onIntent(SettingsIntent.ErrorSoundsToggled(false))
        viewModel.onIntent(SettingsIntent.DataSaverToggled(true))
        advanceUntilIdle()

        assertFalse(repository.current.remindersEnabled)
        assertFalse(repository.current.errorSoundsEnabled)
        assertTrue(repository.current.dataSaverEnabled)
    }

    @Test
    fun `deleting recordings requires confirmation`() = runTest(dispatcher) {
        val recordings = FakeRecordingsRepository()
        val viewModel = viewModel(FakeAppPreferencesRepository(), recordings)
        advanceUntilIdle()

        viewModel.onIntent(SettingsIntent.DeleteRecordingsClicked)
        advanceUntilIdle()

        // Staged only — nothing is destroyed until the dialog is confirmed.
        assertTrue(viewModel.currentState.isDeleteRecordingsDialogVisible)
        assertEquals(0, recordings.deleteAllCount)

        viewModel.onIntent(SettingsIntent.DeleteRecordingsConfirmed)
        advanceUntilIdle()

        assertEquals(1, recordings.deleteAllCount)
        assertFalse(viewModel.currentState.isDeleteRecordingsDialogVisible)
        assertFalse(viewModel.currentState.isDeletingRecordings)
    }

    @Test
    fun `dismissing the delete dialog deletes nothing`() = runTest(dispatcher) {
        val recordings = FakeRecordingsRepository()
        val viewModel = viewModel(FakeAppPreferencesRepository(), recordings)
        advanceUntilIdle()

        viewModel.onIntent(SettingsIntent.DeleteRecordingsClicked)
        viewModel.onIntent(SettingsIntent.DeleteRecordingsDismissed)
        advanceUntilIdle()

        assertFalse(viewModel.currentState.isDeleteRecordingsDialogVisible)
        assertEquals(0, recordings.deleteAllCount)
    }

    private fun viewModel(
        preferences: AppPreferencesRepository,
        recordings: RecordingsRepository = FakeRecordingsRepository(),
    ) = SettingsViewModel(
        appVersion = "1.0.0",
        observePreferences = ObserveAppPreferencesUseCase(preferences),
        setThemeMode = SetThemeModeUseCase(preferences),
        setLanguage = SetAppLanguageUseCase(preferences),
        setRemindersEnabled = SetRemindersEnabledUseCase(preferences),
        setErrorSoundsEnabled = SetErrorSoundsEnabledUseCase(preferences),
        setDataSaverEnabled = SetDataSaverEnabledUseCase(preferences),
        deleteAllRecordings = DeleteAllRecordingsUseCase(recordings),
    )
}

/** In-memory double that behaves like the real store: every write re-emits on [preferences]. */
private class FakeAppPreferencesRepository(
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
}

private class FakeRecordingsRepository : RecordingsRepository {
    var deleteAllCount = 0
        private set

    override suspend fun deleteAll(): Result<Unit> {
        deleteAllCount++
        return Result.Success(Unit)
    }
}
