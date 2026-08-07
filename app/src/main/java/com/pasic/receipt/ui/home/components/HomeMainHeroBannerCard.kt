package com.pasic.receipt.ui.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ArrowRight
import com.composables.icons.lucide.Lucide
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeMainHeroBannerCard(
    todayCount: Int = 0,
    todayAmountFormatted: String = "0원",
    onCardClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 오늘 영수증 스캔 건수에 따른 동적 배지 텍스트 및 문구
    val (badgeText, mainHeadline) = when {
        todayCount == 0 -> "★ 기록 대기 중" to "오늘 영수증을 스캔하고\n지출을 기록해 보세요!"
        todayCount == 1 -> "★ 오늘 첫 영수증" to "오늘 첫 영수증을\n성공적으로 기록했어요!"
        else -> "★ 프로 기록러" to "오늘도 꼼꼼하게\n영수증을 챙기셨네요!"
    }

    // 1. 화면 진입 바운스 스케일 & 페이드인 애니메이션
    val entranceScale = remember { Animatable(0f) }
    val entranceAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            entranceAlpha.animateTo(1f, animationSpec = tween(400))
        }
        entranceScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    // 2. 무한 부유 유동 애니메이션 (위아래 둥실 상하 이동 & 미세 회전)
    val infiniteTransition = rememberInfiniteTransition(label = "piggyFloatingLoop")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatingY"
    )

    val rotationDegrees by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatingRotation"
    )

    // 3. 터치 바운스 & 4번째 연달아 클릭 시 비눗방울 연쇄 터짐 이스터에그 상태
    val coroutineScope = rememberCoroutineScope()
    var tapCount by remember { mutableIntStateOf(0) }
    var isPopped by remember { mutableStateOf(false) }

    val tapScale = remember { Animatable(1f) }
    val tapRotation = remember { Animatable(0f) }

    // 비눗방울 투명도, 스케일 및 돼지 아이콘 중력 낙하 변위 / 기울기 회전
    val mainBubbleAlpha = remember { Animatable(1f) }
    val mainBubbleScale = remember { Animatable(1f) }
    val smallBubble1Alpha = remember { Animatable(1f) }
    val smallBubble2Alpha = remember { Animatable(1f) }
    val pigDropY = remember { Animatable(0f) }
    val pigDropRotation = remember { Animatable(0f) }

    fun triggerJiggle() {
        tapCount++
        coroutineScope.launch {
            if (tapCount % 4 == 0) {
                if (isPopped) return@launch
                isPopped = true

                // 💥 1단계: 메인 유리 비눗방울만 팽창 후 팡-! 터짐 (투명도 0f)
                launch {
                    mainBubbleScale.animateTo(1.22f, animationSpec = tween(90, easing = FastOutSlowInEasing))
                    launch {
                        mainBubbleScale.animateTo(1.45f, animationSpec = tween(100, easing = FastOutSlowInEasing))
                    }
                    launch {
                        mainBubbleAlpha.animateTo(0f, animationSpec = tween(100, easing = FastOutSlowInEasing))
                    }
                }

                // 💥 2단계: 작은 유동 비눗방울 1번/2번 0.1초 시차 연쇄 터짐
                launch {
                    delay(90)
                    smallBubble1Alpha.animateTo(0f, animationSpec = tween(90, easing = FastOutSlowInEasing))
                    delay(70)
                    smallBubble2Alpha.animateTo(0f, animationSpec = tween(90, easing = FastOutSlowInEasing))
                }

                // 🐽 3단계: 비눗방울 부력을 잃은 돼지 아이콘의 자연스러운 중력 가속도 낙하 (22dp) + 갸우뚱 기우는 회전(-8도) + 착지 쿵 반동
                launch {
                    delay(80)
                    // 기울기 회전 (-8도)
                    launch {
                        pigDropRotation.animateTo(-8f, animationSpec = tween(160, easing = FastOutSlowInEasing))
                    }
                    // 자유 낙하 가속도 (22dp)
                    pigDropY.animateTo(22f, animationSpec = tween(180, easing = FastOutLinearInEasing))
                    // 착지 쿵-! 반동 바운스 (22dp -> 18dp -> 22dp)
                    pigDropY.animateTo(
                        22f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }

                // ⏳ 4단계: 비눗방울이 터진 상태로 2.0초간 정적 대기
                delay(2000)

                // ✨ 5단계: 2초 후 새 비눗방울이 뿅 생성되며 돼지가 원래 부유 위치로 복귀 (회전도 0도로 원복)
                launch {
                    launch {
                        pigDropRotation.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessLow))
                    }
                    pigDropY.animateTo(
                        0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
                launch {
                    mainBubbleScale.snapTo(0.3f)
                    mainBubbleAlpha.animateTo(1f, animationSpec = tween(180))
                    mainBubbleScale.animateTo(
                        1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
                launch {
                    delay(100)
                    smallBubble1Alpha.animateTo(1f, animationSpec = spring(stiffness = Spring.StiffnessLow))
                    delay(80)
                    smallBubble2Alpha.animateTo(1f, animationSpec = spring(stiffness = Spring.StiffnessLow))
                }

                delay(300)
                isPopped = false
            } else {
                // 일반 1~3번째 클릭 시 몽글몽글 흔들림
                launch {
                    tapScale.animateTo(1.06f, animationSpec = tween(120, easing = FastOutSlowInEasing))
                    tapScale.animateTo(
                        1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
                launch {
                    tapRotation.animateTo(-5f, animationSpec = tween(90, easing = FastOutSlowInEasing))
                    tapRotation.animateTo(5f, animationSpec = tween(100, easing = FastOutSlowInEasing))
                    tapRotation.animateTo(-2f, animationSpec = tween(90, easing = FastOutSlowInEasing))
                    tapRotation.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessLow))
                }
            }
        }
    }

    var showLevelDialog by remember { mutableStateOf(false) }

    if (showLevelDialog) {
        UserLevelInfoDialog(
            currentCount = todayCount,
            onDismissRequest = { showLevelDialog = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF3B82F6),
                        Color(0xFF2563EB),
                        Color(0xFF1D4ED8)
                    )
                )
            )
            .clickable { triggerJiggle() }
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Column {
            // 상단 등급 배지 알약 버튼 (클릭 시 등급 안내 다이얼로그 팝업)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.22f))
                    .clickable { showLevelDialog = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 메인 헤드라인 타이틀 문구
            Text(
                text = mainHeadline,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 32.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 돼지 저금통 마스코트 & 상단 유동 비눗방울 그래픽 영역
            val context = LocalContext.current
            val piggyResId = remember(context) {
                context.resources.getIdentifier("img_piggy_bank", "drawable", context.packageName).takeIf { it != 0 }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 12.dp, top = 16.dp)
            ) {
                // 유동 비눗방울 1번 (34.dp - 상단 중앙)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(x = (-13).dp, y = (-75).dp)
                        .size(34.dp)
                        .graphicsLayer {
                            scaleX = entranceScale.value * (1f + (tapScale.value - 1f) * 0.5f)
                            scaleY = entranceScale.value * (1f + (tapScale.value - 1f) * 0.5f)
                            alpha = entranceAlpha.value * smallBubble1Alpha.value
                            translationY = (if (isPopped) 0f else offsetY.dp.toPx() * 1.25f) + pigDropY.value.dp.toPx()
                            rotationZ = -rotationDegrees * 1.5f - tapRotation.value * 0.8f
                        }
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.45f),
                                    Color(0x55E0F2FE),
                                    Color(0x40F472B6)
                                )
                            )
                        )
                        .border(
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
                            shape = CircleShape
                        )
                )

                // 유동 비눗방울 2번 (22.dp - 우측 상단)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 10.dp, y = (-36).dp)
                        .size(22.dp)
                        .graphicsLayer {
                            scaleX = entranceScale.value * (1f + (tapScale.value - 1f) * 0.4f)
                            scaleY = entranceScale.value * (1f + (tapScale.value - 1f) * 0.4f)
                            alpha = entranceAlpha.value * smallBubble2Alpha.value
                            translationY = (if (isPopped) 0f else offsetY.dp.toPx() * 0.85f) + pigDropY.value.dp.toPx()
                            rotationZ = rotationDegrees * 2f + tapRotation.value * 1.0f
                        }
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.55f),
                                    Color(0x66E0F2FE),
                                    Color(0x443B82F6)
                                )
                            )
                        )
                        .border(
                            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.85f)),
                            shape = CircleShape
                        )
                )

                // 메인 160.dp 유리 비눗방울 & 200.dp 돼지 저금통 컨테이너
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .graphicsLayer {
                            scaleX = entranceScale.value * tapScale.value
                            scaleY = entranceScale.value * tapScale.value
                            alpha = entranceAlpha.value
                            translationY = (if (isPopped) 0f else offsetY.dp.toPx()) + pigDropY.value.dp.toPx()
                            rotationZ = (if (isPopped) 0f else rotationDegrees) + tapRotation.value + pigDropRotation.value
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // 유리 비눗방울 외곽 껍질 (터질 때 이 껍질만 독립적으로 소멸)
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .graphicsLayer {
                                scaleX = mainBubbleScale.value
                                scaleY = mainBubbleScale.value
                                alpha = mainBubbleAlpha.value
                            }
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.35f),
                                        Color(0x40E0F2FE),
                                        Color(0x30F472B6),
                                        Color(0x203B82F6)
                                    )
                                )
                            )
                            .border(
                                border = BorderStroke(
                                    width = 1.5.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.9f),
                                            Color(0x9993C5FD),
                                            Color(0x99F472B6),
                                            Color.White.copy(alpha = 0.4f)
                                        )
                                    )
                                ),
                                shape = CircleShape
                            )
                    ) {
                        // 좌측 상단 2단 듀얼 글래스 반사광 (미세 은은 스타일: 8dp 메인 도트 + 4dp 미니 도트)
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 27.dp, top = 22.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.35f))
                            )
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.20f))
                            )
                        }
                    }

                    // 돼지 저금통 마스코트 아이콘 (부모 Box 160.dp 제약을 뚫고 200.dp로 확대, 시각적 중앙 보정: 우측 3dp, 아래 7dp)
                    if (piggyResId != null) {
                        Image(
                            painter = painterResource(id = piggyResId),
                            contentDescription = "돼지 저금통 마스코트",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .offset(x = 3.dp, y = 7.dp)
                                .requiredSize(200.dp)
                        )
                    } else {
                        Text(
                            text = "🐷 💰",
                            fontSize = 72.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 하단 오늘 기록한 지출 알약 상자 (클릭 시 영수증 목록 화면 이동)
            Surface(
                onClick = onCardClick,
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "오늘 기록한 지출",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = todayAmountFormatted,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // 우측 화살표 동그라미 버튼
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.ArrowRight,
                            contentDescription = "영수증 목록으로 이동",
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
