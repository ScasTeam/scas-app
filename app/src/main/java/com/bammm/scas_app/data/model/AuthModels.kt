package com.bammm.scas_app.data.model

import com.google.gson.annotations.SerializedName

data class GoogleLoginRequest(
    @SerializedName("google_token") val googleToken: String,
    @SerializedName("client_type") val clientType: String = "mobile",
    @SerializedName("android_id") val androidId: String? = null
)

data class VerifyOtpRequest(
    @SerializedName("email") val email: String,
    @SerializedName("code") val code: String,
    @SerializedName("client_type") val clientType: String = "mobile",
    @SerializedName("android_id") val androidId: String? = null
)

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: String?
)

data class AssignRoleRequest(
    val role: String
)

data class AssignRoleResponse(
    val status: String,
    val message: String?,
    val user: User?
)

data class AuthResponse(
    val status: String,
    val message: String?,
    val user: User?,
    val token: String?,
    @SerializedName("require_otp") val requireOtp: Boolean? = false,
    val email: String?
)
