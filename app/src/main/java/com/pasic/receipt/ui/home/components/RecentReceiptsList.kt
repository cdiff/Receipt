package com.pasic.receipt.ui.home.components

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.*
import com.pasic.receipt.data.local.entity.ReceiptEntity
import com.pasic.receipt.ui.theme.FabNavy
import com.pasic.receipt.ui.theme.StatusBadgeBg
import com.pasic.receipt.ui.theme.StatusBadgeText
import com.pasic.receipt.ui.theme.TextMuted
import com.pasic.receipt.ui.theme.TextPrimary
import com.pasic.receipt.ui.theme.TextSecondary

@Composable
fun RecentReceiptsList(
    receipts: List<ReceiptEntity>,
    onSeeAllClick: () -> Unit = {},
    onScanClick: () -> Unit = {},
    onReceiptClick: (ReceiptEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "최근 영수증 내역",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            if (receipts.isNotEmpty()) {
                Text(
                    text = "전체보기",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.clickable(onClick = onSeeAllClick)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (receipts.isEmpty()) {
            ReceiptEmptyStateView(onScanClick = onScanClick)
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                items(receipts) { receipt ->
                    ReceiptItemCard(receipt = receipt, onClick = { onReceiptClick(receipt) })
                }
            }
        }
    }
}

/**
 * 영수증 데이터가 없을 때 표시되는 스캔 유도 Empty State 뷰.
 * 파란 원 배경 안의 카메라 아이콘이 위아래 6dp 범위로 둥둥 뜨는 애니메이션 적용.
 */
@Composable
private fun ReceiptEmptyStateView(
    onScanClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    // 위아래 6dp 부드러운 플로팅 (둥둥 뜨는) 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "floatingCamera")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 위아래 둥둥 뜨는 카메라 원형 아이콘
        Box(
            modifier = Modifier
                .graphicsLayer { translationY = offsetY }
                .size(88.dp)
                .clip(CircleShape)
                .background(Color(0xFFDCEBFE)), // 연한 소프트 파란색
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.Camera,
                contentDescription = null,
                tint = FabNavy,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. 제목
        Text(
            text = "영수증을 스캔하세요",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 3. 설명 (2줄 정렬)
        Text(
            text = "첫 영수증을 등록하고 자동 소비 분석 리포트를 확인해보세요.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 4. "영수증 촬영하기" CTA 버튼
        Button(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onScanClick()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FabNavy,
                contentColor = Color.White
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Lucide.Camera,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "영수증 촬영하기",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ReceiptItemCard(
    receipt: ReceiptEntity,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            // Status Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(StatusBadgeBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "인증완료",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatusBadgeText
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Merchant Name
            Text(
                text = receipt.merchantName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Category
            Text(
                text = receipt.category,
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Amount
            Text(
                text = "₩${String.format("%,d", receipt.totalAmount.toLong())}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Date
            Text(
                text = receipt.date,
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}
