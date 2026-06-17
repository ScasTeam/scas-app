package com.bammm.scas_app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bammm.scas_app.data.preferences.UserPreferences
import com.bammm.scas_app.ui.screens.HomeScreen
import com.bammm.scas_app.ui.screens.LoginScreen
import com.bammm.scas_app.ui.screens.JoinCourseScreen
import com.bammm.scas_app.ui.screens.SessionListScreen
import com.bammm.scas_app.ui.screens.GenerateQrScreen
import com.bammm.scas_app.ui.screens.ChooseRoleScreen
import com.bammm.scas_app.ui.screens.ProfileScreen
import com.bammm.scas_app.ui.theme.components.ScasBottomBar
import com.bammm.scas_app.ui.theme.ScasTheme
import com.bammm.scas_app.ui.theme.components.TopBar
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.bammm.scas_app.viewmodel.AuthViewModel
import com.bammm.scas_app.viewmodel.CourseViewModel
import com.bammm.scas_app.viewmodel.SessionViewModel
import com.bammm.scas_app.viewmodel.GenerateQrViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Anti-fraud: prevent screenshots/screen-recording of TOTP QR codes
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        enableEdgeToEdge()
        
        val userPreferences = UserPreferences(applicationContext)
        val hasToken = runBlocking { userPreferences.authToken.first() != null }
        val userRole = runBlocking { userPreferences.userRole.first() }
        val startDest = if (hasToken) {
            if (userRole.isNullOrEmpty()) "choose-role" else "home"
        } else {
            "login"
        }

        setContent {
            ScasTheme {
                val navController = rememberNavController()
                val roleState by userPreferences.userRole.collectAsStateWithLifecycle(initialValue = userRole)

                Scaffold(
                    bottomBar = {
                        ScasBottomBar(
                            navController = navController,
                            userRole = roleState
                        )
                    },
                    contentWindowInsets = WindowInsets(0.dp)
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    ) {
                        NavHost(navController = navController, startDestination = startDest) {
                            composable("login") {
                                val authViewModel: AuthViewModel = hiltViewModel()
                                LoginScreen(
                                    viewModel = authViewModel,
                                    onLoginSuccess = { role ->
                                        if (role.isNullOrEmpty()) {
                                            navController.navigate("choose-role") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        } else {
                                            navController.navigate("home") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        }
                                    }
                                )
                            }
                            composable("choose-role") {
                                val authViewModel: AuthViewModel = hiltViewModel()
                                ChooseRoleScreen(
                                    viewModel = authViewModel,
                                    userPreferences = userPreferences,
                                    onRoleAssigned = {
                                        navController.navigate("home") {
                                            popUpTo("choose-role") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("home") {
                                val courseViewModel: CourseViewModel = hiltViewModel()
                                val userName by userPreferences.userName.collectAsStateWithLifecycle(initialValue = "Student")
                                val currentRole by userPreferences.userRole.collectAsStateWithLifecycle(initialValue = "student")
                                HomeScreen(
                                    viewModel = courseViewModel,
                                    userName = userName,
                                    userRole = currentRole ?: "student",
                                    onCourseClick = { courseId, courseName ->
                                        navController.navigate("course/$courseId/$courseName")
                                    },
                                    onJoinCourseClick = {
                                        navController.navigate("join-course")
                                    }
                                )
                            }
                            composable("profile") {
                                val authViewModel: AuthViewModel = hiltViewModel()
                                val userName by userPreferences.userName.collectAsStateWithLifecycle(initialValue = "Student")
                                val userEmail by userPreferences.userEmail.collectAsStateWithLifecycle(initialValue = "")
                                val currentRole by userPreferences.userRole.collectAsStateWithLifecycle(initialValue = "student")
                                ProfileScreen(
                                    authViewModel = authViewModel,
                                    userName = userName,
                                    userEmail = userEmail,
                                    userRole = currentRole,
                                    onLogout = {
                                        navController.navigate("login") {
                                            popUpTo(navController.graph.id) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("join-course") {
                                val courseViewModel: CourseViewModel = hiltViewModel()
                                Scaffold(
                                    topBar = {
                                        TopBar(
                                            title = "Join Course",
                                            showBackButton = true,
                                            onBackClick = { navController.popBackStack() },
                                            showProfileIcon = false
                                        )
                                    }
                                ) { paddingValues ->
                                    Box(modifier = Modifier.padding(paddingValues)) {
                                        JoinCourseScreen(
                                            viewModel = courseViewModel,
                                            onJoinSuccess = {
                                                navController.popBackStack()
                                            }
                                        )
                                    }
                                }
                            }
                            composable("course/{courseId}/{courseName}") {
                                val sessionViewModel: SessionViewModel = hiltViewModel()
                                
                                SessionListScreen(
                                    viewModel = sessionViewModel,
                                    userPreferences = userPreferences,
                                    onBackClick = { navController.popBackStack() },
                                    onSessionClick = { sessionId ->
                                        navController.navigate("generate-qr/$sessionId")
                                    }
                                )
                            }
                            composable("generate-qr/{sessionId}") { backStackEntry ->
                                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                                val generateQrViewModel: GenerateQrViewModel = hiltViewModel()
                                Scaffold(
                                    topBar = {
                                        TopBar(
                                            title = "Attendance QR",
                                            showBackButton = true,
                                            onBackClick = { navController.popBackStack() },
                                            showProfileIcon = false
                                        )
                                    }
                                ) { paddingValues ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(paddingValues)
                                    ) {
                                        GenerateQrScreen(
                                            sessionId = sessionId,
                                            viewModel = generateQrViewModel
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
