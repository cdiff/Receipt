package com.pasic.receipt.ui.settings.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasic.receipt.ui.settings.components.DividerLine
import com.pasic.receipt.ui.settings.components.SettingsToggleRow
import com.pasic.receipt.ui.theme.TextPrimary

@Composable
internal fun NotificationSettingsSection(
    scanReminderEnabled: Boolean,
    expenseDDayEnabled: Boolean,
    backupReminderEnabled: Boolean,
    onToggleScanReminder: (Boolean) -> Unit,
    onToggleExpenseDDay: (Boolean) -> Unit,
    onToggleBackupReminder: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "알림 설정",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
        )
        SettingsToggleRow(
            title = "저녁 영수증 스캔 알림",
            subtitle = null,
            checked = scanReminderEnabled,
            onCheckedChange = onToggleScanReminder,
            verticalPadding = 6.dp
        )
        DividerLine()
        SettingsToggleRow(
            title = "월말 경비 제출 D-Day 알림",
            subtitle = null,
            checked = expenseDDayEnabled,
            onCheckedChange = onToggleExpenseDDay,
            verticalPadding = 6.dp
        )
        DividerLine()
        SettingsToggleRow(
            title = "정기 데이터 백업 알림",
            subtitle = null,
            checked = backupReminderEnabled,
            onCheckedChange = onToggleBackupReminder,
            verticalPadding = 6.dp
        )
    }
}
