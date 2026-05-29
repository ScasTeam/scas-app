package com.bammm.scas_app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bammm.scas_app.data.model.CourseSession
import com.bammm.scas_app.data.model.EnrolledStudent
import com.bammm.scas_app.data.model.MySessionAttendance
import com.bammm.scas_app.data.model.AttendeeLog
import com.bammm.scas_app.data.preferences.UserPreferences
import com.bammm.scas_app.ui.theme.Success
import com.bammm.scas_app.ui.theme.Warning
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.bammm.scas_app.ui.theme.components.TopBar
import com.bammm.scas_app.viewmodel.SessionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    viewModel: SessionViewModel,
    userPreferences: UserPreferences,
    onBackClick: () -> Unit,
    onSessionClick: (sessionId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userRole by userPreferences.userRole.collectAsStateWithLifecycle(initialValue = "student")
    
    val isLecturer = userRole == "lecturer"
    var selectedTab by remember { mutableIntStateOf(0) }
    
    // Automatically fetch relevant data depending on active tab
    LaunchedEffect(selectedTab, userRole) {
        if (selectedTab == 1) {
            if (isLecturer) {
                viewModel.loadCourseStudents()
            } else {
                viewModel.loadMyAttendance()
            }
        } else if (selectedTab == 2 && isLecturer) {
            viewModel.loadSessions() // Keep sessions fresh for the Lecturer Attendance view
        }
    }

    var showAddSessionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopBar(
                title = uiState.courseName.ifEmpty { "Course Details" }.uppercase(),
                showBackButton = true,
                onBackClick = onBackClick,
                showProfileIcon = false
            )
        },
        floatingActionButton = {
            if (isLecturer && selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddSessionDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Session")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ADD SESSION", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Course Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                divider = {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("SESSIONS", fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(if (isLecturer) "STUDENTS" else "ATTENDANCE", fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                )
                if (isLecturer) {
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("ATTENDANCE", fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> SessionsTabContent(
                        uiState = uiState,
                        isLecturer = isLecturer,
                        onSessionClick = onSessionClick,
                        viewModel = viewModel
                    )
                    1 -> {
                        if (isLecturer) {
                            StudentsTabContent(uiState = uiState, viewModel = viewModel)
                        } else {
                            StudentAttendanceTabContent(uiState = uiState, viewModel = viewModel)
                        }
                    }
                    2 -> {
                        if (isLecturer) {
                            LecturerAttendanceTabContent(uiState = uiState, viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    if (showAddSessionDialog) {
        AddSessionDialog(
            onDismiss = { showAddSessionDialog = false },
            onSubmit = { title, desc, mode, opened, closed ->
                viewModel.createSession(title, desc, mode, opened, closed) {
                    showAddSessionDialog = false
                }
            },
            isSaving = uiState.isCreatingSession,
            errorMessage = uiState.createSessionError
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionsTabContent(
    uiState: com.bammm.scas_app.viewmodel.SessionUiState,
    isLecturer: Boolean,
    onSessionClick: (sessionId: String) -> Unit,
    viewModel: SessionViewModel
) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
        }
    } else if (uiState.error != null) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(uiState.error.uppercase(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.loadSessions() }, shape = CircleShape) {
                    Text("RETRY", fontWeight = FontWeight.Bold)
                }
            }
        }
    } else if (uiState.sessions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NO SESSIONS CREATED YET",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isLecturer) "Tap the '+ ADD SESSION' button to create your first session." else "Sessions will appear here when created by your lecturer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val openSessions = uiState.sessions.filter { it.status == "open" }
                val scheduledSessions = uiState.sessions.filter { it.status == "scheduled" }
                val closedSessions = uiState.sessions.filter { it.status == "closed" }

                if (openSessions.isNotEmpty()) {
                    item { SectionHeader(title = "ACTIVE NOW") }
                    items(openSessions, key = { it.id }) { session ->
                        SessionCard(
                            session = session,
                            onClick = { if (!isLecturer) onSessionClick(session.id) }
                        )
                    }
                }

                if (scheduledSessions.isNotEmpty()) {
                    item { SectionHeader(title = "SCHEDULED") }
                    items(scheduledSessions, key = { it.id }) { session ->
                        SessionCard(session = session, onClick = null)
                    }
                }

                if (closedSessions.isNotEmpty()) {
                    item { SectionHeader(title = "COMPLETED") }
                    items(closedSessions, key = { it.id }) { session ->
                        SessionCard(session = session, onClick = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
    )
}

@Composable
private fun SessionCard(
    session: CourseSession,
    onClick: (() -> Unit)?
) {
    val isOpen = session.status == "open"
    val isClosed = session.status == "closed"

    val cardColor = when {
        isOpen -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
    }

    val borderColor = when {
        isOpen -> Success.copy(alpha = 0.6f)
        isClosed -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
        else -> Warning.copy(alpha = 0.4f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isOpen -> Success.copy(alpha = 0.1f)
                            isClosed -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
                            else -> Warning.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isOpen -> Icons.Default.PlayArrow
                        isClosed -> Icons.Default.CheckCircle
                        else -> Icons.Default.Info
                    },
                    contentDescription = session.status,
                    tint = when {
                        isOpen -> Success
                        isClosed -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                        else -> Warning
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isClosed) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!session.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = session.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.mode.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                                RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                    if (!session.openedAt.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatDateTime(session.openedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            if (isOpen && onClick != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TAP TO\nATTEND",
                    style = MaterialTheme.typography.labelSmall,
                    color = Success,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun StudentsTabContent(
    uiState: com.bammm.scas_app.viewmodel.SessionUiState,
    viewModel: SessionViewModel
) {
    var studentToKick by remember { mutableStateOf<EnrolledStudent?>(null) }

    if (uiState.isStudentsLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
        }
    } else if (uiState.studentsError != null) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(uiState.studentsError.uppercase(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.loadCourseStudents() }, shape = CircleShape) {
                    Text("RETRY", fontWeight = FontWeight.Bold)
                }
            }
        }
    } else if (uiState.students.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                "NO ENROLLED STUDENTS YET",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "${uiState.students.size} STUDENTS ENROLLED",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(uiState.students, key = { it.id }) { student ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(student.name.uppercase(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(student.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        }
                        IconButton(
                            onClick = { studentToKick = student },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Kick Student")
                        }
                    }
                }
            }
        }
    }

    if (studentToKick != null) {
        AlertDialog(
            onDismissRequest = { studentToKick = null },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = MaterialTheme.colorScheme.error,
            title = { Text("REMOVE STUDENT", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove ${studentToKick?.name} from this course? All attendance logs will be permanently deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        studentToKick?.let {
                            viewModel.kickStudent(it.id) {
                                studentToKick = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("REMOVE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToKick = null }) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        )
    }
}

@Composable
private fun StudentAttendanceTabContent(
    uiState: com.bammm.scas_app.viewmodel.SessionUiState,
    viewModel: SessionViewModel
) {
    if (uiState.isAttendanceLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
        }
    } else if (uiState.attendanceError != null) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(uiState.attendanceError.uppercase(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.loadMyAttendance() }, shape = CircleShape) {
                    Text("RETRY", fontWeight = FontWeight.Bold)
                }
            }
        }
    } else if (uiState.myAttendance.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("NO ATTENDANCE RECORDS FOUND", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Stats Panel
            uiState.myStats?.let { stats ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stats.attended.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("ATTENDED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                            }
                            Box(modifier = Modifier.height(40.dp).width(1.dp).background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stats.missed.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("MISSED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                            }
                            Box(modifier = Modifier.height(40.dp).width(1.dp).background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${stats.rate}%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("RATE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }

            items(uiState.myAttendance, key = { it.sessionId }) { record ->
                val statusStyles = when (record.attendanceStatus) {
                    "present" -> Success to Success.copy(alpha = 0.08f)
                    "late" -> Warning to Warning.copy(alpha = 0.08f)
                    "absent" -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                    else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f) to Color.Transparent
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(record.sessionTitle.uppercase(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = record.sessionStatus.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when(record.sessionStatus) {
                                        "open" -> Success
                                        "closed" -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                        else -> Warning
                                    },
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(
                                            (when(record.sessionStatus) {
                                                "open" -> Success
                                                "closed" -> MaterialTheme.colorScheme.onBackground
                                                else -> Warning
                                            }).copy(alpha = 0.05f),
                                            RoundedCornerShape(24.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 2.dp)
                                )
                                if (!record.openedAt.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(formatDateTime(record.openedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        if (record.attended) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = record.attendanceStatus?.uppercase() ?: "PRESENT",
                                    color = statusStyles.first,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier
                                        .background(statusStyles.second, RoundedCornerShape(24.dp))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                                if (!record.scannedAt.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formatTimeOnly(record.scannedAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = if (record.sessionStatus == "scheduled") "UPCOMING" else "ABSENT",
                                color = if (record.sessionStatus == "scheduled") Warning else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier
                                    .background(
                                        (if (record.sessionStatus == "scheduled") Warning else MaterialTheme.colorScheme.error).copy(alpha = 0.08f),
                                        RoundedCornerShape(24.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LecturerAttendanceTabContent(
    uiState: com.bammm.scas_app.viewmodel.SessionUiState,
    viewModel: SessionViewModel
) {
    if (uiState.sessions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("NO ATTENDANCE SESSIONS AVAILABLE", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    } else {
        val totalAttendance = uiState.sessions.sumOf { it.attendanceLogsCount ?: 0 }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(uiState.sessions.size.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("SESSIONS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                        }
                        Box(modifier = Modifier.height(40.dp).width(1.dp).background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(totalAttendance.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("ATTENDANCE RECORDS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                        }
                    }
                }
            }

            items(uiState.sessions, key = { it.id }) { session ->
                var isExpanded by remember { mutableStateOf(false) }
                val attendees = uiState.sessionAttendees[session.id] ?: emptyList()
                val isAttendeesLoading = uiState.attendeesLoading[session.id] ?: false

                LaunchedEffect(isExpanded) {
                    if (isExpanded && attendees.isEmpty()) {
                        viewModel.loadSessionAttendees(session.id)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header row (Toggle click)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = session.status.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when(session.status) {
                                            "open" -> Success
                                            "closed" -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                            else -> Warning
                                        },
                                        modifier = Modifier
                                            .border(
                                                width = 1.dp,
                                                color = when(session.status) {
                                                    "open" -> Success
                                                    "closed" -> MaterialTheme.colorScheme.onBackground
                                                    else -> Warning
                                                }.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(24.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${session.attendanceLogsCount ?: 0} ATTENDED",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(session.title.uppercase(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                        }

                        // Accordion expansion animation
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03f)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                if (isAttendeesLoading) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    }
                                } else if (attendees.isEmpty()) {
                                    Text(
                                        text = "NO RECORDS FOUND",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                    )
                                } else {
                                    attendees.forEach { log ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = log.student?.name?.uppercase() ?: "STUDENT",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = log.student?.email ?: "",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = log.status.uppercase(),
                                                    color = if (log.status == "present") Success else Warning,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier
                                                        .background(
                                                            (if (log.status == "present") Success else Warning).copy(alpha = 0.05f),
                                                            RoundedCornerShape(24.dp)
                                                        )
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                                log.scannedAt?.let {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = formatTimeOnly(it),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                                    )
                                                }
                                            }
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f))
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

@Composable
private fun AddSessionDialog(
    onDismiss: () -> Unit,
    onSubmit: (title: String, desc: String, mode: String, opened: String, closed: String) -> Unit,
    isSaving: Boolean,
    errorMessage: String?
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var isOnline by remember { mutableStateOf(false) } // offline default

    // Set opened_at to now, closed_at to 2 hours from now
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    val now = Calendar.getInstance()
    val prefillOpened = dateFormat.format(now.time)
    now.add(Calendar.HOUR, 2)
    val prefillClosed = dateFormat.format(now.time)

    var openedAt by remember { mutableStateOf(prefillOpened) }
    var closedAt by remember { mutableStateOf(prefillClosed) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = {
            Text("CREATE NEW SESSION", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!errorMessage.isNullOrEmpty()) {
                    Text(
                        errorMessage.uppercase(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("SESSION TITLE") },
                    placeholder = { Text("e.g. Session 1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("DESCRIPTION") },
                    placeholder = { Text("e.g. Introduction to SCAS") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ONLINE SESSION", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isOnline,
                        onCheckedChange = { isOnline = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = openedAt,
                    onValueChange = { openedAt = it },
                    label = { Text("OPEN AT (YYYY-MM-DD HH:MM:SS)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = closedAt,
                    onValueChange = { closedAt = it },
                    label = { Text("CLOSE AT (YYYY-MM-DD HH:MM:SS)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSubmit(title, desc, if (isOnline) "online" else "offline", openedAt, closedAt)
                    }
                },
                enabled = !isSaving && title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text("CREATE", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("CANCEL", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
        }
    )
}

private fun formatDateTime(dateTimeStr: String): String {
    return try {
        // Safe ISO8601 or standard DB timestamp formatting
        val cleanStr = dateTimeStr.replace("T", " ").substringBefore(".")
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(cleanStr) ?: return dateTimeStr
        
        val outputFormat = SimpleDateFormat("MMM d, h:mm a", Locale.US)
        outputFormat.timeZone = TimeZone.getDefault()
        outputFormat.format(date).uppercase()
    } catch (e: Exception) {
        dateTimeStr.uppercase()
    }
}

private fun formatTimeOnly(dateTimeStr: String): String {
    return try {
        val cleanStr = dateTimeStr.replace("T", " ").substringBefore(".")
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(cleanStr) ?: return dateTimeStr
        
        val outputFormat = SimpleDateFormat("h:mm a", Locale.US)
        outputFormat.timeZone = TimeZone.getDefault()
        outputFormat.format(date).uppercase()
    } catch (e: Exception) {
        dateTimeStr.uppercase()
    }
}
