package com.bammm.scas_app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bammm.scas_app.data.api.ApiClient
import com.bammm.scas_app.data.model.Course
import com.bammm.scas_app.data.preferences.UserPreferences
import com.bammm.scas_app.data.repository.CourseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class CourseUiState(
    val isLoading: Boolean = true,
    val courses: List<Course> = emptyList(),
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val joinMessage: String? = null,
    val joinError: String? = null,
    val isCreatingCourse: Boolean = false,
    val createCourseError: String? = null
)

@HiltViewModel
class CourseViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CourseUiState())
    val uiState: StateFlow<CourseUiState> = _uiState

    init {
        viewModelScope.launch {
            courseRepository.courses.collect { coursesList ->
                _uiState.update { it.copy(courses = coursesList) }
            }
        }
        loadCourses()
    }

    fun loadCourses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = courseRepository.loadCourses()
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
                val response = courseRepository.loadCourses()
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
                val response = courseRepository.joinCourse(registrationCode)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(joinMessage = response.body()?.message ?: "Joined successfully!") }
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
        _uiState.update { it.copy(joinMessage = null, joinError = null, createCourseError = null) }
    }

    fun createCourse(
        code: String,
        name: String,
        description: String?,
        domain: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingCourse = true, createCourseError = null) }
            try {
                val response = courseRepository.createCourse(code, name, description, domain)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isCreatingCourse = false) }
                    onSuccess()
                } else {
                    val errorBody = response.errorBody()?.string()
                    _uiState.update { it.copy(isCreatingCourse = false, createCourseError = "Failed to create course: $errorBody") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isCreatingCourse = false, createCourseError = "Network error: ${e.message}") }
            }
        }
    }
}
