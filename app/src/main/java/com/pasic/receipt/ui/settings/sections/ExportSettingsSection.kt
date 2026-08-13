package com.pasic.receipt.ui.settings.sections

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.pasic.receipt.ui.settings.csv.CsvInlineOptionsContent
import com.pasic.receipt.ui.settings.csv.CsvOptionsState
import com.pasic.receipt.ui.theme.TextMuted
import com.pasic.receipt.ui.theme.TextPrimary
import com.pasic.receipt.ui.theme.TextSecondary

internal data class ExportSectionState(
    val exportFormat: String,
    val csvSelectedColumns: Set<String>,
    val csvDateFormat: String,
    val csvAmountFormat: String,
    val zipNamingRule: String,
    val defaultAuthor: String,
    val defaultDepartment: String,
    val defaultPurpose: String,
)

@Composable
internal fun ExportSettingsSection(
    state: ExportSectionState,
    onUpdateExportFormat: (String) -> Unit,
    onToggleCsvColumn: (String) -> Unit,
    onUpdateCsvDateFormat: (String) -> Unit,
    onUpdateCsvAmountFormat: (String) -> Unit,
    onUpdateZipNamingRule: (String) -> Unit,
    onUpdateAuthor: (String) -> Unit,
    onUpdateDepartment: (String) -> Unit,
    onUpdatePurpose: (String) -> Unit,
) {
    val isCsv = state.exportFormat != "PDF"

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        // 1. 기본 포맷 세그먼트 셀렉터
        Text(
            text = "기본 포맷",
            modifier = Modifier.padding(start = 2.dp),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(20.dp))

        // 1. 기본 포맷 세그먼트 셀렉터 (PDF로 내보내기 / CSV로 내보내기 - 슬라이딩 캡슐 연출)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                .padding(4.dp)
        ) {
            val itemWidth = (maxWidth - 8.dp) / 2
            val isPdf = !isCsv
            val pillOffset by androidx.compose.animation.core.animateDpAsState(
                targetValue = if (isPdf) 0.dp else itemWidth,
                label = "pillOffset"
            )

            // 하얗고 입체감 있는 슬라이딩 캡슐 배경
            Box(
                modifier = Modifier
                    .offset(x = pillOffset)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .shadow(2.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.06f))
                    .background(Color.White, RoundedCornerShape(12.dp))
            )

            // 텍스트 레이어 (좌: PDF로 내보내기, 우: CSV로 내보내기)
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                // PDF로 내보내기 버튼 (왼쪽)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onUpdateExportFormat("PDF") }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PDF로 내보내기",
                        fontSize = 14.sp,
                        fontWeight = if (isPdf) FontWeight.Bold else FontWeight.Medium,
                        color = if (isPdf) TextPrimary else Color(0xFF475569)
                    )
                }

                // CSV로 내보내기 버튼 (오른쪽)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onUpdateExportFormat("EXCEL") }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CSV로 내보내기",
                        fontSize = 14.sp,
                        fontWeight = if (!isPdf) FontWeight.Bold else FontWeight.Medium,
                        color = if (!isPdf) TextPrimary else Color(0xFF475569)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. 포맷별 상세 설정 카드 (하단 화면 요동 없는 제자리 Crossfade 디졸브 연출)
        Crossfade(
            targetState = isCsv,
            animationSpec = tween(durationMillis = 250),
            label = "ExportDetailCrossfade"
        ) { csvSelected ->
            if (csvSelected) {
                // A. CSV 내보내기 상세 설정
                CsvInlineOptionsContent(
                    state = CsvOptionsState(
                        selectedColumns = state.csvSelectedColumns,
                        dateFormat = state.csvDateFormat,
                        amountFormat = state.csvAmountFormat,
                        zipNamingRule = state.zipNamingRule,
                    ),
                    onToggleColumn = onToggleCsvColumn,
                    onUpdateDateFormat = onUpdateCsvDateFormat,
                    onUpdateAmountFormat = onUpdateCsvAmountFormat,
                    onUpdateZipNamingRule = onUpdateZipNamingRule
                )
            } else {
                // B. PDF 내보내기 상세 설정
                Column {
                    SettingsInputField(
                        label = "기본 작성자",
                        value = state.defaultAuthor,
                        onValueChange = onUpdateAuthor,
                        placeholder = "예: 홍길동"
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    SettingsInputField(
                        label = "소속 부서",
                        value = state.defaultDepartment,
                        onValueChange = onUpdateDepartment,
                        placeholder = "예: 개발팀"
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    SettingsInputField(
                        label = "기본 목적",
                        value = state.defaultPurpose,
                        onValueChange = onUpdatePurpose,
                        placeholder = "예: 업무 경비"
                    )
                }
            }
        }
    }
}

// ── ExportSettingsSection 전용 private 컴포넌트 ────────────────────────

@Composable
private fun SettingsInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.padding(start = 3.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                .border(
                    width = if (isFocused) 1.5.dp else 1.dp,
                    color = if (isFocused) Color(0xFF2563EB) else Color(0xFFE2E8F0),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, fontSize = 14.sp, color = TextMuted)
                    }
                    val customSelectionColors = remember {
                        TextSelectionColors(
                            handleColor = Color.Transparent,
                            backgroundColor = Color(0x332563EB)
                        )
                    }
                    CompositionLocalProvider(LocalTextSelectionColors provides customSelectionColors) {
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            singleLine = true,
                            cursorBrush = SolidColor(Color(0xFF2563EB)),
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isFocused = it.isFocused }
                        )
                    }
                }

                if (value.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = "Clear",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onValueChange("") }
                    )
                }
            }
        }
    }
}
