package com.pasic.receipt.ui.main

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.mutableStateOf
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
import androidx.hilt.navigation.compose.hiltViewModel
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
    // 탭 전환 직후 사라지는 화면의 스크롤 콜백이 뒤늦게 올라오는 걸 차단하는 플래그
    var isTransitioning by remember { androidx.compose.runtime.mutableStateOf(false) }

    // tween 애니메이션 — 화면 전환 시 탭바가 튀지 않고 즉시 부드럽게 리셋됨
    val scrollProgress by animateFloatAsState(
        targetValue = rawScrollProgress,
        animationSpec = tween(durationMillis = 150),
        label = "globalNavScrollProgress"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            // safeDrawingPadding: 상태바·카메라 노치·하단 제스처 영역 인셋 처리 (화이트 마스킹)
            Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
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
                            onScanClick = {
                                isTransitioning = true
                                rawScrollProgress = 0f
                                navController.navigate("scan")
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(300)
                                    isTransitioning = false
                                }
                            },
                            onScrollProgressChanged = { if (!isTransitioning) rawScrollProgress = it },
                            onNavigateToReceiptList = {
                                isTransitioning = true
                                rawScrollProgress = 0f
                                navController.navigate("receipts") { launchSingleTop = true }
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(300)
                                    isTransitioning = false
                                }
                            }
                        )
                    }

                    composable("receipts") {
                        ReceiptListScreen(
                            hazeState = hazeState,
                            onScrollProgressChanged = { if (!isTransitioning) rawScrollProgress = it },
                            onNavigateToHome = {
                                isTransitioning = true
                                rawScrollProgress = 0f
                                navController.navigate("home") { popUpTo("home") { inclusive = true } }
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(300)
                                    isTransitioning = false
                                }
                            },
                            onNavigateToScan = {
                                isTransitioning = true
                                rawScrollProgress = 0f
                                navController.navigate("scan")
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(300)
                                    isTransitioning = false
                                }
                            },
                            onNavigateToExport = {},
                            onNavigateToSettings = {}
                        )
                    }

                    // 2. 스캔 카메라 모달 (아래에서 위로 slideInVertically)
                    composable(
                        route = "scan",
                        enterTransition = { androidx.compose.animation.slideInVertically(animationSpec = tween(300)) { fullHeight -> fullHeight } },
                        exitTransition = { androidx.compose.animation.slideOutVertically(animationSpec = tween(300)) { fullHeight -> fullHeight } },
                        popEnterTransition = { androidx.compose.animation.slideInVertically(animationSpec = tween(300)) { fullHeight -> fullHeight } },
                        popExitTransition = { androidx.compose.animation.slideOutVertically(animationSpec = tween(300)) { fullHeight -> fullHeight } }
                    ) { backStackEntry ->
                        val scanViewModel: com.pasic.receipt.ui.scan.ScanSharedViewModel = hiltViewModel(backStackEntry)
                        com.pasic.receipt.ui.scan.CameraScanScreen(
                            viewModel = scanViewModel,
                            onNavigateToResult = {
                                navController.navigate("scan_result")
                            },
                            onClose = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // 3. 스캔 결과 검증 및 편집 모달
                    composable(
                        route = "scan_result",
                        enterTransition = { androidx.compose.animation.slideInVertically(animationSpec = tween(300)) { fullHeight -> fullHeight } },
                        exitTransition = { androidx.compose.animation.slideOutVertically(animationSpec = tween(300)) { fullHeight -> fullHeight } },
                        popEnterTransition = { androidx.compose.animation.slideInVertically(animationSpec = tween(300)) { fullHeight -> fullHeight } },
                        popExitTransition = { androidx.compose.animation.slideOutVertically(animationSpec = tween(300)) { fullHeight -> fullHeight } }
                    ) {
                        val parentEntry = remember(it) { navController.getBackStackEntry("scan") }
                        val scanViewModel: com.pasic.receipt.ui.scan.ScanSharedViewModel = hiltViewModel(parentEntry)
                        com.pasic.receipt.ui.scan.ReceiptScanResultScreen(
                            viewModel = scanViewModel,
                            onNavigateBackToScan = {
                                navController.popBackStack("scan", inclusive = false)
                            },
                            onSaveSuccess = {
                                navController.popBackStack("home", inclusive = false)
                            }
                        )
                    }
                } // NavHost
            } // safeDrawingPadding Box
        } // Surface

        // 단 1개의 공통 탭바 — 스캔/결과 모달 진입 시 숨김 처리
        if (currentRoute !in listOf("scan", "scan_result")) {
            ReceiptBottomNavigation(
                currentRoute = currentRoute,
                hazeState = hazeState,
                scrollProgress = scrollProgress,
                onTabSelected = { targetRoute ->
                    if (currentRoute != targetRoute) {
                        isTransitioning = true
                        rawScrollProgress = 0f
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(300)
                            isTransitioning = false
                        }
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
}
