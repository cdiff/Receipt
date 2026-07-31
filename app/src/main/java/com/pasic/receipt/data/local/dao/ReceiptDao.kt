package com.pasic.receipt.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pasic.receipt.data.local.entity.ReceiptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllReceipts(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE isDeleted = 0 ORDER BY createdAt DESC LIMIT 5")
    fun getRecentReceipts(): Flow<List<ReceiptEntity>>

    @Query("SELECT SUM(convertedAmountKrw) FROM receipts WHERE isDeleted = 0")
    fun getTotalSpendingKrw(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: ReceiptEntity): Long

    @Query("UPDATE receipts SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteReceipt(id: Long, deletedAt: Long = System.currentTimeMillis())
}
