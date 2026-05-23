package com.bammm.scas_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bammm.scas_app.data.preferences.UserPreferences
import com.bammm.scas_app.ui.screens.HomeScreen
import com.bammm.scas_app.ui.screens.LoginScreen
import com.bammm.scas_app.ui.theme.ScasappTheme
import com.bammm.scas_app.ui.theme.components.TopBar
import com.bammm.scas_app.viewmodel.AuthViewModel
import com.bammm.scas_app.viewmodel.AuthViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val userPreferences = UserPreferences(applicationContext)
        val hasToken = runBlocking { userPreferences.authToken.first() != null }
        val startDest = if (hasToken) "home" else "login"

        setContent {
            ScasappTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModelFactory(applicationContext)
                )

                NavHost(navController = navController, startDestination = startDest) {
                    composable("login") {
                        LoginScreen(
                            viewModel = authViewModel,
                            onLoginSuccess = {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("home") {
                        Layout(
                            authViewModel = authViewModel,
                            onLogout = {
                                navController.navigate("login") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Layout(authViewModel: AuthViewModel, onLogout: () -> Unit) {
    Scaffold(
        topBar = { TopBar() },
        bottomBar = {
            BottomAppBar(containerColor = Color.Gray) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(tint = Color.White, imageVector = Icons.Filled.Home, contentDescription = "Home")
                        Text(text = "Home", color = Color.White, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(tint = Color.White, imageVector = Icons.Filled.Done, contentDescription = "Generate QR")
                        Text(text = "Generate QR", color = Color.White, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(tint = Color.White, imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                        Text(text = "Menu", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            HomeScreen(viewModel = authViewModel, onLogout = onLogout)
        }
    }
}