package com.nrlptt.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    var showRoomPicker by remember { mutableStateOf(false) }

    val isConnected = connState == ConnectionState.CONNECTED

    // Load rooms on login
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

            Spacer(modifier = Modifier.height(4.dp))

            // Status card
            StatusCard(
                state = radioState,
                onlineCount = onlineCount,
                networkOk = isConnected
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Speaker indicator
            SpeakerIndicator(
                callsign = lastSpeaker,
                isReceiving = isReceiving,
                audioLevel = 0f
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Activity log
            ActivityList(
                entries = activityLog,
                modifier = Modifier.weight(1f)
            )

            // PTT button
            PttButton(
                isConnected = isConnected,
                isTransmitting = isTransmitting,
                onPress = { service.startTx() },
                onRelease = { service.stopTx() },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }

        // Room picker dialog
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
