package com.pasic.receipt.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class OcrResult(
    val merchantName: String,
    val date: String,
    val totalAmount: Double,
    val currency: String = "KRW",
    val businessNumber: String = "",
    val confidenceScore: Int = 100,
    val category: String = "식비", // 대분류 (예: 식비, 교통비, 사무용품, 미분류)
    val categoryColor: String = "#FEF3C7",
    val subCategory: String = "", // 소분류 (예: 카페, 일반식당, 택시, 편의점 등)
    val suggestedNewCategory: String = "",
    val paymentMethod: String = "카드",
    val proofType: String = "일반영수증",
    val vatAmount: Double? = null,
    val imagePath: String = ""
)

object ReceiptOcrEngine {

    private val receiptSchema = Schema.obj(
        properties = mapOf(
            "merchantName"           to Schema.string(description = "영수증 발행 가게 상호명 (예: 스타벅스 강남점, GS25 역삼점 등). 모르면 빈 문자열"),
            "date"                   to Schema.string(description = "결제 일시. 반드시 'YYYY-MM-DD HH:mm' 형식 (예: 2026-08-18 15:30). 연도 생략 시 현재 연도 기준"),
            "totalAmount"            to Schema.double(description = "할인/포인트 적용 후 실제 카드 승인 또는 현금 지불된 최종 실결제 금액 (숫자만)"),
            "businessNumber"         to Schema.string(description = "사업자등록번호 'XXX-XX-XXXXX' 형식 (없으면 빈 문자열)"),
            "category"               to Schema.string(description = "기본값은 '미분류'. 실제 구매 품목이 음식/음료이면 '식비', 이동 수단 비용이면 '교통비', 업무용 문구/소모품이면 '사무용품'으로만 변경. 편의점 담배·주류·생활용품은 '미분류'"),
            "subCategory"            to Schema.string(description = "구체적인 업종/품목 소분류 단어 1개 (예: 편의점, 약국, 카페, 식당, 택시, 주유소, 병원 등)"),
            "suggestedNewCategory"   to Schema.string(description = "category가 '미분류'인 경우, 식비/교통비/사무용품과 같은 수준의 넓은 대분류 카테고리명 1개 제안 (예: 생활용품, 의료비, 문화생활, 쇼핑, 주거비, 통신비 등). 미분류가 아니면 빈 문자열"),
            "paymentMethod"          to Schema.string(description = "결제 수단 (예: 카드, 현금, 간편결제 중 하나)"),
            "proofType"              to Schema.string(description = "증빙 유형 (예: 일반영수증, 현금영수증, 세금계산서 중 하나)"),
            "vatAmount"              to Schema.double(description = "영수증에 적힌 부가가치세 금액 (숫자만. 없으면 0.0)"),
            "confidence"             to Schema.integer(description = "0~100 사이 인식 신뢰도 점수")
        ),
        optionalProperties = listOf("businessNumber", "category", "subCategory", "suggestedNewCategory", "paymentMethod", "proofType", "vatAmount")
    )

