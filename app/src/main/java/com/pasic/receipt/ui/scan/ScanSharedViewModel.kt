package com.pasic.receipt.ui.scan

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasic.receipt.ai.OcrResult
import com.pasic.receipt.ai.ReceiptOcrEngine
import com.pasic.receipt.data.local.entity.ReceiptEntity
import com.pasic.receipt.data.repository.ReceiptRepository
import com.pasic.receipt.util.ReceiptImageStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val isManualMode: Boolean = false,
    val scanStep: Int = 1, // 1: 텍스트 스캔 중, 2: 상호명 및 금액 추출 중, 3: 카테고리 자동 분류 중
    val isStepDone: Boolean = false, // 단계 완료 체크 아이콘 팝업 표시 여부
    val isScanFailed: Boolean = false, // 영수증 인식 실패 상태
    val capturedBitmap: Bitmap? = null, // 분석 중인 캡처 정지 화면 이미지
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

    private var scanningJob: Job? = null

    fun cancelScanning() {
        scanningJob?.cancel()
        scanningJob = null
        _uiState.update { it.copy(isScanning = false, scanStep = 1, isStepDone = false, capturedBitmap = null, isScanFailed = false) }
    }

    fun resetScanFailure() {
        _uiState.update { it.copy(isScanFailed = false, isScanning = false, scanStep = 1, isStepDone = false, capturedBitmap = null) }
    }

    fun simulateScanFailure() {
        scanningJob?.cancel()
        _uiState.update { it.copy(isScanning = false, isScanFailed = true, capturedBitmap = null) }
    }

    private fun Bitmap.resizeForGemini(maxPx: Int = 1536): Bitmap {
        val maxDim = maxOf(width, height)
        if (maxDim <= maxPx) return this
        val scale = maxPx.toFloat() / maxDim
        return Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), true)
    }

    fun processBitmap(context: Context, bitmap: Bitmap, onComplete: () -> Unit) {
        scanningJob?.cancel()
        scanningJob = viewModelScope.launch {
            try {
                // 1536px 리사이징
                val resized = bitmap.resizeForGemini(1536)
                _uiState.update { it.copy(isScanning = true, isManualMode = false, isScanFailed = false, scanStep = 1, isStepDone = false, capturedBitmap = resized) }

                val savedPath = ReceiptImageStorage.saveBitmap(context, resized)
                delay(700)

                // 1단계 완료 ➔ 체크 팝업
                _uiState.update { it.copy(isStepDone = true) }
                delay(750)

                // 2단계 시작
                _uiState.update { it.copy(scanStep = 2, isStepDone = false) }
                val existingCats = _uiState.value.customCategories.map { it.name }
                val result = ReceiptOcrEngine.processBitmap(resized, savedPath, existingCats)

                // 2단계 완료 ➔ 체크 팝업
                _uiState.update { it.copy(isStepDone = true) }
                delay(750)

                // 영수증 인식 실패 조건 검사 (신뢰도가 너무 낮거나 필수 정보 없음)
                if (result.confidenceScore < 40 || (result.merchantName.isBlank() && result.totalAmount == 0.0)) {
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            isScanFailed = true,
                            scanStep = 1,
                            isStepDone = false,
                            capturedBitmap = null
                        )
                    }
                    return@launch
                }

                // 3단계 시작
                _uiState.update { it.copy(scanStep = 3, isStepDone = false) }
                delay(600)

                // 3단계 완료 ➔ 체크 팝업
                _uiState.update { it.copy(isStepDone = true) }
                delay(750)

                _uiState.update {
                    it.copy(
                        isScanning = false,
                        isScanFailed = false,
                        scanStep = 1,
                        isStepDone = false,
                        capturedBitmap = null,
                        ocrResult = result
                    )
                }
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isScanning = false, isScanFailed = true, scanStep = 1, isStepDone = false, capturedBitmap = null) }
            }
        }
    }

    fun processGalleryUri(context: Context, uri: Uri, onComplete: () -> Unit) {
        scanningJob?.cancel()
        scanningJob = viewModelScope.launch {
            try {
                val galleryBitmap = try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(context.contentResolver, uri))
                    } else {
                        @Suppress("DEPRECATION")
                        android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                } catch (e: Exception) { null }

                _uiState.update { it.copy(isScanning = true, isScanFailed = false, scanStep = 1, isStepDone = false, capturedBitmap = galleryBitmap) }

                val savedPath = ReceiptImageStorage.copyUriToAppStorage(context, uri) ?: ""
                delay(700)

                // 1단계 완료 ➔ 체크 팝업
                _uiState.update { it.copy(isStepDone = true) }
                delay(750)

                // 2단계 시작
                _uiState.update { it.copy(scanStep = 2, isStepDone = false) }
                val existingCats = _uiState.value.customCategories.map { it.name }
                val result = ReceiptOcrEngine.processImage(context, uri, savedPath, existingCats)

                // 2단계 완료 ➔ 체크 팝업
                _uiState.update { it.copy(isStepDone = true) }
                delay(750)

                // 영수증 인식 실패 조건 검사
                if (result.confidenceScore < 40 || (result.merchantName.isBlank() && result.totalAmount == 0.0)) {
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            isScanFailed = true,
                            scanStep = 1,
                            isStepDone = false,
                            capturedBitmap = null
                        )
                    }
                    return@launch
                }

                // 3단계 시작
                _uiState.update { it.copy(scanStep = 3, isStepDone = false) }
                delay(600)

                // 3단계 완료 ➔ 체크 팝업
                _uiState.update { it.copy(isStepDone = true) }
                delay(750)

                _uiState.update {
                    it.copy(
                        isScanning = false,
                        isScanFailed = false,
                        scanStep = 1,
                        isStepDone = false,
                        capturedBitmap = null,
                        ocrResult = result
                    )
                }
                onComplete()
            } catch (e: Exception) {
                android.util.Log.e("ScanSharedViewModel", "processGalleryUri error: ${e.message}", e)
                _uiState.update { it.copy(isScanning = false, isScanFailed = true, scanStep = 1, isStepDone = false, capturedBitmap = null) }
            }
        }
    }

    fun setSimulationResult(context: Context, onComplete: () -> Unit) {
        scanningJob?.cancel()
        scanningJob = viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanStep = 1) }
            delay(500)
            _uiState.update { it.copy(scanStep = 2) }
            delay(500)
            _uiState.update { it.copy(scanStep = 3) }
            delay(400)

            val fallbackPath = ""
            val result = ReceiptOcrEngine.generateSampleDemoResult(fallbackPath)
            _uiState.update {
                it.copy(
                    isScanning = false,
                    scanStep = 1,
                    ocrResult = result
                )
            }
            onComplete()
        }
    }

    fun updateOcrResult(updated: OcrResult) {
        val with100Score = updated.copy(confidenceScore = 100)
        _uiState.update { it.copy(ocrResult = with100Score) }
    }

    fun addCustomCategory(name: String, colorHex: String) {
        if (name.isBlank()) return
        val newItem = CategoryItem(name, colorHex)
        _uiState.update { state ->
            if (state.customCategories.none { it.name == name }) {
                state.copy(customCategories = state.customCategories + newItem)
            } else {
                state
            }
        }
    }

    fun startManualInputMode() {
        scanningJob?.cancel()
        val now = java.time.LocalDateTime.now()
        val dateStr = String.format(
            java.util.Locale.KOREA,
            "%04d.%02d.%02d %02d:%02d",
            now.year, now.monthValue, now.dayOfMonth, now.hour, now.minute
        )
        _uiState.update {
            it.copy(
                isManualMode = true,
                isScanning = false,
                isScanFailed = false,
                capturedBitmap = null,
                ocrResult = OcrResult(
                    merchantName = "",
                    date = dateStr,
                    totalAmount = 0.0,
                    category = "식비",
                    categoryColor = "#FEF3C7",
                    subCategory = "기타",
                    paymentMethod = "카드",
                    proofType = "일반영수증",
                    confidenceScore = 0,
                    imagePath = ""
                )
            )
        }
    }

    fun setManualImagePath(imagePath: String) {
        _uiState.update { state ->
            state.copy(ocrResult = state.ocrResult?.copy(imagePath = imagePath))
        }
    }

    fun saveReceiptToDatabase(
        merchantName: String,
        date: String,
        amount: Double,
        currency: String,
        businessNumber: String,
        confidence: Int,
        category: String,
        categoryColor: String,
        imagePath: String,
        subCategory: String? = null,
        paymentMethod: String = "신용카드",
        proofType: String = "일반영수증",
        vatAmount: Double? = null
    ) {
        viewModelScope.launch {
            try {
                val isManual = _uiState.value.isManualMode
                val finalConfidence = if (isManual) null else confidence
                val finalSubCategory = if (isManual) (subCategory ?: "기타") else subCategory?.takeIf { it.isNotBlank() }
                val calculatedVat = vatAmount ?: if (amount > 0.0) Math.round(amount / 11.0).toDouble() else null

                val entity = ReceiptEntity(
                    merchantName = merchantName.ifBlank { "알 수 없는 상호" },
                    date = date,
                    totalAmount = amount,
                    currency = currency,
                    businessNumber = businessNumber,
                    ocrConfidence = finalConfidence,
                    category = category,
                    categoryColor = categoryColor,
                    subCategory = finalSubCategory,
                    imagePath = imagePath,
                    paymentMethod = paymentMethod.ifBlank { "카드" },
                    proofType = proofType.ifBlank { "일반영수증" },
                    vatAmount = calculatedVat
                )
                repository.insertReceipt(entity)
                _uiState.update { it.copy(isSaveSuccess = true, errorMessage = null) }
                _saveSuccessEvent.emit(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(errorMessage = "영수증 저장에 실패했습니다.") }
            }
        }
    }

    fun resetSaveSuccess() {
        _uiState.update { it.copy(isSaveSuccess = false, errorMessage = null) }
    }
}
