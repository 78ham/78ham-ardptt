package com.nrlptt.app.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object PttTypography {
    val RoomName = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 1.sp
    )
    val StatusLabel = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 2.sp
    )
    val StatusValue = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = 1.sp
    )
    val StatNumber = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )
    val StatLabel = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        letterSpacing = 1.sp
    )
    val SpeakerName = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = 1.sp
    )
    val ListItem = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    )
    val ListItemBold = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
    )
    val Caption = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp
    )
    val ButtonLabel = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 2.sp
    )
}
