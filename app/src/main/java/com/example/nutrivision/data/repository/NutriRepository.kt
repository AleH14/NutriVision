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

    // UN SOLO método - la fecha es opcional
    suspend fun getProfile(token: String, date: String? = null): Response<User> {
        return apiService.getProfile("Bearer $token", date)
    }

    suspend fun updateProfile(token: String, request: UpdateUserRequest): Response<User> {
        return apiService.updateProfile("Bearer $token", request)
    }

    suspend fun calculateDailyGoal(token: String): Response<CalorieGoalResponse> {
        return apiService.calculateDailyGoal("Bearer $token")
    }

    suspend fun analyzeFoodImage(token: String, image: MultipartBody.Part, photoTakenTime: String? = null): Response<FoodAnalysisResponse> {
        return apiService.analyzeFoodImage("Bearer $token", image, photoTakenTime)
    }

    // Analysis
    suspend fun uploadAnalysisImage(token: String, image: MultipartBody.Part): Response<Analysis> {
        return apiService.uploadAnalysisImage("Bearer $token", image)
    }

    suspend fun getHistory(token: String): Response<List<Analysis>> {
        return apiService.getHistory("Bearer $token")
    }

    suspend fun saveAnalysis(
        token: String,
        imageFilename: String,
        dishes: List<Dish>,
        nutrition: Nutrition,
        plateAnalysis: String,
        mealType: String,
        createdAt: String,
        date: String,
        time: String = ""
    ): Response<SaveAnalysisResponse> {
        val request = SaveAnalysisRequest(
            imageFilename = imageFilename,
            dishes = dishes,
            nutrition = nutrition,
            plateAnalysis = plateAnalysis,
            mealType = mealType,
            createdAt = createdAt,
            date = date,
            time = time
        )
        return apiService.saveAnalysis("Bearer $token", request)
    }

    suspend fun getAnalysesByDate(
        token: String,
        date: String? = null
    ): Response<AnalysesResponse> {
        return apiService.getAnalysesByDate("Bearer $token", date)
    }

    suspend fun changePassword(
        token: String,
        currentPassword: String,
        newPassword: String
    ): Response<Map<String, String>> {
        val request = mapOf(
            "currentPassword" to currentPassword,
            "newPassword" to newPassword
        )
        return apiService.changePassword("Bearer $token", request)
    }

    suspend fun resetDailySummary(token: String): Response<Map<String, Any>> {
        return apiService.resetDailySummary("Bearer $token")
    }

}
