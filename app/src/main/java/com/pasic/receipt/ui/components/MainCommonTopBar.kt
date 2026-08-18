package com.pasic.receipt.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Lucide
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.res.painterResource
import com.pasic.receipt.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.pasic.receipt.ui.theme.ModernCardBlue
import com.pasic.receipt.ui.theme.TextPrimary

/**
 * 메인 탭 화면(홈, 내보내기 등)에서 상단 로고 및 우측 상단 🔔 알림 버튼을 제어하는 재사용 TopBar 컴포넌트.
 */
@Composable
fun MainCommonTopBar(
    showLogo: Boolean = false,
    unreadCount: Int = 0,
    onNotificationClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (showLogo) Arrangement.SpaceBetween else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showLogo) {
            Image(
                painter = painterResource(id = R.drawable.ic_home_logo),
                contentDescription = "SSOC",
                modifier = Modifier.height(20.dp)
            )
        }

        Box(contentAlignment = Alignment.TopEnd) {
            IconButton(
                onClick = onNotificationClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Lucide.Bell,
                    contentDescription = "알림",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 읽지 않은 알림이 있을 경우 우측 상단에 싱그러운 초록색 점 뱃지 표시
            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .padding(top = 3.dp, end = 6.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
            }
        }
    }
}
