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

private fun ReceiptEntity.extractLocalDate(): LocalDate {
    if (createdAt > 1000000000000L) {
        try {
            return java.time.Instant.ofEpochMilli(createdAt)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        } catch (e: Exception) { }
    }
    return try {
        val today = LocalDate.now()
        val cleaned = date.split("·").firstOrNull()?.trim() ?: date
        val monthMatch = Regex("(\\d{1,2})월\\s*(\\d{1,2})일").find(cleaned)
        if (monthMatch != null) {
            val month = monthMatch.groupValues[1].toInt()
            val day = monthMatch.groupValues[2].toInt()
            LocalDate.of(today.year, month, day)
        } else {
            today
        }
    } catch (e: Exception) {
        LocalDate.now()
    }
}

/**
 * 영수증 데이터 기반 차트 데이터 동적 집계 계산
 */
private fun calculateChartData(
    period: ChartPeriod,
    receipts: List<ReceiptEntity>
): Triple<String, String, List<BarData>> {
    val today = LocalDate.now()
    val currentMonth = today.month
    val receiptsByDate = receipts.groupBy { it.extractLocalDate() }

    return when (period) {
        ChartPeriod.MONTHLY -> {
            val monthFormatter = DateTimeFormatter.ofPattern("M월")

            // 과거 6개월 동적 집계 (5 downTo 0)
            val monthlyData = (5 downTo 0).map { offsetMonths ->
                val targetMonthDate = today.minusMonths(offsetMonths.toLong())
                val targetYear = targetMonthDate.year
                val targetMonth = targetMonthDate.month

                val monthSum = receipts.filter { r ->
                    val rDate = r.extractLocalDate()
                    rDate.year == targetYear && rDate.month == targetMonth
                }.sumOf { it.totalAmount }.toInt()

                val isCurrent = (offsetMonths == 0)
                val label = if (isCurrent) "이번 달" else targetMonthDate.format(monthFormatter)
                Triple(label, monthSum, isCurrent)
            }

            val maxMonthSum = maxOf(monthlyData.maxOfOrNull { it.second } ?: 0, 10000)
            val currentMonthSum = monthlyData.last().second
            val lastMonthSum = monthlyData.getOrNull(monthlyData.size - 2)?.second ?: 0

            val bars = monthlyData.map { (label, amount, isCurrent) ->
                val manWon = amount / 10000
                BarData(
                    label = label,
                    amountFormatted = if (manWon > 0) "$manWon" else if (amount > 0) "${amount / 1000}k" else "0",
                    valueRatio = (amount.toFloat() / maxMonthSum.toFloat()).coerceIn(0.05f, 1.0f),
                    isHighlighted = isCurrent
                )
            }

            val validSums = monthlyData.map { it.second }.filter { it > 0 }
            val avgMonthly = if (validSums.isNotEmpty()) validSums.average().toInt() else currentMonthSum
            val diff = lastMonthSum - currentMonthSum
            val subTitle = when {
                lastMonthSum == 0 && currentMonthSum == 0 -> "이번 달 지출 기록이 없어요"
                lastMonthSum == 0 -> "지난달 기록 대비 이번 달 ${formatToManWon(currentMonthSum)} 사용"
                diff >= 0 -> "지난달보다 ${formatToManWon(diff)} 덜 썼어요"
                else -> "지난달보다 ${formatToManWon(-diff)} 더 썼어요"
            }

            Triple("한 달에 ${formatToManWon(avgMonthly)} 정도 써요", subTitle, bars)
        }

        ChartPeriod.WEEKLY -> {
            val dateFormatter = DateTimeFormatter.ofPattern("M.d")

            // 과거 6주 동적 집계 (5 downTo 0)
            val weeklyData = (5 downTo 0).map { offsetWeeks ->
                val targetEndDate = today.minusWeeks(offsetWeeks.toLong())
                val weekStart = targetEndDate.minusDays(6)
                val weekSum = receipts.filter { r ->
                    val rDate = r.extractLocalDate()
                    !rDate.isBefore(weekStart) && !rDate.isAfter(targetEndDate)
                }.sumOf { it.totalAmount }.toInt()

                val isCurrent = (offsetWeeks == 0)
                val label = if (isCurrent) "이번 주" else "~${targetEndDate.format(dateFormatter)}"
                Triple(label, weekSum, isCurrent)
            }

            val maxWeekSum = maxOf(weeklyData.maxOfOrNull { it.second } ?: 0, 5000)
            val currentWeekSum = weeklyData.last().second
            val lastWeekSum = weeklyData.getOrNull(weeklyData.size - 2)?.second ?: 0

            val bars = weeklyData.map { (label, amount, isCurrent) ->
                val manWon = amount / 10000
                BarData(
                    label = label,
                    amountFormatted = if (manWon > 0) "$manWon" else if (amount > 0) "${amount / 1000}k" else "0",
                    valueRatio = (amount.toFloat() / maxWeekSum.toFloat()).coerceIn(0.05f, 1.0f),
                    isHighlighted = isCurrent
                )
            }

            val validSums = weeklyData.map { it.second }.filter { it > 0 }
            val avgWeekly = if (validSums.isNotEmpty()) validSums.average().toInt() else currentWeekSum
            val diff = lastWeekSum - currentWeekSum
            val subTitle = when {
                lastWeekSum == 0 && currentWeekSum == 0 -> "이번 주 지출 기록이 없어요"
                lastWeekSum == 0 -> "지난주 기록 대비 이번 주 ${formatToManWon(currentWeekSum)} 사용"
                diff >= 0 -> "지난주보다 ${formatToManWon(diff)} 덜 썼어요"
                else -> "지난주보다 ${formatToManWon(-diff)} 더 썼어요"
            }

            Triple("일주일 ${formatToManWon(avgWeekly)} 정도 써요", subTitle, bars)
        }

        ChartPeriod.DAILY -> {
            val dayFormatter = DateTimeFormatter.ofPattern("M.d")

            // 과거 28일간의 일별 동적 타임라인 (27 downTo 0)
            val dailyData = (27 downTo 0).map { offsetDays ->
                val targetDate = today.minusDays(offsetDays.toLong())
                val daySum = receiptsByDate[targetDate]?.sumOf { it.totalAmount }?.toInt() ?: 0
                val isToday = (offsetDays == 0)
                val isThisMonth = (targetDate.month == currentMonth)
                val label = if (isToday) "오늘" else targetDate.format(dayFormatter)
                val showLabel = isToday || offsetDays % 5 == 0

                val manWon = daySum / 10000
                val amountFormatted = if (manWon > 0) "$manWon" else if (daySum > 0) "${daySum / 1000}k" else null

                Tuple5(label, daySum, isToday, isThisMonth, showLabel, amountFormatted)
            }

            val maxDaySum = maxOf(dailyData.maxOfOrNull { it.second } ?: 0, 2000)
            val todayAmount = dailyData.last().second

            val bars = dailyData.map { (label, amount, isToday, isThisMonth, showLabel, amountFormatted) ->
                BarData(
                    label = label,
                    amountFormatted = amountFormatted,
                    valueRatio = (amount.toFloat() / maxDaySum.toFloat()).coerceIn(0.04f, 1.0f),
                    isHighlighted = isThisMonth,
                    showLabel = showLabel
                )
            }

            val nonZeroDays = dailyData.map { it.second }.filter { it > 0 }
            val avgDaily = if (nonZeroDays.isNotEmpty()) nonZeroDays.average().toInt() else todayAmount

            Triple(
                "하루에 ${formatToManWon(avgDaily)} 정도 써요",
                "오늘은 ${formatWon(todayAmount)} 썼어요",
                bars
            )
        }
    }
}

private data class Tuple5<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F
)

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
