package com.pasic.receipt.ui.home.components

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ArrowRight
import com.composables.icons.lucide.Lucide
import kotlinx.coroutines.launch

@Composable
fun HomeMainHeroBannerCard(
    todayCount: Int = 0,
    todayAmountFormatted: String = "0원",
    onCardClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Dynamic badge and main copy based on today's scan count
    val (badgeText, mainHeadline) = when {
        todayCount == 0 -> "★ 기록 대기 중" to "오늘 영수증을 스캔하고\n지출을 기록해 보세요!"
        todayCount == 1 -> "★ 오늘 첫 영수증" to "오늘 첫 영수증을\n성공적으로 기록했어요!"
        else -> "★ 프로 기록러" to "오늘도 꼼꼼하게\n영수증을 챙기셨네요!"
    }

    // 1. Entrance Sequence (Bounce-in animation scale & alpha)
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

    // 2. Infinite Floating Loop animation (Vertical bobbing & soft tilt)
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
            .clickable(onClick = onCardClick)
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Column {
            // Top Badge Pill (Click to open Level Info Dialog)
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

            // 2. Main Headline Copy (Top Left)
            Text(
                text = mainHeadline,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 32.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. 3D Piggy Bank Graphic with Small Floating Soap Bubbles Above It
            val context = LocalContext.current
            val piggyResId = remember(context) {
                context.resources.getIdentifier("img_piggy_bank", "drawable", context.packageName).takeIf { it != 0 }
                    ?: context.resources.getIdentifier("ic_piggy_bank", "drawable", context.packageName).takeIf { it != 0 }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 12.dp)
            ) {
                // Small Floating Soap Bubble 1 (2nd largest bubble)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(x = (-13).dp, y = (-75).dp)
                        .size(34.dp)
                        .graphicsLayer {
                            scaleX = entranceScale.value
                            scaleY = entranceScale.value
                            alpha = entranceAlpha.value
                            translationY = offsetY.dp.toPx() * 1.25f
                            rotationZ = -rotationDegrees * 1.5f
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

                // Tiny Floating Soap Bubble 2 (Top-Right floating bubble above main piggy)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 10.dp, y = (-36).dp)
                        .size(22.dp)
                        .graphicsLayer {
                            scaleX = entranceScale.value
                            scaleY = entranceScale.value
                            alpha = entranceAlpha.value
                            translationY = offsetY.dp.toPx() * 0.85f
                            rotationZ = rotationDegrees * 2f
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

                // Main 3D Piggy Soap Bubble Container (160.dp)
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .graphicsLayer {
                            scaleX = entranceScale.value
                            scaleY = entranceScale.value
                            alpha = entranceAlpha.value
                            translationY = offsetY.dp.toPx()
                            rotationZ = rotationDegrees
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
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Soap Bubble Top-Left Specular Gloss Reflection
                    Box(
                        modifier = Modifier
                            .size(40.dp, 20.dp)
                            .align(Alignment.TopStart)
                            .padding(start = 20.dp, top = 18.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.45f))
                    )

                    if (piggyResId != null) {
                        Image(
                            painter = painterResource(id = piggyResId),
                            contentDescription = "돼지 저금통 마스코트",
                            modifier = Modifier.size(145.dp)
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

            // 4. Bottom Glassmorphic Today's Spending Pill Box
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

                    // Right Arrow Circle Button
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
