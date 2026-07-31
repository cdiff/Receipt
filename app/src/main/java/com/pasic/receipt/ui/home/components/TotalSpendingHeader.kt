package com.pasic.receipt.ui.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasic.receipt.ui.theme.TextPrimary
import com.pasic.receipt.ui.theme.TextSecondary
import com.pasic.receipt.ui.theme.TrendRed

@Composable
fun TotalSpendingHeader(
    totalSpendingFormatted: String = "26,809,600원",
    trendFormatted: String = "3,220,500원 (28.64%)",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Text(
            text = "총 지출 현황(전체 내역 합산)",
            fontSize = 14.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = totalSpendingFormatted,
            fontSize = 28.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "↗",
                fontSize = 14.sp,
                color = TrendRed,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = trendFormatted,
                fontSize = 14.sp,
                color = TrendRed,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
