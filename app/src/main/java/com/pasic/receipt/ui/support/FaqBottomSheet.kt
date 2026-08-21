package com.pasic.receipt.ui.support

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Lucide
import com.pasic.receipt.ui.theme.FabNavy
import com.pasic.receipt.ui.theme.TextPrimary
import com.pasic.receipt.ui.theme.TextSecondary

private data class FaqItem(
    val id: Int,
    val question: String,
    val answer: String
)

private val FAQ_LIST = listOf(
    FaqItem(
        id = 1,
        question = "영수증 사진과 데이터는 어디에 저장되나요?",
        answer = "영수증 쏙은 모든 영수증 사진과 데이터를 외부 서버로 전송하지 않고 오직 사용자의 기기 내부(Local Storage)에만 안전하게 암호화하여 저장합니다. 개인정보 유출 걱정 없이 안심하고 사용하실 수 있습니다."
    ),
    FaqItem(
        id = 2,
        question = "스마트폰을 바꿀 때 백업은 어떻게 하나요?",
        answer = "[설정] ➔ [데이터 및 저장소 관리] ➔ [데이터 백업 및 복원] 메뉴에서 ZIP 백업 파일을 생성하여 안전하게 보관할 수 있습니다. 새 기기에서 해당 ZIP 파일을 선택하면 모든 영수증과 사진이 완벽하게 복원됩니다."
    ),
    FaqItem(
        id = 3,
        question = "AI 영수증 인식이 잘 안 될 때는 어떻게 하나요?",
        answer = "영수증이 구겨지거나 어두운 곳에서 촬영될 경우 인식률이 낮아질 수 있습니다. 밝은 곳에서 영수증을 평평하게 펴고 화면 안내선에 맞춰 촬영해 주세요. 인식 실패 시 홈 화면 [더보기]의 [수기 입력]을 통해 직접 금액과 상호명을 등록하실 수도 있습니다."
    ),
    FaqItem(
        id = 4,
        question = "지출 보고서(CSV/PDF)는 회사 제출용으로 쓸 수 있나요?",
        answer = "네, 가능합니다! [내보내기] 또는 홈 화면 [더보기 ➔ 이번달 지출 보고서]에서 표준 회계 양식의 PDF 지출결의서 및 엑셀 연동용 CSV 파일을 다운로드하거나 이메일/메신저로 즉시 공유하실 수 있습니다."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqBottomSheet(
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit
) {
    var expandedId by remember { mutableStateOf<Int?>(1) } // 첫 번째 질문 기본 펼침
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            // 헤더 (타이틀 + 설명)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "자주 묻는 질문 FAQ",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "영수증 쏙 이용 시 가장 궁금해하시는 질문들입니다.",
                    fontSize = 12.5.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // FAQ 아코디언 리스트
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FAQ_LIST.forEach { item ->
                    val isExpanded = expandedId == item.id
                    FaqAccordionCard(
                        item = item,
                        isExpanded = isExpanded,
                        onClick = {
                            expandedId = if (isExpanded) null else item.id
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FaqAccordionCard(
    item: FaqItem,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isExpanded) Color(0xFFF8FAFC) else Color(0xFFF1F5F9))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Q.",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2563EB)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.question,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    lineHeight = 20.sp
                )
            }

            Icon(
                imageVector = Lucide.ChevronDown,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier
                    .size(20.dp)
                    .rotate(if (isExpanded) 180f else 0f)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFE2E8F0))
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = item.answer,
                    fontSize = 13.5.sp,
                    color = Color(0xFF475569),
                    lineHeight = 21.sp
                )
            }
        }
    }
}
