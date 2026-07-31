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
