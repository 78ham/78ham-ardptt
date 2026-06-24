package com.nrlptt.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.nrlptt.app.network.ConnectionState
import com.nrlptt.app.service.PttService
import com.nrlptt.app.ui.components.*
import com.nrlptt.app.theme.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    service: PttService,
    onSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val d = rememberScreenDimens()
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

    val allMessages by service.allMessages.collectAsState()
    val allActivity by service.allActivity.collectAsState()

    var showRoomPicker by remember { mutableStateOf(false) }
    val isConnected = connState == ConnectionState.CONNECTED

    val isLoggedIn = activeConn?.isLoggedIn?.collectAsState()?.value ?: false
    LaunchedEffect(activeConn, isLoggedIn) {
        if (isLoggedIn) { kotlinx.coroutines.delay(500); activeConn?.loadRoomList() }
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
            // Top bar — compact on small screens
            if (d.isCompact) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(BgDark).height(d.topBarHeight)
                        .padding(horizontal = d.smallGap + 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onSettings, modifier = Modifier.size(d.topBarHeight)) {
                        Icon(Icons.Filled.Settings, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(d.iconSize))
                    }
                    IconButton(onClick = onLogout, modifier = Modifier.size(d.topBarHeight)) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = null, tint = TextDim, modifier = Modifier.size(d.iconSize))
                    }
                }
            } else {
                TopAppBar(
                    title = {},
                    actions = {
                        IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = null, tint = TextSecondary) }
                        TextButton(onClick = onLogout) { Text("退出", fontSize = d.captionSize, color = TextDim) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
                )
            }

            ServerSwitcher(connections = connections, activeId = activeId, onSelect = { service.setActiveServer(it) })

            RoomSwitcher(roomName = roomName, roomId = currentRoom, onlineCount = onlineCount,
                onRoomClick = { if (isConnected) showRoomPicker = true })

            Spacer(modifier = Modifier.height(d.componentGap))
            StatusCard(state = radioState, onlineCount = onlineCount, networkOk = isConnected)
            Spacer(modifier = Modifier.height(d.componentGap))

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .clip(RoundedCornerShape(d.cornerRadius)).background(BgCard),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(allMessages, key = { "msg-${it.serverId}-${it.timestamp}-${it.callsign}" }) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = d.listItemPaddingH, vertical = d.listItemPaddingV),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(d.smallGap + 2.dp)
                    ) {
                        Text(msg.serverId.substringBefore(":"), fontSize = d.captionSize, color = TextDim,
                            modifier = Modifier.width(d.cardPadding * 4), maxLines = 1)
                        Text("消息", fontSize = d.captionSize, color = StatusBlue)
                        Text("${msg.callsign}-${msg.ssid}", fontSize = d.listItemBoldSize, fontWeight = FontWeight.Bold, color = StatusBlue)
                        Text(msg.text, fontSize = d.listItemSize, color = TextPrimary,
                            modifier = Modifier.weight(1f), maxLines = 1)
                        Text(msg.time, fontSize = d.captionSize, color = TextDim)
                    }
                }
                items(allActivity, key = { "act-${it.serverId}-${it.timestamp}-${it.type}" }) { entry ->
                    val color = when (entry.type) {
                        "TX" -> StatusOrange; "RX" -> StatusGreen; "MSG" -> StatusBlue
                        else -> TextSecondary
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = d.listItemPaddingH, vertical = d.listItemPaddingV),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(d.smallGap + 2.dp)
                    ) {
                        Text(entry.serverId.substringBefore(":"), fontSize = d.captionSize, color = TextDim,
                            modifier = Modifier.width(d.cardPadding * 4), maxLines = 1)
                        Text(entry.type, fontSize = d.captionSize, color = color)
                        Text(entry.callsign, fontSize = d.listItemBoldSize, fontWeight = FontWeight.Bold, color = color,
                            modifier = Modifier.weight(1f))
                        Text(entry.time, fontSize = d.captionSize, color = TextDim)
                    }
                }
            }

            val anyConnected by service.anyConnected.collectAsState()
            PttButton(
                isConnected = anyConnected, isTransmitting = isTransmitting,
                onPress = { service.startTx() }, onRelease = { service.stopTx() },
                modifier = Modifier.padding(horizontal = d.smallGap + 2.dp, vertical = d.smallGap + 2.dp)
            )
        }

        if (showRoomPicker) {
            RoomPickerDialog(rooms = roomList, currentRoomId = currentRoom,
                onDismiss = { showRoomPicker = false },
                onRoomSelected = { id -> activeConn?.joinRoom(id); showRoomPicker = false })
        }
    }
}
