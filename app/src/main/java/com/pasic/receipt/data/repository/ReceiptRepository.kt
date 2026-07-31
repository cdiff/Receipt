package com.pasic.receipt.data.repository

import com.pasic.receipt.data.local.dao.ReceiptDao
import com.pasic.receipt.data.local.entity.ReceiptEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptRepository @Inject constructor(
    private val receiptDao: ReceiptDao
) {
    fun getAllReceipts(): Flow<List<ReceiptEntity>> = receiptDao.getAllReceipts()

    fun getRecentReceipts(): Flow<List<ReceiptEntity>> = receiptDao.getRecentReceipts()

    fun getTotalSpendingKrw(): Flow<Double?> = receiptDao.getTotalSpendingKrw()

    suspend fun insertReceipt(receipt: ReceiptEntity): Long = receiptDao.insertReceipt(receipt)

    suspend fun softDeleteReceipt(id: Long) = receiptDao.softDeleteReceipt(id)
}
