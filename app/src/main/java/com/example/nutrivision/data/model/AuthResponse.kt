package com.example.nutrivision.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val fullName: String,
    val email: String,
    val password: String,
    // Campos opcionales iniciales
    val age: Int = 18,
    val heightCm: Int = 160,
    val currentWeightLb: Int = 150,
    val gender: String = "masculino",
    val physicalActivity: String = "sedentario",
    val personalGoal: String = "mantener peso",
    val dailyCalorieGoalKcal: Double = 2000.0
)

data class AuthResponse(
    val message: String? = null,
    val token: String? = null,
    val user: User? = null,
    val error: String? = null
)
