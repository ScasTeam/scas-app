package com.bammm.scas_app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bammm.scas_app.viewmodel.AuthState
import com.bammm.scas_app.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val credentialManager = remember { CredentialManager.create(context) }
    
    var otpText by remember { mutableStateOf("") }
    
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            viewModel.resetState()
            onLoginSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "Smart Campus Attendance System", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(32.dp))
            
            when (authState) {
                is AuthState.Loading -> {
                    CircularProgressIndicator()
                }
                is AuthState.Error -> {
                    Text(
                        text = (authState as AuthState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(onClick = { viewModel.signInWithGoogle(credentialManager) }) {
                        Text("Retry Sign in with Google")
                    }
                }
                else -> {
                    Button(onClick = { viewModel.signInWithGoogle(credentialManager) }) {
                        Text("Sign in with Google")
                    }
                }
            }
        }
    }

    if (authState is AuthState.RequireOtp) {
        val email = (authState as AuthState.RequireOtp).email
        AlertDialog(
            onDismissRequest = { viewModel.resetState() },
            title = { Text("Device Verification") },
            text = {
                Column {
                    Text("An OTP has been sent to $email. Please enter it below:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = otpText,
                        onValueChange = { otpText = it },
                        label = { Text("6-digit OTP") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.verifyOtp(email, otpText) }) {
                    Text("Verify")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.resetState() }) {
                    Text("Cancel")
                }
            }
        )
    }
}
