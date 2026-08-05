package com.pasic.receipt.ui.scan.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.clickable
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Scan
import com.composables.icons.lucide.Search

/**
 * 카메라 가이드 뷰파인더 사각형 및 안내 라인 UI
 */
@Composable
fun ReceiptScanOverlay() {
    var isGuideVisible by remember { mutableStateOf(true) }

    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("lottie/receipt-scan-v2.json"))

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(5000)
        isGuideVisible = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.5f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.6f)
                    )
                ),
                size = size
            )
        }

        AnimatedVisibility(
            visible = isGuideVisible,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(600)),
            modifier = Modifier
                .fillMaxSize()
                .clickable { isGuideVisible = false }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LottieAnimation(
                        composition = composition,
                        iterations = LottieConstants.IterateForever,
                        modifier = Modifier.size(180.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "직접 방문한 장소 영수증의\n사업자 정보와 결제 정보가\n잘 나오게 찍어주세요.",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 25.sp
                    )
                }
            }
        }
    }
}

/**
 * 카메라 촬영 가이드 팁 안내 다이얼로그
 */
@Composable
fun CameraHelpDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E293B),
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2563EB).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Info,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "영수증 촬영 팁",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "1. 영수증을 평평한 곳에 놓아주세요.\n2. 조명이 어둡지 않은 곳에서 촬영하세요.\n3. 상호명, 일시, 금액이 잘 보이도록 사각형에 맞춰주세요.",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2563EB),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = "확인했습니다",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
