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
import kotlin.math.sin

@Composable
fun SpeakerIndicator(
    callsign: String,
    isReceiving: Boolean,
    audioLevel: Float,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isReceiving) StatusGreen.copy(alpha = 0.1f) else BgCard
    val textColor = if (isReceiving) StatusGreen else TextDim

    Row(
        modifier = modifier.fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Filled.Mic, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isReceiving && callsign.isNotEmpty()) callsign else "---",
                style = PttTypography.SpeakerName, color = textColor
            )
            // Waveform bars
            AudioWaveform(isActive = isReceiving, level = audioLevel)
        }

        if (isReceiving) BlinkingDot(StatusGreen)
    }
}

@Composable
private fun AudioWaveform(isActive: Boolean, level: Float) {
    val barCount = 16
    val animProgress by rememberInfiniteTransition().animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing))
    )

    Row(
        modifier = Modifier.fillMaxWidth().height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0 until barCount) {
            val h = if (isActive) {
                val wave = sin((animProgress * 2 * Math.PI + i * 0.5).toFloat()).absoluteValue
                val base = level.coerceIn(0.1f, 1f)
                (wave * base * 16f).coerceIn(2f, 16f)
            } else 2f

            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(h.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (isActive) StatusGreen else StatusGray)
            )
        }
    }
}

private val Float.absoluteValue get() = if (this < 0) -this else this

@Composable
private fun BlinkingDot(color: androidx.compose.ui.graphics.Color) {
    val alpha by rememberInfiniteTransition().animateFloat(
        initialValue = 1f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse)
    )
    Box(
        modifier = Modifier.size(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(color.copy(alpha = alpha))
    )
}
