package com.pasic.receipt.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.Briefcase
import com.composables.icons.lucide.Bus
import com.composables.icons.lucide.Film
import com.composables.icons.lucide.HeartPulse
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ShoppingBag
import com.composables.icons.lucide.Tag
import com.composables.icons.lucide.Utensils

data class CategoryTheme(
    val icon: ImageVector,
    val badgeBgColor: Color,
    val iconColor: Color,
    val progressColor: Color,
    val tagBgColor: Color,
    val tagTextColor: Color
)

object CategoryThemeRegistry {
    fun getTheme(categoryName: String): CategoryTheme {
        val cleanName = categoryName.trim()
        return when {
            // 1. 식비 / 외식 / 카페 / 배달 (브라이트 코발트 블루 #3B82F6)
            cleanName.contains("식비") || cleanName.contains("외식") || cleanName.contains("카페") || cleanName.contains("식당") || cleanName.contains("배달") || cleanName.contains("간식") -> {
                CategoryTheme(
                    icon = Lucide.Utensils,
                    badgeBgColor = Color(0xFFEFF6FF),
                    iconColor = Color(0xFF3B82F6),
                    progressColor = Color(0xFF3B82F6),
                    tagBgColor = Color(0xFFDBEAFE),
                    tagTextColor = Color(0xFF1E40AF)
                )
            }
            // 2. 쇼핑 / 마트 / 생활 / 의류 (비비드 에메랄드 #10B981)
            cleanName.contains("쇼핑") || cleanName.contains("마트") || cleanName.contains("의류") || cleanName.contains("잡화") || cleanName.contains("백화점") -> {
                CategoryTheme(
                    icon = Lucide.ShoppingBag,
                    badgeBgColor = Color(0xFFECFDF5),
                    iconColor = Color(0xFF10B981),
                    progressColor = Color(0xFF10B981),
                    tagBgColor = Color(0xFFD1FAE5),
                    tagTextColor = Color(0xFF065F46)
                )
            }
            // 3. 교통 / 주유 / 택시 / 차량 (산뜻한 시안 블루 #06B6D4)
            cleanName.contains("교통") || cleanName.contains("주유") || cleanName.contains("택시") || cleanName.contains("지하철") || cleanName.contains("버스") || cleanName.contains("주차") -> {
                CategoryTheme(
                    icon = Lucide.Bus,
                    badgeBgColor = Color(0xFFECFEFF),
                    iconColor = Color(0xFF06B6D4),
                    progressColor = Color(0xFF06B6D4),
                    tagBgColor = Color(0xFFCFFAFE),
                    tagTextColor = Color(0xFF0E7490)
                )
            }
            // 4. 문화 / 영화 / 여가 / 여행 (모던 바이올렛 #8B5CF6)
            cleanName.contains("문화") || cleanName.contains("영화") || cleanName.contains("여가") || cleanName.contains("여행") || cleanName.contains("숙박") || cleanName.contains("공연") -> {
                CategoryTheme(
                    icon = Lucide.Film,
                    badgeBgColor = Color(0xFFF5F3FF),
                    iconColor = Color(0xFF8B5CF6),
                    progressColor = Color(0xFF8B5CF6),
                    tagBgColor = Color(0xFFEDE9FE),
                    tagTextColor = Color(0xFF6D28D9)
                )
            }
            // 5. 사무 / 문구 / 교육 / 도서 (웜 앰버 골드 #F59E0B)
            cleanName.contains("사무") || cleanName.contains("용품") || cleanName.contains("문구") || cleanName.contains("서점") || cleanName.contains("교육") || cleanName.contains("학원") -> {
                CategoryTheme(
                    icon = Lucide.Briefcase,
                    badgeBgColor = Color(0xFFFFFBEB),
                    iconColor = Color(0xFFF59E0B),
                    progressColor = Color(0xFFF59E0B),
                    tagBgColor = Color(0xFFFEF3C7),
                    tagTextColor = Color(0xFFB45309)
                )
            }
            // 6. 의료 / 건강 / 병원 / 약국 (비비드 로즈 핑크 #F43F5E)
            cleanName.contains("의료") || cleanName.contains("병원") || cleanName.contains("약국") || cleanName.contains("건강") || cleanName.contains("치과") -> {
                CategoryTheme(
                    icon = Lucide.HeartPulse,
                    badgeBgColor = Color(0xFFFFF1F2),
                    iconColor = Color(0xFFF43F5E),
                    progressColor = Color(0xFFF43F5E),
                    tagBgColor = Color(0xFFFFE4E6),
                    tagTextColor = Color(0xFFBE123C)
                )
            }
            // 7. 주거 / 통신 / 관리비 (모던 틸 #14B8A6)
            cleanName.contains("주거") || cleanName.contains("월세") || cleanName.contains("공과금") || cleanName.contains("통신") || cleanName.contains("관리비") || cleanName.contains("전기") -> {
                CategoryTheme(
                    icon = Lucide.House,
                    badgeBgColor = Color(0xFFF0FDFA),
                    iconColor = Color(0xFF14B8A6),
                    progressColor = Color(0xFF14B8A6),
                    tagBgColor = Color(0xFFCCFBF1),
                    tagTextColor = Color(0xFF0F766E)
                )
            }
            // 8. 기타 / 나머지 (뉴트럴 슬레이트 #94A3B8)
            else -> {
                CategoryTheme(
                    icon = Lucide.Tag,
                    badgeBgColor = Color(0xFFF8FAFC),
                    iconColor = Color(0xFF64748B),
                    progressColor = Color(0xFF94A3B8),
                    tagBgColor = Color(0xFFF1F5F9),
                    tagTextColor = Color(0xFF475569)
                )
            }
        }
    }
}
