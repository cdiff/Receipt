package com.pasic.receipt.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Lucide
import com.pasic.receipt.ui.home.components.MonthlyScoreCard
import com.pasic.receipt.ui.home.components.QuickActionGrid
import com.pasic.receipt.ui.home.components.RecentReceiptsList
import com.pasic.receipt.ui.home.components.RecentRegisteredCards
import com.pasic.receipt.ui.home.components.SpendingUsageChart
import com.pasic.receipt.ui.home.components.TotalSpendingHeader
import com.pasic.receipt.ui.theme.ScreenBackground
import com.pasic.receipt.ui.theme.TextPrimary
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlin.math.min

// 네비게이션 바 높이(64dp) + 하단 여백(16dp) + 스크롤 여유 여백(20dp)
private val NAV_BOTTOM_PADDING = 64.dp + 16.dp + 20.dp

// 이 픽셀 이상 스크롤되면 progress = 1f (탭바 완전 축소)
private const val SCROLL_THRESHOLD_PX = 80f

@Composable
fun HomeScreen(
    hazeState: HazeState = remember { HazeState() },
    onScanClick: () -> Unit = {},
    onScrollProgressChanged: (Float) -> Unit = {},
    onNavigateToReceiptList: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // 스크롤 진행도 계산 (0f ~ 1f) — MainAppScaffold로 올려줌
    val rawProgress by remember {
        derivedStateOf { min(scrollState.value / SCROLL_THRESHOLD_PX, 1f) }
    }

    // 스크롤 변화가 있을 때마다 콜백으로 진행도 전달
    LaunchedEffect(rawProgress) {
        onScrollProgressChanged(rawProgress)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScreenBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 탑바 — 상단 알림 Bell 아이콘
            Row(
                modifier = Modifier.fillMaxWidth(),
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

            Spacer(modifier = Modifier.height(8.dp))

            // 1. 상단 총 지출 헤더
            TotalSpendingHeader(
                totalSpendingFormatted = uiState.totalSpendingFormatted,
                trendFormatted = uiState.trendFormatted
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 2. 이달의 소비 성향 카드
            MonthlyScoreCard()

            Spacer(modifier = Modifier.height(24.dp))

            // 3. 4-Grid 빠른 메뉴
            QuickActionGrid(
                onScanClick = onScanClick
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 4. 최근 등록 카드 캐러셀
            RecentRegisteredCards()

            Spacer(modifier = Modifier.height(28.dp))

            // 5. 최근 영수증 목록
            RecentReceiptsList(
                receipts = uiState.recentReceipts,
                onSeeAllClick = onNavigateToReceiptList,
                onScanClick = onScanClick
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 6. 소비 사용량 차트 (월별, 주별, 일별)
            SpendingUsageChart(
                receipts = uiState.allReceipts
            )

            // 하단 네비게이션 바 여백
            Spacer(modifier = Modifier.height(NAV_BOTTOM_PADDING))
        }
    }
}
