package com.pasic.receipt.ui.home.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Archive
import com.composables.icons.lucide.FileSpreadsheet
import com.composables.icons.lucide.Headset
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PenLine
import com.composables.icons.lucide.X
import com.pasic.receipt.ui.theme.ModernCardBlue
import com.pasic.receipt.ui.theme.TextPrimary
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.delay

/**
 * 홈 화면 [더보기] 클릭 시 위로 펼쳐지는 모던 스피드 다이얼(Speed Dial) 메뉴.
 * - Haze 공식 프리셋 HazeMaterials.ultraThin() + 소프트 아이스 블루 틴트 (#E0EDFE 30%)
 * - 4개 메뉴 + 닫기 버튼 모두 평상시 화이트 톤 ➔ 터치 누르는 순간(Pressed) 사진처럼 로얄 블루 반전
 * - 각 아이템별 Animatable 기반의 완벽한 순차 스프링 팝 (아래 -> 위) & 닫기 회전 모션
 */
@Composable
fun HomeSpeedDialMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    onCustomerCenterClick: () -> Unit,
    onMonthlyReportClick: () -> Unit,
    onManualInputClick: () -> Unit,
    onZipBackupClick: () -> Unit,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    // 스피드 다이얼 열려있을 때 뒤로가기 누르면 닫기
    BackHandler(enabled = visible) {
        onDismiss()
    }

    // 닫기(✕) 버튼 90도 회전 애니메이션
    val closeRotation = remember { Animatable(-90f) }

    LaunchedEffect(visible) {
        if (visible) {
            closeRotation.snapTo(-90f)
            closeRotation.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        } else {
            closeRotation.animateTo(
                targetValue = -90f,
                animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
            )
        }
    }

    // 닫기 버튼 터치 인터랙션
    val closeInteractionSource = remember { MutableInteractionSource() }
    val isClosePressed by closeInteractionSource.collectIsPressedAsState()

    val closeBgColor by animateColorAsState(
        targetValue = if (isClosePressed) ModernCardBlue else Color.White,
        animationSpec = tween(120),
        label = "closeBgColor"
    )
    val closeIconTint by animateColorAsState(
        targetValue = if (isClosePressed) Color.White else ModernCardBlue,
        animationSpec = tween(120),
        label = "closeIconTint"
    )
    val closeScale by animateFloatAsState(
        targetValue = if (isClosePressed) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "closeScale"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(180)),
        exit = fadeOut(animationSpec = tween(durationMillis = 200, delayMillis = 100)),
        modifier = modifier.fillMaxSize()
    ) {
        val hazeModifier = if (hazeState != null) {
            Modifier
                .hazeEffect(
                    state = hazeState,
                    style = HazeMaterials.ultraThin()
                )
                .background(Color(0xFFE0EDFE).copy(alpha = 0.30f))
        } else {
            Modifier.background(Color(0xFFE0EDFE).copy(alpha = 0.50f))
        }

        // 1. 전체 화면 Haze 마일드 블러 + 소프트 아이스 블루 틴트 & 탭 시 닫힘 처리
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(hazeModifier)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            // 2. 우측 하단 스피드 다이얼 스택 (아래에서 위로 쌓임)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 33.dp, bottom = 110.dp), // 우측 33dp, 바닥 110dp
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1번 (맨 위): 고객센터 (itemIndex = 0)
                StaggeredSpeedDialRow(
                    visible = visible,
                    itemIndex = 0,
                    title = "고객센터",
                    icon = Lucide.Headset,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCustomerCenterClick()
                    }
                )

                // 2번: 이번달 지출 보고서 (itemIndex = 1)
                StaggeredSpeedDialRow(
                    visible = visible,
                    itemIndex = 1,
                    title = "이번달 지출 보고서",
                    icon = Lucide.FileSpreadsheet,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMonthlyReportClick()
                    }
                )

                // 3번: 직접 수기 입력 (itemIndex = 2)
                StaggeredSpeedDialRow(
                    visible = visible,
                    itemIndex = 2,
                    title = "직접 수기 입력",
                    icon = Lucide.PenLine,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onManualInputClick()
                    }
                )

                // 4번: ZIP 파일로 안전하게 보관 (itemIndex = 3, 가장 먼저 튀어나옴)
                StaggeredSpeedDialRow(
                    visible = visible,
                    itemIndex = 3,
                    title = "ZIP 파일로 안전하게 보관",
                    icon = Lucide.Archive,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onZipBackupClick()
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 메인 닫기 [✕] 버튼 (90도 회전 스프링 + 터치 시 블루 반전)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer {
                            rotationZ = closeRotation.value
                            scaleX = closeScale
                            scaleY = closeScale
                        }
                        .shadow(
                            elevation = if (isClosePressed) 12.dp else 10.dp,
                            shape = CircleShape,
                            spotColor = if (isClosePressed) ModernCardBlue.copy(alpha = 0.35f) else Color(0xFF1E3A8A).copy(alpha = 0.16f),
                            ambientColor = Color(0xFF0F172A).copy(alpha = 0.08f)
                        )
                        .clip(CircleShape)
                        .background(closeBgColor)
                        .clickable(
                            interactionSource = closeInteractionSource,
                            indication = null
                        ) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = "닫기",
                        tint = closeIconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * itemIndex에 따라 계단식 시차(Staggered Delay)를 두고 확실하게 솟아오르고,
 * 누르는 순간(Pressed) 사진처럼 선명한 로얄 블루로 반전되는 아이템 컴포저블
 */
@Composable
private fun StaggeredSpeedDialRow(
    visible: Boolean,
    itemIndex: Int,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val targetOffsetPx = with(density) { 36.dp.toPx() }

    val animProgress = remember { Animatable(0f) }

    // 등장 순서: 4번(index 3) ➔ 1번(index 0) (아래에서 위로 40ms 간격)
    val enterDelayMs = (3 - itemIndex) * 40L
    // 퇴장 순서: 1번(index 0) ➔ 4번(index 3) (위에서 아래로 25ms 간격)
    val exitDelayMs = itemIndex * 25L

    LaunchedEffect(visible) {
        if (visible) {
            animProgress.snapTo(0f)
            delay(enterDelayMs)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        } else {
            delay(exitDelayMs)
            animProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 140,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    // 터치 프레스 인터랙션 (손가락으로 누르는 순간 블루 반전 피드백)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 1. 배경색: 화이트 ➔ 로얄 블루 (#0066FF)
    val cardBgColor by animateColorAsState(
        targetValue = if (isPressed) ModernCardBlue else Color.White,
        animationSpec = tween(120),
        label = "rowBgColor_$itemIndex"
    )

    // 2. 텍스트색: 다크 ➔ 순백색 (Bold)
    val textColor by animateColorAsState(
        targetValue = if (isPressed) Color.White else TextPrimary,
        animationSpec = tween(120),
        label = "rowTextColor_$itemIndex"
    )

    // 3. 아이콘색: 블루 ➔ 순백색
    val iconTint by animateColorAsState(
        targetValue = if (isPressed) Color.White else ModernCardBlue,
        animationSpec = tween(120),
        label = "rowIconTint_$itemIndex"
    )

    // 4. 그림자 색상: 은은한 섀도우 ➔ 블루 글로우
    val shadowSpotColor by animateColorAsState(
        targetValue = if (isPressed) ModernCardBlue.copy(alpha = 0.40f) else Color(0xFF1E3A8A).copy(alpha = 0.14f),
        animationSpec = tween(120),
        label = "rowShadowColor_$itemIndex"
    )

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pressScale_$itemIndex"
    )

    // 진입/퇴장 복합 모션 (Scale, OffsetY, Alpha)
    val currentProgress = animProgress.value
    val currentScale = (0.6f + (0.4f * currentProgress)) * pressScale
    val currentOffsetY = (1f - currentProgress) * targetOffsetPx
    val currentAlpha = currentProgress.coerceIn(0f, 1f)

    if (currentProgress > 0.01f || visible) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = currentScale
                    scaleY = currentScale
                    translationY = currentOffsetY
                    alpha = currentAlpha
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
        ) {
            // 1. 좌측 알약 라벨 (누를 때 사진처럼 로얄 블루 배경 + 화이트 볼드 텍스트)
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = if (isPressed) 10.dp else 8.dp,
                        shape = RoundedCornerShape(12.dp),
                        spotColor = shadowSpotColor,
                        ambientColor = Color(0xFF0F172A).copy(alpha = 0.08f)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardBgColor)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = if (isPressed) FontWeight.Bold else FontWeight.SemiBold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // 2. 우측 원형 아이콘 버튼 (누를 때 로얄 블루 배경 + 화이트 아이콘)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(
                        elevation = if (isPressed) 12.dp else 10.dp,
                        shape = CircleShape,
                        spotColor = shadowSpotColor,
                        ambientColor = Color(0xFF0F172A).copy(alpha = 0.08f)
                    )
                    .clip(CircleShape)
                    .background(cardBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
