package com.iti.al_mahir.sheikh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme
import com.iti.al_mahir.sheikh.navigation.SheikhAppNavHost
import com.iti.domain.settings.model.ThemeMode
import com.iti.domain.usecase.settings.ObserveAppPreferencesUseCase
import org.koin.android.ext.android.inject
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val observePreferences: ObserveAppPreferencesUseCase by inject()

    private var isPreferencesReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isPreferencesReady }

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val preferences by observePreferences().collectAsStateWithLifecycle(initialValue = null)

            val systemInDarkTheme = isSystemInDarkTheme()
            val view = LocalView.current

            val darkTheme = when (preferences?.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM, null -> systemInDarkTheme
            }

            val locale = remember(preferences?.language) {
                preferences?.language?.let { Locale.forLanguageTag(it.tag) } ?: Locale.getDefault()
            }

            LaunchedEffect(preferences) {
                if (preferences != null) isPreferencesReady = true
            }

            LaunchedEffect(darkTheme, view) {
                val window = (view.context as ComponentActivity).window
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }

            AlMahirTheme(isDarkTheme = darkTheme, locale = locale) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Theme.colors.backGround),
                ) {
                    SheikhAppNavHost()
                }
            }
        }
    }
}
