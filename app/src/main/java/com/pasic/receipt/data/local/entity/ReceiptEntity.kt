package com.pasic.receipt.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchantName: String,
    val date: String,
    val totalAmount: Double,
    val currency: String = "KRW",
    val exchangeRate: Double = 1.0,
    val convertedAmountKrw: Double = totalAmount,
    val businessNumber: String? = null,
    val vatAmount: Double? = null,
    val category: String = "식비",
    val categoryColor: String = "#FEF3C7", // 사용자 선택 커스텀 Hex 색상 코드 (기본값 연주황)
    val tags: String = "",
    val paymentMethod: String = "카드",
    val proofType: String = "일반영수증",
    val memo: String? = null,
    val imagePath: String = "",
    val ocrConfidence: Int? = null, // null means unmeasured/unparsed, 0~100 score
    val isPersonalOrCancelled: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

fun ReceiptEntity.extractLocalDate(): java.time.LocalDate {
    if (date.isNotBlank()) {
        val cleaned = date.split("·").firstOrNull()?.trim() ?: date
        val yearMatch = Regex("""(\d{4})[-.년\s/]+(\d{1,2})[-.월\s/]+(\d{1,2})""").find(cleaned)
        if (yearMatch != null) {
            val year = yearMatch.groupValues[1].toInt()
            val month = yearMatch.groupValues[2].toInt()
            val day = yearMatch.groupValues[3].toInt()
            return java.time.LocalDate.of(year, month, day)
        }
        val monthMatch = Regex("""(\d{1,2})[-.월\s/]+(\d{1,2})[일\s]*""").find(cleaned)
        if (monthMatch != null) {
            val month = monthMatch.groupValues[1].toInt()
            val day = monthMatch.groupValues[2].toInt()
            val year = if (createdAt > 1000000000000L) {
                java.time.Instant.ofEpochMilli(createdAt).atZone(java.time.ZoneId.systemDefault()).year
            } else java.time.LocalDate.now().year
            return java.time.LocalDate.of(year, month, day)
        }
    }

    if (createdAt > 1000000000000L) {
        runCatching {
            return java.time.Instant.ofEpochMilli(createdAt)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        }
    }
    return java.time.LocalDate.now()
}
