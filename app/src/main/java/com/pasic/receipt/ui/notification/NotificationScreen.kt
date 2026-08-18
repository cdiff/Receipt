package com.pasic.receipt.ui.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.icons.lucide.Archive
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.CalendarClock
import com.composables.icons.lucide.FileSpreadsheet
import com.composables.icons.lucide.FolderSync
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Megaphone
import com.composables.icons.lucide.ReceiptText
import com.composables.icons.lucide.ScanLine
import com.composables.icons.lucide.ShieldCheck
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Upload
import com.composables.icons.lucide.X
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import com.composables.icons.lucide.Trash2
import com.pasic.receipt.util.ToastEventBus
import com.pasic.receipt.data.notification.DateSection
import com.pasic.receipt.data.notification.NoticeBannerData
import com.pasic.receipt.data.notification.NotificationCategory
import com.pasic.receipt.data.notification.NotificationItem
import com.pasic.receipt.data.notification.NotificationTab
import com.pasic.receipt.ui.theme.ModernCardBlue
import com.pasic.receipt.ui.theme.TextPrimary
import com.pasic.receipt.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun NotificationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val groupedNotifications by viewModel.groupedNotifications.collectAsState()
    val noticeBanner by viewModel.noticeBanner.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val haptics = LocalHapticFeedback.current

    val listState = rememberLazyListState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    // 스크롤 (0 ~ 120px) 기준 헤더 축소 진행도
    val collapseProgress by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset.toFloat() / 120f).coerceIn(0f, 1f)
        }
    }
    val animatedProgress by animateFloatAsState(
        targetValue = collapseProgress,
        animationSpec = tween(durationMillis = 100),
        label = "notificationHeaderCollapse"
    )

    val headerHeight = (56 - (8 * animatedProgress)).dp
    val titleFontSize = (18 - (2 * animatedProgress)).sp
    val titleHorizontalBias = -0.75f * (1f - animatedProgress)

    Scaffold(
        containerColor = Color.White,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                // 1. 영수증 상세 스타일 반응형 축소 탑바 (높이 56dp -> 48dp, 폰트 18sp -> 16sp)
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
                        // 중앙 반응형 타이틀
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 48.dp),
                            contentAlignment = BiasAlignment(horizontalBias = titleHorizontalBias, verticalBias = 0f)
                        ) {
                            Text(
                                text = "알림",
                                fontSize = titleFontSize,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1
                            )
                        }

                        // 좌측 뒤로가기 버튼
                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateBack()
                            },
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(
                                imageVector = Lucide.ArrowLeft,
                                contentDescription = "뒤로가기",
                                tint = Color(0xFF0F172A)
                            )
                        }

                        // 우측 [모두 읽기] 텍스트 버튼
                        TextButton(
                            onClick = {
                                if (unreadCount > 0) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.markAllAsRead()
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterEnd),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "모두 읽기",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (unreadCount > 0) ModernCardBlue else Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                // 2. 상단 고정 3-Tab 바 (전체 | 영수증·일정 | 내보내기·보관)
                NotificationTabBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.selectTab(tab)
                        coroutineScope.launch {
                            listState.scrollToItem(0)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. 상단 닫기 가능한 업데이트 공지 배너 (dismissible)
            if (noticeBanner != null) {
                item(key = "notice_banner") {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        NotificationNoticeBanner(
                            banner = noticeBanner!!,
                            onDismiss = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.dismissNoticeBanner(noticeBanner!!.id)
                            }
                        )
                    }
                }
            }

            // 2. 날짜별 그룹핑 알림 피드
            if (groupedNotifications.isEmpty()) {
                item(key = "empty_state") {
                    NotificationEmptyState()
                }
            } else {
                DateSection.values().forEach { section ->
                    val sectionItems = groupedNotifications[section]
                    if (!sectionItems.isNullOrEmpty()) {
                        item(key = "header_${section.name}") {
                            Text(
                                text = section.headerTitle,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp)
                            )
                        }

                        items(
                            items = sectionItems,
                            key = { it.id }
                        ) { item ->
                            SwipeableNotificationItem(
                                item = item,
                                onClick = {
                                    // 클릭 시 파란 점 읽음 처리만 수행 (화면 이동 없음)
                                    viewModel.markItemAsRead(item)
                                },
                                onDelete = {
                                    viewModel.deleteNotification(item)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3-Tab 바 컴포넌트 ([ 전체 ] | [ 영수증 · 일정 ] | [ 내보내기 · 보관 ])
 */
@Composable
private fun NotificationTabBar(
    selectedTab: NotificationTab,
    onTabSelected: (NotificationTab) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        // 맨 밑바닥 전체 가로 1dp 구분선
        HorizontalDivider(
            thickness = 1.dp,
            color = Color(0xFFF1F5F9),
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            NotificationTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(tab) }
                        )
                        .padding(horizontal = 14.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = tab.displayName,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) ModernCardBlue else Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 블루 인디케이터 바 (구분선 위에 완벽하게 일치)
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(if (isSelected) ModernCardBlue else Color.Transparent)
                    )
                }
            }
        }
    }
}

