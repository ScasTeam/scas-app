package com.bammm.scas_app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bammm.scas_app.ui.theme.MonospacedNumbers
import com.bammm.scas_app.ui.theme.MonospacedSub
import com.bammm.scas_app.viewmodel.GenerateQrViewModel

@Composable
fun GenerateQrScreen(
    sessionId: String,
    viewModel: GenerateQrViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel, sessionId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.startGenerating(sessionId)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.stopGenerating()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopGenerating()
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
                .size(300.dp)
                .background(
                    color = Color.White.copy(alpha = 0.02f),
                    shape = CircleShape
                )
        )

        when {
            uiState.isLoading && uiState.qrBitmap == null -> {
                // Initial loading state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Generating QR codes...",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            uiState.error != null && uiState.qrBitmap == null -> {
                // Error state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.retry(sessionId) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            else -> {
                // QR Display
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(
                            text = "ATTENDANCE PROTOCOL",
                            style = MonospacedSub,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "GENERATE IDENTITY.",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Scan this code to mark presence.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    // QR Code Box
                    QrCodeContainer(viewModel = viewModel)

                    // Timer with tabular numbers and linear progress bar
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = String.format("%02d", uiState.countdown),
                                style = MonospacedNumbers,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .height(24.dp)
                                    .width(1.dp)
                                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
                            )
                            Column(
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = "SECONDS",
                                    style = MonospacedSub,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    lineHeight = 10.sp
                                )
                                Text(
                                    text = "REMAINING",
                                    style = MonospacedSub,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    lineHeight = 10.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Linear Progress Bar
                        val progress by animateFloatAsState(
                            targetValue = uiState.countdown.toFloat() / 10f,
                            animationSpec = tween(durationMillis = 300),
                            label = "linear_progress"
                        )
                        Box(
                            modifier = Modifier
                                .width(180.dp)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }

                    // Progress dots
                    ProgressDots(
                        currentIndex = uiState.currentIndex,
                        total = uiState.totalCodes
                    )

                    // Sleek industrial Info card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "ATTENDANCE PROTOCOL",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• QR code changes every 10 seconds\n• Keep this screen open during scanning\n• Do not screenshot — it won't work",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // Loading indicator for batch refresh
                    if (uiState.isLoading && uiState.qrBitmap != null) {
                        Text(
                            text = "Updating secure buffer...",
                            style = MonospacedSub,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(1.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun QrCodeContainer(
    viewModel: GenerateQrViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(16.dp)
    ) {
        AnimatedContent(
            targetState = uiState.currentIndex,
            transitionSpec = {
                (fadeIn(animationSpec = tween(200)) +
                        scaleIn(initialScale = 0.92f, animationSpec = tween(200)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(150)) +
                                scaleOut(targetScale = 1.08f, animationSpec = tween(150))
                    )
            },
            label = "qr_transition"
        ) { _ ->
            uiState.qrBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Attendance QR Code",
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun ProgressDots(
    currentIndex: Int,
    total: Int
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(total) { index ->
            val isActive = index == currentIndex
            val isPast = index < currentIndex

            val dotColor = when {
                isActive -> MaterialTheme.colorScheme.primary
                isPast -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
            }

            val animatedSize by animateFloatAsState(
                targetValue = if (isActive) 10f else 6f,
                animationSpec = tween(200),
                label = "dot_size_$index"
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(animatedSize.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

