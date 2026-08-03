package com.pasic.receipt.ui.receipts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasic.receipt.data.local.entity.ReceiptEntity
import com.pasic.receipt.data.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class FilterChipType(val label: String) {
    // 카테고리 관련 필터 칩
    FOOD("식비"),
    TRANSPORT("교통비"),
    OFFICE("사무용품"),
    UNCLASSIFIED("미분류")
}

data class ReceiptListUiState(
    val searchQuery: String = "",
    val selectedYearMonth: YearMonth? = YearMonth.now(),
    val selectedDateRange: Pair<LocalDate, LocalDate>? = null, // (시작일, 종료일) 범위 선택
    val selectedFilterChips: Set<FilterChipType> = emptySet(),
    val groupedReceipts: Map<String, List<ReceiptEntity>> = emptyMap(),
    val totalCount: Int = 0,
    val totalAmountSum: Double = 0.0,
    val isLoading: Boolean = false
)

@HiltViewModel
class ReceiptListViewModel @Inject constructor(
    private val repository: ReceiptRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedFilterChips = MutableStateFlow<Set<FilterChipType>>(emptySet())
    val selectedFilterChips: StateFlow<Set<FilterChipType>> = _selectedFilterChips

    private val _selectedYearMonth = MutableStateFlow<YearMonth?>(YearMonth.now())
    val selectedYearMonth: StateFlow<YearMonth?> = _selectedYearMonth

    private val _selectedDateRange = MutableStateFlow<Pair<LocalDate, LocalDate>?>(null)
    val selectedDateRange: StateFlow<Pair<LocalDate, LocalDate>?> = _selectedDateRange

    val uiState: StateFlow<ReceiptListUiState> = combine(
        repository.getAllReceipts(),
        _searchQuery,
        _selectedFilterChips,
        _selectedYearMonth,
        _selectedDateRange
    ) { allReceipts, query, chips, yearMonth, dateRange ->
        val rawReceipts = if (allReceipts.isEmpty()) {
            getDemoReceipts()
        } else {
            allReceipts
        }

        val filtered = rawReceipts.filter { receipt ->
            // 1. 날짜 범위 필터 (dateRange 가 설정된 경우)
            val matchesRange = if (dateRange != null) {
                val receiptDate = parseReceiptDate(receipt.date)
                if (receiptDate != null) {
                    !receiptDate.isBefore(dateRange.first) && !receiptDate.isAfter(dateRange.second)
                } else true
            } else {
                // dateRange 가 없으면 월 필터
                if (yearMonth == null) true else {
                    val targetMonthStr = "${yearMonth.year}.${yearMonth.monthValue.toString().padStart(2, '0')}"
                    val targetKoreanStr = "${yearMonth.monthValue}월"
                    receipt.date.contains(targetMonthStr) || receipt.date.contains(targetKoreanStr)
                }
            }

            // 2. 검색어 필터
            val matchesQuery = query.isBlank() ||
                    receipt.merchantName.contains(query, ignoreCase = true) ||
                    receipt.category.contains(query, ignoreCase = true)

            // 3. 카테고리 필터 칩 (다중 선택 시 OR 조건 : 선택된 카테고리 중 하나라도 일치하면 표시)
            val matchesChips = if (chips.isEmpty()) true else {
                chips.any { chip ->
                    when (chip) {
                        FilterChipType.FOOD -> receipt.category == "식비"
                        FilterChipType.TRANSPORT -> receipt.category == "교통비"
                        FilterChipType.OFFICE -> receipt.category == "사무용품"
                        FilterChipType.UNCLASSIFIED -> receipt.category == "미분류"
                    }
                }
            }

            matchesRange && matchesQuery && matchesChips
        }

        // 일별 그룹핑 ("10월 24일 (토)", "9월 28일 (월)" 등)
        val grouped = filtered.groupBy { receipt ->
            extractDailyGroupHeader(receipt.date)
        }

        ReceiptListUiState(
            searchQuery = query,
            selectedYearMonth = yearMonth,
            selectedDateRange = dateRange,
            selectedFilterChips = chips,
            groupedReceipts = grouped,
            totalCount = filtered.size,
            totalAmountSum = filtered.sumOf { it.totalAmount },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReceiptListUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChipToggled(chip: FilterChipType) {
        val current = _selectedFilterChips.value
        _selectedFilterChips.value = if (current.contains(chip)) {
            current - chip
        } else {
            current + chip
        }
    }

    fun onPreviousMonthClicked() {
        val current = _selectedYearMonth.value ?: YearMonth.now()
        _selectedYearMonth.value = current.minusMonths(1)
        _selectedDateRange.value = null
    }

    fun onNextMonthClicked() {
        val current = _selectedYearMonth.value ?: YearMonth.now()
        _selectedYearMonth.value = current.plusMonths(1)
        _selectedDateRange.value = null
    }

    fun onYearMonthSelected(yearMonth: YearMonth?) {
        _selectedYearMonth.value = yearMonth
        _selectedDateRange.value = null
    }

    fun onDateRangeSelected(start: LocalDate, end: LocalDate) {
        _selectedDateRange.value = Pair(start, end)
        _selectedYearMonth.value = YearMonth.from(start)
    }

    private fun parseReceiptDate(dateStr: String): LocalDate? {
        return runCatching {
            val regex = Regex("""(\d{1,2})월\s*(\d{1,2})일""")
            val match = regex.find(dateStr)
            if (match != null) {
                val month = match.groupValues[1].toInt()
                val day = match.groupValues[2].toInt()
                LocalDate.of(2026, month, day)
            } else null
        }.getOrNull()
    }

    private fun extractDailyGroupHeader(dateStr: String): String {
        val parsed = parseReceiptDate(dateStr)
        if (parsed != null) {
            val dayOfWeekStr = when (parsed.dayOfWeek) {
                java.time.DayOfWeek.MONDAY -> "월"
                java.time.DayOfWeek.TUESDAY -> "화"
                java.time.DayOfWeek.WEDNESDAY -> "수"
                java.time.DayOfWeek.THURSDAY -> "목"
                java.time.DayOfWeek.FRIDAY -> "금"
                java.time.DayOfWeek.SATURDAY -> "토"
                java.time.DayOfWeek.SUNDAY -> "일"
            }
            return "${parsed.monthValue}월 ${parsed.dayOfMonth}일 ($dayOfWeekStr)"
        }
        return if (dateStr.contains(" · ")) dateStr.split(" · ").first() else dateStr
    }

    private fun getDemoReceipts(): List<ReceiptEntity> {
        return listOf(
            // --- 8월 (August 2026) : 오늘(8/3) 및 과거 8월 내역 --- 6개
            ReceiptEntity(id = 1, merchantName = "스타벅스 강남대로점", date = "8월 03일 · 오후 3:40", totalAmount = 13800.0, category = "식비", categoryColor = "#FEF3C7", paymentMethod = "삼성카드 (4582)"),
            ReceiptEntity(id = 2, merchantName = "CU 역삼하이츠점", date = "8월 03일 · 오전 8:30", totalAmount = 5600.0, category = "식비", categoryColor = "#FEF3C7", paymentMethod = "카카오페이"),
            ReceiptEntity(id = 3, merchantName = "카카오 T (택시)", date = "8월 02일 · 오후 10:15", totalAmount = 19400.0, category = "교통비", categoryColor = "#DBEAFE", paymentMethod = "법인카드 (8821)"),
            ReceiptEntity(id = 4, merchantName = "투썸플레이스 삼성점", date = "8월 02일 · 오후 2:15", totalAmount = 11200.0, category = "식비", categoryColor = "#FEF3C7", paymentMethod = "개인카드"),
            ReceiptEntity(id = 5, merchantName = "영풍문고 코엑스점", date = "8월 01일 · 오후 5:45", totalAmount = 27000.0, category = "사무용품", categoryColor = "#F3E8FF", paymentMethod = "국민카드 (1029)"),
            ReceiptEntity(id = 6, merchantName = "GS25 강남역점", date = "8월 01일 · 오전 9:10", totalAmount = 4800.0, category = "식비", categoryColor = "#FEF3C7", paymentMethod = "현금"),

            // --- 7월 (July 2026) --- 10개
            ReceiptEntity(id = 7, merchantName = "올리브영 강남중앙점", date = "7월 28일 · 오후 6:10", totalAmount = 32800.0, category = "미분류", categoryColor = "#F1F5F9", paymentMethod = "현대카드 (9012)"),
            ReceiptEntity(id = 8, merchantName = "아웃백 스테이크하우스", date = "7월 25일 · 오후 7:30", totalAmount = 145000.0, category = "식비", categoryColor = "#FEF3C7", paymentMethod = "신한카드 (3019)"),
            ReceiptEntity(id = 9, merchantName = "카카오 T (대리)", date = "7월 23일 · 오후 11:45", totalAmount = 38000.0, category = "교통비", categoryColor = "#DBEAFE", paymentMethod = "법인카드 (8821)"),
            ReceiptEntity(id = 10, merchantName = "명동교자 본점", date = "7월 20일 · 오후 1:15", totalAmount = 24000.0, category = "식비", categoryColor = "#FEF3C7", paymentMethod = "신한카드 (3019)"),
            ReceiptEntity(id = 11, merchantName = "알파문구 역삼점", date = "7월 18일 · 오후 4:30", totalAmount = 56000.0, category = "사무용품", categoryColor = "#F3E8FF", paymentMethod = "국민카드 (1029)"),
            ReceiptEntity(id = 12, merchantName = "GS25 역삼하이엔드점", date = "7월 15일 · 오전 9:10", totalAmount = 6500.0, category = "식비", categoryColor = "#FEF3C7", paymentMethod = "카카오페이"),
            ReceiptEntity(id = 13, merchantName = "교보문고 강남점", date = "7월 12일 · 오후 2:00", totalAmount = 45000.0, category = "사무용품", categoryColor = "#F3E8FF", paymentMethod = "국민카드 (1029)"),
            ReceiptEntity(id = 14, merchantName = "버거킹 신논현역점", date = "7월 08일 · 오후 12:40", totalAmount = 11800.0, category = "식비", categoryColor = "#FEF3C7", paymentMethod = "현금"),
            ReceiptEntity(id = 15, merchantName = "지하철 2호선 충전", date = "7월 05일 · 오전 8:20", totalAmount = 50000.0, category = "교통비", categoryColor = "#DBEAFE", paymentMethod = "법인카드 (8821)"),
            ReceiptEntity(id = 16, merchantName = "투썸플레이스 테헤란로점", date = "7월 01일 · 오전 10:15", totalAmount = 8900.0, category = "식비", categoryColor = "#FEF3C7", paymentMethod = "개인카드"),

            // --- 6월 (June 2026) --- 10개
            ReceiptEntity(id = 17, merchantName = "스타벅스 삼성역점", date = "6월 29일 · 오후 2:30", totalAmount = 13200.0, category = "식비", categoryColor = "#FEF3C7", paymentMethod = "삼성카드 (4582)"),
            ReceiptEntity(id = 18, merchantName = "쿠팡 로켓와우 결제", date = "6월 26일 · 오전 11:05", totalAmount = 89000.0, category = "사무용품", categoryColor = "#F3E8FF", paymentMethod = "법인카드 (8821)"),
            ReceiptEntity(id = 19, merchantName = "카카오 T (택시)", date = "6월 24일 · 오후 10:20", totalAmount = 22500.0, category = "교통비", categoryColor = "#DBEAFE", paymentMethod = "법인카드 (8821)"),
            ReceiptEntity(id = 20, merchantName = "성심당 대전역점", date = "6월 21일 · 오후 4:50", totalAmount = 36000.0, category = "식비", categoryColor = "#FEF3C7", paymentMethod = "개인카드"),
            ReceiptEntity(id = 21, merchantName = "다이소 강남본점", date = "6월 19일 · 오후 5:15", totalAmount = 18500.0, category = "사무용품", categoryColor = "#F3E8FF", paymentMethod = "국민카드 (1029)"),
            ReceiptEntity(id = 22, merchantName = "CU 대치스타점", date = "6월 16일 · 오전 8:05", totalAmount = 4200.0, category = "식비", categoryColor = "#FEF3C7", paymentMethod = "현금"),
            ReceiptEntity(id = 23, merchantName = "배달의민족 (족발)", date = "6월 12일 · 오후 8:40", totalAmount = 42000.0, category = "식비", categoryColor = "#FEF3C7", paymentMethod = "삼성카드 (4582)"),
            ReceiptEntity(id = 24, merchantName = "코레일 KTX 예매", date = "6월 08일 · 오후 1:15", totalAmount = 59800.0, category = "교통비", categoryColor = "#DBEAFE", paymentMethod = "법인카드 (8821)"),
            ReceiptEntity(id = 25, merchantName = "맥도날드 선릉역점", date = "6월 05일 · 오후 12:10", totalAmount = 9800.0, category = "식비", categoryColor = "#FEF3C7", paymentMethod = "카카오페이"),
            ReceiptEntity(id = 26, merchantName = "블루보틀 역삼 카페", date = "6월 02일 · 오후 3:45", totalAmount = 16000.0, category = "식비", categoryColor = "#FEF3C7", paymentMethod = "신한카드 (3019)"),

            // --- 5월 (May 2026) --- 10개
            ReceiptEntity(id = 27, merchantName = "스타벅스 강남점", date = "5월 28일 · 오후 2:15", totalAmount = 12400.0, category = "식비", categoryColor = "#FEF3C7", paymentMethod = "삼성카드 (4582)"),
            ReceiptEntity(id = 28, merchantName = "카카오 T (대리)", date = "5월 26일 · 오후 11:30", totalAmount = 45000.0, category = "교통비", categoryColor = "#DBEAFE", paymentMethod = "법인카드 (8821)"),
            ReceiptEntity(id = 29, merchantName = "알파문구 논현점", date = "5월 22일 · 오후 4:05", totalAmount = 85100.0, category = "사무용품", categoryColor = "#F3E8FF", paymentMethod = "국민카드 (1029)"),
            ReceiptEntity(id = 30, merchantName = "GS25 강남역점", date = "5월 18일 · 오전 8:40", totalAmount = 4800.0, category = "식비", categoryColor = "#FEF3C7", paymentMethod = "현금"),
            ReceiptEntity(id = 31, merchantName = "맘스터치 역삼점", date = "5월 15일 · 오후 1:05", totalAmount = 10500.0, category = "식비", categoryColor = "#FEF3C7", paymentMethod = "카카오페이"),
            ReceiptEntity(id = 32, merchantName = "올리브영 신논현점", date = "5월 12일 · 오후 7:20", totalAmount = 47900.0, category = "미분류", categoryColor = "#F1F5F9", paymentMethod = "신한카드 (3019)"),
            ReceiptEntity(id = 33, merchantName = "지하철 9호선 충전", date = "5월 09일 · 오전 8:15", totalAmount = 45000.0, category = "교통비", categoryColor = "#DBEAFE", paymentMethod = "법인카드 (8821)"),
            ReceiptEntity(id = 34, merchantName = "이디야커피 학동역점", date = "5월 06일 · 오후 3:30", totalAmount = 7600.0, category = "식비", categoryColor = "#FEF3C7", paymentMethod = "개인카드"),
            ReceiptEntity(id = 35, merchantName = "교보문고 광화문점", date = "5월 03일 · 오후 5:10", totalAmount = 38000.0, category = "사무용품", categoryColor = "#F3E8FF", paymentMethod = "국민카드 (1029)"),
            ReceiptEntity(id = 36, merchantName = "파리바게뜨 강남역점", date = "5월 01일 · 오전 9:00", totalAmount = 15200.0, category = "식비", categoryColor = "#FEF3C7", paymentMethod = "삼성카드 (4582)")
        )
    }
}
