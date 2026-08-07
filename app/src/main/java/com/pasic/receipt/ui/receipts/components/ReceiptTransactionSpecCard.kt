package com.pasic.receipt.ui.receipts.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Lucide
import com.pasic.receipt.data.local.entity.ReceiptEntity
import java.text.DecimalFormat

@Composable
fun ReceiptTransactionSpecCard(
    receipt: ReceiptEntity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "상세 거래 명세",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(2.dp))

            // 결제 수단
            val paymentEmoji = when {
                receipt.paymentMethod.contains("현금") -> "💵 "
                receipt.paymentMethod.contains("간편") || receipt.paymentMethod.contains("페이") -> "📱 "
                else -> "💳 "
            }
            DetailSpecRow(label = "결제 수단", value = "$paymentEmoji${receipt.paymentMethod}")

            // 증빙 유형
            DetailSpecRow(label = "증빙 유형", value = receipt.proofType)

            // 사업자등록번호 (복사 버튼 포함)
            val bizNum = receipt.businessNumber?.ifBlank { "정보 없음" } ?: "정보 없음"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "사업자등록번호",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        if (!receipt.businessNumber.isNullOrBlank()) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("사업자등록번호", receipt.businessNumber)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "사업자등록번호가 복사되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(
                        text = bizNum,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                    if (!receipt.businessNumber.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Lucide.Copy,
                            contentDescription = "복사하기",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 부가세 및 공급가액 계산
            val vat = receipt.vatAmount ?: (if (receipt.totalAmount > 0) Math.round(receipt.totalAmount / 11.0).toDouble() else 0.0)
            val netAmount = (receipt.totalAmount - vat).coerceAtLeast(0.0)

            // 공급가액
            DetailSpecRow(
                label = "공급가액",
                value = "${DecimalFormat("#,###").format(netAmount.toLong())}원"
            )

            // 부가가치세
            DetailSpecRow(
                label = "부가가치세",
                value = "${DecimalFormat("#,###").format(vat.toLong())}원"
            )

            // AI 인식 신뢰도
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI 인식 신뢰도",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFDCFCE7)
                ) {
                    Text(
                        text = "🤖 AI 인식률 ${receipt.ocrConfidence ?: 98}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF166534),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF64748B)
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F172A)
        )
    }
}
