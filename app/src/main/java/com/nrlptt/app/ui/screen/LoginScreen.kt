package com.nrlptt.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nrlptt.app.data.ServerConfig
import com.nrlptt.app.data.SettingsRepository
import com.nrlptt.app.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onSkip: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { SettingsRepository(context) }
    val servers by repo.servers.collectAsState()

    var server by remember { mutableStateOf("m.nrlptt.com") }
    var port by remember { mutableStateOf("60050") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(BgBlack)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            Text("NRL PTT", style = PttTypography.StatusValue, color = BrandGreen)
            Text("PUBLIC NETWORK RADIO", style = PttTypography.StatLabel, color = TextSecondary)
            Spacer(Modifier.height(32.dp))

            val fc = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandGreen, unfocusedBorderColor = Border,
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                cursorColor = BrandGreen, focusedLabelColor = BrandGreen
            )

            OutlinedTextField(server, { server = it }, label = { Text("SERVER") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, colors = fc)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(port, { port = it.filter { c -> c.isDigit() } }, label = { Text("PORT") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = fc)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(user, { user = it }, label = { Text("USERNAME") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, colors = fc)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(pass, { pass = it }, label = { Text("PASSWORD") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), colors = fc)

            if (error.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = StatusRed, fontSize = 12.sp)
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (user.isEmpty() || pass.isEmpty()) { error = "REQUIRED"; return@Button }
                    loading = true; error = ""
                    scope.launch {
                        repo.addServer(ServerConfig(server, port.toIntOrNull() ?: 60050, user, pass, true))
                        loading = false
                        onLoginSuccess()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !loading && user.isNotEmpty() && pass.isNotEmpty(),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen, contentColor = BgBlack)
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = BgBlack, strokeWidth = 2.dp)
                else Text("LOGIN", style = PttTypography.ButtonLabel)
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onSkip) {
                Text("SKIP", color = TextDim, style = PttTypography.Caption)
            }

            // Show existing servers
            if (servers.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("SAVED SERVERS", style = PttTypography.StatLabel, color = TextSecondary)
                servers.forEach { cfg ->
                    Text("${cfg.displayLabel} (${cfg.host})", style = PttTypography.Caption, color = TextDim)
                }
            }
        }
    }
}
