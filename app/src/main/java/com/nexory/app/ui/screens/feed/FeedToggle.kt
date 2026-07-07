package com.nexory.app.ui.screens.feed

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexory.app.ui.theme.NexoryColors

@Composable
fun FeedToggle(
    isMyEvents: Boolean,
    onToggle:   () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(NexoryColors.SurfaceDark),
    ) {
        listOf("Все мероприятия" to false, "Мои записи" to true).forEach { (label, isMyTab) ->
            val selected = isMyEvents == isMyTab
            val bgColor by animateColorAsState(
                targetValue = if (selected) NexoryColors.DeepBlue else Color.Transparent,
                label       = "tabBg",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .clickable { if (!selected) onToggle() }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = label,
                    color      = if (selected) Color.White else NexoryColors.TextSecondary,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize   = 14.sp,
                )
            }
        }
    }
}