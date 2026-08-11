package com.pasic.receipt.ui.receipts.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.pasic.receipt.R
import com.pasic.receipt.data.local.entity.ReceiptEntity
import java.text.DecimalFormat

@Composable
fun ReceiptSavingTipCard(
    isVisible: Boolean,
    receipt: ReceiptEntity,
    categoryAverageAmount: Double,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(
            expandFrom = Alignment.Top,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(animationSpec = tween(400)) + scaleIn(
            initialScale = 0.85f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = 250)
        ) + fadeOut(animationSpec = tween(250)),
        modifier = modifier
    ) {
        val diff = (receipt.totalAmount - categoryAverageAmount).coerceAtLeast(0.0).toLong()
        val formattedDiff = DecimalFormat("#,###").format(diff)
        val mainCat = receipt.category.split("/").firstOrNull() ?: receipt.category

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFF8FAFC),
            border = null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 14.dp, end = 34.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tipIconRes = if (diff > 10000) R.drawable.ic_saving_tip_alt else R.drawable.ic_saving_tip

                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = tipIconRes),
                            contentDescription = "절약 팁 3D 아이콘",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "절약 팁! (평균 초과 지출)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$mainCat 평균 결제액보다 ${formattedDiff}원 높아요! 꼼꼼하게 기록해 지출을 절약해 보세요.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF334155),
                            lineHeight = 18.sp
                        )
                    }
                }

                IconButton(
                    onClick = onCloseClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = "절약 팁 닫기",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
