package com.pasic.receipt.ui.scan

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.RefreshCw
import com.pasic.receipt.ui.scan.result.AddCategoryBottomSheet
import com.pasic.receipt.ui.scan.result.AiCategorySuggestionCard
import com.pasic.receipt.ui.scan.result.ConfidenceBadge
import com.pasic.receipt.ui.scan.result.IosGlassTextField
import com.pasic.receipt.ui.scan.result.LightboxDialog
import com.pasic.receipt.ui.scan.result.ReceiptPreviewCard
import com.pasic.receipt.ui.scan.result.ReceiptSaveSuccessDialog
import com.pasic.receipt.ui.scan.result.TaxWarningBanner
import com.pasic.receipt.ui.scan.result.parseHexColor
import com.pasic.receipt.ui.theme.ScreenBackground

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

    // AI Category Suggestion state
    val suggestedCategory = ocrResult?.suggestedNewCategory ?: ""
    var isAiCatSuggestionDismissed by remember(ocrResult) { mutableStateOf(false) }
    val showAiCategoryCard = suggestedCategory.isNotBlank() &&
            uiState.customCategories.none { it.name == suggestedCategory } &&
            !isAiCatSuggestionDismissed

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
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

            // 4. AI New Category Recommendation Banner
            if (showAiCategoryCard) {
                AiCategorySuggestionCard(
                    merchantName = merchantName,
                    suggestedCategory = suggestedCategory,
                    onCreateCategory = {
                        viewModel.addCustomCategory(suggestedCategory, "#F3E8FF")
                        selectedCategory = suggestedCategory
                        selectedCategoryColor = "#F3E8FF"
                        isAiCatSuggestionDismissed = true
                        markEdited()
                    },
                    onCustomInput = {
                        showAddCategorySheet = true
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 5. Form Fields
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

                // 카테고리 칩 그룹
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

            // 6. Bottom Action Buttons
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
                        viewModel.saveReceiptToDatabase(
                            merchantName = merchantName,
                            date = dateString,
                            amount = totalAmountDouble,
                            currency = currency,
                            businessNumber = businessNumber,
                            confidence = confidenceScore,
                            category = selectedCategory,
                            categoryColor = selectedCategoryColor,
                            imagePath = ocrResult?.imagePath ?: ""
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
                    viewModel.addCustomCategory(newCategoryName, newColorHex)
                    selectedCategory = newCategoryName
                    selectedCategoryColor = newColorHex
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
