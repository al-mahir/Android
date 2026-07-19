package com.iti.al_mahir

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme
import com.iti.al_mahir.navigation.AppNavHost

import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.OvershootInterpolator
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var isAppReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        
        // Keep the splash screen visible for 1 second so the logo is displayed clearly
        splashScreen.setKeepOnScreenCondition { !isAppReady }
        
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
            delay(1000)
            isAppReady = true
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlMahirTheme {
                val darkTheme = isSystemInDarkTheme()
                val view = LocalView.current

                SideEffect {
                    val window = (view.context as ComponentActivity).window
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Theme.colors.backGround),
                ) {
                    AppNavHost()
                }
            }
        }
    }
}
