package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuickAction
import com.example.data.model.QuickActionsList
import com.example.ui.theme.PandaBgCardElevated
import com.example.ui.theme.PandaCyanAccent
import com.example.ui.theme.TextPrimary

@Composable
fun QuickActionsBar(
    onActionSelected: (QuickAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (action in QuickActionsList.items) {
            ActionChip(
                action = action,
                onClick = { onActionSelected(action) }
            )
        }
    }
}

@Composable
private fun ActionChip(
    action: QuickAction,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .testTag("action_chip_${action.id}")
            .clip(RoundedCornerShape(16.dp))
            .background(PandaBgCardElevated)
            .border(1.dp, Color(0xFF2E2A50), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(text = action.iconEmoji, fontSize = 13.sp)
        Text(
            text = action.title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                fontSize = 12.sp
            )
        )
    }
}
