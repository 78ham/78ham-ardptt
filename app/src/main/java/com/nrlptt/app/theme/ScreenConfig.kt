package com.nrlptt.app.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ScreenDimens(
    val scale: Float,
    val isCompact: Boolean,
    val roomNameSize: TextUnit,
    val statusLabelSize: TextUnit,
    val statNumberSize: TextUnit,
    val speakerNameSize: TextUnit,
    val listItemSize: TextUnit,
    val listItemBoldSize: TextUnit,
    val captionSize: TextUnit,
    val buttonLabelSize: TextUnit,
    val statusValueSize: TextUnit,
    val pttHeight: Dp,
    val cardPadding: Dp,
    val componentGap: Dp,
    val smallGap: Dp,
    val iconSize: Dp,
    val smallIconSize: Dp,
    val waveformHeight: Dp,
    val waveformBarWidth: Dp,
    val dotSize: Dp,
    val topBarHeight: Dp,
    val serverTabPadding: Dp,
    val listItemPaddingH: Dp,
    val listItemPaddingV: Dp,
    val roomSwitcherPaddingV: Dp,
    val statusCardPaddingV: Dp,
    val speakerPaddingV: Dp,
    val cornerRadius: Dp,
)

@Composable
fun rememberScreenDimens(): ScreenDimens {
    val config = LocalConfiguration.current
    val w = config.screenWidthDp

    return remember(w) {
        val scale = when {
            w < 320 -> 0.7f
            w < 360 -> 0.8f
            w < 400 -> 0.9f
            else -> 1.0f
        }
        val isCompact = w < 360

        fun sp(base: Float) = (base * scale).sp
        fun dp(base: Float) = (base * scale).dp

        ScreenDimens(
            scale = scale,
            isCompact = isCompact,
            roomNameSize = sp(20f),
            statusLabelSize = sp(14f),
            statNumberSize = sp(18f),
            speakerNameSize = sp(18f),
            listItemSize = sp(12f),
            listItemBoldSize = sp(12f),
            captionSize = sp(10f),
            buttonLabelSize = sp(16f),
            statusValueSize = sp(24f),
            pttHeight = dp(56f),
            cardPadding = dp(10f),
            componentGap = dp(3f),
            smallGap = dp(1.5f),
            iconSize = dp(20f),
            smallIconSize = dp(14f),
            waveformHeight = dp(16f),
            waveformBarWidth = dp(2f),
            dotSize = dp(10f),
            topBarHeight = dp(48f),
            serverTabPadding = dp(6f),
            listItemPaddingH = dp(10f),
            listItemPaddingV = dp(4f),
            roomSwitcherPaddingV = dp(8f),
            statusCardPaddingV = dp(8f),
            speakerPaddingV = dp(8f),
            cornerRadius = dp(4f),
        )
    }
}
