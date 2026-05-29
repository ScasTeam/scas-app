package com.bammm.scas_app.data.model

import com.google.gson.annotations.SerializedName

data class SessionsResponse(
    val status: String?,
    val sessions: List<CourseSession>
)

data class CourseSession(
    val id: String,
    @SerializedName("course_id") val courseId: String,
    val title: String,
    val description: String?,
    val mode: String,
    val status: String,
    @SerializedName("opened_at") val openedAt: String?,
    @SerializedName("closed_at") val closedAt: String?,
    @SerializedName("attendance_logs_count") val attendanceLogsCount: Int? = null
)

data class CreateSessionRequest(
    val title: String,
    val description: String,
    val mode: String, // "online" or "offline"
    @SerializedName("opened_at") val openedAt: String,
    @SerializedName("closed_at") val closedAt: String
)

data class CreateSessionResponse(
    val status: String,
    val message: String?,
    val session: CourseSession?
)

data class AttendeeLog(
    val id: String,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("user_id") val userId: String,
    val status: String, // "present" or "late"
    @SerializedName("scanned_at") val scannedAt: String?,
    val student: User?
)

data class AttendeesResponse(
    val status: String?,
    val attendees: List<AttendeeLog>?
)

data class MySessionAttendance(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("session_title") val sessionTitle: String,
    @SerializedName("session_status") val sessionStatus: String, // "scheduled", "open", "closed"
    @SerializedName("opened_at") val openedAt: String?,
    @SerializedName("closed_at") val closedAt: String?,
    val attended: Boolean,
    @SerializedName("attendance_status") val attendanceStatus: String?, // "present", "late", "sick", "absent"
    @SerializedName("scanned_at") val scannedAt: String?
)

data class MyAttendanceStats(
    @SerializedName("total_sessions") val totalSessions: Int,
    val attended: Int,
    val missed: Int,
    val rate: Double
)

data class MyAttendanceResponse(
    val status: String?,
    val attendance: List<MySessionAttendance>?,
    val stats: MyAttendanceStats?
)

data class EnrolledStudent(
    val id: String,
    val name: String,
    val email: String,
    @SerializedName("enrolled_at") val enrolledAt: String?
)

data class StudentsResponse(
    val status: String?,
    val students: List<EnrolledStudent>?,
    val total: Int?
)

data class GenericResponse(
    val status: String,
    val message: String?
)
