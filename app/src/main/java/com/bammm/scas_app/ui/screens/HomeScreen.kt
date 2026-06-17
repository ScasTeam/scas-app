package com.bammm.scas_app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bammm.scas_app.data.model.Course
import com.bammm.scas_app.ui.theme.components.TopBar
// Strictly themed using MaterialTheme properties
import com.bammm.scas_app.viewmodel.CourseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: CourseViewModel,
    userName: String?,
    userRole: String,
    onCourseClick: (courseId: String, courseName: String) -> Unit,
    onJoinCourseClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddCourseDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopBar(
                title = "SCAS",
                showBackButton = false,
                showProfileIcon = true
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Welcome header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Text(
                    text = "HELLO, ${userName?.uppercase() ?: "USER"}.",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (userRole == "lecturer") "YOUR MANAGED COURSES" else "YOUR ENROLLED COURSES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = uiState.error ?: "Unknown error",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadCourses() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = CircleShape
                            ) {
                                Text("RETRY", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                uiState.courses.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                                .padding(horizontal = 24.dp, vertical = 40.dp)
                        ) {
                            // Sleek visual icon box
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Text(
                                text = "NO COURSES YET",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (userRole == "lecturer") "CREATE A COURSE TO GET STARTED" else "JOIN A COURSE USING A REGISTRATION CODE FROM YOUR LECTURER",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                            if (userRole == "student") {
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = onJoinCourseClick,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .height(48.dp)
                                        .padding(horizontal = 16.dp)
                                ) {
                                    Text(
                                        text = "JOIN COURSE",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    var isVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(uiState.courses) {
                        if (uiState.courses.isNotEmpty()) {
                            isVisible = true
                        }
                    }

                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
                            initialOffsetY = { it / 8 },
                            animationSpec = tween(400)
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        PullToRefreshBox(
                            isRefreshing = uiState.isRefreshing,
                            onRefresh = { viewModel.refresh() },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.courses, key = { it.id }) { course ->
                                    CourseCard(
                                        course = course,
                                        onClick = { onCourseClick(course.id, course.courseName) }
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(80.dp)) }
                            }
                        }
                    }
                }
            }
        }

        // Student persistent FloatingActionButton
        if (userRole == "student" && uiState.courses.isNotEmpty() && !uiState.isLoading && uiState.error == null) {
            FloatingActionButton(
                onClick = onJoinCourseClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Join Course"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "JOIN COURSE",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // Lecturer persistent FloatingActionButton
        if (userRole == "lecturer" && !uiState.isLoading && uiState.error == null) {
            FloatingActionButton(
                onClick = { showAddCourseDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Course"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ADD COURSE",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showAddCourseDialog) {
            AddCourseDialog(
                onDismiss = { showAddCourseDialog = false },
                onSubmit = { code, name, desc, domain ->
                    viewModel.createCourse(code, name, desc, domain) {
                        showAddCourseDialog = false
                    }
                },
                isSaving = uiState.isCreatingCourse,
                errorMessage = uiState.createCourseError
            )
        }
    }
}
}

@Composable
private fun CourseCard(
    course: Course,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row (Code Badge + Sessions count)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Code badge
                Text(
                    text = course.code,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )

                // Sessions Count
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${course.sessionsCount ?: 0} SESSIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Course Title (Uppercase, Days font)
            Text(
                text = course.courseName.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Lecturer Name
            if (course.lecturer != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = course.lecturer.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            // Description
            if (!course.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = course.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun JoinCourseButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "+ JOIN COURSE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AddCourseDialog(
    onDismiss: () -> Unit,
    onSubmit: (code: String, name: String, desc: String?, domain: String?) -> Unit,
    isSaving: Boolean,
    errorMessage: String?
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = Color(0xFF1E1E1E),
        titleContentColor = MaterialTheme.colorScheme.primary,
        title = { Text("CREATE COURSE", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Course Code (e.g. CS101)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Course Name") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text("Allowed Email Domain (Optional)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isNotBlank() && name.isNotBlank()) {
                        onSubmit(
                            code,
                            name,
                            description.takeIf { it.isNotBlank() },
                            domain.takeIf { it.isNotBlank() }
                        )
                    }
                },
                enabled = !isSaving && code.isNotBlank() && name.isNotBlank(),
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

