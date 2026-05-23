package com.nrlptt.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nrlptt.app.service.PttService
import com.nrlptt.app.theme.*

@Composable
fun ActivityList(
    entries: List<PttService.ActivityEntry>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(BgCard),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(entries.take(5)) { entry ->
            ActivityRow(entry)
        }
    }
}

@Composable
private fun ActivityRow(entry: PttService.ActivityEntry) {
    val (icon, color) = when (entry.type) {
        PttService.ActivityType.TX -> Icons.Filled.Send to StatusOrange
        PttService.ActivityType.RX -> Icons.Filled.Mic to StatusGreen
        PttService.ActivityType.MSG -> Icons.Filled.Email to StatusBlue
        PttService.ActivityType.JOIN -> Icons.Filled.Login to StatusBlue
        PttService.ActivityType.LEAVE -> Icons.Filled.Login to TextDim
        PttService.ActivityType.SYSTEM -> Icons.Filled.Login to TextSecondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(
            text = entry.callsign,
            style = PttTypography.ListItemBold,
            color = color,
            modifier = Modifier.weight(1f)
        )
        Text(text = entry.time, style = PttTypography.Caption, color = TextDim)
    }
}
