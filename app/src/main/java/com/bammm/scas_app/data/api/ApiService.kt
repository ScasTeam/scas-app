package com.bammm.scas_app.data.api

import com.bammm.scas_app.data.model.AuthResponse
import com.bammm.scas_app.data.model.CoursesResponse
import com.bammm.scas_app.data.model.GenerateQrResponse
import com.bammm.scas_app.data.model.GoogleLoginRequest
import com.bammm.scas_app.data.model.JoinCourseRequest
import com.bammm.scas_app.data.model.JoinCourseResponse
import com.bammm.scas_app.data.model.SessionsResponse
import com.bammm.scas_app.data.model.VerifyOtpRequest
import com.bammm.scas_app.data.model.AssignRoleRequest
import com.bammm.scas_app.data.model.AssignRoleResponse
import com.bammm.scas_app.data.model.MyAttendanceResponse
import com.bammm.scas_app.data.model.StudentsResponse
import com.bammm.scas_app.data.model.GenericResponse
import com.bammm.scas_app.data.model.CreateSessionRequest
import com.bammm.scas_app.data.model.CreateSessionResponse
import com.bammm.scas_app.data.model.AttendeesResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @POST("auth/google")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<AuthResponse>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Void>

    @POST("auth/assign-role")
    suspend fun assignRole(@Body request: AssignRoleRequest): Response<AssignRoleResponse>

    @GET("courses")
    suspend fun getCourses(): Response<CoursesResponse>

    @GET("courses/{courseId}/sessions")
    suspend fun getCourseSessions(@Path("courseId") courseId: String): Response<SessionsResponse>

    @POST("courses/{courseId}/sessions")
    suspend fun createSession(
        @Path("courseId") courseId: String,
        @Body request: CreateSessionRequest
    ): Response<CreateSessionResponse>

    @GET("courses/{courseId}/my-attendance")
    suspend fun getMyAttendance(
        @Path("courseId") courseId: String
    ): Response<MyAttendanceResponse>

    @GET("courses/{courseId}/students")
    suspend fun getCourseStudents(
        @Path("courseId") courseId: String
    ): Response<StudentsResponse>

    @DELETE("courses/{courseId}/students/{studentId}")
    suspend fun kickStudent(
        @Path("courseId") courseId: String,
        @Path("studentId") studentId: String
    ): Response<GenericResponse>

    @GET("courses/{courseId}/sessions/{sessionId}/attendees")
    suspend fun getSessionAttendees(
        @Path("courseId") courseId: String,
        @Path("sessionId") sessionId: String
    ): Response<AttendeesResponse>

    @POST("attendance/generate-qr")
    suspend fun generateQr(): Response<GenerateQrResponse>

    @POST("enrollment/join")
    suspend fun joinCourse(@Body request: JoinCourseRequest): Response<JoinCourseResponse>
}
