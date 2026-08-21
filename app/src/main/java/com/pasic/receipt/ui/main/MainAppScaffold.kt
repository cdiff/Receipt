package com.pasic.receipt.ui.main

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.pasic.receipt.ui.components.AppFloatingToast
import com.pasic.receipt.ui.components.ReceiptBottomNavigation
import com.pasic.receipt.ui.export.ExportScreen
import com.pasic.receipt.ui.export.ExportViewModel
import com.pasic.receipt.ui.export.PdfInfoBottomSheet
import com.pasic.receipt.ui.export.PdfPreviewDialog
import com.pasic.receipt.ui.export.PdfSaveSuccessDialog
import com.pasic.receipt.ui.export.ReportFormatSelectBottomSheet
import com.pasic.receipt.ui.home.HomeScreen
import com.pasic.receipt.ui.receipts.ReceiptListScreen
import com.pasic.receipt.ui.scan.ScanSharedViewModel
import com.pasic.receipt.util.ToastEventBus
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 앱 전체 화면 및 하단 네비게이션, 전역 토스트 오버레이, 뒤로가기 처리를 단 1곳에서 공통 제어하는 최상위 래퍼 스캐폴드.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    navController: NavHostController = rememberNavController()
) {
    val hazeState = remember { HazeState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    // 뒤로가기 2회 종료 감지 상태
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    // 전역 토스트 메시지 상태
    var activeToastMessage by remember { mutableStateOf<String?>(null) }

    // 홈 화면 [더보기] 스피드 다이얼(Speed Dial) 표시 상태 (하단 탭바까지 덮는 최상위 오버레이)
    var showSpeedDial by remember { mutableStateOf(false) }

    // ToastEventBus 수신 및 2초 자동 닫힘 타이머
    LaunchedEffect(Unit) {
        ToastEventBus.toastEvents.collectLatest { msg ->
            activeToastMessage = msg
            delay(2000)
            activeToastMessage = null
        }
    }

    // 각 화면에서 콜백으로 올려주는 원시 스크롤 진행도 (0f ~ 1f)
    var rawScrollProgress by remember { mutableFloatStateOf(0f) }
    // 탭 전환 직후 사라지는 화면의 스크롤 콜백이 뒤늦게 올라오는 걸 차단하는 플래그
    var isTransitioning by remember { mutableStateOf(false) }

    // ── 1. 타 탭(영수증, 내보내기, 설정)에서 뒤로가기 ➔ 즉시 홈 탭으로 직행 이동 ──
    BackHandler(enabled = currentRoute in listOf("receipts", "export", "settings")) {
        isTransitioning = true
        rawScrollProgress = 0f
        navController.navigate("home") {
            popUpTo("home") { inclusive = true }
        }
        coroutineScope.launch {
            delay(300)
            isTransitioning = false
        }
    }

    // ── 2. 홈 탭에서 뒤로가기 ➔ 2초 내 2회 입력 시 앱 안전 종료 ──
    BackHandler(enabled = currentRoute == "home") {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000L) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressTime = currentTime
            ToastEventBus.showToast("뒤로가기 버튼을 한 번 더 누르면 종료됩니다")
        }
    }

    // tween 애니메이션 — 화면 전환 시 탭바가 튀지 않고 즉시 부드럽게 리셋됨
    val scrollProgress by animateFloatAsState(
        targetValue = rawScrollProgress,
        animationSpec = tween(durationMillis = 150),
        label = "globalNavScrollProgress"
    )

    // 알림 뷰모델 및 읽지 않은 알림 개수 구독
    val notificationViewModel: com.pasic.receipt.ui.notification.NotificationViewModel = hiltViewModel()
    val unreadNotificationCount by notificationViewModel.unreadCount.collectAsState()

    // 내보내기 뷰모델 및 스캔/수기입력 공유 뷰모델
    val exportViewModel: ExportViewModel = hiltViewModel()
    val exportUiState by exportViewModel.uiState.collectAsState()
    val scanSharedViewModel: ScanSharedViewModel = hiltViewModel()

    var showReportFormatSheet by remember { mutableStateOf(false) }
    var showMonthlyPdfInfoSheet by remember { mutableStateOf(false) }

    // 하단 탭바 표시 여부
    val hasBottomBar = currentRoute !in listOf("scan", "scan_result", "notifications", "support_center") && !currentRoute.startsWith("receipt_detail") && !currentRoute.startsWith("notice_detail")

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = com.pasic.receipt.ui.theme.ScreenBackground
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
                                    delay(300)
                                    isTransitioning = false
                                }
                            },
                            onScrollProgressChanged = { if (!isTransitioning) rawScrollProgress = it },
                            onNavigateToReceiptList = {
                                isTransitioning = true
                                rawScrollProgress = 0f
                                navController.navigate("receipts") {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                coroutineScope.launch {
                                    delay(300)
                                    isTransitioning = false
                                }
                            },
                            onNavigateToReceiptDetail = { id ->
                                navController.navigate("receipt_detail/$id")
                            },
                            onMoreClick = {
                                showSpeedDial = true
                            },
                            unreadNotificationCount = unreadNotificationCount,
                            onNotificationClick = {
                                isTransitioning = true
                                rawScrollProgress = 0f
                                navController.navigate("notifications")
                                coroutineScope.launch {
                                    delay(300)
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
                                    delay(300)
                                    isTransitioning = false
                                }
                            },
                            onNavigateToScan = {
                                isTransitioning = true
                                rawScrollProgress = 0f
                                navController.navigate("scan")
                                coroutineScope.launch {
                                    delay(300)
                                    isTransitioning = false
                                }
                            },
                            onNavigateToDetail = { id ->
                                navController.navigate("receipt_detail/$id")
                            }
                        )
                    }

                    // 2. 스캔 카메라 모달 (아래에서 위로 slideInVertically)
                    composable(
                        route = "scan",
                        enterTransition = { slideInVertically(animationSpec = tween(320)) { fullHeight -> fullHeight } },
                        exitTransition = { slideOutVertically(animationSpec = tween(280)) { fullHeight -> fullHeight } },
                        popEnterTransition = { slideInVertically(animationSpec = tween(320)) { fullHeight -> fullHeight } },
                        popExitTransition = { slideOutVertically(animationSpec = tween(280)) { fullHeight -> fullHeight } }
                    ) {
                        com.pasic.receipt.ui.scan.CameraScanScreen(
                            viewModel = scanSharedViewModel,
                            onNavigateToResult = {
                                navController.navigate("scan_result")
                            },
                            onClose = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // 3. 스캔 결과 및 직접 수기 입력 화면
                    composable(
                        route = "scan_result",
                        enterTransition = { fadeIn(animationSpec = tween(250)) },
                        exitTransition = { fadeOut(animationSpec = tween(250)) },
                        popEnterTransition = { fadeIn(animationSpec = tween(250)) },
                        popExitTransition = { fadeOut(animationSpec = tween(250)) }
                    ) {
                        com.pasic.receipt.ui.scan.ReceiptScanResultScreen(
                            viewModel = scanSharedViewModel,
                            onNavigateBackToScan = {
                                navController.popBackStack()
                            },
                            onSaveSuccess = {
                                navController.navigate("receipts") {
                                    popUpTo("home") { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    // 4. 영수증 상세 페이지 (수평 슬라이드)
                    composable(
                        route = "receipt_detail/{receiptId}",
                        enterTransition = { androidx.compose.animation.slideInHorizontally(animationSpec = tween(300)) { fullWidth -> fullWidth } },
                        exitTransition = { androidx.compose.animation.slideOutHorizontally(animationSpec = tween(300)) { fullWidth -> -fullWidth } },
                        popEnterTransition = { androidx.compose.animation.slideInHorizontally(animationSpec = tween(300)) { fullWidth -> -fullWidth } },
                        popExitTransition = { androidx.compose.animation.slideOutHorizontally(animationSpec = tween(300)) { fullWidth -> fullWidth } }
                    ) { backStackEntry ->
                        val receiptIdStr = backStackEntry.arguments?.getString("receiptId") ?: "0"
                        val receiptId = receiptIdStr.toLongOrNull() ?: 0L
                        com.pasic.receipt.ui.receipts.ReceiptDetailScreen(
                            receiptId = receiptId,
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onNavigateToEdit = { id ->
                                // 편집 기능
                            }
                        )
                    }

                    // 5. 내보내기 화면
                    composable("export") {
                        ExportScreen(
                            hazeState = hazeState,
                            onScrollProgressChanged = { if (!isTransitioning) rawScrollProgress = it },
                            unreadNotificationCount = unreadNotificationCount,
                            onNotificationClick = {
                                isTransitioning = true
                                rawScrollProgress = 0f
                                navController.navigate("notifications")
                                coroutineScope.launch {
                                    delay(300)
                                    isTransitioning = false
                                }
                            }
                        )
                    }

                    // 6. 설정 화면
                    composable("settings") {
                        com.pasic.receipt.ui.settings.SettingsScreen(
                            hazeState = hazeState,
                            onScrollProgressChanged = { if (!isTransitioning) rawScrollProgress = it },
                            unreadNotificationCount = unreadNotificationCount,
                            onNotificationClick = {
                                isTransitioning = true
                                rawScrollProgress = 0f
                                navController.navigate("notifications")
                                coroutineScope.launch {
                                    delay(300)
                                    isTransitioning = false
                                }
                            },
                            onNavigateToSupport = {
                                navController.navigate("support_center")
                            }
                        )
                    }

                    // 7. 알림 센터 화면 (영수증 상세와 동일한 수평 슬라이드 트랜지션)
                    composable(
                        route = "notifications",
                        enterTransition = { androidx.compose.animation.slideInHorizontally(animationSpec = tween(300)) { fullWidth -> fullWidth } },
                        exitTransition = { androidx.compose.animation.slideOutHorizontally(animationSpec = tween(300)) { fullWidth -> -fullWidth } },
                        popEnterTransition = { androidx.compose.animation.slideInHorizontally(animationSpec = tween(300)) { fullWidth -> -fullWidth } },
                        popExitTransition = { androidx.compose.animation.slideOutHorizontally(animationSpec = tween(300)) { fullWidth -> fullWidth } }
                    ) {
                        com.pasic.receipt.ui.notification.NotificationScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onNavigateToRoute = { route ->
                                navController.navigate(route)
                            },
                            onNavigateToNoticeDetail = { noticeId ->
                                navController.navigate("notice_detail/$noticeId")
                            },
                            viewModel = notificationViewModel
                        )
                    }

                    // 8. 업데이트 공지 상세 화면
                    composable(
                        route = "notice_detail/{noticeId}",
                        enterTransition = { androidx.compose.animation.slideInHorizontally(animationSpec = tween(300)) { fullWidth -> fullWidth } },
                        exitTransition = { androidx.compose.animation.slideOutHorizontally(animationSpec = tween(300)) { fullWidth -> -fullWidth } },
                        popEnterTransition = { androidx.compose.animation.slideInHorizontally(animationSpec = tween(300)) { fullWidth -> -fullWidth } },
                        popExitTransition = { androidx.compose.animation.slideOutHorizontally(animationSpec = tween(300)) { fullWidth -> fullWidth } }
                    ) { backStackEntry ->
                        val noticeId = backStackEntry.arguments?.getString("noticeId") ?: "notice_v110"
                        com.pasic.receipt.ui.notification.NoticeDetailScreen(
                            noticeId = noticeId,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // 9. 고객센터 및 라이선스 화면 (수평 슬라이드 트랜지션)
                    composable(
                        route = "support_center",
                        enterTransition = { androidx.compose.animation.slideInHorizontally(animationSpec = tween(300)) { fullWidth -> fullWidth } },
                        exitTransition = { androidx.compose.animation.slideOutHorizontally(animationSpec = tween(300)) { fullWidth -> -fullWidth } },
                        popEnterTransition = { androidx.compose.animation.slideInHorizontally(animationSpec = tween(300)) { fullWidth -> -fullWidth } },
                        popExitTransition = { androidx.compose.animation.slideOutHorizontally(animationSpec = tween(300)) { fullWidth -> fullWidth } }
                    ) {
                        com.pasic.receipt.ui.support.CustomerSupportScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onNavigateToNoticeDetail = { noticeId ->
                                navController.navigate("notice_detail/$noticeId")
                            }
                        )
                    }
                } // NavHost
            } // safeDrawingPadding Box
        } // Surface

        // 단 1개의 공통 탭바 — 스캔/결과/상세 모달 진입 시 숨김 처리
        if (hasBottomBar) {
            ReceiptBottomNavigation(
                currentRoute = currentRoute,
                hazeState = hazeState,
                scrollProgress = scrollProgress,
                onTabSelected = { targetRoute ->
                    if (currentRoute != targetRoute) {
                        isTransitioning = true
                        rawScrollProgress = 0f
                        coroutineScope.launch {
                            delay(300)
                            isTransitioning = false
                        }
                        when (targetRoute) {
                            "home" -> navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                                launchSingleTop = true
                            }
                            "receipts" -> navController.navigate("receipts") {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                            "export" -> navController.navigate("export") {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                            "settings" -> navController.navigate("settings") {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = 600.dp)
            )
        }

        // ── [더보기] 스피드 다이얼(Speed Dial) 최상위 오버레이 (하단 탭바까지 100% 덮음) ──
        com.pasic.receipt.ui.home.components.HomeSpeedDialMenu(
            visible = showSpeedDial,
            onDismiss = { showSpeedDial = false },
            hazeState = hazeState,
            onCustomerCenterClick = {
                showSpeedDial = false
                try {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://open.kakao.com/o/scpC0uJi")
                    )
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    com.pasic.receipt.util.ToastEventBus.showToast("오픈채팅 링크를 열 수 없습니다.")
                }
            },
            onMonthlyReportClick = {
                showSpeedDial = false
                showReportFormatSheet = true
            },
            onManualInputClick = {
                showSpeedDial = false
                scanSharedViewModel.startManualInputMode()
                navController.navigate("scan_result")
            },
            onZipBackupClick = {
                showSpeedDial = false
                exportViewModel.exportAllReceiptsZipQuick(context)
            }
        )

        // ── [이번달 지출 보고서] 파일 형식 선택 바텀시트 (CSV 즉시 다운로드 / PDF ➔ 내보내기 공통 PdfInfoBottomSheet 호출) ──
        if (showReportFormatSheet) {
            ReportFormatSelectBottomSheet(
                onDismissRequest = { showReportFormatSheet = false },
                onSelectCsv = {
                    exportViewModel.exportMonthlyCsvQuick(context)
                },
                onSelectPdf = {
                    showReportFormatSheet = false
                    coroutineScope.launch {
                        delay(150)
                        showMonthlyPdfInfoSheet = true
                    }
                }
            )
        }

        // ── [내보내기 공통] 지출결의서 정보 입력 바텀시트 (홈 화면 더보기에서도 오리지널 UI 100% 동일 재사용) ──
        if (showMonthlyPdfInfoSheet) {
            PdfInfoBottomSheet(
                initialAuthor = exportUiState.defaultAuthor,
                initialDept = exportUiState.defaultDepartment,
                initialPurpose = exportUiState.defaultPurpose,
                onDismiss = { showMonthlyPdfInfoSheet = false },
                onConfirm = { author, dept, purpose ->
                    showMonthlyPdfInfoSheet = false
                    exportViewModel.exportMonthlyPdfQuick(context, author, dept, purpose)
                }
            )
        }

        // ── PDF 미리보기 다이얼로그 ──
        if (exportUiState.showPdfPreviewDialog && exportUiState.previewPdfFile != null) {
            PdfPreviewDialog(
                pdfFile = exportUiState.previewPdfFile!!,
                onDismiss = { exportViewModel.dismissPdfPreviewDialog() },
                onConfirmSave = { exportViewModel.confirmSavePdf(context) }
            )
        }

        // ── 파일 저장 완료 다이얼로그 (CSV, PDF, ZIP 공통) ──
        if (exportUiState.showSaveSuccessDialog) {
            PdfSaveSuccessDialog(
                fileName = exportUiState.savedFileName,
                savedFile = exportUiState.previewPdfFile,
                onDismiss = { exportViewModel.dismissSaveSuccessDialog() }
            )
        }

        // ── 전역 단일 플로팅 알약 캡슐 토스트 오버레이 (탭바 유무에 따른 동적 패딩) ──
        AppFloatingToast(
            message = activeToastMessage,
            bottomPadding = if (hasBottomBar) 96.dp else 48.dp,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
