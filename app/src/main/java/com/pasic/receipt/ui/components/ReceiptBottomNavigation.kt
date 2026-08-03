package com.pasic.receipt.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.FolderOutput
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ReceiptText
import com.composables.icons.lucide.Settings
import com.pasic.receipt.ui.theme.TextPrimary
import com.pasic.receipt.ui.theme.TextSecondary
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object HomeTab : BottomNavItem("home", "홈", Lucide.House)
    object ReceiptsTab : BottomNavItem("receipts", "영수증", Lucide.ReceiptText)
    object ExportTab : BottomNavItem("export", "내보내기", Lucide.FolderOutput)
    object SettingsTab : BottomNavItem("settings", "설정", Lucide.Settings)
}

@Composable
fun ReceiptBottomNavigation(
    currentRoute: String = "home",
    hazeState: HazeState? = null,
    /** 0f = 완전 펼침(상단), 1f = 완전 축소(스크롤 중) */
    scrollProgress: Float = 0f,
    onTabSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    // 스크롤 진행에 따른 자연스러운 보간
    val barHeight = lerp(64.dp, 48.dp, scrollProgress)
    val barHorizontalPadding = lerp(20.dp, 48.dp, scrollProgress)
    val rowHorizontalPadding = lerp(12.dp, 6.dp, scrollProgress)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // iOS 26 Liquid Glass Floating Navigation Bar (부드러운 스프링 축소)
        FigmaGlassSurface(
            hazeState = hazeState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = barHorizontalPadding)
                .padding(bottom = 16.dp)
                .height(barHeight)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = rowHorizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val items = listOf(
                    BottomNavItem.HomeTab,
                    BottomNavItem.ReceiptsTab,
                    BottomNavItem.ExportTab,
                    BottomNavItem.SettingsTab
                )

                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    GlassNavItemCell(
                        item = item,
                        isSelected = isSelected,
                        isCompact = scrollProgress > 0.4f,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTabSelected(item.route)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * iOS 26 Liquid Glassmorphism 컨테이너 (맑은 수성 유리 필)
 */
@Composable
private fun FigmaGlassSurface(
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val glassShape = RoundedCornerShape(50)

    val glassGradientFill = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.25f),
            Color.White.copy(alpha = 0.10f)
        )
    )

    val glassBorderStroke = BorderStroke(
        width = 1.2.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.90f),
                Color.White.copy(alpha = 0.10f)
            )
        )
    )

    val hazeModifier = if (hazeState != null) {
        Modifier.hazeEffect(
            state = hazeState,
            style = HazeMaterials.ultraThin()
        )
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .shadow(
                elevation = 14.dp,
                shape = glassShape,
                spotColor = Color(0x1F000000),
                ambientColor = Color(0x14000000)
            )
            .then(hazeModifier),
        shape = glassShape,
        color = Color.Transparent,
        border = glassBorderStroke
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = glassGradientFill)
        ) {
            content()
        }
    }
}

@Composable
private fun GlassNavItemCell(
    item: BottomNavItem,
    isSelected: Boolean,
    isCompact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // iOS 프레스 애니메이션: 눌릴 때 0.90x 축소 → 선택 시 1.05x 스프링 바운스
    val scaleTarget = when {
        isPressed -> 0.90f
        isSelected -> 1.05f
        else -> 1.0f
    }

    val scale by animateFloatAsState(
        targetValue = scaleTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tabScale"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = if (isSelected) TextPrimary else TextSecondary.copy(alpha = 0.55f),
                modifier = Modifier.size(if (isCompact) 22.dp else 20.dp)
            )

            AnimatedVisibility(
                visible = !isCompact,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.title,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) TextPrimary else TextSecondary.copy(alpha = 0.55f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
