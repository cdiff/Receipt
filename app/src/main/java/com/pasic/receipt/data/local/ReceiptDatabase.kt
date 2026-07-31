package com.pasic.receipt.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pasic.receipt.data.local.dao.ReceiptDao
import com.pasic.receipt.data.local.entity.ReceiptEntity

@Database(entities = [ReceiptEntity::class], version = 1, exportSchema = false)
abstract class ReceiptDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao
}
