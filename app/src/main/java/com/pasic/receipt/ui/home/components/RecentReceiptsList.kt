package com.pasic.receipt.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasic.receipt.data.local.entity.ReceiptEntity
import com.pasic.receipt.ui.theme.StatusBadgeBg
import com.pasic.receipt.ui.theme.StatusBadgeText
import com.pasic.receipt.ui.theme.TextMuted
import com.pasic.receipt.ui.theme.TextPrimary
import com.pasic.receipt.ui.theme.TextSecondary

@Composable
fun RecentReceiptsList(
    receipts: List<ReceiptEntity>,
    onSeeAllClick: () -> Unit = {},
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
            Text(
                text = "전체보기",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.clickable(onClick = onSeeAllClick)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

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
