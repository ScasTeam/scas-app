package com.bammm.scas_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bammm.scas_app.ui.theme.MonospacedNumbers
import com.bammm.scas_app.ui.theme.MonospacedSub
import com.bammm.scas_app.viewmodel.AuthState
import com.bammm.scas_app.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: (role: String?) -> Unit
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val credentialManager = remember { CredentialManager.create(context) }
    
    var otpText by remember { mutableStateOf("") }
    
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            val role = (authState as AuthState.Success).role
            viewModel.resetState()
            onLoginSuccess(role)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Subtle background glow matching the web app
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(360.dp)
                .background(
                    color = Color.White.copy(alpha = 0.02f),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Brand Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "SCAS\nPORTAL.",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 38.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "IDENTIFY YOURSELF TO CONTINUE",
                    style = MonospacedSub,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }

            // Authentication Action Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .padding(8.dp)
            ) {
                when (authState) {
                    is AuthState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    is AuthState.Error -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = (authState as AuthState.Error).message.uppercase(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Button(
                                onClick = { viewModel.signInWithGoogle(credentialManager) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = CircleShape,
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Text("RETRY SIGN IN WITH GOOGLE", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    else -> {
                        Button(
                            onClick = { viewModel.signInWithGoogle(credentialManager) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "LOGIN WITH GOOGLE SSO",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // Footer
            Text(
                text = "SECURE PROTOCOL BINDING",
                style = MonospacedSub,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }

    if (authState is AuthState.RequireOtp) {
        val email = (authState as AuthState.RequireOtp).email
        AlertDialog(
            onDismissRequest = { viewModel.resetState() },
            containerColor = Color(0xFF202020),
            titleContentColor = MaterialTheme.colorScheme.primary,
            textContentColor = MaterialTheme.colorScheme.onBackground,
            title = {
                Text(
                    text = "DEVICE VERIFICATION",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "An OTP has been sent to $email. Please enter it below to authorize this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    OutlinedTextField(
                        value = otpText,
                        onValueChange = { if (it.length <= 6) otpText = it },
                        label = { Text("6-DIGIT OTP CODE") },
                        placeholder = { Text("e.g. 123456") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f)
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (otpText.length == 6) {
                                    viewModel.verifyOtp(email, otpText)
                                }
                            }
                        ),
                        textStyle = MonospacedNumbers.copy(
                            fontSize = 18.sp,
                            letterSpacing = 4.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.verifyOtp(email, otpText) },
                    enabled = otpText.length == 6,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        disabledContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    ),
                    shape = CircleShape,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("VERIFY", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.resetState() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                ) {
                    Text("CANCEL", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

