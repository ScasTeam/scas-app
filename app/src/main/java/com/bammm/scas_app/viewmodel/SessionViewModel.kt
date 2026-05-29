package com.bammm.scas_app.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bammm.scas_app.data.api.ApiClient
import com.bammm.scas_app.data.model.CourseSession
import com.bammm.scas_app.data.model.EnrolledStudent
import com.bammm.scas_app.data.model.MySessionAttendance
import com.bammm.scas_app.data.model.MyAttendanceStats
import com.bammm.scas_app.data.model.AttendeeLog
import com.bammm.scas_app.data.model.CreateSessionRequest
import com.bammm.scas_app.data.model.AssignRoleRequest
import com.bammm.scas_app.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionUiState(
    val isLoading: Boolean = true,
    val sessions: List<CourseSession> = emptyList(),
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val courseName: String = "",
    val students: List<EnrolledStudent> = emptyList(),
    val isStudentsLoading: Boolean = false,
    val studentsError: String? = null,
    val myAttendance: List<MySessionAttendance> = emptyList(),
    val myStats: MyAttendanceStats? = null,
    val isAttendanceLoading: Boolean = false,
    val attendanceError: String? = null,
    val sessionAttendees: Map<String, List<AttendeeLog>> = emptyMap(),
    val attendeesLoading: Map<String, Boolean> = emptyMap(),
    val isCreatingSession: Boolean = false,
    val createSessionError: String? = null
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val apiService: com.bammm.scas_app.data.api.ApiService,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val courseId: String = savedStateHandle["courseId"] ?: ""
    private val courseName: String = savedStateHandle["courseName"] ?: ""

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

    fun loadMyAttendance() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAttendanceLoading = true, attendanceError = null) }
            try {
                val response = apiService.getMyAttendance(courseId)
                if (response.isSuccessful) {
                    val body = response.body()
                    _uiState.update { 
                        it.copy(
                            isAttendanceLoading = false,
                            myAttendance = body?.attendance ?: emptyList(),
                            myStats = body?.stats
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isAttendanceLoading = false, attendanceError = "Failed to load attendance: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isAttendanceLoading = false, attendanceError = e.message)
                }
            }
        }
    }

    fun loadCourseStudents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isStudentsLoading = true, studentsError = null) }
            try {
                val response = apiService.getCourseStudents(courseId)
                if (response.isSuccessful) {
                    val students = response.body()?.students ?: emptyList()
                    _uiState.update { it.copy(isStudentsLoading = false, students = students) }
                } else {
                    _uiState.update {
                        it.copy(isStudentsLoading = false, studentsError = "Failed to load students: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isStudentsLoading = false, studentsError = e.message)
                }
            }
        }
    }

    fun kickStudent(studentId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response = apiService.kickStudent(courseId, studentId)
                if (response.isSuccessful && response.body()?.status == "success") {
                    // Reload students list
                    loadCourseStudents()
                    onSuccess()
                } else {
                    _uiState.update {
                        it.copy(studentsError = "Failed to kick student: ${response.body()?.message ?: response.code()}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(studentsError = e.message)
                }
            }
        }
    }

    fun loadSessionAttendees(sessionId: String) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    attendeesLoading = it.attendeesLoading.toMutableMap().apply { put(sessionId, true) }
                )
            }
            try {
                val response = apiService.getSessionAttendees(courseId, sessionId)
                if (response.isSuccessful) {
                    val attendees = response.body()?.attendees ?: emptyList()
                    _uiState.update { 
                        it.copy(
                            sessionAttendees = it.sessionAttendees.toMutableMap().apply { put(sessionId, attendees) },
                            attendeesLoading = it.attendeesLoading.toMutableMap().apply { put(sessionId, false) }
                        )
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            attendeesLoading = it.attendeesLoading.toMutableMap().apply { put(sessionId, false) }
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        attendeesLoading = it.attendeesLoading.toMutableMap().apply { put(sessionId, false) }
                    )
                }
            }
        }
    }

    fun createSession(
        title: String,
        description: String,
        mode: String,
        openedAt: String,
        closedAt: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingSession = true, createSessionError = null) }
            try {
                val request = CreateSessionRequest(
                    title = title,
                    description = description,
                    mode = mode,
                    openedAt = openedAt,
                    closedAt = closedAt
                )
                val response = apiService.createSession(courseId, request)
                if (response.isSuccessful && response.body()?.status == "success") {
                    _uiState.update { it.copy(isCreatingSession = false) }
                    loadSessions()
                    onSuccess()
                } else {
                    val errorMsg = response.body()?.message ?: "Server code: ${response.code()}"
                    _uiState.update { it.copy(isCreatingSession = false, createSessionError = "Failed: $errorMsg") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isCreatingSession = false, createSessionError = e.message) }
            }
        }
    }
}
