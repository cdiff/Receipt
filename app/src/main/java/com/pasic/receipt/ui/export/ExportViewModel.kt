package com.pasic.receipt.ui.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasic.receipt.data.local.entity.ReceiptEntity
import com.pasic.receipt.data.local.entity.extractLocalDate
import com.pasic.receipt.data.notification.NotificationRepository
import com.pasic.receipt.data.preferences.ALL_CSV_COLUMNS
import com.pasic.receipt.data.preferences.UserPreferences
import com.pasic.receipt.data.preferences.UserPreferencesRepository
import com.pasic.receipt.data.repository.ReceiptRepository
import com.pasic.receipt.util.PdfReportGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

enum class ExportPeriod { THIS_MONTH, CUSTOM }
enum class ExportFormat { EXCEL, PDF }
enum class PendingPdfAction { NONE, DOWNLOAD, EMAIL, MESSENGER }

data class ExportUiState(
    val customStartMs: Long? = null,
    val customEndMs: Long? = null,
    val formattedDateRangeText: String = "",
    val csvHeaderColumns: List<String> = ALL_CSV_COLUMNS,
    val userPreferences: UserPreferences = UserPreferences(),
    val selectedFormat: ExportFormat = ExportFormat.EXCEL,
    val defaultAuthor: String = "",
    val defaultDepartment: String = "",
    val defaultPurpose: String = "",
    val includeAll: Boolean = true,
    val groupByCategory: Boolean = false,
    val includeImages: Boolean = true,
    val targetReceiptCount: Int = 0,
    val targetTotalAmount: Double = 0.0,
    val isGenerating: Boolean = false,
    val showPdfInfoSheet: Boolean = false,
    val pendingPdfAction: PendingPdfAction = PendingPdfAction.NONE,
    val showPdfPreviewDialog: Boolean = false,
    val showSaveSuccessDialog: Boolean = false,
    val previewPdfFile: File? = null,
    val savedFileName: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val repository: ReceiptRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    private var hasInitializedDefaults = false

    init {
        val today = LocalDate.now()
        val start = today.withDayOfMonth(1)
        _uiState.update { it.copy(formattedDateRangeText = formatDateRangeText(start, today)) }
        refreshTargetCount()

        viewModelScope.launch {
            preferencesRepository.userPreferencesFlow.collect { prefs ->
                val columns = ALL_CSV_COLUMNS.filter { prefs.csvSelectedColumns.contains(it) }
                _uiState.update { state ->
                    state.copy(
                        defaultAuthor = prefs.defaultAuthor,
                        defaultDepartment = prefs.defaultDepartment,
                        defaultPurpose = prefs.defaultPurpose,
                        selectedFormat = if (!hasInitializedDefaults) ExportFormat.EXCEL else state.selectedFormat,
                        csvHeaderColumns = if (columns.isNotEmpty()) columns else ALL_CSV_COLUMNS,
                        userPreferences = prefs
                    )
                }
                hasInitializedDefaults = true
            }
        }
    }

    fun setCustomDateRange(startDate: LocalDate, endDate: LocalDate) {
        val startMs = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMs = endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        _uiState.update {
            it.copy(
                customStartMs = startMs,
                customEndMs = endMs,
                formattedDateRangeText = formatDateRangeText(startDate, endDate)
            )
        }
        refreshTargetCount()
    }

    private fun formatDateRangeText(start: LocalDate, end: LocalDate): String {
        val startStr = start.format(DateTimeFormatter.ofPattern("yyyy. MM. dd"))
        val endStr = if (start.year == end.year) {
            end.format(DateTimeFormatter.ofPattern("MM. dd"))
        } else {
            end.format(DateTimeFormatter.ofPattern("yyyy. MM. dd"))
        }
        return "$startStr ~ $endStr"
    }

    fun selectFormat(format: ExportFormat) {
        _uiState.update { it.copy(selectedFormat = format) }
    }

    fun toggleIncludeAll(v: Boolean) = _uiState.update { it.copy(includeAll = v) }
    fun toggleGroupByCategory(v: Boolean) = _uiState.update { it.copy(groupByCategory = v) }
    fun toggleIncludeImages(v: Boolean) = _uiState.update { it.copy(includeImages = v) }

    fun onGenerateClicked() {
        if (_uiState.value.targetReceiptCount == 0) {
            com.pasic.receipt.util.ToastEventBus.showToast("내보낼 영수증 내역이 없습니다.")
            return
        }
        if (_uiState.value.selectedFormat == ExportFormat.PDF) {
            _uiState.update {
                it.copy(
                    showPdfInfoSheet = true,
                    pendingPdfAction = PendingPdfAction.DOWNLOAD
                )
            }
        }
    }

    fun onEmailShareClicked(context: Context) {
        if (_uiState.value.targetReceiptCount == 0) {
            com.pasic.receipt.util.ToastEventBus.showToast("내보낼 영수증 내역이 없습니다.")
            return
        }
        if (_uiState.value.selectedFormat == ExportFormat.PDF) {
            _uiState.update {
                it.copy(
                    showPdfInfoSheet = true,
                    pendingPdfAction = PendingPdfAction.EMAIL
                )
            }
        } else {
            shareViaEmail(context)
        }
    }

    fun onMessengerShareClicked(context: Context) {
        if (_uiState.value.targetReceiptCount == 0) {
            com.pasic.receipt.util.ToastEventBus.showToast("내보낼 영수증 내역이 없습니다.")
            return
        }
        if (_uiState.value.selectedFormat == ExportFormat.PDF) {
            _uiState.update {
                it.copy(
                    showPdfInfoSheet = true,
                    pendingPdfAction = PendingPdfAction.MESSENGER
                )
            }
        } else {
            shareViaMessenger(context)
        }
    }

    /**
     * PdfInfoBottomSheet에서 사용자 정보 입력 후 확인 버튼을 눌렀을 때 실행됩니다.
     */
    fun onPdfInfoConfirmed(context: Context, author: String, dept: String, purpose: String) {
        val action = _uiState.value.pendingPdfAction
        _uiState.update { it.copy(showPdfInfoSheet = false, pendingPdfAction = PendingPdfAction.NONE) }

        when (action) {
            PendingPdfAction.DOWNLOAD -> generatePdf(context, author, dept, purpose)
            PendingPdfAction.EMAIL -> sharePdfViaEmail(context, author, dept, purpose)
            PendingPdfAction.MESSENGER -> sharePdfViaMessenger(context, author, dept, purpose)
            PendingPdfAction.NONE -> generatePdf(context, author, dept, purpose)
        }
    }

    fun dismissPdfInfoSheet() = _uiState.update {
        it.copy(showPdfInfoSheet = false, pendingPdfAction = PendingPdfAction.NONE)
    }
    fun dismissPdfPreviewDialog() = _uiState.update { it.copy(showPdfPreviewDialog = false) }
    fun dismissSaveSuccessDialog() = _uiState.update { it.copy(showSaveSuccessDialog = false) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    private suspend fun getSortedReceipts(startMs: Long, endMs: Long): List<ReceiptEntity> {
        val receipts = repository.getReceiptsByDateRange(startMs, endMs)
        val isGroupByCategory = _uiState.value.groupByCategory

        return if (isGroupByCategory) {
            receipts.sortedWith(
                compareBy<ReceiptEntity> { it.category }
                    .thenByDescending { it.extractLocalDate() }
                    .thenByDescending { it.createdAt }
            )
        } else {
            receipts.sortedWith(
                compareByDescending<ReceiptEntity> { it.extractLocalDate() }
                    .thenByDescending { it.createdAt }
            )
        }
    }

    private fun refreshTargetCount() {
        viewModelScope.launch {
            val (startMs, endMs) = getDateRangeMs()
            val receipts = withContext(Dispatchers.IO) { getSortedReceipts(startMs, endMs) }
            _uiState.update {
                it.copy(
                    targetReceiptCount = receipts.size,
                    targetTotalAmount = receipts.sumOf { r -> r.totalAmount }
                )
            }
        }
    }

    // ── CSV 내보내기 ───────────────────────────────────────────────

    fun generateCsv(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            try {
                val (startMs, endMs) = getDateRangeMs()
                val receipts = withContext(Dispatchers.IO) { getSortedReceipts(startMs, endMs) }

                if (receipts.isEmpty()) {
                    com.pasic.receipt.util.ToastEventBus.showToast("내보낼 영수증 내역이 없습니다.")
                    return@launch
                }

                val includeImages = _uiState.value.includeImages
                val periodLabel = getPeriodLabel()

                val (fileName, mimeType, fileBytes) = withContext(Dispatchers.IO) {
                    if (includeImages) {
                        val zipName = "${periodLabel}_영수증_증빙.zip"
                        val csvContent = buildCsvString(receipts, includeImageColumn = true)
                        val baos = ByteArrayOutputStream()
                        ZipOutputStream(baos).use { zos ->
                            val csvEntry = ZipEntry("${periodLabel}_지출내역장부.csv")
                            zos.putNextEntry(csvEntry)
                            zos.write(csvContent.toByteArray(Charsets.UTF_8))
                            zos.closeEntry()

                            receipts.forEachIndexed { index, r ->
                                if (r.imagePath.isNotBlank()) {
                                    val imageFile = File(r.imagePath)
                                    if (imageFile.exists() && imageFile.length() > 0) {
                                        val ext = imageFile.extension.ifBlank { "jpg" }
                                        val cleanDate = r.date.replace(Regex("[^0-9]"), "")
                                        val entryName = "images/receipt_${r.id}_${index + 1}_${cleanDate}.$ext"
                                        zos.putNextEntry(ZipEntry(entryName))
                                        imageFile.inputStream().use { input -> input.copyTo(zos) }
                                        zos.closeEntry()
                                    }
                                }
                            }
                        }
                        Triple(zipName, "application/zip", baos.toByteArray())
                    } else {
                        val csvName = "${periodLabel}_지출보고서.csv"
                        val csvContent = buildCsvString(receipts, includeImageColumn = false)
                        Triple(csvName, "text/csv", csvContent.toByteArray(Charsets.UTF_8))
                    }
                }

                saveToDownloads(context, fileName, mimeType, fileBytes)

                val dir = File(context.filesDir, "exports").apply { mkdirs() }
                val localFile = File(dir, fileName).apply { writeBytes(fileBytes) }

                // 알림 센터에 실제 내보내기 이력 로그 실시간 누적 (포맷 표준화: CSV / ZIP)
                notificationRepository.addExportLog(
                    fileName = fileName,
                    receiptCount = receipts.size,
                    format = if (includeImages) "ZIP" else "CSV",
                    filePath = localFile.absolutePath
                )

                if (includeImages) {
                    context.getSharedPreferences("receipt_notification_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putLong("last_backup_export_timestamp", System.currentTimeMillis())
                        .apply()
                }

                _uiState.update {
                    it.copy(
                        showSaveSuccessDialog = true,
                        savedFileName = fileName,
                        previewPdfFile = localFile
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "파일 저장 중 오류가 발생했습니다: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    // ── PDF 생성 및 미리보기 팝업 열기 ─────────────────────────────

    fun generatePdf(context: Context, author: String, dept: String, purpose: String) {
        _uiState.update { it.copy(showPdfInfoSheet = false, isGenerating = true) }
        viewModelScope.launch {
            try {
                val (startMs, endMs) = getDateRangeMs()
                val receipts = withContext(Dispatchers.IO) {
                    getSortedReceipts(startMs, endMs)
                }

                if (receipts.isEmpty()) {
                    com.pasic.receipt.util.ToastEventBus.showToast("내보낼 영수증 내역이 없습니다.")
                    return@launch
                }

                val periodLabel = getPeriodLabel()

                val pdfFile = PdfReportGenerator.generate(
                    context, receipts, periodLabel, author, dept, purpose
                )

                _uiState.update {
                    it.copy(
                        showPdfPreviewDialog = true,
                        previewPdfFile = pdfFile
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "PDF 생성 중 오류가 발생했습니다: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    // ── 미리보기에서 다운로드 저장 확인 ────────────────────────────

    fun confirmSavePdf(context: Context) {
        val pdfFile = _uiState.value.previewPdfFile ?: return
        viewModelScope.launch {
            try {
                val receipts = withContext(Dispatchers.IO) {
                    val (startMs, endMs) = getDateRangeMs()
                    getSortedReceipts(startMs, endMs)
                }
                saveToDownloads(context, pdfFile.name, "application/pdf", pdfFile.readBytes())

                // 알림 센터에 실제 PDF 다운로드 이력 로그 실시간 누적
                notificationRepository.addExportLog(
                    fileName = pdfFile.name,
                    receiptCount = receipts.size,
                    format = "PDF",
                    filePath = pdfFile.absolutePath
                )

                _uiState.update {
                    it.copy(
                        showPdfPreviewDialog = false,
                        showSaveSuccessDialog = true,
                        savedFileName = pdfFile.name
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "PDF 저장 중 오류가 발생했습니다: ${e.message}") }
            }
        }
    }

    // ── 정보 입력 후 이메일 전송 ───────────────────────────────────

    private fun sharePdfViaEmail(context: Context, author: String, dept: String, purpose: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            try {
                val (startMs, endMs) = getDateRangeMs()
                val receipts = withContext(Dispatchers.IO) {
                    repository.getReceiptsByDateRange(startMs, endMs)
                }
                val periodLabel = getPeriodLabel()
                val pdfFile = PdfReportGenerator.generate(context, receipts, periodLabel, author, dept, purpose)
                
                // 알림 센터에 실제 PDF 이메일 공유 이력 로그 실시간 누적
                notificationRepository.addExportLog(
                    fileName = pdfFile.name,
                    receiptCount = receipts.size,
                    format = "PDF",
                    filePath = pdfFile.absolutePath
                )

                launchEmailIntent(context, pdfFile)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "이메일 전송 중 오류가 발생했습니다: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    // ── 정보 입력 후 메신저 공유 ───────────────────────────────────

    private fun sharePdfViaMessenger(context: Context, author: String, dept: String, purpose: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            try {
                val (startMs, endMs) = getDateRangeMs()
                val receipts = withContext(Dispatchers.IO) {
                    getSortedReceipts(startMs, endMs)
                }
                val periodLabel = getPeriodLabel()
                val pdfFile = PdfReportGenerator.generate(context, receipts, periodLabel, author, dept, purpose)

                // 알림 센터에 실제 PDF 메신저 공유 이력 로그 실시간 누적
                notificationRepository.addExportLog(
                    fileName = pdfFile.name,
                    receiptCount = receipts.size,
                    format = "PDF",
                    filePath = pdfFile.absolutePath
                )

                launchMessengerIntent(context, pdfFile, "application/pdf")
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "메신저 공유 중 오류가 발생했습니다: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    // ── CSV 및 일반 공유 ──────────────────────────────────────────

    fun shareViaEmail(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            try {
                val (file, _) = prepareShareFile(context)
                launchEmailIntent(context, file)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "이메일 전송 중 오류가 발생했습니다: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    fun shareViaMessenger(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            try {
                val (file, mimeType) = prepareShareFile(context)
                launchMessengerIntent(context, file, mimeType)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "메신저 공유 중 오류가 발생했습니다: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    // ── 공통 인텐트 실행 헬퍼 ─────────────────────────────────────

    private fun launchEmailIntent(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_SUBJECT, "[영수증 내보내기] ${getPeriodLabel()} 지출 내역")
            putExtra(Intent.EXTRA_TEXT, "안녕하세요,\n요청하신 ${getPeriodLabel()} 영수증 내역 파일(${file.name})을 첨부합니다.")
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "이메일 전송").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    }

    private fun launchMessengerIntent(context: Context, file: File, mimeType: String) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "📄 [영수증 내보내기] ${getPeriodLabel()} 내역 파일입니다.")
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "메신저로 공유").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    }

    private suspend fun prepareShareFile(context: Context): Pair<File, String> {
        val previewFile = _uiState.value.previewPdfFile
        if (previewFile != null && previewFile.exists()) {
            val ext = previewFile.extension.lowercase()
            val mimeType = when (ext) {
                "csv" -> "text/csv"
                "zip" -> "application/zip"
                else -> "application/pdf"
            }
            return Pair(previewFile, mimeType)
        }

        val (startMs, endMs) = getDateRangeMs()
        val receipts = withContext(Dispatchers.IO) {
            getSortedReceipts(startMs, endMs)
        }
        val periodLabel = getPeriodLabel()
        val dir = File(context.filesDir, "exports").apply { mkdirs() }

        return if (_uiState.value.includeImages) {
            val zipFile = withContext(Dispatchers.IO) {
                val baos = ByteArrayOutputStream()
                ZipOutputStream(baos).use { zos ->
                    val csvContent = buildCsvString(receipts, includeImageColumn = true)
                    zos.putNextEntry(ZipEntry("${periodLabel}_지출내역장부.csv"))
                    zos.write(csvContent.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()

                    receipts.forEachIndexed { index, receipt ->
                        receipt.imagePath?.let { imgPath ->
                            val imgFile = File(imgPath)
                            if (imgFile.exists()) {
                                val ext = imgFile.extension.ifEmpty { "jpg" }
                                val localDate = receipt.extractLocalDate()
                                val cleanDate = String.format(Locale.KOREA, "%04d%02d%02d", localDate.year, localDate.monthValue, localDate.dayOfMonth)
                                val entryName = "images/receipt_${receipt.id}_${index + 1}_${cleanDate}.$ext"
                                zos.putNextEntry(ZipEntry(entryName))
                                imgFile.inputStream().use { it.copyTo(zos) }
                                zos.closeEntry()
                            }
                        }
                    }
                }
                File(dir, "${periodLabel}_영수증_증빙.zip").also { it.writeBytes(baos.toByteArray()) }
            }
            Pair(zipFile, "application/zip")
        } else {
            val csvFile = withContext(Dispatchers.IO) {
                File(dir, "${periodLabel}_지출보고서.csv").also {
                    it.writeText(buildCsvString(receipts, includeImageColumn = false), Charsets.UTF_8)
                }
            }
            Pair(csvFile, "text/csv")
        }
    }

    private suspend fun saveToDownloads(context: Context, fileName: String, mimeType: String, bytes: ByteArray) =
        withContext(Dispatchers.IO) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { stream -> stream.write(bytes) }
                }
            } else {
                val targetDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                File(targetDir, fileName).writeBytes(bytes)
            }
        }

    private fun getDateRangeMs(): Pair<Long, Long> {
        val state = _uiState.value
        if (state.customStartMs != null && state.customEndMs != null) {
            return state.customStartMs to state.customEndMs
        }
        val today = LocalDate.now()
        val start = today.withDayOfMonth(1)
        val startMs = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMs = today.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return startMs to endMs
    }

    private fun getPeriodLabel(): String {
        val state = _uiState.value
        return if (state.customStartMs != null && state.customEndMs != null) {
            val startDate = LocalDate.ofEpochDay(state.customStartMs / 86400000)
            val endDate = LocalDate.ofEpochDay(state.customEndMs / 86400000)
            "${startDate.year}년_${startDate.monthValue}월_${startDate.dayOfMonth}일~${endDate.year}년_${endDate.monthValue}월_${endDate.dayOfMonth}일"
        } else {
            val now = YearMonth.now()
            "${now.year}년_${now.monthValue}월"
        }
    }

    private fun buildCsvString(receipts: List<ReceiptEntity>, includeImageColumn: Boolean = false): String {
        val prefs = _uiState.value.userPreferences
        val selectedCols = ALL_CSV_COLUMNS.filter { prefs.csvSelectedColumns.contains(it) }
        val columnsToUse = if (selectedCols.isNotEmpty()) selectedCols else ALL_CSV_COLUMNS

        return buildString {
            append('\uFEFF') // BOM: 엑셀 한글 깨짐 방지
            val headers = columnsToUse.toMutableList()
            if (includeImageColumn) headers.add("영수증 이미지 파일명")
            appendLine(headers.joinToString(","))

            receipts.forEachIndexed { index, r ->
                val rowValues = columnsToUse.map { col ->
                    val raw = getColumnValue(r, col, prefs)
                    if (raw.contains(",") || raw.contains("\"") || raw.contains("\n")) {
                        "\"" + raw.replace("\"", "\"\"") + "\""
                    } else {
                        raw
                    }
                }.toMutableList()

                if (includeImageColumn) {
                    val imgFileName = if (r.imagePath.isNotBlank()) {
                        val ext = File(r.imagePath).extension.ifBlank { "jpg" }
                        val localDate = r.extractLocalDate()
                        val cleanDate = String.format(Locale.KOREA, "%04d%02d%02d", localDate.year, localDate.monthValue, localDate.dayOfMonth)
                        "images/receipt_${index + 1}_${cleanDate}.$ext"
                    } else ""
                    rowValues.add("\"$imgFileName\"")
                }

                appendLine(rowValues.joinToString(","))
            }
        }
    }

    private fun getColumnValue(r: ReceiptEntity, colName: String, prefs: UserPreferences): String {
        return when (colName) {
            "결제일시" -> formatCsvDate(r, prefs.csvDateFormat)
            "가맹점명" -> r.merchantName
            "결제금액" -> formatCsvAmount(r.totalAmount, prefs.csvAmountFormat)
            "공급가액" -> {
                val vat = r.vatAmount ?: 0.0
                formatCsvAmount(r.totalAmount - vat, prefs.csvAmountFormat)
            }
            "부가세" -> formatCsvAmount(r.vatAmount ?: 0.0, prefs.csvAmountFormat)
            "카테고리" -> r.category
            "결제수단" -> r.paymentMethod
            "사업자번호" -> r.businessNumber ?: ""
            "승인번호" -> ""
            "통화" -> r.currency.ifBlank { "KRW" }
            "메모" -> r.memo ?: ""
            else -> ""
        }
    }

    private fun formatCsvDate(r: ReceiptEntity, formatPattern: String): String {
        val localDate = r.extractLocalDate()
        val yyyy = String.format(Locale.KOREA, "%04d", localDate.year)
        val mm = String.format(Locale.KOREA, "%02d", localDate.monthValue)
        val dd = String.format(Locale.KOREA, "%02d", localDate.dayOfMonth)
        val yy = yyyy.takeLast(2)

        return when (formatPattern) {
            "YYYY. MM. DD" -> "$yyyy. $mm. $dd"
            "YY/MM/DD" -> "$yy/$mm/$dd"
            "YYYY년 MM월 DD일" -> "${yyyy}년 ${mm}월 ${dd}일"
            else -> "$yyyy-$mm-$dd"
        }
    }

    private fun formatCsvAmount(amount: Double, formatType: String): String {
        val longVal = amount.toLong()
        return when (formatType) {
            "RAW_NUMBER" -> longVal.toString()
            "CURRENCY_TEXT" -> String.format(Locale.KOREA, "%,d원", longVal)
            else -> String.format(Locale.KOREA, "%,d", longVal)
        }
    }

    // ── 홈 화면 [더보기] 퀵 액션 전용 메서드들 ───────────────────────

    fun exportMonthlyCsvQuick(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            try {
                val today = LocalDate.now()
                val start = today.withDayOfMonth(1)
                val startMs = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endMs = today.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                val receipts = withContext(Dispatchers.IO) {
                    getSortedReceipts(startMs, endMs)
                }

                if (receipts.isEmpty()) {
                    com.pasic.receipt.util.ToastEventBus.showToast("이번 달 등록된 영수증이 없습니다.")
                    return@launch
                }

                val now = YearMonth.now()
                val fileName = "${now.year}년_${now.monthValue}월_지출보고서.csv"
                val csvContent = buildCsvString(receipts, includeImageColumn = false)
                val bytes = csvContent.toByteArray(Charsets.UTF_8)

                saveToDownloads(context, fileName, "text/csv", bytes)

                // 임시 파일 생성 (공유 다이얼로그용)
                val dir = File(context.filesDir, "exports").apply { mkdirs() }
                val tempFile = File(dir, fileName).also { it.writeBytes(bytes) }

                notificationRepository.addExportLog(
                    fileName = fileName,
                    receiptCount = receipts.size,
                    format = "CSV",
                    filePath = tempFile.absolutePath
                )

                _uiState.update {
                    it.copy(
                        showSaveSuccessDialog = true,
                        savedFileName = fileName,
                        previewPdfFile = tempFile
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "CSV 저장 중 오류가 발생했습니다: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    fun exportMonthlyPdfQuick(context: Context, author: String, dept: String, purpose: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            try {
                val today = LocalDate.now()
                val start = today.withDayOfMonth(1)
                val startMs = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endMs = today.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                val receipts = withContext(Dispatchers.IO) {
                    getSortedReceipts(startMs, endMs)
                }

                if (receipts.isEmpty()) {
                    com.pasic.receipt.util.ToastEventBus.showToast("이번 달 등록된 영수증이 없습니다.")
                    return@launch
                }

                val now = YearMonth.now()
                val periodLabel = "${now.year}년 ${now.monthValue}월"
                val pdfFile = PdfReportGenerator.generate(
                    context, receipts, periodLabel, author, dept, purpose
                )

                _uiState.update {
                    it.copy(
                        showPdfPreviewDialog = true,
                        previewPdfFile = pdfFile
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "PDF 생성 중 오류가 발생했습니다: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    fun exportAllReceiptsZipQuick(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            try {
                val receipts = withContext(Dispatchers.IO) {
                    repository.getAllReceipts().first()
                }

                if (receipts.isEmpty()) {
                    com.pasic.receipt.util.ToastEventBus.showToast("보관할 영수증 내역이 없습니다.")
                    return@launch
                }

                val fileName = "전체_영수증_증빙_보관.zip"
                val dir = File(context.filesDir, "exports").apply { mkdirs() }

                val zipFile = withContext(Dispatchers.IO) {
                    val baos = ByteArrayOutputStream()
                    ZipOutputStream(baos).use { zos ->
                        val csvContent = buildCsvString(receipts, includeImageColumn = true)
                        zos.putNextEntry(ZipEntry("전체_영수증_내역장부.csv"))
                        zos.write(csvContent.toByteArray(Charsets.UTF_8))
                        zos.closeEntry()

                        receipts.forEachIndexed { index, receipt ->
                            receipt.imagePath.takeIf { it.isNotBlank() }?.let { imgPath ->
                                val imgFile = File(imgPath)
                                if (imgFile.exists()) {
                                    val ext = imgFile.extension.ifEmpty { "jpg" }
                                    val cleanDate = receipt.date.replace(Regex("[^0-9]"), "")
                                    val entryName = "images/receipt_${receipt.id}_${index + 1}_${cleanDate}.$ext"
                                    zos.putNextEntry(ZipEntry(entryName))
                                    imgFile.inputStream().use { it.copyTo(zos) }
                                    zos.closeEntry()
                                }
                            }
                        }
                    }
                    File(dir, fileName).also { it.writeBytes(baos.toByteArray()) }
                }

                val bytes = zipFile.readBytes()
                saveToDownloads(context, fileName, "application/zip", bytes)

                notificationRepository.addExportLog(
                    fileName = fileName,
                    receiptCount = receipts.size,
                    format = "ZIP",
                    filePath = zipFile.absolutePath
                )

                _uiState.update {
                    it.copy(
                        showSaveSuccessDialog = true,
                        savedFileName = fileName,
                        previewPdfFile = zipFile
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "ZIP 보관 파일 생성 중 오류가 발생했습니다: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }
}
