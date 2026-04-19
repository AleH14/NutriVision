package com.example.nutrivision.data.model

data class UpdateUserRequest(
    val fullName: String? = null,
    val age: Int? = null,
    val heightCm: Int? = null,
    val currentWeightLb: Int? = null,
    val gender: String? = null,
    val physicalActivity: String? = null,
    val personalGoal: String? = null,
    val dailyCalorieGoalKcal: Double? = null
)
