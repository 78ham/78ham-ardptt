package com.nrlptt.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nrlptt.app.theme.*

@Composable
fun SpeakerIndicator(
    callsign: String,
    isReceiving: Boolean,
    audioLevel: Float,
    modifier: Modifier = Modifier
    ) {
    val bgColor = if (isReceiving) StatusGreen.copy(alpha = 0.1f) else BgCard
    val textColor = if (isReceiving) StatusGreen else TextDim
    val barColor = if (isReceiving) StatusGreen else StatusGray

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            Icons.Filled.Mic,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(20.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isReceiving && callsign.isNotEmpty()) callsign else "---",
                style = PttTypography.SpeakerName,
                color = textColor
            )
            // Audio level bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(BgElevated)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(if (isReceiving) audioLevel else 0f)
                        .clip(RoundedCornerShape(2.dp))
                        .background(barColor)
                )
            }
        }

        if (isReceiving) {
            BlinkingDot(StatusGreen)
        }
    }
}

@Composable
private fun BlinkingDot(color: androidx.compose.ui.graphics.Color) {
    val alpha by rememberInfiniteTransition().animateFloat(
        initialValue = 1f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse)
    )
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(color.copy(alpha = alpha))
    )
}
