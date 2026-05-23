package com.nrlptt.app.ui.screen

import android.widget.Toast
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
import com.nrlptt.app.data.SettingsRepository
import com.nrlptt.app.data.UserSettings
import com.nrlptt.app.network.ApiClient
import com.nrlptt.app.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onSkip: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { SettingsRepository(context) }

    var server by remember { mutableStateOf("js.nrlptt.com") }
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

            @Composable
            fun fieldColors() = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandGreen, unfocusedBorderColor = Border,
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                cursorColor = BrandGreen, focusedLabelColor = BrandGreen
            )

            OutlinedTextField(server, { server = it }, label = { Text("SERVER") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, colors = fieldColors())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(user, { user = it }, label = { Text("USERNAME") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, colors = fieldColors())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(pass, { pass = it }, label = { Text("PASSWORD") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = fieldColors())

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
                        ApiClient.login(server, user, pass).fold(
                            onSuccess = { info ->
                                repo.save(UserSettings(
                                    serverAddress = server, username = user, password = pass,
                                    callsign = info.callsign, dmrId = info.dmrId, autoConnect = true
                                ))
                                onLoginSuccess()
                            },
                            onFailure = { e -> error = e.message ?: "FAILED" }
                        )
                        loading = false
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
        }
    }
}
