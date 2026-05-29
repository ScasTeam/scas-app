package com.bammm.scas_app.di

import android.content.Context
import com.bammm.scas_app.data.api.ApiClient
import com.bammm.scas_app.data.api.ApiService
import com.bammm.scas_app.data.preferences.UserPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences {
        return UserPreferences(context)
    }

    @Provides
    @Singleton
    fun provideApiService(userPreferences: UserPreferences): ApiService {
        return ApiClient.getService(userPreferences)
    }

    @Provides
    @Singleton
    fun provideCourseRepository(apiService: ApiService): com.bammm.scas_app.data.repository.CourseRepository {
        return com.bammm.scas_app.data.repository.CourseRepository(apiService)
    }
}
