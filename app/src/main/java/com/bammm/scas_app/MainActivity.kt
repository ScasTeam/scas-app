package com.bammm.scas_app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import com.bammm.scas_app.ui.theme.ScasappTheme
import com.bammm.scas_app.ui.theme.ScasPrimary
import com.bammm.scas_app.ui.theme.ScasPrimaryLight
import com.bammm.scas_app.ui.theme.ScasSecondary
import com.bammm.scas_app.ui.theme.components.TopBar
import com.bammm.scas_app.viewmodel.AuthViewModel
import com.bammm.scas_app.viewmodel.AuthViewModelFactory
import com.bammm.scas_app.viewmodel.CourseViewModel
import com.bammm.scas_app.viewmodel.CourseViewModelFactory
import com.bammm.scas_app.viewmodel.SessionViewModel
import com.bammm.scas_app.viewmodel.SessionViewModelFactory
import com.bammm.scas_app.viewmodel.GenerateQrViewModel
import com.bammm.scas_app.viewmodel.GenerateQrViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

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
        val startDest = if (hasToken) "home" else "login"

        setContent {
            ScasappTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModelFactory(applicationContext)
                )
                val courseViewModel: CourseViewModel = viewModel(
                    factory = CourseViewModelFactory(applicationContext)
                )
                val generateQrViewModel: GenerateQrViewModel = viewModel(
                    factory = GenerateQrViewModelFactory(applicationContext)
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
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("join-course") {
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
                    composable("course/{courseId}/{courseName}") { backStackEntry ->
                        val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
                        val courseName = backStackEntry.arguments?.getString("courseName") ?: ""
                        
                        val sessionViewModel: SessionViewModel = viewModel(
                            key = courseId,
                            factory = SessionViewModelFactory(applicationContext, courseId, courseName)
                        )
                        
                        SessionListScreen(
                            viewModel = sessionViewModel,
                            onBackClick = { navController.popBackStack() },
                            onSessionClick = { _ ->
                                navController.navigate("generate-qr")
                            }
                        )
                    }
                    composable("generate-qr") {
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
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onGenerateQrClick,
                    icon = { Icon(Icons.Default.Done, contentDescription = "Generate QR") },
                    label = { Text("Generate QR") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Menu, contentDescription = "Menu") },
                    label = { Text("Menu") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                )
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
                        // User avatar/profile icon with gradient
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(ScasPrimary, ScasPrimaryLight)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = userName ?: "Student",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = userEmail ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = (userRole ?: "student").uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = ScasSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(
                                    ScasSecondary.copy(alpha = 0.15f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(48.dp))
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    authViewModel.logout()
                                    onLogout()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .padding(horizontal = 16.dp, vertical = 0.dp)
                            ) {
                                Text("Log Out", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}