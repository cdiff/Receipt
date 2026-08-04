package com.pasic.receipt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.pasic.receipt.ui.theme.TextPrimary
import com.pasic.receipt.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * 범용 재사용 가능 캘린더 날짜 범위 선택 바텀시트 (공통 디자인 컴포넌트)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerBottomSheet(
    onDismissRequest: () -> Unit,
    onRangeSelected: (LocalDate, LocalDate) -> Unit,
    initialStartDate: LocalDate? = null,
    initialEndDate: LocalDate? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val haptics = LocalHapticFeedback.current
    var calendarMonth by remember {
        mutableStateOf(initialStartDate?.let { YearMonth.from(it) } ?: YearMonth.now())
    }

    // 범위 선택 상태 (초기에는 선택 상태 비움, 점(Dot)으로 오늘 날짜만 표기)
    var startDate by remember { mutableStateOf<LocalDate?>(initialStartDate) }
    var endDate by remember { mutableStateOf<LocalDate?>(initialEndDate) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 월 이동 헤더 (< 2026년 10월 >)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        calendarMonth = calendarMonth.minusMonths(1)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Lucide.ChevronLeft,
                        contentDescription = "Previous Month",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "${calendarMonth.year}년 ${calendarMonth.monthValue}월",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        calendarMonth = calendarMonth.plusMonths(1)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Lucide.ChevronRight,
                        contentDescription = "Next Month",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. 요일 라벨 (일 월 화 수 목 금 토) — 일요일(빨강), 토요일(파랑)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("일", "월", "화", "수", "목", "금", "토").forEachIndexed { index, dayLabel ->
                    val labelColor = when (index) {
                        0 -> Color(0xFFEF4444) // 일요일 (Red)
                        6 -> Color(0xFF2563EB) // 토요일 (Blue)
                        else -> TextSecondary
                    }
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = labelColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. 일자 그리드 (보편적인 5줄 기준 고정 높이 220.dp 설정 — 4,5,6줄 상관없이 시트 높이/CTA 위치 100% 고정)
            val firstDayOfMonth = calendarMonth.atDay(1)
            val firstDayOfWeekOffset = firstDayOfMonth.dayOfWeek.value % 7 // 일요일 시작(0~6)
            val lengthOfMonth = calendarMonth.lengthOfMonth()

            val totalGridSlots = firstDayOfWeekOffset + lengthOfMonth
            val rows = (totalGridSlots + 6) / 7

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                for (rowIndex in 0 until rows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (colIndex in 0..6) {
                            val slotIndex = rowIndex * 7 + colIndex
                            val dayNum = slotIndex - firstDayOfWeekOffset + 1

                            if (dayNum in 1..lengthOfMonth) {
                                val currentDate = calendarMonth.atDay(dayNum)

                                val isStart = startDate == currentDate
                                val isEnd = endDate == currentDate
                                val isInRange = if (startDate != null && endDate != null) {
                                    currentDate.isAfter(startDate) && currentDate.isBefore(endDate)
                                } else false

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(vertical = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val rangeSlateBg = Color(0xFFF1F5F9)
                                    val isSelectedEdge = isStart || isEnd

                                    // 줄 수(4, 5, 6줄)에 맞춰 카드 크기 및 하이라이트 배경 높이 동적 조율
                                    val cardSize = when (rows) {
                                        4 -> 38.dp
                                        6 -> 30.dp
                                        else -> 34.dp
                                    }
                                    val rangeBgHeight = when (rows) {
                                        4 -> 42.dp
                                        6 -> 34.dp
                                        else -> 38.dp
                                    }

                                    // 범위 연결 하이라이트 배경 (은은한 라이트 슬레이트 그레이 캡슐)
                                    if (isInRange) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(rangeBgHeight)
                                                .background(rangeSlateBg)
                                        )
                                    } else if (isStart && endDate != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(rangeBgHeight)
                                                .padding(start = 2.dp)
                                                .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                                                .background(rangeSlateBg)
                                        )
                                    } else if (isEnd && startDate != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(rangeBgHeight)
                                                .padding(end = 2.dp)
                                                .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                                                .background(rangeSlateBg)
                                        )
                                    } else if (isStart && endDate == null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(rangeBgHeight)
                                                .padding(horizontal = 2.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(rangeSlateBg)
                                        )
                                    }

                                    // 날짜 텍스트 카드 (동적 크기 적용 + 360도 입체 소프트 섀도우)
                                    val squircleShape = RoundedCornerShape(12.dp)
                                    val cardModifier = if (isSelectedEdge) {
                                        Modifier
                                            .size(cardSize)
                                            .shadow(
                                                elevation = 8.dp,
                                                shape = squircleShape,
                                                spotColor = Color(0x40000000),
                                                ambientColor = Color(0x20000000)
                                            )
                                            .clip(squircleShape)
                                            .background(Color.White)
                                    } else {
                                        Modifier
                                            .size(cardSize)
                                            .clip(squircleShape)
                                    }

                                    // 날짜 글자 색상 (일요일: Red, 토요일: Blue)
                                    val dateTextColor = when {
                                        isSelectedEdge -> when (colIndex) {
                                            0 -> Color(0xFFEF4444)
                                            6 -> Color(0xFF2563EB)
                                            else -> Color(0xFF0F172A)
                                        }
                                        colIndex == 0 -> Color(0xFFEF4444) // 일요일
                                        colIndex == 6 -> Color(0xFF2563EB) // 토요일
                                        isInRange -> Color(0xFF1E293B)
                                        else -> Color(0xFF475569)
                                    }

                                    val isToday = currentDate == LocalDate.now()

                                    Box(
                                        modifier = cardModifier
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = {
                                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    if (startDate == null || (startDate != null && endDate != null)) {
                                                        startDate = currentDate
                                                        endDate = null
                                                    } else if (startDate != null && endDate == null) {
                                                        if (currentDate.isBefore(startDate)) {
                                                            startDate = currentDate
                                                        } else {
                                                            endDate = currentDate
                                                        }
                                                    }
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // 날짜 숫자 — 항상 정중앙 고정
                                        Text(
                                            text = dayNum.toString(),
                                            fontSize = 15.sp,
                                            fontWeight = if (isSelectedEdge || colIndex == 0 || colIndex == 6) FontWeight.Bold else FontWeight.SemiBold,
                                            color = dateTextColor
                                        )
                                        // 오늘 날짜 인디케이터 Dot — 숫자 아래 절대 위치 오버레이
                                        if (isToday) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .padding(bottom = 3.dp)
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelectedEdge) Color(0xFF0F172A) else Color(0xFF2563EB))
                                            )
                                        }
                                    }
                                }
                            } else {
                                // 이전달 또는 다음달 날짜 표기 (연한 회색 Color(0xFFCBD5E1) + 클릭 시 해당 월로 자연스럽게 이동)
                                val (fadedDayText, isPrev) = if (dayNum < 1) {
                                    val prevMonth = calendarMonth.minusMonths(1)
                                    (prevMonth.lengthOfMonth() + dayNum).toString() to true
                                } else {
                                    (dayNum - lengthOfMonth).toString() to false
                                }

                                val cardSize = when (rows) {
                                    4 -> 38.dp
                                    6 -> 30.dp
                                    else -> 34.dp
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(vertical = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(cardSize)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = {
                                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    calendarMonth = if (isPrev) calendarMonth.minusMonths(1) else calendarMonth.plusMonths(1)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = fadedDayText,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFCBD5E1)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. 하단 브랜드 대표 색상 CTA 버튼 ("03월 09일~03월 13일 선택")
            val ctaButtonText = remember(startDate, endDate) {
                val formatter = DateTimeFormatter.ofPattern("MM월 dd일")
                when {
                    startDate != null && endDate != null -> {
                        "${startDate?.format(formatter)}~${endDate?.format(formatter)} 선택"
                    }
                    startDate != null -> {
                        "${startDate?.format(formatter)} 선택"
                    }
                    else -> "날짜를 선택하세요"
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F172A))
                    .clickable(enabled = startDate != null) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val start = startDate ?: return@clickable
                        val end = endDate ?: start
                        onRangeSelected(start, end)
                        onDismissRequest()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ctaButtonText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
