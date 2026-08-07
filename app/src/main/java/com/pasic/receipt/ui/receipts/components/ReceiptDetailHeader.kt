package com.pasic.receipt.ui.receipts.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Scan
import com.pasic.receipt.data.local.entity.ReceiptEntity
import com.pasic.receipt.ui.theme.CategoryThemeRegistry
import java.text.DecimalFormat

@Composable
fun ReceiptDetailHeader(
    receipt: ReceiptEntity,
    imageBitmap: ImageBitmap?,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 스캔 결과 화면과 동일한 크기의 이미지 프리뷰 카드 (fillMaxWidth, height 180dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF1F5F9))
                .clickable {
                    if (imageBitmap != null) {
                        onImageClick()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "영수증 원본 사진",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Lucide.Scan,
                        contentDescription = "영수증 미표시",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "영수증 원본 사진 보기",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 상호명
        Text(
            text = receipt.merchantName.ifBlank { "알 수 없는 상호" },
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 결제 일시
        Text(
            text = receipt.date,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF64748B)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 최종 결제 금액
        val formattedAmount = DecimalFormat("#,###").format(receipt.totalAmount.toLong())
        Text(
            text = "$formattedAmount 원",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 카테고리 뱃지
        val categoryDisplay = remember(receipt.category, receipt.merchantName) {
            if (receipt.category.contains("/")) {
                receipt.category
            } else {
                val sub = when {
                    receipt.merchantName.contains("카페") || receipt.merchantName.contains("스타벅스") || receipt.merchantName.contains("투썸") -> "카페"
                    receipt.merchantName.contains("택시") -> "택시"
                    receipt.merchantName.contains("지하철") -> "지하철"
                    receipt.merchantName.contains("식당") || receipt.merchantName.contains("푸드") || receipt.merchantName.contains("버거") -> "식당"
                    else -> "기타"
                }
                "${receipt.category}/$sub"
            }
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xFFF1F5F9),
            modifier = Modifier.padding(horizontal = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val categoryTheme = CategoryThemeRegistry.getTheme(receipt.category)
                Icon(
                    imageVector = categoryTheme.icon,
                    contentDescription = null,
                    tint = categoryTheme.iconColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = categoryDisplay,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )
            }
        }
    }
}


