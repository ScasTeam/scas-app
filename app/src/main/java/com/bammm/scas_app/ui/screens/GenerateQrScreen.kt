package com.bammm.scas_app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bammm.scas_app.ui.theme.ScasQrAccent
import com.bammm.scas_app.ui.theme.ScasQrBackground
import com.bammm.scas_app.viewmodel.GenerateQrViewModel

@Composable
fun GenerateQrScreen(
    viewModel: GenerateQrViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ScasQrBackground,
                        ScasQrBackground.copy(alpha = 0.95f),
                        Color(0xFF0D1B2A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            uiState.isLoading && uiState.qrBitmap == null -> {
                // Initial loading state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = ScasQrAccent,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Generating QR codes...",
                        color = Color.White.copy(alpha = 0.7f),
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
                    Text(
                        text = "⚠️",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.retry() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ScasQrAccent
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
                    verticalArrangement = Arrangement.Center
                ) {
                    // Title
                    Text(
                        text = "Your Attendance QR",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Show this code to the scanner",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // QR Code with countdown ring
                    QrCodeWithCountdown(
                        viewModel = viewModel,
                        countdown = uiState.countdown,
                        totalSeconds = 10
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Progress dots
                    ProgressDots(
                        currentIndex = uiState.currentIndex,
                        total = uiState.totalCodes
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Countdown text
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .background(
                                color = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Next code in ",
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${uiState.countdown}s",
                            color = ScasQrAccent,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Info card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "📱 How it works",
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• QR code changes every 10 seconds\n• Show it to the barcode scanner in class\n• Keep this screen open during scanning\n• Do not screenshot — it won't work",
                                color = Color.White.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    // Loading indicator for batch refresh
                    if (uiState.isLoading && uiState.qrBitmap != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Refreshing batch...",
                            color = ScasQrAccent.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QrCodeWithCountdown(
    viewModel: GenerateQrViewModel,
    countdown: Int,
    totalSeconds: Int
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val progress by animateFloatAsState(
        targetValue = countdown.toFloat() / totalSeconds.toFloat(),
        animationSpec = tween(durationMillis = 300),
        label = "countdown_progress"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(280.dp)
    ) {
        // Countdown ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 4.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2

            // Background ring
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = radius,
                style = Stroke(width = strokeWidth)
            )

            // Progress arc
            val sweepAngle = 360f * progress
            drawArc(
                color = ScasQrAccent,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // QR Code bitmap with transition
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
                        .size(240.dp)
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
                isActive -> ScasQrAccent
                isPast -> ScasQrAccent.copy(alpha = 0.4f)
                else -> Color.White.copy(alpha = 0.15f)
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
