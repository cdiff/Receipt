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
    val availableCategories: List<String> = listOf("식비", "교통비", "사무용품", "미분류"),
    val selectedCategories: Set<String> = emptySet(),
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

    private val _selectedCategories = MutableStateFlow<Set<String>>(emptySet())
    val selectedCategories: StateFlow<Set<String>> = _selectedCategories

    private val _selectedYearMonth = MutableStateFlow<YearMonth?>(YearMonth.now())
    val selectedYearMonth: StateFlow<YearMonth?> = _selectedYearMonth

    private val _selectedDateRange = MutableStateFlow<Pair<LocalDate, LocalDate>?>(null)
    val selectedDateRange: StateFlow<Pair<LocalDate, LocalDate>?> = _selectedDateRange

    val uiState: StateFlow<ReceiptListUiState> = combine(
        repository.getAllReceipts(),
        _searchQuery,
        _selectedCategories,
        _selectedYearMonth,
        _selectedDateRange
    ) { allReceipts, query, selectedCats, yearMonth, dateRange ->
        val rawReceipts = allReceipts

        // 로컬 DB 영수증에 등록된 커스텀 카테고리까지 100% 동적 추출
        val defaultCats = listOf("식비", "교통비", "사무용품", "미분류")
        val dbCats = allReceipts.map { it.category }.filter { it.isNotBlank() }
        val availableCats = (defaultCats + dbCats).distinct()

        val filtered = rawReceipts.filter { receipt ->
            val receiptDate = extractLocalDate(receipt)
            // 1. 날짜 범위 필터 (dateRange 가 설정된 경우)
            val matchesRange = if (dateRange != null) {
                !receiptDate.isBefore(dateRange.first) && !receiptDate.isAfter(dateRange.second)
            } else {
                // dateRange 가 없으면 월 필터
                if (yearMonth == null) true else {
                    receiptDate.year == yearMonth.year && receiptDate.monthValue == yearMonth.monthValue
                }
            }

            // 2. 검색어 필터
            val matchesQuery = query.isBlank() ||
                    receipt.merchantName.contains(query, ignoreCase = true) ||
                    receipt.category.contains(query, ignoreCase = true)

            // 3. 동적 카테고리 필터 칩
            val matchesChips = if (selectedCats.isEmpty()) true else {
                selectedCats.contains(receipt.category)
            }

            matchesRange && matchesQuery && matchesChips
        }

        // 일별 그룹핑 ("8월 6일 (목)", "8월 5일 (수)" 등)
        val grouped = filtered.groupBy { receipt ->
            extractDailyGroupHeader(receipt)
        }

        ReceiptListUiState(
            searchQuery = query,
            selectedYearMonth = yearMonth,
            selectedDateRange = dateRange,
            availableCategories = availableCats,
            selectedCategories = selectedCats,
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

    fun onFilterChipToggled(category: String) {
        val current = _selectedCategories.value
        _selectedCategories.value = if (current.contains(category)) {
            current - category
        } else {
            current + category
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

    private fun extractLocalDate(receipt: ReceiptEntity): LocalDate {
        // 1. 영수증 결제 일시(또는 휠 피커 수정 일시 `receipt.date`)를 최우선 파싱
        val dateStr = receipt.date
        if (dateStr.isNotBlank()) {
            val cleaned = dateStr.split("·").firstOrNull()?.trim() ?: dateStr
            val yearMatch = Regex("""(\d{4})[.년\s]+(\d{1,2})[.월\s]+(\d{1,2})""").find(cleaned)
            if (yearMatch != null) {
                val year = yearMatch.groupValues[1].toInt()
                val month = yearMatch.groupValues[2].toInt()
                val day = yearMatch.groupValues[3].toInt()
                return LocalDate.of(year, month, day)
            }
            val monthMatch = Regex("""(\d{1,2})월\s*(\d{1,2})일""").find(cleaned)
            if (monthMatch != null) {
                val month = monthMatch.groupValues[1].toInt()
                val day = monthMatch.groupValues[2].toInt()
                val year = if (receipt.createdAt > 1000000000000L) {
                    java.time.Instant.ofEpochMilli(receipt.createdAt).atZone(java.time.ZoneId.systemDefault()).year
                } else LocalDate.now().year
                return LocalDate.of(year, month, day)
            }
        }

        // 2. 결제 일시가 비어있거나 판독 불가 시 DB 저장 시각(`createdAt`) 보완
        if (receipt.createdAt > 1000000000000L) {
            runCatching {
                return java.time.Instant.ofEpochMilli(receipt.createdAt)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
            }
        }
        return LocalDate.now()
    }

    private fun extractDailyGroupHeader(receipt: ReceiptEntity): String {
        val parsed = extractLocalDate(receipt)
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


}