    private val generativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.1-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = receiptSchema
                temperature = 0.0f  // 카테고리 분류 일관성을 위해 결정론적 출력 강제
            }
        )
    }

    suspend fun processImage(
        context: Context,
        imageUri: Uri?,
        imagePath: String,
        existingCategories: List<String> = listOf("식비", "교통비", "사무용품", "미분류")
    ): OcrResult {
        if (imageUri == null) return generateUnrecognizedOcrResult(imagePath)
        return try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, imageUri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
            }
            processBitmap(bitmap, imagePath, existingCategories)
        } catch (e: Exception) {
            Log.e("ReceiptOcrEngine", "processImage error: ${e.message}", e)
            generateUnrecognizedOcrResult(imagePath)
        }
    }

    suspend fun processBitmap(
        bitmap: Bitmap,
        imagePath: String,
        existingCategories: List<String> = listOf("식비", "교통비", "사무용품", "미분류")
    ): OcrResult {
        return try {
            val catListStr = existingCategories.joinToString(", ")
            val promptText = """
                영수증 이미지를 분석하여 아래 JSON 형식으로 정보를 추출하세요.

                [카테고리 분류 방법 - 가장 중요]
                등록된 카테고리: [$catListStr]

                분류는 '상호명'이 아닌 반드시 '실제 구매된 품목(Items)'을 기준으로 판단합니다.
                - 기본값은 항상 "미분류"입니다.
                - 아래 조건을 100% 만족할 때만 해당 카테고리로 변경합니다:
                  · 식비로 변경 → 구매 품목 대부분이 "직접 먹거나 마시는 음식/음료"일 때만. 담배·주류·화장품·생활용품·위생용품은 식비 아님.
                  · 교통비로 변경 → 대중교통 승차권, 주유, 렌터카 등 이동 수단 비용일 때만.
                  · 사무용품으로 변경 → 업무용 문구, 인쇄, 소모품 구매일 때만.
                  · 위 조건 중 하나라도 불확실하면 "미분류" 유지.

                규칙:
                1. merchantName: 영수증 발행 가게 상호명 (없으면 빈 문자열)
                2. date: 결제 일시 ('YYYY-MM-DD HH:mm' 형식으로 정규화, 연도 생략 시 현재 연도 기준)
                3. totalAmount: 할인/쿠폰/포인트 사용 후 최종 실결제 금액 (숫자만)
                4. businessNumber: 사업자등록번호 "XXX-XX-XXXXX" (없으면 빈 문자열)
                5. category: 위 [카테고리 분류 방법]에 따라 결정. 기본값은 "미분류".
                6. subCategory: 업종/품목 소분류 단어 1개 (예: 편의점, 카페, 약국, 택시, 마트 등)
                7. suggestedNewCategory: category가 "미분류"인 경우, 구매 품목의 실제 지출 성격에 맞는 새로운 대분류 카테고리명을 1개 제안하세요. 반드시 식비/교통비/사무용품과 같은 수준의 넓은 대분류여야 합니다 (예: 생활용품, 의료비, 문화생활, 쇼핑, 주거비, 통신비, 외식 등). category가 미분류가 아니고 완벽히 일치하면 빈 문자열("").
                8. paymentMethod: "카드", "현금", "간편결제" 중 하나 (불확실 시 "카드")
                9. proofType: "일반영수증", "현금영수증", "세금계산서" 중 하나 (불확실 시 "일반영수증")
                10. vatAmount: 영수증에 표기된 부가세 금액 (숫자만, 없으면 0.0)
                11. confidence: 인식 신뢰도 점수 (0~100 정수)
            """.trimIndent()

            val optimizedBitmap = scaleDownBitmap(bitmap, maxDimension = 1280)

            val inputContent = content {
                image(optimizedBitmap)
                text(promptText)
            }

            val response = generativeModel.generateContent(inputContent)
            val jsonText = response.text ?: ""
            parseGeminiJsonResponse(jsonText, imagePath, existingCategories)
        } catch (e: Exception) {
            safeLogE("ReceiptOcrEngine", "processBitmap error: ${e.message}", e)
            generateUnrecognizedOcrResult(imagePath)
        }
    }

    /**
     * 영수증 텍스트 가독성을 100% 보존하면서 전송 용량을 90% 줄여 속도를 극대화하는 스마트 리사이저
     */
    private fun scaleDownBitmap(bitmap: Bitmap, maxDimension: Int = 1280): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val maxSide = maxOf(width, height)
        if (maxSide <= maxDimension) return bitmap

        val scaleFactor = maxDimension.toFloat() / maxSide
        val targetWidth = (width * scaleFactor).toInt().coerceAtLeast(1)
        val targetHeight = (height * scaleFactor).toInt().coerceAtLeast(1)

        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun parseGeminiJsonResponse(
        jsonText: String,
        imagePath: String,
        existingCategories: List<String> = listOf("식비", "교통비", "사무용품", "미분류")
    ): OcrResult {
        if (jsonText.isBlank()) return generateUnrecognizedOcrResult(imagePath)

        return try {
            val cleanJson = jsonText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val startIdx = cleanJson.indexOf('{')
            val endIdx = cleanJson.lastIndexOf('}')
            if (startIdx == -1 || endIdx == -1 || endIdx < startIdx) {
                return generateUnrecognizedOcrResult(imagePath)
            }
            val jsonPayload = cleanJson.substring(startIdx, endIdx + 1)

            val merchantName = extractJsonField(jsonPayload, "merchantName")
            val rawDateStr = extractJsonField(jsonPayload, "date")
            val dateStr = normalizeDateString(rawDateStr)
            val totalAmount = extractJsonField(jsonPayload, "totalAmount").toDoubleOrNull() ?: 0.0
            val businessNumber = extractJsonField(jsonPayload, "businessNumber")
            val rawCategory = extractJsonField(jsonPayload, "category")
            var subCategory = extractJsonField(jsonPayload, "subCategory")
            val suggestedNewCategory = extractJsonField(jsonPayload, "suggestedNewCategory").replace("#", "").trim()
            val confidence = extractJsonField(jsonPayload, "confidence").toIntOrNull()?.coerceIn(30, 100) ?: 85

            val rawPaymentMethod = extractJsonField(jsonPayload, "paymentMethod").ifBlank { "카드" }
            val paymentMethod = when {
                rawPaymentMethod.contains("카드") -> "카드"
                rawPaymentMethod.contains("현금") -> "현금"
                rawPaymentMethod.contains("간편") || rawPaymentMethod.contains("페이") -> "간편결제"
                else -> "카드"
            }
            val proofType = extractJsonField(jsonPayload, "proofType").ifBlank { "일반영수증" }
            val extractedVat = extractJsonField(jsonPayload, "vatAmount").toDoubleOrNull()

            val vatAmount = if (extractedVat != null && extractedVat > 0.0) {
                extractedVat
            } else if (totalAmount > 0.0) {
                Math.round(totalAmount / 11.0).toDouble()
            } else {
                null
            }

            // 슬래시가 섞여 들어온 경우 분리 보정 (예: "식비/카페" -> 대분류: "식비", 소분류: "카페")
            val sanitizedCat = if (rawCategory.contains("/")) {
                val parts = rawCategory.split("/")
                if (subCategory.isBlank() && parts.size > 1) {
                    subCategory = parts[1].trim()
                }
                parts[0].trim()
            } else {
                rawCategory.trim()
            }

            val (finalCategory, categoryColor) = inferCategory(sanitizedCat, existingCategories)

            OcrResult(
                merchantName = merchantName,
                date = dateStr,
                totalAmount = totalAmount,
                currency = "KRW",
                businessNumber = businessNumber,
                confidenceScore = confidence,
                category = finalCategory,
                categoryColor = categoryColor,
                subCategory = subCategory,
                suggestedNewCategory = suggestedNewCategory,
                paymentMethod = paymentMethod,
                proofType = proofType,
                vatAmount = vatAmount,
                imagePath = imagePath
            )
        } catch (e: Exception) {
            safeLogE("ReceiptOcrEngine", "JSON parsing error: ${e.message}", e)
            generateUnrecognizedOcrResult(imagePath)
        }
    }

    private fun normalizeDateString(rawDate: String): String {
        if (rawDate.isBlank()) {
            val now = java.time.LocalDateTime.now()
            return String.format(Locale.KOREA, "%04d-%02d-%02d %02d:%02d", now.year, now.monthValue, now.dayOfMonth, now.hour, now.minute)
        }

        // 1. 이미 YYYY-MM-DD HH:mm 형태인 경우 빠른 반환
        val standardMatch = Regex("""^(\d{4})-(\d{2})-(\d{2})\s+(\d{2}):(\d{2})$""").find(rawDate.trim())
        if (standardMatch != null) {
            return rawDate.trim()
        }

        // 2. 날짜 파싱 (년/월/일)
        val cleaned = rawDate.split("·").firstOrNull()?.trim() ?: rawDate
        val currentYear = java.time.LocalDate.now().year
        var year = currentYear
        var month = 1
        var day = 1
        var dateFound = false

        val fullDateMatch = Regex("""(\d{4})[-.년\s/]+(\d{1,2})[-.월\s/]+(\d{1,2})""").find(cleaned)
        if (fullDateMatch != null) {
            year = fullDateMatch.groupValues[1].toInt()
            month = fullDateMatch.groupValues[2].toInt()
            day = fullDateMatch.groupValues[3].toInt()
            dateFound = true
        } else {
            val mdMatch = Regex("""(\d{1,2})[-.월\s/]+(\d{1,2})[일\s]*""").find(cleaned)
            if (mdMatch != null) {
                month = mdMatch.groupValues[1].toInt()
                day = mdMatch.groupValues[2].toInt()
                dateFound = true
            }
        }

        if (!dateFound) {
            return rawDate.trim()
        }

        // 3. 시간 파싱 (오전/오후/시/분)
        val timeRegex = Regex("""(오전|오후)?\s*(\d{1,2}):(\d{2})(?::(\d{2}))?""")
        val timeMatch = timeRegex.find(rawDate)
        var hour = 0
        var min = 0
        if (timeMatch != null) {
            val ampm = timeMatch.groupValues[1]
            hour = timeMatch.groupValues[2].toIntOrNull() ?: 0
            min = timeMatch.groupValues[3].toIntOrNull() ?: 0
            if (ampm == "오후" && hour in 1..11) {
                hour += 12
            } else if (ampm == "오전" && hour == 12) {
                hour = 0
            }
        } else {
            val now = java.time.LocalTime.now()
            hour = now.hour
            min = now.minute
        }

        return String.format(Locale.KOREA, "%04d-%02d-%02d %02d:%02d", year, month, day, hour, min)
    }

    private fun extractJsonField(json: String, key: String): String {
        val stringMatch = Regex("""["']$key["']\s*:\s*["']([^"']*)["']""").find(json)
        if (stringMatch != null) {
            return stringMatch.groupValues[1].trim()
        }
        val primitiveMatch = Regex("""["']$key["']\s*:\s*([0-9.]+)""").find(json)
        if (primitiveMatch != null) {
            return primitiveMatch.groupValues[1].trim()
        }
        return ""
    }

    private fun safeLogE(tag: String, msg: String, tr: Throwable? = null) {
        try {
            Log.e(tag, msg, tr)
        } catch (_: Throwable) {
            // Log.e 자체가 실패하는 극단적 상황 — 조용히 무시
        }
    }

    /**
     * AI가 판단한 카테고리를 덮어쓰지 않고 안전하게 색상 매칭 및 폴백만 수행
     */
    private fun inferCategory(
        suggestedCat: String,
        existingCategories: List<String> = listOf("식비", "교통비", "사무용품", "미분류")
    ): Pair<String, String> {
        val finalCategory = if (existingCategories.contains(suggestedCat)) {
            suggestedCat
        } else if (suggestedCat.isNotBlank() && suggestedCat != "미분류") {
            suggestedCat
        } else {
            "미분류"
        }

        val colorHex = getHexForCategory(finalCategory)
        return finalCategory to colorHex
    }

    private fun getHexForCategory(cat: String): String {
        return when (cat) {
            "식비" -> "#FEF3C7"     // 연주황
            "교통비" -> "#DBEAFE"   // 연파랑
            "사무용품" -> "#F3E8FF" // 연보라
            "생활비" -> "#DCFCE7"   // 연초록
            "의료비" -> "#FEE2E2"   // 연분홍
            else -> "#F1F5F9"       // 연회색
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
            subCategory = "",
            suggestedNewCategory = "",
            imagePath = imagePath
        )
    }

    fun generateSampleDemoResult(imagePath: String): OcrResult {
        val nowMonthDay = LocalDate.now().format(DateTimeFormatter.ofPattern("M월 d일"))
        val nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("a h:mm"))
        return OcrResult(
            merchantName = "(주) 카페단밤",
            date = "$nowMonthDay · $nowTime",
            totalAmount = 14500.0,
            currency = "KRW",
            businessNumber = "201-81-21515",
            confidenceScore = 95,
            category = "식비",
            categoryColor = "#FEF3C7",
            subCategory = "카페",
            suggestedNewCategory = "",
            imagePath = imagePath
        )
    }
}
