package com.pasic.receipt.ui.export

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pasic.receipt.R
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mail
import com.composables.icons.lucide.MessageSquare
import com.composables.icons.lucide.Sheet
import com.pasic.receipt.ui.components.DateRangePickerBottomSheet
import com.pasic.receipt.ui.theme.FabNavy
import com.pasic.receipt.ui.theme.TextMuted
import com.pasic.receipt.ui.theme.TextPrimary
import com.pasic.receipt.ui.theme.TextSecondary
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlin.math.min
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val SCROLL_THRESHOLD_PX = 80f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    hazeState: HazeState = remember { HazeState() },
    onScrollProgressChanged: (Float) -> Unit = {},
    viewModel: ExportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showDatePickerSheet by remember { mutableStateOf(false) }

    val rawProgress by remember {
        derivedStateOf { min(scrollState.value / SCROLL_THRESHOLD_PX, 1f) }
    }

    LaunchedEffect(rawProgress) {
        onScrollProgressChanged(rawProgress)
    }

    val now = YearMonth.now()
    val periodLabel = now.format(DateTimeFormatter.ofPattern("yyyy년 M월"))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 120.dp)
        ) {

            // ── 섹션 1. 조회 기간 선택 카드 ───────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(18.dp),
                        spotColor = Color(0x120F172A),
                        ambientColor = Color(0x080F172A)
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(18.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showDatePickerSheet = true
                    }
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 좌측 텍스트 영역 (조회 기간 + 2026. 08. 01 ~ 08. 31)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "조회 기간",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.formattedDateRangeText,
                            fontSize = 16.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 우측 달력 아이콘 (배경 없이 우측 끝에 배치)
                    Icon(
                        imageVector = Lucide.Calendar,
                        contentDescription = "기간 선택",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── 섹션 2. 파일 형식 선택 ─────────────────────────────────
            SectionLabel("파일 형식 선택")
            Spacer(modifier = Modifier.height(10.dp))

            // CSV 카드
            FormatCard(
                selected = uiState.selectedFormat == ExportFormat.EXCEL,
                onClick = { viewModel.selectFormat(ExportFormat.EXCEL) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFD1FAE5), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Lucide.Sheet, null, tint = Color(0xFF059669), modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("CSV (.csv)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("데이터 집계 및 분석용", fontSize = 12.sp, color = TextSecondary)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                val cols = uiState.csvHeaderColumns
                val previewCols = cols.take(4)
                val extraCount = cols.size - previewCols.size

                Text(
                    text = "포함된 열 (${cols.size}개)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    previewCols.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (extraCount > 0) {
                        Box(
                            modifier = Modifier
                                .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "+$extraCount",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // PDF 카드
            FormatCard(
                selected = uiState.selectedFormat == ExportFormat.PDF,
                onClick = { viewModel.selectFormat(ExportFormat.PDF) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFFE4E6), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Lucide.FileText, null, tint = Color(0xFFE11D48), modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PDF 지출결의서", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("증빙 및 제출용 문서", fontSize = 12.sp, color = TextSecondary)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))

                // PDF 미리보기 박스 (상단 여백 0 + 가로 100% 꽉 채움 + 하단 잘림 페이드 효과)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_pdf_preview),
                        contentDescription = "PDF 지출결의서 미리보기",
                        contentScale = ContentScale.FillWidth,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // 하단 은은한 페이드아웃 그라데이션 레이어
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0x88FFFFFF), Color.White)
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── 섹션 3. 상세 옵션 ────────────────────────────────────
            SectionLabel("상세 옵션")
            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            ) {
                val isCsv = uiState.selectedFormat == ExportFormat.EXCEL

                Column {
                    ExportOptionRow(
                        label = "전체 포함",
                        checked = uiState.includeAll,
                        onCheckedChange = { viewModel.toggleIncludeAll(it) }
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF1F5F9)))
                    ExportOptionRow(
                        label = "카테고리별 분리",
                        checked = uiState.groupByCategory,
                        onCheckedChange = { viewModel.toggleGroupByCategory(it) }
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF1F5F9)))
                    ExportOptionRow(
                        label = "영수증 이미지 포함",
                        checked = uiState.includeImages,
                        enabled = isCsv,
                        badgeText = if (isCsv) "ZIP 압축" else "CSV 전용",
                        onCheckedChange = { viewModel.toggleIncludeImages(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── 섹션 4. 보고서 생성 및 다운로드 버튼 ─────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(FabNavy)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (uiState.selectedFormat == ExportFormat.PDF) {
                            viewModel.onGenerateClicked()
                        } else {
                            viewModel.generateCsv(context)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isGenerating) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Lucide.Download, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "보고서 생성 및 다운로드",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 하단 안내 칩 (다운로드 버튼 바로 아래) ───────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "ℹ️  현재 ${uiState.targetReceiptCount}건의 영수증이 내보내기 대기 중입니다." +
                        if (uiState.selectedFormat == ExportFormat.PDF && uiState.includeImages) " PDF 내보내기 시 파일 용량이 커질 수 있습니다." else "",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── 섹션 5. 바로 공유하기 (양옆 얇은 구분선) ────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Color(0xFFE2E8F0))
                )
                Text(
                    text = "또는 바로 공유하기",
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Color(0xFFE2E8F0))
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 이메일 전송
                QuickShareButton(
                    label = "이메일 전송",
                    bgColor = Color(0xFFDBEAFE),
                    iconColor = Color(0xFF1D4ED8),
                    icon = { Icon(Lucide.Mail, null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(22.dp)) },
                    onClick = { viewModel.shareViaEmail(context) }
                )
                Spacer(modifier = Modifier.width(24.dp))
                // 메신저 공유
                QuickShareButton(
                    label = "메신저 공유",
                    bgColor = Color(0xFFFEF3C7),
                    iconColor = Color(0xFFD97706),
                    icon = { Icon(Lucide.MessageSquare, null, tint = Color(0xFFD97706), modifier = Modifier.size(22.dp)) },
                    onClick = { viewModel.shareViaMessenger(context) }
                )
            }
        }

        // ── PDF 정보 입력 바텀시트 ─────────────────────────────────────
        if (uiState.showPdfInfoSheet) {
            PdfInfoBottomSheet(
                onDismiss = { viewModel.dismissPdfInfoSheet() },
                onConfirm = { author, dept, purpose ->
                    viewModel.generatePdf(context, author, dept, purpose)
                }
            )
        }

        // ── 달력 기간 선택 바텀시트 ───────────────────────────────────
        if (showDatePickerSheet) {
            DateRangePickerBottomSheet(
                onDismissRequest = { showDatePickerSheet = false },
                onRangeSelected = { startDate, endDate ->
                    viewModel.setCustomDateRange(startDate, endDate)
                    showDatePickerSheet = false
                }
            )
        }
    }
}

