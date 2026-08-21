package com.pasic.receipt.ui.support

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageSquare
import com.composables.icons.lucide.Phone
import com.pasic.receipt.R
import com.pasic.receipt.ui.theme.TextPrimary
import com.pasic.receipt.ui.theme.TextSecondary
import com.pasic.receipt.util.ToastEventBus
import kotlinx.coroutines.delay

private const val KAKAO_OPEN_CHAT_URL = "https://open.kakao.com/o/receipt_help" // 카카오톡 오픈채팅방 링크
private const val TERMS_OF_SERVICE_URL = "https://github.com/pasic/Receipt" // 서비스 이용약관 웹 링크
private const val PRIVACY_POLICY_URL = "https://github.com/pasic/Receipt" // 개인정보 처리방침 웹 링크

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CustomerSupportScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNoticeDetail: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showFaqSheet by remember { mutableStateOf(false) }
    var showLicenseSheet by remember { mutableStateOf(false) }

    // 웹 URL / 카카오톡 외부 인텐트 실행 헬퍼
    val openUrl = { url: String ->
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }.onFailure {
            ToastEventBus.showToast("링크를 열 수 없습니다.")
        }
    }

    // 스크롤 (0 ~ 120px) 기준 헤더 반응형 축소 및 중앙 이동 진행도
    val collapseProgress by remember {
        derivedStateOf {
            (scrollState.value.toFloat() / 120f).coerceIn(0f, 1f)
        }
    }
    val animatedProgress by animateFloatAsState(
        targetValue = collapseProgress,
        animationSpec = tween(durationMillis = 100),
        label = "supportHeaderCollapseAnimation"
    )

    val headerHeight = (56 - (4 * animatedProgress)).dp
    val titleFontSize = (19 - (2 * animatedProgress)).sp
    val titleHorizontalBias = -0.75f * (1f - animatedProgress)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── 1. 스크롤 반응형 상단 탑바 (좌측 ➔ 중앙 부드러운 슬라이딩 & 화이트 배경) ──
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
                // 중앙 슬라이딩 타이틀
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 48.dp),
                    contentAlignment = BiasAlignment(
                        horizontalBias = titleHorizontalBias,
                        verticalBias = 0f
                    )
                ) {
                    Text(
                        text = "고객센터",
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1
                    )
                }

                // 좌측 뒤로가기 버튼
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Lucide.ArrowLeft,
                        contentDescription = "뒤로가기",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // ── 2. 본문 컨텐츠 (스크롤 가능) ──────────────────────────────────
        // 화면 진입 시 "뿅!" 튀어나오는 스프링 애니메이션 상태
        var isImagePopped by remember { mutableStateOf(false) }
        val imageScale by animateFloatAsState(
            targetValue = if (isImagePopped) 1f else 0.2f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "agentImageSpringPop"
        )
        val imageAlpha by animateFloatAsState(
            targetValue = if (isImagePopped) 1f else 0f,
            animationSpec = tween(200),
            label = "agentImageAlpha"
        )

        // 둥실둥실 잔잔한 무한 루프 플로팅(Floating) 애니메이션
        val infiniteTransition = rememberInfiniteTransition(label = "agentFloatingTransition")
        val floatingOffsetY by infiniteTransition.animateFloat(
            initialValue = -5f,
            targetValue = 5f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "agentFloatingOffsetY"
        )

        LaunchedEffect(Unit) {
            delay(100) // 화면 전환 직후 살짝 호흡을 준 뒤 뿅!
            isImagePopped = true
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상단 상담원 3D 일러스트 ("뿅!" 스프링 팝업 + 둥실둥실 플로팅 효과 적용)
            Image(
                painter = painterResource(id = R.drawable.img_customer_support),
                contentDescription = "고객센터 상담원",
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = imageScale
                        scaleY = imageScale
                        alpha = imageAlpha
                        translationY = if (isImagePopped) floatingOffsetY else 0f
                    }
                    .padding(4.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 메인 타이틀
            Text(
                text = "무엇을 도와드릴까요?",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 서브 설명문
            Text(
                text = "영수증 쏙 이용 중 궁금한 점이나 불편한 사항이 있다면 언제든지 문의해 주세요.",
                fontSize = 13.5.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── 3. 2열 퀵 액션 카드 (1:1 문의하기 & 고객센터 전화) ───────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                QuickContactCard(
                    icon = Lucide.MessageSquare,
                    label = "1:1 문의하기",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        openUrl(KAKAO_OPEN_CHAT_URL)
                    }
                )

                QuickContactCard(
                    icon = Lucide.Phone,
                    label = "고객센터 전화",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        ToastEventBus.showToast("고객센터 전화 상담 준비 중입니다.")
                    }
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // ── 4. SUPPORT 섹션 ──────────────────────────────────────────
            SupportSectionHeader(title = "SUPPORT")
            Spacer(modifier = Modifier.height(8.dp))

            SupportListRow(
                title = "자주 묻는 질문 FAQ",
                onClick = { showFaqSheet = true }
            )
            SectionInnerDivider()

            SupportListRow(
                title = "공지사항",
                onClick = { onNavigateToNoticeDetail("notice_v110") }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── 5. LEGAL 섹션 ────────────────────────────────────────────
            SupportSectionHeader(title = "LEGAL")
            Spacer(modifier = Modifier.height(8.dp))

            SupportListRow(
                title = "서비스 이용약관",
                onClick = { openUrl(TERMS_OF_SERVICE_URL) }
            )
            SectionInnerDivider()

            SupportListRow(
                title = "개인정보 처리방침",
                onClick = { openUrl(PRIVACY_POLICY_URL) }
            )
            SectionInnerDivider()

            SupportListRow(
                title = "오픈소스 라이선스",
                onClick = { showLicenseSheet = true }
            )
        }
    }

    // ── 6. 바텀시트 모달 ───────────────────────────────────────────────
    if (showFaqSheet) {
        FaqBottomSheet(
            onDismiss = { showFaqSheet = false }
        )
    }

    if (showLicenseSheet) {
        OpenSourceLicenseBottomSheet(
            onDismiss = { showLicenseSheet = false }
        )
    }
}

/**
 * 2열 퀵 액션 카드 (1:1 문의하기 / 고객센터 전화)
 */
@Composable
private fun QuickContactCard(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(108.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF1F5F9))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF334155),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            )
        }
    }
}

/**
 * 섹션 대문자 캡션 헤더 (SUPPORT / LEGAL)
 */
@Composable
private fun SupportSectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * 섹션 내 단일 목록 행
 */
@Composable
private fun SupportListRow(
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )

        Icon(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFCBD5E1),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * 항목 간 얇은 구분선
 */
@Composable
private fun SectionInnerDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFF1F5F9))
    )
}
