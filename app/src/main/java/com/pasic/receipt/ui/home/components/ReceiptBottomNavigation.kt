package com.pasic.receipt.ui.home.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasic.receipt.ui.theme.FabNavy
import com.pasic.receipt.ui.theme.TextPrimary
import com.pasic.receipt.ui.theme.TextSecondary

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem("home", "홈", Icons.Rounded.Home)
    object Receipts : BottomNavItem("receipts", "영수증", Icons.Rounded.ReceiptLong)
    object Export : BottomNavItem("export", "내보내기", Icons.Rounded.IosShare)
    object Settings : BottomNavItem("settings", "설정", Icons.Rounded.Settings)
}

@Composable
fun ReceiptBottomNavigation(
    currentRoute: String = "home",
    onTabSelected: (String) -> Unit = {},
    onCameraClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(horizontalAlignment = Alignment.End) {
            // Camera FAB floating on bottom right with rounded camera icon
            FloatingActionButton(
                onClick = onCameraClick,
                shape = CircleShape,
                containerColor = FabNavy,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                modifier = Modifier
                    .padding(end = 20.dp, bottom = 10.dp)
                    .size(54.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CameraAlt,
                    contentDescription = "Camera Scan",
                    modifier = Modifier.size(26.dp)
                )
            }

            // Authentic Figma Glassmorphism Floating Navigation Bar (64.dp)
            FigmaGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp)
                    .height(64.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val items = listOf(
                        BottomNavItem.Home,
                        BottomNavItem.Receipts,
                        BottomNavItem.Export,
                        BottomNavItem.Settings
                    )

                    items.forEach { item ->
                        val isSelected = currentRoute == item.route
                        GlassNavItemCell(
                            item = item,
                            isSelected = isSelected,
                            onClick = { onTabSelected(item.route) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Recreates crisp & authentic Figma Glassmorphism container:
 * 1. High contrast White-translucency gradient fill (88% -> 70%)
 * 2. Top-to-Bottom Glass Reflection Gradient Border
 * 3. Soft ambient drop shadow
 */
@Composable
private fun FigmaGlassSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val glassShape = RoundedCornerShape(50)

    // Glass Gradient Background Fill (Top 88% -> Bottom 70% White Translucency)
    val glassGradientFill = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.90f),
            Color.White.copy(alpha = 0.75f)
        )
    )

    // Glass Highlight Border Stroke (Top 95% White Reflection -> Bottom 30% Fade)
    val glassBorderStroke = BorderStroke(
        width = 1.2.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f),
                Color.White.copy(alpha = 0.30f)
            )
        )
    )

    Surface(
        modifier = modifier
            .shadow(
                elevation = 14.dp,
                shape = glassShape,
                spotColor = Color(0x1F000000),
                ambientColor = Color(0x14000000)
            ),
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.06f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tabScale"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
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
                tint = if (isSelected) TextPrimary else TextSecondary.copy(alpha = 0.70f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) TextPrimary else TextSecondary.copy(alpha = 0.70f)
            )
        }
    }
}
