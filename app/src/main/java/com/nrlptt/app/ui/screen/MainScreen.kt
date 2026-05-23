package com.nrlptt.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nrlptt.app.network.ConnectionState
import com.nrlptt.app.service.PttService
import com.nrlptt.app.ui.components.*
import com.nrlptt.app.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    service: PttService,
    onSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val connState by service.connState.collectAsState()
    val isLoggedIn by service.isLoggedIn.collectAsState()
    val currentRoom by service.currentRoom.collectAsState()
    val roomName by service.roomName.collectAsState()
    val onlineCount by service.onlineCount.collectAsState()
    val lastSpeaker by service.lastSpeaker.collectAsState()
    val activityLog by service.activityLog.collectAsState()
    val isTransmitting by service.isTransmitting.collectAsState()
    val isReceiving by service.isReceiving.collectAsState()
    val roomList by service.roomList.collectAsState()
    val messages by service.messages.collectAsState()

    var showRoomPicker by remember { mutableStateOf(false) }
    val isConnected = connState == ConnectionState.CONNECTED

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) { kotlinx.coroutines.delay(500); service.loadRoomList() }
    }

    val radioState = when {
        isTransmitting -> RadioState.TRANSMITTING
        isReceiving -> RadioState.RECEIVING
        !isConnected -> RadioState.ERROR
        else -> RadioState.STANDBY
    }

    Box(modifier = Modifier.fillMaxSize().background(BgBlack)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            TopAppBar(
                title = {},
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = null, tint = TextSecondary)
                    }
                    TextButton(onClick = onLogout) {
                        Text("EXIT", style = PttTypography.Caption, color = TextDim)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
            )

            // Room switcher
            RoomSwitcher(
                roomName = roomName,
                roomId = currentRoom,
                onlineCount = onlineCount,
                onRoomClick = { showRoomPicker = true }
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Status card
            StatusCard(state = radioState, onlineCount = onlineCount, networkOk = isConnected)

            Spacer(modifier = Modifier.height(3.dp))

            // Speaker indicator
            SpeakerIndicator(callsign = lastSpeaker, isReceiving = isReceiving, audioLevel = 0f)

            Spacer(modifier = Modifier.height(3.dp))

            // Activity + Messages area
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp)).background(BgCard),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Text messages first (newest on top)
                items(messages.take(5)) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("MSG", style = PttTypography.Caption, color = StatusBlue,
                            modifier = Modifier.width(28.dp))
                        Text("${msg.callsign}-${msg.ssid}", style = PttTypography.ListItemBold,
                            color = StatusBlue)
                        Text(msg.text, style = PttTypography.ListItem, color = TextPrimary,
                            modifier = Modifier.weight(1f), maxLines = 1)
                        Text(msg.time, style = PttTypography.Caption, color = TextDim)
                    }
                }
                // Activity log
                items(activityLog.take(5)) { entry ->
                    ActivityRowInline(entry)
                }
            }

            // PTT button
            PttButton(
                isConnected = isConnected,
                isTransmitting = isTransmitting,
                onPress = { service.startTx() },
                onRelease = { service.stopTx() },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }

        if (showRoomPicker) {
            RoomPickerDialog(
                rooms = roomList,
                currentRoomId = currentRoom,
                onDismiss = { showRoomPicker = false },
                onRoomSelected = { id -> service.joinRoom(id); showRoomPicker = false }
            )
        }
    }
}

@Composable
private fun ActivityRowInline(entry: PttService.ActivityEntry) {
    val color = when (entry.type) {
        PttService.ActivityType.TX -> StatusOrange
        PttService.ActivityType.RX -> StatusGreen
        PttService.ActivityType.MSG -> StatusBlue
        else -> TextSecondary
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(entry.type.name, style = PttTypography.Caption, color = color,
            modifier = Modifier.width(28.dp))
        Text(entry.callsign, style = PttTypography.ListItemBold, color = color,
            modifier = Modifier.weight(1f))
        Text(entry.time, style = PttTypography.Caption, color = TextDim)
    }
}
