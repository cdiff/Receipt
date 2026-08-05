package com.pasic.receipt

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ReceiptApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } catch (e: Exception) {
            Log.e("ReceiptApp", "Firebase init error: ${e.message}", e)
        }
    }
}
