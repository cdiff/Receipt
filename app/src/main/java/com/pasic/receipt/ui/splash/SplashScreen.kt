package com.pasic.receipt.ui.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.pasic.receipt.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val compositionResult = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.splash_animation))
    val composition = compositionResult.value
    val isFailure = compositionResult.isFailure

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1
    )

    // 로딩 실패 시 크래시 없이 즉시 홈 화면으로 안전하게 진입
    LaunchedEffect(isFailure) {
        if (isFailure) {
            onSplashFinished()
        }
    }

    // 애니메이션 1회 완주 시 부드럽게 메인 화면 전환
    LaunchedEffect(progress) {
        if (progress >= 0.99f) {
            onSplashFinished()
        }
    }

    LaunchedEffect(Unit) {
        delay(2800)
        onSplashFinished()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize()
    ) {
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
