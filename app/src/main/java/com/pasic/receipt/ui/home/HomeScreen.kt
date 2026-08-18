package com.pasic.receipt.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasic.receipt.ui.home.components.CategorySpendingBreakdown
import com.pasic.receipt.ui.home.components.HomeMainHeroBannerCard
import com.pasic.receipt.ui.home.components.QuickActionGrid
import com.pasic.receipt.ui.home.components.RecentReceiptsList
import com.pasic.receipt.ui.home.components.SpendingUsageChart
import com.pasic.receipt.ui.theme.ScreenBackground
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
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
    onNavigateToReceiptDetail: (Long) -> Unit = {},
    onMoreClick: () -> Unit = {},
    unreadNotificationCount: Int = 0,
    onNotificationClick: () -> Unit = {},
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

    val coroutineScope = rememberCoroutineScope()
    var chartOffsetY by remember { mutableIntStateOf(0) }

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

            // 공통 탑바 (홈 화면에 영수증 쏙 로고 표시 + 알림 뱃지 및 클릭 연동)
            com.pasic.receipt.ui.components.MainCommonTopBar(
                showLogo = true,
                unreadCount = unreadNotificationCount,
                onNotificationClick = onNotificationClick
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 1. 메인 히어로 배너 카드 (오늘 지출 & 스캔 건수 기반 동적 배지/타이틀)
            HomeMainHeroBannerCard(
                todayCount = uiState.todayCount,
                todayAmountFormatted = uiState.todayAmountFormatted,
                onCardClick = onNavigateToReceiptList
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. 4-Grid 빠른 메뉴
            QuickActionGrid(
                onScanClick = onScanClick,
                onNavigateToReceiptList = onNavigateToReceiptList,
                onChartClick = {
                    coroutineScope.launch {
                        // 차트 영역이 화면 상단에 알맞게 보이도록 스무스 스크롤 (헤더 여백 고려)
                        val target = maxOf(0, chartOffsetY - 16)
                        scrollState.animateScrollTo(
                            value = target,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                        )
                    }
                },
                onMoreClick = onMoreClick
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 4. 소비 사용량 차트 (월별, 주별, 일별) - 위치 측정(onGloballyPositioned)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        chartOffsetY = coordinates.positionInParent().y.toInt()
                    }
            ) {
                SpendingUsageChart(
                    receipts = uiState.allReceipts
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 5. 카테고리별 지출 (로컬 DB 전체 누적 상위 4개)
            CategorySpendingBreakdown(
                receipts = uiState.allReceipts
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 5. 최근 영수증 목록
            RecentReceiptsList(
                receipts = uiState.recentReceipts,
                onSeeAllClick = onNavigateToReceiptList,
                onScanClick = onScanClick,
                onReceiptClick = { receipt -> onNavigateToReceiptDetail(receipt.id) }
            )

            // 하단 네비게이션 바 여백
            Spacer(modifier = Modifier.height(NAV_BOTTOM_PADDING))
        }
    }
}
