package com.pasic.receipt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pasic.receipt.data.preferences.UserPreferencesRepository
import com.pasic.receipt.ui.main.MainAppScaffold
import com.pasic.receipt.ui.splash.SplashScreen
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
            var showSplash by remember { mutableStateOf(true) }

            ReceiptTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = com.pasic.receipt.ui.theme.ScreenBackground
                ) {
                    Crossfade(
                        targetState = showSplash,
                        animationSpec = tween(durationMillis = 200),
                        label = "splashCrossfade"
                    ) { isSplash ->
                        if (isSplash) {
                            SplashScreen(
                                onSplashFinished = { showSplash = false }
                            )
                        } else {
                            MainAppScaffold()
                        }
                    }
                }
            }
        }
    }
}
