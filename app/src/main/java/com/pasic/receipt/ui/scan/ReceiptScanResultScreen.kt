package com.pasic.receipt.ui.scan

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Scan
import com.composables.icons.lucide.TriangleAlert
import com.composables.icons.lucide.ZoomIn
import com.pasic.receipt.R
import com.pasic.receipt.ui.theme.ScreenBackground
import java.io.File
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScanResultScreen(
    viewModel: ScanSharedViewModel,
    onNavigateBackToScan: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ocrResult = uiState.ocrResult

    // Editable form states
    var merchantName by remember(ocrResult) { mutableStateOf(ocrResult?.merchantName ?: "") }
    var dateString by remember(ocrResult) { mutableStateOf(ocrResult?.date ?: "") }
    var amountString by remember(ocrResult) {
        val num = ocrResult?.totalAmount?.toInt() ?: 0
        mutableStateOf(if (num == 0) "" else num.toString())
    }
    var currency by remember(ocrResult) { mutableStateOf(ocrResult?.currency ?: "KRW") }
    var businessNumber by remember(ocrResult) { mutableStateOf(ocrResult?.businessNumber ?: "") }
    var selectedCategory by remember(ocrResult) { mutableStateOf(ocrResult?.category ?: "미분류") }
    var selectedCategoryColor by remember(ocrResult) { mutableStateOf(ocrResult?.categoryColor ?: "#F1F5F9") }
    var confidenceScore by remember(ocrResult) { mutableStateOf(ocrResult?.confidenceScore ?: 30) }

    // Dialog & BottomSheet Visibility states
    var showLightbox by remember { mutableStateOf(false) }
    var showAddCategorySheet by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Mark 100% confidence when user edits any field
    fun markEdited() {
        if (confidenceScore < 100) {
            confidenceScore = 100
        }
    }

    val totalAmountDouble = amountString.toDoubleOrNull() ?: 0.0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Header Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBackToScan) {
                    Icon(
                        imageVector = Lucide.ArrowLeft,
                        contentDescription = "뒤로가기",
                        tint = Color(0xFF1E293B)
                    )
                }

                Text(
                    text = "스캔 결과 검증",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.width(48.dp)) // balance layout
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Top Receipt Preview Card with Lightbox button
            ReceiptPreviewCard(
                imagePath = ocrResult?.imagePath ?: "",
                onZoomClick = { showLightbox = true }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Dynamic Accuracy Confidence Badge
            ConfidenceBadge(score = confidenceScore)

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Tax Warning Banner (> 30,000 KRW)
            if (totalAmountDouble > 30000) {
                TaxWarningBanner(amount = totalAmountDouble)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 4. iOS Liquid Glass Editor Form Fields
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 상호명
                IosGlassTextField(
                    label = "상호명",
                    value = merchantName,
                    onValueChange = {
                        merchantName = it
                        markEdited()
                    }
                )

                // 일시
                IosGlassTextField(
                    label = "결제 일시",
                    value = dateString,
                    onValueChange = {
                        dateString = it
                        markEdited()
                    }
                )

                // 합계 금액
                IosGlassTextField(
                    label = "합계 금액 ($currency)",
                    value = amountString,
                    onValueChange = {
                        amountString = it.filter { char -> char.isDigit() }
                        markEdited()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // 사업자등록번호
                IosGlassTextField(
                    label = "사업자등록번호",
                    value = businessNumber,
                    onValueChange = {
                        businessNumber = it
                        markEdited()
                    }
                )

                // 카테고리 칩 그룹 (아이콘 없음)
                Text(
                    text = "카테고리",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(start = 4.dp)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    uiState.customCategories.forEach { categoryItem ->
                        val isSelected = selectedCategory == categoryItem.name
                        Surface(
                            onClick = {
                                selectedCategory = categoryItem.name
                                selectedCategoryColor = categoryItem.colorHex
                                markEdited()
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) parseHexColor(categoryItem.colorHex) else Color.White,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = categoryItem.name,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF1E293B) else Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    // [+] 추가 칩
                    Surface(
                        onClick = { showAddCategorySheet = true },
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.Plus,
                                contentDescription = "카테고리 추가",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "추가",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 5. Bottom Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBackToScan,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF475569)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Icon(
                        imageVector = Lucide.RefreshCw,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "다시 촬영",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        viewModel.saveReceipt(
                            merchantName = merchantName,
                            date = dateString,
                            amount = totalAmountDouble,
                            currency = currency,
                            businessNumber = businessNumber,
                            category = selectedCategory,
                            categoryColor = selectedCategoryColor,
                            imagePath = ocrResult?.imagePath ?: "",
                            confidence = confidenceScore
                        )
                        showSuccessDialog = true
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F172A),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(54.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "저장하기",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Lightbox Image Dialog
        if (showLightbox) {
            LightboxDialog(
                imagePath = ocrResult?.imagePath ?: "",
                onDismiss = { showLightbox = false }
            )
        }

        // Add Category Bottom Sheet
        if (showAddCategorySheet) {
            AddCategoryBottomSheet(
                onDismiss = { showAddCategorySheet = false },
                onCategorySaved = { newCategoryName, newColorHex ->
                    val created = viewModel.addCustomCategory(newCategoryName, newColorHex)
                    selectedCategory = created.name
                    selectedCategoryColor = created.colorHex
                    markEdited()
                    showAddCategorySheet = false
                }
            )
        }

        // Success Dialog
        if (showSuccessDialog) {
            ReceiptSaveSuccessDialog(
                onDismiss = {
                    showSuccessDialog = false
                    onSaveSuccess()
                }
            )
        }
    }
}

@Composable
fun ReceiptPreviewCard(
    imagePath: String,
    onZoomClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val bitmap = remember(imagePath) {
                if (imagePath.isNotBlank() && File(imagePath).exists()) {
                    BitmapFactory.decodeFile(imagePath)
                } else null
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "영수증 이미지",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Fallback Demo Receipt Illustration
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Lucide.Scan,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "스캔 완료된 영수증 원본",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }

            // Lightbox Zoom button overlay
            Surface(
                onClick = onZoomClick,
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Lucide.ZoomIn,
                        contentDescription = "원본 보기",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "원본 보기",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ConfidenceBadge(score: Int) {
    val bgColor: Color
    val textColor: Color
    val icon: androidx.compose.ui.graphics.vector.ImageVector
    val label: String

    when {
        score >= 85 -> {
            bgColor   = Color(0xFFDCFCE7)
            textColor = Color(0xFF15803D)
            icon      = Lucide.CircleCheck
            label     = "정확도 높음 ($score%) — 바로 저장 가능"
        }
        score >= 60 -> {
            bgColor   = Color(0xFFFEF3C7)
            textColor = Color(0xFFB45309)
            icon      = Lucide.RefreshCw
            label     = "정확도 보통 ($score%) — 주요 필드 확인 권장"
        }
        else -> {
            bgColor   = Color(0xFFFEE2E2)
            textColor = Color(0xFFB91C1C)
            icon      = Lucide.TriangleAlert
            label     = "정확도 낮음 ($score%) — 전체 필드 확인 필요"
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
fun TaxWarningBanner(amount: Double) {
    val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
    val amountFormatted = formatter.format(amount.toInt())

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFFF7ED),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFEDD5)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(14.dp)
        ) {
            Icon(
                imageVector = Lucide.TriangleAlert,
                contentDescription = null,
                tint = Color(0xFFEA580C),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "건당 3만원 초과 지출 경고 (${amountFormatted}원)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC2410C)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "세법상 3만원 초과 지출은 적격증빙(신용카드, 현금영수증) 실물 보관 및 사업자등록번호 확인이 필수입니다.",
                    fontSize = 12.sp,
                    color = Color(0xFF9A3412),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun IosGlassTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFF8FAFC),
                focusedBorderColor = Color(0xFF2563EB),
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedTextColor = Color(0xFF0F172A),
                unfocusedTextColor = Color(0xFF1E293B)
            ),
            keyboardOptions = keyboardOptions,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun LightboxDialog(
    imagePath: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        offset += offsetChange
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            val bitmap = remember(imagePath) {
                if (imagePath.isNotBlank() && File(imagePath).exists()) {
                    BitmapFactory.decodeFile(imagePath)
                } else null
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "확대 보기",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .transformable(state = transformState)
                )
            } else {
                Text(
                    text = "영수증 원본 이미지 (핀치 투 줌 가능)",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Lucide.ArrowLeft,
                    contentDescription = "닫기",
                    tint = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryBottomSheet(
    onDismiss: () -> Unit,
    onCategorySaved: (String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var categoryName by remember { mutableStateOf("") }

    val colorSwatches = listOf(
        "#DBEAFE", // 연파랑
        "#DCFCE7", // 연초록
        "#FEF3C7", // 연노랑
        "#FCE7F3", // 연핑크
        "#F3E8FF"  // 연보라
    )
    var selectedColorHex by remember { mutableStateOf(colorSwatches[0]) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "카테고리 추가",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 1. 미리보기 섹션
            Text(
                text = "미리보기",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = parseHexColor(selectedColorHex),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        ) {
                            Text(
                                text = if (categoryName.isBlank()) "#카테고리" else "#$categoryName",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. 카테고리 이름 입력
            Text(
                text = "카테고리 이름",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                placeholder = { Text("예: 식비, 교통비", color = Color(0xFF94A3B8)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3. 색상 선택 원형 스와치 5종
            Text(
                text = "색상 선택",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                colorSwatches.forEach { hex ->
                    val isSelected = selectedColorHex == hex
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(parseHexColor(hex))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color(0xFF0F172A) else Color(0xFFCBD5E1),
                                shape = CircleShape
                            )
                            .clickable { selectedColorHex = hex }
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Lucide.Check,
                                contentDescription = null,
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 4. 저장하기 버튼
            Button(
                onClick = {
                    if (categoryName.isNotBlank()) {
                        onCategorySaved(categoryName.trim(), selectedColorHex)
                    }
                },
                enabled = categoryName.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "저장하기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ReceiptSaveSuccessDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("확인", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Lucide.CircleCheck,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "영수증 저장 완료",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A)
                )
            }
        },
        text = {
            Text(
                text = "스캔한 영수증 내역이 성공적으로 저장되었습니다.\n홈 및 영수증 목록에 즉시 반영됩니다.",
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}

fun parseHexColor(hex: String): Color {
    return try {
        val cleaned = hex.removePrefix("#")
        val colorInt = cleaned.toLong(16).toInt() or 0xFF000000.toInt()
        Color(colorInt)
    } catch (e: Exception) {
        Color(0xFFFEF3C7)
    }
}
