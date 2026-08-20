package com.pasic.receipt.data.notification

enum class NotificationTab(val displayName: String) {
    ALL("전체"),
    RECEIPT_SCHEDULE("영수증·일정"),
    EXPORT_BACKUP("내보내기·보관")
}

enum class NotificationCategory(val tab: NotificationTab, val displayName: String) {
    EXPENSE_REPORT(NotificationTab.RECEIPT_SCHEDULE, "경비 제출 D-Day"),
    SCAN_REMINDER(NotificationTab.RECEIPT_SCHEDULE, "스캔 리마인더"),
    EXPORT_LOG(NotificationTab.EXPORT_BACKUP, "내보내기 완료"),
    BACKUP_SUCCESS(NotificationTab.EXPORT_BACKUP, "백업 완료"),
    BACKUP_REMINDER(NotificationTab.EXPORT_BACKUP, "정기 백업 권장"),
    SYSTEM_UPDATE(NotificationTab.ALL, "업데이트 안내")
}

enum class DateSection(val headerTitle: String) {
    TODAY("오늘"),
    YESTERDAY("어제"),
    PREVIOUS("이전 알림")
}

data class NotificationItem(
    val id: String,
    val category: NotificationCategory,
    val title: String,
    val message: String,
    val timestampMs: Long,
    val timeLabel: String,
    val section: DateSection,
    val isRead: Boolean = false,
    val targetRoute: String? = null
)

data class NoticeBannerData(
    val id: String = "notice_v110",
    val title: String = "앱 버전 1.1.0 업데이트 안내",
    val bannerTitle: String = "[업데이트] 스마트 영수증 인식 AI 엔진 개선 안내",
    val date: String = "2026.08.18",
    val content: String = """
영수증 AI 스캔 성능이 대폭 향상되었습니다.

• 흐릿한 영수증 인식률 40% 개선 (1280px 초고화질 전처리)
• 스캔 처리 속도 2배 향상 및 상호명/품목 자동 교차 분석
• 지능형 스마트 알림 센터 & 저녁 스캔 리마인더 도입
• 원클릭 데이터 백업/복원 및 카카오톡 1:1 고객센터 지원

앞으로도 더 나은 서비스를 제공하기 위해 노력하겠습니다. 감사합니다.
    """.trimIndent(),
    val isDismissed: Boolean = false
)

data class ExportLogRecord(
    val id: String,
    val fileName: String,
    val receiptCount: Int,
    val format: String, // "EXCEL", "PDF", "ZIP"
    val timestampMs: Long,
    val filePath: String? = null
)
