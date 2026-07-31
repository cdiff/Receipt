package com.pasic.receipt.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasic.receipt.ui.home.components.MonthlyScoreCard
import com.pasic.receipt.ui.home.components.QuickActionGrid
import com.pasic.receipt.ui.home.components.ReceiptBottomNavigation
import com.pasic.receipt.ui.home.components.RecentReceiptsList
import com.pasic.receipt.ui.home.components.RecentRegisteredCards
import com.pasic.receipt.ui.home.components.TotalSpendingHeader
import com.pasic.receipt.ui.theme.ScreenBackground
import com.pasic.receipt.ui.theme.TextPrimary

@Composable
fun HomeScreen(
    onScanClick: () -> Unit = {},
    onSeeAllReceiptsClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = ScreenBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            contentAlignment = Alignment.TopCenter
        ) {
            // Scrollable Content Column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                // Compact Top Action Bar (Sleek right-aligned Bell Icon)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {},
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 1. Total Spending Header
                TotalSpendingHeader(
                    totalSpendingFormatted = uiState.totalSpendingFormatted,
                    trendFormatted = uiState.trendFormatted
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Monthly Score Banner Card
                MonthlyScoreCard()

                Spacer(modifier = Modifier.height(20.dp))

                // 3. 4-Grid Quick Actions
                QuickActionGrid(
                    onScanClick = onScanClick
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 4. Recent Registered Cards Carousel
                RecentRegisteredCards()

                Spacer(modifier = Modifier.height(28.dp))

                // 5. Recent Receipts List
                RecentReceiptsList(
                    receipts = uiState.recentReceipts,
                    onSeeAllClick = onSeeAllReceiptsClick
                )

                // Bottom Padding Spacer to guarantee 100% visibility above Floating Navigation Bar & FAB
                Spacer(modifier = Modifier.height(180.dp))
            }

            // Floating Navigation & Camera FAB Overlay pinned at bottom
            ReceiptBottomNavigation(
                currentRoute = uiState.currentTab,
                onTabSelected = { viewModel.selectTab(it) },
                onCameraClick = onScanClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = 600.dp)
            )
        }
    }
}
