package com.pasic.receipt.ui.scan.result

import android.graphics.BitmapFactory
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasic.receipt.R
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Scan
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.TriangleAlert
import com.composables.icons.lucide.ZoomIn
import java.io.File
import java.text.NumberFormat
import java.util.Locale

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
fun AiCategorySuggestionCard(
    merchantName: String,
    suggestedCategory: String,
    onCreateCategory: () -> Unit,
    onCustomInput: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BubbleFloating")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BubbleOffsetY"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_bubble_small),
                    contentDescription = "AI 카테고리 추천 아이콘",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(36.dp)
                        .graphicsLayer {
                            translationY = offsetY.dp.toPx()
                        }
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "새로운 카테고리가 필요해 보여요.",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "'${merchantName.ifBlank { "사용처" }}' 사용처 분석 결과,\n#$suggestedCategory 카테고리를 추천해요.",
                        fontSize = 14.sp,
                        color = Color(0xFF475569),
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onCreateCategory,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "+ 추천 카테고리 바로 만들기",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onCustomInput,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF1F5F9),
                    contentColor = Color(0xFF475569)
                ),
                shape = RoundedCornerShape(14.dp),
                elevation = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(
                    text = "직접 입력하기",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475569)
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
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Column {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                readOnly = readOnly || (onClick != null),
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
                trailingIcon = trailingIcon,
                modifier = Modifier.fillMaxWidth()
            )

            if (onClick != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onClick)
                )
            }
        }
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
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 16.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "카테고리 추가",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(20.dp))

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

fun getCategoryBorderColor(hex: String): Color {
    return when (hex.uppercase().trim()) {
        "#DBEAFE" -> Color(0xFF2563EB) // 연파랑 ➔ 블루
        "#DCFCE7" -> Color(0xFF16A34A) // 연초록 ➔ 그린
        "#FEF3C7" -> Color(0xFFD97706) // 연노랑 ➔ 앰버
        "#FCE7F3", "#FFE4E6" -> Color(0xFFE11D48) // 연핑크 ➔ 로즈
        "#F3E8FF", "#E0E7FF" -> Color(0xFF9333EA) // 연보라 ➔ 퍼플
        "#CCFBF1" -> Color(0xFF0D9488) // 연틸 ➔ 틸
        else -> Color(0xFF2563EB) // 기본 ➔ 메인 블루
    }
}
