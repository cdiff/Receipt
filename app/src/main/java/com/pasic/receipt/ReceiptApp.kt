package com.pasic.receipt

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ReceiptApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            if (BuildConfig.DEBUG) {
                // 개발 및 로컬 테스트 환경 (Logcat 디버그 토큰)
                firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
            } else {
                // Google Play 스토어 정식 배포 환경 (Play Integrity 무결성 검증)
                firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            }
        } catch (e: Exception) {
            Log.e("ReceiptApp", "Firebase App Check init error: ${e.message}", e)
        }

        // 🔔 스마트 알림 채널 등록 및 WorkManager 백그라운드 스케줄러 등록
        try {
            com.pasic.receipt.data.notification.NotificationPushWorker.createNotificationChannel(this)
            scheduleNotificationPushWorker()
        } catch (e: Exception) {
            Log.e("ReceiptApp", "Notification scheduler init error: ${e.message}", e)
        }
    }

    private fun scheduleNotificationPushWorker() {
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.pasic.receipt.data.notification.NotificationPushWorker>(
            1, java.util.concurrent.TimeUnit.HOURS
        ).build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ReceiptPushWorker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
