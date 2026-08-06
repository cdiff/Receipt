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
    val category: String = "식비",
    val categoryColor: String = "#FEF3C7",
    val suggestedNewCategory: String = "",
    val imagePath: String = ""
)

object ReceiptOcrEngine {

    private val receiptSchema = Schema.obj(
        properties = mapOf(
            "merchantName"           to Schema.string(description = "가게 상호명 (예: (주)스타벅스코리아). 모르면 빈 문자열"),
            "date"                   to Schema.string(description = "결제 일시 (예: 8월 5일 · 오후 3:30 또는 2026-08-05 15:30)"),
            "totalAmount"            to Schema.double(description = "최종 결제 금액 (숫자만, 예: 15500.0)"),
            "businessNumber"         to Schema.string(description = "사업자등록번호 'XXX-XX-XXXXX' 형식 (모르면 빈 문자열)"),
            "category"               to Schema.string(description = "기존 카테고리 목록 중 가장 적합한 이름 (없으면 미분류)"),
            "suggestedNewCategory" to Schema.string(description = "기존 카테고리에 맞지 않는 경우 추천할 새 카테고리명. 적합하면 빈 문자열"),
            "confidence"             to Schema.integer(description = "0~100 사이 인식 신뢰도 점수")
        ),
        optionalProperties = listOf("businessNumber", "category", "suggestedNewCategory")
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
                현재 등록된 카테고리 목록: [$catListStr]

                규칙:
                1. merchantName: 상호명 (없으면 빈 문자열)
                2. date: 결제 일시 (영수증 날짜와 시간)
                3. totalAmount: 최종 결제 금액 (숫자만)
                4. businessNumber: 사업자등록번호 "XXX-XX-XXXXX" (없으면 빈 문자열)
                5. category: 현재 등록된 카테고리 목록 [$catListStr] 중에서 상호명/품목에 명확히 들어맞는 것이 있다면 해당 카테고리명을 선택하고, suggestedNewCategory는 빈 문자열로 하세요.
                6. suggestedNewCategory: 기존 카테고리 목록에 없거나 애매한 새로운 종류(예: 디저트, 의류, 뷰티, 취미, 의료비 등)라면 category를 "미분류"로 하고, suggestedNewCategory에 한국어 카테고리 단어 하나를 추천하세요. 적합한 기존 카테고리가 이미 있다면 빈 문자열("")로 하세요.
                7. confidence: 인식 신뢰도 점수 (0~100 정수)
            """.trimIndent()

            val inputContent = content {
                image(bitmap)
                text(promptText)
            }

            val response = generativeModel.generateContent(inputContent)
            val jsonText = response.text ?: ""
            parseGeminiJsonResponse(jsonText, imagePath)
        } catch (e: Exception) {
            safeLogE("ReceiptOcrEngine", "processBitmap error: ${e.message}", e)
            generateUnrecognizedOcrResult(imagePath)
        }
    }

    private fun parseGeminiJsonResponse(jsonText: String, imagePath: String): OcrResult {
        if (jsonText.isBlank()) return generateUnrecognizedOcrResult(imagePath)

        return try {
            val merchantName = extractJsonValue(jsonText, "merchantName")
            var dateStr = extractJsonValue(jsonText, "date")
            val totalAmount = extractJsonValue(jsonText, "totalAmount").toDoubleOrNull() ?: 0.0
            val businessNumber = extractJsonValue(jsonText, "businessNumber")
            val category = extractJsonValue(jsonText, "category").ifBlank { "식비" }
            val suggestedNewCategory = extractJsonValue(jsonText, "suggestedNewCategory").replace("#", "")
            val confidence = extractJsonValue(jsonText, "confidence").toIntOrNull()?.coerceIn(30, 100) ?: 85


            val (finalCategory, categoryColor) = inferCategory(merchantName, category)

            OcrResult(
                merchantName = merchantName,
                date = dateStr,
                totalAmount = totalAmount,
                currency = "KRW",
                businessNumber = businessNumber,
                confidenceScore = confidence,
                category = finalCategory,
                categoryColor = categoryColor,
                suggestedNewCategory = suggestedNewCategory,
                imagePath = imagePath
            )
        } catch (e: Exception) {
            safeLogE("ReceiptOcrEngine", "JSON parsing error: ${e.message}", e)
            generateUnrecognizedOcrResult(imagePath)
        }
    }

    private fun extractJsonValue(json: String, key: String): String {
        // String value matching "key": "value"
        val stringMatch = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"").find(json)
        if (stringMatch != null) {
            return stringMatch.groupValues[1].trim()
        }
        // Numeric/Primitive value matching "key": 123.45
        val numberMatch = Regex("\"$key\"\\s*:\\s*([0-9.]+)").find(json)
        if (numberMatch != null) {
            return numberMatch.groupValues[1].trim()
        }
        return ""
    }

    private fun safeLogD(tag: String, msg: String) {
        try {
            Log.d(tag, msg)
        } catch (t: Throwable) {
            println("[$tag] $msg")
        }
    }

    private fun safeLogI(tag: String, msg: String) {
        try {
            Log.i(tag, msg)
        } catch (t: Throwable) {
            println("[$tag] $msg")
        }
    }

    private fun safeLogE(tag: String, msg: String, tr: Throwable? = null) {
        try {
            Log.e(tag, msg, tr)
        } catch (t: Throwable) {
            println("[$tag] ERROR: $msg")
        }
    }

    private fun inferCategory(merchant: String, suggestedCat: String): Pair<String, String> {
        val cat = when {
            suggestedCat.isNotBlank() && suggestedCat != "미분류" -> suggestedCat
            merchant.contains("카페") || merchant.contains("스타벅스") || merchant.contains("식당") ||
                    merchant.contains("푸드") || merchant.contains("버거") || merchant.contains("투썸") ||
                    merchant.contains("GS25") || merchant.contains("CU") -> "식비"
            merchant.contains("택시") || merchant.contains("지하철") || merchant.contains("KTX") ||
                    merchant.contains("카카오 T") || merchant.contains("교통") -> "교통비"
            merchant.contains("문구") || merchant.contains("서점") || merchant.contains("다이소") ||
                    merchant.contains("사무") -> "사무용품"
            else -> "미분류"
        }

        val colorHex = when (cat) {
            "식비" -> "#FEF3C7"     // 연주황
            "교통비" -> "#DBEAFE"   // 연파랑
            "사무용품" -> "#F3E8FF" // 연보라
            else -> "#F1F5F9"       // 연회색
        }

        return cat to colorHex
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
            category = "미분류",
            categoryColor = "#F1F5F9",
            suggestedNewCategory = "디저트",
            imagePath = imagePath
        )
    }
}