// ── 공통 컴포넌트 ─────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary
    )
}

@Composable
private fun PeriodButton(
    text: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (selected) Color(0xFF2563EB) else Color(0xFFE2E8F0)
    val bgColor = if (selected) Color(0xFFEFF6FF) else Color.White
    val textColor = if (selected) Color(0xFF1D4ED8) else TextSecondary

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        icon()
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textColor)
    }
}

@Composable
private fun FormatCard(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val borderColor = if (selected) Color(0xFF2563EB) else Color(0xFFE2E8F0)
    val bgColor = if (selected) Color(0xFFFAFDFF) else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
        // 선택 체크마크
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(Color(0xFF2563EB), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.Check,
                    contentDescription = "선택됨",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun ExportOptionRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    badgeText: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    val textColor = if (enabled) TextPrimary else TextMuted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = textColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (badgeText != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        Checkbox(
            checked = checked && enabled,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = FabNavy,
                uncheckedColor = Color(0xFFCBD5E1),
                disabledCheckedColor = Color(0xFFCBD5E1),
                disabledUncheckedColor = Color(0xFFE2E8F0),
                checkmarkColor = Color.White
            )
        )
    }
}

@Composable
private fun QuickShareButton(
    label: String,
    bgColor: Color,
    iconColor: Color,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(bgColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, fontSize = 11.sp, color = TextSecondary)
    }
}
