package com.pasic.receipt.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pasic.receipt.MainActivity
import com.pasic.receipt.R
import com.pasic.receipt.data.local.dao.ReceiptDao
import com.pasic.receipt.data.preferences.UserPreferencesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PushWorkerEntryPoint {
    fun receiptDao(): ReceiptDao
    fun userPreferencesRepository(): UserPreferencesRepository
}

class NotificationPushWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "channel_receipt_reminder"
        const val CHANNEL_NAME = "영수증 알림"

        const val NOTIFICATION_ID_SCAN = 1001
        const val NOTIFICATION_ID_EXPENSE = 1002
        const val NOTIFICATION_ID_BACKUP = 1003

        const val PREFS_NAME = "receipt_notification_prefs"
        const val KEY_LAST_SCAN_DATE = "last_scan_reminder_date"
        const val KEY_LAST_EXPENSE_DATE = "last_expense_dday_date"
        const val KEY_LAST_BACKUP_REMINDER_TIME = "last_backup_reminder_timestamp"
        const val KEY_LAST_BACKUP_EXPORT_TIME = "last_backup_export_timestamp"

        private const val FOURTEEN_DAYS_MS = 14L * 24 * 60 * 60 * 1000
        private const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "영수증 스캔 리마인더 및 경비 제출 D-Day 알림"
                    enableVibration(true)
                }
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager?.createNotificationChannel(channel)
            }
        }
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            PushWorkerEntryPoint::class.java
        )
        val receiptDao = entryPoint.receiptDao()
        val userPrefsRepo = entryPoint.userPreferencesRepository()

        val userPrefs = userPrefsRepo.userPreferencesFlow.first()
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE) // "YYYY-MM-DD"
        val currentHour = now.hour
        val currentTimeMs = System.currentTimeMillis()

        val allReceipts = receiptDao.getAllReceiptsList()
        val todayReceiptCount = allReceipts.count { r ->
            // 영수증 결제일 또는 등록일이 오늘인 건수
            r.date.replace(".", "-").startsWith(todayStr) ||
                    (r.createdAt >= today.atStartOfDay().toEpochSecond(java.time.ZoneOffset.ofHours(9)) * 1000)
        }

        // ─────────────────────────────────────────────────────────────
        // 1. 🌙 저녁 영수증 스캔 리마인더 (20:00 ~ 22:00, 오늘 영수증 0건)
        // ─────────────────────────────────────────────────────────────
        if (userPrefs.scanReminderPushEnabled && currentHour in 20..22) {
            val lastScanDate = prefs.getString(KEY_LAST_SCAN_DATE, "")
            if (lastScanDate != todayStr && todayReceiptCount == 0) {
                sendNotification(
                    notificationId = NOTIFICATION_ID_SCAN,
                    title = "영수증 등록할 시간이에요 📸",
                    message = "오늘 사용한 영수증을 잊기 전에 등록해 보세요!"
                )
                prefs.edit().putString(KEY_LAST_SCAN_DATE, todayStr).apply()
            }
        }

        // ─────────────────────────────────────────────────────────────
        // 2. 💼 월말 경비 제출 D-Day 알림 (09:00 ~ 12:00, 말일 3일 전 기간)
        // ─────────────────────────────────────────────────────────────
        if (userPrefs.expenseDDayPushEnabled && currentHour in 9..12) {
            val lastDayOfMonth = today.lengthOfMonth()
            val dayOfMonth = today.dayOfMonth
            val daysUntilMonthEnd = lastDayOfMonth - dayOfMonth

            if (daysUntilMonthEnd in 0..3) {
                val lastExpenseDate = prefs.getString(KEY_LAST_EXPENSE_DATE, "")
                if (lastExpenseDate != todayStr) {
                    val currentMonthStr = today.format(DateTimeFormatter.ofPattern("yyyy.MM"))
                    val thisMonthReceipts = allReceipts.filter { it.date.startsWith(currentMonthStr) }
                    val count = thisMonthReceipts.size
                    val totalAmount = thisMonthReceipts.sumOf { it.totalAmount }
                    val df = DecimalFormat("#,###")
                    val amountStr = "${df.format(totalAmount)}원"

                    val dDayText = if (daysUntilMonthEnd == 0) "오늘 마감" else "D-$daysUntilMonthEnd"
                    sendNotification(
                        notificationId = NOTIFICATION_ID_EXPENSE,
                        title = "월말 경비 제출 $dDayText 💼",
                        message = "이번 달 등록된 경비가 ${count}건($amountStr) 있습니다. 정산 전 미리 내보내 보세요!"
                    )
                    prefs.edit().putString(KEY_LAST_EXPENSE_DATE, todayStr).apply()
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // 3. 💾 정기 데이터 안전 백업 권장 (14:00 ~ 15:00, 50건 이상 AND 백업 30일 경과)
        // ─────────────────────────────────────────────────────────────
        if (userPrefs.backupReminderPushEnabled && currentHour in 14..15) {
            val totalCount = allReceipts.size
            val lastBackupExportTime = prefs.getLong(KEY_LAST_BACKUP_EXPORT_TIME, 0L)
            val isBackupOld = (currentTimeMs - lastBackupExportTime) >= THIRTY_DAYS_MS

            if (totalCount >= 50 && isBackupOld) {
                val lastBackupReminderTime = prefs.getLong(KEY_LAST_BACKUP_REMINDER_TIME, 0L)
                val isCooldownPassed = (currentTimeMs - lastBackupReminderTime) >= FOURTEEN_DAYS_MS

                if (isCooldownPassed) {
                    sendNotification(
                        notificationId = NOTIFICATION_ID_BACKUP,
                        title = "소중한 영수증 데이터를 안전하게 백업하세요 💾",
                        message = "현재 ${totalCount}건의 영수증이 누적되어 있습니다. ZIP 파일로 안전하게 보관해 보세요."
                    )
                    prefs.edit().putLong(KEY_LAST_BACKUP_REMINDER_TIME, currentTimeMs).apply()
                }
            }
        }

        return Result.success()
    }

    private fun sendNotification(notificationId: Int, title: String, message: String) {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_icon)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
}
