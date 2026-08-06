package com.pasic.receipt.ui.home.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.pasic.receipt.ui.theme.TextMuted
import com.pasic.receipt.ui.theme.TextPrimary
import com.pasic.receipt.ui.theme.TextSecondary

data class LevelInfo(
    val levelNumber: Int,
    val title: String,
    val description: String,
    val emoji: String,
    val levelBadgeBg: Color,
    val levelBadgeText: Color,
    val gradientColors: List<Color>
)

@Composable
fun UserLevelInfoDialog(
    currentCount: Int,
    onDismissRequest: () -> Unit
) {
    val levels = listOf(
        LevelInfo(
            levelNumber = 1,
            title = "기록 대기 중",
            description = "오늘의 첫 영수증을 스캔하여 지출 기록을 시작해 보세요.",
            emoji = "🐷",
            levelBadgeBg = Color(0xFFEEF2FF),
            levelBadgeText = Color(0xFF4F46E5),
            gradientColors = listOf(Color(0xFFE0E7FF), Color(0xFFC7D2FE))
        ),
        LevelInfo(
            levelNumber = 2,
            title = "오늘 첫 영수증",
            description = "첫 기록을 축하합니다! 체계적인 소비 관리를 이어가세요.",
            emoji = "🧾",
            levelBadgeBg = Color(0xFFE0F2FE),
            levelBadgeText = Color(0xFF0284C7),
            gradientColors = listOf(Color(0xFFE0F2FE), Color(0xFFBAE6FD))
        ),
        LevelInfo(
            levelNumber = 3,
            title = "프로 기록러",
            description = "꾸준한 기록 습관의 증명. 완벽한 재무 관리를 실천하고 계십니다.",
            emoji = "🏆",
            levelBadgeBg = Color(0xFFFEF3C7),
            levelBadgeText = Color(0xFFD97706),
            gradientColors = listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A))
        )
    )

    val activeLevelIndex = when {
        currentCount == 0 -> 0
        currentCount == 1 -> 1
        else -> 2
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // 헤더 타이틀 및 닫기 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "나의 기록 등급 안내",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.X,
                            contentDescription = "닫기",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 등급 안내 카드 목록
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    levels.forEachIndexed { index, level ->
                        val isActive = index == activeLevelIndex
                        LevelCard(
                            level = level,
                            isActive = isActive
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 하단 확인 버튼
                Button(
                    onClick = onDismissRequest,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "확인",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelCard(
    level: LevelInfo,
    isActive: Boolean
) {
    val borderColor = if (isActive) Color(0xFF2563EB) else Color(0xFFF1F5F9)
    val cardBg = if (isActive) Color(0xFFF8FAFC) else Color(0xFFFAFAFA)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 좌측 3D 이모지 그라데이션 박스 영역
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        brush = Brush.linearGradient(level.gradientColors)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = level.emoji,
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 우측 등급 상세 설명
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(level.levelBadgeBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Level ${level.levelNumber}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = level.levelBadgeText
                        )
                    }

                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2563EB))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "현재 등급",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = level.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = level.description,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
