package com.pasic.receipt.ui.receipts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasic.receipt.data.local.entity.ReceiptEntity
import com.pasic.receipt.data.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReceiptDetailUiState(
    val receipt: ReceiptEntity? = null,
    val categoryAverageAmount: Double = 0.0,
    val isAboveAverage: Boolean = false,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ReceiptDetailViewModel @Inject constructor(
    private val repository: ReceiptRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiptDetailUiState())
    val uiState: StateFlow<ReceiptDetailUiState> = _uiState.asStateFlow()

    private var currentReceiptId: Long = 0L

    fun loadReceipt(id: Long) {
        currentReceiptId = id
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getReceiptById(id).collect { entity ->
                if (entity != null) {
                    // 동종 카테고리 평균 지출 계산
                    val topCategory = entity.category.split("/").firstOrNull() ?: entity.category
                    repository.getAllReceipts().collect { allReceipts ->
                        val sameCategoryReceipts = allReceipts.filter { r ->
                            r.category.contains(topCategory) || topCategory.contains(r.category.split("/").firstOrNull() ?: r.category)
                        }
                        val avg = if (sameCategoryReceipts.isNotEmpty()) {
                            sameCategoryReceipts.map { it.totalAmount }.average()
                        } else {
                            0.0
                        }
                        val isAbove = entity.totalAmount > avg && avg > 0.0

                        _uiState.update {
                            it.copy(
                                receipt = entity,
                                categoryAverageAmount = avg,
                                isAboveAverage = isAbove,
                                isLoading = false,
                                isDeleted = false
                            )
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            receipt = null,
                            isLoading = false,
                            isDeleted = true
                        )
                    }
                }
            }
        }
    }

    fun deleteReceipt() {
        val id = currentReceiptId
        if (id <= 0L) return
        viewModelScope.launch {
            try {
                repository.softDeleteReceipt(id)
                _uiState.update { it.copy(isDeleted = true) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(errorMessage = "영수증 삭제에 실패했습니다.") }
            }
        }
    }

    fun updateReceipt(updated: ReceiptEntity) {
        viewModelScope.launch {
            try {
                repository.insertReceipt(updated)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(errorMessage = "영수증 수정에 실패했습니다.") }
            }
        }
    }

    fun updateMemo(newMemo: String) {
        val current = _uiState.value.receipt ?: return
        val updated = current.copy(memo = newMemo)
        updateReceipt(updated)
    }

    fun togglePersonalOrCancelled() {
        val current = _uiState.value.receipt ?: return
        val updated = current.copy(isPersonalOrCancelled = !current.isPersonalOrCancelled)
        updateReceipt(updated)
    }
}
