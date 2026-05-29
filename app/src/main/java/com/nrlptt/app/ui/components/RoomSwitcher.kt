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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.nrlptt.app.theme.*
import androidx.compose.ui.unit.dp

@Composable
fun RoomSwitcher(
    roomName: String,
    roomId: Int,
    onlineCount: Int,
    onRoomClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val d = rememberScreenDimens()
    Row(
        modifier = modifier.fillMaxWidth()
            .background(BgCard)
            .clickable { onRoomClick() }
            .padding(horizontal = d.cardPadding, vertical = d.roomSwitcherPaddingV),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = roomName.ifEmpty { "SELECT CHANNEL" },
                fontSize = d.roomNameSize, fontWeight = FontWeight.Bold,
                color = TextWhite, maxLines = 1
            )
            Text(
                text = "ID:$roomId  ON:$onlineCount",
                fontSize = d.captionSize, color = TextSecondary
            )
        }
        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(d.iconSize + 4.dp))
    }
}
