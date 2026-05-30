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
        RadioState.STANDBY -> "待机" to StatusGray
        RadioState.TRANSMITTING -> "发射" to StatusOrange
        RadioState.RECEIVING -> "接收" to StatusGreen
        RadioState.ERROR -> "故障" to StatusRed
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
            Text(text = label, fontSize = d.statusLabelSize, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = color, letterSpacing = 2.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.smallGap)) {
            Text(text = "$onlineCount", fontSize = d.statNumberSize, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = TextWhite)
            Text(text = "在线", fontSize = d.captionSize, color = TextSecondary, letterSpacing = 1.sp)
        }
        Text(text = if (networkOk) "网络正常" else "网络异常", fontSize = d.captionSize, color = if (networkOk) StatusGreen else StatusRed, letterSpacing = 1.sp)
    }
}
