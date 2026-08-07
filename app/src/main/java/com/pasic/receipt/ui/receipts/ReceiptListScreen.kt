package com.pasic.receipt.ui.receipts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.min
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.icons.lucide.Car
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ReceiptText
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.ShoppingBag
import com.composables.icons.lucide.SlidersHorizontal
import com.composables.icons.lucide.Utensils
import com.pasic.receipt.data.local.entity.ReceiptEntity
import com.pasic.receipt.ui.components.DateRangePickerBottomSheet
import com.pasic.receipt.ui.theme.TextMuted
import com.pasic.receipt.ui.theme.TextPrimary
import com.pasic.receipt.ui.theme.TextSecondary
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.text.NumberFormat
import java.time.YearMonth
import java.util.Locale

private const val RECEIPT_SCROLL_THRESHOLD_PX = 80f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptListScreen(
    hazeState: HazeState = remember { HazeState() },
    onScrollProgressChanged: (Float) -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToScan: () -> Unit = {},
    onNavigateToExport: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: ReceiptListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lazyListState = rememberLazyListState()
    var showDatePickerSheet by remember { mutableStateOf(false) }

    // LazyColumn의 첫 번째 아이템 오프셋으로 스크롤 진행도 계산 (0f ~ 1f)
    val rawProgress by remember {
        derivedStateOf {
            val firstItem = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()
            if (firstItem == null || lazyListState.firstVisibleItemIndex > 0) {
                1f
            } else {
                min(-firstItem.offset / RECEIPT_SCROLL_THRESHOLD_PX, 1f)
            }
        }
    }

    LaunchedEffect(rawProgress) {
        onScrollProgressChanged(rawProgress)
    }

    if (showDatePickerSheet) {
        DateRangePickerBottomSheet(
            onDismissRequest = { showDatePickerSheet = false },
            initialStartDate = uiState.selectedDateRange?.first,
            initialEndDate = uiState.selectedDateRange?.second,
            onRangeSelected = { start, end ->
                viewModel.onDateRangeSelected(start, end)
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. 최상단 인터랙티브 월 셀렉터 헤더 (< 2026년 10월 >) -> 클릭 시 달력 바텀시트 오픈
            MonthHeaderSelector(
                selectedYearMonth = uiState.selectedYearMonth,
                selectedDateRange = uiState.selectedDateRange,
                onPreviousClick = viewModel::onPreviousMonthClicked,
                onNextClick = viewModel::onNextMonthClicked,
                onHeaderTitleClick = { showDatePickerSheet = true }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 2. 영수증 검색바
            ReceiptSearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 3. 가로 스크롤 필터 칩 목록 (DB 카테고리 100% 동적 표출)
            FilterChipRow(
                categories = uiState.availableCategories,
                selectedCategories = uiState.selectedCategories,
                onChipToggle = viewModel::onFilterChipToggled
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 4. 일별 그룹핑 영수증 리스트 (hazeSource 등록으로 최상단 탭바 유리 블러 투영)
            if (uiState.groupedReceipts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "조회된 영수증이 없습니다.",
                        fontSize = 15.sp,
                        color = TextMuted
                    )
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .weight(1f)
                        .hazeSource(hazeState),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    uiState.groupedReceipts.forEach { (dateHeader, receiptsOnDate) ->
                        item(key = dateHeader) {
                            Text(
                                text = dateHeader,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        items(
                            items = receiptsOnDate,
                            key = { it.id }
                        ) { receipt ->
                            ReceiptListItemRow(
                                receipt = receipt,
                                onClick = { onNavigateToDetail(receipt.id) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 상단 인터랙티브 월 선택 헤더 (< 2026년 10월 >)
 */
@Composable
private fun MonthHeaderSelector(
    selectedYearMonth: YearMonth?,
    selectedDateRange: Pair<java.time.LocalDate, java.time.LocalDate>?,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onHeaderTitleClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 이전 달 버튼
        IconButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPreviousClick()
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

        Spacer(modifier = Modifier.width(4.dp))

        // 중앙 월 타이틀 (클릭 시 날짜 범위 선택 바텀시트 오픈)
        val monthTitleText = when {
            selectedDateRange != null -> {
                val start = selectedDateRange.first
                val end = selectedDateRange.second
                "${start.monthValue}.${start.dayOfMonth} ~ ${end.monthValue}.${end.dayOfMonth}"
            }
            selectedYearMonth == null -> "전체 영수증"
            else -> "${selectedYearMonth.year}년 ${selectedYearMonth.monthValue}월"
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onHeaderTitleClick()
                }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = monthTitleText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // 다음 달 버튼
        IconButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onNextClick()
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
}

/**
 * 돋보기 아이콘이 포함된 영수증 검색바
 */
@Composable
private fun ReceiptSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF1F5F9))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Lucide.Search,
                contentDescription = "Search Icon",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                ),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "영수증 검색 (상호명, 품목)",
                            fontSize = 15.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    innerTextField()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 카테고리 필터 칩 카루셀 행 (로컬 DB 카테고리 100% 동적 연동)
 */
@Composable
private fun FilterChipRow(
    categories: List<String>,
    selectedCategories: Set<String>,
    onChipToggle: (String) -> Unit
) {
    val haptics = LocalHapticFeedback.current

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(categories) { category ->
            val isSelected = selectedCategories.contains(category)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) Color(0xFF0F172A) else Color.White)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onChipToggle(category)
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else Color(0xFF475569)
                )
            }
        }

        // 슬라이더 필터 아이콘 버튼
        item {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.SlidersHorizontal,
                    contentDescription = "Filter Sliders",
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * 개별 영수증 목록 항목 컴포넌트
 */
@Composable
private fun ReceiptListItemRow(
    receipt: ReceiptEntity,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 좌측 원형 썸네일 (로컬 DB 영수증 이미지 우선 표시, 없을 시 카테고리별 시그니처 썸네일)
        val imageBitmap = remember(receipt.imagePath) {
            if (receipt.imagePath.isNotBlank()) {
                runCatching {
                    val file = java.io.File(receipt.imagePath)
                    if (file.exists()) {
                        android.graphics.BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                    } else null
                }.getOrNull()
            } else null
        }

        if (imageBitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = imageBitmap,
                contentDescription = receipt.merchantName,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.Black.copy(alpha = 0.08f), CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(getThumbnailBgColor(receipt)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(receipt.category),
                    contentDescription = receipt.category,
                    tint = getThumbnailIconColor(receipt.category),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // 2. 중앙 상호명 및 날짜
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = receipt.merchantName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = receipt.date,
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 3. 우측 결제 금액 및 하단 카테고리 태그 칩
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = formatAmount(receipt.totalAmount),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 카테고리 태그 칩 (은은하고 세련된 보조 태그 스타일)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(getTagBgColor(receipt.category))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "#${receipt.category}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = getTagTextColor(receipt.category)
                )
            }
        }
    }
}

private fun getCategoryIcon(category: String) = when (category) {
    "식비" -> Lucide.Utensils
    "교통비" -> Lucide.Car
    "사무용품" -> Lucide.ShoppingBag
    else -> Lucide.ReceiptText
}

private fun getThumbnailBgColor(receipt: ReceiptEntity): Color {
    return parseHexColor(receipt.categoryColor) ?: when (receipt.category) {
        "식비" -> Color(0xFFFEF3C7)
        "교통비" -> Color(0xFFDBEAFE)
        "사무용품" -> Color(0xFFF3E8FF)
        else -> Color(0xFFF1F5F9)
    }
}

private fun parseHexColor(hexString: String): Color? {
    return runCatching {
        val cleanHex = hexString.replace("#", "")
        val colorInt = cleanHex.toLong(16)
        if (cleanHex.length == 6) {
            Color(colorInt or 0xFF000000)
        } else {
            Color(colorInt)
        }
    }.getOrNull()
}

private fun getThumbnailIconColor(category: String) = when (category) {
    "식비" -> Color(0xFFD97706)
    "교통비" -> Color(0xFF2563EB)
    "사무용품" -> Color(0xFF9333EA)
    else -> Color(0xFF64748B)
}

private fun getTagBgColor(category: String) = when (category) {
    "식비" -> Color(0xFFFFF7ED)
    "교통비" -> Color(0xFFEFF6FF)
    "사무용품" -> Color(0xFFFAF5FF)
    else -> Color(0xFFF8FAFC)
}

private fun getTagTextColor(category: String) = when (category) {
    "식비" -> Color(0xFFC2410C)
    "교통비" -> Color(0xFF1D4ED8)
    "사무용품" -> Color(0xFF7E22CE)
    else -> Color(0xFF475569)
}

private fun formatAmount(amount: Double): String {
    return NumberFormat.getNumberInstance(Locale.KOREA).format(amount.toInt()) + "원"
}
