package com.example.designsystem.components.button

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.example.designsystem.R

@Composable
internal fun LottieButtonLoader(
    tintColor: Color,
    modifier: Modifier = Modifier,
    @RawRes rawRes: Int = R.raw.button_loading,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(rawRes))
    val argb = tintColor.toArgb()
    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.STROKE_COLOR,
            value = argb,
            keyPath = arrayOf("**")
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = argb,
            keyPath = arrayOf("**")
        )
    )
    LottieAnimation(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        dynamicProperties = dynamicProperties,
        modifier = modifier.size(24.dp)
    )
}
