package com.pasic.receipt.ui.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasic.receipt.data.local.entity.ReceiptEntity
import com.pasic.receipt.ui.theme.TextMuted
import com.pasic.receipt.ui.theme.TextPrimary
import com.pasic.receipt.ui.theme.TextSecondary
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ChartPeriod(val label: String) {
    MONTHLY("월별"),
    WEEKLY("주별"),
    DAILY("일별")
}

data class BarData(
    val label: String,
    val amountFormatted: String? = null,
    val valueRatio: Float, // 0.0f ~ 1.0f
    val isHighlighted: Boolean = false,
    val showLabel: Boolean = true
)

@Composable
fun SpendingUsageChart(
    receipts: List<ReceiptEntity>,
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableStateOf(ChartPeriod.MONTHLY) }

    val (mainTitle, subTitle, barItems) = remember(selectedPeriod, receipts) {
        calculateChartData(selectedPeriod, receipts)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // 1. 헤더 타이틀
        Text(
            text = mainTitle,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subTitle,
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 2. Canvas 기반 바 차트 (막대 상단 숫자 & 이번 달 하이라이트)
        CanvasBarChart(
            barItems = barItems,
            selectedPeriod = selectedPeriod,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 3. 슬라이딩 세그먼트 캡슐 탭
        ChartSegmentTab(
            selectedPeriod = selectedPeriod,
            onPeriodSelected = { selectedPeriod = it }
        )
    }
}

/**
 * Canvas 기반 바 차트 컴포넌트
 */
@Composable
private fun CanvasBarChart(
    barItems: List<BarData>,
    selectedPeriod: ChartPeriod,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val isDaily = selectedPeriod == ChartPeriod.DAILY

    // key(selectedPeriod)로 탭 변경 시 Compose 슬롯 테이블을 완전히 재구성하여 index -1 크래시 100% 방지
    key(selectedPeriod) {
        var animStarted by remember { mutableStateOf(false) }

        LaunchedEffect(selectedPeriod) {
            animStarted = true
        }

        val animProgress by animateFloatAsState(
            targetValue = if (animStarted) 1f else 0f,
            animationSpec = tween(
                durationMillis = 400,
                easing = FastOutSlowInEasing
            ),
            label = "chartAnimProgress"
        )

        val highlightColor = Color(0xFF3B82F6)
        val normalBarColor = Color(0xFFCBD5E1)
        val labelTextColor = TextSecondary.toArgb()
        val highlightTextColor = highlightColor.toArgb()
        val topAmountTextColor = TextMuted.toArgb()

    val labelTextSizePx = with(density) { 11.sp.toPx() }
    val topAmountTextSizePx = with(density) { 11.sp.toPx() }

    Canvas(modifier = modifier) {
        val totalWidth = size.width
        val totalHeight = size.height

        val topTextHeight = if (!isDaily) topAmountTextSizePx + 6.dp.toPx() else 0f
        val labelAreaHeight = labelTextSizePx + 8.dp.toPx()
        val barAreaHeight = totalHeight - topTextHeight - labelAreaHeight - 4.dp.toPx()

        val barCount = barItems.size
        if (barCount == 0) return@Canvas

        val barWidthFraction = if (isDaily) 0.45f else 0.58f
        val slotWidth = totalWidth / barCount
        val barWidth = slotWidth * barWidthFraction
        val cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())

        barItems.forEachIndexed { i, item ->
            val ratio = (item.valueRatio.coerceIn(0.04f, 1.0f) * animProgress).coerceIn(0f, 1f)
            val barHeight = (barAreaHeight * ratio).coerceAtLeast(3.dp.toPx())

            val left = slotWidth * i + (slotWidth - barWidth) / 2f
            val top = topTextHeight + (barAreaHeight - barHeight)
            val barColor = if (item.isHighlighted) highlightColor else normalBarColor

            // 1. 막대 상단 금액 숫자 표시 (월별, 주별 탭일 때 "85", "78" 표기)
            if (!isDaily && item.amountFormatted != null) {
                val amountPaint = android.graphics.Paint().apply {
                    textSize = topAmountTextSizePx
                    color = if (item.isHighlighted) highlightTextColor else topAmountTextColor
                    typeface = if (item.isHighlighted)
                        android.graphics.Typeface.DEFAULT_BOLD
                    else
                        android.graphics.Typeface.DEFAULT
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawContext.canvas.nativeCanvas.drawText(
                    item.amountFormatted,
                    left + barWidth / 2f,
                    top - 6.dp.toPx(),
                    amountPaint
                )
            }

            // 2. 막대 그리기 (이번 달은 파란색, 지난달은 연회색)
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = cornerRadius
            )

            // 3. 하단 라벨 텍스트 그리기
            if (item.showLabel) {
                val labelPaint = android.graphics.Paint().apply {
                    textSize = labelTextSizePx
                    color = if (item.isHighlighted) highlightTextColor else labelTextColor
                    typeface = if (item.isHighlighted)
                        android.graphics.Typeface.DEFAULT_BOLD
                    else
                        android.graphics.Typeface.DEFAULT
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawContext.canvas.nativeCanvas.drawText(
                    item.label,
                    left + barWidth / 2f,
                    totalHeight,
                    labelPaint
                )
            }
        }
    }
}
}

/**
 * 쫀득한 iOS 스프링 바운스 슬라이딩 캡슐 탭 (월별, 주별, 일별)
 */
@Composable
private fun ChartSegmentTab(
    selectedPeriod: ChartPeriod,
    onPeriodSelected: (ChartPeriod) -> Unit
) {
    val haptics = LocalHapticFeedback.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF1F5F9))
            .padding(4.dp)
    ) {
        val totalWidth = maxWidth
        val tabCount = ChartPeriod.entries.size
        val tabWidth = totalWidth / tabCount
        val selectedIndex = selectedPeriod.ordinal

        val indicatorOffsetX by animateDpAsState(
            targetValue = tabWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "tabSlidingIndicator"
        )

        // 미끄러지는 흰색 캡슐 인디케이터
        Box(
            modifier = Modifier
                .offset(x = indicatorOffsetX)
                .width(tabWidth)
                .height(40.dp)
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(11.dp),
                    spotColor = Color(0x1A000000)
                )
                .clip(RoundedCornerShape(11.dp))
                .background(Color.White)
        )

        // 탭 텍스트 오버레이
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ChartPeriod.entries.forEach { period ->
                val isSelected = selectedPeriod == period

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (!isSelected) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onPeriodSelected(period)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period.label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) TextPrimary else TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * 영수증 데이터 기반 차트 데이터 계산
 */
