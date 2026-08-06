package com.pasic.receipt.ui.scan.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.X

/**
 * 상단 돋보기 아이콘 핑퐁 이동 애니메이션 컴포저블 (사각 프레임 없음)
 */
@Composable
fun ScanningMagnifierEmblem(
    modifier: Modifier = Modifier
) {
    // 돋보기 아이콘 상하/좌우 핑퐁 이동 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "magnifierScan")

    val scanOffsetX by infiniteTransition.animateFloat(
        initialValue = -14f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanX"
    )

    val scanOffsetY by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanY"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(68.dp)
    ) {
        Icon(
            imageVector = Lucide.Search,
            contentDescription = "스캔 돋보기",
            tint = Color.White,
            modifier = Modifier
                .offset(x = scanOffsetX.dp, y = scanOffsetY.dp)
                .size(56.dp)
        )
    }
}

/**
 * 영수증 AI 분석/스캔 진행 중 표시되는 투명 글래스모피즘 오버레이 UI
 */
@Composable
fun AiScanningOverlay(
    scanStep: Int,
    isStepDone: Boolean,
    capturedBitmap: Bitmap? = null,
    onCancel: () -> Unit
) {
    // 끊김 없이 지속적으로 흐르는 실키한 프로그레스 바 애니메이션
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
            .clickable(enabled = false) {}, // Intercept touch events
        contentAlignment = Alignment.Center
    ) {
        // 1. Captured Photo Background Freeze (If available)
        if (capturedBitmap != null) {
            Image(
                bitmap = capturedBitmap.asImageBitmap(),
                contentDescription = "촬영된 영수증 배경",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Dark Translucent Overlay Filter
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.60f))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Spacer(modifier = Modifier.height(98.dp))

            // 1. Top Magnifying Glass Emblem (Pure White, Larger, No Box Frame)
            ScanningMagnifierEmblem()

            Spacer(modifier = Modifier.height(20.dp))

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

            Spacer(modifier = Modifier.height(90.dp))

            // 3. Stage Card with Pure Translucent Glass Finish & Fade Transition
            val stepTitle = when (scanStep) {
                1 -> "이미지 텍스트 스캔 중"
                2 -> "상호명 및 금액 추출 중"
                else -> "카테고리 자동 분류 중"
            }

            AnimatedContent(
                targetState = scanStep to stepTitle,
                transitionSpec = {
                    (slideInVertically(
                        animationSpec = tween(780, easing = FastOutSlowInEasing),
                        initialOffsetY = { height -> (height * 0.55f).toInt() }
                    ) + fadeIn(
                        animationSpec = tween(780, easing = FastOutSlowInEasing)
                    ) + scaleIn(
                        initialScale = 0.90f,
                        animationSpec = tween(780, easing = FastOutSlowInEasing)
                    )).togetherWith(
                        slideOutVertically(
                            animationSpec = tween(680, easing = FastOutSlowInEasing),
                            targetOffsetY = { height -> -(height * 0.55f).toInt() }
                        ) + fadeOut(
                            animationSpec = tween(680, easing = FastOutSlowInEasing)
                        ) + scaleOut(
                            targetScale = 0.84f,
                            animationSpec = tween(680, easing = FastOutSlowInEasing)
                        )
                    )
                },
                label = "slideUpStepCardTransition"
            ) { (_, titleText) ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF2A2E35).copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)
                    ) {
                        // Left Indicator Icon (Spinning Arc ➔ Crisp Center Pop Check Circle)
                        AnimatedContent(
                            targetState = isStepDone,
                            transitionSpec = {
                                (scaleIn(
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessHigh,
                                        dampingRatio = Spring.DampingRatioNoBouncy
                                    ),
                                    initialScale = 0.2f
                                ) + fadeIn(tween(180))).togetherWith(fadeOut(tween(120)))
                            },
                            label = "checkIconPopTransition"
                        ) { done ->
                            if (done) {
                                // Completed: Solid Blue Check Circle (Exact Match to Reference Photo)
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2563EB))
                                ) {
                                    Icon(
                                        imageVector = Lucide.Check,
                                        contentDescription = "완료",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                // In Progress: Smooth Blue Spinning Progress Arc
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = Color(0xFF38BDF8),
                                    trackColor = Color.White.copy(alpha = 0.20f),
                                    strokeWidth = 2.5.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = titleText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(44.dp))

            // 4. Custom Solid Continuous Blue Progress Bar (No M3 Stop Indicator/Gap Breaks)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF38BDF8), Color(0xFF2563EB))
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(56.dp))

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
