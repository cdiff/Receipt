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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.Image
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ScanLine
import com.pasic.receipt.data.local.entity.ReceiptEntity
import com.pasic.receipt.ui.theme.CategoryThemeRegistry
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

        if (receipts.isEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "아직 등록된 영수증이 없습니다.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(16.dp))
            ReceiptEmptyStateView(onScanClick = onScanClick)
        } else {
            Spacer(modifier = Modifier.height(16.dp))
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
 * 영수증 데이터가 없을 때 표시되는 점선 테두리 스캔 유도 Empty State 카드.
 * - 원형 배경 없는 깔끔한 ScanLine 아이콘
 * - 남색 "영수증을 스캔하세요" 타이틀
 * - "카메라로 촬영하거나 갤러리에서 선택" 가이드 문구
 */
@Composable
private fun ReceiptEmptyStateView(
    onScanClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    // 은은하고 자연스러운 미세 플로팅(±3.5dp) 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "scanLineFloating")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLineOffsetY"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .drawBehind {
                val strokeWidth = 1.5.dp.toPx()
                val cornerRadius = 20.dp.toPx()
                val pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(12f, 10f),
                    0f
                )
                drawRoundRect(
                    color = Color(0xFFCBD5E1),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(
                        size.width - strokeWidth,
                        size.height - strokeWidth
                    ),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = pathEffect
                    )
                )
            }
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onScanClick()
            }
            .padding(vertical = 36.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. #3B82F6 컬러의 미세 플로팅 ScanLine 심볼
            Icon(
                imageVector = Lucide.ScanLine,
                contentDescription = "영수증 스캔",
                tint = Color(0xFF3B82F6),
                modifier = Modifier
                    .graphicsLayer { translationY = offsetY }
                    .size(42.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 2. 남색 메인 타이틀
            Text(
                text = "영수증을 스캔하세요",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. 소프트 그레이 서브텍스트
            Text(
                text = "카메라로 촬영하거나 갤러리에서 선택",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
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
            .background(com.pasic.receipt.ui.theme.CardBackground)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            // Header Row (Circular Receipt Photo Thumbnail in place of deleted 인증완료 badge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val imageBitmap = remember(receipt.imagePath) {
                    if (receipt.imagePath.isNotBlank()) {
                        runCatching {
                            val file = java.io.File(receipt.imagePath)
                            if (file.exists()) {
                                android.graphics.BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                            } else null
                        }.getOrNull()
                    } else null
                }

                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = receipt.merchantName,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .border(0.8.dp, Color.Black.copy(alpha = 0.08f), CircleShape)
                    )
                } else {
                    val theme = CategoryThemeRegistry.getTheme(receipt.category)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .border(0.8.dp, Color(0xFFE2E8F0), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = theme.icon,
                            contentDescription = receipt.category,
                            tint = FabNavy, // 남색 통일
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
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

            // Category (Prefix with #)
            val categoryText = if (receipt.category.startsWith("#")) receipt.category else "#${receipt.category}"
            Text(
                text = categoryText,
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
