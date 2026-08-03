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
                val targetList = if (receipts.isEmpty()) getDemoReceipts() else receipts
                val totalSum = targetList.sumOf { it.totalAmount }
                val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
                val formattedTotal = numberFormat.format(totalSum.toInt()) + "원"

                val currentMonth = LocalDate.now().monthValue
                val thisMonthSum = targetList.filter { receipt ->
                    receipt.date.contains("${currentMonth}월") || receipt.date.contains(".0${currentMonth}.")
                }.sumOf { it.totalAmount }
                val formattedThisMonth = numberFormat.format(thisMonthSum.toInt()) + "원"

                _uiState.update {
                    it.copy(
                        totalSpendingFormatted = formattedTotal,
                        trendFormatted = "이번 달 $formattedThisMonth 지출 중",
                        recentReceipts = targetList.take(5),
                        allReceipts = targetList
                    )
                }
            }
        }
    }

    private fun getDemoReceipts(): List<ReceiptEntity> {
        return listOf(
            ReceiptEntity(id = 1, merchantName = "스타벅스 강남대로점", date = "8월 03일 · 오후 3:40", totalAmount = 13800.0, category = "식비", categoryColor = "#FEF3C7", paymentMethod = "삼성카드 (4582)"),
            ReceiptEntity(id = 2, merchantName = "CU 역삼하이츠점", date = "8월 03일 · 오전 8:30", totalAmount = 5600.0, category = "식비", categoryColor = "#FEF3C7", paymentMethod = "카카오페이"),
            ReceiptEntity(id = 3, merchantName = "카카오 T (택시)", date = "8월 02일 · 오후 10:15", totalAmount = 19400.0, category = "교통비", categoryColor = "#DBEAFE", paymentMethod = "법인카드 (8821)"),
            ReceiptEntity(id = 4, merchantName = "투썸플레이스 삼성점", date = "8월 02일 · 오후 2:15", totalAmount = 11200.0, category = "식비", categoryColor = "#FEF3C7", paymentMethod = "개인카드"),
            ReceiptEntity(id = 5, merchantName = "영풍문고 코엑스점", date = "8월 01일 · 오후 5:45", totalAmount = 27000.0, category = "사무용품", categoryColor = "#F3E8FF", paymentMethod = "국민카드 (1029)"),
            ReceiptEntity(id = 6, merchantName = "GS25 강남역점", date = "8월 01일 · 오전 9:10", totalAmount = 4800.0, category = "식비", categoryColor = "#FEF3C7", paymentMethod = "현금")
        )
    }

    fun selectTab(route: String) {
        _uiState.update { it.copy(currentTab = route) }
    }
}
