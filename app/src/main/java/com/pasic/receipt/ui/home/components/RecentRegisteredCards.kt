package com.pasic.receipt.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pasic.receipt.ui.theme.BlackCardObsidian
import com.pasic.receipt.ui.theme.ModernCardBlue
import com.pasic.receipt.ui.theme.SlateCardNavy
import com.pasic.receipt.ui.theme.TextMuted
import com.pasic.receipt.ui.theme.TextPrimary
import com.pasic.receipt.ui.theme.TextSecondary

data class RegisteredCardItem(
    val id: String,
    val title: String,
    val color: Color
)

@Composable
fun RecentRegisteredCards(
    onSeeAllClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dummyCards = listOf(
        RegisteredCardItem("1", "MODERN CARD", ModernCardBlue),
        RegisteredCardItem("2", "THE BLACK", BlackCardObsidian),
        RegisteredCardItem("3", "HYUNDAI CARD", SlateCardNavy)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "최근 등록 카드",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "모두보기",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.clickable(onClick = onSeeAllClick)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(dummyCards) { card ->
                CardItemView(card)
            }
        }
    }
}

@Composable
private fun CardItemView(card: RegisteredCardItem) {
    Box(
        modifier = Modifier
            .width(140.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(card.color)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = card.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f)
            )

            // IC Chip placeholder
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            )
        }
    }
}
