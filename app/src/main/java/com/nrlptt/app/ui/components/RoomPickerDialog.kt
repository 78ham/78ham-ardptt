package com.nrlptt.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.nrlptt.app.network.RoomInfo
import com.nrlptt.app.theme.*

@Composable
fun RoomPickerDialog(
    rooms: List<RoomInfo>,
    currentRoomId: Int,
    onDismiss: () -> Unit,
    onRoomSelected: (Int) -> Unit
) {
    val d = rememberScreenDimens()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        titleContentColor = TextWhite,
        title = { Text("SELECT CHANNEL", fontSize = d.statusLabelSize, fontWeight = FontWeight.Bold, letterSpacing = androidx.compose.ui.unit.sp(2)) },
        text = {
            LazyColumn {
                itemsIndexed(rooms) { i, room ->
                    val sel = room.id == currentRoomId
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(d.cornerRadius))
                            .background(if (sel) BrandGreen.copy(alpha = 0.1f) else BgCardAlt)
                            .clickable { onRoomSelected(room.id) }
                            .padding(horizontal = d.cardPadding, vertical = d.cardPadding),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(room.name, fontSize = d.listItemBoldSize,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                color = if (sel) BrandGreen else TextPrimary)
                            Text("ID:${room.id}  N:${room.memberCount}", fontSize = d.captionSize, color = TextSecondary)
                        }
                        if (sel) Text("●", color = BrandGreen, fontSize = d.statusLabelSize + androidx.compose.ui.unit.sp(2))
                    }
                    if (i < rooms.size - 1) HorizontalDivider(color = Border, thickness = 0.5.dp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", fontSize = d.captionSize, color = TextSecondary)
            }
        }
    )
}
