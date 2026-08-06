package com.pasic.receipt.ui.home.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.Ellipsis
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Receipt
import com.composables.icons.lucide.SlidersHorizontal
import com.pasic.receipt.ui.theme.TextPrimary
import com.pasic.receipt.ui.theme.TextSecondary

data class QuickActionItem(
    val title: String,
    val icon: ImageVector,
    val drawableResName: String,
    val iconColor: Color = TextPrimary,
    val onClick: () -> Unit = {}
)

@Composable
fun QuickActionGrid(
    onScanClick: () -> Unit = {},
    onNavigateToReceiptList: () -> Unit = {},
    onChartClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val items = listOf(
        QuickActionItem("영수증 스캔", Lucide.Camera, "ic_quick_scan", onClick = onScanClick),
        QuickActionItem("전체 내역", Lucide.Receipt, "ic_quick_receipt_list", onClick = onNavigateToReceiptList),
        QuickActionItem("지출 분석", Lucide.SlidersHorizontal, "ic_quick_chart", onClick = onChartClick),
        QuickActionItem("더보기", Lucide.Ellipsis, "ic_quick_more", onClick = onMoreClick)
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            QuickActionButton(
                item = item,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    item: QuickActionItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customResId = remember(context, item.drawableResName) {
        context.resources.getIdentifier(item.drawableResName, "drawable", context.packageName).takeIf { it != 0 }
    }

    Column(
        modifier = modifier
            .bounceClick(onClick = item.onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (customResId != null) {
            Image(
                painter = painterResource(id = customResId),
                contentDescription = item.title,
                modifier = Modifier.size(48.dp)
            )
        } else {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = item.iconColor,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
    }
}

@Composable
private fun Modifier.bounceClick(
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "quickButtonSpringScale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}
