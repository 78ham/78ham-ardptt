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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nrlptt.app.network.ApiClient
import com.nrlptt.app.theme.*

@Composable
fun RoomPickerDialog(
    rooms: List<ApiClient.RoomInfo>,
    currentRoomId: Int,
    onDismiss: () -> Unit,
    onRoomSelected: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        titleContentColor = TextWhite,
        title = { Text("SELECT CHANNEL", style = PttTypography.StatusLabel) },
        text = {
            LazyColumn {
                itemsIndexed(rooms) { i, room ->
                    val sel = room.id == currentRoomId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (sel) BrandGreen.copy(alpha = 0.1f) else BgCardAlt)
                            .clickable { onRoomSelected(room.id) }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(room.name, style = PttTypography.ListItemBold,
                                color = if (sel) BrandGreen else TextPrimary)
                            Text("ID:${room.id}  N:${room.memberCount}", style = PttTypography.Caption,
                                color = TextSecondary)
                        }
                        if (sel) Text("●", color = BrandGreen, fontSize = 18.sp)
                    }
                    if (i < rooms.size - 1) HorizontalDivider(color = Border, thickness = 0.5.dp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = TextSecondary, style = PttTypography.Caption)
            }
        }
    )
}
