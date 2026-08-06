package com.pasic.receipt.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasic.receipt.data.local.entity.ReceiptEntity
import com.pasic.receipt.data.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

data class HomeUiState(
    val totalSpendingFormatted: String = "0원",
    val trendFormatted: String = "0원",
    val todayCount: Int = 0,
    val todayAmountFormatted: String = "0원",
    val recentReceipts: List<ReceiptEntity> = emptyList(),
    val allReceipts: List<ReceiptEntity> = emptyList(),
    val currentTab: String = "home",
    val isLoading: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ReceiptRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getAllReceipts().collectLatest { receipts ->
                val totalSum = receipts.sumOf { it.totalAmount }
                val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
                val formattedTotal = numberFormat.format(totalSum.toInt()) + "원"

                val currentMonth = LocalDate.now().monthValue
                val thisMonthSum = receipts.filter { receipt ->
                    receipt.date.contains("${currentMonth}월") || receipt.date.contains(".0${currentMonth}.")
                }.sumOf { it.totalAmount }
                val formattedThisMonth = numberFormat.format(thisMonthSum.toInt()) + "원"

                // Calculate today's receipts
                val today = LocalDate.now()
                val todayReceipts = receipts.filter { receipt ->
                    if (receipt.createdAt > 1000000000000L) {
                        val date = java.time.Instant.ofEpochMilli(receipt.createdAt)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        if (date == today) return@filter true
                    }
                    val dateStr = receipt.date
                    val cleaned = dateStr.split("·").firstOrNull()?.trim() ?: dateStr
                    val monthDayMatch = Regex("""(\d{1,2})월\s*(\d{1,2})일""").find(cleaned)
                    if (monthDayMatch != null) {
                        val m = monthDayMatch.groupValues[1].toInt()
                        val d = monthDayMatch.groupValues[2].toInt()
                        m == today.monthValue && d == today.dayOfMonth
                    } else false
                }

                val todayCount = todayReceipts.size
                val todaySum = todayReceipts.sumOf { it.totalAmount }
                val formattedTodayAmount = numberFormat.format(todaySum.toInt()) + "원"

                _uiState.update {
                    it.copy(
                        totalSpendingFormatted = formattedTotal,
                        trendFormatted = "이번 달 $formattedThisMonth 지출 중",
                        todayCount = todayCount,
                        todayAmountFormatted = formattedTodayAmount,
                        recentReceipts = receipts.take(5),
                        allReceipts = receipts
                    )
                }
            }
        }
    }

    fun selectTab(route: String) {
        _uiState.update { it.copy(currentTab = route) }
    }
}
