package com.pasic.receipt.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

enum class AppThemeOption(val label: String) {
    SYSTEM("시스템 기본 설정"),
    LIGHT("라이트 모드"),
    DARK("다크 모드")
}

val ALL_CSV_COLUMNS = listOf("결제일시", "가맹점명", "결제금액", "공급가액", "부가세", "카테고리", "결제수단", "사업자번호", "승인번호", "통화", "메모")

data class UserPreferences(
    val defaultAuthor: String = "홍길동",
    val defaultDepartment: String = "개발팀",
    val defaultPurpose: String = "업무 경비",
    val defaultExportFormat: String = "PDF", // "PDF" or "EXCEL" (CSV)
    val csvSelectedColumns: Set<String> = ALL_CSV_COLUMNS.toSet(),
    val csvDateFormat: String = "YYYY-MM-DD", // "YYYY-MM-DD", "YYYY.MM.DD", "YYYYMMDD"
    val csvAmountFormat: String = "CURRENCY_TEXT", // "RAW_NUMBER", "CURRENCY_TEXT"
    val zipImageNamingRule: String = "{date}_{merchant}_{index}",
    val autoOptimizeEnabled: Boolean = true,
    val autoCropEnabled: Boolean = false,
    val bwEnhancementEnabled: Boolean = false,
    val aiCategoryEnabled: Boolean = true,
    val scanReminderPushEnabled: Boolean = true,
    val expenseDDayPushEnabled: Boolean = true,
    val backupReminderPushEnabled: Boolean = true,
    val appTheme: AppThemeOption = AppThemeOption.SYSTEM
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object PreferencesKeys {
        val DEFAULT_AUTHOR = stringPreferencesKey("default_author")
        val DEFAULT_DEPARTMENT = stringPreferencesKey("default_department")
        val DEFAULT_PURPOSE = stringPreferencesKey("default_purpose")
        val DEFAULT_EXPORT_FORMAT = stringPreferencesKey("default_export_format")

        val CSV_SELECTED_COLUMNS = stringSetPreferencesKey("csv_selected_columns")
        val CSV_DATE_FORMAT = stringPreferencesKey("csv_date_format")
        val CSV_AMOUNT_FORMAT = stringPreferencesKey("csv_amount_format")
        val ZIP_IMAGE_NAMING_RULE = stringPreferencesKey("zip_image_naming_rule")

        val AUTO_OPTIMIZE = booleanPreferencesKey("auto_optimize")
        val AUTO_CROP = booleanPreferencesKey("auto_crop")
        val BW_ENHANCEMENT = booleanPreferencesKey("bw_enhancement")
        val AI_CATEGORY = booleanPreferencesKey("ai_category")

        val SCAN_REMINDER_PUSH = booleanPreferencesKey("scan_reminder_push")
        val EXPENSE_DDAY_PUSH = booleanPreferencesKey("expense_dday_push")
        val BACKUP_REMINDER_PUSH = booleanPreferencesKey("backup_reminder_push")

        val APP_THEME = stringPreferencesKey("app_theme")
    }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeStr = preferences[PreferencesKeys.APP_THEME] ?: AppThemeOption.SYSTEM.name
            val themeOption = try {
                AppThemeOption.valueOf(themeStr)
            } catch (_: Exception) {
                AppThemeOption.SYSTEM
            }

            UserPreferences(
                defaultAuthor = preferences[PreferencesKeys.DEFAULT_AUTHOR] ?: "홍길동",
                defaultDepartment = preferences[PreferencesKeys.DEFAULT_DEPARTMENT] ?: "개발팀",
                defaultPurpose = preferences[PreferencesKeys.DEFAULT_PURPOSE] ?: "업무 경비",
                defaultExportFormat = preferences[PreferencesKeys.DEFAULT_EXPORT_FORMAT] ?: "PDF",
                csvSelectedColumns = preferences[PreferencesKeys.CSV_SELECTED_COLUMNS] ?: ALL_CSV_COLUMNS.toSet(),
                csvDateFormat = preferences[PreferencesKeys.CSV_DATE_FORMAT] ?: "YYYY-MM-DD",
                csvAmountFormat = preferences[PreferencesKeys.CSV_AMOUNT_FORMAT] ?: "CURRENCY_TEXT",
                zipImageNamingRule = preferences[PreferencesKeys.ZIP_IMAGE_NAMING_RULE] ?: "{date}_{merchant}_{index}",
                autoOptimizeEnabled = preferences[PreferencesKeys.AUTO_OPTIMIZE] ?: true,
                autoCropEnabled = preferences[PreferencesKeys.AUTO_CROP] ?: false,
                bwEnhancementEnabled = preferences[PreferencesKeys.BW_ENHANCEMENT] ?: false,
                aiCategoryEnabled = preferences[PreferencesKeys.AI_CATEGORY] ?: true,
                scanReminderPushEnabled = preferences[PreferencesKeys.SCAN_REMINDER_PUSH] ?: true,
                expenseDDayPushEnabled = preferences[PreferencesKeys.EXPENSE_DDAY_PUSH] ?: true,
                backupReminderPushEnabled = preferences[PreferencesKeys.BACKUP_REMINDER_PUSH] ?: true,
                appTheme = themeOption
            )
        }

    suspend fun updateDefaultAuthor(author: String) {
        dataStore.edit { it[PreferencesKeys.DEFAULT_AUTHOR] = author }
    }

    suspend fun updateDefaultDepartment(dept: String) {
        dataStore.edit { it[PreferencesKeys.DEFAULT_DEPARTMENT] = dept }
    }

    suspend fun updateDefaultPurpose(purpose: String) {
        dataStore.edit { it[PreferencesKeys.DEFAULT_PURPOSE] = purpose }
    }

    suspend fun updateDefaultExportFormat(format: String) {
        dataStore.edit { it[PreferencesKeys.DEFAULT_EXPORT_FORMAT] = format }
    }

    suspend fun updateCsvSelectedColumns(columns: Set<String>) {
        dataStore.edit { it[PreferencesKeys.CSV_SELECTED_COLUMNS] = columns }
    }

    suspend fun updateCsvDateFormat(format: String) {
        dataStore.edit { it[PreferencesKeys.CSV_DATE_FORMAT] = format }
    }

    suspend fun updateCsvAmountFormat(format: String) {
        dataStore.edit { it[PreferencesKeys.CSV_AMOUNT_FORMAT] = format }
    }

    suspend fun updateZipImageNamingRule(rule: String) {
        dataStore.edit { it[PreferencesKeys.ZIP_IMAGE_NAMING_RULE] = rule }
    }

    suspend fun updateAutoOptimizeEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.AUTO_OPTIMIZE] = enabled }
    }

    suspend fun updateAutoCropEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.AUTO_CROP] = enabled }
    }

    suspend fun updateBwEnhancementEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.BW_ENHANCEMENT] = enabled }
    }

    suspend fun updateAiCategoryEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.AI_CATEGORY] = enabled }
    }

    suspend fun updateScanReminderPushEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.SCAN_REMINDER_PUSH] = enabled }
    }

    suspend fun updateExpenseDDayPushEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.EXPENSE_DDAY_PUSH] = enabled }
    }

    suspend fun updateBackupReminderPushEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.BACKUP_REMINDER_PUSH] = enabled }
    }

    suspend fun updateAppTheme(theme: AppThemeOption) {
        dataStore.edit { it[PreferencesKeys.APP_THEME] = theme.name }
    }
}
