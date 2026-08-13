package com.pasic.receipt.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pasic.receipt.ui.components.MainCommonTopBar
import com.pasic.receipt.ui.settings.components.SectionDividerBand
import com.pasic.receipt.ui.settings.dialogs.BackupRestoreBottomSheet
import com.pasic.receipt.ui.settings.dialogs.LicenseDialog
import com.pasic.receipt.ui.settings.dialogs.OptimizeWarningDialog
import com.pasic.receipt.ui.settings.dialogs.ThemeSelectionDialog
import com.pasic.receipt.ui.settings.sections.AiCameraSettingsSection
import com.pasic.receipt.ui.settings.sections.ExportSectionState
import com.pasic.receipt.ui.settings.sections.ExportSettingsSection
import com.pasic.receipt.ui.settings.sections.GeneralSettingsSection
import com.pasic.receipt.ui.settings.sections.StorageSettingsSection
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import kotlin.math.min

private const val SCROLL_THRESHOLD_PX = 80f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    hazeState: HazeState = remember { HazeState() },
    onScrollProgressChanged: (Float) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val userPrefs by viewModel.userPreferences.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var activeToastMessage by remember { mutableStateOf<String?>(null) }

    val rawProgress = min(scrollState.value / SCROLL_THRESHOLD_PX, 1f)
    LaunchedEffect(rawProgress) {
        onScrollProgressChanged(rawProgress)
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            activeToastMessage = msg
            viewModel.clearToastMessage()
            delay(2200)
            activeToastMessage = null
        }
    }

    // SAF 런처 — Activity 결과 콜백이므로 Screen에서만 선언 가능
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.exportBackupZip(context, it) }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.restoreBackupZip(context, it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .verticalScroll(scrollState)
                .padding(bottom = 120.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                MainCommonTopBar()
            }
            Spacer(modifier = Modifier.height(8.dp))

            // ── 섹션 1. 내보내기 설정 ──────────────────────────────────
            ExportSettingsSection(
                state = ExportSectionState(
                    exportFormat = userPrefs.defaultExportFormat,
                    csvSelectedColumns = userPrefs.csvSelectedColumns,
                    csvDateFormat = userPrefs.csvDateFormat,
                    csvAmountFormat = userPrefs.csvAmountFormat,
                    zipNamingRule = userPrefs.zipImageNamingRule,
                    defaultAuthor = userPrefs.defaultAuthor,
                    defaultDepartment = userPrefs.defaultDepartment,
                    defaultPurpose = userPrefs.defaultPurpose,
                ),
                onUpdateExportFormat = { viewModel.updateExportFormat(it) },
                onToggleCsvColumn = { viewModel.toggleCsvColumn(it) },
                onUpdateCsvDateFormat = { viewModel.updateCsvDateFormat(it) },
                onUpdateCsvAmountFormat = { viewModel.updateCsvAmountFormat(it) },
                onUpdateZipNamingRule = { viewModel.updateZipNamingRule(it) },
                onUpdateAuthor = { viewModel.updateAuthor(it) },
                onUpdateDepartment = { viewModel.updateDepartment(it) },
                onUpdatePurpose = { viewModel.updatePurpose(it) },
            )

            SectionDividerBand()

            // ── 섹션 2. 데이터 및 저장소 관리 ───────────────────────────
            StorageSettingsSection(
                storageInfo = uiState.storageInfo,
                autoOptimizeEnabled = userPrefs.autoOptimizeEnabled,
                onBackupRestoreClick = { viewModel.setShowBackupRestoreDialog(true) },
                onStorageRefresh = { viewModel.refreshStorageInfo() },
                onAutoOptimizeToggle = { checked ->
                    if (checked) viewModel.setShowOptimizeWarningDialog(true)
                    else viewModel.toggleAutoOptimize(false)
                }
            )

            SectionDividerBand()

            // ── 섹션 3. AI 인식 및 카메라 ───────────────────────────────
            AiCameraSettingsSection(
                autoCropEnabled = userPrefs.autoCropEnabled,
                bwEnhancementEnabled = userPrefs.bwEnhancementEnabled,
                aiCategoryEnabled = userPrefs.aiCategoryEnabled,
                onToggleAutoCrop = { viewModel.toggleAutoCrop(it) },
                onToggleBwEnhancement = { viewModel.toggleBwEnhancement(it) },
                onToggleAiCategory = { viewModel.toggleAiCategory(it) }
            )

            SectionDividerBand()

            // ── 섹션 4. 기본 설정 ───────────────────────────────────────
            GeneralSettingsSection(
                currentTheme = userPrefs.appTheme,
                onThemeClick = { viewModel.setShowThemeDialog(true) },
                onLicenseClick = { viewModel.setShowLicenseDialog(true) }
            )
        }

        // ── 다이얼로그 / 바텀시트 show/hide 판단 ──────────────────────

        if (uiState.showThemeDialog) {
            ThemeSelectionDialog(
                currentTheme = userPrefs.appTheme,
                onDismiss = { viewModel.setShowThemeDialog(false) },
                onSelect = {
                    viewModel.updateAppTheme(it)
                    viewModel.setShowThemeDialog(false)
                }
            )
        }

        if (uiState.showBackupRestoreDialog) {
            BackupRestoreBottomSheet(
                isProcessingBackup = uiState.isProcessingBackup,
                isProcessingRestore = uiState.isProcessingRestore,
                onDismiss = { viewModel.setShowBackupRestoreDialog(false) },
                onBackupClick = {
                    backupLauncher.launch("receipt_backup_.zip")
                },
                onRestoreClick = {
                    restoreLauncher.launch(arrayOf("application/zip"))
                },
                onGoogleDriveClick = {
                    activeToastMessage = "Google Drive 클라우드 백업 준비 중입니다."
                }
            )
        }

        if (uiState.showOptimizeWarningDialog) {
            OptimizeWarningDialog(
                onDismiss = { viewModel.setShowOptimizeWarningDialog(false) },
                onConfirm = {
                    viewModel.toggleAutoOptimize(true)
                    viewModel.executeImageOptimization(context)
                    viewModel.setShowOptimizeWarningDialog(false)
                }
            )
        }

        if (uiState.showLicenseDialog) {
            LicenseDialog(
                onDismiss = { viewModel.setShowLicenseDialog(false) }
            )
        }

        // ── 아이콘 없이 깔끔한 플로팅 커스텀 토스트 (반투명 연회색 캡슐 스타일) ──
        AnimatedVisibility(
            visible = activeToastMessage != null,
            enter = fadeIn(animationSpec = tween(150)) + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut(animationSpec = tween(150)) + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        ) {
            Box(
                modifier = Modifier
                    .shadow(6.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.08f))
                    .background(Color(0xFFF1F5F9).copy(alpha = 0.94f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeToastMessage ?: "",
                    color = Color(0xFF0F172A),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
