package com.bammm.scas_app.data.model

import com.google.gson.annotations.SerializedName

data class JoinCourseRequest(
    @SerializedName("registration_code") val registrationCode: String
)

data class JoinCourseResponse(
    val status: String,
    val message: String?
)
