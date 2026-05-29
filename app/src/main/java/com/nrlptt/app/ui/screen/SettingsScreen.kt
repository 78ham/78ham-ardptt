package com.nrlptt.app.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.nrlptt.app.R
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.nrlptt.app.data.AudioCodec
import com.nrlptt.app.data.ServerConfig
import com.nrlptt.app.data.SettingsRepository
import com.nrlptt.app.data.UserSettings
import com.nrlptt.app.network.ApiClient
import com.nrlptt.app.theme.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val d = rememberScreenDimens()
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    val saved by repo.settings.collectAsState()
    val servers by repo.servers.collectAsState()
    val scope = rememberCoroutineScope()

    var codec by remember { mutableStateOf(saved.codec) }
    var volume by remember { mutableStateOf(saved.volume) }
    var screenOff by remember { mutableStateOf(saved.screenOffPtt) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDiscoverDialog by remember { mutableStateOf(false) }
    var editingServer by remember { mutableStateOf<ServerConfig?>(null) }

    LaunchedEffect(saved) { codec = saved.codec; volume = saved.volume; screenOff = saved.screenOffPtt }

    val fc = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = BrandGreen, unfocusedBorderColor = Border,
        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = BrandGreen
    )

    Box(modifier = Modifier.fillMaxSize().background(BgBlack)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                modifier = Modifier.fillMaxWidth().background(BgDark).padding(horizontal = d.smallGap + 2.dp, vertical = d.smallGap + 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextSecondary, modifier = Modifier.size(d.iconSize)) }
                Text("SETTINGS", fontSize = d.statusLabelSize, fontWeight = FontWeight.Bold, color = TextWhite, letterSpacing = androidx.compose.ui.unit.sp(2))
            }

            Column(modifier = Modifier.padding(d.cardPadding), verticalArrangement = Arrangement.spacedBy(d.cardPadding)) {
                Section("SERVERS", d) {
                    servers.forEach { cfg ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(d.smallGap + 2.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cfg.displayLabel, fontSize = d.listItemBoldSize, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("${cfg.host}:${cfg.port}", fontSize = d.captionSize, color = TextSecondary)
                            }
                            if (cfg.username.isNotEmpty()) Text(cfg.username, fontSize = d.captionSize, color = TextDim)
                            IconButton(onClick = { editingServer = cfg }, modifier = Modifier.size(d.iconSize)) {
                                Icon(Icons.Filled.Edit, null, tint = TextSecondary, modifier = Modifier.size(d.smallIconSize))
                            }
                            IconButton(onClick = { repo.removeServer(cfg.id) }, modifier = Modifier.size(d.iconSize)) {
                                Icon(Icons.Filled.Delete, null, tint = StatusRed, modifier = Modifier.size(d.smallIconSize))
                            }
                        }
                        HorizontalDivider(color = Border, thickness = 0.5.dp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(d.smallGap + 2.dp)) {
                        TextButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Filled.Add, null, tint = BrandGreen, modifier = Modifier.size(d.smallIconSize))
                            Spacer(Modifier.width(d.smallGap))
                            Text("CUSTOM", fontSize = d.captionSize, color = BrandGreen)
                        }
                        TextButton(onClick = { showDiscoverDialog = true }) {
                            Icon(painterResource(R.drawable.ic_public), null, tint = BrandGreen, modifier = Modifier.size(d.smallIconSize))
                            Spacer(Modifier.width(d.smallGap))
                            Text("DISCOVER", fontSize = d.captionSize, color = BrandGreen)
                        }
                    }
                }

                Section("AUDIO", d) {
                    Row(horizontalArrangement = Arrangement.spacedBy(d.cardPadding * 1.5f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(codec == AudioCodec.G711, { codec = AudioCodec.G711 }, colors = RadioButtonDefaults.colors(selectedColor = BrandGreen))
                            Text("G711", fontSize = d.listItemSize, color = TextPrimary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(codec == AudioCodec.OPUS, { codec = AudioCodec.OPUS }, colors = RadioButtonDefaults.colors(selectedColor = BrandGreen))
                            Text("OPUS", fontSize = d.listItemSize, color = TextPrimary)
                        }
                    }
                    Text("VOL: $volume%", fontSize = d.captionSize, color = TextSecondary)
                    Slider(volume.toFloat(), { volume = it.toInt() }, valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = BrandGreen, activeTrackColor = BrandGreen))
                }

                Section("PTT", d) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("SCREEN OFF PTT", fontSize = d.listItemSize, color = TextPrimary)
                        Switch(screenOff, { screenOff = it }, colors = SwitchDefaults.colors(checkedTrackColor = BrandGreen))
                    }
                }

                Button(
                    onClick = {
                        repo.save(UserSettings(codec = codec, volume = volume, screenOffPtt = screenOff))
                        Toast.makeText(context, "SAVED", Toast.LENGTH_SHORT).show(); onBack()
                    },
                    modifier = Modifier.fillMaxWidth().height(d.pttHeight - 8.dp),
                    shape = RoundedCornerShape(d.cornerRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen, contentColor = BgBlack)
                ) { Text("SAVE", fontSize = d.buttonLabelSize, fontWeight = FontWeight.Bold, letterSpacing = androidx.compose.ui.unit.sp(2)) }

                Spacer(Modifier.height(d.cardPadding * 2))
            }
        }

        if (showAddDialog) AddServerDialog(d, onDismiss = { showAddDialog = false }, onAdd = { repo.addServer(it); showAddDialog = false })
        if (showDiscoverDialog) DiscoverDialog(d, onDismiss = { showDiscoverDialog = false }, onAdd = { repo.addServer(it) })
        editingServer?.let { cfg -> EditServerDialog(d, cfg, onDismiss = { editingServer = null }, onSave = { repo.updateServer(cfg.id, it); editingServer = null }) }
    }
}

