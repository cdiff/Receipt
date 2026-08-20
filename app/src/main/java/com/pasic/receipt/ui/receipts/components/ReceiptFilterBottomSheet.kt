package com.pasic.receipt.ui.receipts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasic.receipt.data.local.entity.ReceiptEntity
import com.pasic.receipt.ui.receipts.ReceiptFilterOptions
import com.pasic.receipt.ui.receipts.SortOrder
import com.pasic.receipt.ui.theme.BorderLight
import com.pasic.receipt.ui.theme.BrandPrimary
import com.pasic.receipt.ui.theme.CardBackground
import com.pasic.receipt.ui.theme.SecondaryCardBackground
import com.pasic.receipt.ui.theme.TextMuted
import com.pasic.receipt.ui.theme.TextPrimary
import com.pasic.receipt.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptFilterBottomSheet(
    currentOptions: ReceiptFilterOptions,
    allReceipts: List<ReceiptEntity>,
    onApply: (ReceiptFilterOptions) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var draftOptions by remember(currentOptions) { mutableStateOf(currentOptions) }
    val haptics = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    // 실시간 조건 매칭 건수 계산
    val matchingCount = remember(allReceipts, draftOptions) {
        allReceipts.count { receipt ->
            val matchesPayment = when (draftOptions.paymentMethod) {
                "전체" -> true
                "카드" -> receipt.paymentMethod.contains("카드", ignoreCase = true)
                "현금" -> receipt.paymentMethod.contains("현금", ignoreCase = true)
                "간편결제" -> receipt.paymentMethod.contains("간편", ignoreCase = true) || receipt.paymentMethod.contains("페이", ignoreCase = true)
                else -> receipt.paymentMethod.equals(draftOptions.paymentMethod, ignoreCase = true)
            }
            val matchesProof = if (draftOptions.proofType == "전체") true else {
                receipt.proofType.equals(draftOptions.proofType, ignoreCase = true)
            }
            val matchesImage = if (!draftOptions.hasImageOnly) true else {
                receipt.imagePath.isNotBlank()
            }
            matchesPayment && matchesProof && matchesImage
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 14.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(BorderLight)
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // ── 상단 타이틀 ───────────────────────────────────────────────
            Text(
                text = "상세 필터 및 정렬",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 6.dp)
            )

            // ── 본문 스크롤 영역 ───────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. 정렬 기준 섹션 (2x2 그리드)
                FilterSection(title = "정렬 기준") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SortGridButton(
                                label = SortOrder.DATE_DESC.label,
                                isSelected = draftOptions.sortOrder == SortOrder.DATE_DESC,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    draftOptions = draftOptions.copy(sortOrder = SortOrder.DATE_DESC)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            SortGridButton(
                                label = SortOrder.DATE_ASC.label,
                                isSelected = draftOptions.sortOrder == SortOrder.DATE_ASC,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    draftOptions = draftOptions.copy(sortOrder = SortOrder.DATE_ASC)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SortGridButton(
                                label = SortOrder.AMOUNT_DESC.label,
                                isSelected = draftOptions.sortOrder == SortOrder.AMOUNT_DESC,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    draftOptions = draftOptions.copy(sortOrder = SortOrder.AMOUNT_DESC)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            SortGridButton(
                                label = SortOrder.AMOUNT_ASC.label,
                                isSelected = draftOptions.sortOrder == SortOrder.AMOUNT_ASC,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    draftOptions = draftOptions.copy(sortOrder = SortOrder.AMOUNT_ASC)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 2. 결제 수단 섹션 (칩 목록: 전체, 카드, 현금, 간편결제)
                FilterSection(title = "결제 수단") {
                    val paymentOptions = listOf("전체", "카드", "현금", "간편결제")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(paymentOptions) { method ->
                            FilterSelectChip(
                                label = method,
                                isSelected = draftOptions.paymentMethod == method,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    draftOptions = draftOptions.copy(paymentMethod = method)
                                }
                            )
                        }
                    }
                }

                // 3. 증빙 유형 섹션 (칩 목록: 전체, 일반영수증, 현금영수증, 세금계산서)
                FilterSection(title = "증빙 유형") {
                    val proofOptions = listOf("전체", "일반영수증", "현금영수증", "세금계산서")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(proofOptions) { proof ->
                            FilterSelectChip(
                                label = proof,
                                isSelected = draftOptions.proofType == proof,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    draftOptions = draftOptions.copy(proofType = proof)
                                }
                            )
                        }
                    }
                }

                // 4. 사진 유무 섹션 (토글 스위치)
                FilterSection(title = "사진 유무") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "영수증 사진 있음",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = draftOptions.hasImageOnly,
                            onCheckedChange = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                draftOptions = draftOptions.copy(hasImageOnly = it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CardBackground,
                                checkedTrackColor = BrandPrimary,
                                uncheckedThumbColor = CardBackground,
                                uncheckedTrackColor = BorderLight,
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            }

            // ── 하단 고정 액션 바 ([ 초기화 ] + [ N건의 영수증 보기 ]) ─────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 초기화 (누르는 즉시 기본값 확정 적용 후 바텀시트 닫힘)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            val defaultOptions = ReceiptFilterOptions()
                            draftOptions = defaultOptions
                            onApply(defaultOptions)
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "초기화",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }

                // 확정 적용 버튼 ([ N건의 영수증 보기 ], weight 2f)
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandPrimary)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onApply(draftOptions)
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${matchingCount}건의 영수증 보기",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * 섹션 헤더 및 콘텐츠 컨테이너
 */
@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
        content()
    }
}

/**
 * 2x2 정렬 그리드 버튼
 */
@Composable
private fun SortGridButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) SecondaryCardBackground else CardBackground)
            .border(
                width = if (isSelected) 1.2.dp else 1.dp,
                color = if (isSelected) BrandPrimary else BorderLight,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) BrandPrimary else TextPrimary
        )
    }
}

/**
 * 결제 수단 & 증빙 유형 선택 칩 (타원형 캡슐 디자인)
 */
@Composable
private fun FilterSelectChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) BrandPrimary else CardBackground)
            .border(
                width = 1.dp,
                color = if (isSelected) BrandPrimary else BorderLight,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) CardBackground else TextPrimary
        )
    }
}
