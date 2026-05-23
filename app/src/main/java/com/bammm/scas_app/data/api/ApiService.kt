package com.bammm.scas_app.data.api

import com.bammm.scas_app.data.model.AuthResponse
import com.bammm.scas_app.data.model.CoursesResponse
import com.bammm.scas_app.data.model.GenerateQrResponse
import com.bammm.scas_app.data.model.GoogleLoginRequest
import com.bammm.scas_app.data.model.JoinCourseRequest
import com.bammm.scas_app.data.model.JoinCourseResponse
import com.bammm.scas_app.data.model.SessionsResponse
import com.bammm.scas_app.data.model.VerifyOtpRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("auth/google")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<AuthResponse>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Void>

    @GET("courses")
    suspend fun getCourses(): Response<CoursesResponse>

    @GET("courses/{courseId}/sessions")
    suspend fun getCourseSessions(@Path("courseId") courseId: String): Response<SessionsResponse>

    @POST("attendance/generate-qr")
    suspend fun generateQr(): Response<GenerateQrResponse>

    @POST("enrollment/join")
    suspend fun joinCourse(@Body request: JoinCourseRequest): Response<JoinCourseResponse>
}
