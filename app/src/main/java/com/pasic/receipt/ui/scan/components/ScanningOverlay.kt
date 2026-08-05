package com.pasic.receipt.ui.scan.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ReceiptText
import com.composables.icons.lucide.X

/**
 * 영수증 AI 분석/스캔 진행 중 표시되는 투명 글래스모피즘 오버레이 UI
 */
@Composable
fun AiScanningOverlay(
    scanStep: Int,
    isStepDone: Boolean,
    onCancel: () -> Unit
) {
    // 1. 영수증 엠블럼 상하 Floating 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "floatingEmblem")
    val floatingY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emblemY"
    )

    // 2. 끊김 없이 지속적으로 흐르는 실키한 프로그레스 바 애니메이션
    val targetProgress = when {
        scanStep == 1 && !isStepDone -> 0.28f
        scanStep == 1 && isStepDone -> 0.48f
        scanStep == 2 && !isStepDone -> 0.68f
        scanStep == 2 && isStepDone -> 0.85f
        scanStep == 3 && !isStepDone -> 0.95f
        else -> 1.0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1200, easing = androidx.compose.animation.core.LinearEasing),
        label = "progressBar"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(enabled = false) {}, // Intercept touch events
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // 1. Top Receipt Emblem
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(y = floatingY.dp)
                    .size(64.dp)
            ) {
                Icon(
                    imageVector = Lucide.ReceiptText,
                    contentDescription = "영수증 분석",
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 2. Main Title & Subtitle
            Text(
                text = "영수증 분석 중",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "잠시만 기다려주세요",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.70f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 3. Stage Card with Vertical Slide Up Transition (Pure Translucent Glass)
            val stepTitle = when (scanStep) {
                1 -> "이미지 텍스트 스캔 중"
                2 -> "상호명 및 금액 추출 중"
                else -> "카테고리 자동 분류 중"
            }

            AnimatedContent(
                targetState = scanStep to stepTitle,
                transitionSpec = {
                    (androidx.compose.animation.slideInVertically { height -> height } + fadeIn()).togetherWith(
                        androidx.compose.animation.slideOutVertically { height -> -height } + fadeOut()
                    )
                },
                label = "verticalStepCardTransition"
            ) { (_, titleText) ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                    ) {
                        // Left Indicator Icon (Spinning Arc ➔ Bounce Check Pop)
                        AnimatedContent(
                            targetState = isStepDone,
                            transitionSpec = {
                                (androidx.compose.animation.scaleIn(
                                    animationSpec = androidx.compose.animation.core.spring(
                                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
                                    )
                                ) + fadeIn()).togetherWith(fadeOut())
                            },
                            label = "checkIconBounceTransition"
                        ) { done ->
                            if (done) {
                                // Completed: Blue Check Circle Pop
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2563EB))
                                ) {
                                    Icon(
                                        imageVector = Lucide.Check,
                                        contentDescription = "완료",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            } else {
                                // In Progress: Blue Spinning Progress Arc
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF38BDF8),
                                    trackColor = Color.White.copy(alpha = 0.20f),
                                    strokeWidth = 2.5.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = titleText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 4. Thin Blue Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF2563EB),
                trackColor = Color.White.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(60.dp))

            // 5. Bottom Cancel Button [✕ 취소하기]
            Surface(
                onClick = onCancel,
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = "취소",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "취소하기",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}
