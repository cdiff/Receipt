package com.pasic.receipt.ui.scan

import com.pasic.receipt.ai.OcrResult
import com.pasic.receipt.ai.ReceiptOcrEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ReceiptOcrEngineTest {

    @Test
    fun `parseGeminiJsonResponse parses valid Gemini JSON and subCategory correctly`() {
        val sampleJson = """
            {
                "merchantName": "(주)스타벅스코리아 강남점",
                "date": "8월 5일 · 오후 3:30",
                "totalAmount": 12500.0,
                "businessNumber": "120-86-12345",
                "category": "식비",
                "subCategory": "카페",
                "confidence": 98
            }
        """.trimIndent()

        val parseMethod = ReceiptOcrEngine::class.java.getDeclaredMethod(
            "parseGeminiJsonResponse",
            String::class.java,
            String::class.java,
            List::class.java
        )
        parseMethod.isAccessible = true

        val result = parseMethod.invoke(
            ReceiptOcrEngine,
            sampleJson,
            "/test/path.jpg",
            listOf("식비", "교통비", "사무용품", "미분류")
        ) as OcrResult

        val expectedYear = java.time.LocalDate.now().year
        assertEquals("(주)스타벅스코리아 강남점", result.merchantName)
        assertEquals("$expectedYear-08-05 15:30", result.date)
        assertEquals(12500.0, result.totalAmount, 0.01)
        assertEquals("120-86-12345", result.businessNumber)
        assertEquals("식비", result.category)
        assertEquals("카페", result.subCategory)
        assertEquals(98, result.confidenceScore)
        assertEquals("/test/path.jpg", result.imagePath)
    }

    @Test
    fun `parseGeminiJsonResponse normalizes slash category into main and sub category`() {
        val sampleJson = """
            {
                "merchantName": "카카오택시",
                "date": "8월 6일 · 오후 11:30",
                "totalAmount": 18400.0,
                "category": "교통비/택시",
                "confidence": 95
            }
        """.trimIndent()

        val parseMethod = ReceiptOcrEngine::class.java.getDeclaredMethod(
            "parseGeminiJsonResponse",
            String::class.java,
            String::class.java,
            List::class.java
        )
        parseMethod.isAccessible = true

        val result = parseMethod.invoke(
            ReceiptOcrEngine,
            sampleJson,
            "/test/path.jpg",
            listOf("식비", "교통비", "사무용품", "미분류")
        ) as OcrResult

        val expectedYear = java.time.LocalDate.now().year
        assertEquals("교통비", result.category)
        assertEquals("택시", result.subCategory)
        assertEquals("$expectedYear-08-06 23:30", result.date)
    }

    @Test
    fun `parseGeminiJsonResponse falls back safely on invalid JSON without crashing`() {
        val invalidJson = "Invalid JSON response string from network error"

        val parseMethod = ReceiptOcrEngine::class.java.getDeclaredMethod(
            "parseGeminiJsonResponse",
            String::class.java,
            String::class.java,
            List::class.java
        )
        parseMethod.isAccessible = true

        val result = parseMethod.invoke(
            ReceiptOcrEngine,
            invalidJson,
            "/test/path.jpg",
            listOf("식비", "교통비", "사무용품", "미분류")
        ) as OcrResult

        assertNotNull(result)
        assertEquals(30, result.confidenceScore)
        assertEquals("미분류", result.category)
    }
}
