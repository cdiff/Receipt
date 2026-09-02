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
    val id: String = "notice_v100_v5",
    val title: String = "영수증 쏙 1.0.0 업데이트 안내",
    val bannerTitle: String = "[업데이트] 서비스 안정성 강화 및 편의 기능 개선 안내",
    val date: String = "2026.09.02",
    val content: String = """
영수증 쏙을 이용해 주시는 테스터 여러분 감사합니다.
더욱 안전하고 쾌적한 사용성을 위한 주요 개선 사항이 적용되었습니다.

• Google Play Integrity 기반 AI 보안 인증 체계 적용 (스캔 안정성 강화)
• 결제수단 3종(카드·현금·간편결제) 표준화 및 AI 인식 정확도 향상
• 지출결의서 PDF 메모(적요) 열 확장 및 2줄 멀티라인 줄바꿈 지원
• 영수증 삭제 확인 다이얼로그 추가 및 프리텐다드 전역 폰트 적용
• 시스템 3버튼 네비게이션 바 하단 겹침 현상 및 한글 키보드 조합 개선
• 내보내기 CSV 열 선택 다단(FlowRow) UI 개선 및 정렬 최적화

앞으로도 더 나은 서비스를 제공하기 위해 지속적으로 개선해 나가겠습니다. 감사합니다.
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