@Composable
private fun DiscoverDialog(d: ScreenDimens, onDismiss: () -> Unit, onAdd: (ServerConfig) -> Unit) {
    var servers by remember { mutableStateOf<List<com.nrlptt.app.network.PlatformServer>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    val existingIds = remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) {
        val result = ApiClient.getPlatformList()
        result.fold(onSuccess = { servers = it; loading = false }, onFailure = { error = it.message ?: "Failed"; loading = false })
    }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = BgCard,
        title = { Text("DISCOVER SERVERS", fontSize = d.statusLabelSize, fontWeight = FontWeight.Bold, color = TextWhite, letterSpacing = androidx.compose.ui.unit.sp(2)) },
        text = {
            Column {
                if (loading) { CircularProgressIndicator(Modifier.size(d.iconSize + 4.dp), color = BrandGreen); return@Column }
                if (error.isNotEmpty()) { Text(error, color = StatusRed, fontSize = d.captionSize); return@Column }
                if (servers.isEmpty()) { Text("No servers found", color = TextDim, fontSize = d.captionSize); return@Column }
                servers.forEach { srv ->
                    val alreadyAdded = existingIds.value.contains("${srv.host}:${srv.port}")
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = d.smallGap + 1.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.smallGap + 2.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(srv.name, fontSize = d.listItemBoldSize, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("${srv.host}:${srv.port}  ON:${srv.online}", fontSize = d.captionSize, color = TextSecondary)
                        }
                        TextButton(onClick = {
                            onAdd(ServerConfig(srv.host, srv.port.toIntOrNull() ?: 60050, label = srv.name, autoConnect = true))
                            existingIds.value = existingIds.value + "${srv.host}:${srv.port}"
                        }, enabled = !alreadyAdded) { Text(if (alreadyAdded) "ADDED" else "ADD", color = if (alreadyAdded) TextDim else BrandGreen, fontSize = d.captionSize) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE", fontSize = d.captionSize, color = TextSecondary) } }
    )
}

@Composable
private fun EditServerDialog(d: ScreenDimens, config: ServerConfig, onDismiss: () -> Unit, onSave: (ServerConfig) -> Unit) {
    var host by remember { mutableStateOf(config.host) }
    var port by remember { mutableStateOf(config.port.toString()) }
    var user by remember { mutableStateOf(config.username) }
    var pass by remember { mutableStateOf(config.password) }
    var label by remember { mutableStateOf(config.label) }
    var autoConnect by remember { mutableStateOf(config.autoConnect) }

    val fc = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandGreen, unfocusedBorderColor = Border,
        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = BrandGreen)

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = BgCard,
        title = { Text("EDIT SERVER", fontSize = d.statusLabelSize, fontWeight = FontWeight.Bold, color = TextWhite, letterSpacing = androidx.compose.ui.unit.sp(2)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(d.smallGap + 2.dp)) {
                OutlinedTextField(host, { host = it }, label = { Text("HOST") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = fc)
                OutlinedTextField(port, { port = it.filter { c -> c.isDigit() } }, label = { Text("PORT") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = fc)
                OutlinedTextField(label, { label = it }, label = { Text("LABEL") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = fc)
                OutlinedTextField(user, { user = it }, label = { Text("USERNAME") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = fc)
                OutlinedTextField(pass, { pass = it }, label = { Text("PASSWORD") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), colors = fc)
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(autoConnect, { autoConnect = it }, colors = CheckboxDefaults.colors(checkedColor = BrandGreen)); Text("AUTO CONNECT", fontSize = d.listItemSize, color = TextPrimary) }
            }
        },
        confirmButton = { TextButton(onClick = { if (host.isNotEmpty()) onSave(ServerConfig(host, port.toIntOrNull() ?: 60050, user, pass, autoConnect, label)) }) { Text("SAVE", color = BrandGreen, fontSize = d.captionSize) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = TextSecondary, fontSize = d.captionSize) } }
    )
}

@Composable
private fun AddServerDialog(d: ScreenDimens, onDismiss: () -> Unit, onAdd: (ServerConfig) -> Unit) {
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("60050") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var autoConnect by remember { mutableStateOf(true) }

    val fc = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandGreen, unfocusedBorderColor = Border,
        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = BrandGreen)

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = BgCard,
        title = { Text("ADD SERVER", fontSize = d.statusLabelSize, fontWeight = FontWeight.Bold, color = TextWhite, letterSpacing = androidx.compose.ui.unit.sp(2)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(d.smallGap + 2.dp)) {
                OutlinedTextField(host, { host = it }, label = { Text("HOST") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = fc)
                OutlinedTextField(port, { port = it.filter { c -> c.isDigit() } }, label = { Text("PORT") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = fc)
                OutlinedTextField(label, { label = it }, label = { Text("LABEL (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = fc)
                OutlinedTextField(user, { user = it }, label = { Text("USERNAME") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = fc)
                OutlinedTextField(pass, { pass = it }, label = { Text("PASSWORD") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), colors = fc)
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(autoConnect, { autoConnect = it }, colors = CheckboxDefaults.colors(checkedColor = BrandGreen)); Text("AUTO CONNECT", fontSize = d.listItemSize, color = TextPrimary) }
            }
        },
        confirmButton = { TextButton(onClick = { if (host.isNotEmpty() && user.isNotEmpty()) onAdd(ServerConfig(host, port.toIntOrNull() ?: 60050, user, pass, autoConnect, label)) }) { Text("ADD", color = BrandGreen, fontSize = d.captionSize) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = TextSecondary, fontSize = d.captionSize) } }
    )
}

@Composable
private fun Section(title: String, d: ScreenDimens, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(d.smallGap + 2.dp)) {
        Text(title, fontSize = d.captionSize + androidx.compose.ui.unit.sp(2), fontWeight = FontWeight.Bold, color = BrandGreen, letterSpacing = androidx.compose.ui.unit.sp(1))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(d.cornerRadius), colors = CardDefaults.cardColors(containerColor = BgCard)) {
            Column(modifier = Modifier.padding(d.cardPadding), verticalArrangement = Arrangement.spacedBy(d.smallGap + 2.dp)) { content() }
        }
    }
}
