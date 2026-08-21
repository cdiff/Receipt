package com.pasic.receipt.ui.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Tag
import com.pasic.receipt.data.local.entity.ReceiptEntity
import com.pasic.receipt.data.local.entity.extractLocalDate
import com.pasic.receipt.ui.theme.CategoryThemeRegistry
import com.pasic.receipt.ui.theme.TextPrimary
import com.pasic.receipt.ui.theme.TextSecondary
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class DonutSliceData(
    val name: String,
    val totalAmount: Double,
    val percentage: Int,
    val sweepAngle: Float,
    val startAngle: Float,
    val centerAngle: Float,
    val color: Color,
    val icon: ImageVector
)

@Composable
fun CategorySpendingBreakdown(
    receipts: List<ReceiptEntity>,
    modifier: Modifier = Modifier
) {
    val currentMonth = remember { LocalDate.now().monthValue }
    var selectedMonth by remember { mutableIntStateOf(currentMonth) }
    var showMonthMenu by remember { mutableStateOf(false) }

    // 1. 선택된 월에 해당하는 영수증 필터링
    val monthlyReceipts = remember(receipts, selectedMonth) {
        receipts.filter { receipt ->
            val date = receipt.extractLocalDate()
            date.monthValue == selectedMonth
        }
    }

    val totalMonthSpending = remember(monthlyReceipts) {
        monthlyReceipts.sumOf { it.totalAmount }
    }

    // 2. 카테고리별 집계 (Top 1, 2, 3위 + 나머지 통합)
    val sliceList = remember(monthlyReceipts, totalMonthSpending) {
        if (totalMonthSpending <= 0 || monthlyReceipts.isEmpty()) {
            return@remember emptyList<DonutSliceData>()
        }

        val grouped = monthlyReceipts.groupBy { it.category }
            .map { (catName, list) ->
                val sum = list.sumOf { it.totalAmount }
                val theme = CategoryThemeRegistry.getTheme(catName)
                Triple(catName, sum, theme)
            }
            .sortedByDescending { it.second }

        val result = mutableListOf<DonutSliceData>()
        val top3 = grouped.take(3)
        val rest = grouped.drop(3)

        // 시작 각도 (-90도 = 12시 방향)
        var currentAngle = -90f

        top3.forEach { (catName, sum, theme) ->
            val pct = ((sum / totalMonthSpending) * 100).roundToInt().coerceAtLeast(1)
            val sweep = ((sum / totalMonthSpending) * 360f).toFloat()
            val center = currentAngle + sweep / 2f

            result.add(
                DonutSliceData(
                    name = catName,
                    totalAmount = sum,
                    percentage = pct,
                    sweepAngle = sweep,
                    startAngle = currentAngle,
                    centerAngle = center,
                    color = theme.progressColor,
                    icon = theme.icon
                )
            )
            currentAngle += sweep
        }

        if (rest.isNotEmpty()) {
            val restSum = rest.sumOf { it.second }
            val restPct = ((restSum / totalMonthSpending) * 100).roundToInt().coerceAtLeast(1)
            val restSweep = (360f - top3.sumOf { (it.second / totalMonthSpending) * 360.0 }).toFloat().coerceAtLeast(0f)
            val restCenter = currentAngle + restSweep / 2f

            result.add(
                DonutSliceData(
                    name = "나머지",
                    totalAmount = restSum,
                    percentage = restPct,
                    sweepAngle = restSweep,
                    startAngle = currentAngle,
                    centerAngle = restCenter,
                    color = Color(0xFFCBD5E1), // 뉴트럴 슬레이트 그레이
                    icon = Lucide.Tag
                )
            )
        }

        result
    }

    var selectedIndex by remember(selectedMonth) { mutableIntStateOf(0) }
    val selectedSlice = sliceList.getOrNull(selectedIndex) ?: sliceList.firstOrNull()

    // 3. 최다 지출 카테고리 헤드라인 문구 및 고유 컬러
    val topSlice = sliceList.firstOrNull()
    val topCategoryName = topSlice?.name ?: "지출"
    val topCategoryColor = topSlice?.color ?: Color(0xFF3B82F6)

    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.KOREA) }
    val formattedTotal = numberFormat.format(totalMonthSpending.toLong()) + "원"

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // ── 상단 1: 월 선택 드롭다운 + 연결 문구 ────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showMonthMenu = true }
                        .padding(vertical = 2.dp, horizontal = 2.dp)
                ) {
                    Text(
                        text = "${selectedMonth}월",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Lucide.ChevronDown,
                        contentDescription = "월 변경",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMonthMenu,
                    onDismissRequest = { showMonthMenu = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    (1..12).reversed().forEach { month ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${month}월",
                                    fontWeight = if (month == selectedMonth) FontWeight.Bold else FontWeight.Normal,
                                    color = if (month == selectedMonth) Color(0xFF3B82F6) else TextPrimary
                                )
                            },
                            onClick = {
                                selectedMonth = month
                                selectedIndex = 0
                                showMonthMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

            Text(
                text = "에는",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF64748B)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── 상단 2: 최다 지출 카테고리 인사이트 문구 (해당 카테고리 고유색 적용) ─────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (totalMonthSpending > 0) topCategoryName else "지출",
                fontSize = 17.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (totalMonthSpending > 0) topCategoryColor else Color(0xFF3B82F6)
            )
            Text(
                text = if (totalMonthSpending > 0) "에서 가장 많이 결제했어요!" else " 내역이 아직 없어요!",
                fontSize = 17.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── 상단 3: 총 결제 금액 표기 (볼드 28sp) ───────────────────────
        Text(
            text = formattedTotal,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF0F172A),
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── 중앙: 인터랙티브 도넛 차트 & 위치 추적 팝업 버블 ────────────
        if (sliceList.isNotEmpty()) {
            InteractiveDonutChart(
                slices = sliceList,
                selectedIndex = selectedIndex,
                onSelectSlice = { clickedIndex ->
                    selectedIndex = clickedIndex
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(215.dp)
            )
        } else {
            // 데이터 없을 때 빈 링 뷰
            EmptyDonutChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(215.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── 하단: 2열 카테고리 범례 및 금액 리스트 ───────────────────────
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            sliceList.forEachIndexed { index, slice ->
                val isSelected = index == selectedIndex
                val interactionSource = remember { MutableInteractionSource() }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {
                                selectedIndex = index
                            }
                        )
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 좌측: 네모 컬러 칩 + 카테고리명
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(slice.color)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = slice.name,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFF0F172A) else Color(0xFF334155)
                        )
                    }

                    // 우측: 금액 (원)
                    Text(
                        text = numberFormat.format(slice.totalAmount.toLong()) + "원",
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF0F172A) else Color(0xFF1E293B)
                    )
                }
            }
        }
    }
}

