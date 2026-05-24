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
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nrlptt.app.data.AudioCodec
import com.nrlptt.app.data.ServerConfig
import com.nrlptt.app.data.SettingsRepository
import com.nrlptt.app.data.UserSettings
import com.nrlptt.app.network.ApiClient
import com.nrlptt.app.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
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
                modifier = Modifier.fillMaxWidth().background(BgDark).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextSecondary) }
                Text("SETTINGS", style = PttTypography.StatusLabel, color = TextWhite)
            }

            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Server list
                Section("SERVERS") {
                    servers.forEach { cfg ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cfg.displayLabel, style = PttTypography.ListItemBold, color = TextPrimary)
                                Text("${cfg.host}:${cfg.port}", style = PttTypography.Caption, color = TextSecondary)
                            }
                            if (cfg.username.isNotEmpty()) Text(cfg.username, style = PttTypography.Caption, color = TextDim)
                            IconButton(onClick = { editingServer = cfg }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Filled.Edit, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                            }
                            IconButton(onClick = { repo.removeServer(cfg.id) }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Filled.Delete, null, tint = StatusRed, modifier = Modifier.size(14.dp))
                            }
                        }
                        HorizontalDivider(color = Border, thickness = 0.5.dp)
                    }
                    // Action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Filled.Add, null, tint = BrandGreen, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("CUSTOM", style = PttTypography.Caption, color = BrandGreen)
                        }
                        TextButton(onClick = { showDiscoverDialog = true }) {
                            Icon(Icons.Filled.Public, null, tint = BrandGreen, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("DISCOVER", style = PttTypography.Caption, color = BrandGreen)
                        }
                    }
                }

                // Audio
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

                // PTT
                Section("PTT") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("SCREEN OFF PTT", style = PttTypography.ListItem, color = TextPrimary)
                        Switch(screenOff, { screenOff = it }, colors = SwitchDefaults.colors(checkedTrackColor = BrandGreen))
                    }
                }

                Button(
                    onClick = {
                        repo.save(UserSettings(codec = codec, volume = volume, screenOffPtt = screenOff))
                        Toast.makeText(context, "SAVED", Toast.LENGTH_SHORT).show(); onBack()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen, contentColor = BgBlack)
                ) { Text("SAVE", style = PttTypography.ButtonLabel) }

                Spacer(Modifier.height(24.dp))
            }
        }

        if (showAddDialog) AddServerDialog(onDismiss = { showAddDialog = false }, onAdd = { repo.addServer(it); showAddDialog = false })
        if (showDiscoverDialog) DiscoverDialog(onDismiss = { showDiscoverDialog = false }, onAdd = { repo.addServer(it) })
        editingServer?.let { cfg -> EditServerDialog(cfg, onDismiss = { editingServer = null }, onSave = { repo.updateServer(cfg.id, it); editingServer = null }) }
    }
}

@Composable
private fun DiscoverDialog(onDismiss: () -> Unit, onAdd: (ServerConfig) -> Unit) {
    var servers by remember { mutableStateOf<List<com.nrlptt.app.network.PlatformServer>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    val existingIds = remember { mutableStateOf(setOf<String>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val result = ApiClient.getPlatformList()
        result.fold(
            onSuccess = { servers = it; loading = false },
            onFailure = { error = it.message ?: "Failed"; loading = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        title = { Text("DISCOVER SERVERS", style = PttTypography.StatusLabel, color = TextWhite) },
        text = {
            Column {
                if (loading) { CircularProgressIndicator(Modifier.size(24.dp), color = BrandGreen); return@Column }
                if (error.isNotEmpty()) { Text(error, color = StatusRed, style = PttTypography.Caption); return@Column }
                if (servers.isEmpty()) { Text("No servers found", color = TextDim, style = PttTypography.Caption); return@Column }

                servers.forEach { srv ->
                    val alreadyAdded = existingIds.value.contains("${srv.host}:${srv.port}")
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(srv.name, style = PttTypography.ListItemBold, color = TextPrimary)
                            Text("${srv.host}:${srv.port}  ON:${srv.online}", style = PttTypography.Caption, color = TextSecondary)
                        }
                        TextButton(
                            onClick = {
                                onAdd(ServerConfig(srv.host, srv.port.toIntOrNull() ?: 60050, label = srv.name, autoConnect = true))
                                existingIds.value = existingIds.value + "${srv.host}:${srv.port}"
                            },
                            enabled = !alreadyAdded
                        ) { Text(if (alreadyAdded) "ADDED" else "ADD", color = if (alreadyAdded) TextDim else BrandGreen) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE", color = TextSecondary) } }
    )
}

@Composable
private fun EditServerDialog(config: ServerConfig, onDismiss: () -> Unit, onSave: (ServerConfig) -> Unit) {
    var host by remember { mutableStateOf(config.host) }
    var port by remember { mutableStateOf(config.port.toString()) }
    var user by remember { mutableStateOf(config.username) }
    var pass by remember { mutableStateOf(config.password) }
    var label by remember { mutableStateOf(config.label) }
    var autoConnect by remember { mutableStateOf(config.autoConnect) }

    val fc = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = BrandGreen, unfocusedBorderColor = Border,
        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = BrandGreen
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        title = { Text("EDIT SERVER", style = PttTypography.StatusLabel, color = TextWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(host, { host = it }, label = { Text("HOST") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = fc)
                OutlinedTextField(port, { port = it.filter { c -> c.isDigit() } }, label = { Text("PORT") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = fc)
                OutlinedTextField(label, { label = it }, label = { Text("LABEL") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = fc)
                OutlinedTextField(user, { user = it }, label = { Text("USERNAME") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = fc)
                OutlinedTextField(pass, { pass = it }, label = { Text("PASSWORD") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), colors = fc)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(autoConnect, { autoConnect = it }, colors = CheckboxDefaults.colors(checkedColor = BrandGreen))
                    Text("AUTO CONNECT", style = PttTypography.ListItem, color = TextPrimary)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (host.isNotEmpty()) onSave(ServerConfig(host, port.toIntOrNull() ?: 60050, user, pass, autoConnect, label))
            }) { Text("SAVE", color = BrandGreen) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = TextSecondary) } }
    )
}

@Composable
private fun AddServerDialog(onDismiss: () -> Unit, onAdd: (ServerConfig) -> Unit) {
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("60050") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var autoConnect by remember { mutableStateOf(true) }

    val fc = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = BrandGreen, unfocusedBorderColor = Border,
        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = BrandGreen
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        title = { Text("ADD SERVER", style = PttTypography.StatusLabel, color = TextWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(host, { host = it }, label = { Text("HOST") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = fc)
                OutlinedTextField(port, { port = it.filter { c -> c.isDigit() } }, label = { Text("PORT") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = fc)
                OutlinedTextField(label, { label = it }, label = { Text("LABEL (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = fc)
                OutlinedTextField(user, { user = it }, label = { Text("USERNAME") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = fc)
                OutlinedTextField(pass, { pass = it }, label = { Text("PASSWORD") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), colors = fc)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(autoConnect, { autoConnect = it }, colors = CheckboxDefaults.colors(checkedColor = BrandGreen))
                    Text("AUTO CONNECT", style = PttTypography.ListItem, color = TextPrimary)
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (host.isNotEmpty() && user.isNotEmpty()) onAdd(ServerConfig(host, port.toIntOrNull() ?: 60050, user, pass, autoConnect, label)) }) { Text("ADD", color = BrandGreen) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = TextSecondary) } }
    )
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = PttTypography.StatLabel, color = BrandGreen)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp), colors = CardDefaults.cardColors(containerColor = BgCard)) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
        }
    }
}
