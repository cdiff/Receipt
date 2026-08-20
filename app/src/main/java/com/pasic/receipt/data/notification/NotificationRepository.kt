package com.pasic.receipt.data.notification

import android.content.Context
import com.pasic.receipt.data.local.entity.ReceiptEntity
import com.pasic.receipt.data.repository.ReceiptRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val receiptRepository: ReceiptRepository
) {
    private val prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 읽은 알림 ID Set 상태 관리 (SharedPreferences 영속화)
    private val _readIds = MutableStateFlow<Set<String>>(loadReadIds())
    val readIds: StateFlow<Set<String>> = _readIds.asStateFlow()

    // 삭제된 알림 ID Set 상태 관리 (SharedPreferences 영속화)
    private val _deletedIds = MutableStateFlow<Set<String>>(loadDeletedIds())
    val deletedIds: StateFlow<Set<String>> = _deletedIds.asStateFlow()

    // 닫은 공지 배너 ID Set 상태 관리
    private val _dismissedNoticeIds = MutableStateFlow<Set<String>>(loadDismissedNoticeIds())
    val dismissedNoticeIds: StateFlow<Set<String>> = _dismissedNoticeIds.asStateFlow()

    // 실제 내보내기/백업 히스토리 로그 (최대 20건 유지, SharedPreferences 영속화)
    private val _exportLogs = MutableStateFlow<List<ExportLogRecord>>(loadExportLogs())
    val exportLogs: StateFlow<List<ExportLogRecord>> = _exportLogs.asStateFlow()

    // 영수증 DB 실시간 구독 + 스마트 동적 알림 생성 스트림
    val notificationsFlow: StateFlow<List<NotificationItem>> = combine(
        receiptRepository.getAllReceipts(),
        _readIds,
        _deletedIds,
        _exportLogs
    ) { receipts, readIds, deletedIds, exportLogs ->
        generateSmartNotifications(receipts, readIds, deletedIds, exportLogs)
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    // 읽지 않은 알림 개수 전역 스트림 (홈 상단바 종 뱃지와 실시간 동기화)
    val unreadCountFlow: StateFlow<Int> = combine(
        notificationsFlow,
        _dismissedNoticeIds
    ) { notifications, _ ->
        notifications.count { !it.isRead }
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = 0
    )

    // 공지 배너 데이터 스트림
    val noticeBannerFlow: StateFlow<NoticeBannerData?> = _dismissedNoticeIds.combine(MutableStateFlow(Unit)) { dismissed, _ ->
        val banner = NoticeBannerData()
        if (dismissed.contains(banner.id)) null else banner
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = NoticeBannerData()
    )

    private fun loadReadIds(): Set<String> {
        return prefs.getStringSet("read_notification_ids", emptySet()) ?: emptySet()
    }

    private fun saveReadIds(ids: Set<String>) {
        prefs.edit().putStringSet("read_notification_ids", ids).apply()
    }

    private fun loadDismissedNoticeIds(): Set<String> {
        return prefs.getStringSet("dismissed_notice_ids", emptySet()) ?: emptySet()
    }

    private fun saveDismissedNoticeIds(ids: Set<String>) {
        prefs.edit().putStringSet("dismissed_notice_ids", ids).apply()
    }

    private fun loadDeletedIds(): Set<String> {
        return prefs.getStringSet("deleted_notification_ids", emptySet()) ?: emptySet()
    }

    private fun saveDeletedIds(ids: Set<String>) {
        prefs.edit().putStringSet("deleted_notification_ids", ids).apply()
    }

    /**
     * 자가 치유(Self-healing) JSON 파싱을 통한 내보내기 로그 로드 (최대 20건)
     */
    private fun loadExportLogs(): List<ExportLogRecord> {
        val jsonStr = prefs.getString("export_logs_json", null) ?: return emptyList()
        return runCatching {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<ExportLogRecord>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ExportLogRecord(
                        id = obj.getString("id"),
                        fileName = obj.getString("fileName"),
                        receiptCount = obj.getInt("receiptCount"),
                        format = obj.getString("format"),
                        timestampMs = obj.getLong("timestampMs"),
                        filePath = if (obj.has("filePath") && !obj.isNull("filePath")) obj.getString("filePath") else null
                    )
                )
            }
            list.sortedByDescending { it.timestampMs }.take(20)
        }.getOrElse {
            // JSON 파싱 실패 시 손상된 데이터 초기화 후 빈 리스트 반환 (자가 치유)
            prefs.edit().remove("export_logs_json").apply()
            emptyList()
        }
    }

    private fun saveExportLogs(logs: List<ExportLogRecord>) {
        runCatching {
            val jsonArray = JSONArray()
            logs.take(20).forEach { log ->
                val obj = JSONObject().apply {
                    put("id", log.id)
                    put("fileName", log.fileName)
                    put("receiptCount", log.receiptCount)
                    put("format", log.format)
                    put("timestampMs", log.timestampMs)
                    put("filePath", log.filePath)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("export_logs_json", jsonArray.toString()).apply()
        }
    }

    /**
     * 실제 CSV / PDF / ZIP 내보내기 시 실시간 알림 로그 누적
     * - 1분 이내 동일 파일명 중복 생성 방지
     * - 최대 20건 유지 (FIFO)
     */
    fun addExportLog(fileName: String, receiptCount: Int, format: String, filePath: String? = null) {
        val now = System.currentTimeMillis()
        val currentLogs = _exportLogs.value.toMutableList()

        // 1분 이내 동일한 파일명 내보내기는 기존 로그 갱신 (Deduplication)
        val duplicateIndex = currentLogs.indexOfFirst {
            it.fileName == fileName && (now - it.timestampMs) < 60_000L
        }

        val logId = if (duplicateIndex != -1) currentLogs[duplicateIndex].id else "export_log_${now}"
        val newRecord = ExportLogRecord(
            id = logId,
            fileName = fileName,
            receiptCount = receiptCount,
            format = format,
            timestampMs = now,
            filePath = filePath
        )

        if (duplicateIndex != -1) {
            currentLogs[duplicateIndex] = newRecord
        } else {
            currentLogs.add(0, newRecord)
        }

        val updatedLogs = currentLogs.sortedByDescending { it.timestampMs }.take(20)
        _exportLogs.value = updatedLogs
        saveExportLogs(updatedLogs)
    }

    fun markAsRead(id: String) {
        val newSet = _readIds.value + id
        _readIds.value = newSet
        saveReadIds(newSet)
    }

    fun markAllAsRead() {
        val allIds = notificationsFlow.value.map { it.id }.toSet()
        val newSet = _readIds.value + allIds
        _readIds.value = newSet
        saveReadIds(newSet)
    }

    fun dismissNoticeBanner(bannerId: String) {
        val newSet = _dismissedNoticeIds.value + bannerId
        _dismissedNoticeIds.value = newSet
        saveDismissedNoticeIds(newSet)
    }

    fun getNoticeById(id: String): NoticeBannerData? {
        val defaultNotice = NoticeBannerData()
        return if (id == defaultNotice.id || id.isBlank() || id == "latest") defaultNotice else defaultNotice
    }

    /**
     * 알림 개별 삭제 (스마트 알림 영구 숨김 및 내보내기 로그 영구 제거)
     */
    fun deleteNotification(id: String) {
        // 1. deletedIds Set에 추가하여 영구 숨김 처리
        val newDeleted = _deletedIds.value + id
        _deletedIds.value = newDeleted
        saveDeletedIds(newDeleted)

        // 2. 만약 내보내기 로그에 포함되어 있다면 목록에서 완전 제거
        val currentLogs = _exportLogs.value
        if (currentLogs.any { it.id == id }) {
            val updatedLogs = currentLogs.filterNot { it.id == id }
            _exportLogs.value = updatedLogs
            saveExportLogs(updatedLogs)
        }
    }

    /**
     * 실제 로컬 DB 데이터 및 내보내기 히스토리 기반 지능형 알림 생성
     */
    private fun generateSmartNotifications(
        receipts: List<ReceiptEntity>,
        readIds: Set<String>,
        deletedIds: Set<String>,
        exportLogs: List<ExportLogRecord>
    ): List<NotificationItem> {
        val items = mutableListOf<NotificationItem>()
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        // 오늘 시작/끝 밀리초 계산
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStartMs = calendar.timeInMillis
        val yesterdayStartMs = todayStartMs - (1000 * 60 * 60 * 24)

        // 오늘 스캔한 영수증 필터링
        val todayReceipts = receipts.filter { it.createdAt >= todayStartMs }
        val thisMonthReceipts = receipts.filter {
            calendar.timeInMillis = it.createdAt
            calendar.get(Calendar.MONTH) + 1 == currentMonth
        }
        val thisMonthCount = thisMonthReceipts.size
        val thisMonthTotal = thisMonthReceipts.sumOf { it.convertedAmountKrw }
        val formattedTotal = DecimalFormat("#,###").format(thisMonthTotal.toLong())

        // 오늘 날짜 및 해당 월의 마지막 날(말일) 계산
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val isMonthEndApproaching = (lastDayOfMonth - currentDay) <= 3 // 말일 3일 전부터 활성화

        // ── 1. [영수증·일정] 월말 경비 제출 D-Day 알림 (말일 3일 전부터만 활성화) ──
        if (isMonthEndApproaching) {
            val dDay = lastDayOfMonth - currentDay
            val dDayLabel = if (dDay == 0) "D-Day" else "D-$dDay"
            val expenseNoticeTime = todayStartMs + (1000L * 60 * 60 * 9) // 오늘 오전 09:00 기준
            val expenseTimestamp = if (now >= expenseNoticeTime) expenseNoticeTime else now
            val expenseTimeLabel = formatDynamicTimeLabel(expenseTimestamp, todayStartMs, yesterdayStartMs, now)

            if (thisMonthCount > 0) {
                items.add(
                    NotificationItem(
                        id = "expense_summary_${currentMonth}",
                        category = NotificationCategory.EXPENSE_REPORT,
                        title = "이번 달 등록된 경비 영수증이 ${thisMonthCount}건(${formattedTotal}원) 있습니다.",
                        message = "월말 정산 및 경비 제출($dDayLabel)을 위해 엑셀 또는 PDF 보고서로 내보내 보세요.",
                        timestampMs = expenseTimestamp,
                        timeLabel = expenseTimeLabel,
                        section = DateSection.TODAY,
                        isRead = readIds.contains("expense_summary_${currentMonth}"),
                        targetRoute = "export"
                    )
                )
            } else {
                items.add(
                    NotificationItem(
                        id = "expense_summary_empty_${currentMonth}",
                        category = NotificationCategory.EXPENSE_REPORT,
                        title = "이번 달 등록된 지출 영수증이 아직 없습니다.",
                        message = "월말 정산($dDayLabel) 전, 이번 달 사용하신 영수증을 '영수증 쏙'에 등록해 보세요.",
                        timestampMs = expenseTimestamp,
                        timeLabel = expenseTimeLabel,
                        section = DateSection.TODAY,
                        isRead = readIds.contains("expense_summary_empty_${currentMonth}"),
                        targetRoute = "scan"
                    )
                )
            }
        }

        // ── 2. [영수증·일정] 스캔 스마트 분기 알림 (저녁 8시 리마인더 & 오늘 스캔 성공) ──
        if (todayReceipts.isNotEmpty()) {
            val lastScanTime = todayReceipts.maxOf { it.createdAt }
            items.add(
                NotificationItem(
                    id = "scan_success_today",
                    category = NotificationCategory.SCAN_REMINDER,
                    title = "오늘 지출 영수증 ${todayReceipts.size}건 정리 완료!",
                    message = "오늘 등록하신 영수증 ${todayReceipts.size}건이 안전하게 저장되었습니다. 내역을 확인해 보세요.",
                    timestampMs = lastScanTime,
                    timeLabel = formatDynamicTimeLabel(lastScanTime, todayStartMs, yesterdayStartMs, now),
                    section = DateSection.TODAY,
                    isRead = readIds.contains("scan_success_today"),
                    targetRoute = "receipts"
                )
            )
        } else if (currentHour >= 20) {
            // 저녁 8시(20:00) 이후에만 리마인더 알림 활성화
            val eveningReminderTime = todayStartMs + (1000L * 60 * 60 * 20) // 오늘 저녁 20:00 기준
            items.add(
                NotificationItem(
                    id = "scan_reminder_today",
                    category = NotificationCategory.SCAN_REMINDER,
                    title = "오늘 사용한 영수증을 등록해 보세요.",
                    message = "오늘 결제한 지출 내역이 있나요? 잊어버리기 전에 영수증 쏙으로 쏙! 스캔해 보세요.",
                    timestampMs = eveningReminderTime,
                    timeLabel = formatDynamicTimeLabel(eveningReminderTime, todayStartMs, yesterdayStartMs, now),
                    section = DateSection.TODAY,
                    isRead = readIds.contains("scan_reminder_today"),
                    targetRoute = "scan"
                )
            )
        }

        // ── 3. [내보내기·보관] 실제 내보내기/백업 히스토리 로그 실시간 렌더링 ──
        if (exportLogs.isNotEmpty()) {
            exportLogs.forEach { log ->
                val section = when {
                    log.timestampMs >= todayStartMs -> DateSection.TODAY
                    log.timestampMs >= yesterdayStartMs -> DateSection.YESTERDAY
                    else -> DateSection.PREVIOUS
                }
                val timeLabel = formatDynamicTimeLabel(log.timestampMs, todayStartMs, yesterdayStartMs, now)
                val isZip = log.format.equals("ZIP", ignoreCase = true)

                items.add(
                    NotificationItem(
                        id = log.id,
                        category = if (isZip) NotificationCategory.BACKUP_SUCCESS else NotificationCategory.EXPORT_LOG,
                        title = if (isZip) "ZIP 파일 안전 백업 완료" else "${log.format} 내보내기 완료",
                        message = "요청하신 '${log.fileName}' (${log.receiptCount}건) 생성이 완료되어 안전하게 저장되었습니다.",
                        timestampMs = log.timestampMs,
                        timeLabel = timeLabel,
                        section = section,
                        isRead = readIds.contains(log.id),
                        targetRoute = "export"
                    )
                )
            }
        } else {
            // 내보내기 이력이 없을 때의 기본 안내 로그 (어제 날짜 기준)
            val guideTimestamp = yesterdayStartMs + (1000L * 60 * 60 * 14)
            items.add(
                NotificationItem(
                    id = "export_guide_log",
                    category = NotificationCategory.EXPORT_LOG,
                    title = "영수증 내보내기 기능을 활용해 보세요.",
                    message = "원하는 기간의 영수증을 Excel, PDF, ZIP 형식으로 언제든 간편하게 추출할 수 있습니다.",
                    timestampMs = guideTimestamp,
                    timeLabel = formatDynamicTimeLabel(guideTimestamp, todayStartMs, yesterdayStartMs, now),
                    section = DateSection.YESTERDAY,
                    isRead = readIds.contains("export_guide_log"),
                    targetRoute = "export"
                )
            )
        }

        // ── 4. [내보내기·보관] 20건 이상 백업 권장 알림 (7일 쿨다운) ──
        if (receipts.size >= 20) {
            val lastBackupReadMs = prefs.getLong("backup_reminder_read_time", 0L)
            val isCoolDownActive = (now - lastBackupReadMs) < (1000L * 60 * 60 * 24 * 7) // 7일 쿨다운

            if (!isCoolDownActive || !readIds.contains("backup_reminder_20")) {
                val backupNoticeTimestamp = now - (1000L * 60 * 60 * 48) // 2일 전
                items.add(
                    NotificationItem(
                        id = "backup_reminder_20",
                        category = NotificationCategory.BACKUP_REMINDER,
                        title = "소중한 영수증 데이터를 안전하게 백업해 보세요.",
                        message = "현재 등록된 영수증이 ${receipts.size}건 있습니다. 데이터 유실 방지를 위해 ZIP 안전 백업을 권장합니다.",
                        timestampMs = backupNoticeTimestamp,
                        timeLabel = formatDynamicTimeLabel(backupNoticeTimestamp, todayStartMs, yesterdayStartMs, now),
                        section = DateSection.PREVIOUS,
                        isRead = readIds.contains("backup_reminder_20"),
                        targetRoute = "export"
                    )
                )
            }
        }

        return items
            .filterNot { deletedIds.contains(it.id) }
            .sortedByDescending { it.timestampMs }
    }

    /**
     * 🕒 섹션별 맞춤 동적 시간 포맷터
     * - [오늘]  1분 미만: "방금 전", 1~59분: "N분 전", 1시간 이상: "N시간 전"
     * - [어제]  발생 시각: "오전/오후 h:mm" (예: 오후 11:55)
     * - [이전]  날짜: "M월 d일" (예: 8월 18일)
     */
    private fun formatDynamicTimeLabel(
        timestampMs: Long,
        todayStartMs: Long,
        yesterdayStartMs: Long,
        now: Long
    ): String {
        return when {
            // [오늘] 00:00 이후 발생
            timestampMs >= todayStartMs -> {
                val diffMs = maxOf(0L, now - timestampMs)
                val diffMins = diffMs / (1000 * 60)
                val diffHours = diffMins / 60
                when {
                    diffMins < 1 -> "방금 전"
                    diffMins < 60 -> "${diffMins}분 전"
                    else -> "${diffHours}시간 전"
                }
            }
            // [어제] 어제 00:00 ~ 23:59:59 발생
            timestampMs >= yesterdayStartMs -> {
                SimpleDateFormat("a h:mm", Locale.KOREAN).format(Date(timestampMs))
            }
            // [이전] 그저께 이전 발생
            else -> {
                val calNow = Calendar.getInstance().apply { timeInMillis = now }
                val calTarget = Calendar.getInstance().apply { timeInMillis = timestampMs }
                if (calNow.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR)) {
                    SimpleDateFormat("M월 d일", Locale.KOREAN).format(Date(timestampMs))
                } else {
                    SimpleDateFormat("yyyy년 M월 d일", Locale.KOREAN).format(Date(timestampMs))
                }
            }
        }
    }
}
