package com.pasic.receipt.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasic.receipt.data.notification.DateSection
import com.pasic.receipt.data.notification.NoticeBannerData
import com.pasic.receipt.data.notification.NotificationItem
import com.pasic.receipt.data.notification.NotificationRepository
import com.pasic.receipt.data.notification.NotificationTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(NotificationTab.ALL)
    val selectedTab: StateFlow<NotificationTab> = _selectedTab.asStateFlow()

    val noticeBanner: StateFlow<NoticeBannerData?> = repository.noticeBannerFlow
    val unreadCount: StateFlow<Int> = repository.unreadCountFlow

    // 선택된 탭에 따라 필터링하고 날짜 섹션별로 그룹화된 알림 맵
    val groupedNotifications: StateFlow<Map<DateSection, List<NotificationItem>>> = combine(
        repository.notificationsFlow,
        _selectedTab
    ) { notifications, tab ->
        val filtered = when (tab) {
            NotificationTab.ALL -> notifications
            NotificationTab.RECEIPT_SCHEDULE -> notifications.filter { it.category.tab == NotificationTab.RECEIPT_SCHEDULE }
            NotificationTab.EXPORT_BACKUP -> notifications.filter { it.category.tab == NotificationTab.EXPORT_BACKUP }
        }
        // 섹션 순서대로 그룹핑 (TODAY -> YESTERDAY -> PREVIOUS)
        filtered.groupBy { it.section }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    fun selectTab(tab: NotificationTab) {
        _selectedTab.value = tab
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
        }
    }

    fun markItemAsRead(item: NotificationItem) {
        viewModelScope.launch {
            repository.markAsRead(item.id)
        }
    }

    fun dismissNoticeBanner(bannerId: String) {
        viewModelScope.launch {
            repository.dismissNoticeBanner(bannerId)
        }
    }

    fun deleteNotification(item: NotificationItem) {
        viewModelScope.launch {
            repository.deleteNotification(item.id)
        }
    }
}
