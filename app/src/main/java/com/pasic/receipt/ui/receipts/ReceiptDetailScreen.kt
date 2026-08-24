package com.pasic.receipt.ui.receipts

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.Lucide
import com.pasic.receipt.data.local.entity.ReceiptEntity
import com.pasic.receipt.ui.receipts.components.ImageZoomDialog
import com.pasic.receipt.ui.receipts.components.ReceiptDetailHeader
import com.pasic.receipt.ui.receipts.components.ReceiptPostItMemoCard
import com.pasic.receipt.ui.receipts.components.ReceiptSavingTipCard
import com.pasic.receipt.ui.receipts.components.ReceiptTransactionSpecCard
import com.pasic.receipt.ui.receipts.components.SaveMemoConfirmationDialog
import kotlinx.coroutines.delay
import java.io.File
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailScreen(
    receiptId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit = {},
    viewModel: ReceiptDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(receiptId) {
        viewModel.loadReceipt(receiptId)
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            com.pasic.receipt.util.ToastEventBus.showToast("영수증이 삭제되었습니다.")
            onNavigateBack()
        }
    }

    var showMenu by remember { mutableStateOf(false) }
    var showImageZoomDialog by remember { mutableStateOf(false) }

    val receipt = uiState.receipt

    // 비동기 이미지 로딩
    val imageBitmap = remember(receipt?.imagePath) {
        val path = receipt?.imagePath ?: ""
        if (path.isNotBlank()) {
            runCatching {
                val file = File(path)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } else null
            }.getOrNull()
        } else null
    }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var isTipAnimatedVisible by remember { mutableStateOf(false) }

    // 메모 상태 관리 및 수정 여부 감지
    var editableMemo by remember { mutableStateOf("") }

    LaunchedEffect(receipt?.memo) {
        if (receipt != null) {
            editableMemo = receipt.memo ?: ""
        }
    }

    val hasUnsavedMemoChanges = remember(editableMemo, receipt?.memo) { editableMemo != (receipt?.memo ?: "") }
    var showSaveDialog by remember { mutableStateOf(false) }

    // 시스템 뒤로가기 / 탑바 뒤로가기 처리
    BackHandler(enabled = hasUnsavedMemoChanges) {
        showSaveDialog = true
    }

    val handleBackPress = {
        if (hasUnsavedMemoChanges) {
            showSaveDialog = true
        } else {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.isAboveAverage) {
        if (uiState.isAboveAverage) {
            delay(1000)
            isTipAnimatedVisible = true
        } else {
            isTipAnimatedVisible = false
        }
    }

    // 스크롤 (0 ~ 140px) 기준 헤더 축소 진행도
    val collapseProgress by remember {
        derivedStateOf {
            (scrollState.value.toFloat() / 140f).coerceIn(0f, 1f)
        }
    }
    val animatedProgress by animateFloatAsState(
        targetValue = collapseProgress,
        animationSpec = tween(durationMillis = 100),
        label = "headerCollapseAnimation"
    )

    val headerHeight = (56 - (8 * animatedProgress)).dp
    val titleFontSize = (18 - (2 * animatedProgress)).sp
    val titleHorizontalBias = -0.75f * (1f - animatedProgress)

    Scaffold(
        containerColor = Color.White,
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // 키보드 이중 여백 방지를 위한 윈도우 인셋 초기화
        topBar = {
            Surface(
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 48.dp),
                        contentAlignment = BiasAlignment(horizontalBias = titleHorizontalBias, verticalBias = 0f)
                    ) {
                        Text(
                            text = "영수증 상세",
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1
                        )
                    }

                    IconButton(
                        onClick = handleBackPress,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Lucide.ArrowLeft,
                            contentDescription = "뒤로가기",
                            tint = Color(0xFF0F172A)
                        )
                    }

                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Lucide.EllipsisVertical,
                                contentDescription = "더보기 메뉴",
                                tint = Color(0xFF0F172A)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("공유하기", color = Color(0xFF0F172A)) },
                                onClick = {
                                    showMenu = false
                                    receipt?.let { shareReceiptInfo(context, it) }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("삭제하기", color = Color(0xFFEF4444)) },
                                onClick = {
                                    showMenu = false
                                    viewModel.deleteReceipt()
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (receipt == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uiState.isLoading) "영수증 정보를 불러오는 중..." else "영수증을 찾을 수 없습니다.",
                    color = Color(0xFF64748B),
                    fontSize = 15.sp
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .navigationBarsPadding()
                    .imePadding()
                    .background(Color.White)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. 헤더 영역 (이미지 프리뷰, 상호명, 일시, 금액, 카테고리 칩)
                ReceiptDetailHeader(
                    receipt = receipt,
                    imageBitmap = imageBitmap,
                    onImageClick = { showImageZoomDialog = true }
                )

                // 2. 절약 팁 카드 (조건 만족 시 3D 아이콘 및 애니메이션 노출)
                ReceiptSavingTipCard(
                    isVisible = isTipAnimatedVisible,
                    receipt = receipt,
                    categoryAverageAmount = uiState.categoryAverageAmount,
                    onCloseClick = { isTipAnimatedVisible = false }
                )

                // 3. 상세 거래 명세 섹션 (결제수단, 증빙유형, 사업자번호 복사, 부가세, AI 신뢰도)
                ReceiptTransactionSpecCard(
                    receipt = receipt
                )

                // 4. 메모 섹션 (연노랑 포스트잇 노트 카드)
                ReceiptPostItMemoCard(
                    memoText = editableMemo,
                    onMemoChange = { editableMemo = it },
                    onFocused = {
                        coroutineScope.launch {
                            delay(200)
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    }
                )
            }
        }
    }

    // 메모 수정 저장 확인 다이얼로그
    SaveMemoConfirmationDialog(
        showDialog = showSaveDialog,
        onSaveConfirm = {
            viewModel.updateMemo(editableMemo)
            showSaveDialog = false
            onNavigateBack()
        },
        onDiscardChanges = {
            showSaveDialog = false
            onNavigateBack()
        },
        onCancel = { showSaveDialog = false }
    )

    // 영수증 원본 이미지 확대보기 모달
    ImageZoomDialog(
        showDialog = showImageZoomDialog,
        imageBitmap = imageBitmap,
        onDismiss = { showImageZoomDialog = false }
    )
}

private fun shareReceiptInfo(context: Context, receipt: ReceiptEntity) {
    try {
        val shareText = """
            [영수증 정보]
            • 상호명: ${receipt.merchantName}
            • 일시: ${receipt.date}
            • 금액: ${DecimalFormat("#,###").format(receipt.totalAmount.toLong())}원
            • 결제수단: ${receipt.paymentMethod}
            • 카테고리: ${receipt.category}
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "영수증 정보 공유하기"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
