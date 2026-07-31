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
import javax.inject.Inject

data class HomeUiState(
    val totalSpendingFormatted: String = "26,809,600원",
    val trendFormatted: String = "3,220,500원 (28.64%)",
    val recentReceipts: List<ReceiptEntity> = emptyList(),
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
            // Seed mock data if database is empty for initial demo presentation
            repository.getAllReceipts().collectLatest { receipts ->
                if (receipts.isEmpty()) {
                    val defaultMockList = listOf(
                        ReceiptEntity(
                            id = 1,
                            merchantName = "스타벅스 강남점",
                            date = "2026.07.31",
                            totalAmount = 12400.0,
                            category = "식비",
                            paymentMethod = "법인카드"
                        ),
                        ReceiptEntity(
                            id = 2,
                            merchantName = "명동교자 본점",
                            date = "2026.07.30",
                            totalAmount = 28000.0,
                            category = "식비",
                            paymentMethod = "개인카드"
                        )
                    )
                    _uiState.update { it.copy(recentReceipts = defaultMockList) }
                } else {
                    _uiState.update { it.copy(recentReceipts = receipts.take(5)) }
                }
            }
        }
    }

    fun selectTab(route: String) {
        _uiState.update { it.copy(currentTab = route) }
    }
}
