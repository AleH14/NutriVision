package com.example.nutrivision.data.network

import com.example.nutrivision.data.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("api/users/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/users/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @GET("api/users/profile")
    suspend fun getProfile(@Header("Authorization") token: String): Response<User>

    @PUT("api/users/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateUserRequest
    ): Response<User>

    @POST("api/users/calculate-daily-goal")
    suspend fun calculateDailyGoal(
        @Header("Authorization") token: String
    ): Response<CalorieGoalResponse>

    @Multipart
    @POST("api/users/analyze-food-image")
    suspend fun analyzeFoodImage(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part,
        @Part("photoTakenTime") photoTakenTime: String? = null
    ): Response<FoodAnalysisResponse>

    // Analysis
    @Multipart
    @POST("api/analysis/upload")
    suspend fun uploadAnalysisImage(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part
    ): Response<Analysis>

    @GET("api/analysis/history")
    suspend fun getHistory(@Header("Authorization") token: String): Response<List<Analysis>>

    @POST("api/users/save-analysis")
    suspend fun saveAnalysis(
        @Header("Authorization") token: String,
        @Body analysisData: SaveAnalysisRequest
    ): Response<SaveAnalysisResponse>
}
