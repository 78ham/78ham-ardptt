package com.nrlptt.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nrlptt.app.theme.*

enum class RadioState { STANDBY, TRANSMITTING, RECEIVING, ERROR }

@Composable
fun StatusCard(
    state: RadioState,
    onlineCount: Int,
    networkOk: Boolean,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (state) {
        RadioState.STANDBY -> "STANDBY" to StatusGray
        RadioState.TRANSMITTING -> "TX" to StatusOrange
        RadioState.RECEIVING -> "RX" to StatusGreen
        RadioState.ERROR -> "ERR" to StatusRed
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(BgCard)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // State indicator
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
            Text(text = label, style = PttTypography.StatusLabel, color = color)
        }

        // Online count
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "$onlineCount", style = PttTypography.StatNumber, color = TextWhite)
            Text(text = "ONLINE", style = PttTypography.StatLabel, color = TextSecondary)
        }

        // Network
        Text(
            text = if (networkOk) "NET OK" else "NET ERR",
            style = PttTypography.StatLabel,
            color = if (networkOk) StatusGreen else StatusRed
        )
    }
}
