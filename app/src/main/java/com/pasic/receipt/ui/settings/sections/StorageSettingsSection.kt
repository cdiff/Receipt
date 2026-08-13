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
import com.pasic.receipt.ui.settings.StorageInfo
import com.pasic.receipt.ui.settings.components.DividerLine
import com.pasic.receipt.ui.settings.components.SettingsNavigationRow
import com.pasic.receipt.ui.settings.components.SettingsToggleRow
import com.pasic.receipt.ui.theme.TextPrimary

@Composable
internal fun StorageSettingsSection(
    storageInfo: StorageInfo,
    autoOptimizeEnabled: Boolean,
    onBackupRestoreClick: () -> Unit,
    onStorageRefresh: () -> Unit,
    onAutoOptimizeToggle: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "데이터 및 저장소 관리",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
        )
        SettingsNavigationRow(
            title = "백업 및 복원",
            subtitle = ".zip / .json으로 내보내기",
            onClick = onBackupRestoreClick
        )
        DividerLine()
        SettingsNavigationRow(
            title = "저장공간 관리",
            subtitle = "${storageInfo.sizeMbText} / ${storageInfo.totalCount}개 항목 사용 중",
            onClick = onStorageRefresh
        )
        DividerLine()
        SettingsToggleRow(
            title = "자동 최적화",
            subtitle = "6개월 이상 된 이미지 압축",
            checked = autoOptimizeEnabled,
            onCheckedChange = onAutoOptimizeToggle
        )
    }
}
