package com.pasic.receipt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.pasic.receipt.data.preferences.AppThemeOption
import com.pasic.receipt.data.preferences.UserPreferences
import com.pasic.receipt.data.preferences.UserPreferencesRepository
import com.pasic.receipt.ui.main.MainAppScaffold
import com.pasic.receipt.ui.theme.ReceiptTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            val userPrefs by userPreferencesRepository.userPreferencesFlow.collectAsState(initial = UserPreferences())
            val darkTheme = when (userPrefs.appTheme) {
                AppThemeOption.LIGHT -> false
                AppThemeOption.DARK -> true
                AppThemeOption.SYSTEM -> isSystemInDarkTheme()
            }

            ReceiptTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppScaffold()
                }
            }
        }
    }
}
