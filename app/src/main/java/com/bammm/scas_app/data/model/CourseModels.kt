package com.bammm.scas_app.data.model

import com.google.gson.annotations.SerializedName

data class CoursesResponse(
    val status: String?,
    val courses: List<Course>
)

data class Course(
    val id: String,
    val code: String,
    @SerializedName("course_name") val courseName: String,
    val description: String?,
    @SerializedName("registration_code") val registrationCode: String?,
    @SerializedName("sessions_count") val sessionsCount: Int?,
    val lecturer: CourseLecturer?,
    val pivot: CoursePivot?
)

data class CourseLecturer(
    val id: String,
    val name: String,
    val email: String
)

data class CoursePivot(
    @SerializedName("user_id") val userId: String?,
    @SerializedName("course_id") val courseId: String?
)

data class CreateCourseRequest(
    val code: String,
    @SerializedName("course_name") val courseName: String,
    val description: String?,
    @SerializedName("allowed_email_domain") val allowedEmailDomain: String?
)

data class CreateCourseResponse(
    val status: String?,
    val message: String?,
    val course: Course?
)
