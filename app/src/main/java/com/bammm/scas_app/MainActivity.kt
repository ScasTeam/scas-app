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

                NavHost(navController = navController, startDestination = startDest) {
                    composable("login") {
                        val authViewModel: AuthViewModel = hiltViewModel()
                        LoginScreen(
                            viewModel = authViewModel,
                            onLoginSuccess = {
                                val role = runBlocking { userPreferences.userRole.first() }
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
                        val authViewModel: AuthViewModel = hiltViewModel()
                        val courseViewModel: CourseViewModel = hiltViewModel()
                        Layout(
                            authViewModel = authViewModel,
                            courseViewModel = courseViewModel,
                            userPreferences = userPreferences,
                            onCourseClick = { courseId, courseName ->
                                navController.navigate("course/$courseId/$courseName")
                            },
                            onJoinCourseClick = {
                                navController.navigate("join-course")
                            },
                            onGenerateQrClick = {
                                navController.navigate("generate-qr")
                            },
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
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding)) {
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
                            onSessionClick = { _ ->
                                navController.navigate("generate-qr")
                            }
                        )
                    }
                    composable("generate-qr") {
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
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                GenerateQrScreen(viewModel = generateQrViewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Layout(
    authViewModel: AuthViewModel,
    courseViewModel: CourseViewModel,
    userPreferences: UserPreferences,
    onCourseClick: (courseId: String, courseName: String) -> Unit,
    onJoinCourseClick: () -> Unit,
    onGenerateQrClick: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val userName by userPreferences.userName.collectAsStateWithLifecycle(initialValue = "Student")
    val userEmail by userPreferences.userEmail.collectAsStateWithLifecycle(initialValue = "")
    val userRole by userPreferences.userRole.collectAsStateWithLifecycle(initialValue = "student")

    Scaffold(
        topBar = {
            TopBar(
                title = if (selectedTab == 0) "SCAS" else "My Profile",
                showBackButton = false,
                showProfileIcon = selectedTab == 0
            )
        },
        bottomBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp,
                    windowInsets = NavigationBarDefaults.windowInsets
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("HOME", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            indicatorColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                        )
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = onGenerateQrClick,
                        icon = { Icon(Icons.Default.Done, contentDescription = "Generate QR") },
                        label = { Text("ATTEND", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Menu, contentDescription = "Menu") },
                        label = { Text("MENU", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            indicatorColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> {
                    HomeScreen(
                        viewModel = courseViewModel,
                        userName = userName,
                        userRole = userRole ?: "student",
                        onCourseClick = onCourseClick,
                        onJoinCourseClick = onJoinCourseClick
                    )
                }
                2 -> {
                    // Profile Menu
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // User avatar/profile icon with industrial border
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                                    shape = CircleShape
                                )
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = userName?.uppercase() ?: "STUDENT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = userEmail?.uppercase() ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = (userRole ?: "student").uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                    RoundedCornerShape(24.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                        Spacer(modifier = Modifier.height(48.dp))
                        
                        Button(
                            onClick = {
                                authViewModel.logout()
                                onLogout()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text(
                                text = "LOG OUT",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}