package com.example.nutrivision.data.repository

import com.example.nutrivision.data.model.*
import com.example.nutrivision.data.network.ApiService
import okhttp3.MultipartBody
import retrofit2.Response

class NutriRepository(private val apiService: ApiService) {

    // Auth
    suspend fun login(request: LoginRequest): Response<AuthResponse> {
        return apiService.login(request)
    }

    suspend fun register(request: RegisterRequest): Response<AuthResponse> {
        return apiService.register(request)
    }

    suspend fun getProfile(token: String): Response<User> {
        return apiService.getProfile("Bearer $token")
    }

    suspend fun updateProfile(token: String, request: UpdateUserRequest): Response<User> {
        return apiService.updateProfile("Bearer $token", request)
    }

    suspend fun calculateDailyGoal(token: String): Response<CalorieGoalResponse> {
        return apiService.calculateDailyGoal("Bearer $token")
    }

    // Analysis
    suspend fun uploadAnalysisImage(token: String, image: MultipartBody.Part): Response<Analysis> {
        return apiService.uploadAnalysisImage("Bearer $token", image)
    }

    suspend fun getHistory(token: String): Response<List<Analysis>> {
        return apiService.getHistory("Bearer $token")
    }
}