/**
 * 인터랙티브 도넛 차트 컴포넌트
 * - 캔버스 도넛 렌더링
 * - 선택된 슬라이스 Lift-up (5dp 팝아웃 & Z-Index 최상단)
 * - 슬라이스 선택 시에만 동그라미 팝업 버블 표출 (Spring Pop 애니메이션)
 */
@Composable
private fun InteractiveDonutChart(
    slices: List<DonutSliceData>,
    selectedIndex: Int,
    onSelectSlice: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedSlice = slices.getOrNull(selectedIndex) ?: slices.firstOrNull()

    // 슬라이스 선택 변경 시마다 제자리에서 '뿅!' 튀어나오는 스프링 스케일 애니메이션
    val popScale = remember(selectedIndex) { androidx.compose.animation.core.Animatable(0.4f) }

    androidx.compose.runtime.LaunchedEffect(selectedIndex) {
        if (selectedSlice != null) {
            popScale.snapTo(0.4f)
            popScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    val density = LocalDensity.current
    val chartSizeDp = 200.dp
    val strokeWidthDp = 38.dp
    val bubbleSizeDp = 64.dp

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // ── 1. 도넛 캔버스 ──────────────────────────────────────────────
        Canvas(
            modifier = Modifier
                .size(chartSizeDp)
                .pointerInput(slices) {
                    detectTapGestures(
                        onPress = { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            val dist = sqrt(dx * dx + dy * dy)

                            val radius = size.width / 2f
                            val strokePx = with(density) { strokeWidthDp.toPx() }
                            val innerR = radius - strokePx - 10f
                            val outerR = radius + 20f

                            // 도넛 링 범위 내 터치만 유효 판정 (손가락 닿는 즉시 0ms 반응)
                            if (dist in innerR..outerR) {
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < -90f) angle += 360f

                                slices.forEachIndexed { index, slice ->
                                    val start = slice.startAngle
                                    val end = slice.startAngle + slice.sweepAngle
                                    if (angle in start..end) {
                                        onSelectSlice(index)
                                        return@detectTapGestures
                                    }
                                }
                            }
                        }
                    )
                }
        ) {
            val strokePx = strokeWidthDp.toPx()
            val canvasRadius = (size.width - strokePx) / 2f
            val canvasCenter = Offset(size.width / 2f, size.height / 2f)

            // 1. 도넛 슬라이스 렌더링 (원래 각도대로 빈틈없이 렌더링)
            slices.forEach { slice ->
                drawArc(
                    color = slice.color,
                    startAngle = slice.startAngle,
                    sweepAngle = slice.sweepAngle,
                    useCenter = false,
                    topLeft = Offset(canvasCenter.x - canvasRadius, canvasCenter.y - canvasRadius),
                    size = Size(canvasRadius * 2, canvasRadius * 2),
                    style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                )
            }

            // 2. 슬라이스 경계마다 안쪽부터 바깥쪽까지 정확히 균일한 두께(3dp)의 평행 분할선 그리기
            if (slices.size > 1) {
                val dividerWidthPx = 3.dp.toPx()
                val innerRadius = canvasRadius - (strokePx / 2f) - 2f
                val outerRadius = canvasRadius + (strokePx / 2f) + 2f

                slices.forEach { slice ->
                    val rad = Math.toRadians(slice.startAngle.toDouble())
                    val startX = (canvasCenter.x + cos(rad) * innerRadius).toFloat()
                    val startY = (canvasCenter.y + sin(rad) * innerRadius).toFloat()
                    val endX = (canvasCenter.x + cos(rad) * outerRadius).toFloat()
                    val endY = (canvasCenter.y + sin(rad) * outerRadius).toFloat()

                    drawLine(
                        color = Color.White,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = dividerWidthPx,
                        cap = StrokeCap.Butt
                    )
                }
            }
        }

        // ── 2. 동그라미 팝업 버블 (선택된 슬라이스 위치에서 뿅! 튀어나오는 스프링 모션) ──
        if (selectedSlice != null) {
            val bubbleRadiusDp = chartSizeDp / 2f - (strokeWidthDp / 2f) + 2.dp
            val rad = Math.toRadians(selectedSlice.centerAngle.toDouble())
            val bubbleOffsetX = (cos(rad) * bubbleRadiusDp.value).dp
            val bubbleOffsetY = (sin(rad) * bubbleRadiusDp.value).dp

            Box(
                modifier = Modifier
                    .offset {
                        with(density) {
                            IntOffset(
                                bubbleOffsetX.roundToPx(),
                                bubbleOffsetY.roundToPx()
                            )
                        }
                    }
                    .scale(popScale.value)
                    .size(bubbleSizeDp)
                    .shadow(elevation = 8.dp, shape = CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(2.dp)
                ) {
                    Icon(
                        imageVector = selectedSlice.icon,
                        contentDescription = selectedSlice.name,
                        tint = selectedSlice.color,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "${selectedSlice.percentage}%",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = selectedSlice.name,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

/**
 * 데이터가 없을 때 표시되는 빈 회색 도넛 링
 */
@Composable
private fun EmptyDonutChart(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val strokePx = 38.dp.toPx()
            val canvasRadius = (size.width - strokePx) / 2f
            val canvasCenter = Offset(size.width / 2f, size.height / 2f)

            drawArc(
                color = Color(0xFFF1F5F9),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(canvasCenter.x - canvasRadius, canvasCenter.y - canvasRadius),
                size = Size(canvasRadius * 2, canvasRadius * 2),
                style = Stroke(width = strokePx)
            )
        }
        Text(
            text = "내역 없음",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF94A3B8)
        )
    }
}
