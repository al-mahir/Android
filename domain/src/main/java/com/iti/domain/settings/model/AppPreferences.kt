package com.iti.domain.settings.model


enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}


enum class AppLanguage(val tag: String) {
    ARABIC("ar"),
    ENGLISH("en"),
    ;

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag == tag } ?: ARABIC
    }
}


data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.ARABIC,
    val remindersEnabled: Boolean = true,
    val errorSoundsEnabled: Boolean = true,
    val dataSaverEnabled: Boolean = false,
)
