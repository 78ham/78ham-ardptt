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
import com.nrlptt.app.network.ConnectionState
import com.nrlptt.app.network.ServerConnection
import com.nrlptt.app.theme.*
import androidx.compose.ui.unit.dp

@Composable
fun ServerSwitcher(
    connections: Map<String, ServerConnection>,
    activeId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val d = rememberScreenDimens()
    if (connections.isEmpty()) {
        Text("无服务器", fontSize = d.captionSize, color = TextDim,
            modifier = modifier.padding(horizontal = d.cardPadding, vertical = d.smallGap + 1.dp))
        return
    }

    Row(
        modifier = modifier.fillMaxWidth()
            .background(BgDark)
            .padding(horizontal = d.smallGap + 2.dp, vertical = d.smallGap),
        horizontalArrangement = Arrangement.spacedBy(d.smallGap)
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
                modifier = Modifier.clip(RoundedCornerShape(d.cornerRadius)).background(bg)
                    .clickable { onSelect(conn.id) }
                    .padding(horizontal = d.serverTabPadding, vertical = d.smallGap + 1.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(d.smallGap)
            ) {
                Box(Modifier.size(d.dotSize * 0.6f).clip(RoundedCornerShape(d.dotSize * 0.3f)).background(stateColor))
                Text(conn.config.displayLabel, fontSize = d.captionSize, color = textColor, maxLines = 1)
            }
        }
    }
}
