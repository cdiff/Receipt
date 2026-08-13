package com.pasic.receipt.ui.settings.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.pasic.receipt.ui.theme.FabNavy
import com.pasic.receipt.ui.theme.TextMuted
import com.pasic.receipt.ui.theme.TextPrimary
import com.pasic.receipt.ui.theme.TextSecondary

@Composable
internal fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier.padding(start = 3.dp),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = TextSecondary
    )
}

@Composable
internal fun SectionDividerBand() {
    Spacer(modifier = Modifier.height(16.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(Color(0xFFF1F5F9))
    )
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
internal fun DividerLine() {
    Box(modifier = Modifier.fillMaxWidth().height(0.8.dp).background(Color(0xFFE2E8F0)))
}

@Composable
internal fun SettingsNavigationRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    verticalPadding: Dp = if (subtitle != null) 14.dp else 12.dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle, fontSize = 13.sp, color = TextSecondary)
            }
        }
        Icon(Lucide.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
internal fun SettingsValueRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    verticalPadding: Dp = 12.dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        Text(text = value, fontSize = 13.sp, color = FabNavy, fontWeight = FontWeight.Medium)
    }
}


@Composable
internal fun SettingsToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    verticalPadding: Dp = if (subtitle != null) 14.dp else 12.dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle, fontSize = 13.sp, color = TextSecondary)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.8f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2563EB),
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE2E8F0),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
