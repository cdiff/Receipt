package com.pasic.receipt.ui.receipts.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Lucide

@Composable
fun ReceiptPostItMemoCard(
    memoText: String,
    onMemoChange: (String) -> Unit,
    onFocused: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFFEF9C3),
        shadowElevation = 0.5.dp,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 110.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 우측 상단 모서리 접힌 종이 (Dog-ear folded corner) 음영 효과
            Canvas(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp)
            ) {
                val foldPath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(
                    path = foldPath,
                    color = Color(0xFFEAB308).copy(alpha = 0.35f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 상단 타이틀
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Lucide.FileText,
                        contentDescription = null,
                        tint = Color(0xFF334155),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "메모",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF334155)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                var textState by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(text = memoText, selection = androidx.compose.ui.text.TextRange(memoText.length))) }

                androidx.compose.runtime.LaunchedEffect(memoText) {
                    if (textState.text != memoText) {
                        textState = androidx.compose.ui.text.input.TextFieldValue(text = memoText, selection = androidx.compose.ui.text.TextRange(memoText.length))
                    }
                }

                // 메모 본문 내용
                BasicTextField(
                    value = textState,
                    onValueChange = { newTextState ->
                        textState = newTextState
                        if (newTextState.text != memoText) {
                            onMemoChange(newTextState.text)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                onFocused()
                            }
                        },
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF1E293B),
                        lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(Color(0xFF92400E)),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (textState.text.isEmpty()) {
                                Text(
                                    text = "메모를 입력해 보세요...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF92400E).copy(alpha = 0.5f),
                                    lineHeight = 20.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
    }
}
