package com.pasic.receipt.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasic.receipt.data.local.entity.ReceiptEntity
import com.pasic.receipt.ui.theme.CategoryThemeRegistry
import com.pasic.receipt.ui.theme.TextMuted
import com.pasic.receipt.ui.theme.TextPrimary
import java.text.NumberFormat
import java.util.Locale

data class CategoryBreakdownData(
    val name: String,
    val totalAmount: Double,
    val count: Int,
    val percentage: Int,
    val icon: ImageVector,
    val badgeBgColor: Color,
    val iconColor: Color,
    val progressColor: Color
)

@Composable
fun CategorySpendingBreakdown(
    receipts: List<ReceiptEntity>,
    modifier: Modifier = Modifier
) {
    val breakdownList = remember(receipts) {
        if (receipts.isEmpty()) return@remember emptyList()

        val overallSum = receipts.sumOf { it.totalAmount }
        val grouped = receipts.groupBy { it.category }

        grouped.map { (catName, catReceipts) ->
            val sum = catReceipts.sumOf { it.totalAmount }
            val count = catReceipts.size
            val pct = if (overallSum > 0) ((sum / overallSum) * 100).toInt() else 0

            val theme = CategoryThemeRegistry.getTheme(catName)

            CategoryBreakdownData(
                name = catName,
                totalAmount = sum,
                count = count,
                percentage = pct,
                icon = theme.icon,
                badgeBgColor = theme.badgeBgColor,
                iconColor = theme.iconColor,
                progressColor = theme.progressColor
            )
        }
            .sortedByDescending { it.totalAmount }
            .take(4)
    }

    if (breakdownList.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "카테고리별 지출",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            breakdownList.forEach { item ->
                CategorySpendingCard(item = item)
            }
        }
    }
}

@Composable
private fun CategorySpendingCard(
    item: CategoryBreakdownData
) {
    val formattedAmount = remember(item.totalAmount) {
        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
        formatter.format(item.totalAmount.toLong()) + " 원"
    }

    val animatedProgress by animateFloatAsState(
        targetValue = (item.percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "progressAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(com.pasic.receipt.ui.theme.CardBackground)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 좌측 사각형 아이콘 뱃지
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(item.badgeBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.name,
                            tint = item.iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // 카테고리명 및 영수증 건수
                    Column {
                        Text(
                            text = item.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "총 ${item.count}건",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted
                        )
                    }
                }

                // 카테고리별 누적 지출 금액
                Text(
                    text = formattedAmount,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 게이지 진행바
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFF1F5F9))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(item.progressColor)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 우측 비율 퍼센트 라벨
            Text(
                text = "${item.percentage}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}