private fun calculateChartData(
    period: ChartPeriod,
    receipts: List<ReceiptEntity>
): Triple<String, String, List<BarData>> {
    val today = LocalDate.now()
    val currentMonth = today.month
    val totalReceiptsSum = if (receipts.isNotEmpty()) receipts.sumOf { it.totalAmount }.toInt() else 0
    val currentPeriodSum = if (totalReceiptsSum > 0) totalReceiptsSum else 40400

    return when (period) {
        ChartPeriod.MONTHLY -> {
            val monthFormatter = DateTimeFormatter.ofPattern("M월")
            val mockMonthlyAmounts = listOf(650000, 780000, 920000, 850000, 980000, 810000, 850000)

            val bars = (5 downTo 0).mapIndexed { i, offsetMonths ->
                val targetMonthDate = today.minusMonths(offsetMonths.toLong())
                val isCurrent = (offsetMonths == 0)
                val label = if (isCurrent) "이번 달" else targetMonthDate.format(monthFormatter)
                val amount = if (isCurrent) currentPeriodSum else mockMonthlyAmounts.getOrNull(i) ?: 800000
                val manWon = amount / 10000

                BarData(
                    label = label,
                    amountFormatted = if (manWon > 0) "$manWon" else "${amount / 1000}k",
                    valueRatio = (amount.toFloat() / 1000000f).coerceIn(0.05f, 1.0f),
                    isHighlighted = isCurrent
                )
            }

            val avgMonthly = 850000
            val lastMonthSum = 810000
            val diff = lastMonthSum - currentPeriodSum
            val subTitle = if (diff >= 0) "이번 달엔 ${formatToManWon(diff)} 덜 썼어요"
            else "이번 달엔 ${formatToManWon(-diff)} 더 썼어요"

            Triple("한 달에 ${formatToManWon(avgMonthly)} 정도 써요", subTitle, bars)
        }

        ChartPeriod.WEEKLY -> {
            val dateFormatter = DateTimeFormatter.ofPattern("M.d")
            val mockWeeklyAmounts = listOf(190000, 230000, 650000, 300000, 260000, 250000, 250000)

            val bars = (5 downTo 0).mapIndexed { i, offsetWeeks ->
                val targetEndDate = today.minusWeeks(offsetWeeks.toLong())
                val isCurrent = (offsetWeeks == 0)
                val label = if (isCurrent) "이번 주" else "~${targetEndDate.format(dateFormatter)}"
                val amount = if (isCurrent) currentPeriodSum else mockWeeklyAmounts.getOrNull(i) ?: 250000
                val manWon = amount / 10000

                BarData(
                    label = label,
                    amountFormatted = if (manWon > 0) "$manWon" else "${amount / 1000}k",
                    valueRatio = (amount.toFloat() / 700000f).coerceIn(0.05f, 1.0f),
                    isHighlighted = isCurrent
                )
            }

            val avgWeekly = 220000
            val prevWeekSum = 250000
            val diff = prevWeekSum - currentPeriodSum
            val subTitle = if (diff >= 0) "이번 주엔 ${formatToManWon(diff)} 덜 썼어요"
            else "이번 주엔 ${formatToManWon(-diff)} 더 썼어요"

            Triple("일주일 ${formatToManWon(avgWeekly)} 정도 써요", subTitle, bars)
        }

        ChartPeriod.DAILY -> {
            val dayFormatter = DateTimeFormatter.ofPattern("M.d")
            val todayAmount = receipts.firstOrNull()?.totalAmount?.toInt() ?: 12400
            val mockPattern = listOf(
                18000, 45000, 12000, 95000, 22000, 15000, 32000,
                55000, 18000, 42000, 88000, 14000, 62000, 38000,
                25000, 48000, 75000, 31000, 28000, 34000, 52000,
                19000, 41000, 63000, 27000, 39000, 84000, todayAmount, 25000, 25000
            )

            // 최근 28일간의 일별 타임라인 (27 downTo 0 ➔ 28개 데이터 포인트)
            val bars = (27 downTo 0).mapIndexed { idx, offsetDays ->
                val targetDate = today.minusDays(offsetDays.toLong())
                val isToday = (offsetDays == 0)
                val isThisMonth = (targetDate.month == currentMonth)

                val label = if (isToday) "오늘" else targetDate.format(dayFormatter)
                val showLabel = isToday || offsetDays % 5 == 0
                val amount = if (isToday) todayAmount else mockPattern.getOrNull(idx) ?: 25000

                BarData(
                    label = label,
                    amountFormatted = null,
                    valueRatio = (amount.toFloat() / 100000f).coerceIn(0.04f, 1.0f),
                    isHighlighted = isThisMonth,
                    showLabel = showLabel
                )
            }

            Triple(
                "하루에 ${formatToManWon(40000)} 정도 써요",
                "오늘은 ${formatWon(todayAmount)} 썼어요",
                bars
            )
        }
    }
}

private fun formatToManWon(amount: Int): String {
    val manWon = amount / 10000
    val remainder = (amount % 10000) / 1000
    return when {
        manWon > 0 && remainder > 0 -> "${manWon}.${remainder}만원"
        manWon > 0 -> "${manWon}만원"
        else -> formatWon(amount)
    }
}

private fun formatWon(amount: Int): String {
    return NumberFormat.getNumberInstance(Locale.KOREA).format(amount) + "원"
}
