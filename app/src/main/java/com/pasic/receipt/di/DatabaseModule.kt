package com.pasic.receipt.di

import android.content.Context
import androidx.room.Room
import com.pasic.receipt.data.local.ReceiptDatabase
import com.pasic.receipt.data.local.dao.ReceiptDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideReceiptDatabase(
        @ApplicationContext context: Context
    ): ReceiptDatabase {
        return Room.databaseBuilder(
            context,
            ReceiptDatabase::class.java,
            "receipt_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideReceiptDao(database: ReceiptDatabase): ReceiptDao {
        return database.receiptDao()
    }
}
