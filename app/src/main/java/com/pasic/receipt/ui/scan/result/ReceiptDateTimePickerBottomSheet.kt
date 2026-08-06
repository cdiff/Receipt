package com.pasic.receipt.ui.scan.result

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDateTimePickerBottomSheet(
    onDismissRequest: () -> Unit,
    initialDateString: String,
    onDateTimeSelected: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Current Step: 1 (날짜 선택) or 2 (시간 선택)
    var currentStep by remember { mutableIntStateOf(1) }

    val today = remember { LocalDate.now() }
    val nowTime = remember { LocalTime.now() }

    // Parse initial date & time from input string (e.g., "2026-08-05 13:28:09" or "8월 5일 · 오후 1:28")
    val (parsedDate, parsedTime) = remember(initialDateString) {
        parseInitialDateTime(initialDateString)
    }

    // Clamp initial date/time to not exceed today and current time
    val initialDate = if (parsedDate.isAfter(today)) today else parsedDate
    val initialTime = if (parsedDate == today && parsedTime.isAfter(nowTime)) nowTime else parsedTime

    var selectedYear by remember { mutableIntStateOf(initialDate.year) }
    var selectedMonth by remember { mutableIntStateOf(initialDate.monthValue) }
    var selectedDay by remember { mutableIntStateOf(initialDate.dayOfMonth) }

    var isAm by remember { mutableStateOf(initialTime.hour < 12) }
    var selectedHour by remember {
        val h = initialTime.hour % 12
        mutableIntStateOf(if (h == 0) 12 else h)
    }
    var selectedMinute by remember { mutableIntStateOf((initialTime.minute / 5) * 5) }

    // Auto-clamp state if user scrolls to a future date or time
    LaunchedEffect(selectedYear, selectedMonth, selectedDay, isAm, selectedHour, selectedMinute) {
        if (selectedYear > today.year) {
            selectedYear = today.year
        }

        val maxMonth = if (selectedYear == today.year) today.monthValue else 12
        if (selectedMonth > maxMonth) {
            selectedMonth = maxMonth
        }

        val maxDayInMonth = YearMonth.of(selectedYear, selectedMonth).lengthOfMonth()
        val maxDay = if (selectedYear == today.year && selectedMonth == today.monthValue) today.dayOfMonth else maxDayInMonth
        if (selectedDay > maxDay) {
            selectedDay = maxDay
        }

        val isTodaySelected = (selectedYear == today.year && selectedMonth == today.monthValue && selectedDay == today.dayOfMonth)
        if (isTodaySelected) {
            val currentIsAm = nowTime.hour < 12
            val current12Hour = if (nowTime.hour % 12 == 0) 12 else nowTime.hour % 12
            val currentMaxMinute = (nowTime.minute / 5) * 5

            if (currentIsAm && !isAm) {
                isAm = true
            }

            if (isAm == currentIsAm) {
                if (selectedHour > current12Hour) {
                    selectedHour = current12Hour
                }
                if (selectedHour == current12Hour && selectedMinute > currentMaxMinute) {
                    selectedMinute = currentMaxMinute
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // Header: Step sub-label, Title, Top-right X button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = if (currentStep == 1) "1 / 2 단계" else "2 / 2 단계",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (currentStep == 1) "결제 날짜 선택" else "결제 시간 선택",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = "닫기",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Step Content Switcher
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "stepPickerTransition"
            ) { step ->
                if (step == 1) {
                    // Step 1: Date Wheel Picker (Year / Month / Day)
                    DateWheelPickerSection(
                        today = today,
                        selectedYear = selectedYear,
                        onYearChange = { selectedYear = it },
                        selectedMonth = selectedMonth,
                        onMonthChange = { selectedMonth = it },
                        selectedDay = selectedDay,
                        onDayChange = { selectedDay = it }
                    )
                } else {
                    // Step 2: Time Wheel Picker (AM-PM / Hour / Minute)
                    TimeWheelPickerSection(
                        today = today,
                        nowTime = nowTime,
                        selectedYear = selectedYear,
                        selectedMonth = selectedMonth,
                        selectedDay = selectedDay,
                        isAm = isAm,
                        onAmPmChange = { isAm = it },
                        selectedHour = selectedHour,
                        onHourChange = { selectedHour = it },
                        selectedMinute = selectedMinute,
                        onMinuteChange = { selectedMinute = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Bottom CTA Button
            Button(
                onClick = {
                    if (currentStep == 1) {
                        currentStep = 2
                    } else {
                        val amPmStr = if (isAm) "오전" else "오후"
                        val formattedMinute = selectedMinute.toString().padStart(2, '0')
                        val formattedResult = "${selectedMonth}월 ${selectedDay}일 · $amPmStr ${selectedHour}:$formattedMinute"
                        onDateTimeSelected(formattedResult)
                        onDismissRequest()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (currentStep == 1) "다음 (시간 선택)" else "시간 선택 완료 (저장)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DateWheelPickerSection(
    today: LocalDate,
    selectedYear: Int,
    onYearChange: (Int) -> Unit,
    selectedMonth: Int,
    onMonthChange: (Int) -> Unit,
    selectedDay: Int,
    onDayChange: (Int) -> Unit
) {
    val years = (2020..today.year).toList()
    val maxMonth = if (selectedYear == today.year) today.monthValue else 12
    val months = (1..maxMonth).toList()

    val maxDayInMonth = YearMonth.of(selectedYear, selectedMonth).lengthOfMonth()
    val maxDay = if (selectedYear == today.year && selectedMonth == today.monthValue) today.dayOfMonth else maxDayInMonth
    val days = (1..maxDay).toList()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        // Center Selected Highlight Pill Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF1F5F9))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SingleWheelColumn(
                items = years.map { "${it}년" },
                selectedIndex = years.indexOf(selectedYear).coerceAtLeast(0),
                onIndexSelected = { onYearChange(years[it]) },
                modifier = Modifier.weight(1f)
            )
            SingleWheelColumn(
                items = months.map { "${it}월" },
                selectedIndex = months.indexOf(selectedMonth).coerceAtLeast(0),
                onIndexSelected = { onMonthChange(months[it]) },
                modifier = Modifier.weight(1f)
            )
            SingleWheelColumn(
                items = days.map { "${it}일" },
                selectedIndex = days.indexOf(selectedDay).coerceAtLeast(0),
                onIndexSelected = { onDayChange(days[it]) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TimeWheelPickerSection(
    today: LocalDate,
    nowTime: LocalTime,
    selectedYear: Int,
    selectedMonth: Int,
    selectedDay: Int,
    isAm: Boolean,
    onAmPmChange: (Boolean) -> Unit,
    selectedHour: Int,
    onHourChange: (Int) -> Unit,
    selectedMinute: Int,
    onMinuteChange: (Int) -> Unit
) {
    val isTodaySelected = (selectedYear == today.year && selectedMonth == today.monthValue && selectedDay == today.dayOfMonth)
    val currentIsAm = nowTime.hour < 12
    val current12Hour = if (nowTime.hour % 12 == 0) 12 else nowTime.hour % 12
    val currentMaxMinute = (nowTime.minute / 5) * 5

    val amPmItems = if (isTodaySelected && currentIsAm) listOf("오전") else listOf("오전", "오후")

    val maxHour = if (isTodaySelected && (isAm == currentIsAm)) current12Hour else 12
    val hours = (1..maxHour).toList()

    val maxMinute = if (isTodaySelected && (isAm == currentIsAm) && selectedHour == current12Hour) currentMaxMinute else 55
    val minutes = (0..maxMinute step 5).toList()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        // Center Selected Highlight Pill Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF1F5F9))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SingleWheelColumn(
                items = amPmItems,
                selectedIndex = if (isAm) 0 else 1.coerceAtMost(amPmItems.size - 1),
                onIndexSelected = { onAmPmChange(it == 0) },
                modifier = Modifier.weight(1f)
            )
            SingleWheelColumn(
                items = hours.map { it.toString().padStart(2, '0') },
                selectedIndex = hours.indexOf(selectedHour).coerceAtLeast(0),
                onIndexSelected = { onHourChange(hours[it]) },
                modifier = Modifier.weight(1f)
            )
            SingleWheelColumn(
                items = minutes.map { it.toString().padStart(2, '0') },
                selectedIndex = minutes.indexOf(selectedMinute).coerceAtLeast(0),
                onIndexSelected = { onMinuteChange(minutes[it]) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SingleWheelColumn(
    items: List<String>,
    selectedIndex: Int,
    onIndexSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, items.size - 1))
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val coroutineScope = rememberCoroutineScope()

    val currentCenterIndex by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex
        }
    }

    LaunchedEffect(currentCenterIndex) {
        if (currentCenterIndex in items.indices) {
            onIndexSelected(currentCenterIndex)
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = snapBehavior,
        contentPadding = PaddingValues(vertical = 68.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.height(180.dp)
    ) {
        items(items.size) { index ->
            val isSelected = index == currentCenterIndex
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .fillMaxWidth()
                    .clickable {
                        coroutineScope.launch {
                            listState.animateScrollToItem(index)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = items[index],
                    fontSize = if (isSelected) 18.sp else 15.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color(0xFF0F172A) else Color(0xFF94A3B8)
                )
            }
        }
    }
}

private fun parseInitialDateTime(dateStr: String): Pair<LocalDate, LocalTime> {
    var date = LocalDate.now()
    var time = LocalTime.now()

    if (dateStr.isBlank()) return date to time

    runCatching {
        // 1. "2026-08-05 13:28:09" or "2026-08-05 13:28" ISO / Standard format
        val isoRegex = Regex("""(\d{4})[-.](\d{1,2})[-.](\d{1,2})(?:\s+(\d{1,2}):(\d{1,2}))?""")
        val isoMatch = isoRegex.find(dateStr)
        if (isoMatch != null) {
            val y = isoMatch.groupValues[1].toInt()
            val m = isoMatch.groupValues[2].toInt()
            val d = isoMatch.groupValues[3].toInt()
            date = LocalDate.of(y, m, d)

            val hStr = isoMatch.groupValues.getOrNull(4)
            val minStr = isoMatch.groupValues.getOrNull(5)
            if (!hStr.isNullOrBlank() && !minStr.isNullOrBlank()) {
                time = LocalTime.of(hStr.toInt().coerceIn(0, 23), minStr.toInt().coerceIn(0, 59))
            }
            return date to time
        }

        // 2. "8월 5일 · 오후 1:28" Korean format
        val korRegex = Regex("""(?:(\d{4})년\s*)?(\d{1,2})월\s*(\d{1,2})일(?:\s*·\s*(오전|오후)\s*(\d{1,2}):(\d{1,2}))?""")
        val korMatch = korRegex.find(dateStr)
        if (korMatch != null) {
            val y = korMatch.groupValues[1].toIntOrNull() ?: date.year
            val m = korMatch.groupValues[2].toInt()
            val d = korMatch.groupValues[3].toInt()
            date = LocalDate.of(y, m, d)

            val amPm = korMatch.groupValues.getOrNull(4)
            val hStr = korMatch.groupValues.getOrNull(5)
            val minStr = korMatch.groupValues.getOrNull(6)
            if (!amPm.isNullOrBlank() && !hStr.isNullOrBlank() && !minStr.isNullOrBlank()) {
                var h = hStr.toInt()
                if (amPm == "오후" && h < 12) h += 12
                if (amPm == "오전" && h == 12) h = 0
                time = LocalTime.of(h.coerceIn(0, 23), minStr.toInt().coerceIn(0, 59))
            }
            return date to time
        }
    }

    return date to time
}
