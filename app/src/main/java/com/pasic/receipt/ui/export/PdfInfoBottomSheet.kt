package com.pasic.receipt.ui.export

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasic.receipt.ui.theme.FabNavy
import com.pasic.receipt.ui.theme.TextMuted
import com.pasic.receipt.ui.theme.TextPrimary
import com.pasic.receipt.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfInfoBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (author: String, dept: String, purpose: String) -> Unit
) {
    var author by remember { mutableStateOf("") }
    var dept by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // 헤더
            Text(
                text = "지출결의서 정보 입력",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "PDF에 포함될 정보를 입력하세요",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 작성자 이름
            PdfInfoField(
                label = "작성자 이름",
                value = author,
                onValueChange = { author = it },
                placeholder = "예: 홍길동"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 결의 부서
            PdfInfoField(
                label = "결의 부서",
                value = dept,
                onValueChange = { dept = it },
                placeholder = "예: 경영지원팀"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 지출 목적
            PdfInfoField(
                label = "지출 목적",
                value = purpose,
                onValueChange = { purpose = it },
                placeholder = "예: 10월 법인카드 사용내역"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 빈칸 안내 칩
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "💡 입력 없이 빈칸으로 출력하여 수기 기재도 가능합니다.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 버튼 Row (높이 50.dp 확장)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 건너뛰기 (빈칸 PDF 생성 - 투명 배경)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onConfirm("", "", "")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "건너뛰기",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }

                // PDF 생성하기
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FabNavy)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onConfirm(author, dept, purpose)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PDF 생성하기",
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
private fun PdfInfoField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = TextPrimary
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, if (value.isNotEmpty()) FabNavy.copy(0.6f) else Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (value.isEmpty()) {
                        Text(placeholder, fontSize = 14.sp, color = TextMuted)
                    }
                    inner()
                }
            }
        )
    }
}
