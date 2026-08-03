package com.pasic.receipt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pasic.receipt.ui.main.MainAppScaffold
import com.pasic.receipt.ui.theme.ReceiptTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReceiptTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppScaffold()
                }
            }
        }
    }
}
