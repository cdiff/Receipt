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
    val progressColor: Color
)

object CategoryThemeRegistry {
    fun getTheme(categoryName: String): CategoryTheme {
        val cleanName = categoryName.trim()
        return when {
            // 1. 식비 (Food & Dining)
            cleanName.contains("식비") || cleanName.contains("외식") || cleanName.contains("카페") || cleanName.contains("식당") || cleanName.contains("배달") || cleanName.contains("간식") -> {
                CategoryTheme(
                    icon = Lucide.Utensils,
                    badgeBgColor = Color(0xFFDBEAFE),
                    iconColor = Color(0xFF2563EB),
                    progressColor = Color(0xFF2563EB)
                )
            }
            // 2. 교통 (Transportation)
            cleanName.contains("교통") || cleanName.contains("주유") || cleanName.contains("택시") || cleanName.contains("지하철") || cleanName.contains("버스") || cleanName.contains("주차") -> {
                CategoryTheme(
                    icon = Lucide.Bus,
                    badgeBgColor = Color(0xFFFFEDD5),
                    iconColor = Color(0xFFEA580C),
                    progressColor = Color(0xFFEA580C)
                )
            }
            // 3. 쇼핑/생활 (Shopping & Retail)
            cleanName.contains("쇼핑") || cleanName.contains("마트") || cleanName.contains("의류") || cleanName.contains("잡화") || cleanName.contains("백화점") -> {
                CategoryTheme(
                    icon = Lucide.ShoppingBag,
                    badgeBgColor = Color(0xFFF3E8FF),
                    iconColor = Color(0xFF9333EA),
                    progressColor = Color(0xFF9333EA)
                )
            }
            // 4. 의료/건강 (Medical & Health)
            cleanName.contains("의료") || cleanName.contains("병원") || cleanName.contains("약국") || cleanName.contains("건강") || cleanName.contains("치과") -> {
                CategoryTheme(
                    icon = Lucide.HeartPulse,
                    badgeBgColor = Color(0xFFFFE4E6),
                    iconColor = Color(0xFFE11D48),
                    progressColor = Color(0xFFE11D48)
                )
            }
            // 5. 문화/여가 (Culture & Leisure)
            cleanName.contains("문화") || cleanName.contains("영화") || cleanName.contains("여가") || cleanName.contains("여행") || cleanName.contains("숙박") || cleanName.contains("공연") -> {
                CategoryTheme(
                    icon = Lucide.Film,
                    badgeBgColor = Color(0xFFE0E7FF),
                    iconColor = Color(0xFF4F46E5),
                    progressColor = Color(0xFF4F46E5)
                )
            }
            // 6. 사무/교육 (Office & Education)
            cleanName.contains("사무") || cleanName.contains("용품") || cleanName.contains("문구") || cleanName.contains("서점") || cleanName.contains("교육") || cleanName.contains("학원") -> {
                CategoryTheme(
                    icon = Lucide.Briefcase,
                    badgeBgColor = Color(0xFFCCFBF1),
                    iconColor = Color(0xFF0D9488),
                    progressColor = Color(0xFF0D9488)
                )
            }
            // 7. 주거/통신 (Housing & Utilities)
            cleanName.contains("주거") || cleanName.contains("월세") || cleanName.contains("공과금") || cleanName.contains("통신") || cleanName.contains("관리비") || cleanName.contains("전기") -> {
                CategoryTheme(
                    icon = Lucide.House,
                    badgeBgColor = Color(0xFFFEF3C7),
                    iconColor = Color(0xFFD97706),
                    progressColor = Color(0xFFD97706)
                )
            }
            // 8. 기타 (Misc / Default)
            else -> {
                CategoryTheme(
                    icon = Lucide.Tag,
                    badgeBgColor = Color(0xFFF1F5F9),
                    iconColor = Color(0xFF64748B),
                    progressColor = Color(0xFF334155)
                )
            }
        }
    }
}
