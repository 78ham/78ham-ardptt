package com.nrlptt.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.nrlptt.app.theme.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class RadioState { STANDBY, TRANSMITTING, RECEIVING, ERROR }

@Composable
fun StatusCard(
    state: RadioState,
    onlineCount: Int,
    networkOk: Boolean,
    modifier: Modifier = Modifier
) {
    val d = rememberScreenDimens()
    val (label, color) = when (state) {
        RadioState.STANDBY -> "STANDBY" to StatusGray
        RadioState.TRANSMITTING -> "TX" to StatusOrange
        RadioState.RECEIVING -> "RX" to StatusGreen
        RadioState.ERROR -> "ERR" to StatusRed
    }

    Row(
        modifier = modifier.fillMaxWidth()
            .clip(RoundedCornerShape(d.cornerRadius))
            .background(BgCard)
            .padding(horizontal = d.cardPadding, vertical = d.statusCardPaddingV),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.smallGap)) {
            Box(Modifier.size(d.dotSize * 0.8f).clip(RoundedCornerShape(d.dotSize * 0.4f)).background(color))
            Text(text = label, fontSize = d.statusLabelSize, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = color, letterSpacing = androidx.compose.ui.unit.sp(2))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.smallGap)) {
            Text(text = "$onlineCount", fontSize = d.statNumberSize, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = TextWhite)
            Text(text = "ONLINE", fontSize = d.captionSize, color = TextSecondary, letterSpacing = androidx.compose.ui.unit.sp(1))
        }
        Text(text = if (networkOk) "NET OK" else "NET ERR", fontSize = d.captionSize, color = if (networkOk) StatusGreen else StatusRed, letterSpacing = androidx.compose.ui.unit.sp(1))
    }
}
