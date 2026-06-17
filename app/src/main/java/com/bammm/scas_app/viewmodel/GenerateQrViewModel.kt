package com.bammm.scas_app.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bammm.scas_app.data.api.ApiClient
import com.bammm.scas_app.data.preferences.UserPreferences
import com.bammm.scas_app.data.model.GenerateQrRequest
import com.bammm.scas_app.util.QrCodeGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class QrUiState(
    val isLoading: Boolean = true,
    val qrBitmap: Bitmap? = null,
    val qrPayloads: List<String> = emptyList(),
    val currentIndex: Int = 0,
    val totalCodes: Int = 5,
    val countdown: Int = 10,
    val error: String? = null
)

@HiltViewModel
class GenerateQrViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val apiService: com.bammm.scas_app.data.api.ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrUiState())
    val uiState: StateFlow<QrUiState> = _uiState

    private var rotationJob: Job? = null

    fun startGenerating(sessionId: String) {
        stopGenerating()
        rotationJob = viewModelScope.launch {
            var payloads: List<String> = emptyList()
            while (isActive) {
                if (payloads.isEmpty()) {
                    _uiState.update { it.copy(isLoading = true, error = null) }
                    try {
                        val response = apiService.generateQr(GenerateQrRequest(sessionId))
                        if (response.isSuccessful) {
                            payloads = response.body()?.qrBatch ?: emptyList()
                            if (payloads.isEmpty()) {
                                _uiState.update {
                                    it.copy(isLoading = false, error = "No QR codes received")
                                }
                                delay(5000)
                                continue
                            }
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    qrPayloads = payloads,
                                    totalCodes = payloads.size,
                                    currentIndex = 0
                                )
                            }
                        } else {
                            val errorBody = response.errorBody()?.string()
                            _uiState.update {
                                it.copy(isLoading = false, error = "Failed to generate QR: ${response.code()} $errorBody")
                            }
                            delay(5000)
                            continue
                        }
                    } catch (e: Exception) {
                        Log.e("GenerateQrVM", "Failed to fetch QR batch", e)
                        _uiState.update {
                            it.copy(isLoading = false, error = "Network error: ${e.message}")
                        }
                        delay(5000)
                        continue
                    }
                }

                // If we successfully have payloads, run the rotation loop
                for (i in payloads.indices) {
                    if (!isActive) return@launch
                    val bitmap = QrCodeGenerator.generate(payloads[i])
                    _uiState.update {
                        it.copy(
                            currentIndex = i,
                            countdown = 10,
                            qrBitmap = bitmap
                        )
                    }
                    // Count down from 9 to 0
                    for (sec in 9 downTo 0) {
                        if (!isActive) return@launch
                        delay(1000)
                        _uiState.update { it.copy(countdown = sec) }
                    }
                }

                // Once we complete rotating all codes, clear payloads to fetch a new batch next time
                payloads = emptyList()
            }
        }
    }

    fun stopGenerating() {
        rotationJob?.cancel()
        rotationJob = null
    }

    fun retry(sessionId: String) {
        startGenerating(sessionId)
    }

    override fun onCleared() {
        super.onCleared()
        stopGenerating()
    }
}
