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
import androidx.compose.ui.text.font.FontWeight
import com.nrlptt.app.theme.*
import kotlin.math.sin

@Composable
fun SpeakerIndicator(
    callsign: String,
    isReceiving: Boolean,
    audioLevel: Float,
    modifier: Modifier = Modifier
) {
    val d = rememberScreenDimens()
    val bgColor = if (isReceiving) StatusGreen.copy(alpha = 0.1f) else BgCard
    val textColor = if (isReceiving) StatusGreen else TextDim

    Row(
        modifier = modifier.fillMaxWidth()
            .clip(RoundedCornerShape(d.cornerRadius))
            .background(bgColor)
            .padding(horizontal = d.cardPadding, vertical = d.speakerPaddingV),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(d.cardPadding)
    ) {
        Icon(Icons.Filled.Mic, contentDescription = null, tint = textColor, modifier = Modifier.size(d.iconSize))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isReceiving && callsign.isNotEmpty()) callsign else "---",
                fontSize = d.speakerNameSize, fontWeight = FontWeight.Bold, color = textColor
            )
            AudioWaveform(isActive = isReceiving, level = audioLevel, height = d.waveformHeight, barWidth = d.waveformBarWidth)
        }
        if (isReceiving) BlinkingDot(StatusGreen, d.dotSize)
    }
}

@Composable
private fun AudioWaveform(isActive: Boolean, level: Float, height: androidx.compose.ui.unit.Dp, barWidth: androidx.compose.ui.unit.Dp) {
    val barCount = 16
    val animProgress by rememberInfiniteTransition().animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing))
    )
    val maxH = height.value

    Row(
        modifier = Modifier.fillMaxWidth().height(height),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0 until barCount) {
            val h = if (isActive) {
                val wave = sin((animProgress * 2 * Math.PI + i * 0.5).toFloat()).let { if (it < 0) -it else it }
                (wave * level.coerceIn(0.1f, 1f) * maxH).coerceIn(2f, maxH)
            } else 2f

            Box(
                modifier = Modifier.width(barWidth).height(h.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (isActive) StatusGreen else StatusGray)
            )
        }
    }
}

@Composable
private fun BlinkingDot(color: androidx.compose.ui.graphics.Color, size: androidx.compose.ui.unit.Dp) {
    val alpha by rememberInfiniteTransition().animateFloat(
        initialValue = 1f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse)
    )
    Box(
        modifier = Modifier.size(size)
            .clip(RoundedCornerShape(size / 2))
            .background(color.copy(alpha = alpha))
    )
}
