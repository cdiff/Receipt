package com.pasic.receipt.ui.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasic.receipt.data.local.entity.ReceiptEntity
import com.pasic.receipt.data.repository.ReceiptRepository
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
import java.text.NumberFormat
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
            _uiState.update { it.copy(showPdfInfoSheet = true) }
        }
        // Excel은 ExportScreen에서 직접 generateCsv() 호출
    }

    fun dismissPdfInfoSheet() = _uiState.update { it.copy(showPdfInfoSheet = false) }
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
                            resolver.openOutputStream(it)?.use { stream ->
                                stream.write(fileBytes)
                            }
                        }
                    } else {
                        val targetDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                        val targetFile = File(targetDir, fileName)
                        targetFile.writeBytes(fileBytes)
                    }
                }

                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        "📥 다운로드 폴더에 저장되었습니다: $fileName",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "파일 저장 중 오류가 발생했습니다: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    fun generatePdf(context: Context, author: String, dept: String, purpose: String) {
        _uiState.update { it.copy(showPdfInfoSheet = false, isGenerating = true) }
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val (startMs, endMs) = getDateRangeMs()
                val receipts = withContext(Dispatchers.IO) {
                    repository.getReceiptsByDateRange(startMs, endMs)
                }
                val htmlTemplate = withContext(Dispatchers.IO) {
                    context.assets.open("templates/expense_report.html").bufferedReader().readText()
                }
                val html = buildHtml(htmlTemplate, receipts, author, dept, purpose)
                renderPdfFromHtml(context, html)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isGenerating = false, errorMessage = "PDF 생성 중 오류가 발생했습니다: ${e.message}")
                }
            }
        }
    }

    private fun renderPdfFromHtml(context: Context, html: String) {
        val webView = WebView(context)
        webView.settings.javaScriptEnabled = false
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        webView.setWebViewClient(object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                val printAdapter = view.createPrintDocumentAdapter("법인카드 지출결의서")
                val printAttrs = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
                // 시스템 인쇄/저장 다이얼로그 오픈 (PDF 저장, 이메일, Drive 등 지원)
                printManager.print("법인카드 지출결의서_${getPeriodLabel()}", printAdapter, printAttrs)
                viewModelScope.launch(Dispatchers.Main) {
                    _uiState.update { it.copy(isGenerating = false) }
                }
            }
        })
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "파일 공유").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun shareViaEmail(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            try {
                val (startMs, endMs) = getDateRangeMs()
                val receipts = repository.getReceiptsByDateRange(startMs, endMs)
                val csv = buildCsvString(receipts)
                val file = withContext(Dispatchers.IO) {
                    val dir = File(context.filesDir, "exports").apply { mkdirs() }
                    File(dir, "receipts_${getPeriodLabel()}.csv").also { it.writeText(csv, Charsets.UTF_8) }
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "message/rfc822"
                    putExtra(Intent.EXTRA_SUBJECT, "[영수증 내보내기] ${getPeriodLabel()} 지출 내역")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "이메일 전송").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    fun shareViaMessenger(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            try {
                val (startMs, endMs) = getDateRangeMs()
                val receipts = repository.getReceiptsByDateRange(startMs, endMs)
                val csv = buildCsvString(receipts)
                val file = withContext(Dispatchers.IO) {
                    val dir = File(context.filesDir, "exports").apply { mkdirs() }
                    File(dir, "receipts_${getPeriodLabel()}.csv").also { it.writeText(csv, Charsets.UTF_8) }
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "메신저로 공유").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────────

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
            if (includeImageColumn) {
                headers.add("영수증 이미지 파일명")
            }
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

    private fun buildHtml(
        template: String,
        receipts: List<ReceiptEntity>,
        author: String,
        dept: String,
        purpose: String
    ): String {
        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))
        val totalAmount = receipts.sumOf { it.totalAmount }

        val rows = buildString {
            receipts.forEach { r ->
                appendLine("""
                    <tr>
                      <td>${r.date}</td>
                      <td class="left">${r.merchantName}</td>
                      <td class="left">${r.category}</td>
                      <td>&nbsp;</td>
                      <td class="right">${formatter.format(r.totalAmount.toLong())}원</td>
                      <td>&nbsp;</td>
                      <td class="left">${r.memo ?: ""}</td>
                    </tr>
                """.trimIndent())
            }
            // 빈 행 패딩 (최소 10줄)
            val emptyCount = maxOf(0, 10 - receipts.size)
            repeat(emptyCount) {
                appendLine("<tr class=\"empty-row\"><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>")
            }
        }

        return template
            .replace("{{DATE}}", today)
            .replace("{{AUTHOR}}", author.ifBlank { "&nbsp;" })
            .replace("{{DEPT}}", dept.ifBlank { "&nbsp;" })
            .replace("{{PURPOSE}}", purpose.ifBlank { "&nbsp;" })
            .replace("{{DOC_NUM}}", "&nbsp;")
            .replace("{{ROWS}}", rows)
            .replace("{{TOTAL}}", "${formatter.format(totalAmount.toLong())}원")
    }
}
