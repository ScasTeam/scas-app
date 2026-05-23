package com.bammm.scas_app.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bammm.scas_app.data.api.ApiClient
import com.bammm.scas_app.data.preferences.UserPreferences
import com.bammm.scas_app.util.QrCodeGenerator
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

class GenerateQrViewModel(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val apiService = ApiClient.getService(userPreferences)

    private val _uiState = MutableStateFlow(QrUiState())
    val uiState: StateFlow<QrUiState> = _uiState

    private var rotationJob: Job? = null

    init {
        fetchAndStartRotation()
    }

    fun fetchAndStartRotation() {
        rotationJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = apiService.generateQr()
                if (response.isSuccessful) {
                    val payloads = response.body()?.qrBatch ?: emptyList()
                    if (payloads.isEmpty()) {
                        _uiState.update {
                            it.copy(isLoading = false, error = "No QR codes received")
                        }
                        return@launch
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            qrPayloads = payloads,
                            totalCodes = payloads.size,
                            currentIndex = 0
                        )
                    }
                    startRotation(payloads)
                } else {
                    val errorBody = response.errorBody()?.string()
                    _uiState.update {
                        it.copy(isLoading = false, error = "Failed to generate QR: ${response.code()} $errorBody")
                    }
                }
            } catch (e: Exception) {
                Log.e("GenerateQrVM", "Failed to fetch QR batch", e)
                _uiState.update {
                    it.copy(isLoading = false, error = "Network error: ${e.message}")
                }
            }
        }
    }

    private fun startRotation(payloads: List<String>) {
        rotationJob?.cancel()
        rotationJob = viewModelScope.launch {
            while (isActive) {
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
                    // Count down from 10 to 1
                    for (sec in 9 downTo 0) {
                        if (!isActive) return@launch
                        delay(1000)
                        _uiState.update { it.copy(countdown = sec) }
                    }
                }
                // After showing all codes, fetch a new batch
                if (!isActive) return@launch
                _uiState.update { it.copy(isLoading = true) }
                try {
                    val response = apiService.generateQr()
                    if (response.isSuccessful) {
                        val newPayloads = response.body()?.qrBatch ?: emptyList()
                        if (newPayloads.isNotEmpty()) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    qrPayloads = newPayloads,
                                    totalCodes = newPayloads.size
                                )
                            }
                            // Continue loop with new payloads — but we need to update the local var
                            // So we call startRotation recursively and return
                            startRotation(newPayloads)
                            return@launch
                        }
                    }
                    _uiState.update { it.copy(isLoading = false, error = "Failed to refresh QR codes") }
                    return@launch
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, error = "Network error: ${e.message}") }
                    return@launch
                }
            }
        }
    }

    fun retry() {
        fetchAndStartRotation()
    }

    override fun onCleared() {
        super.onCleared()
        rotationJob?.cancel()
    }
}

class GenerateQrViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GenerateQrViewModel::class.java)) {
            val prefs = UserPreferences(context.applicationContext)
            @Suppress("UNCHECKED_CAST")
            return GenerateQrViewModel(prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
