package com.nrlptt.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nrlptt.app.theme.*

@Composable
fun RoomSwitcher(
    roomName: String,
    roomId: Int,
    onlineCount: Int,
    onRoomClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BgCard)
            .clickable { onRoomClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = roomName.ifEmpty { "SELECT CHANNEL" },
                style = PttTypography.RoomName,
                color = TextWhite,
                maxLines = 1
            )
            Text(
                text = "ID:$roomId  ON:$onlineCount",
                style = PttTypography.Caption,
                color = TextSecondary
            )
        }
        Icon(
            Icons.Filled.ArrowDropDown,
            contentDescription = null,
            tint = BrandGreen,
            modifier = Modifier.size(28.dp)
        )
    }
}
