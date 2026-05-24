package com.nrlptt.app.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nrlptt.app.data.AudioCodec
import com.nrlptt.app.data.SettingsRepository
import com.nrlptt.app.data.UserSettings
import com.nrlptt.app.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    val saved by repo.settings.collectAsState()

    var server by remember { mutableStateOf(saved.serverAddress) }
    var port by remember { mutableStateOf(saved.serverPort.toString()) }
    var user by remember { mutableStateOf(saved.username) }
    var pass by remember { mutableStateOf(saved.password) }
    var ssid by remember { mutableStateOf(saved.ssid.toString()) }
    var codec by remember { mutableStateOf(saved.codec) }
    var volume by remember { mutableStateOf(saved.volume) }
    var screenOff by remember { mutableStateOf(saved.screenOffPtt) }
    var autoConn by remember { mutableStateOf(saved.autoConnect) }

    LaunchedEffect(saved) {
        server = saved.serverAddress; port = saved.serverPort.toString()
        user = saved.username; pass = saved.password; ssid = saved.ssid.toString()
        codec = saved.codec; volume = saved.volume; screenOff = saved.screenOffPtt
        autoConn = saved.autoConnect
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = BrandGreen, unfocusedBorderColor = Border,
        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = BrandGreen
    )

    Box(modifier = Modifier.fillMaxSize().background(BgBlack)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                modifier = Modifier.fillMaxWidth().background(BgDark).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextSecondary) }
                Text("SETTINGS", style = PttTypography.StatusLabel, color = TextWhite)
            }

            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Section("SERVER") {
                    OutlinedTextField(server, { server = it }, label = { Text("HOST") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, colors = fieldColors)
                    OutlinedTextField(port, { port = it.filter { c -> c.isDigit() } }, label = { Text("PORT") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = fieldColors)
                }

                Section("ACCOUNT") {
                    OutlinedTextField(user, { user = it }, label = { Text("USERNAME") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, colors = fieldColors)
                    OutlinedTextField(pass, { pass = it }, label = { Text("PASSWORD") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), colors = fieldColors)
                }

                Section("DEVICE") {
                    if (saved.callsign.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("CALLSIGN: ${saved.callsign}", style = PttTypography.ListItemBold, color = BrandGreen)
                            Text("DMR: ${saved.dmrId}", style = PttTypography.ListItemBold, color = BrandGreen)
                        }
                    }
                    OutlinedTextField(ssid, { ssid = it.filter { c -> c.isDigit() } }, label = { Text("SSID") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = fieldColors)
                }

                Section("AUDIO") {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(codec == AudioCodec.G711, { codec = AudioCodec.G711 },
                                colors = RadioButtonDefaults.colors(selectedColor = BrandGreen))
                            Text("G711", style = PttTypography.ListItem, color = TextPrimary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(codec == AudioCodec.OPUS, { codec = AudioCodec.OPUS },
                                colors = RadioButtonDefaults.colors(selectedColor = BrandGreen))
                            Text("OPUS", style = PttTypography.ListItem, color = TextPrimary)
                        }
                    }
                    Text("VOL: $volume%", style = PttTypography.Caption, color = TextSecondary)
                    Slider(volume.toFloat(), { volume = it.toInt() }, valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = BrandGreen, activeTrackColor = BrandGreen))
                }

                Section("PTT") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("SCREEN OFF PTT", style = PttTypography.ListItem, color = TextPrimary)
                        Switch(screenOff, { screenOff = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = BrandGreen))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("AUTO CONNECT", style = PttTypography.ListItem, color = TextPrimary)
                        Switch(autoConn, { autoConn = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = BrandGreen))
                    }
                }

                Button(
                    onClick = {
                        repo.save(UserSettings(
                            serverAddress = server, serverPort = port.toIntOrNull() ?: 60050,
                            username = user, password = pass,
                            callsign = saved.callsign, dmrId = saved.dmrId,
                            ssid = ssid.toIntOrNull() ?: 178, codec = codec, volume = volume,
                            screenOffPtt = screenOff, autoConnect = autoConn
                        ))
                        Toast.makeText(context, "SAVED", Toast.LENGTH_SHORT).show(); onBack()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen, contentColor = BgBlack)
                ) { Text("SAVE", style = PttTypography.ButtonLabel) }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = PttTypography.StatLabel, color = BrandGreen)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = BgCard)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                content()
            }
        }
    }
}
