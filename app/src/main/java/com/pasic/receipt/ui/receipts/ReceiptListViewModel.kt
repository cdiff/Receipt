package com.pasic.receipt.ui.receipts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasic.receipt.data.local.entity.ReceiptEntity
import com.pasic.receipt.data.local.entity.extractLocalDate
import com.pasic.receipt.data.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

enum class SortOrder(val label: String) {
    DATE_DESC("결제일 최신순"),
    DATE_ASC("결제일 과거순"),
    AMOUNT_DESC("금액 높은순"),
    AMOUNT_ASC("금액 낮은순")
}

data class ReceiptFilterOptions(
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val paymentMethod: String = "전체",
    val proofType: String = "전체",
    val hasImageOnly: Boolean = false
)

data class ReceiptListUiState(
    val searchQuery: String = "",
    val selectedYearMonth: YearMonth? = YearMonth.now(),
    val selectedDateRange: Pair<LocalDate, LocalDate>? = null, // (시작일, 종료일) 범위 선택
    val availableCategories: List<String> = listOf("식비", "교통비", "사무용품", "미분류"),
    val selectedCategories: Set<String> = emptySet(),
    val filterOptions: ReceiptFilterOptions = ReceiptFilterOptions(),
    val isAmountSorted: Boolean = false,
    val sortedFlatReceipts: List<ReceiptEntity> = emptyList(),
    val groupedReceipts: Map<String, List<ReceiptEntity>> = emptyMap(),
    val allReceiptsForFilter: List<ReceiptEntity> = emptyList(),
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

    private val _filterOptions = MutableStateFlow(ReceiptFilterOptions())
    val filterOptions: StateFlow<ReceiptFilterOptions> = _filterOptions

    init {
        viewModelScope.launch {
            repository.ensureDefaultCategories()
        }
    }

    val uiState: StateFlow<ReceiptListUiState> = combine(
        repository.getAllReceipts(),
        repository.getAllCategories(),
        _searchQuery,
        _selectedCategories,
        _selectedYearMonth,
        _selectedDateRange,
        _filterOptions
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val rawReceipts = flows[0] as List<ReceiptEntity>
        @Suppress("UNCHECKED_CAST")
        val categoryEntities = flows[1] as List<com.pasic.receipt.data.local.entity.CategoryEntity>
        val query = flows[2] as String
        @Suppress("UNCHECKED_CAST")
        val selectedCats = flows[3] as Set<String>
        val yearMonth = flows[4] as YearMonth?
        @Suppress("UNCHECKED_CAST")
        val dateRange = flows[5] as Pair<LocalDate, LocalDate>?
        val filterOptions = flows[6] as ReceiptFilterOptions

        // 100% 순수 DB categories 테이블에서 동적 추출 (미분류는 항상 맨 끝에 배치)
        val allDbCatNames = categoryEntities.map { it.name }.filter { it.isNotBlank() }
        val distinctGeneralCats = allDbCatNames.distinct().filter { it != "미분류" }
        val availableCats = distinctGeneralCats + if (allDbCatNames.contains("미분류")) listOf("미분류") else emptyList()

        // 1단계: 날짜 범위 / 월 필터 + 검색어 + 상단 카테고리 칩
        val baseFiltered = rawReceipts.filter { receipt ->
            val receiptDate = receipt.extractLocalDate()
            val matchesRange = if (dateRange != null) {
                !receiptDate.isBefore(dateRange.first) && !receiptDate.isAfter(dateRange.second)
            } else {
                if (yearMonth == null) true else {
                    receiptDate.year == yearMonth.year && receiptDate.monthValue == yearMonth.monthValue
                }
            }

            val matchesQuery = query.isBlank() ||
                    receipt.merchantName.contains(query, ignoreCase = true) ||
                    receipt.category.contains(query, ignoreCase = true)

            val matchesChips = if (selectedCats.isEmpty()) true else {
                selectedCats.contains(receipt.category)
            }

            matchesRange && matchesQuery && matchesChips
        }

        // 2단계: 바텀시트 3대 조건 필터링 (결제수단, 증빙유형, 사진유무)
        val fullyFiltered = baseFiltered.filter { receipt ->
            val matchesPayment = when (filterOptions.paymentMethod) {
                "전체" -> true
                "카드" -> receipt.paymentMethod.contains("카드", ignoreCase = true)
                "현금" -> receipt.paymentMethod.contains("현금", ignoreCase = true)
                "간편결제" -> receipt.paymentMethod.contains("간편", ignoreCase = true) || receipt.paymentMethod.contains("페이", ignoreCase = true)
                else -> receipt.paymentMethod.equals(filterOptions.paymentMethod, ignoreCase = true)
            }

            val matchesProof = if (filterOptions.proofType == "전체") true else {
                receipt.proofType.equals(filterOptions.proofType, ignoreCase = true)
            }

            val matchesImage = if (!filterOptions.hasImageOnly) true else {
                receipt.imagePath.isNotBlank()
            }

            matchesPayment && matchesProof && matchesImage
        }

        // 3단계: 정렬 처리
        val sortedReceipts = when (filterOptions.sortOrder) {
            SortOrder.DATE_DESC -> fullyFiltered.sortedWith(
                compareByDescending<ReceiptEntity> { it.extractLocalDate() }
                    .thenByDescending { it.createdAt }
            )
            SortOrder.DATE_ASC -> fullyFiltered.sortedWith(
                compareBy<ReceiptEntity> { it.extractLocalDate() }
                    .thenBy { it.createdAt }
            )
            SortOrder.AMOUNT_DESC -> fullyFiltered.sortedWith(
                compareByDescending<ReceiptEntity> { it.totalAmount }
                    .thenByDescending { it.createdAt }
            )
            SortOrder.AMOUNT_ASC -> fullyFiltered.sortedWith(
                compareBy<ReceiptEntity> { it.totalAmount }
                    .thenByDescending { it.createdAt }
            )
        }

        // 4단계: 일별 그룹핑 vs 플랫 리스트 분기 (금액순일 때는 헤더 없이 플랫하게)
        val isAmountSorted = filterOptions.sortOrder == SortOrder.AMOUNT_DESC || filterOptions.sortOrder == SortOrder.AMOUNT_ASC
        val grouped = if (!isAmountSorted) {
            sortedReceipts.groupBy { receipt ->
                extractDailyGroupHeader(receipt)
            }
        } else {
            emptyMap()
        }

        ReceiptListUiState(
            searchQuery = query,
            selectedYearMonth = yearMonth,
            selectedDateRange = dateRange,
            availableCategories = availableCats,
            selectedCategories = selectedCats,
            filterOptions = filterOptions,
            isAmountSorted = isAmountSorted,
            sortedFlatReceipts = sortedReceipts,
            groupedReceipts = grouped,
            allReceiptsForFilter = baseFiltered,
            totalCount = sortedReceipts.size,
            totalAmountSum = sortedReceipts.sumOf { it.totalAmount },
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

    fun onFilterOptionsChanged(options: ReceiptFilterOptions) {
        _filterOptions.value = options
    }


    private fun extractDailyGroupHeader(receipt: ReceiptEntity): String {
        val parsed = receipt.extractLocalDate()
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
