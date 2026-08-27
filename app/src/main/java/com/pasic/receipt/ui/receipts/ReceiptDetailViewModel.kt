package com.pasic.receipt.ui.receipts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasic.receipt.data.local.entity.ReceiptEntity
import com.pasic.receipt.data.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReceiptDetailUiState(
    val receipt: ReceiptEntity? = null,
    val categoryAverageAmount: Double = 0.0,
    val isAboveAverage: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed class ReceiptDetailEvent {
    data object NavigateBack : ReceiptDetailEvent()
}

@HiltViewModel
class ReceiptDetailViewModel @Inject constructor(
    private val repository: ReceiptRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiptDetailUiState())
    val uiState: StateFlow<ReceiptDetailUiState> = _uiState.asStateFlow()

    // 삭제 완료 후 일회성 화면 이탈 이벤트 채널
    private val _events = Channel<ReceiptDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var currentReceiptId: Long = 0L

    @OptIn(ExperimentalCoroutinesApi::class)
    fun loadReceipt(id: Long) {
        currentReceiptId = id
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 중첩 collect 제거: flatMapLatest + combine으로 단일 스트림으로 통합
            repository.getReceiptById(id)
                .flatMapLatest { entity ->
                    if (entity == null) {
                        // 영수증이 삭제된 경우 (isDeleted=1로 DB 변경) → 이벤트 채널로 화면 이탈 신호
                        flowOf(null to emptyList<ReceiptEntity>())
                    } else {
                        // 존재하는 경우: 동종 카테고리 전체 목록과 combine
                        combine(
                            flowOf(entity),
                            repository.getAllReceipts()
                        ) { e, allReceipts -> e to allReceipts }
                    }
                }
                .collect { (entity, allReceipts) ->
                    if (entity == null) {
                        // getReceiptById가 null을 방출 → 외부에서 softDelete 완료된 것
                        // deleteReceipt()의 Channel 이벤트가 화면 이탈을 담당하므로 여기서는 로딩만 해제
                        _uiState.update { it.copy(isLoading = false) }
                    } else {
                        val topCategory = entity.category.split("/").firstOrNull() ?: entity.category
                        val sameCategoryReceipts = allReceipts.filter { r ->
                            r.category.contains(topCategory) ||
                                    topCategory.contains(r.category.split("/").firstOrNull() ?: r.category)
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
                                isLoading = false
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
                // 일회성 이탈 이벤트 발송 (isDeleted 상태 플래그 제거로 AllReceipts 재방출이 덮어쓸 가능성 원천 차단)
                _events.send(ReceiptDetailEvent.NavigateBack)
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