/**
 * 상단 닫기 가능 공지 배너 ([업데이트] 스마트 영수증 인식 AI 엔진 개선 안내)
 */
@Composable
private fun NotificationNoticeBanner(
    banner: NoticeBannerData,
    onDismiss: () -> Unit
) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 메가폰 아이콘 배지 (36dp)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEFF6FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.Megaphone,
                    contentDescription = "공지",
                    tint = ModernCardBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = banner.title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = banner.date,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF94A3B8)
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Lucide.X,
                    contentDescription = "공지 닫기",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableNotificationItem(
    item: NotificationItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.35f }
    )

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onDelete()
            ToastEventBus.showToast("알림이 삭제되었습니다.")
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Color(0xFFEF4444)
                } else {
                    Color(0xFFFCA5A5)
                },
                label = "dismissBackground"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Lucide.Trash2,
                    contentDescription = "삭제",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    ) {
        Surface(color = Color.White) {
            NotificationItemRow(
                item = item,
                onClick = onClick
            )
        }
    }
}

/**
 * 개별 알림 아이템 행 (읽지 않음 블루 점 + 원형 아이콘 + 3줄 텍스트)
 */
@Composable
private fun NotificationItemRow(
    item: NotificationItem,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 1. 읽지 않은 신규 알림 인디케이터 (첫 번째 텍스트 줄 높이에 정렬)
        Box(
            modifier = Modifier
                .width(10.dp)
                .padding(top = 5.dp),
            contentAlignment = Alignment.TopStart
        ) {
            if (!item.isRead) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
            }
        }

        // 2. 카테고리별 원형 아이콘 배지 (44dp)
        val icon = getCategoryIcon(item.category)
        val isBlueTint = item.category.tab == NotificationTab.RECEIPT_SCHEDULE || item.category == NotificationCategory.SYSTEM_UPDATE

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isBlueTint) Color(0xFFEFF6FF) else Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = item.category.displayName,
                tint = if (isBlueTint) ModernCardBlue else Color(0xFF64748B),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // 3. 3줄 알림 본문 (카테고리+시간 ➔ 타이틀 ➔ 메시지)
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.category.displayName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isBlueTint) ModernCardBlue else Color(0xFF64748B)
                )
                Text(
                    text = item.timeLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.message,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF64748B),
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * 알림이 없을 때의 빈 상태 뷰
 */
@Composable
private fun NotificationEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFFF8FAFC)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.Bell,
                contentDescription = "알림 없음",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "새로운 알림이 없습니다",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "영수증 일정과 백업 소식을 여기서 알려드릴게요.",
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF64748B)
        )
    }
}

/**
 * 카테고리별 이미지 벡터 아이콘 매핑
 */
private fun getCategoryIcon(category: NotificationCategory): ImageVector {
    return when (category) {
        NotificationCategory.EXPENSE_REPORT -> Lucide.ReceiptText
        NotificationCategory.SCAN_REMINDER -> Lucide.ScanLine
        NotificationCategory.EXPORT_LOG -> Lucide.Upload
        NotificationCategory.BACKUP_SUCCESS -> Lucide.Archive
        NotificationCategory.BACKUP_REMINDER -> Lucide.FolderSync
        NotificationCategory.SYSTEM_UPDATE -> Lucide.Sparkles
    }
}
