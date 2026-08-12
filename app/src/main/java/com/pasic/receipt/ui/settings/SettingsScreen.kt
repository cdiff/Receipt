package com.pasic.receipt.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.Upload
import com.pasic.receipt.data.preferences.ALL_CSV_COLUMNS
import com.pasic.receipt.data.preferences.AppThemeOption
import com.pasic.receipt.ui.components.MainCommonTopBar
import com.pasic.receipt.ui.theme.FabNavy
import com.pasic.receipt.ui.theme.TextMuted
import com.pasic.receipt.ui.theme.TextPrimary
import com.pasic.receipt.ui.theme.TextSecondary
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
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

    val rawProgress = min(scrollState.value / SCROLL_THRESHOLD_PX, 1f)
    LaunchedEffect(rawProgress) {
        onScrollProgressChanged(rawProgress)
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    // 백업 SAF 파일 생성 래운처
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.exportBackupZip(context, it) }
    }

    // 복원 SAF 파일 선택 래운처
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
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionHeader("내보내기 설정")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "원하는 형식으로 영수증 데이터를 내보낼 수 있습니다. ERP 시스템 요구사항에 맞춰 설정하세요.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                // 1. 상단: 포맷 선택에 따라 PDF 입력 필드 ↔ CSV 세부 옵션이 동일 위치에서 스위칭
                // A. PDF 선택 시: PDF 입력 필드 (작성자/부서/목적)
                AnimatedVisibility(
                    visible = userPrefs.defaultExportFormat == "PDF",
                    enter = fadeIn() + slideInHorizontally(initialOffsetX = { -it / 3 }) + expandVertically(),
                    exit = fadeOut() + slideOutHorizontally(targetOffsetX = { -it / 3 }) + shrinkVertically()
                ) {
                    Column {
                        SettingsInputField(
                            label = "기본 작성자",
                            value = userPrefs.defaultAuthor,
                            onValueChange = { viewModel.updateAuthor(it) },
                            placeholder = "예: 홍길동"
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        SettingsInputField(
                            label = "소속 부서",
                            value = userPrefs.defaultDepartment,
                            onValueChange = { viewModel.updateDepartment(it) },
                            placeholder = "예: 개발팀"
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        SettingsInputField(
                            label = "기본 목적",
                            value = userPrefs.defaultPurpose,
                            onValueChange = { viewModel.updatePurpose(it) },
                            placeholder = "예: 업무 경비"
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // B. CSV 선택 시: CSV 맞춤 세부 옵션 (열 칩/날짜/금액/ZIP 규칙)
                AnimatedVisibility(
                    visible = userPrefs.defaultExportFormat != "PDF",
                    enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 3 }) + expandVertically(),
                    exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 3 }) + shrinkVertically()
                ) {
                    Column {
                        CsvInlineOptionsContent(
                            userPrefs = userPrefs,
                            onToggleColumn = { viewModel.toggleCsvColumn(it) },
                            onUpdateDateFormat = { viewModel.updateCsvDateFormat(it) },
                            onUpdateAmountFormat = { viewModel.updateCsvAmountFormat(it) },
                            onUpdateZipNamingRule = { viewModel.updateZipNamingRule(it) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // 2. 하단 고정: 기본 포맷 선택 세그먼트 버튼 (PDF / CSV)
                Text(text = "기본 포맷", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isPdf = userPrefs.defaultExportFormat == "PDF"
                    SegmentedButton(
                        text = "PDF로 내보내기",
                        selected = isPdf,
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.updateExportFormat("PDF")
                    }
                    SegmentedButton(
                        text = "CSV로 내보내기",
                        selected = !isPdf,
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.updateExportFormat("EXCEL")
                    }
                }
            }

            SectionDividerBand()

            // ── 섹션 2. 데이터 및 저장소 관리 ───────────────────────────
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionHeader("데이터 및 저장소 관리")
                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp), spotColor = Color.Black.copy(alpha = 0.05f))
                        .background(Color.White, RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                ) {
                    Column {
                        SettingsNavigationRow(
                            title = "백업 및 복원",
                            subtitle = ".zip / .json으로 내보내기 및 복원",
                            onClick = { viewModel.setShowBackupRestoreDialog(true) }
                        )
                        DividerLine()
                        SettingsNavigationRow(
                            title = "저장공간 관리",
                            subtitle = "${uiState.storageInfo.sizeMbText} / ${uiState.storageInfo.totalCount}개 항목 사용 중",
                            onClick = { viewModel.refreshStorageInfo() }
                        )
                        DividerLine()
                        SettingsToggleRow(
                            title = "자동 최적화",
                            subtitle = "6개월 이상 된 이미지 압축",
                            checked = userPrefs.autoOptimizeEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    viewModel.setShowOptimizeWarningDialog(true)
                                } else {
                                    viewModel.toggleAutoOptimize(false)
                                }
                            }
                        )
                    }
                }
            }

            SectionDividerBand()

            // ── 섹션 3. AI 인식 및 카메라 ───────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionHeader("AI 인식 및 카메라")
                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp), spotColor = Color.Black.copy(alpha = 0.05f))
                        .background(Color.White, RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                ) {
                    Column {
                        SettingsToggleRow(
                            title = "자동 모서리 인식",
                            subtitle = null,
                            checked = userPrefs.autoCropEnabled,
                            onCheckedChange = { viewModel.toggleAutoCrop(it) }
                        )
                        DividerLine()
                        SettingsToggleRow(
                            title = "흑백 문서 향상",
                            subtitle = null,
                            checked = userPrefs.bwEnhancementEnabled,
                            onCheckedChange = { viewModel.toggleBwEnhancement(it) }
                        )
                        DividerLine()
                        SettingsToggleRow(
                            title = "AI 스마트 카테고리 분류",
                            subtitle = null,
                            checked = userPrefs.aiCategoryEnabled,
                            onCheckedChange = { viewModel.toggleAiCategory(it) }
                        )
                    }
                }
            }

            SectionDividerBand()

            // ── 섹션 4. 기본 설정 ───────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionHeader("기본 설정")
                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp), spotColor = Color.Black.copy(alpha = 0.05f))
                        .background(Color.White, RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                ) {
                    Column {
                        SettingsValueRow(
                            title = "테마",
                            value = userPrefs.appTheme.label,
                            onClick = { viewModel.setShowThemeDialog(true) }
                        )
                        DividerLine()
                        SettingsValueRow(
                            title = "앱 버전",
                            value = "v1.0.0 (최신 버전)",
                            onClick = {}
                        )
                        DividerLine()
                        SettingsNavigationRow(
                            title = "고객 센터 및 라이선스",
                            subtitle = null,
                            onClick = { viewModel.setShowLicenseDialog(true) }
                        )
                    }
                }
            }
        }

        // ── 테마 선택 다이얼로그 ───────────────────────────────────────
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

        // ── 백업 / 복원 다이얼로그 ─────────────────────────────────────
        if (uiState.showBackupRestoreDialog) {
            BackupRestoreDialog(
                isProcessingBackup = uiState.isProcessingBackup,
                isProcessingRestore = uiState.isProcessingRestore,
                onDismiss = { viewModel.setShowBackupRestoreDialog(false) },
                onBackupClick = {
                    backupLauncher.launch("receipt_backup_${LocalDate.now()}.zip")
                },
                onRestoreClick = {
                    restoreLauncher.launch(arrayOf("application/zip"))
                }
            )
        }

        // ── 이미지 자동 최적화 경고 다이얼로그 ───────────────────────
        if (uiState.showOptimizeWarningDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.setShowOptimizeWarningDialog(false) },
                title = { Text("이미지 자동 최적화 안내", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Text(
                        "6개월이 지난 영수증 원본 이미지의 화질을 JPEG 75% 수준으로 압축하여 용량을 확보합니다.\n\n" +
                            "⚠️ 압축된 이미지는 원본 화질로 복구할 수 없습니다. 실행하시겠습니까?",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.toggleAutoOptimize(true)
                        viewModel.executeImageOptimization(context)
                    }) {
                        Text("최적화 실행", color = FabNavy, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.setShowOptimizeWarningDialog(false) }) {
                        Text("취소", color = TextMuted)
                    }
                }
            )
        }

        // ── 오픈소스 라이선스 다이얼로그 ─────────────────────────────
        if (uiState.showLicenseDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.setShowLicenseDialog(false) },
                title = { Text("고객 센터 및 오픈소스 라이선스", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Text(
                        "Receipt Manager v1.0.0\n\n" +
                            "• Hilt, Room, Jetpack Compose, DataStore (Apache 2.0)\n" +
                            "• Lucide Icons, Haze Backdrop Blur (MIT License)\n" +
                            "• ML Kit Korean OCR, Firebase (Google SDK)\n\n" +
                            "문의사항: support@pasic.com",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.setShowLicenseDialog(false) }) {
                        Text("확인", color = FabNavy, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

// ── 세부 서브 컴포넌트 ───────────────────────────────────────────────

@Composable
private fun SectionHeader(text: String) {
    Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
}

@Composable
private fun SectionDividerBand() {
    Spacer(modifier = Modifier.height(16.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(Color(0xFFF1F5F9))
    )
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
private fun DividerLine() {
    Box(modifier = Modifier.fillMaxWidth().height(0.8.dp).background(Color(0xFFE2E8F0)))
}

@Composable
private fun SettingsInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(text = placeholder, fontSize = 14.sp, color = TextMuted)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SegmentedButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color(0xFF2563EB) else TextSecondary
        )
    }
}

@Composable
private fun SettingsNavigationRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
            }
        }
        Icon(Lucide.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsValueRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        Text(text = value, fontSize = 13.sp, color = FabNavy, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.8f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2563EB),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE2E8F0)
            )
        )
    }
}

