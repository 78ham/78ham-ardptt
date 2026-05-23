package com.nrlptt.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.nrlptt.app.theme.*

@Composable
fun PttButton(
    isConnected: Boolean,
    isTransmitting: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isTransmitting -> PttActive
        isConnected -> BgElevated
        else -> BgCardAlt
    }
    val textColor = when {
        isTransmitting -> TextWhite
        isConnected -> TextPrimary
        else -> TextDim
    }
    val label = when {
        isTransmitting -> "TRANSMITTING"
        isConnected -> "HOLD TO TALK"
        else -> "OFFLINE"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .pointerInput(isConnected) {
                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown(requireUnconsumed = false)
                        if (isConnected) onPress()
                        waitForUpOrCancellation()
                        onRelease()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                if (isTransmitting) Icons.Filled.Mic else Icons.Filled.MicOff,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
            Text(text = label, style = PttTypography.ButtonLabel, color = textColor)
        }
    }
}
