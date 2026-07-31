package com.iti.al_mahir
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme
import com.iti.al_mahir.navigation.AppNavHost
import java.util.Locale

import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.OvershootInterpolator
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.iti.domain.settings.model.ThemeMode
import com.iti.domain.usecase.settings.ObserveAppPreferencesUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {

    private val observePreferences: ObserveAppPreferencesUseCase by inject()

    private var isSplashDelayDone = false

    private var isPreferencesReady = false

    private val pendingAction = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        splashScreen.setKeepOnScreenCondition { !(isSplashDelayDone && isPreferencesReady) }
        
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val scaleX = ObjectAnimator.ofFloat(splashScreenView.view, View.SCALE_X, 1f, 1.2f)
            val scaleY = ObjectAnimator.ofFloat(splashScreenView.view, View.SCALE_Y, 1f, 1.2f)
            val fadeOut = ObjectAnimator.ofFloat(splashScreenView.view, View.ALPHA, 1f, 0f)
            
            val animatorSet = android.animation.AnimatorSet()
            animatorSet.playTogether(scaleX, scaleY, fadeOut)
            animatorSet.interpolator = OvershootInterpolator()
            animatorSet.duration = 400L
            
            animatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    splashScreenView.remove()
                }
            })
            animatorSet.start()
        }
        
        lifecycleScope.launch {
            delay(1000.milliseconds)
            isSplashDelayDone = true
        }

        super.onCreate(savedInstanceState)
        intent?.action?.let { pendingAction.value = it }

        enableEdgeToEdge()
        setContent {
            val preferences by observePreferences().collectAsStateWithLifecycle(initialValue = null)
            val action by pendingAction.collectAsStateWithLifecycle()

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
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Theme.colors.backGround),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        AppNavHost(
                            pendingAction = action,
                            onActionHandled = { pendingAction.value = null }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        intent.action?.let { pendingAction.value = it }
    }
}
