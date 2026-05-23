package com.bammm.scas_app.data.api

import com.bammm.scas_app.data.model.AuthResponse
import com.bammm.scas_app.data.model.GoogleLoginRequest
import com.bammm.scas_app.data.model.VerifyOtpRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @POST("auth/google")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<AuthResponse>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<AuthResponse>
    
    @POST("auth/logout")
    suspend fun logout(): Response<Void>
}
