package com.pasic.receipt.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pasic.receipt.data.local.ReceiptDatabase
import com.pasic.receipt.data.local.dao.CategoryDao
import com.pasic.receipt.data.local.dao.ReceiptDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideReceiptDatabase(
        @ApplicationContext context: Context,
        databaseProvider: Provider<ReceiptDatabase>
    ): ReceiptDatabase {
        return Room.databaseBuilder(
            context,
            ReceiptDatabase::class.java,
            "receipt_db"
        )
            // 정식 Migration: 기존 receipts 데이터 보전하면서 categories 테이블만 신설 (v3 → v4)
            .addMigrations(MIGRATION_3_4)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // 앱 최초 설치(DB 생성) 시 기본 4대 카테고리 자동 삽입
                    insertDefaultCategories(db)
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    // Migration 3→4 후 categories가 비어있는 경우 기본값 보정
                    db.execSQL(
                        "INSERT OR IGNORE INTO categories (name, isDefault, createdAt) " +
                        "SELECT '식비', 1, ${System.currentTimeMillis()} " +
                        "WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = '식비')"
                    )
                    db.execSQL(
                        "INSERT OR IGNORE INTO categories (name, isDefault, createdAt) " +
                        "SELECT '교통비', 1, ${System.currentTimeMillis()} " +
                        "WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = '교통비')"
                    )
                    db.execSQL(
                        "INSERT OR IGNORE INTO categories (name, isDefault, createdAt) " +
                        "SELECT '사무용품', 1, ${System.currentTimeMillis()} " +
                        "WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = '사무용품')"
                    )
                    db.execSQL(
                        "INSERT OR IGNORE INTO categories (name, isDefault, createdAt) " +
                        "SELECT '미분류', 1, ${System.currentTimeMillis()} " +
                        "WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = '미분류')"
                    )
                }
            })
            .build()
    }

    private fun insertDefaultCategories(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        db.execSQL("INSERT OR IGNORE INTO categories (name, isDefault, createdAt) VALUES ('식비', 1, $now)")
        db.execSQL("INSERT OR IGNORE INTO categories (name, isDefault, createdAt) VALUES ('교통비', 1, $now)")
        db.execSQL("INSERT OR IGNORE INTO categories (name, isDefault, createdAt) VALUES ('사무용품', 1, $now)")
        db.execSQL("INSERT OR IGNORE INTO categories (name, isDefault, createdAt) VALUES ('미분류', 1, $now)")
    }

    @Provides
    fun provideReceiptDao(database: ReceiptDatabase): ReceiptDao {
        return database.receiptDao()
    }

    @Provides
    fun provideCategoryDao(database: ReceiptDatabase): CategoryDao {
        return database.categoryDao()
    }
}

/**
 * Migration 3 → 4
 * - `categories` 테이블 신설
 * - 기존 `receipts` 데이터 완전 보전 (파괴 없음)
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `categories` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `isDefault` INTEGER NOT NULL DEFAULT 0,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)"
        )
    }
}
