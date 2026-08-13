package com.pasic.receipt.ui.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasic.receipt.ui.settings.components.DividerLine
import com.pasic.receipt.ui.settings.components.SettingsToggleRow
import com.pasic.receipt.ui.theme.TextPrimary

@Composable
internal fun AiCameraSettingsSection(
    autoCropEnabled: Boolean,
    bwEnhancementEnabled: Boolean,
    aiCategoryEnabled: Boolean,
    onToggleAutoCrop: (Boolean) -> Unit,
    onToggleBwEnhancement: (Boolean) -> Unit,
    onToggleAiCategory: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "AI 인식 및 카메라",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
        )
        SettingsToggleRow(
            title = "자동 모서리 인식",
            subtitle = null,
            checked = autoCropEnabled,
            onCheckedChange = onToggleAutoCrop,
            verticalPadding = 6.dp
        )
        DividerLine()
        SettingsToggleRow(
            title = "흑백 문서 향상",
            subtitle = null,
            checked = bwEnhancementEnabled,
            onCheckedChange = onToggleBwEnhancement,
            verticalPadding = 6.dp
        )
        DividerLine()
        SettingsToggleRow(
            title = "AI 스마트 카테고리 분류",
            subtitle = null,
            checked = aiCategoryEnabled,
            onCheckedChange = onToggleAiCategory,
            verticalPadding = 6.dp
        )
    }
}
