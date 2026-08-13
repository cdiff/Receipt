package com.pasic.receipt.ui.settings.csv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.X
import com.pasic.receipt.data.preferences.ALL_CSV_COLUMNS
import com.pasic.receipt.ui.settings.components.StyledDropdownSpinner
import com.pasic.receipt.ui.theme.TextPrimary
import com.pasic.receipt.ui.theme.TextSecondary

internal data class CsvOptionsState(
    val selectedColumns: Set<String>,
    val dateFormat: String,
    val amountFormat: String,
    val zipNamingRule: String,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CsvInlineOptionsContent(
    state: CsvOptionsState,
    onToggleColumn: (String) -> Unit,
    onUpdateDateFormat: (String) -> Unit,
    onUpdateAmountFormat: (String) -> Unit,
    onUpdateZipNamingRule: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val selectedCount = ALL_CSV_COLUMNS.count { state.selectedColumns.contains(it) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "포함할 항목",
                modifier = Modifier.padding(start = 3.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Text(
                text = "${selectedCount}개 선택됨",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2563EB)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        // 칩 필터 목록 (FlowRow)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ALL_CSV_COLUMNS.forEach { col ->
                val isChecked = state.selectedColumns.contains(col)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isChecked) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
                        .then(
                            if (!isChecked) Modifier.border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                            else Modifier
                        )
                        .clickable { onToggleColumn(col) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = col,
                            fontSize = 13.sp,
                            fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isChecked) Color(0xFF2563EB) else Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isChecked) Lucide.X else Lucide.Plus,
                            contentDescription = null,
                            tint = if (isChecked) Color(0xFF2563EB) else Color(0xFF64748B),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. 날짜 형식 (커스텀 스피너 드롭다운)
        Text(
            text = "날짜 형식",
            modifier = Modifier.padding(start = 3.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        StyledDropdownSpinner(
            selectedValue = state.dateFormat,
            options = listOf(
                "YYYY-MM-DD" to "YYYY-MM-DD",
                "YYYY. MM. DD" to "YYYY. MM. DD",
                "YY/MM/DD" to "YY/MM/DD",
                "YYYY년 MM월 DD일" to "YYYY년 MM월 DD일"
            ),
            onSelect = onUpdateDateFormat
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 3. 금액 표시 형식 (커스텀 스피너 드롭다운)
        Text(
            text = "금액 표시 형식",
            modifier = Modifier.padding(start = 3.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        StyledDropdownSpinner(
            selectedValue = state.amountFormat,
            options = listOf(
                "COMMA_NUMBER" to "1,234 (천단위 콤마)",
                "RAW_NUMBER" to "1234 (숫자만 - 계산용)",
                "CURRENCY_TEXT" to "1,234원 (원화 표시)"
            ),
            onSelect = onUpdateAmountFormat
        )
    }
}
