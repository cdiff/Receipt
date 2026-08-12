package com.pasic.receipt.ui.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasic.receipt.data.local.entity.ReceiptEntity
import com.pasic.receipt.data.repository.ReceiptRepository
import com.pasic.receipt.util.PdfReportGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    val csvHeaderColumns: List<String> = listOf("날짜", "상호명", "금액", "카테고리", "결제수단", "사업자번호", "부가세", "증빙유형", "메모"),
    val selectedFormat: ExportFormat = ExportFormat.EXCEL,
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
    private val repository: ReceiptRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    init {
        val today = LocalDate.now()
        val start = today.withDayOfMonth(1)
        _uiState.update { it.copy(formattedDateRangeText = formatDateRangeText(start, today)) }
        refreshTargetCount()
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

    private fun refreshTargetCount() {
        viewModelScope.launch {
            val (startMs, endMs) = getDateRangeMs()
            val receipts = repository.getReceiptsByDateRange(startMs, endMs)
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
                val receipts = repository.getReceiptsByDateRange(startMs, endMs)
                val includeImages = _uiState.value.includeImages
                val periodLabel = getPeriodLabel()

                val (fileName, mimeType, fileBytes) = withContext(Dispatchers.IO) {
                    if (includeImages) {
                        val zipName = "receipts_$periodLabel.zip"
                        val csvContent = buildCsvString(receipts, includeImageColumn = true)
                        val baos = ByteArrayOutputStream()
                        ZipOutputStream(baos).use { zos ->
                            val csvEntry = ZipEntry("receipts_$periodLabel.csv")
                            zos.putNextEntry(csvEntry)
                            zos.write(csvContent.toByteArray(Charsets.UTF_8))
                            zos.closeEntry()

                            receipts.forEachIndexed { index, r ->
                                if (r.imagePath.isNotBlank()) {
                                    val imageFile = File(r.imagePath)
                                    if (imageFile.exists() && imageFile.length() > 0) {
                                        val ext = imageFile.extension.ifBlank { "jpg" }
                                        val entryName = "images/receipt_${index + 1}_${r.date.replace(".", "")}.$ext"
                                        zos.putNextEntry(ZipEntry(entryName))
                                        imageFile.inputStream().use { input -> input.copyTo(zos) }
                                        zos.closeEntry()
                                    }
                                }
                            }
                        }
                        Triple(zipName, "application/zip", baos.toByteArray())
                    } else {
                        val csvName = "receipts_$periodLabel.csv"
                        val csvContent = buildCsvString(receipts, includeImageColumn = false)
                        Triple(csvName, "text/csv", csvContent.toByteArray(Charsets.UTF_8))
                    }
                }

                saveToDownloads(context, fileName, mimeType, fileBytes)

                val dir = File(context.filesDir, "exports").apply { mkdirs() }
                val localFile = File(dir, fileName).apply { writeBytes(fileBytes) }

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
                    repository.getReceiptsByDateRange(startMs, endMs)
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
                saveToDownloads(context, pdfFile.name, "application/pdf", pdfFile.readBytes())
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
                    repository.getReceiptsByDateRange(startMs, endMs)
                }
                val periodLabel = getPeriodLabel()
                val pdfFile = PdfReportGenerator.generate(context, receipts, periodLabel, author, dept, purpose)
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
            repository.getReceiptsByDateRange(startMs, endMs)
        }
        val periodLabel = getPeriodLabel()
        val dir = File(context.filesDir, "exports").apply { mkdirs() }

        return if (_uiState.value.includeImages) {
            val zipFile = withContext(Dispatchers.IO) {
                val baos = ByteArrayOutputStream()
                ZipOutputStream(baos).use { zos ->
                    val csvContent = buildCsvString(receipts, includeImageColumn = true)
                    zos.putNextEntry(ZipEntry("receipts_$periodLabel.csv"))
                    zos.write(csvContent.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()

                    receipts.forEachIndexed { index, receipt ->
                        receipt.imagePath?.let { imgPath ->
                            val imgFile = File(imgPath)
                            if (imgFile.exists()) {
                                val ext = imgFile.extension.ifEmpty { "jpg" }
                                val entryName = "images/receipt_${index + 1}_${receipt.date.replace("-", "")}.$ext"
                                zos.putNextEntry(ZipEntry(entryName))
                                imgFile.inputStream().use { it.copyTo(zos) }
                                zos.closeEntry()
                            }
                        }
                    }
                }
                File(dir, "receipts_$periodLabel.zip").also { it.writeBytes(baos.toByteArray()) }
            }
            Pair(zipFile, "application/zip")
        } else {
            val csvFile = withContext(Dispatchers.IO) {
                File(dir, "receipts_$periodLabel.csv").also {
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
        return if (state.customStartMs != null) {
            val date = LocalDate.ofEpochDay(state.customStartMs / 86400000)
            "${date.year}_${date.monthValue.toString().padStart(2, '0')}"
        } else {
            val now = YearMonth.now()
            "${now.year}_${now.monthValue.toString().padStart(2, '0')}"
        }
    }

    private fun buildCsvString(receipts: List<ReceiptEntity>, includeImageColumn: Boolean = false): String {
        return buildString {
            append('\uFEFF') // BOM: 한글 깨짐 방지
            val headers = uiState.value.csvHeaderColumns.toMutableList()
            if (includeImageColumn) headers.add("영수증 이미지 파일명")
            appendLine(headers.joinToString(","))

            receipts.forEachIndexed { index, r ->
                val memo = r.memo?.replace(",", " ") ?: ""
                val baseRow = "${r.date},\"${r.merchantName}\",${r.totalAmount.toLong()},${r.category},${r.paymentMethod},${r.businessNumber ?: ""},${r.vatAmount?.toLong() ?: ""},${r.proofType},\"$memo\""
                if (includeImageColumn) {
                    val imgFileName = if (r.imagePath.isNotBlank()) {
                        val ext = File(r.imagePath).extension.ifBlank { "jpg" }
                        "images/receipt_${index + 1}_${r.date.replace(".", "")}.$ext"
                    } else ""
                    appendLine("$baseRow,\"$imgFileName\"")
                } else {
                    appendLine(baseRow)
                }
            }
        }
    }
}
