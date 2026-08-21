package com.pasic.receipt.ui.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasic.receipt.data.local.entity.ReceiptEntity
import com.pasic.receipt.data.preferences.ALL_CSV_COLUMNS
import com.pasic.receipt.data.preferences.AppThemeOption
import com.pasic.receipt.data.preferences.UserPreferences
import com.pasic.receipt.data.preferences.UserPreferencesRepository
import com.pasic.receipt.data.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

data class StorageInfo(
    val sizeMbText: String = "0.0 MB",
    val totalCount: Int = 0
)

data class SettingsUiState(
    val storageInfo: StorageInfo = StorageInfo(),
    val isProcessingBackup: Boolean = false,
    val isProcessingRestore: Boolean = false,
    val isOptimizing: Boolean = false,
    val toastMessage: String? = null,
    val showThemeDialog: Boolean = false,
    val showCsvOptionsSheet: Boolean = false,
    val showOptimizeWarningDialog: Boolean = false,
    val showBackupRestoreDialog: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val receiptRepository: ReceiptRepository
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshStorageInfo()
    }

    fun refreshStorageInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val receipts = receiptRepository.getAllReceipts().first()
            val totalCount = receipts.size

            var totalBytes = 0L
            receipts.forEach { r ->
                if (r.imagePath.isNotBlank()) {
                    val file = File(r.imagePath)
                    if (file.exists()) {
                        totalBytes += file.length()
                    }
                }
            }

            val mb = totalBytes / (1024f * 1024f)
            val df = DecimalFormat("#,##0.0")
            val sizeText = "${df.format(mb)} MB"

            _uiState.update {
                it.copy(storageInfo = StorageInfo(sizeMbText = sizeText, totalCount = totalCount))
            }
        }
    }

    // ── 내보내기 기본값 ──────────────────────────────────────────

    fun updateAuthor(author: String) {
        viewModelScope.launch { preferencesRepository.updateDefaultAuthor(author) }
    }

    fun updateDepartment(dept: String) {
        viewModelScope.launch { preferencesRepository.updateDefaultDepartment(dept) }
    }

    fun updatePurpose(purpose: String) {
        viewModelScope.launch { preferencesRepository.updateDefaultPurpose(purpose) }
    }

    fun updateExportFormat(format: String) {
        viewModelScope.launch { preferencesRepository.updateDefaultExportFormat(format) }
    }

    // ── CSV 맞춤 세부 옵션 ────────────────────────────────────────

    fun toggleCsvColumn(columnName: String) {
        viewModelScope.launch {
            val current = userPreferences.value.csvSelectedColumns.filter { ALL_CSV_COLUMNS.contains(it) }.toMutableSet()
            if (current.contains(columnName)) {
                if (current.size > 1) current.remove(columnName)
            } else {
                current.add(columnName)
            }
            preferencesRepository.updateCsvSelectedColumns(current)
        }
    }

    fun updateCsvDateFormat(format: String) {
        viewModelScope.launch { preferencesRepository.updateCsvDateFormat(format) }
    }

    fun updateCsvAmountFormat(format: String) {
        viewModelScope.launch { preferencesRepository.updateCsvAmountFormat(format) }
    }

    fun updateZipNamingRule(rule: String) {
        viewModelScope.launch { preferencesRepository.updateZipImageNamingRule(rule) }
    }

    // ── 토글 옵션 ────────────────────────────────────────────────

    fun toggleAutoOptimize(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateAutoOptimizeEnabled(enabled) }
    }

    fun toggleAutoCrop(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateAutoCropEnabled(enabled) }
    }

    fun toggleBwEnhancement(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateBwEnhancementEnabled(enabled) }
    }

    fun toggleAiCategory(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateAiCategoryEnabled(enabled) }
    }

    fun toggleScanReminderPush(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateScanReminderPushEnabled(enabled) }
    }

    fun toggleExpenseDDayPush(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateExpenseDDayPushEnabled(enabled) }
    }

    fun toggleBackupReminderPush(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateBackupReminderPushEnabled(enabled) }
    }

    fun updateAppTheme(theme: AppThemeOption) {
        viewModelScope.launch { preferencesRepository.updateAppTheme(theme) }
    }

    // ── 다이얼로그 제어 ───────────────────────────────────────────

    fun setShowThemeDialog(show: Boolean) = _uiState.update { it.copy(showThemeDialog = show) }
    fun setShowCsvOptionsSheet(show: Boolean) = _uiState.update { it.copy(showCsvOptionsSheet = show) }
    fun setShowOptimizeWarningDialog(show: Boolean) = _uiState.update { it.copy(showOptimizeWarningDialog = show) }
    fun setShowBackupRestoreDialog(show: Boolean) = _uiState.update { it.copy(showBackupRestoreDialog = show) }
    fun clearToastMessage() = _uiState.update { it.copy(toastMessage = null) }

    // ── 데이터 백업 & 복원 (.zip) ──────────────────────────────────

    fun exportBackupZip(context: Context, targetUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isProcessingBackup = true) }
            try {
                val receipts = receiptRepository.getAllReceipts().first()

                val jsonArray = JSONArray()
                receipts.forEach { r ->
                    val obj = JSONObject().apply {
                        put("id", r.id)
                        put("merchantName", r.merchantName)
                        put("totalAmount", r.totalAmount)
                        put("date", r.date)
                        put("category", r.category)
                        put("paymentMethod", r.paymentMethod)
                        put("businessNumber", r.businessNumber ?: "")
                        put("vatAmount", r.vatAmount ?: 0.0)
                        put("proofType", r.proofType)
                        put("memo", r.memo ?: "")
                        put("imagePath", r.imagePath)
                        put("createdAt", r.createdAt)
                    }
                    jsonArray.put(obj)
                }

                val backupRootObj = JSONObject().apply {
                    put("version", 1)
                    put("exportDate", LocalDate.now().toString())
                    put("receipts", jsonArray)
                }

                context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                    ZipOutputStream(outputStream).use { zos ->
                        // 1. JSON 메타데이터 덤프
                        zos.putNextEntry(ZipEntry("backup_data.json"))
                        zos.write(backupRootObj.toString(2).toByteArray(Charsets.UTF_8))
                        zos.closeEntry()

                        // 2. 영수증 이미지 패키징
                        receipts.forEachIndexed { index, r ->
                            if (r.imagePath.isNotBlank()) {
                                val imgFile = File(r.imagePath)
                                if (imgFile.exists()) {
                                    val ext = imgFile.extension.ifEmpty { "jpg" }
                                    val entryName = "images/receipt_${r.id}_${index + 1}.$ext"
                                    zos.putNextEntry(ZipEntry(entryName))
                                    imgFile.inputStream().use { it.copyTo(zos) }
                                    zos.closeEntry()
                                }
                            }
                        }
                    }
                }

                // 💾 백업 성공 즉시 타임스탬프 갱신 (30일 타이머 리셋)
                context.getSharedPreferences("receipt_notification_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putLong("last_backup_export_timestamp", System.currentTimeMillis())
                    .apply()

                _uiState.update {
                    it.copy(
                        isProcessingBackup = false,
                        showBackupRestoreDialog = false,
                        toastMessage = "데이터 백업이 성공적으로 완료되었습니다."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessingBackup = false,
                        toastMessage = "백업 중 오류가 발생했습니다: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun restoreBackupZip(context: Context, sourceUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isProcessingRestore = true) }
            try {
                var jsonContent: String? = null
                val restoredImages = mutableMapOf<String, ByteArray>()

                context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zis ->
                        var entry: ZipEntry? = zis.nextEntry
                        while (entry != null) {
                            if (entry.name == "backup_data.json") {
                                jsonContent = zis.bufferedReader(Charsets.UTF_8).readText()
                            } else if (entry.name.startsWith("images/")) {
                                val baos = ByteArrayOutputStream()
                                zis.copyTo(baos)
                                restoredImages[entry.name] = baos.toByteArray()
                            }
                            entry = zis.nextEntry
                        }
                    }
                }

                if (jsonContent.isNullOrBlank()) {
                    throw IllegalArgumentException("올바른 백업 파일(backup_data.json)을 찾을 수 없습니다.")
                }

                val rootObj = JSONObject(jsonContent!!)
                val version = rootObj.optInt("version", 0)
                if (version < 1) {
                    throw IllegalArgumentException("지원하지 않는 백업 파일 버전입니다.")
                }

                val jsonArray = rootObj.getJSONArray("receipts")
                val imagesDir = File(context.filesDir, "receipt_images").apply { mkdirs() }
                val existingReceipts = receiptRepository.getAllReceipts().first()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val merchant = obj.optString("merchantName", "알 수 없는 상호")
                    val amount = obj.optDouble("totalAmount", 0.0)
                    val date = obj.optString("date", "")
                    val createdAt = obj.optLong("createdAt", System.currentTimeMillis())

                    // 이미 DB에 동일한 영수증(상호명 + 금액 + 날짜)이 존재하면 중복 생성 방지
                    val isDuplicate = existingReceipts.any {
                        it.merchantName == merchant && it.totalAmount == amount && it.date == date
                    }
                    if (isDuplicate) continue

                    val oldPath = obj.optString("imagePath", "")
                    var newImagePath: String? = null
                    if (oldPath.isNotBlank()) {
                        val matchingEntry = restoredImages.entries.find { it.key.contains("receipt_${obj.optLong("id")}_") }
                            ?: restoredImages.entries.find { it.key.endsWith(".jpg") || it.key.endsWith(".png") }

                        matchingEntry?.let { entry ->
                            val fileExt = if (entry.key.endsWith(".png")) "png" else "jpg"
                            val restoredFile = File(imagesDir, "restored_${System.currentTimeMillis()}_$i.$fileExt")
                            restoredFile.writeBytes(entry.value)
                            newImagePath = restoredFile.absolutePath
                        }
                    }

                    val entity = ReceiptEntity(
                        merchantName = merchant,
                        totalAmount = amount,
                        date = date,
                        category = obj.optString("category", "식비"),
                        paymentMethod = obj.optString("paymentMethod", "카드"),
                        businessNumber = obj.optString("businessNumber").ifEmpty { null },
                        vatAmount = if (obj.has("vatAmount")) obj.getDouble("vatAmount") else null,
                        proofType = obj.optString("proofType", "일반영수증"),
                        memo = obj.optString("memo").ifEmpty { null },
                        imagePath = newImagePath ?: oldPath,
                        createdAt = createdAt
                    )
                    receiptRepository.insertReceipt(entity)
                }

                refreshStorageInfo()
                _uiState.update {
                    it.copy(
                        isProcessingRestore = false,
                        showBackupRestoreDialog = false,
                        toastMessage = "데이터 복원이 완료되었습니다."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessingRestore = false,
                        toastMessage = "복원 실패: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    // ── 이미지 최적화 (JPEG 75% 압축) ───────────────────────────────

    fun executeImageOptimization(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isOptimizing = true, showOptimizeWarningDialog = false) }
            try {
                val receipts = receiptRepository.getAllReceipts().first()
                val sixMonthsAgo = LocalDate.now().minusMonths(6).toString()

                var freedBytes = 0L
                var optimizedCount = 0

                receipts.forEach { r ->
                    if (r.date < sixMonthsAgo && r.imagePath.isNotBlank()) {
                        val file = File(r.imagePath)
                        if (file.exists() && file.length() > 200 * 1024) { // 200KB 초과 파일 대상
                            val originalSize = file.length()
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            if (bitmap != null) {
                                val baos = ByteArrayOutputStream()
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos)
                                val compressedBytes = baos.toByteArray()
                                if (compressedBytes.size < originalSize) {
                                    FileOutputStream(file).use { fos -> fos.write(compressedBytes) }
                                    freedBytes += (originalSize - compressedBytes.size)
                                    optimizedCount++
                                }
                                bitmap.recycle()
                            }
                        }
                    }
                }

                val freedMb = freedBytes / (1024f * 1024f)
                val df = DecimalFormat("#,##0.0")
                refreshStorageInfo()

                _uiState.update {
                    it.copy(
                        isOptimizing = false,
                        toastMessage = if (optimizedCount > 0) "${optimizedCount}개 이미지 최적화 완료! (${df.format(freedMb)} MB 절감)" else "최적화할 오래된 이미지가 없습니다."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isOptimizing = false,
                        toastMessage = "최적화 중 오류가 발생했습니다: ${e.localizedMessage}"
                    )
                }
            }
        }
    }
}
