package com.pasic.receipt.data.repository

import com.pasic.receipt.data.local.dao.CategoryDao
import com.pasic.receipt.data.local.dao.ReceiptDao
import com.pasic.receipt.data.local.entity.CategoryEntity
import com.pasic.receipt.data.local.entity.ReceiptEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptRepository @Inject constructor(
    private val receiptDao: ReceiptDao,
    private val categoryDao: CategoryDao
) {
    fun getAllReceipts(): Flow<List<ReceiptEntity>> = receiptDao.getAllReceipts()

    fun getRecentReceipts(): Flow<List<ReceiptEntity>> = receiptDao.getRecentReceipts()

    fun getTotalSpendingKrw(): Flow<Double?> = receiptDao.getTotalSpendingKrw()

    fun getReceiptById(id: Long): Flow<ReceiptEntity?> = receiptDao.getReceiptById(id)

    suspend fun insertReceipt(receipt: ReceiptEntity): Long {
        if (receipt.category.isNotBlank()) {
            categoryDao.insertCategory(CategoryEntity(name = receipt.category.trim()))
        }
        return receiptDao.insertReceipt(receipt)
    }

    suspend fun softDeleteReceipt(id: Long) = receiptDao.softDeleteReceipt(id)

    suspend fun getReceiptsByDateRange(startMs: Long, endMs: Long): List<ReceiptEntity> =
        receiptDao.getReceiptsByDateRange(startMs, endMs)

    // ── 카테고리 마스터 DB 연동 ───────────────────────────────────────
    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    suspend fun insertCategory(name: String): Long {
        if (name.isBlank()) return -1L
        return categoryDao.insertCategory(CategoryEntity(name = name.trim()))
    }

    suspend fun ensureDefaultCategories() {
        if (categoryDao.getCategoryCount() == 0) {
            categoryDao.insertCategories(
                listOf(
                    CategoryEntity(name = "식비", isDefault = true),
                    CategoryEntity(name = "교통비", isDefault = true),
                    CategoryEntity(name = "사무용품", isDefault = true),
                    CategoryEntity(name = "미분류", isDefault = true)
                )
            )
        }
    }
}
