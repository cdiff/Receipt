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
    val paymentMethod: String = "신용카드",
    val proofType: String = "일반영수증",
    val vatAmount: Double? = null,
    val imagePath: String = ""
)

object ReceiptOcrEngine {

    private val receiptSchema = Schema.obj(
        properties = mapOf(
            "merchantName"           to Schema.string(description = "가게 상호명 (예: (주)스타벅스코리아). 모르면 빈 문자열"),
            "date"                   to Schema.string(description = "결제 일시 (예: 8월 5일 · 오후 3:30 또는 2026-08-05 15:30)"),
            "totalAmount"            to Schema.double(description = "최종 결제 금액 (숫자만, 예: 15500.0)"),
            "businessNumber"         to Schema.string(description = "사업자등록번호 'XXX-XX-XXXXX' 형식 (모르면 빈 문자열)"),
            "category"               to Schema.string(description = "기존 카테고리 목록 중 가장 적합한 대분류 이름 1개 (없으면 미분류)"),
            "subCategory"            to Schema.string(description = "세부 업종 및 품목 소분류 (예: 카페, 식당, 패스트푸드, 택시, 지하철, 문구, 편의점 등). 모르면 빈 문자열"),
            "suggestedNewCategory"   to Schema.string(description = "기존 카테고리에 맞지 않는 경우 추천할 새 카테고리명. 적합하면 빈 문자열"),
            "paymentMethod"          to Schema.string(description = "결제 수단 (예: 신용카드, 체크카드, 현금, 간편결제 중 하나)"),
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
                영수증 이미지를 분석하여 아래 규칙에 따라 정보를 추출하세요.
                현재 등록된 대분류 카테고리 목록: [$catListStr]

                규칙:
                1. merchantName: 상호명 (없으면 빈 문자열)
                2. date: 결제 일시 (영수증 날짜와 시간)
                3. totalAmount: 최종 결제 금액 (숫자만)
                4. businessNumber: 사업자등록번호 "XXX-XX-XXXXX" (없으면 빈 문자열)
                5. category: 상호명 및 세부 품목을 분석하여 현재 등록된 대분류 목록 [$catListStr] 중 가장 적합한 항목 1개(예: 식비, 교통비, 사무용품 등)를 정확히 일치시켜 선택하세요.
                6. subCategory: 영수증의 구체적인 세부 업종 및 품목 소분류 1개(예: 카페, 일반식당, 패스트푸드, 디저트, 택시, 지하철, 버스, 주유, 문구, 도서, 편의점, 마트, 병원, 약국 등)를 단어 하나로 작성하세요.
                7. suggestedNewCategory: 기존 대분류 목록에 전혀 맞지 않는 새로운 대분류일 때만 추천 단어 1개(예: 뷰티, 의료비, 취미 등)를 적고, 기존 목록에 해당하면 빈 문자열("")로 하세요.
                8. paymentMethod: 영수증 문구/카드종류를 분석하여 "신용카드", "체크카드", "현금", "간편결제" 중 하나 선택 (확실치 않으면 "신용카드")
                9. proofType: "일반영수증", "현금영수증", "세금계산서" 중 하나 선택 (확실치 않으면 "일반영수증")
                10. vatAmount: 영수증에 표기된 부가세/부가세액 금액 (숫자만. 표기 없으면 0.0)
                11. confidence: 인식 신뢰도 점수 (0~100 정수)
            """.trimIndent()

            val inputContent = content {
                image(bitmap)
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
            val dateStr = extractJsonField(jsonPayload, "date")
            val totalAmount = extractJsonField(jsonPayload, "totalAmount").toDoubleOrNull() ?: 0.0
            val businessNumber = extractJsonField(jsonPayload, "businessNumber")
            val rawCategory = extractJsonField(jsonPayload, "category")
            var subCategory = extractJsonField(jsonPayload, "subCategory")
            val suggestedNewCategory = extractJsonField(jsonPayload, "suggestedNewCategory").replace("#", "").trim()
            val confidence = extractJsonField(jsonPayload, "confidence").toIntOrNull()?.coerceIn(30, 100) ?: 85

            val paymentMethod = extractJsonField(jsonPayload, "paymentMethod").ifBlank { "신용카드" }
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

            val (finalCategory, categoryColor) = inferCategory(merchantName, sanitizedCat, existingCategories)

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
        } catch (t: Throwable) {
            println("[$tag] ERROR: $msg")
        }
    }

    private fun inferCategory(
        merchant: String,
        suggestedCat: String,
        existingCategories: List<String> = listOf("식비", "교통비", "사무용품", "미분류")
    ): Pair<String, String> {
        // 기존 목록에 정확히 일치하는 항목이 있으면 즉시 채택
        if (existingCategories.contains(suggestedCat)) {
            return suggestedCat to getHexForCategory(suggestedCat)
        }

        // 상호명 및 제안 키워드 기반 스마트 매핑
        val cat = when {
            merchant.contains("카페") || merchant.contains("스타벅스") || merchant.contains("식당") ||
                    merchant.contains("푸드") || merchant.contains("버거") || merchant.contains("투썸") ||
                    merchant.contains("커피") || merchant.contains("베이커리") ||
                    suggestedCat.contains("식") || suggestedCat.contains("카페") || suggestedCat.contains("음식") -> "식비"

            merchant.contains("택시") || merchant.contains("지하철") || merchant.contains("KTX") ||
                    merchant.contains("카카오 T") || merchant.contains("교통") || merchant.contains("주유") ||
                    suggestedCat.contains("교통") || suggestedCat.contains("운전") -> "교통비"

            merchant.contains("문구") || merchant.contains("서점") || merchant.contains("다이소") ||
                    merchant.contains("사무") || suggestedCat.contains("사무") || suggestedCat.contains("용품") -> "사무용품"

            suggestedCat.isNotBlank() && suggestedCat != "미분류" -> suggestedCat
            else -> "미분류"
        }

        val colorHex = getHexForCategory(cat)
        return cat to colorHex
    }

    private fun getHexForCategory(cat: String): String {
        return when (cat) {
            "식비" -> "#FEF3C7"     // 연주황
            "교통비" -> "#DBEAFE"   // 연파랑
            "사무용품" -> "#F3E8FF" // 연보라
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
