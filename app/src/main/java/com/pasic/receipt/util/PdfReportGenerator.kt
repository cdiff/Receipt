package com.pasic.receipt.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.pasic.receipt.data.local.entity.ReceiptEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object PdfReportGenerator {

    // ── 페이지 규격 ────────────────────────────────────────────────
    private const val PAGE_WIDTH = 595f
    private const val PAGE_HEIGHT = 842f
    private const val MARGIN_LEFT = 40f
    private const val MARGIN_RIGHT = 40f
    private const val MARGIN_TOP = 30f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT   // 515pt

    // ── 행 높이 ────────────────────────────────────────────────────
    private const val ROW_HEIGHT = 28f          // 2줄 날짜 및 여유로운 패딩을 위한 28pt
    private const val HEADER_ROW_HEIGHT = 26f
    private const val INFO_ROW_HEIGHT = 24f
    private const val PURPOSE_ROW_HEIGHT = 34f
    private const val TARGET_PAGE1_DATA_ROWS = 15 // 1페이지 전용 고정 격자 행 수

    // ── 열 너비 (합계 = CONTENT_WIDTH = 515pt) ─────────────────────
    private val COL_WIDTHS = floatArrayOf(70f, 110f, 65f, 35f, 75f, 35f, 125f)
    private val COL_HEADERS = arrayOf("날  자", "거 래 처 명", "내  역", "부진", "지  출", "기타", "적  요")

    // ── Paint 스타일 (선명한 흑백 또렷한 선 & 폰트) ─────────────────
    private fun titlePaint() = Paint().apply {
        color = Color.BLACK
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private fun labelPaint() = Paint().apply {
        color = Color.BLACK
        textSize = 9.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private fun valuePaint() = Paint().apply {
        color = Color.BLACK
        textSize = 9.5f
        textAlign = Paint.Align.LEFT
    }

    private fun headerPaint() = Paint().apply {
        color = Color.BLACK
        textSize = 9.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private fun cellPaint() = Paint().apply {
        color = Color.parseColor("#222222")
        textSize = 9f
        textAlign = Paint.Align.CENTER
    }

    private fun cellSubTextPaint() = Paint().apply {
        color = Color.parseColor("#555555")
        textSize = 8f
        textAlign = Paint.Align.CENTER
    }

    private fun totalLabelPaint() = Paint().apply {
        color = Color.BLACK
        textSize = 9.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private fun totalValuePaint() = Paint().apply {
        color = Color.BLACK
        textSize = 9.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private fun linePaint() = Paint().apply {
        color = Color.parseColor("#333333")
        strokeWidth = 1.0f
        style = Paint.Style.STROKE
    }

    private fun headerBgPaint() = Paint().apply {
        color = Color.parseColor("#F5F5F5")
        style = Paint.Style.FILL
    }

    private fun statementPaint() = Paint().apply {
        color = Color.parseColor("#333333")
        textSize = 9.5f
        textAlign = Paint.Align.CENTER
    }

    // ── 공개 진입점 ────────────────────────────────────────────────

    suspend fun generate(
        context: Context,
        receipts: List<ReceiptEntity>,
        periodLabel: String,
        author: String,
        dept: String,
        purpose: String
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val pdfFile = File(dir, "${periodLabel}_지출결의서.pdf")

        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))
        val docNum = ""
        val total = receipts.sumOf { it.totalAmount }

        val pdfDocument = PdfDocument()
        var pageNumber = 1
        var (currentPage, canvas) = newPage(pdfDocument, pageNumber)

        // 1페이지: 제목 및 헤더 정보 작성
        var y = drawPageHeader(canvas, docNum, today, author, dept, purpose)
        y = drawTableHeader(canvas, y)

        val firstPageReceipts = receipts.take(TARGET_PAGE1_DATA_ROWS)
        val remainingReceipts = receipts.drop(TARGET_PAGE1_DATA_ROWS)

        // 1페이지 데이터 행 출력
        firstPageReceipts.forEach { receipt ->
            y = drawRow(canvas, receipt, formatter, y)
        }

        // 데이터가 모자란 경우 HTML처럼 빈 행(Empty Rows)을 채워 정갈한 고정 서식 유지
        val emptyRowsNeeded1 = TARGET_PAGE1_DATA_ROWS - firstPageReceipts.size
        for (i in 0 until emptyRowsNeeded1) {
            y = drawEmptyRow(canvas, y)
        }

        // 남아있는 영수증이 없는 경우 (1페이지 단독 완료): 마지막 합계 행 출력
        if (remainingReceipts.isEmpty()) {
            drawTotalRow(canvas, total, formatter, y)
            pdfDocument.finishPage(currentPage)
        } else {
            // 다중 페이지 처리: 1페이지에는 1페이지 분량의 합계 및 마감 표시 후 다음 페이지 이동
            drawTotalRow(canvas, total, formatter, y)
            pdfDocument.finishPage(currentPage)

            // 2페이지 이상 처리
            val rowsPerPage = 22
            val chunked = remainingReceipts.chunked(rowsPerPage)
            chunked.forEachIndexed { pageIdx, chunk ->
                pageNumber++
                val next = newPage(pdfDocument, pageNumber)
                val pCanvas = next.second

                y = drawSubsequentPageHeader(pCanvas)
                y = drawTableHeader(pCanvas, y)

                chunk.forEach { receipt ->
                    y = drawRow(pCanvas, receipt, formatter, y)
                }

                // 마지막 청크가 아닌 경우 빈 행으로 페이지 채우기
                val isLastChunk = (pageIdx == chunked.size - 1)
                val emptyCount = if (isLastChunk) (rowsPerPage - chunk.size) else (rowsPerPage - chunk.size)
                for (i in 0 until emptyCount) {
                    y = drawEmptyRow(pCanvas, y)
                }

                drawTotalRow(pCanvas, total, formatter, y)
                pdfDocument.finishPage(next.first)
            }
        }

        pdfFile.outputStream().use { pdfDocument.writeTo(it) }
        pdfDocument.close()

        pdfFile
    }

    private fun newPage(pdfDocument: PdfDocument, pageNumber: Int): Pair<PdfDocument.Page, Canvas> {
        val pageInfo = PdfDocument.PageInfo.Builder(
            PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), pageNumber
        ).create()
        val page = pdfDocument.startPage(pageInfo)
        return Pair(page, page.canvas)
    }

    // ── 1페이지 헤더 (제목 + 정보 테이블 + 안내 문구) ──────────────

    private fun drawPageHeader(
        canvas: Canvas,
        docNum: String,
        today: String,
        author: String,
        dept: String,
        purpose: String
    ): Float {
        var y = MARGIN_TOP

        // 제목
        y += 22f
        canvas.drawText("법인카드 지출 결의서", PAGE_WIDTH / 2f, y, titlePaint())
        y += 8f
        canvas.drawLine(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, y, linePaint())
        y += 14f

        // 정보 테이블 그리기
        y = drawInfoTable(canvas, y, docNum, today, author, dept, purpose)

        y += 12f

        // 안내 문구
        canvas.drawText(
            "아래와 같이 지출하고자 하오니 결재 후 자가 바랍니다.",
            PAGE_WIDTH / 2f, y, statementPaint()
        )
        y += 14f
        canvas.drawText("- 이 하 -", PAGE_WIDTH / 2f, y, statementPaint())
        y += 12f

        return y
    }

    // ── 정보 테이블 ───────────────────────────────────────────────

    private fun drawInfoTable(
        canvas: Canvas,
        startY: Float,
        docNum: String,
        today: String,
        author: String,
        dept: String,
        purpose: String
    ): Float {
        val lp = linePaint()
        val lb = labelPaint()
        val vp = valuePaint()

        val labelW = 75f                           // 레이블 너비 (75pt)
        val value1W = 155f                         // 첫 번째 값(문서번호): 155pt
        val value2W = CONTENT_WIDTH - labelW - value1W - labelW // 두 번째 값(작성일자): 210pt (더 넓음!)
        val x0 = MARGIN_LEFT
        var y = startY

        // 행 1: 문서번호 | EXP-XXX | 작성일자 | today
        drawInfoRow(canvas, x0, y, INFO_ROW_HEIGHT, lp, lb, vp,
            label1 = "문서번호", value1 = docNum,
            label2 = "작성일자", value2 = today,
            labelW = labelW, value1W = value1W, value2W = value2W)
        y += INFO_ROW_HEIGHT

        // 행 2: 결의부서 | dept | 작성자 | author
        drawInfoRow(canvas, x0, y, INFO_ROW_HEIGHT, lp, lb, vp,
            label1 = "결의부서", value1 = dept,
            label2 = "작성자", value2 = author,
            labelW = labelW, value1W = value1W, value2W = value2W)
        y += INFO_ROW_HEIGHT

        // 행 3: 보존기간 | "" | 확  조 | ""
        drawInfoRow(canvas, x0, y, INFO_ROW_HEIGHT, lp, lb, vp,
            label1 = "보존기간", value1 = "",
            label2 = "확  조", value2 = "",
            labelW = labelW, value1W = value1W, value2W = value2W)
        y += INFO_ROW_HEIGHT

        // 행 4: 적요 (전체 너비)
        val purposeTop = y
        val purposeBottom = y + PURPOSE_ROW_HEIGHT
        canvas.drawRect(RectF(x0, purposeTop, x0 + labelW, purposeBottom), headerBgPaint())
        canvas.drawRect(RectF(x0, purposeTop, x0 + CONTENT_WIDTH, purposeBottom), lp)
        canvas.drawLine(x0 + labelW, purposeTop, x0 + labelW, purposeBottom, lp)

        val textY = purposeTop + PURPOSE_ROW_HEIGHT / 2f + 4f
        canvas.drawText("적  요", x0 + labelW / 2f, textY, lb)
        canvas.drawText(
            if (purpose.length > 50) purpose.take(50) + "..." else purpose,
            x0 + labelW + 8f, textY, vp
        )
        y += PURPOSE_ROW_HEIGHT

        return y
    }

    private fun drawInfoRow(
        canvas: Canvas,
        x0: Float, y: Float, rowH: Float,
        lp: Paint, lb: Paint, vp: Paint,
        label1: String, value1: String,
        label2: String, value2: String,
        labelW: Float, value1W: Float, value2W: Float
    ) {
        val col1End = x0 + labelW
        val col2End = col1End + value1W
        val col3End = col2End + labelW
        val col4End = x0 + CONTENT_WIDTH

        // 배경 (레이블 셀 1 & 2)
        canvas.drawRect(RectF(x0, y, col1End, y + rowH), headerBgPaint())
        canvas.drawRect(RectF(col2End, y, col3End, y + rowH), headerBgPaint())

        // 테두리 및 수직 구분선
        canvas.drawRect(RectF(x0, y, col4End, y + rowH), lp)
        canvas.drawLine(col1End, y, col1End, y + rowH, lp)
        canvas.drawLine(col2End, y, col2End, y + rowH, lp)
        canvas.drawLine(col3End, y, col3End, y + rowH, lp)

        val textY = y + rowH / 2f + 3.5f
        canvas.drawText(label1, x0 + labelW / 2f, textY, lb)
        canvas.drawText(value1, col1End + 6f, textY, vp)
        canvas.drawText(label2, col2End + labelW / 2f, textY, lb)
        canvas.drawText(value2, col3End + 6f, textY, vp)
    }

    // ── 2페이지~ 헤더 ──────────────────────────────────────────────

    private fun drawSubsequentPageHeader(canvas: Canvas): Float {
        var y = MARGIN_TOP + 20f
        canvas.drawText("법인카드 지출 결의서", PAGE_WIDTH / 2f, y, titlePaint())
        y += 8f
        canvas.drawLine(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, y, linePaint())
        y += 14f
        return y
    }

    // ── 테이블 헤더 행 ─────────────────────────────────────────────

    private fun drawTableHeader(canvas: Canvas, y: Float): Float {
        val lp = linePaint()
        val hp = headerPaint()
        val bottom = y + HEADER_ROW_HEIGHT

        // 배경 및 테두리
        canvas.drawRect(RectF(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, bottom), headerBgPaint())
        canvas.drawRect(RectF(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, bottom), lp)

        var x = MARGIN_LEFT
        COL_WIDTHS.forEachIndexed { i, w ->
            val centerX = x + w / 2f
            val textY = y + HEADER_ROW_HEIGHT / 2f + 3.5f
            canvas.drawText(COL_HEADERS[i], centerX, textY, hp)
            if (i < COL_WIDTHS.size - 1) {
                canvas.drawLine(x + w, y, x + w, bottom, lp)
            }
            x += w
        }

        return bottom
    }

    // ── 영수증 데이터 행 (날짜 2줄 분동 & 정갈한 격자) ───────────────

    private fun drawRow(
        canvas: Canvas,
        receipt: ReceiptEntity,
        formatter: NumberFormat,
        y: Float
    ): Float {
        val lp = linePaint()
        val cp = cellPaint()
        val subCp = cellSubTextPaint()
        val bottom = y + ROW_HEIGHT

        // 셀 테두리
        canvas.drawRect(RectF(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, bottom), lp)

        // 날짜/시간 2줄 나누기
        val dateParts = receipt.date.split(" ")
        val mainDate = dateParts.getOrNull(0) ?: receipt.date
        val subTime = dateParts.getOrNull(1) ?: ""

        val merchantStr = truncate(receipt.merchantName, 12)
        val categoryStr = truncate(receipt.category, 8)
        val paymentStr = truncate(receipt.paymentMethod, 6)
        val amountStr = "${formatter.format(receipt.totalAmount.toLong())}원"
        val memoRaw = receipt.memo?.trim() ?: ""

        var x = MARGIN_LEFT
        COL_WIDTHS.forEachIndexed { i, w ->
            val centerX = x + w / 2f

            when (i) {
                0 -> { // [날자]: 2줄 분동 (날짜 / 시간)
                    if (subTime.isNotBlank()) {
                        canvas.drawText(mainDate, centerX, y + 11f, cp)
                        canvas.drawText(subTime, centerX, y + 22f, subCp)
                    } else {
                        canvas.drawText(mainDate, centerX, y + ROW_HEIGHT / 2f + 3.5f, cp)
                    }
                }
                1 -> canvas.drawText(merchantStr, centerX, y + ROW_HEIGHT / 2f + 3.5f, cp) // [거래처명]: 중간 정렬
                2 -> canvas.drawText(categoryStr, centerX, y + ROW_HEIGHT / 2f + 3.5f, cp) // [내역]: 중간 정렬
                3 -> canvas.drawText(paymentStr, centerX, y + ROW_HEIGHT / 2f + 3.5f, cp)  // [부진]: 중간 정렬
                4 -> canvas.drawText(amountStr, centerX, y + ROW_HEIGHT / 2f + 3.5f, cp)   // [지출]: '원' 부착 및 중간 정렬
                5 -> { /* [기타]: 공란 */ }
                6 -> { // [적요]: 1줄 또는 2줄 멀티라인 자동 줄바꿈
                    if (memoRaw.isNotBlank()) {
                        val availableWidth = w - 8f // 좌우 여백 4pt씩
                        if (cp.measureText(memoRaw) <= availableWidth) {
                            canvas.drawText(memoRaw, centerX, y + ROW_HEIGHT / 2f + 3.5f, cp)
                        } else {
                            val (line1, line2) = splitTextIntoTwoLines(memoRaw, subCp, availableWidth)
                            canvas.drawText(line1, centerX, y + 11f, subCp)
                            if (line2.isNotBlank()) {
                                canvas.drawText(line2, centerX, y + 22f, subCp)
                            }
                        }
                    }
                }
            }

            if (i < COL_WIDTHS.size - 1) {
                canvas.drawLine(x + w, y, x + w, bottom, lp)
            }
            x += w
        }

        return bottom
    }

    // ── 빈 행 채우기 (HTML 양식처럼 정갈한 고정 서식 유지) ─────────

    private fun drawEmptyRow(canvas: Canvas, y: Float): Float {
        val lp = linePaint()
        val bottom = y + ROW_HEIGHT

        canvas.drawRect(RectF(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, bottom), lp)

        var x = MARGIN_LEFT
        COL_WIDTHS.forEachIndexed { i, w ->
            if (i < COL_WIDTHS.size - 1) {
                canvas.drawLine(x + w, y, x + w, bottom, lp)
            }
            x += w
        }

        return bottom
    }

    // ── 합계 행 (마지막 페이지 하단) ──────────────────────────────

    private fun drawTotalRow(
        canvas: Canvas,
        total: Double,
        formatter: NumberFormat,
        y: Float
    ) {
        val lp = linePaint()
        val bottom = y + HEADER_ROW_HEIGHT

        // 배경 및 테두리
        canvas.drawRect(RectF(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, bottom), headerBgPaint())
        canvas.drawRect(RectF(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, bottom), lp)

        // "합  계" (1~4열 병합 영역)
        val mergedWidth = COL_WIDTHS[0] + COL_WIDTHS[1] + COL_WIDTHS[2] + COL_WIDTHS[3]
        val labelCenterX = MARGIN_LEFT + mergedWidth / 2f
        val textY = y + HEADER_ROW_HEIGHT / 2f + 3.5f
        canvas.drawText("합  계", labelCenterX, textY, totalLabelPaint())

        // 4열과 5열 사이 구분선
        canvas.drawLine(MARGIN_LEFT + mergedWidth, y, MARGIN_LEFT + mergedWidth, bottom, lp)

        // 금액 (5열)
        val amountCenterX = MARGIN_LEFT + mergedWidth + COL_WIDTHS[4] / 2f
        canvas.drawText(
            "${formatter.format(total.toLong())}원",
            amountCenterX, textY, totalValuePaint()
        )

        // 5열 이후 구분선
        var x = MARGIN_LEFT + mergedWidth + COL_WIDTHS[4]
        canvas.drawLine(x, y, x, bottom, lp)
        x += COL_WIDTHS[5]
        canvas.drawLine(x, y, x, bottom, lp)
    }

    // ── 유틸 ───────────────────────────────────────────────────────

    private fun truncate(text: String, maxLen: Int): String =
        if (text.length > maxLen) text.take(maxLen - 1) + "…" else text

    /**
     * 긴 텍스트를 정해진 너비에 맞추어 2줄로 자연스럽게 분할하는 유틸
     */
    private fun splitTextIntoTwoLines(text: String, paint: Paint, maxWidth: Float): Pair<String, String> {
        var splitIndex = 0
        for (i in 1..text.length) {
            val sub = text.substring(0, i)
            if (paint.measureText(sub) > maxWidth) {
                splitIndex = (i - 1).coerceAtLeast(1)
                break
            }
            splitIndex = i
        }
        val firstLine = text.substring(0, splitIndex)
        var secondLine = text.substring(splitIndex).trim()

        if (paint.measureText(secondLine) > maxWidth) {
            var secondSplitIndex = secondLine.length
            for (i in 1..secondLine.length) {
                if (paint.measureText(secondLine.substring(0, i) + "…") > maxWidth) {
                    secondSplitIndex = (i - 1).coerceAtLeast(1)
                    break
                }
            }
            secondLine = secondLine.substring(0, secondSplitIndex) + "…"
        }
        return Pair(firstLine, secondLine)
    }
}
