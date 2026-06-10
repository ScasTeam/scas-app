package com.bammm.scas_app.data.repository

import com.bammm.scas_app.data.api.ApiService
import com.bammm.scas_app.data.model.Course
import com.bammm.scas_app.data.model.CoursesResponse
import com.bammm.scas_app.data.model.CreateCourseRequest
import com.bammm.scas_app.data.model.CreateCourseResponse
import com.bammm.scas_app.data.model.JoinCourseRequest
import com.bammm.scas_app.data.model.JoinCourseResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepository @Inject constructor(
    private val apiService: ApiService
) {
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    suspend fun loadCourses(): Response<CoursesResponse> {
        val response = apiService.getCourses()
        if (response.isSuccessful) {
            response.body()?.courses?.let {
                _courses.value = it
            }
        }
        return response
    }

    suspend fun joinCourse(registrationCode: String): Response<JoinCourseResponse> {
        val response = apiService.joinCourse(JoinCourseRequest(registrationCode))
        if (response.isSuccessful) {
            loadCourses() // Automatically refresh courses cache upon successful join
        }
        return response
    }

    suspend fun createCourse(code: String, name: String, description: String?, domain: String?): Response<CreateCourseResponse> {
        val response = apiService.createCourse(
            CreateCourseRequest(
                code = code,
                courseName = name,
                description = description,
                allowedEmailDomain = domain
            )
        )
        if (response.isSuccessful) {
            loadCourses() // Refresh courses after creating
        }
        return response
    }
}
