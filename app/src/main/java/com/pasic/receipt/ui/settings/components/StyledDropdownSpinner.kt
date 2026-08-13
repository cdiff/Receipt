package com.pasic.receipt.ui.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Lucide
import com.pasic.receipt.ui.theme.TextMuted
import com.pasic.receipt.ui.theme.TextPrimary

@Composable
internal fun StyledDropdownSpinner(
    selectedValue: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var spinnerWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val currentLabel = options.find { it.first == selectedValue }?.second ?: selectedValue

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { spinnerWidthPx = it.width }
    ) {
        // 닫혀있을 때 표시되는 박스 (ChevronDown 아이콘)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Icon(
                    Lucide.ChevronDown,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 열렸을 때 펼쳐지는 커스텀 팝업 드롭다운 (스피너 가로 길이 100% 동기화 + 부드러운 슬라이드&페이드 연출)
        if (expanded && spinnerWidthPx > 0) {
            val popupWidthDp = with(density) { spinnerWidthPx.toDp() }

            Popup(
                onDismissRequest = { expanded = false },
                properties = PopupProperties(
                    focusable = true,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                )
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(150)) + expandVertically(expandFrom = Alignment.Top, animationSpec = tween(150)),
                    exit = fadeOut(animationSpec = tween(120)) + shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = tween(120))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(top = 54.dp) // 스피너(48dp) + 6dp 간격
                            .width(popupWidthDp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(14.dp),
                                spotColor = Color.Black.copy(alpha = 0.08f)
                            )
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                    ) {
                        options.forEachIndexed { index, (key, label) ->
                            val isSelected = selectedValue == key

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) Color(0xFFEFF6FF) else Color.White)
                                    .clickable {
                                        onSelect(key)
                                        expanded = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF2563EB) else TextPrimary
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Lucide.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
