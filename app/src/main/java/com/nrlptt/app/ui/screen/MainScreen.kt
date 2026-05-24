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
    val connections by service.connections.collectAsState()
    val activeId by service.activeServerId.collectAsState()
    val isTransmitting by service.isTransmitting.collectAsState()
    val isReceiving by service.isReceiving.collectAsState()

    val activeConn = connections[activeId]
    val connState = activeConn?.connState?.collectAsState()?.value ?: ConnectionState.DISCONNECTED
    val currentRoom = activeConn?.currentRoom?.collectAsState()?.value ?: 0
    val roomName = activeConn?.roomName?.collectAsState()?.value ?: ""
    val onlineCount = activeConn?.onlineCount?.collectAsState()?.value ?: 0
    val roomList = activeConn?.roomList?.collectAsState()?.value ?: emptyList()

    // Merge messages and activity from all connections
    val allMessages = connections.values.flatMap { it.messages.value }.sortedByDescending { it.time }.take(5)
    val allActivity = connections.values.flatMap { it.activityLog.value }.sortedByDescending { it.time }.take(5)

    var showRoomPicker by remember { mutableStateOf(false) }
    val isConnected = connState == ConnectionState.CONNECTED

    // Load rooms on login
    LaunchedEffect(activeConn?.isLoggedIn?.value) {
        if (activeConn?.isLoggedIn?.value == true) { kotlinx.coroutines.delay(500); activeConn.loadRoomList() }
    }

    val radioState = remember(isTransmitting, isReceiving, isConnected) {
        when {
            isTransmitting -> RadioState.TRANSMITTING
            isReceiving -> RadioState.RECEIVING
            !isConnected -> RadioState.ERROR
            else -> RadioState.STANDBY
        }
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

            // Server switcher
            ServerSwitcher(
                connections = connections,
                activeId = activeId,
                onSelect = { service.setActiveServer(it) }
            )

            // Room switcher
            RoomSwitcher(
                roomName = roomName,
                roomId = currentRoom,
                onlineCount = onlineCount,
                onRoomClick = { if (isConnected) showRoomPicker = true }
            )

            Spacer(modifier = Modifier.height(3.dp))
            StatusCard(state = radioState, onlineCount = onlineCount, networkOk = isConnected)
            Spacer(modifier = Modifier.height(3.dp))

            // Combined message + activity list
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp)).background(BgCard),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(allMessages, key = { "msg-${it.serverId}-${it.time}-${it.callsign}" }) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(msg.serverId.substringBefore(":"), style = PttTypography.Caption,
                            color = TextDim, modifier = Modifier.width(40.dp), maxLines = 1)
                        Text("MSG", style = PttTypography.Caption, color = StatusBlue)
                        Text("${msg.callsign}-${msg.ssid}", style = PttTypography.ListItemBold, color = StatusBlue)
                        Text(msg.text, style = PttTypography.ListItem, color = TextPrimary,
                            modifier = Modifier.weight(1f), maxLines = 1)
                        Text(msg.time, style = PttTypography.Caption, color = TextDim)
                    }
                }
                items(allActivity, key = { "act-${it.serverId}-${it.time}-${it.type}" }) { entry ->
                    val color = when (entry.type) {
                        "TX" -> StatusOrange; "RX" -> StatusGreen; "MSG" -> StatusBlue
                        else -> TextSecondary
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(entry.serverId.substringBefore(":"), style = PttTypography.Caption,
                            color = TextDim, modifier = Modifier.width(40.dp), maxLines = 1)
                        Text(entry.type, style = PttTypography.Caption, color = color)
                        Text(entry.callsign, style = PttTypography.ListItemBold, color = color,
                            modifier = Modifier.weight(1f))
                        Text(entry.time, style = PttTypography.Caption, color = TextDim)
                    }
                }
            }

            // PTT button
            val anyConnected = connections.values.any { it.connState.value == ConnectionState.CONNECTED }
            PttButton(
                isConnected = anyConnected, isTransmitting = isTransmitting,
                onPress = { service.startTx() }, onRelease = { service.stopTx() },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }

        if (showRoomPicker) {
            RoomPickerDialog(rooms = roomList, currentRoomId = currentRoom,
                onDismiss = { showRoomPicker = false },
                onRoomSelected = { id -> activeConn?.joinRoom(id); showRoomPicker = false })
        }
    }
}
