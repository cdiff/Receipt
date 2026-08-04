package com.pasic.receipt.ui.scan

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasic.receipt.data.local.entity.ReceiptEntity
import com.pasic.receipt.data.repository.ReceiptRepository
import com.pasic.receipt.util.ReceiptImageStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryItem(
    val name: String,
    val colorHex: String
)

data class ScanUiState(
    val isScanning: Boolean = false,
    val ocrResult: OcrResult? = null,
    val customCategories: List<CategoryItem> = listOf(
        CategoryItem("식비", "#FEF3C7"),
        CategoryItem("교통비", "#DBEAFE"),
        CategoryItem("사무용품", "#F3E8FF"),
        CategoryItem("미분류", "#F1F5F9")
    ),
    val isSaveSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ScanSharedViewModel @Inject constructor(
    private val repository: ReceiptRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _saveSuccessEvent = MutableSharedFlow<Unit>()
    val saveSuccessEvent: SharedFlow<Unit> = _saveSuccessEvent.asSharedFlow()

    fun processBitmap(context: Context, bitmap: Bitmap, onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            val savedPath = ReceiptImageStorage.saveBitmap(context, bitmap)
            val result = ReceiptOcrEngine.processBitmap(bitmap, savedPath)
            _uiState.update {
                it.copy(
                    isScanning = false,
                    ocrResult = result
                )
            }
            onComplete()
        }
    }

    fun processGalleryUri(context: Context, uri: Uri, onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            val savedPath = ReceiptImageStorage.copyUriToAppStorage(context, uri) ?: ""
            val result = ReceiptOcrEngine.processImage(context, uri, savedPath)
            _uiState.update {
                it.copy(
                    isScanning = false,
                    ocrResult = result
                )
            }
            onComplete()
        }
    }

    fun setSimulationResult(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            val fallbackPath = ""
            val result = ReceiptOcrEngine.generateSampleDemoResult(fallbackPath)
            _uiState.update {
                it.copy(
                    isScanning = false,
                    ocrResult = result
                )
            }
            onComplete()
        }
    }

    fun updateOcrResult(updated: OcrResult) {
        // When user manually edits any field, set confidence score to 100%
        val with100Score = updated.copy(confidenceScore = 100)
        _uiState.update { it.copy(ocrResult = with100Score) }
    }

    fun addCustomCategory(name: String, colorHex: String): CategoryItem {
        val newItem = CategoryItem(name, colorHex)
        _uiState.update { state ->
            val updatedList = state.customCategories.toMutableList().apply {
                if (none { it.name == name }) {
                    add(newItem)
                }
            }
            state.copy(customCategories = updatedList)
        }
        return newItem
    }

    fun saveReceipt(
        merchantName: String,
        date: String,
        amount: Double,
        currency: String,
        businessNumber: String?,
        category: String,
        categoryColor: String,
        imagePath: String,
        confidence: Int
    ) {
        viewModelScope.launch {
            val entity = ReceiptEntity(
                merchantName = merchantName.ifBlank { "(주) 스타벅스 코리아" },
                date = date.ifBlank { "8월 04일 · 오후 2:30" },
                totalAmount = amount,
                currency = currency,
                convertedAmountKrw = amount,
                businessNumber = businessNumber,
                category = category,
                categoryColor = categoryColor,
                imagePath = imagePath,
                ocrConfidence = confidence
            )
            repository.insertReceipt(entity)
            _uiState.update { it.copy(isSaveSuccess = true) }
            _saveSuccessEvent.emit(Unit)
        }
    }

    fun resetState() {
        _uiState.update {
            it.copy(
                isScanning = false,
                ocrResult = null,
                isSaveSuccess = false,
                errorMessage = null
            )
        }
    }
}
