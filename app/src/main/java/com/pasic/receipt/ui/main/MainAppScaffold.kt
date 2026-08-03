package com.pasic.receipt.ui.main

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pasic.receipt.ui.components.ReceiptBottomNavigation
import com.pasic.receipt.ui.home.HomeScreen
import com.pasic.receipt.ui.receipts.ReceiptListScreen
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

/**
 * 앱 전체 화면 및 하단 iOS Liquid Glass 네비게이션을 단 1곳에서 공통 제어하는 최상위 래퍼 스캐폴드.
 * 3단계 화면 전환 규칙:
 * 1. 탭 간 이동: 180ms 부드러운 FadeIn/Out + 미세 0.98x 스케일
 * 2. 상세 페이지: iOS 스타일 수평 Slide In/Out
 * 3. 카메라/작성 모달: 아래에서 위로 Slide In Vertically
 */
@Composable
fun MainAppScaffold(
    navController: NavHostController = rememberNavController()
) {
    val hazeState = remember { HazeState() }
    val coroutineScope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    // 각 화면에서 콜백으로 올려주는 원시 스크롤 진행도 (0f ~ 1f)
    var rawScrollProgress by remember { mutableFloatStateOf(0f) }

    // spring 애니메이션 적용 — 화면 전환 시에도 부드럽게 리셋됨
    val scrollProgress by animateFloatAsState(
        targetValue = rawScrollProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "globalNavScrollProgress"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            // safeDrawingPadding: 상태바·카메라 노치·하단 제스처 영역 인셋을 단 1곳에서 처리
            Box(modifier = Modifier.safeDrawingPadding()) {
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    enterTransition = { fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.98f, animationSpec = tween(180)) },
                    exitTransition = { fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.98f, animationSpec = tween(180)) },
                    popEnterTransition = { fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.98f, animationSpec = tween(180)) },
                    popExitTransition = { fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.98f, animationSpec = tween(180)) }
                ) {
                    // 1. 하단 탭 간 이동 (홈 ↔ 영수증 ↔ 내보내기 ↔ 설정) : 은은한 크로스페이드 + 미세 0.98x 스케일 (180ms)
                    composable("home") {
                        HomeScreen(
                            hazeState = hazeState,
                            onScanClick = {},
                            onScrollProgressChanged = { rawScrollProgress = it },
                            onNavigateToReceiptList = {
                                rawScrollProgress = 0f
                                navController.navigate("receipts") { launchSingleTop = true }
                            }
                        )
                    }

                    composable("receipts") {
                        ReceiptListScreen(
                            hazeState = hazeState,
                            onScrollProgressChanged = { rawScrollProgress = it },
                            onNavigateToHome = {
                                rawScrollProgress = 0f
                                navController.navigate("home") { popUpTo("home") { inclusive = true } }
                            },
                            onNavigateToScan = {},
                            onNavigateToExport = {},
                            onNavigateToSettings = {}
                        )
                    }
                } // NavHost
            } // safeDrawingPadding Box
        } // Surface

        // 단 1개의 공통 탭바 — scrollProgress에 따라 텍스트 숨김 & 크기 축소
        ReceiptBottomNavigation(
            currentRoute = currentRoute,
            hazeState = hazeState,
            scrollProgress = scrollProgress,
            onTabSelected = { targetRoute ->
                if (currentRoute != targetRoute) {
                    rawScrollProgress = 0f
                    when (targetRoute) {
                        "home" -> navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                        "receipts" -> navController.navigate("receipts") {
                            launchSingleTop = true
                        }
                        "export" -> {}
                        "settings" -> {}
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 600.dp)
        )
    }
}
