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
    @SerializedName("closed_at") val closedAt: String?
)
