package com.pasic.receipt.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.*
import com.pasic.receipt.ui.home.components.MonthlyScoreCard
import com.pasic.receipt.ui.home.components.QuickActionGrid
import com.pasic.receipt.ui.home.components.ReceiptBottomNavigation
import com.pasic.receipt.ui.home.components.RecentReceiptsList
import com.pasic.receipt.ui.home.components.RecentRegisteredCards
import com.pasic.receipt.ui.home.components.SpendingUsageChart
import com.pasic.receipt.ui.home.components.TotalSpendingHeader
import com.pasic.receipt.ui.theme.ScreenBackground
import com.pasic.receipt.ui.theme.TextPrimary
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

// 네비게이션 바 높이(64dp) + 하단 여백(16dp) + 스크롤 여유 여백(20dp)
private val NAV_BOTTOM_PADDING = 64.dp + 16.dp + 20.dp

@Composable
fun HomeScreen(
    onScanClick: () -> Unit = {},
    onSeeAllReceiptsClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hazeState = remember { HazeState() }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // 40px 이상 스크롤했는지 여부 (derivedStateOf로 불필요한 리컴포지션 방지)
    val isScrolled by remember {
        derivedStateOf { scrollState.value > 40 }
    }

    // iOS 쫀득한 스프링 애니메이션으로 progress 변환 (0f -> 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = if (isScrolled) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fluidNavSpring"
    )

    Scaffold(
        containerColor = ScreenBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            contentAlignment = Alignment.TopCenter
        ) {
            // Scrollable Content Column — hazeSource로 Liquid Glass 블러 소스 제공
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .hazeSource(state = hazeState)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
            ) {
                // Compact Top Action Bar (Lucide Bell Icon)
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
                            imageVector = Lucide.Bell,
                            contentDescription = "Notifications",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
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
                    onSeeAllClick = onSeeAllReceiptsClick,
                    onScanClick = onScanClick
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 6. Spending Usage Bar Chart (월별, 주별, 일별)
                SpendingUsageChart(
                    receipts = uiState.recentReceipts
                )

                // 네비게이션 바 + FAB 영역만큼 여백
                Spacer(modifier = Modifier.height(NAV_BOTTOM_PADDING))
            }

            // 부드러운 스프링 물리 기반 iOS Liquid Glass 네비게이션
            ReceiptBottomNavigation(
                currentRoute = uiState.currentTab,
                hazeState = hazeState,
                scrollProgress = animatedProgress,
                onTabSelected = { route ->
                    if (uiState.currentTab == route) {
                        coroutineScope.launch { scrollState.animateScrollTo(0) }
                    } else {
                        viewModel.selectTab(route)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = 600.dp)
            )
        }
    }
}
