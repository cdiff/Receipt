package com.pasic.receipt.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 앱 전체에서 공통으로 사용하는 모던 플로팅 캡슐 토스트 UI 컴포넌트.
 * 반투명 연회색 캡슐 디자인(#F1F5F9) + 다크 텍스트(#0F172A) + 24dp 라운드 + 그림자
 */
@Composable
fun AppFloatingToast(
    message: String?,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 96.dp
) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = fadeIn(animationSpec = tween(150)) + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut(animationSpec = tween(150)) + slideOutVertically(targetOffsetY = { it / 2 }),
        modifier = modifier.padding(bottom = bottomPadding)
    ) {
        Box(
            modifier = Modifier
                .shadow(6.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.08f))
                .background(Color(0xFFF1F5F9).copy(alpha = 0.94f), RoundedCornerShape(24.dp))
                .padding(horizontal = 20.dp, vertical = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message ?: "",
                color = Color(0xFF0F172A),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
