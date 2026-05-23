package com.bammm.scas_app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bammm.scas_app.data.api.ApiClient
import com.bammm.scas_app.data.model.CourseSession
import com.bammm.scas_app.data.preferences.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionUiState(
    val isLoading: Boolean = true,
    val sessions: List<CourseSession> = emptyList(),
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val courseName: String = ""
)

class SessionViewModel(
    private val userPreferences: UserPreferences,
    private val courseId: String,
    courseName: String
) : ViewModel() {

    private val apiService = ApiClient.getService(userPreferences)

    private val _uiState = MutableStateFlow(SessionUiState(courseName = courseName))
    val uiState: StateFlow<SessionUiState> = _uiState

    init {
        loadSessions()
    }

    fun loadSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = apiService.getCourseSessions(courseId)
                if (response.isSuccessful) {
                    val sessions = response.body()?.sessions ?: emptyList()
                    _uiState.update { it.copy(isLoading = false, sessions = sessions) }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Failed to load sessions: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Network error: ${e.message}")
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            try {
                val response = apiService.getCourseSessions(courseId)
                if (response.isSuccessful) {
                    val sessions = response.body()?.sessions ?: emptyList()
                    _uiState.update { it.copy(isRefreshing = false, sessions = sessions) }
                } else {
                    _uiState.update {
                        it.copy(isRefreshing = false, error = "Failed to refresh: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isRefreshing = false, error = "Network error: ${e.message}")
                }
            }
        }
    }
}

class SessionViewModelFactory(
    private val context: Context,
    private val courseId: String,
    private val courseName: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionViewModel::class.java)) {
            val prefs = UserPreferences(context.applicationContext)
            @Suppress("UNCHECKED_CAST")
            return SessionViewModel(prefs, courseId, courseName) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
