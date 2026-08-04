package com.pasic.receipt.ui.scan

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs

data class OcrResult(
    val merchantName: String,
    val date: String,
    val totalAmount: Double,
    val currency: String = "KRW",
    val businessNumber: String = "",
    val confidenceScore: Int = 100,
    val category: String = "식비",
    val categoryColor: String = "#FEF3C7",
    val imagePath: String = ""
)

// 행 단위 클러스터링 결과
private data class ReceiptRow(
    val text: String,
    val yCenter: Int,
    val xStart: Int,
    val xEnd: Int,
    val avgConf: Float
)

object ReceiptOcrEngine {

    private val recognizer by lazy {
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    }

    suspend fun processImage(context: Context, imageUri: Uri?, imagePath: String): OcrResult {
        return if (imageUri != null) {
            try {
                val inputImage = InputImage.fromFilePath(context, imageUri)
                val text = analyzeText(inputImage)
                parseReceiptText(text, imagePath)
            } catch (e: Exception) {
                e.printStackTrace()
                generateUnrecognizedOcrResult(imagePath)
            }
        } else {
            generateUnrecognizedOcrResult(imagePath)
        }
    }

    suspend fun processBitmap(bitmap: Bitmap, imagePath: String): OcrResult {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val text = analyzeText(inputImage)
            parseReceiptText(text, imagePath)
        } catch (e: Exception) {
            e.printStackTrace()
            generateUnrecognizedOcrResult(imagePath)
        }
    }

    private suspend fun analyzeText(inputImage: InputImage): Text = suspendCoroutine { continuation ->
        recognizer.process(inputImage)
            .addOnSuccessListener { text ->
                continuation.resume(text)
            }
            .addOnFailureListener {
                continuation.resume(Text("", emptyList<Text.TextBlock>()))
            }
    }

    // ─────────────────────────────────────────────
    // 핵심: y좌표 클러스터링으로 행 묶음
    // ─────────────────────────────────────────────
    private fun buildRows(blocks: List<Text.TextBlock>): List<ReceiptRow> {
        // Element 단위로 평탄화
        val elements = blocks
            .flatMap { it.lines }
            .flatMap { it.elements }
            .filter { it.boundingBox != null }

        if (elements.isEmpty()) return emptyList()

        // y 오름차순 정렬
        val sorted = elements.sortedBy { it.boundingBox!!.centerY() }

        val rows = mutableListOf<ReceiptRow>()
        var currentGroup = mutableListOf(sorted[0])

        for (i in 1 until sorted.size) {
            val prev = currentGroup.last()
            val curr = sorted[i]
            val prevBox = prev.boundingBox!!
            val currBox = curr.boundingBox!!

            // 글자 높이 기반 상대 임계값 (고정 px 미사용)
            val threshold = prevBox.height() * 0.6f
            val yDiff = abs(currBox.centerY() - prevBox.centerY()).toFloat()

            if (yDiff <= threshold) {
                currentGroup.add(curr)
            } else {
                rows.add(currentGroup.toReceiptRow())
                currentGroup = mutableListOf(curr)
            }
        }
        if (currentGroup.isNotEmpty()) rows.add(currentGroup.toReceiptRow())

        return rows
    }

    private fun List<Text.Element>.toReceiptRow(): ReceiptRow {
        val sorted = this.sortedBy { it.boundingBox!!.left }
        val text = sorted.joinToString(" ") { it.text }
        val yCenter = sorted.map { it.boundingBox!!.centerY() }.average().toInt()
        val xStart = sorted.first().boundingBox!!.left
        val xEnd = sorted.last().boundingBox!!.right
        val avgConf = sorted.mapNotNull { it.confidence }.average().let {
            if (it.isNaN()) 0.8f else it.toFloat()
        }
        return ReceiptRow(text, yCenter, xStart, xEnd, avgConf)
    }

    // ─────────────────────────────────────────────
    // 금액 혼동 문자 보정 (금액 라인 전용)
    // ─────────────────────────────────────────────
    private fun fixAmountText(raw: String): String {
        return raw
            .replace(Regex("[^0-9,원\\-]"), "")
            .replace(Regex(",{2,}"), ",")
    }

    // ─────────────────────────────────────────────
    // 사업자번호 체크섬 검증 (국세청 표준 알고리즘)
    // ─────────────────────────────────────────────
    private fun isValidBrn(brn: String): Boolean {
        if (!brn.matches(Regex("\\d{3}-\\d{2}-\\d{5}"))) return false
        val map = intArrayOf(1, 3, 7, 1, 3, 7, 1, 3, 5)
        val d = brn.replace("-", "").map { it.digitToInt() }
        if (d.size != 10) return false
        var sum = 0
        for (i in 0..7) sum += d[i] * map[i]
        sum += (d[8] * map[8]) / 10
        val check = (10 - (sum % 10)) % 10
        return check == d[9]
    }

    // ─────────────────────────────────────────────
    // 항목합 ≈ 합계 교차검증
    // ─────────────────────────────────────────────
    private fun crossValidateTotal(itemAmounts: List<Int>, total: Int): Int {
        if (itemAmounts.isEmpty() || total <= 0) return 0
        val sum = itemAmounts.sum()
        val diff = abs(total - sum)
        return when {
            diff == 0                    -> 10   // 완전 일치 보너스
            diff <= itemAmounts.size * 2 -> 5    // 반올림/세금 허용 오차
            else                         -> -10  // 합계 라인 신뢰 불가
        }
    }

    // ─────────────────────────────────────────────
    // 메인 파싱 함수
    // ─────────────────────────────────────────────
    private fun parseReceiptText(mlText: Text, imagePath: String): OcrResult {
        val fullText = mlText.text
        if (fullText.isBlank()) {
            return generateUnrecognizedOcrResult(imagePath)
        }

        val textBlocks = mlText.textBlocks
        val rows = buildRows(textBlocks)

        // ML Kit Line confidence 평균 (OCR 품질 점수)
        val allLines = textBlocks.flatMap { it.lines }
        val ocrQuality = allLines.mapNotNull { it.confidence }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?: 0.75  // 신뢰도 반환 없을 때 기본값

        var itemScore = 0  // 최대 60점

        // 1. 상호명 (20점)
        val (merchantName, merchantFound) = extractMerchantName(textBlocks, rows)
        if (merchantFound) itemScore += 20

        // 2. 합계금액 (20점)
        val (amount, amountFound, itemAmounts) = extractTotalAmount(fullText, rows)
        if (amountFound) itemScore += 20

        // 3. 일시 (10점)
        val (dateStr, dateFound) = extractDate(fullText)
        if (dateFound) itemScore += 10

        // 4. 사업자번호 + 체크섬 검증 (10점)
        val (bizNo, bizFound) = extractBusinessNumber(fullText)
        if (bizFound) itemScore += 10

        // 항목합 ≈ 합계 교차검증 보정
        val crossBonus = crossValidateTotal(itemAmounts, amount.toInt())

        // 최종 정확도 = 항목 발견 점수(60%) + OCR 품질(40%) + 교차검증 보정
        val qualityScore = (ocrQuality * 40).toInt()
        val finalScore = (itemScore + qualityScore + crossBonus).coerceIn(30, 100)

        // 카테고리 자동 추천
        val (category, categoryColor) = inferCategory(merchantName)

        return OcrResult(
            merchantName = merchantName,
            date = dateStr,
            totalAmount = amount,
            currency = "KRW",
            businessNumber = bizNo,
            confidenceScore = finalScore,
            category = category,
            categoryColor = categoryColor,
            imagePath = imagePath
        )
    }

    // ─────────────────────────────────────────────
    // 상호명 추출 — y좌표 상위 20% Row 우선 탐색
    // ─────────────────────────────────────────────
    private fun extractMerchantName(
        textBlocks: List<Text.TextBlock>,
        rows: List<ReceiptRow>
    ): Pair<String, Boolean> {
        val koreanPattern = Regex("(주|카페|식당|마트|점|스토어|베이커리|푸드|치킨|피자|갈비|국밥)")
        val englishPattern = Regex(".*(STARBUCKS|GS25|CU|SEVEN|MCDONALD|BURGER|EDIYA|TWOSOME|OLIVE|COUPANG).*", RegexOption.IGNORE_CASE)
        val skipPattern = Regex("영수증|신용카드|매출전표|사업자|전화|TEL|FAX|주소")

        // 상위 20% 행에서 우선 탐색
        if (rows.isNotEmpty()) {
            val topCutoff = rows.minOf { it.yCenter } + (rows.maxOf { it.yCenter } - rows.minOf { it.yCenter }) * 0.2f
            val topRows = rows.filter { it.yCenter <= topCutoff }

            for (row in topRows) {
                val text = row.text.trim()
                if (skipPattern.containsMatchIn(text)) continue
                if (text.length in 2..25) {
                    val isVerified = koreanPattern.containsMatchIn(text) || englishPattern.matches(text)
                    return text to isVerified
                }
            }
        }

        // 폴백: 기존 Block 단위 로직
        for (block in textBlocks.take(4)) {
            val text = block.text.trim()
            if (skipPattern.containsMatchIn(text)) continue
            if (text.length in 2..25) {
                val isVerified = koreanPattern.containsMatchIn(text) || englishPattern.matches(text)
                return text.replace("\n", " ") to isVerified
            }
        }

        val firstBlockText = textBlocks.firstOrNull()?.text?.trim()?.replace("\n", " ") ?: ""
        return firstBlockText to false
    }

    // ─────────────────────────────────────────────
    // 합계금액 추출 — Row 기반 + 금액 열(xEnd) 추정 + 혼동 문자 보정
    // ─────────────────────────────────────────────
    private fun extractTotalAmount(
        fullText: String,
        rows: List<ReceiptRow>
    ): Triple<Double, Boolean, List<Int>> {
        val amountRegex = Regex("(합계|결제금액|받을금액|승인금액|총액|AMOUNT)\\s*[:=]?\\s*([0-9,]+)", RegexOption.IGNORE_CASE)
        val numberRowRegex = Regex("([0-9,]{3,10})\\s*원?$")
        val itemAmounts = mutableListOf<Int>()

        // 1순위: Row 리스트에서 합계 키워드 행 탐색
        if (rows.isNotEmpty()) {
            // 금액 열 추정: 숫자 Row의 xEnd 최빈값 ±30px
            val numericRows = rows.filter { numberRowRegex.containsMatchIn(it.text) }
            val xEndMode = numericRows.map { it.xEnd }
                .groupBy { it / 30 * 30 }  // 30px 단위 버킷팅
                .maxByOrNull { it.value.size }
                ?.value?.average()?.toInt() ?: Int.MAX_VALUE

            // 합계 키워드 행 탐색
            for (row in rows) {
                if (amountRegex.containsMatchIn(row.text)) {
                    val fixed = fixAmountText(row.text)
                    val match = Regex("([0-9]+)").findAll(fixed).lastOrNull()
                    val amount = match?.value?.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        // 항목 금액 수집 (합계 제외, 금액 열에 있는 행들)
                        numericRows.forEach { r ->
                            if (r != row && abs(r.xEnd - xEndMode) <= 30) {
                                val fixed2 = fixAmountText(r.text)
                                Regex("([0-9]+)").find(fixed2)?.value?.toIntOrNull()?.let { itemAmounts.add(it) }
                            }
                        }
                        return Triple(amount, true, itemAmounts)
                    }
                }
            }
        }

        // 2순위: fullText 전체 정규식 매칭 (기존 로직 폴백)
        val match = amountRegex.find(fullText)
        if (match != null) {
            val rawNum = match.groupValues[2].replace(",", "").toDoubleOrNull()
            if (rawNum != null && rawNum > 0) {
                return Triple(rawNum, true, itemAmounts)
            }
        }

        // 3순위: '원' 패턴
        val wonRegex = Regex("([0-9,]{3,10})\\s*원")
        val wonMatch = wonRegex.find(fullText)
        if (wonMatch != null) {
            val rawNum = wonMatch.groupValues[1].replace(",", "").toDoubleOrNull()
            if (rawNum != null && rawNum > 0) {
                return Triple(rawNum, true, itemAmounts)
            }
        }

        return Triple(0.0, false, itemAmounts)
    }

    // ─────────────────────────────────────────────
    // 날짜·시간 추출
    // ─────────────────────────────────────────────
    private fun extractDate(text: String): Pair<String, Boolean> {
        val dateTimeRegex = Regex("(\\d{2,4})[.-/](\\d{1,2})[.-/](\\d{1,2})\\s*(?:[일시:=]*)\\s*(?:(오전|오후)?\\s*(\\d{1,2}):(\\d{2})(?::\\d{2})?)?")
        val monthDayRegex = Regex("(\\d{1,2})월\\s*(\\d{1,2})일\\s*(?:[일시:=]*)\\s*(?:(오전|오후)?\\s*(\\d{1,2}):(\\d{2})(?::\\d{2})?)?")

        val match = dateTimeRegex.find(text) ?: monthDayRegex.find(text)
        if (match != null) {
            try {
                val groups = match.groupValues
                val month = groups[2].toIntOrNull() ?: 1
                val day = groups[3].toIntOrNull() ?: 1
                val amPm = groups.getOrNull(4)?.ifBlank { null }
                var hour = groups.getOrNull(5)?.toIntOrNull() ?: 12
                val minute = groups.getOrNull(6)?.toIntOrNull() ?: 0

                val periodStr = if (amPm != null) {
                    amPm
                } else if (hour >= 12) {
                    if (hour > 12) hour -= 12
                    "오후"
                } else {
                    if (hour == 0) hour = 12
                    "오전"
                }

                val formattedMinute = String.format("%02d", minute)
                return "${month}월 ${day}일 · $periodStr $hour:$formattedMinute" to true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val rawDateRegex = Regex("(\\d{1,2})월\\s*(\\d{1,2})일|(\\d{4})[.-/](\\d{1,2})[.-/](\\d{1,2})")
        val rawTimeRegex = Regex("(오전|오후)?\\s*(\\d{1,2}):(\\d{2})")

        val dateMatch = rawDateRegex.find(text)
        val timeMatch = rawTimeRegex.find(text)

        if (dateMatch != null) {
            val dateText = dateMatch.value
            val timeText = timeMatch?.value ?: ""
            val fullString = if (timeText.isNotBlank()) "$dateText · $timeText" else dateText
            return fullString to true
        }

        return "" to false
    }

    // ─────────────────────────────────────────────
    // 사업자번호 추출 + 체크섬 검증
    // ─────────────────────────────────────────────
    private fun extractBusinessNumber(text: String): Pair<String, Boolean> {
        val bizRegex = Regex("\\d{3}-\\d{2}-\\d{5}")
        val match = bizRegex.find(text)?.value ?: return "" to false

        // 체크섬 검증 실패 시 필드 비워둠
        return if (isValidBrn(match)) {
            match to true
        } else {
            "" to false
        }
    }

    // ─────────────────────────────────────────────
    // 카테고리 자동 추천
    // ─────────────────────────────────────────────
    private fun inferCategory(merchant: String): Pair<String, String> {
        return when {
            merchant.contains("카페") || merchant.contains("스타벅스") || merchant.contains("식당") ||
                    merchant.contains("푸드") || merchant.contains("버거") || merchant.contains("투썸") ||
                    merchant.contains("GS25") || merchant.contains("CU") || merchant.contains("식비") -> {
                "식비" to "#FEF3C7"
            }
            merchant.contains("택시") || merchant.contains("지하철") || merchant.contains("KTX") ||
                    merchant.contains("카카오 T") || merchant.contains("교통") || merchant.contains("주유") -> {
                "교통비" to "#DBEAFE"
            }
            merchant.contains("문구") || merchant.contains("서점") || merchant.contains("다이소") ||
                    merchant.contains("사무") || merchant.contains("교보문고") || merchant.contains("영풍문고") -> {
                "사무용품" to "#F3E8FF"
            }
            else -> {
                "미분류" to "#F1F5F9"
            }
        }
    }

    private fun generateUnrecognizedOcrResult(imagePath: String): OcrResult {
        val nowMonthDay = LocalDate.now().format(DateTimeFormatter.ofPattern("M월 d일"))
        val nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("a h:mm"))
        return OcrResult(
            merchantName = "",
            date = "$nowMonthDay · $nowTime",
            totalAmount = 0.0,
            currency = "KRW",
            businessNumber = "",
            confidenceScore = 30,
            category = "미분류",
            categoryColor = "#F1F5F9",
            imagePath = imagePath
        )
    }

    fun generateSampleDemoResult(imagePath: String): OcrResult {
        val nowMonthDay = LocalDate.now().format(DateTimeFormatter.ofPattern("M월 d일"))
        val nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("a h:mm"))
        return OcrResult(
            merchantName = "(주) 스타벅스 코리아",
            date = "$nowMonthDay · $nowTime",
            totalAmount = 45500.0,
            currency = "KRW",
            businessNumber = "201-81-21515",
            confidenceScore = 98,
            category = "식비",
            categoryColor = "#FEF3C7",
            imagePath = imagePath
        )
    }
}