// ── 다이얼로그 컴포넌트 ─────────────────────────────────────────────

@Composable
private fun ThemeSelectionDialog(
    currentTheme: AppThemeOption,
    onDismiss: () -> Unit,
    onSelect: (AppThemeOption) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("앱 테마 선택", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column {
                AppThemeOption.entries.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(theme) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == theme,
                            onClick = { onSelect(theme) },
                            colors = RadioButtonDefaults.colors(selectedColor = FabNavy)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(theme.label, fontSize = 14.sp, color = TextPrimary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = TextMuted)
            }
        }
    )
}

@Composable
private fun BackupRestoreDialog(
    isProcessingBackup: Boolean,
    isProcessingRestore: Boolean,
    onDismiss: () -> Unit,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("백업 및 복원", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("영수증 DB 내역과 저장된 이미지 원본을 .zip 파일로 안전하게 백업하거나 복원합니다.", fontSize = 13.sp, color = TextSecondary)

                if (isProcessingBackup || isProcessingRestore) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = FabNavy, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(if (isProcessingBackup) "백업 압축 파일 생성 중..." else "데이터 복원 처리 중...", fontSize = 13.sp)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .background(Color(0xFFEFF6FF), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(10.dp))
                                .clickable(onClick = onBackupClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Lucide.Download, null, tint = FabNavy, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("데이터 백업", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FabNavy)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                .clickable(onClick = onRestoreClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Lucide.Upload, null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("백업 복원", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기", color = TextMuted)
            }
        }
    )
}

// ── CSV 핵심 옵션 인라인 컴포넌트 ───────────────────────────────────

@Composable
private fun CsvInlineOptionsContent(
    userPrefs: com.pasic.receipt.data.preferences.UserPreferences,
    onToggleColumn: (String) -> Unit,
    onUpdateDateFormat: (String) -> Unit,
    onUpdateAmountFormat: (String) -> Unit,
    onUpdateZipNamingRule: (String) -> Unit
) {
    var showDetailDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
    ) {
        // 1. 날짜 형식 (스피너 드롭다운)
        Text(
            text = "날짜 형식",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))

        var showDateSpinner by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(Color.White, RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                .clickable { showDateSpinner = true }
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val dateDisplayLabel = when (userPrefs.csvDateFormat) {
                "YYYY-MM-DD" -> "2026-08-12 (YYYY-MM-DD)"
                "YYYY.MM.DD" -> "2026.08.12 (YYYY.MM.DD)"
                "YYYYMMDD" -> "20260812 (YYYYMMDD)"
                else -> userPrefs.csvDateFormat
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(dateDisplayLabel, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                Icon(Lucide.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
            }

            DropdownMenu(
                expanded = showDateSpinner,
                onDismissRequest = { showDateSpinner = false }
            ) {
                listOf(
                    "YYYY-MM-DD" to "2026-08-12 (YYYY-MM-DD)",
                    "YYYY.MM.DD" to "2026.08.12 (YYYY.MM.DD)",
                    "YYYYMMDD" to "20260812 (YYYYMMDD)"
                ).forEach { (fmtKey, label) ->
                    DropdownMenuItem(
                        text = { Text(label, fontSize = 13.sp) },
                        onClick = {
                            onUpdateDateFormat(fmtKey)
                            showDateSpinner = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. 금액 표시 (라디오 버튼 목록)
        Text(
            text = "금액 표시",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUpdateAmountFormat("RAW_NUMBER") }
                    .padding(vertical = 2.dp)
            ) {
                RadioButton(
                    selected = userPrefs.csvAmountFormat == "RAW_NUMBER",
                    onClick = { onUpdateAmountFormat("RAW_NUMBER") },
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2563EB))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("숫자만 (예: 15000) - 계산용", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUpdateAmountFormat("CURRENCY_TEXT") }
                    .padding(vertical = 2.dp)
            ) {
                RadioButton(
                    selected = userPrefs.csvAmountFormat == "CURRENCY_TEXT",
                    onClick = { onUpdateAmountFormat("CURRENCY_TEXT") },
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2563EB))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("통화 기호 포함 (예: 15,000 KRW) - 보고용", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. 포함할 열 & ZIP 파일명 규칙 (세부 설정 다이얼로그 호출 버튼)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                .clickable { showDetailDialog = true }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "⚙️ 포함할 열 & ZIP 파일명 세부 설정",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${userPrefs.csvSelectedColumns.size}개 열 선택 중 · ${userPrefs.zipImageNamingRule}",
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(Lucide.ChevronRight, null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
            }
        }

        // CSV 세부 옵션 다이얼로그
        if (showDetailDialog) {
            CsvDetailOptionsDialog(
                userPrefs = userPrefs,
                onDismiss = { showDetailDialog = false },
                onToggleColumn = onToggleColumn,
                onUpdateZipNamingRule = onUpdateZipNamingRule
            )
        }
    }
}

// ── CSV 포함 열 및 ZIP 규칙 상세 설정 다이얼로그 ───────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CsvDetailOptionsDialog(
    userPrefs: com.pasic.receipt.data.preferences.UserPreferences,
    onDismiss: () -> Unit,
    onToggleColumn: (String) -> Unit,
    onUpdateZipNamingRule: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("⚙️ CSV 포함 열 및 ZIP 규칙 세부 설정", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. 포함할 열 선택 (캡슐 칩)
                Text(
                    text = "CSV 파일에 포함될 열(Column)을 선택하세요.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ALL_CSV_COLUMNS.forEach { col ->
                        val isChecked = userPrefs.csvSelectedColumns.contains(col)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isChecked) Color(0xFF2563EB) else Color.White)
                                .then(
                                    if (!isChecked) Modifier.border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                                    else Modifier
                                )
                                .clickable { onToggleColumn(col) }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = col,
                                fontSize = 12.sp,
                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                color = if (isChecked) Color.White else TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2. 영수증 이미지 압축(ZIP) 파일명 규칙
                Text(
                    text = "영수증 이미지 (ZIP) 파일명 규칙",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                var patternText by remember(userPrefs.zipImageNamingRule) {
                    mutableStateOf(userPrefs.zipImageNamingRule)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = patternText,
                        onValueChange = {
                            patternText = it
                            onUpdateZipNamingRule(it)
                        },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "+ {date}" to "{date}",
                        "+ {merchant}" to "{merchant}",
                        "+ {amount}" to "{amount}",
                        "+ {index}" to "{index}"
                    ).forEach { (chipLabel, tagValue) ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFEFF6FF), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(6.dp))
                                .clickable {
                                    val newPattern = patternText + tagValue
                                    patternText = newPattern
                                    onUpdateZipNamingRule(newPattern)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(chipLabel, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF2563EB))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val samplePreview = patternText
                    .replace("{date}", "20260812")
                    .replace("{merchant}", "스타벅스")
                    .replace("{amount}", "15000")
                    .replace("{index}", "01")
                    .ifEmpty { "receipt_01" } + ".jpg"

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "📄 생성될 파일명 예시: $samplePreview",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2563EB)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("설정 완료", color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
            }
        }
    )
}
