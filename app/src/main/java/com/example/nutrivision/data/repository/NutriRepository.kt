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

    suspend fun register(user: User): Response<AuthResponse> {
        return apiService.register(user)
    }

    suspend fun getProfile(token: String): Response<User> {
        return apiService.getProfile("Bearer $token")
    }

    // Analysis
    suspend fun uploadAnalysisImage(token: String, image: MultipartBody.Part): Response<Analysis> {
        return apiService.uploadAnalysisImage("Bearer $token", image)
    }

    suspend fun getHistory(token: String): Response<List<Analysis>> {
        return apiService.getHistory("Bearer $token")
    }
}
