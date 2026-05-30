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
import com.nrlptt.app.data.ServerConfig
import com.nrlptt.app.data.SettingsRepository
import com.nrlptt.app.theme.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onSkip: () -> Unit) {
    val d = rememberScreenDimens()
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

    val gap = (d.componentGap.value * 2.5f).dp

    Box(modifier = Modifier.fillMaxSize().background(BgBlack)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(d.cardPadding * 2),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(d.cardPadding * 4))
            Text("NRL PTT", fontSize = d.statusValueSize, fontWeight = FontWeight.Bold, color = BrandGreen, letterSpacing = 1.sp)
            Text("公网对讲", fontSize = d.captionSize, color = TextSecondary, letterSpacing = 1.sp)
            Spacer(Modifier.height(gap * 2))

            val fc = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandGreen, unfocusedBorderColor = Border,
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                cursorColor = BrandGreen, focusedLabelColor = BrandGreen
            )

            OutlinedTextField(server, { server = it }, label = { Text("服务器") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, colors = fc)
            Spacer(Modifier.height(gap))
            OutlinedTextField(port, { port = it.filter { c -> c.isDigit() } }, label = { Text("端口") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = fc)
            Spacer(Modifier.height(gap))
            OutlinedTextField(user, { user = it }, label = { Text("用户名") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, colors = fc)
            Spacer(Modifier.height(gap))
            OutlinedTextField(pass, { pass = it }, label = { Text("密码") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), colors = fc)

            if (error.isNotEmpty()) {
                Spacer(Modifier.height(gap))
                Text(error, color = StatusRed, fontSize = d.listItemSize)
            }

            Spacer(Modifier.height(gap * 1.5f))

            Button(
                onClick = {
                    if (user.isEmpty() || pass.isEmpty()) { error = "请填写完整"; return@Button }
                    loading = true; error = ""
                    scope.launch {
                        repo.addServer(ServerConfig(server, port.toIntOrNull() ?: 60050, user, pass, true))
                        loading = false; onLoginSuccess()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(d.pttHeight - 8.dp),
                enabled = !loading && user.isNotEmpty() && pass.isNotEmpty(),
                shape = RoundedCornerShape(d.cornerRadius),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen, contentColor = BgBlack)
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(d.iconSize - 2.dp), color = BgBlack, strokeWidth = 2.dp)
                else Text("登录", fontSize = d.buttonLabelSize, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }

            Spacer(Modifier.height(gap))
            TextButton(onClick = onSkip) {
                Text("跳过", fontSize = d.captionSize, color = TextDim)
            }

            if (servers.isNotEmpty()) {
                Spacer(Modifier.height(gap * 1.5f))
                Text("已保存的服务器", fontSize = d.captionSize, color = TextSecondary, letterSpacing = 1.sp)
                servers.forEach { cfg ->
                    Text("${cfg.displayLabel} (${cfg.host})", fontSize = d.captionSize, color = TextDim)
                }
            }
        }
    }
}
