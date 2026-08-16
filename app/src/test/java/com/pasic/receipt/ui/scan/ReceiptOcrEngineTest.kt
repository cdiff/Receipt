package com.pasic.receipt.ui.scan

import com.pasic.receipt.ai.OcrResult
import com.pasic.receipt.ai.ReceiptOcrEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptOcrEngineTest {

    @Test
    fun `parseGeminiJsonResponse parses valid Gemini JSON correctly`() {
        val sampleJson = """
            {
                "merchantName": "(주)스타벅스코리아",
                "date": "8월 5일 · 오후 3:30",
                "totalAmount": 12500.0,
                "businessNumber": "120-86-12345",
                "category": "식비",
                "confidence": 98
            }
        """.trimIndent()

        val parseMethod = ReceiptOcrEngine::class.java.getDeclaredMethod(
            "parseGeminiJsonResponse",
            String::class.java,
            String::class.java
        )
        parseMethod.isAccessible = true

        val result = parseMethod.invoke(ReceiptOcrEngine, sampleJson, "/test/path.jpg") as OcrResult

        assertEquals("(주)스타벅스코리아", result.merchantName)
        assertEquals("8월 5일 · 오후 3:30", result.date)
        assertEquals(12500.0, result.totalAmount, 0.01)
        assertEquals("120-86-12345", result.businessNumber)
        assertEquals("식비", result.category)
        assertEquals(98, result.confidenceScore)
        assertEquals("/test/path.jpg", result.imagePath)
    }

    @Test
    fun `parseGeminiJsonResponse parses AI new category recommendation correctly`() {
        val sampleJson = """
            {
                "merchantName": "카페단밤",
                "date": "8월 5일 · 오후 4:00",
                "totalAmount": 14000.0,
                "businessNumber": "201-81-99999",
                "category": "미분류",
                "suggestedNewCategory": "디저트",
                "confidence": 94
            }
        """.trimIndent()

        val parseMethod = ReceiptOcrEngine::class.java.getDeclaredMethod(
            "parseGeminiJsonResponse",
            String::class.java,
            String::class.java
        )
        parseMethod.isAccessible = true

        val result = parseMethod.invoke(ReceiptOcrEngine, sampleJson, "/test/path.jpg") as OcrResult

        assertEquals("카페단밤", result.merchantName)
        assertEquals("디저트", result.suggestedNewCategory)
        assertEquals(94, result.confidenceScore)
    }

    @Test
    fun `parseGeminiJsonResponse falls back safely on invalid JSON without crashing`() {
        val invalidJson = "Invalid JSON response string from network error"

        val parseMethod = ReceiptOcrEngine::class.java.getDeclaredMethod(
            "parseGeminiJsonResponse",
            String::class.java,
            String::class.java
        )
        parseMethod.isAccessible = true

        val result = parseMethod.invoke(ReceiptOcrEngine, invalidJson, "/test/path.jpg") as OcrResult

        assertNotNull(result)
        assertEquals(30, result.confidenceScore)
        assertEquals("미분류", result.category)
    }
}
