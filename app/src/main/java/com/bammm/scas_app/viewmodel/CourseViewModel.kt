package com.bammm.scas_app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bammm.scas_app.data.api.ApiClient
import com.bammm.scas_app.data.model.Course
import com.bammm.scas_app.data.model.JoinCourseRequest
import com.bammm.scas_app.data.preferences.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CourseUiState(
    val isLoading: Boolean = true,
    val courses: List<Course> = emptyList(),
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val joinMessage: String? = null,
    val joinError: String? = null
)

class CourseViewModel(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val apiService = ApiClient.getService(userPreferences)

    private val _uiState = MutableStateFlow(CourseUiState())
    val uiState: StateFlow<CourseUiState> = _uiState

    init {
        loadCourses()
    }

    fun loadCourses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = apiService.getCourses()
                if (response.isSuccessful) {
                    val courses = response.body()?.courses ?: emptyList()
                    _uiState.update { it.copy(isLoading = false, courses = courses) }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Failed to load courses: ${response.code()}")
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
                val response = apiService.getCourses()
                if (response.isSuccessful) {
                    val courses = response.body()?.courses ?: emptyList()
                    _uiState.update { it.copy(isRefreshing = false, courses = courses) }
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

    fun joinCourse(registrationCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(joinError = null, joinMessage = null) }
            try {
                val response = apiService.joinCourse(JoinCourseRequest(registrationCode))
                if (response.isSuccessful) {
                    _uiState.update { it.copy(joinMessage = response.body()?.message ?: "Joined successfully!") }
                    loadCourses() // Refresh course list
                } else {
                    val errorBody = response.errorBody()?.string()
                    _uiState.update { it.copy(joinError = "Failed to join: $errorBody") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(joinError = "Network error: ${e.message}") }
            }
        }
    }

    fun clearJoinMessages() {
        _uiState.update { it.copy(joinMessage = null, joinError = null) }
    }
}

class CourseViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CourseViewModel::class.java)) {
            val prefs = UserPreferences(context.applicationContext)
            @Suppress("UNCHECKED_CAST")
            return CourseViewModel(prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
