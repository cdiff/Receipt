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
import com.pasic.receipt.data.preferences.AppThemeOption
import com.pasic.receipt.ui.settings.components.DividerLine
import com.pasic.receipt.ui.settings.components.SettingsNavigationRow
import com.pasic.receipt.ui.settings.components.SettingsValueRow
import com.pasic.receipt.ui.theme.TextPrimary

@Composable
internal fun GeneralSettingsSection(
    currentTheme: AppThemeOption,
    onThemeClick: () -> Unit,
    onLicenseClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "기본 설정",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
        )
        SettingsValueRow(
            title = "테마",
            value = currentTheme.label,
            onClick = onThemeClick,
            verticalPadding = 18.dp
        )
        DividerLine()
        SettingsValueRow(
            title = "앱 버전",
            value = "v1.0.0 (최신 버전)",
            onClick = {},
            verticalPadding = 18.dp
        )
        DividerLine()
        SettingsNavigationRow(
            title = "고객 센터 및 라이선스",
            subtitle = null,
            onClick = onLicenseClick,
            verticalPadding = 18.dp
        )
    }
}
