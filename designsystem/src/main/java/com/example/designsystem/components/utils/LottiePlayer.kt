package com.example.designsystem.components.utils

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
internal fun LottiePlayer(
    @RawRes resId: Int,
    iterations: Int,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    onAnimationFinished: (() -> Unit)? = null,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resId))

    val animationState = animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
    )

    // Track the latest callback so the LaunchedEffect below always invokes
    // the current lambda even if the caller passes a fresh closure on
    // every recomposition.
    val currentOnFinished by rememberUpdatedState(onAnimationFinished)

    // Guard: while `composition` is still loading, Lottie reports
    // `isAtEnd = true` (because both `progress` and `endProgress` are 0f),
    // which would fire the callback before a single frame renders.
    LaunchedEffect(animationState.isAtEnd, composition) {
        if (composition != null && animationState.isAtEnd) {
            currentOnFinished?.invoke()
        }
    }

    LottieAnimation(
        composition = composition,
        progress = { animationState.progress },
        modifier = modifier.size(size),
    )
}
