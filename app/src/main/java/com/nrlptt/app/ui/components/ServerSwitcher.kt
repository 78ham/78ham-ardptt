package com.nrlptt.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nrlptt.app.network.ConnectionState
import com.nrlptt.app.network.ServerConnection
import com.nrlptt.app.theme.*

@Composable
fun ServerSwitcher(
    connections: Map<String, ServerConnection>,
    activeId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (connections.isEmpty()) {
        Text("NO SERVERS", style = PttTypography.Caption, color = TextDim,
            modifier = modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BgDark)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        connections.values.forEach { conn ->
            val isActive = conn.id == activeId
            val stateColor = when (conn.connState.value) {
                ConnectionState.CONNECTED -> StatusGreen
                ConnectionState.CONNECTING -> StatusOrange
                ConnectionState.DISCONNECTED -> StatusGray
            }
            val bg = if (isActive) BrandGreen.copy(alpha = 0.15f) else BgCard
            val textColor = if (isActive) BrandGreen else TextSecondary

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(bg)
                    .clickable { onSelect(conn.id) }
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(stateColor))
                Text(conn.config.displayLabel, style = PttTypography.Caption, color = textColor, maxLines = 1)
            }
        }
    }
}
